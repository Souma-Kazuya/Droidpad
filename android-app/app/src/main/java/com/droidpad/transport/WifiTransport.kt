package com.droidpad.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WifiTransport(
    private val host: String,
    private val port: Int = 27183
) : DroidTransport {

    override val name: String = "Wi-Fi"
    private var sslSocket: SSLSocket? = null

    override suspend fun open(): Pair<InputStream, OutputStream> = withContext(Dispatchers.IO) {
        // Build trust-all SSL context for self-signed development certificates
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val factory = sslContext.socketFactory
        val socket = factory.createSocket() as SSLSocket
        socket.tcpNoDelay = true
        socket.connect(InetSocketAddress(host, port), 5000)
        socket.startHandshake()

        sslSocket = socket
        Pair(socket.inputStream, socket.outputStream)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            sslSocket?.close()
        } catch (_: Exception) {}
        sslSocket = null
    }

    companion object {
        /**
         * Discover local DroidPad Linux hosts via mDNS / NSD service discovery.
         * Hardened with a serialized resolution queue to prevent NsdManager FAILURE_ALREADY_ACTIVE errors.
         */
        fun discoverHosts(
            context: Context,
            onHostFound: (name: String, host: String, port: Int) -> Unit
        ): NsdManager.DiscoveryListener {
            val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val serviceType = "_droidpad._tcp."

            val pendingResolutions = java.util.ArrayDeque<NsdServiceInfo>()
            var isResolving = false
            val lock = Any()

            fun resolveNext() {
                val nextService: NsdServiceInfo
                synchronized(lock) {
                    if (isResolving || pendingResolutions.isEmpty()) {
                        return
                    }
                    isResolving = true
                    nextService = pendingResolutions.removeFirst()
                }

                try {
                    nsdManager.resolveService(nextService, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            synchronized(lock) {
                                isResolving = false
                            }
                            resolveNext()
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                            try {
                                resolvedInfo?.let {
                                    val ip = it.host?.hostAddress ?: return@let
                                    onHostFound(it.serviceName, ip, it.port)
                                }
                            } finally {
                                synchronized(lock) {
                                    isResolving = false
                                }
                                resolveNext()
                            }
                        }
                    })
                } catch (e: Exception) {
                    synchronized(lock) {
                        isResolving = false
                    }
                    resolveNext()
                }
            }

            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    synchronized(lock) {
                        pendingResolutions.clear()
                        isResolving = false
                    }
                }
                override fun onDiscoveryStarted(serviceType: String?) {}
                override fun onDiscoveryStopped(serviceType: String?) {
                    synchronized(lock) {
                        pendingResolutions.clear()
                        isResolving = false
                    }
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    if (serviceInfo?.serviceType?.contains("droidpad") == true) {
                        synchronized(lock) {
                            pendingResolutions.addLast(serviceInfo)
                        }
                        resolveNext()
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }

            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            return listener
        }
    }
}
