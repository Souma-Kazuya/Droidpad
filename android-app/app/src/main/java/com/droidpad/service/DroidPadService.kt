package com.droidpad.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.droidpad.MainActivity
import com.droidpad.R
import com.droidpad.hid.GamepadEncoder
import com.droidpad.hid.KeyboardEncoder
import com.droidpad.transport.ConnectionState
import com.droidpad.transport.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DroidPadService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var transportManager: TransportManager
        private set
    val keyboardEncoder = KeyboardEncoder()
    val gamepadEncoder = GamepadEncoder()

    private var vibrator: Vibrator? = null

    inner class LocalBinder : Binder() {
        fun getService(): DroidPadService = this@DroidPadService
    }

    override fun onCreate() {
        super.onCreate()
        transportManager = TransportManager(serviceScope)

        initVibrator()
        createNotificationChannel()

        transportManager.onRumbleReceived = { left, right ->
            triggerHaptic(left, right)
        }

        transportManager.connectionState.onEach { state ->
            updateNotification(state)
        }.launchIn(serviceScope)
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun triggerHaptic(left: Byte, right: Byte) {
        val l = left.toInt() and 0xFF
        val r = right.toInt() and 0xFF
        val amplitude = (l.coerceAtLeast(r)).coerceIn(1, 255)
        val duration = 80L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(state: ConnectionState) {
        val text = when (state) {
            is ConnectionState.Connected -> "Connected to ${state.peerName} via ${state.transportName}"
            is ConnectionState.Connecting -> "Connecting via ${state.transportName}..."
            is ConnectionState.Error -> "Connection error: ${state.message}"
            ConnectionState.Disconnected -> "Ready to connect"
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DroidPad")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(state is ConnectionState.Connected)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        transportManager.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "droidpad_foreground"
        const val NOTIFICATION_ID = 1001
    }
}
