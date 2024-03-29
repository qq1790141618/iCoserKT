package com.fixeam.icoser.model

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.fixeam.icoser.R
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.ApiNetService
import com.fixeam.icoser.network.MediaFormatItem
import com.fixeam.icoser.network.PackageInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.pow

// 字节转可视化大小
fun bytesToReadableSize(size: Long): String {
    if (size <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = min((Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt(), units.size - 1)

    return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

// 获取最佳视频分辨率
fun getBestMedia(list: List<MediaFormatItem>, bestResolutionRatio: Int = 540): Int{
    fun getIntResolution(resolutionRatio: String): Int{
        return resolutionRatio.replace("p", "").toInt()
    }

    for ((index, _) in list.withIndex()){
        if(index == 0){
            continue
        }

        val thisResolutionRatio = getIntResolution(list[index].resolution_ratio)
        if(thisResolutionRatio == bestResolutionRatio){
            return index
        }

        val lastResolutionRatio = getIntResolution(list[index - 1].resolution_ratio)
        if(bestResolutionRatio in (thisResolutionRatio + 1)..<lastResolutionRatio){
            return index
        }
    }
    return 0
}

// 格式化时间显示
fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val remainingSeconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", minutes, remainingSeconds)
    }
}

var hotData: List<Albums> = listOf()
var newsData: List<Albums> = listOf()

// 计算时间差
fun calculateTimeAgo(eventTime: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = LocalDateTime.parse(eventTime, formatter)
    val now = LocalDateTime.now()

    val seconds = ChronoUnit.SECONDS.between(dateTime, now)
    if (seconds < 60) {
        return "$seconds 秒前"
    }

    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    if (minutes < 60) {
        return "$minutes 分钟前"
    }

    val hours = ChronoUnit.HOURS.between(dateTime, now)
    if (hours < 24) {
        return "$hours 小时前"
    }

    val days = ChronoUnit.DAYS.between(dateTime, now)
    if (days < 7) {
        return "$days 天前"
    }

    val weeks = days / 7
    if (weeks < 4) {
        return "$weeks 周前"
    }

    val months = ChronoUnit.MONTHS.between(dateTime, now)
    if (months < 12) {
        return "${(months + 1)} 个月前"
    }

    val years = ChronoUnit.YEARS.between(dateTime, now)
    return "$years 年前"
}

// 获取设备信息
fun getSystemInfo(context: Context): String {
    val osVersion = android.os.Build.VERSION.RELEASE
    val deviceModel = android.os.Build.MODEL
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val softwareVersion = packageInfo.versionName
    val systemVersion = android.os.Build.DISPLAY

    return "Android $osVersion;$deviceModel Build/$systemVersion; Version $softwareVersion"
}

var newVersion: PackageInfo? = null
fun checkForUpdate(context: Context, callback: (Boolean) -> Unit) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = packageInfo.versionCode

    val sharedPreferences = context.getSharedPreferences("version", AppCompatActivity.MODE_PRIVATE)
    val doNotAlertVersion = sharedPreferences.getInt("do_not_alert_version", -1)

    val call = ApiNetService.getLatestVersion(context.getString(R.string.app_type))
    call.enqueue(object : Callback<PackageInfo> {
        override fun onResponse(call: Call<PackageInfo>, response: Response<PackageInfo>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if(responseBody != null && responseBody.version_id > versionCode){
                    if(doNotAlertVersion < 0 || doNotAlertVersion != responseBody.version_id){
                        newVersion = responseBody
                        callback(true)
                    } else {
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            }
        }

        override fun onFailure(call: Call<PackageInfo>, t: Throwable) {
            // 处理网络请求失败的情况
            callback(false)
        }
    })
}

data class Option(
    var iconId: Int,
    var iconColor: ColorStateList,
    var textId: Int = -1,
    var text: String? = null,
    var contentText: String? = null,
    var onClick: () -> Unit = {},
    var onClickWithContext: (view: View) -> Unit = {},
    var leftImageUrl: String? = null,
    var showHrefIcon: Boolean = true,
    var clearMargin: Boolean = false,
    var showRemoveButton: Boolean = false,
    var onRemove: () -> Unit = {},
)

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val sharedPreferences = context.getSharedPreferences("glide_module", Context.MODE_PRIVATE)
        val cacheSize = sharedPreferences.getInt("disk_cache_size_gb", 10)
        val diskCacheSizeBytes = 1024L * 1024 * 1024 * cacheSize
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
    }
}



