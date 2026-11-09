//! Wi-Fi transport: TLS over TCP + mDNS service advertisement.
//!
//! The daemon listens on `0.0.0.0:<port>` with mutual or server-only TLS
//! (using `rustls`) and publishes a `_droidpad._tcp.local.` mDNS record so
//! that the Android app can discover it on the LAN without manual IP entry.
//!
//! # TLS certificate
//! TODO: Replace the self-signed certificate generated here with a proper PKI
//!       or a user-provided PEM file from the settings directory.

use std::net::SocketAddr;
use std::sync::Arc;

use anyhow::{Context, Result};
use async_trait::async_trait;
use mdns_sd::{ServiceDaemon, ServiceInfo};
use rustls::ServerConfig;
use tokio::net::{TcpListener, TcpStream};
use tokio_rustls::{server::TlsStream, TlsAcceptor};
use tracing::{info, warn};

use super::{Connection, Transport};
use crate::protocol::packet::Packet;

// ---------------------------------------------------------------------------
// WifiConnection
// ---------------------------------------------------------------------------

/// An accepted TLS-over-TCP connection.
pub struct WifiConnection {
    stream: TlsStream<TcpStream>,
    peer: SocketAddr,
}

#[async_trait]
impl Connection for WifiConnection {
    async fn read_packet(&mut self) -> Result<Packet> {
        // We need a mutable reference to the inner stream implementing AsyncRead.
        Packet::read_async(&mut self.stream).await
    }

    async fn write_packet(&mut self, pkt: &Packet) -> Result<()> {
        pkt.write_async(&mut self.stream).await
    }

    fn peer_name(&self) -> String {
        self.peer.to_string()
    }
}

// ---------------------------------------------------------------------------
// WifiTransport
// ---------------------------------------------------------------------------

/// TCP+TLS transport with mDNS discovery.
pub struct WifiTransport {
    listener: TcpListener,
    acceptor: TlsAcceptor,
    _mdns: ServiceDaemon,
    port: u16,
}

impl WifiTransport {
    /// Create and bind the TLS TCP listener, register the mDNS service.
    ///
    /// `tls_config` must be a fully configured `rustls::ServerConfig`.
    /// Use [`build_self_signed_tls_config`] to create a development config.
    pub async fn new(port: u16, tls_config: Arc<ServerConfig>) -> Result<Self> {
        let addr = format!("0.0.0.0:{port}");
        let listener = TcpListener::bind(&addr)
            .await
            .with_context(|| format!("binding Wi-Fi TCP port {port}"))?;
        info!("Wi-Fi transport listening on {addr}");

        let acceptor = TlsAcceptor::from(tls_config);

        // Start mDNS daemon and register service.
        let mdns = ServiceDaemon::new().context("creating mDNS daemon")?;

        // Build a service info record.
        let hostname = gethostname();
        let service_info = ServiceInfo::new(
            "_droidpad._tcp.local.",
            "DroidPad",
            &format!("{hostname}.local."),
            "", // empty = auto-detect IP
            port,
            None, // no extra TXT records
        )
        .context("building mDNS ServiceInfo")?;

        mdns.register(service_info)
            .context("registering mDNS _droidpad._tcp.local. service")?;
        info!("mDNS service _droidpad._tcp.local. registered on port {port}");

        Ok(Self {
            listener,
            acceptor,
            _mdns: mdns,
            port,
        })
    }
}

#[async_trait]
impl Transport for WifiTransport {
    async fn accept(&mut self) -> Result<Box<dyn Connection>> {
        loop {
            let (stream, peer) = self.listener.accept().await.context("TCP accept")?;
            info!("Wi-Fi: incoming TCP connection from {peer}");

            match self.acceptor.accept(stream).await {
                Ok(tls_stream) => {
                    return Ok(Box::new(WifiConnection {
                        stream: tls_stream,
                        peer,
                    }));
                }
                Err(e) => {
                    warn!("Wi-Fi: TLS handshake failed from {peer}: {e}");
                    // Loop and try the next connection.
                }
            }
        }
    }

    fn name(&self) -> &'static str {
        "Wi-Fi"
    }
}

// ---------------------------------------------------------------------------
// Self-signed TLS config helper
// ---------------------------------------------------------------------------

/// Build a `rustls::ServerConfig` with a freshly generated self-signed certificate.
///
/// TODO: This is **development-only**.  For production, generate a certificate
///       once (e.g. with `rcgen`), persist it to `~/.local/share/droidpad/`,
///       and load it on subsequent launches.  Also implement certificate pinning
///       on the Android side so that the user is prompted to trust on first
///       connection.
pub fn build_self_signed_tls_config() -> Result<Arc<ServerConfig>> {
    use rcgen::{generate_simple_self_signed, CertifiedKey};
    use rustls::pki_types::{CertificateDer, PrivateKeyDer};

    let subject_alt_names = vec!["droidpad.local".to_string(), "localhost".to_string()];
    let CertifiedKey { cert, key_pair } =
        generate_simple_self_signed(subject_alt_names).context("generating self-signed cert")?;

    let cert_der = CertificateDer::from(cert.der().to_vec());
    let key_der = PrivateKeyDer::try_from(key_pair.serialize_der())
        .map_err(|e| anyhow::anyhow!("invalid private key DER: {e}"))?;

    let config = ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(vec![cert_der], key_der)
        .context("building rustls ServerConfig")?;

    Ok(Arc::new(config))
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fn gethostname() -> String {
    std::fs::read_to_string("/etc/hostname")
        .unwrap_or_else(|_| "droidpad-host".to_string())
        .trim()
        .to_string()
}
