package com.example.niloclient.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.niloclient.Constants
import com.example.niloclient.R
import com.example.niloclient.order.OrderActivity
import com.example.niloclient.product.MainActivity

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)

        registerNewTokenLocal(newToken)
    }

    private fun registerNewTokenLocal(newToken: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        preferences.edit {
            putString(Constants.PROP_TOKEN, newToken)
                .apply()
        }
        Log.i("new token", newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()){
            sendNotificationByData(remoteMessage.data)
        }
        remoteMessage.notification?.let {
            val imgUrl = it.imageUrl //"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQQnMKN8w0uIgPqkIFd--6LoaGrH5R2a-o-cA&usqp=CAU"
            if (imgUrl == null){
                sendNotification(it)
            }else{
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(imgUrl)
                    .into(object : CustomTarget<Bitmap?>(){
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            sendNotification(it, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }

        }

    }

    private fun sendNotification(notifaction: RemoteMessage.Notification, bitmap: Bitmap? = null) {
        val intent = Intent(this, OrderActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(notifaction.title)
            .setContentText(notifaction.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setColor(ContextCompat.getColor(this, R.color.yellow_a400))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notifaction.body))

        bitmap?.let {
            notificationBuilder
                .setLargeIcon(bitmap)
                .setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap))
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun sendNotificationByData(data: Map<String, String>) {
        val intent = Intent(this, OrderActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(data["title"])
            .setContentText(data["body"])
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setColor(ContextCompat.getColor(this, R.color.yellow_a400))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(data["body"]))


        val actionIntent = data.get(Constants.ACTION_INTENT)?.toInt()
        val orderId = data.get(Constants.PROP_ID)
        val status = data.get(Constants.PROP_STATUS)?.toInt()

        val trackIntent = Intent(this, OrderActivity::class.java).apply {
            putExtra(Constants.ACTION_INTENT, actionIntent) // 1 = track
            putExtra(Constants.PROP_ID,orderId)
            putExtra(Constants.PROP_STATUS, status)
        }
        val trackPendingIntent = PendingIntent.getActivity(this,
            System.currentTimeMillis().toInt(), trackIntent, PendingIntent.FLAG_IMMUTABLE)
        val action = NotificationCompat.Action.Builder(R.drawable.ic_local_shipping,
            "Rastrear ahora", trackPendingIntent).build()

        notificationBuilder.addAction(action)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}