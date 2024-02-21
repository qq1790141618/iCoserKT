package com.fixeam.icoserkt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.OutputStream

// 界面变量
var homeFragment: HomeFragment? = null
var smartVideoFragment: SmartVideoFragment? = null
var searchFragment: SearchFragment? = null
var userFragment:   UserFragment? = null
var showFragment: Fragment? = null
var overCard: ConstraintLayout? = null

// 移除共享变量
fun removeSharedPreferencesKey(key: String, context: Context){
    val sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(key)
    editor.apply()
}

// 关闭MainActivity遮罩层卡片
fun closeOverCard(){
    val card = overCard!!.findViewById<CardView>(R.id.overlay_card)

    val slideAnimation = ObjectAnimator.ofFloat(card, "translationY", 0f, overCard!!.height.toFloat())
    slideAnimation.duration = 500
    slideAnimation.start()

    val startColor = Color.parseColor("#AE000000")
    val endColor = Color.parseColor("#00000000")

    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
    colorAnimation.duration = 500
    colorAnimation.addUpdateListener { animator ->
        val color = animator.animatedValue as Int
        overCard?.setBackgroundColor(color)
    }

    colorAnimation.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            overCard?.visibility = View.GONE
        }
    })

    colorAnimation.start()
}

// 展示MainActivity遮罩层卡片
fun openOverCard(){
    val card = overCard!!.findViewById<CardView>(R.id.overlay_card)
    val slideAnimation = ObjectAnimator.ofFloat(card, "translationY", overCard!!.height.toFloat(), 0f)
    slideAnimation.duration = 500
    slideAnimation.start()

    val startColor = Color.parseColor("#AE000000")
    val endColor = Color.parseColor("#00000000")

    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), endColor, startColor)
    colorAnimation.duration = 500
    colorAnimation.addUpdateListener { animator ->
        val color = animator.animatedValue as Int
        overCard?.setBackgroundColor(color)
    }

    colorAnimation.start()
}

// 分享文字
fun shareTextContent(text: String, title: String = "来自iCoser的分享", context: Context) {
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(shareIntent, title))
}

// 分享图片调用
fun shareImageContent(imageUrl: String, title: String = "来自iCoser的分享", context: Context) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // 创建下载请求
    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl.substringAfterLast("/"))

    // 将下载请求加入下载队列
    val downloadId = downloadManager.enqueue(request)

    // 注册下载完成的广播接收器
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadIdCompleted = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                if (downloadIdCompleted == downloadId) {
                    val downloadedUri = downloadManager.getUriForDownloadedFile(downloadIdCompleted)
                    context?.let { shareImageUri(downloadedUri, title, it) }
                }
            }
        }
    }
    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    context.registerReceiver(receiver, filter)
}

// 分享图片
fun shareImageUri(imageUri: Uri?, title: String = "来自iCoser的分享", context: Context) {
    imageUri?.let {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}

// 获取屏幕宽度
fun getScreenWidth(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

// 发送系统通知
fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {
    val CHANNEL_ID = "icoser_channel_01"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "iCoser"
        val descriptionText = "iCoser写真分享"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(notificationId, notification)
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
            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
        } else {
            // 提示保存失败
            Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
        }
    } else {
        // 提示无法获取图片
        Toast.makeText(context, "无法获取图片", Toast.LENGTH_SHORT).show()
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

// 设置状态栏主题
fun setStatusBarColor(activity: Activity, color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activity.window.statusBarColor = color
    }
}

// 设置状态栏文字颜色
fun setStatusBarTextColor(activity: Activity, isDark: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val decorView = activity.window.decorView
        var flags = decorView.systemUiVisibility
        if (isDark) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        decorView.systemUiVisibility = flags
    }
}

// 自动设置状态栏
fun setStatusBar(activity: Activity, lightColor: Int, darkColor: Int){
    val currentTheme = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
        setStatusBarColor(activity, darkColor)
        setStatusBarTextColor(activity, false)
    } else {
        setStatusBarColor(activity, lightColor)
        setStatusBarTextColor(activity, true)
    }
}

// 获取是否为深色主题
fun isDarken(activity: Activity): Boolean{
    val currentTheme = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentTheme == Configuration.UI_MODE_NIGHT_YES
}

// 设置收藏
fun setAlbumCollection(context: Context, album: Albums, fold: String, callback: () -> Unit, unlog: () -> Unit){
    if(userToken != null){
        var call = ApiNetService.SetCollectionItem(userToken!!, album.id, "album", fold)
        if(album.is_collection != null){
            call = ApiNetService.RemoveCollectionItem(userToken!!, album.id, "album")
        }

        call.enqueue(object : Callback<ActionResponse> {
            override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if(!responseBody?.result!!){
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        return
                    }

                    callback()
                    Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    } else {
        unlog()
    }
}

// 创建图片预览框
fun createImageView(application: ConstraintLayout, activity: Activity): ConstraintLayout {
    val imagePreview: ConstraintLayout = activity.layoutInflater.inflate(R.layout.image_preview, application, false) as ConstraintLayout
    val imageViewPrev = imagePreview.findViewById<AppCompatImageView>(R.id.image_view_prev)

    imagePreview.setOnClickListener {
        imagePreview.visibility = View.GONE

        imageViewPrev?.rotation = 0f
        imageViewPrev?.scaleX = 1f
        imageViewPrev?.scaleY = 1f
    }

    val downloadButton = imagePreview.findViewById<MaterialButton>(R.id.download)
    downloadButton?.setOnClickListener {
        imageViewPrev?.let { it1 -> saveImageToGallery(activity, it1) }
    }

    val scaleUpButton = imagePreview.findViewById<MaterialButton>(R.id.scale_up)
    scaleUpButton?.setOnClickListener {
        val currentScale = imageViewPrev?.scaleX ?: 1f
        val newScale = currentScale + 0.4f

        val clampedScale = newScale.coerceIn(0.21f, 2.99f)
        if (clampedScale < currentScale) {
            Toast.makeText(activity, "已经达到最大缩放比例", Toast.LENGTH_SHORT).show()
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

    val scaleDownButton = imagePreview.findViewById<MaterialButton>(R.id.scale_down)
    scaleDownButton?.setOnClickListener {
        scaleDownButton.isEnabled = false
        val currentScale = imageViewPrev?.scaleX ?: 1f
        val newScale = currentScale - 0.4f

        val clampedScale = newScale.coerceIn(0.21f, 2.99f)
        if (clampedScale > currentScale) {
            Toast.makeText(activity, "已经达到最小缩放比例", Toast.LENGTH_SHORT).show()
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

    val scaleResetButton = imagePreview.findViewById<MaterialButton>(R.id.scale_reset)
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

    val rotateLeftButton = imagePreview.findViewById<MaterialButton>(R.id.rotate_left)
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

    imagePreview.visibility = View.GONE


    val layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT,
        ConstraintLayout.LayoutParams.MATCH_PARENT
    )
    layoutParams.topToBottom = R.id.tab_bar_card
    imagePreview.layoutParams = layoutParams

    application.addView(imagePreview)
    return imagePreview
}

// 打开图片预览框
fun imageViewInstantiate(url: String, context: Context, imagePreview: ConstraintLayout){
    val imageViewPrev = imagePreview.findViewById<AppCompatImageView>(R.id.image_view_prev)
    Glide.with(context)
        .load(url)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(imageViewPrev)
    imagePreview.visibility = View.VISIBLE
}