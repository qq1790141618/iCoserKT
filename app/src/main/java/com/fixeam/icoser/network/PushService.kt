package com.fixeam.icoser.network

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fixeam.icoser.model.checkForUpdate
import com.fixeam.icoser.model.createCustomNotification
import com.fixeam.icoser.model.newVersion
import com.fixeam.icoser.model.newsData
import com.fixeam.icoser.model.sendAlbumNotification
import com.fixeam.icoser.model.sendSoftwareUpdateNotification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class PushService : Service() {
    override fun onBind(intent: Intent): IBinder {
        TODO("提供返回值")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 服务已经启动
        val notification = createCustomNotification(
            context = this,
            title = "iCoser服务启动",
            message = "iCoser服务启动, 这条通知将代表可以顺利向您推送内容。",
            sendRightNow = false
        )
        startForeground(1, notification)
        stopForeground(true)

        // 启动消息定时推送
        val periodicWorkRequest = PeriodicWorkRequestBuilder<DataFetchWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(periodicWorkRequest)

        return super.onStartCommand(intent, flags, startId)
    }
}

// 定时执行请求类
class DataFetchWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        checkForUser(applicationContext)
        donePushRequest()
        return Result.success()
    }

    private fun donePushRequest(){
        // 获取已经推送过的写真集
        val pushedAlbums = applicationContext.getSharedPreferences("push", AppCompatActivity.MODE_PRIVATE).getString("pushed_albums", "[]")
        val listType = object : TypeToken<List<Int>>() {}.type
        val pushedList: List<Int> = Gson().fromJson(pushedAlbums, listType)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentTime = LocalDateTime.now()
        if(userToken != null){
            requestFollowData(applicationContext, true){
                for (album in followAlbumList){
                    if(!album.isNew || pushedList.contains(album.id)){
                        continue
                    }
                    val pastTime = LocalDateTime.parse(album.create_time, formatter)
                    if(album.type == "follow" || ChronoUnit.MINUTES.between(pastTime, currentTime) < 120){
                        sendAlbumNotification(applicationContext, album)
                    }
                }
            }
        } else {
            requestNewData(applicationContext){
                for (album in newsData){
                    if(pushedList.contains(album.id)){
                        continue
                    }
                    val pastTime = LocalDateTime.parse(album.create_time, formatter)
                    if(ChronoUnit.MINUTES.between(pastTime, currentTime) < 120){
                        sendAlbumNotification(applicationContext, album)
                    }
                }
            }
        }
        checkForUpdate(applicationContext){
            newVersion?.version?.let { it1 ->
                // 检测是否已经推送过此版本，推送过则不推送
                val sharedPreferences = applicationContext.getSharedPreferences("version",
                    AppCompatActivity.MODE_PRIVATE
                )
                val pushVersion = sharedPreferences.getString("do_not_notification_version", "")

                if(pushVersion != it1){
                    newVersion?.version_id?.let { it2 ->
                        sendSoftwareUpdateNotification(
                            applicationContext,
                            it1,
                            it2
                        )
                    }
                    sharedPreferences.edit().putString("do_not_notification_version", it1).apply()
                }
            }
        }
    }
}