package com.fixeam.icoser.ui.main.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
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
import com.fixeam.icoser.network.checkForUser
import com.fixeam.icoser.ui.main.fragment.CollectionFragment
import com.fixeam.icoser.ui.main.fragment.HomeFragment
import com.fixeam.icoser.ui.main.fragment.SmartVideoFragment
import com.fixeam.icoser.ui.main.fragment.UserFragment
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)
        // 设置页面切换
        setTab()
        // 检查网络状态和用户登录
        checkForUser(this)
        // 打开启动图
        openLaunchLoading()
        // 检测消息推送权限
        notificationRequestPermission(){
            // 启动推送服务
            startForegroundService(Intent(applicationContext, PushService::class.java))
        }
    }

    private fun openLaunchLoading(){
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        binding.launchImage.loading.startAnimation(animation)
    }

    private fun setTab(){
        binding.tabBar.addOnButtonCheckedListener{group, checkedId, isChecked ->
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
        binding.tabBar.check(R.id.home_button)
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
    fun notificationRequestPermission(callback: () -> Unit) {
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

    private var pressBack: Boolean = false
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(!pressBack){
            pressBack = true
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({
                pressBack = false
            }, 1500)
        } else {
            finish()
        }
    }
}