package com.fixeam.icoserkt

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// 字节转可视化大小
fun bytesToReadableSize(size: Int): String {
    if (size <= 0) {
        return "0 B"
    }
    val units = arrayOf("B", "KB", "MB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
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
        return "${(months + 1)} 月前"
    }

    val years = ChronoUnit.YEARS.between(dateTime, now)
    return "$years 年前"
}