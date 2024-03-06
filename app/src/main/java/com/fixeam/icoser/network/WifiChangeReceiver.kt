package com.fixeam.icoser.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.Toast

class WifiChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = cm.activeNetworkInfo
            if (networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected) {
                Toast.makeText(context, "已切换WIFI环境，可放心浏览本APP", Toast.LENGTH_SHORT).show()
                // 在这里可以执行其他相关操作
            }
        }
    }
}