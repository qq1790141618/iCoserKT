package com.fixeam.icoser.ui.media_page

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityMediaViewBinding
import com.fixeam.icoser.databinding.ActivityMediaViewLandscapeBinding
import com.fixeam.icoser.databinding.MediaListBinding
import com.fixeam.icoser.databinding.MediaListItemBinding
import com.fixeam.icoser.model.CustomArrayAdapter
import com.fixeam.icoser.model.formatTime
import com.fixeam.icoser.model.getBestMedia
import com.fixeam.icoser.model.getScreenWidth
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.Media
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.requestMediaData
import com.fixeam.icoser.network.updateAccessLog

class MediaViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewBinding
    private lateinit var bindingLandscape: ActivityMediaViewLandscapeBinding
    private var player: ExoPlayer? = null
    private var mediaList: List<Media> = listOf()
    private var playIndex: Int = 0
    private var playRatio: Int = 720
    private var isPlaying: Boolean = false

    override fun onConfigurationChanged(newConfig: Configuration) {
        clearPlayer()
        super.onConfigurationChanged(newConfig)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        bindingLandscape = ActivityMediaViewLandscapeBinding.inflate(layoutInflater)
        setLandscapeMode(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        startOfAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        bindingLandscape = ActivityMediaViewLandscapeBinding.inflate(layoutInflater)
        setLandscapeMode(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        startOfAll()
    }

    private fun setLandscapeMode(status: Boolean = false){
        if(status){
            setContentView(bindingLandscape.root)

            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE

            val layoutParams = window.attributes
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = layoutParams
        } else {
            setContentView(binding.root)

            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = 0

            val layoutParams = window.attributes
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            window.attributes = layoutParams
        }
    }

    private fun startOfAll(){
        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)
        // 设置导航栏
        setToolBar()
        // 设置按钮
        setChangeRatio()
        // 获取默认清晰度
        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        playRatio = sharedPreferences.getInt("best_resolution_ratio", 720)
        // 获取数据
        val albumId = intent.getIntExtra("album-id", -1)
        val modelId = intent.getIntExtra("model-id", -1)
        val id = intent.getIntExtra("id", -1)
        requireMedia(modelId, albumId, id)
    }

    // 设置加载状态
    private fun setLoading(){
        val imageView = when(resources.configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> bindingLandscape.imageLoading
            else -> binding.imageLoading
        }
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE
    }
    // 初始化状态栏
    private fun setToolBar(){
        val toolbar: Toolbar = when(resources.configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> bindingLandscape.toolbar
            else -> binding.toolbar
        }
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            when(resources.configuration.orientation){
                Configuration.ORIENTATION_LANDSCAPE -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else -> { onBackPressed() }
            }
        }
        if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE){
            binding.fullscreen.setOnClickListener {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        if(isDarken(this) || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            toolbar.navigationIcon?.setTint(Color.WHITE)
        } else {
            toolbar.navigationIcon?.setTint(Color.BLACK)
        }
    }
    // 切换清晰度按钮绑定
    private fun setChangeRatio(){
        val changeRatio = when(resources.configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> bindingLandscape.changeRatio
            else -> binding.changeRatio
        }
        changeRatio.setOnClickListener { selectRatio() }
    }
    // 获取视频列表
    @SuppressLint("SetTextI18n")
    private fun requireMedia(modelId: Int = -1, albumId: Int = -1, id: Int = -1){
        setLoading()

        requestMediaData(this, modelId, albumId, id){
            if(it.isNotEmpty()){
                mediaList = it
                initPlayer()

                val changeVideo = when(resources.configuration.orientation){
                    Configuration.ORIENTATION_LANDSCAPE -> bindingLandscape.changeVideo
                    else -> binding.changeVideo
                }
                if(it.size == 1){
                    changeVideo.visibility = View.GONE
                } else {
                    if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE){
                        changeVideo.text = "切换视频 (${it.size})"
                    }
                    changeVideo.visibility = View.VISIBLE
                    changeVideo.setOnClickListener {
                        selectVideo()
                    }
                }
            } else {
                Toast.makeText(this, "请求结果为空", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 切换视频
    @OptIn(UnstableApi::class) private fun initPlayer(index: Int = 0, ratio: Int = 1080){
        if(mediaList.isEmpty() || index > mediaList.size - 1){
            return
        }
        // 获取视频信息
        val media = mediaList[index]
        playIndex = index
        // 记录访问日志
        var accessLogId = 0
        accessLog(this@MediaViewActivity, media.id.toString(), "VISIT_MEDIA"){
            accessLogId = it
        }
        // 获取视频播放组件
        val videoView = when(resources.configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> bindingLandscape.videoView
            else -> binding.videoView
        }
        // 释放播放器资源
        if(player != null){
            videoView.player?.pause()
            videoView.player = null
            player!!.release()
            player = null
        }
        // 启用软解码
        val renderersFactory = DefaultRenderersFactory(this@MediaViewActivity)
        renderersFactory.setEnableDecoderFallback(true)
        // 创建播放器实例
        val player = ExoPlayer.Builder(this@MediaViewActivity, renderersFactory).build()
        // 设置播放资源
        val bestMediaIndex = getBestMedia(media.format, ratio)
        val mediaItem = MediaItem.fromUri(media.format[bestMediaIndex].url)
        // 获取视频分辨率
        playRatio = media.format[bestMediaIndex].resolution_ratio.replace("p", "").toInt()
        // 获取存储共享器 从本地存储中读取上次播放的位置
        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        val lastPlayedPosition = sharedPreferences.getInt("last_played_position_${media.id}", 0)
        // 修改分辨率按钮文字
        if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE){
            binding.changeRatio.text = media.format[bestMediaIndex].resolution_ratio
        }
        // 播放视频
        player.setMediaItem(mediaItem)
        player.prepare()
        // 注册播放器
        videoView.player = player
        // 更新标题栏关闭加载动画
        val title = "${media.album_name} ${media.model_name} ${media.name}"
        when(resources.configuration.orientation){
            Configuration.ORIENTATION_LANDSCAPE -> {
                bindingLandscape.toolbar.title = title
                bindingLandscape.imageLoading.clearAnimation()
                bindingLandscape.imageLoading.visibility = View.GONE
            }
            else -> {
                binding.toolbar.title = title
                binding.imageLoading.clearAnimation()
                binding.imageLoading.visibility = View.GONE
            }
        }
        // 定时记录访问日志
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        val editor = sharedPreferences.edit()
        player.addListener(object: Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    player.seekTo(lastPlayedPosition.toLong())
                    if(isPlaying){
                        Handler(Looper.getMainLooper()).postDelayed({
                            player.play()
                        }, 100)
                    }
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    runnable = object : Runnable {
                        override fun run() {
                            val currentPosition = player.currentPosition
                            val duration = player.duration
                            val percent = currentPosition / duration.toFloat()

                            editor.putInt(
                                "last_played_position_${media.id}",
                                currentPosition.toInt()
                            ).apply()
                            updateAccessLog(accessLogId, (currentPosition / 1000).toInt())

                            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                val progressBar = bindingLandscape.progressBar
                                val currentWidth = progressBar.layoutParams.width
                                val animator = ValueAnimator.ofInt(
                                    currentWidth,
                                    (getScreenWidth(this@MediaViewActivity) * percent).toInt()
                                )
                                animator.addUpdateListener { animation ->
                                    val animatedValue = animation.animatedValue as Int
                                    progressBar.layoutParams.width = animatedValue
                                    progressBar.requestLayout()
                                }
                                animator.duration = 100
                                animator.start()
                            }

                            handler.postDelayed(this, 500)
                        }
                    }
                    handler.postDelayed(runnable!!, 500)
                } else {
                    handler.removeCallbacks(runnable!!)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if(mediaList.size == 1){
                        player.seekTo(0.toLong())
                    }
                    if(mediaList.size  > 1){
                        initPlayer(playIndex + 1)
                    }
                }
            }
        })
        // 隐藏工具栏
        var toolShow = false
        val runnable2: Runnable?
        val handler2 = Handler(Looper.getMainLooper())
        runnable2 = object: Runnable {
            override fun run() {
                if(videoView.isControllerFullyVisible && !toolShow){
                    bindingLandscape.toolbar.visibility = View.VISIBLE
                    bindingLandscape.tool.visibility = View.VISIBLE
                    bindingLandscape.progressBar.visibility = View.GONE
                    toolShow = true
                } else if(!videoView.isControllerFullyVisible && toolShow){
                    bindingLandscape.toolbar.visibility = View.GONE
                    bindingLandscape.tool.visibility = View.GONE
                    bindingLandscape.progressBar.visibility = View.VISIBLE
                    toolShow = false
                }
                handler2.postDelayed(this, 100)
            }
        }
        handler2.postDelayed(runnable2, 100)
        // 点击事件
        var clickTimes = 0
        videoView.setOnClickListener {
            if(clickTimes == 0){
                // 增加点击次数
                clickTimes++
                Handler(Looper.getMainLooper()).postDelayed({
                    clickTimes = 0
                }, 500)
            } else if(player.isPlaying){
                player.pause()
            } else {
                player.play()
            }
        }
    }
    // 选择视频
    @SuppressLint("SetTextI18n")
    private fun selectVideo(){
        if(mediaList.isEmpty()){
            return
        }

        val builder = AlertDialog.Builder(this)
        val binding = MediaListBinding.inflate(layoutInflater)

        builder.setView(binding.root)
        val alertDialog = builder.create()

        for ((index, media) in mediaList.withIndex()){
            val mediaListItem = MediaListItemBinding.inflate(layoutInflater)
            mediaListItem.root.setOnClickListener {
                initPlayer(index, playRatio)
                alertDialog.cancel()
            }
            Glide.with(this@MediaViewActivity)
                .load("${media.cover}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(mediaListItem.coverImage)
            mediaListItem.modelName.text = media.model_name
            mediaListItem.name.text = "${media.album_name} ${media.name}"
            mediaListItem.duration.text = formatTime((media.duration * 1000).toLong())
            binding.content.addView(mediaListItem.root)
        }

        binding.close.setOnClickListener {
            alertDialog.cancel()
        }
        alertDialog.show()
    }
    // 选择视频分辨率
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun selectRatio(){
        if(mediaList.isEmpty()){
            return
        }

        val builder = AlertDialog.Builder(this)
        val binding = MediaListBinding.inflate(layoutInflater)

        builder.setView(binding.root)
        val alertDialog = builder.create()

        val format = mediaList[playIndex].format
        binding.title.text = "选择视频分辨率"

        val list = ListView(this)
        list.dividerHeight = (resources.displayMetrics.density * 0.5).toInt()
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.density * 160).toInt()
        )
        val margin = (resources.displayMetrics.density * 10).toInt()
        layoutParams.leftMargin = margin
        layoutParams.rightMargin = margin
        layoutParams.topMargin = margin
        layoutParams.bottomMargin = margin

        list.layoutParams = layoutParams
        binding.content.addView(list)

        val adapter = CustomArrayAdapter(this, android.R.layout.simple_list_item_1, format.map {
            it.resolution_ratio }, 12f, 42, true)
        list.adapter = adapter
        adapter.setNotifyOnChange(true)
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            initPlayer(playIndex, format[position].resolution_ratio.replace("p", "").toInt())
            alertDialog.cancel()
        }

        binding.close.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.show()
    }
    // 释放播放器
    private fun clearPlayer(){
        binding.videoView.player?.pause()
        binding.videoView.player = null
        bindingLandscape.videoView.player?.pause()
        bindingLandscape.videoView.player = null
        player = null
    }
    // 释放播放器时机
    override fun onDestroy() {
        clearPlayer()
        super.onDestroy()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        clearPlayer()
        super.onBackPressed()
    }
    override fun onPause() {
        clearPlayer()
        super.onPause()
    }
    override fun onStop() {
        clearPlayer()
        super.onStop()
    }
}