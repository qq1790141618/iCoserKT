package com.fixeam.icoserkt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.OutputStream


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
            setStatusBarColor(Color.BLACK)
            setStatusBarTextColor(false)
        } else {
            setStatusBarColor(Color.WHITE)
            setStatusBarTextColor(true)
        }

        var toggleButton = findViewById<MaterialButtonToggleGroup>(R.id.tab_bar)
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
                        0 -> { R.drawable.home_fill }
                        1 -> { R.drawable.video_fill }
                        2 -> { R.drawable.search }
                        3 -> { R.drawable.user_fill }
                        else -> { 0 }
                    }
                    childAt.setIconResource(icon)
                } else {
                    if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
                        childAt.setTextColor(Color.WHITE)
                        childAt.iconTint = ColorStateList.valueOf(Color.WHITE)
                    } else {
                        childAt.setTextColor(Color.BLACK)
                        childAt.iconTint = ColorStateList.valueOf(Color.BLACK)
                    }

                    val icon = when(index){
                        0 -> { R.drawable.home }
                        1 -> { R.drawable.video }
                        2 -> { R.drawable.search }
                        3 -> { R.drawable.user }
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
                Toast.makeText(this, "当前为WIFI环境，可放心浏览本APP", Toast.LENGTH_SHORT).show()
            } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                // 当前连接为移动网络
                Toast.makeText(this, "当前为流量环境，APP加载资源较多，请注意您的流量消耗", Toast.LENGTH_SHORT).show()
            }
            checkForUser()
        }

        createImageView()
        setOverlay()
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
                if(smartVideoFragment == null){
                    smartVideoFragment = SmartVideoFragment()
                }
                smartVideoFragment
            }
            2->{
                if(searchFragment == null){
                    searchFragment = SearchFragment()
                }
                searchFragment
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

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun setStatusBarTextColor(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            if (isDark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }

    private fun checkForUser() {
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        if(accessToken != null){
            userToken = accessToken
            verifyTokenAndGetUserInform(accessToken, this)
        }
    }

    fun createImageView(){
        val application = findViewById<ConstraintLayout>(R.id.application)
        if(imagePreview == null) {
            imagePreview = layoutInflater.inflate(R.layout.image_preview, application, false) as ConstraintLayout
            val imageViewPrev = imagePreview?.findViewById<AppCompatImageView>(R.id.image_view_prev)

            imagePreview?.setOnClickListener {
                imagePreview?.visibility = View.GONE

                imageViewPrev?.rotation = 0f
                imageViewPrev?.scaleX = 1f
                imageViewPrev?.scaleY = 1f
            }

            val downloadButton = imagePreview?.findViewById<MaterialButton>(R.id.download)
            downloadButton?.setOnClickListener {
                imageViewPrev?.let { it1 -> saveImageToGallery(this@MainActivity, it1) }
            }

            val scaleUpButton = imagePreview?.findViewById<MaterialButton>(R.id.scale_up)
            scaleUpButton?.setOnClickListener {
                val currentScale = imageViewPrev?.scaleX ?: 1f
                val newScale = currentScale + 0.4f

                val clampedScale = newScale.coerceIn(0.21f, 2.99f)
                if (clampedScale < currentScale) {
                    Toast.makeText(this, "已经达到最大缩放比例", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val animator = ValueAnimator.ofFloat(currentScale, clampedScale)
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    imageViewPrev?.scaleX = value
                    imageViewPrev?.scaleY = value
                }
                animator.start()
            }

            val scaleDownButton = imagePreview?.findViewById<MaterialButton>(R.id.scale_down)
            scaleDownButton?.setOnClickListener {
                scaleDownButton.isEnabled = false
                val currentScale = imageViewPrev?.scaleX ?: 1f
                val newScale = currentScale - 0.4f

                val clampedScale = newScale.coerceIn(0.21f, 2.99f)
                if (clampedScale > currentScale) {
                    Toast.makeText(this, "已经达到最小缩放比例", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val animator = ValueAnimator.ofFloat(currentScale, clampedScale)
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    imageViewPrev?.scaleX = value
                    imageViewPrev?.scaleY = value
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 在动画完成时重新启用按钮
                        scaleDownButton.isEnabled = true
                    }
                })
                animator.start()
            }

            val scaleResetButton = imagePreview?.findViewById<MaterialButton>(R.id.scale_reset)
            scaleResetButton?.setOnClickListener {
                scaleResetButton.isEnabled = false
                val currentScale = imageViewPrev?.scaleX ?: 1f
                val newScale = 1f

                val animator = ValueAnimator.ofFloat(currentScale, newScale)
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    imageViewPrev?.scaleX = value
                    imageViewPrev?.scaleY = value
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 在动画完成时重新启用按钮
                        scaleResetButton.isEnabled = true
                    }
                })
                animator.start()
            }

            val rotateLeftButton = imagePreview?.findViewById<MaterialButton>(R.id.rotate_left)
            rotateLeftButton?.setOnClickListener {
                // 禁用按钮
                rotateLeftButton.isEnabled = false

                // 向左旋转 imageViewPrev 90 度
                val currentRotation = imageViewPrev?.rotation ?: 0f
                val newRotation = currentRotation - 90f

                val animator = ObjectAnimator.ofFloat(imageViewPrev, "rotation", currentRotation, newRotation)
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 在动画完成时重新启用按钮
                        rotateLeftButton.isEnabled = true
                    }
                })
                animator.start()
            }

            imagePreview?.visibility = View.GONE
        }

        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.topToBottom = R.id.tab_bar_card
        imagePreview?.layoutParams = layoutParams

        application.addView(imagePreview)
    }

    // 保存图片到相册
    fun saveImageToGallery(context: Context, imageView: ImageView) {
        val drawable = imageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            val savedUri = saveBitmapToGallery(context, bitmap)
            if (savedUri != null) {
                // 发送媒体扫描广播，通知相册更新
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = savedUri
                context.sendBroadcast(mediaScanIntent)
                // 提示保存成功
                Toast.makeText(this@MainActivity, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            } else {
                // 提示保存失败
                Toast.makeText(this@MainActivity, "图片保存失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 提示无法获取图片
            Toast.makeText(this@MainActivity, "无法获取图片", Toast.LENGTH_SHORT).show()
        }
    }

    // 保存 Bitmap 到相册
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val displayName = "${System.currentTimeMillis()}.png"

        val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        val resolver = context.contentResolver
        var stream: OutputStream? = null
        var uri: Uri? = null

        try {
            // 插入图片
            uri = resolver.insert(imageCollection, contentValues)
            if (uri == null) {
                throw Exception("Failed to create new MediaStore record.")
            }

            // 写入数据
            stream = resolver.openOutputStream(uri)
            if (stream == null) {
                throw Exception("Failed to get output stream.")
            }
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw Exception("Failed to save bitmap.")
            }
        } catch (e: Exception) {
            uri = null
            e.printStackTrace()
        } finally {
            stream?.close()
        }

        return uri
    }

    private fun setOverlay(){
        val application = findViewById<ConstraintLayout>(R.id.application)
        overCard = layoutInflater.inflate(R.layout.overlay_card, application, false) as ConstraintLayout
        val card = overCard!!.findViewById<CardView>(R.id.overlay_card)

        // 初始化面板位置
        overCard?.setBackgroundColor(Color.parseColor("#00000000"))
        card.translationY = overCard!!.height.toFloat()
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