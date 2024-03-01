package com.fixeam.icoser.model

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fixeam.icoser.R
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.ui.album_page.AlbumViewActivity

fun sendAlbumNotification(context: Context, album: Albums) {
    val sharedPreferences = context.getSharedPreferences("notification", Context.MODE_PRIVATE)
    val allowedNotification = sharedPreferences.getInt("allow", 1)
    if(allowedNotification == 0){
        return
    }

    val channelId = context.getString(R.string.default_notification_channel_id)
    val channelName = context.getString(R.string.default_notification_channel_name)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    notificationManager.createNotificationChannel(channel)

    val title = "您关注的模特 ${album.model} 有更新"
    val message = "发布了新的写真集 ${album.name}, 时间${album.create_time}"
    val notificationId = album.id

    val intent = Intent(context, AlbumViewActivity::class.java)
    intent.putExtra("id", album.id)
    val pendingIntent = PendingIntent.getActivity(context, album.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    accessLog(context, album.id.toString(), "PUSH"){ }

    // 使用 Glide 加载网络图片
    Glide.with(context)
        .asBitmap()
        .load(album.poster)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val bigPictureStyle = NotificationCompat.BigPictureStyle()
                    .bigPicture(resource)
                    .setSummaryText("写真集详情")

                val notificationBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(bigPictureStyle) // 设置 BigPictureStyle

                val notification = notificationBuilder.build()
                notificationManager.notify(notificationId, notification)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // 图片加载失败
                val notificationBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                val notification = notificationBuilder.build()
                notificationManager.notify(notificationId, notification)
            }
        })
}
