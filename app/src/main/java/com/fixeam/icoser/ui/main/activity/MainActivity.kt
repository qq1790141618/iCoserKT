package com.fixeam.icoser.ui.main.activity

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.fixeam.icoser.ui.main.fragment.HomeFragment
import com.fixeam.icoser.R
import com.fixeam.icoser.ui.main.fragment.SmartVideoFragment
import com.fixeam.icoser.ui.main.fragment.UserFragment
import com.fixeam.icoser.model.closeOverCard
import com.fixeam.icoser.model.collectionFragment
import com.fixeam.icoser.model.createImageView
import com.fixeam.icoser.model.homeFragment
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.overCard
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.showFragment
import com.fixeam.icoser.model.smartVideoFragment
import com.fixeam.icoser.ui.main.fragment.CollectionFragment
import com.fixeam.icoser.model.userFragment
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.network.verifyTokenAndGetUserInform
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

var mainImagePreview: ConstraintLayout? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        val application = findViewById<ConstraintLayout>(R.id.application)
        mainImagePreview = createImageView(application, this)
        setOverlay()

        openLaunchLoading()
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

    private fun setOverlay() {
        val application = findViewById<ConstraintLayout>(R.id.application)
        overCard = layoutInflater.inflate(R.layout.overlay_card, application, false) as ConstraintLayout
        val card = overCard!!.findViewById<CardView>(R.id.overlay_card)

        // 等待布局测量完成后再设置卡片的初始位置
        overCard?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                overCard?.setBackgroundColor(Color.parseColor("#00000000"))
                overCard?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                card.translationY = overCard!!.height.toFloat()
            }
        })

        overCard!!.visibility = View.GONE

        // 注册遮罩层关闭
        overCard!!.setOnClickListener {
            closeOverCard()
        }
        // 防止卡片点击事件冒泡
        card.setOnClickListener {
            false
        }

        // 添加面板到应用
        application.addView(overCard)
    }
}