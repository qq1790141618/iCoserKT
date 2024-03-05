package com.fixeam.icoser.ui.main.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityMainBinding
import com.fixeam.icoser.model.collectionFragment
import com.fixeam.icoser.model.hasNotificationProgression
import com.fixeam.icoser.model.homeFragment
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.showFragment
import com.fixeam.icoser.model.smartVideoFragment
import com.fixeam.icoser.model.userFragment
import com.fixeam.icoser.network.PushService
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.network.verifyTokenAndGetUserInform
import com.fixeam.icoser.ui.main.fragment.CollectionFragment
import com.fixeam.icoser.ui.main.fragment.HomeFragment
import com.fixeam.icoser.ui.main.fragment.SmartVideoFragment
import com.fixeam.icoser.ui.main.fragment.UserFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {
    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        layout = binding.application
        setContentView(view)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        val toggleButton = findViewById<MaterialButtonToggleGroup>(R.id.tab_bar)
        toggleButton.addOnButtonCheckedListener{group, checkedId, isChecked ->
            if(!isChecked){
                return@addOnButtonCheckedListener
            }

            val childCount = group.childCount
            var selectIndex = 0
            val colorSelected = ContextCompat.getColor(this, R.color.brand_primary)

            for (index in 0 until childCount){
                val childAt = group.getChildAt(index) as MaterialButton
                if(childAt.id == checkedId){
                    selectIndex = index
                    childAt.setTextColor(colorSelected)
                    childAt.iconTint = ColorStateList.valueOf(colorSelected)

                    val icon = when(index){
                        0 -> {
                            R.drawable.home_fill
                        }
                        1 -> {
                            R.drawable.like_fill
                        }
                        2 -> {
                            R.drawable.video_fill
                        }
                        3 -> {
                            R.drawable.user_fill
                        }
                        else -> { 0 }
                    }
                    childAt.setIconResource(icon)
                } else {
                    if (isDarken(this)) {
                        childAt.setTextColor(Color.WHITE)
                        childAt.iconTint = ColorStateList.valueOf(Color.WHITE)
                    } else {
                        childAt.setTextColor(Color.BLACK)
                        childAt.iconTint = ColorStateList.valueOf(Color.BLACK)
                    }

                    val icon = when(index){
                        0 -> {
                            R.drawable.home
                        }
                        1 -> {
                            R.drawable.like
                        }
                        2 -> {
                            R.drawable.video
                        }
                        3 -> {
                            R.drawable.user
                        }
                        else -> { 0 }
                    }
                    childAt.setIconResource(icon)
                }
            }

            switchFragment(selectIndex)
        }
        toggleButton.check(R.id.home_button)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val isConnected = activeNetworkInfo?.isConnectedOrConnecting == true

        if(!isConnected){
            Toast.makeText(this, "网络连接失败, 请检查您的网络和应用权限配置", Toast.LENGTH_SHORT).show()
        } else {
            val networkType = activeNetworkInfo?.type
            if (networkType == ConnectivityManager.TYPE_WIFI) {
                // 当前连接为 Wi-Fi
//                Toast.makeText(this, "当前为WIFI环境，可放心浏览本APP", Toast.LENGTH_SHORT).show()
            } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                // 当前连接为移动网络
//                Toast.makeText(this, "当前为流量环境，APP加载资源较多，请注意您的流量消耗", Toast.LENGTH_SHORT).show()
            }
            checkForUser()
        }

        // 打开启动图
        openLaunchLoading()
        // 检测消息推送权限
        notificationRequestPermission(view){
            // 启动推送服务
            startForegroundService(Intent(applicationContext, PushService::class.java))
        }
    }

    private lateinit var layout: View
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasNotificationProgression = if (isGranted) {
                val sharedPreferences = getSharedPreferences("notification", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putInt("allow", 1).apply()
                true
            } else {
                val sharedPreferences = getSharedPreferences("notification", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putInt("allow", 0).apply()
                false
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun notificationRequestPermission(view: View, callback: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已授予权限
                callback()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun openLaunchLoading(){
        val imageView = findViewById<ImageView>(R.id.loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView?.startAnimation(animation)
    }

    private fun switchFragment(selectIndex: Int){
        val fragment = when(selectIndex){
            0->{
                if(homeFragment == null){
                    homeFragment = HomeFragment()
                }
                homeFragment
            }
            1->{
                if(collectionFragment == null){
                    collectionFragment = CollectionFragment()
                }
                collectionFragment
            }
            2->{
                if(smartVideoFragment == null){
                    smartVideoFragment = SmartVideoFragment()
                }
                smartVideoFragment
            }
            3->{
                if(userFragment == null){
                    userFragment = UserFragment()
                }
                userFragment
            }
            else -> {
                throw IllegalStateException("下标不符合预期")
            }
        } ?: return

        val ft = supportFragmentManager.beginTransaction()
        if(!fragment.isAdded){
            ft.add(R.id.container, fragment)
        }
        ft.show(fragment)
        if(showFragment != null){
            ft.hide(showFragment!!)
        }
        showFragment = fragment
        ft.commitAllowingStateLoss()
    }

    private fun checkForUser() {
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        if(accessToken != null){
            userToken = accessToken
            verifyTokenAndGetUserInform(accessToken, this)
        }
    }
}