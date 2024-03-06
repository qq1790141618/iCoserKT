package com.fixeam.icoser.model

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
import com.fixeam.icoser.ui.main.activity.MainActivity

/**
 * 向用户发送一条写真集类的通知
 * @param context 执行本次操作的上下文对象
 * @param album 需要向用户推送的写真集对象
 */
fun sendAlbumNotification(context: Context, album: Albums) {
    val title = when(album.type){
        "follow" -> "您关注的模特 ${album.model} 有更新"
        else -> "热门推荐 模特 ${album.model} 的新内容"
    }
    val message = "发布了新的写真集 ${album.name}, 时间${album.create_time}"

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
                createCustomNotification(context, title, message, album.id, pendingIntent, bigPictureStyle)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // 图片加载失败
                createCustomNotification(context, title, message, album.id, pendingIntent)
            }
        })
}
// 发送软件更新通知
fun sendSoftwareUpdateNotification(context: Context, version: String, versionId: Int) {
    createCustomNotification(context = context, title = "当前软件存在更新", message = "软件版本 $version 已经发布, 点击立即更新", notificationId = versionId)
}
// 发送自定义通知内容
fun createCustomNotification(context: Context, title: String, message: String, notificationId: Int? = null, pendingIntent: PendingIntent? = null, bigPictureStyle: NotificationCompat.BigPictureStyle? = null, sendRightNow: Boolean = true): Notification {
    val channelId = context.getString(R.string.default_notification_channel_id)
    val channelName = context.getString(R.string.default_notification_channel_name)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    notificationManager.createNotificationChannel(channel)
    val pi: PendingIntent = when(pendingIntent){
        null -> Intent(context, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        else -> pendingIntent
    }

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setAutoCancel(true)
        .setContentIntent(pi)
    if(bigPictureStyle != null){
        notificationBuilder.setStyle(bigPictureStyle)
    }
    val notification = notificationBuilder.build()
    val id = when(notificationId){
        null -> System.currentTimeMillis().toInt()
        else -> notificationId
    }

    // 检测是否立即发送和用户是否允许推送
    val sharedPreferences = context.getSharedPreferences("notification", Context.MODE_PRIVATE)
    val allowedNotification = sharedPreferences.getInt("allow", 1)
    if(sendRightNow && allowedNotification != 0){
        notificationManager.notify(id, notification)
    }

    return notification
}
