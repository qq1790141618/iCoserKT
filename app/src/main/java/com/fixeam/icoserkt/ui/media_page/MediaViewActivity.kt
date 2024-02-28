package com.fixeam.icoserkt.ui.media_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.model.formatTime
import com.fixeam.icoserkt.model.getBestMedia
import com.fixeam.icoserkt.model.setStatusBar
import com.fixeam.icoserkt.network.ApiNetService
import com.fixeam.icoserkt.network.Media
import com.fixeam.icoserkt.network.MediaResponse
import com.fixeam.icoserkt.network.accessLog
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MediaViewActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null
    private var mediaList: List<Media> = listOf()
    private var playIndex: Int = 0
    private var playRatio: Int = 1080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_view)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val albumId = intent.getIntExtra("album-id", -1)
        val modelId = intent.getIntExtra("model-id", -1)
        val id = intent.getIntExtra("id", -1)
        requireMedia(modelId, albumId, id)

        // 设置按钮
        val changeRatio = findViewById<MaterialButton>(R.id.change_ratio)
        changeRatio.setOnClickListener {
            selectRatio()
        }
    }

    override fun onPause() {
        clearPlayer()
        super.onPause()
    }

    override fun onStop() {
        clearPlayer()
        super.onStop()
    }

    private fun clearPlayer(){
        val videoView = findViewById<StyledPlayerView>(R.id.video_view)
        videoView.player?.pause()
        videoView.player = null
        player = null
    }

    private fun requireMedia(modelId: Int = -1, albumId: Int = -1, id: Int = -1){
        if(id <= 0 && albumId <= 0 && modelId <= 0){
            return
        }

        var call = ApiNetService.GetMedia(id = id.toString(), number = 9999)
        if(albumId > 0){
            call = ApiNetService.GetMedia(album_id = albumId.toString(), number = 9999)
        }
        if(modelId > 0){
            call = ApiNetService.GetMedia(model_id = modelId.toString(), number = 9999)
        }

        call.enqueue(object : Callback<MediaResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<MediaResponse>, response: Response<MediaResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.result) {
                        val medias = responseBody.data
                        mediaList = medias
                        initPlayer()

                        val changeVideo = findViewById<MaterialButton>(R.id.change_video)
                        if(medias.size == 1){
                            changeVideo.visibility = View.GONE
                        } else {
                            changeVideo.text = "切换视频 (${medias.size})"
                            changeVideo.visibility = View.VISIBLE
                            changeVideo.setOnClickListener {
                                selectVideo()
                            }
                        }
                    }
                } else {
                    // 处理错误情况
                    Toast.makeText(this@MediaViewActivity, "轮播图数据加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MediaResponse>, t: Throwable) {
                // 处理网络请求失败的情况
                Toast.makeText(this@MediaViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initPlayer(index: Int = 0, ratio: Int = 1080){
        if(mediaList.size == 0 || index > mediaList.size - 1){
            return
        }
        val media = mediaList[index]
        playIndex = index
        var accessLogId = 0
        accessLog(this@MediaViewActivity, media.id.toString(), "VISIT_MEDIA"){
            accessLogId = it
        }

        // 获取视频播放组件
        val videoView = findViewById<StyledPlayerView>(R.id.video_view)

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
        val player = SimpleExoPlayer.Builder(this@MediaViewActivity, renderersFactory).build()

        // 设置播放资源
        val bestMediaIndex = getBestMedia(media.format, ratio)
        val mediaItem = MediaItem.fromUri(media.format[bestMediaIndex].url)

        playRatio = media.format[bestMediaIndex].resolution_ratio.replace("p", "").toInt()
        val changeRatio = findViewById<MaterialButton>(R.id.change_ratio)
        changeRatio.text = media.format[bestMediaIndex].resolution_ratio

        player.setMediaItem(mediaItem)
        player.prepare()

        // 注册播放器

        videoView.player = player

        // 更新标题栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "${media.album_name} ${media.model_name} ${media.name}"

        // 关闭加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        imageView.clearAnimation()
        imageView.visibility = View.GONE
    }

    private fun selectVideo(){
        if(mediaList.size == 0){
            return
        }

        val builder = AlertDialog.Builder(this)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView: View = inflater.inflate(R.layout.media_list, null)

        builder.setView(dialogView)
        val alertDialog = builder.create()
        val linearLayout = dialogView.findViewById<LinearLayout>(R.id.content)

        for ((index, media) in mediaList.withIndex()){
            val mediaListItemView = inflater.inflate(R.layout.media_list_item, null)
            mediaListItemView.setOnClickListener {
                initPlayer(index, playRatio)
                alertDialog.cancel()
            }

            val coverImage = mediaListItemView.findViewById<ImageView>(R.id.cover_image)
            Glide.with(this@MediaViewActivity)
                .load("${media.cover}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(coverImage)

            val modeName = mediaListItemView.findViewById<TextView>(R.id.model_name)
            modeName.text = media.model_name

            val name = mediaListItemView.findViewById<TextView>(R.id.name)
            name.text = "${media.album_name} ${media.name}"

            val duration = mediaListItemView.findViewById<TextView>(R.id.duration)
            duration.text = formatTime((media.duration * 1000).toLong())

            linearLayout.addView(mediaListItemView)
        }

        val close = dialogView.findViewById<MaterialButton>(R.id.close)
        close.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.show()
    }

    private fun selectRatio(){
        if(mediaList.size == 0){
            return
        }

        val builder = AlertDialog.Builder(this)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView: View = inflater.inflate(R.layout.media_list, null)

        builder.setView(dialogView)
        val alertDialog = builder.create()
        val linearLayout = dialogView.findViewById<LinearLayout>(R.id.content)

        val format = mediaList[playIndex].format

        for (formatItem in format){
            val formatItemView = MaterialButton(this)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val marginInPx = 35
            layoutParams.setMargins(marginInPx, 0, marginInPx, 0)
            formatItemView.layoutParams = layoutParams

            formatItemView.text = formatItem.resolution_ratio
            formatItemView.setOnClickListener {
                playRatio = formatItem.resolution_ratio.replace("p", "").toInt()
                initPlayer(playIndex, playRatio)
                alertDialog.cancel()
            }

            linearLayout.addView(formatItemView)
        }

        val close = dialogView.findViewById<MaterialButton>(R.id.close)
        close.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}