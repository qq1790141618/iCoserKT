package com.fixeam.icoserkt

import GlideBlurTransformation
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SmartVideoFragment : Fragment() {
    private var mediaList: MutableList<Media> = mutableListOf()
    private var playIndex: Int = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_smart_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createPlayer()
        requestMediaData(createViewPager = true)
    }

    private fun requestMediaData(insert: Boolean = false, createViewPager: Boolean = false){
        var call = ApiNetService.GetMedia(number = 20)
        if(userToken != null){
            call = ApiNetService.GetMedia(userToken!!, 20)
        }

        call.enqueue(object : Callback<MediaResponse> {
            override fun onResponse(call: Call<MediaResponse>, response: Response<MediaResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.result) {
                        val medias = responseBody.data

                        if(insert){
                            playIndex += medias.size
                            mediaList.addAll(0, medias)
                        } else {
                            mediaList.addAll(medias)
                        }

                        if(createViewPager){
                            createViewPager()
                        } else {
                            refreshViewPager(medias.size, insert)
                        }
                    }
                } else {
                    // 处理错误情况
                    Toast.makeText(requireContext(), "轮播图数据加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MediaResponse>, t: Throwable) {
                // 处理网络请求失败的情况
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createViewPager(){
        val viewPager: ViewPager2? = view?.findViewById(R.id.view_pager)
        viewPager?.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        val adapter = MyPagerAdapter()
        viewPager?.adapter = adapter

        viewPager?.offscreenPageLimit = 1
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val adapter = viewPager.adapter

                if (adapter != null && playIndex != position) {
                    // 暂停之前页面的视频播放
                    val lastViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
                    if (lastViewHolder is MyViewHolder) {
                        lastViewHolder.itemView.findViewById<PlayerView>(R.id.video_view).player?.pause()
                    }

                    // 播放当前页面的视频
                    val displayViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(position)
                        ?: adapter.createViewHolder(viewPager, adapter.getItemViewType(position))
                    adapter.bindViewHolder(displayViewHolder, position)
                    displayViewHolder.itemView.findViewById<PlayerView>(R.id.video_view).player?.play()

                    // 更新播放位置
                    playIndex = position
                }

                if(position <= 3){
                    requestMediaData(true, false)
                } else if(mediaList.size - position <= 3){
                    requestMediaData(false, false)
                }
            }
        })

        viewPager?.setCurrentItem(playIndex, false)
    }

    private var lastIsPlaying = false

    private fun leaveFragment(){
        val viewPager: ViewPager2? = view?.findViewById(R.id.view_pager)
        val lastViewHolder = (viewPager?.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
        if (lastViewHolder is MyViewHolder) {
            val player = lastViewHolder.itemView.findViewById<PlayerView>(R.id.video_view).player

            if (player != null && player.isPlaying) {
                lastIsPlaying = true
                player.pause()
            }
        }
    }

    private fun enterFragment(){
        if(lastIsPlaying){
            val viewPager: ViewPager2? = view?.findViewById(R.id.view_pager)
            val lastViewHolder = (viewPager?.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
            if (lastViewHolder is MyViewHolder) {
                val player = lastViewHolder.itemView.findViewById<PlayerView>(R.id.video_view).player

                if (player != null) {
                    lastIsPlaying = false
                    player.play()
                }
            }
        }
    }

    override fun onStop() {
        leaveFragment()
        super.onStop()
    }

    override fun onPause() {
        leaveFragment()
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            leaveFragment()
        } else {
            enterFragment()
        }
        super.onHiddenChanged(hidden)
    }

    override fun onResume() {
        enterFragment()
        super.onResume()
    }

    private fun refreshViewPager(number: Int, insert: Boolean = false) {
        val viewPager: ViewPager2? = view?.findViewById(R.id.view_pager)
        val adapter = viewPager?.adapter
        if (adapter != null) {
            val currentItemCount = adapter.itemCount
            val currentPosition = viewPager.currentItem  // 记录当前位置
            if (insert) {
                adapter.notifyDataSetChanged()
                viewPager.post {
                    viewPager.setCurrentItem(currentPosition + number, false)
                }
            } else {
                adapter.notifyItemRangeInserted(currentItemCount, number)
            }
        }
    }

    private var playerList: MutableList<SimpleExoPlayer> = mutableListOf()

    private fun createPlayer(){
        // 启用软解码
        val renderersFactory = DefaultRenderersFactory(requireContext())
        renderersFactory.setEnableDecoderFallback(true)
        // 创建播放器实例
        playerList.clear()
        for(index in 0..2){
            val player = SimpleExoPlayer.Builder(requireContext(), renderersFactory).build()
            playerList.add(player)
        }
    }

    inner class MyPagerAdapter : RecyclerView.Adapter<MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.smart_video_item, parent, false)
            return MyViewHolder(view)
        }

        override fun getItemCount(): Int {
            return mediaList.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val media = mediaList[position]

            // 创建背景图
            val blurBackground = holder.itemView.findViewById<ImageView>(R.id.blur_background)
            Glide.with(requireContext())
                .load("${media.cover}/short1200px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(requireContext())))
                .into(blurBackground)

            // 获取播放器实例
            val videoView = holder.itemView.findViewById<PlayerView>(R.id.video_view)
            videoView.useController = false

            // 获取播放器资源定位
            var player = playerList[playIndex % 3]

            // 释放播放器资源
            player.release()

            // 获取存储共享器
            val sharedPreferences = requireContext().getSharedPreferences("video_progress", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // 启用软解码
            val renderersFactory = DefaultRenderersFactory(requireContext())
            renderersFactory.setEnableDecoderFallback(true)

            // 创建播放器实例
            player = SimpleExoPlayer.Builder(requireContext(), renderersFactory).build()

            // 获取最佳视频分辨率并设置到资源
            val bestMediaIndex = getBestMedia(media.format, 720)
            val mediaItem = MediaItem.fromUri(media.format[bestMediaIndex].url)
            player.setMediaItem(mediaItem)
            player.prepare()
            videoView.player = player

            // 从本地存储中读取上次播放的位置
            val lastPlayedPosition = sharedPreferences.getInt("last_played_position_${media.id}", 0)
            player.seekTo(lastPlayedPosition.toLong())

            // 点击暂停/播放
            videoView.setOnClickListener {
                if(player.isPlaying){
                    player.pause()
                } else {
                    player.play()
                }
            }

            // 更新文本
            val mediaName: TextView = holder.itemView.findViewById(R.id.media_name)
            mediaName.text = media.name
            val mediaDescription: TextView = holder.itemView.findViewById(R.id.media_description)
            mediaDescription.text = media.album_name
            if(media.description != null){
                mediaDescription.text = "${media.album_name} ${media.description}"
            }

            // 更新按钮
            val mediaAvatar: ImageView = holder.itemView.findViewById(R.id.model_avatar)
            Glide.with(requireContext())
                .load("${media.model_avatar_image}/yswidth300px")
                .into(mediaAvatar)
            mediaAvatar.setOnClickListener {
                val intent = Intent(requireContext(), ModelViewActivity::class.java)
                intent.putExtra("id", media.bind_model_id)
                startActivity(intent)
            }

            val shareButton: FloatingActionButton = holder.itemView.findViewById(R.id.share)
            shareButton.setOnClickListener {
                player.pause()
                shareTextContent(
                    context = requireContext(),
                    text = "来自iCoser的分享内容：模特 - ${media.bind_model_id}, 写真集 - ${media.bind_album_id}, 视频 - ${media.name}, 访问链接：https://app.fixeam.com/media?video-id=${media.id}"
                )
            }
            val albumButton: FloatingActionButton = holder.itemView.findViewById(R.id.album)
            albumButton.setOnClickListener {
                val intent = Intent(requireContext(), AlbumViewActivity::class.java)
                intent.putExtra("id", media.bind_album_id)
                startActivity(intent)
            }

            val playButton = holder.itemView.findViewById<ImageView>(R.id.play_button)
            playButton.visibility = View.GONE
            val loading = holder.itemView.findViewById<ImageView>(R.id.loading)
            val progressBar: LinearLayout = holder.itemView.findViewById(R.id.progress_bar)
            val progressBarContainer: LinearLayout = holder.itemView.findViewById(R.id.progress_bar_containter)
            val progressText: TextView = holder.itemView.findViewById(R.id.progress_text)

            // 进度条触摸
            progressBarContainer.setOnTouchListener(object : View.OnTouchListener {
                private var startX = 0f // 记录触摸事件开始时的x坐标

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // 用户开始触摸屏幕
                            val currentHeight = progressBar.layoutParams.height
                            val animator = ValueAnimator.ofInt(currentHeight, (resources.displayMetrics.density * 8).toInt())
                            animator.addUpdateListener { animation ->
                                val animatedValue = animation.animatedValue as Int
                                progressBar.layoutParams.height = animatedValue
                                progressBar.requestLayout()
                            }
                            animator.duration = 100 // 设置动画持续时间，单位为毫秒
                            animator.start()

                            startX = event.x
                            return true // 返回true表示该事件已被处理
                        }
                        MotionEvent.ACTION_MOVE -> {
                            // 用户正在滑动屏幕
                            val diffX = event.x - startX
                            // 在这里处理滑动事件，比如更新进度条的进度
                            val duration = player.duration

                            val percent = event.x / getScreenWidth(requireContext())
                            progressBar.layoutParams = progressBar.layoutParams.apply {
                                width = (getScreenWidth(requireContext()) * percent).toInt()
                            }

                            val currentPosition = duration * percent
                            player.seekTo(currentPosition.toLong())

                            progressText.text = "${formatTime(currentPosition.toLong())} / ${formatTime(duration)}"

                            // 更新 startX 值，以便下次重新计算偏移量
                            startX = event.x
                            return true // 返回true表示该事件已被处理
                        }
                        MotionEvent.ACTION_UP -> {
                            // 用户结束触摸屏幕
                            val currentHeight = progressBar.layoutParams.height
                            val animator = ValueAnimator.ofInt(currentHeight, (resources.displayMetrics.density * 3).toInt())
                            animator.addUpdateListener { animation ->
                                val animatedValue = animation.animatedValue as Int
                                progressBar.layoutParams.height = animatedValue
                                progressBar.requestLayout()
                            }
                            animator.duration = 100 // 设置动画持续时间，单位为毫秒
                            animator.start()

                            return true // 返回true表示该事件已被处理
                        }
                    }
                    return false // 返回false表示该事件未被处理
                }
            })

            // 定时器
            val handler = Handler()
            var runnable: Runnable? = null

            player.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) {
                        playButton.visibility = View.GONE
                    } else {
                        playButton.visibility = View.VISIBLE
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        playButton.visibility = View.VISIBLE
                        progressBar.layoutParams = progressBar.layoutParams.apply {
                            width = 0
                        }
                        progressText.text = "00:00 / 00:00"
                        player.seekTo(0.toLong())
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        playButton.visibility = View.GONE
                        loading.clearAnimation()
                        loading.visibility = View.GONE

                        runnable = object : Runnable {
                            override fun run() {
                                val currentPosition = player.currentPosition
                                val duration = player.duration
                                val percent = currentPosition.toFloat() / duration.toFloat()

                                // 记录播放位置
                                editor.putInt("last_played_position_${media.id}",
                                    currentPosition.toInt()
                                )
                                editor.apply()

                                // 更新进度条
                                val currentWidth = progressBar.layoutParams.width
                                val animator = ValueAnimator.ofInt(currentWidth, (getScreenWidth(requireContext()) * percent).toInt())
                                animator.addUpdateListener { animation ->
                                    val animatedValue = animation.animatedValue as Int
                                    progressBar.layoutParams.width = animatedValue
                                    progressBar.requestLayout()
                                }
                                animator.duration = 100 // 设置动画持续时间，单位为毫秒
                                animator.start()

                                progressText.text = "${formatTime(currentPosition)} / ${formatTime(duration)}"

                                handler.postDelayed(this, 500) // 延迟1秒钟
                            }
                        }

                        handler.postDelayed(runnable!!, 500)
                    } else {
                        playButton.visibility = View.VISIBLE

                        handler.removeCallbacks(runnable!!)
                    }
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    if(isLoading && !player.isPlaying){
                        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
                        loading.startAnimation(animation)
                        loading.visibility = View.VISIBLE
                    } else {
                        loading.clearAnimation()
                        loading.visibility = View.GONE

                        if(position == 5){
                            player.play()
                        }
                    }
                    super.onIsLoadingChanged(isLoading)
                }
            })

        }

        override fun onViewRecycled(holder: MyViewHolder) {
            // 释放ExoPlayer资源
            val videoView = holder.itemView.findViewById<PlayerView>(R.id.video_view)
            videoView.player?.release()

            super.onViewRecycled(holder)
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}