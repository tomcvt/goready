package com.tomcvt.goready.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tomcvt.goready.R
import com.tomcvt.goready.data.AlarmEntity

class BleNotificationManager(
    private val passedContext: Context,
) {
    private val context = passedContext.applicationContext
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    fun notifyConnected(address: String) {
        createNotificationChannel()

        val navIntent = null //for now

        val cColor = ContextCompat.getColor(context, R.color.bt_connected)
        val largeIcon = drawableToBitmap(context, R.drawable.ic_bt_con_24, cColor)

        val notification = NotificationCompat.Builder(context, BLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bt_con_24)
            .setContentTitle("Bluetooth")
            .setContentText("Connected to $address")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            //.setFullScreenIntent(fullScreenIntent, true)
            .build()

        notificationManager.notify(BLE_NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(BLE_NOTIFICATION_ID)
    }

    fun notifyDisconnected(address: String) {
        createNotificationChannel()

        val navIntent = null //for now

        val cColor = ContextCompat.getColor(context, R.color.bt_disconnected)
        val largeIcon = drawableToBitmap(context, R.drawable.ic_bt_dc_24, cColor)

        val notification = NotificationCompat.Builder(context, BLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bt_dc_24)
            .setContentTitle("Bluetooth")
            .setContentText("Disconnected from $address")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            //.setFullScreenIntent(fullScreenIntent, true)
            .build()

        notificationManager.notify(BLE_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(BLE_CHANNEL_ID)
            if (existingChannel != null) {
                return
            }
            val channel = NotificationChannel(
                BLE_CHANNEL_ID,
                BLE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // sound handled by MediaPlayer
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun drawableToBitmap(context: Context, @DrawableRes resId: Int, tint: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resId)!!.mutate()
        drawable.setTint(tint)

        val sizePx = (64 * context.resources.displayMetrics.density).toInt() // 64dp -> px for this device

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        const val BLE_NOTIFICATION_ID = 113
        const val BLE_CHANNEL_ID = "ble_channel"
        const val BLE_CHANNEL_NAME = "BT Connection"
    }
}