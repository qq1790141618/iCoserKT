package com.fixeam.icoser.ui.main.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.FragmentSmartVideoBinding
import com.fixeam.icoser.databinding.SmartVideoItemBinding
import com.fixeam.icoser.model.formatTime
import com.fixeam.icoser.model.getBestMedia
import com.fixeam.icoser.model.getScreenWidth
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.shareTextContent
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.Media
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.appreciate
import com.fixeam.icoser.network.appreciateCancel
import com.fixeam.icoser.network.requestMediaData
import com.fixeam.icoser.network.setMediaCollection
import com.fixeam.icoser.network.updateAccessLog
import com.fixeam.icoser.network.userMediaLike
import com.fixeam.icoser.painter.GlideBlurTransformation

class SmartVideoFragment : Fragment() {
    private var mediaList: MutableList<Media> = mutableListOf()
    private var playIndex: Int = 5
    private lateinit var binding: FragmentSmartVideoBinding
    private var sharedPreferences: SharedPreferences? = null
    private var isMyFavor = false
    private var startFrom = 0
    private var id = -1
    private var albumId = -1
    private var modelId = -1
    private var getMoreAllow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(arguments != null){
            isMyFavor = requireArguments().getBoolean("is-my-favor", false)
            id = requireArguments().getInt("id", -1)
            albumId = requireArguments().getInt("album-id", -1)
            modelId = requireArguments().getInt("model-id", -1)
            startFrom = requireArguments().getInt("start-from", 0)
            if(isMyFavor || id > 1 || albumId > 1 || modelId > 1){
                getMoreAllow = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmartVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        requestMedia(createViewPager = true)
    }

    private fun requestMedia(insert: Boolean = false, createViewPager: Boolean = false){
        var number = 20
        if(isMyFavor || id > 1 || albumId > 1 || modelId > 1){
            number = 9999
        }
        if(!isMyFavor){
            requestMediaData(requireContext(), modelId, albumId, id, number){
                if(insert){
                    playIndex += it.size
                    mediaList.addAll(0, it)
                } else {
                    mediaList.addAll(it)
                }
                if(createViewPager){
                    createViewPager()
                } else {
                    refreshViewPager(it.size, insert)
                }
            }
        } else {
            mediaList.clear()
            for (mediaFavor in userMediaLike){
                mediaList.add(mediaFavor.content)
            }
            createViewPager()
        }
    }

    private var adapter = MyPagerAdapter()
    private fun createViewPager(){
        val viewPager: ViewPager2 = binding.viewPager
        viewPager.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val adapter = viewPager.adapter

                if (adapter != null && playIndex != position) {
                    // 暂停之前页面的视频播放
                    val lastViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
                    if (lastViewHolder is MyViewHolder) {
                        lastViewHolder.binding.videoView.player?.pause()
                    }

                    // 播放当前页面的视频
                    val displayViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(position)
                    if (displayViewHolder is MyViewHolder) {
                        displayViewHolder.binding.videoView.player?.play()
                    }

                    // 更新播放位置
                    playIndex = position
                }

                if(getMoreAllow) {
                    if (position <= 3) {
                        requestMedia(insert = true, createViewPager = false)
                    } else if (mediaList.size - position <= 3) {
                        requestMedia(insert = false, createViewPager = false)
                    }
                }
            }
        })

        if(getMoreAllow){
            viewPager.setCurrentItem(playIndex, false)
        }
        if(startFrom > 0){
            viewPager.setCurrentItem(startFrom, false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshViewPager(number: Int, insert: Boolean = false) {
        val viewPager: ViewPager2 = binding.viewPager
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

    inner class MyPagerAdapter : RecyclerView.Adapter<MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val binding = SmartVideoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return mediaList.size
        }

        @OptIn(UnstableApi::class) @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
        override fun onBindViewHolder(holder: MyViewHolder, @SuppressLint("RecyclerView") position: Int) {
            val media = mediaList[position]
            val item = holder.binding
            var accessLogId = 0
            accessLog(requireContext(), media.id.toString(), "VISIT_MEDIA"){
                accessLogId = it
            }

            // 创建背景图
            Glide.with(requireContext())
                .load("${media.cover}/short1200px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(requireContext())))
                .into(item.blurBackground)

            // 获取最佳视频分辨率并设置到资源
            var bestResolutionRatio = 720
            if(sharedPreferences != null){
                bestResolutionRatio = sharedPreferences!!.getInt("best_resolution_ratio", 720)
            }
            val bestMediaIndex = getBestMedia(media.format, bestResolutionRatio)
            val bestMediaUrl = media.format[bestMediaIndex].url
            val mediaItem = MediaItem.fromUri(bestMediaUrl)

            // 获取播放器实例并注册
            val player = ExoPlayer.Builder(requireContext()).build()
            item.videoView.player = player
            item.videoView.useController = false
            player.setMediaItem(mediaItem)
            player.prepare()

            // 设置赞过
            fun setIsAppreciate(){
                if(media.like != null){
                    item.appreciate.setImageResource(R.drawable.appreciate_fill)
                    item.appreciate.imageTintList = ColorStateList.valueOf(Color.parseColor("#F53F3F"))
                } else {
                    item.appreciate.setImageResource(R.drawable.appreciate)
                    val color = when(isDarken(requireActivity())){
                        true -> Color.WHITE
                        false -> Color.BLACK
                    }
                    item.appreciate.imageTintList = ColorStateList.valueOf(color)
                }
            }
            item.appreciate.setOnClickListener {
                item.appreciate.isEnabled = false
                if(media.like == null){
                    media.like = 1
                    setIsAppreciate()

                    appreciate(media.id){result, id ->
                        if(result){
                            media.like = id
                        } else {
                            media.like = null
                            setIsAppreciate()
                            Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                        }
                        item.appreciate.isEnabled = true
                    }
                } else {
                    appreciateCancel(media.like!!){
                        media.like = null
                        setIsAppreciate()
                        item.appreciate.isEnabled = true
                    }
                }
            }
            setIsAppreciate()

            // 设置收藏
            fun setIsLike(){
                if(media.is_collection != null){
                    item.favor.setImageResource(R.drawable.like_fill)
                    item.favor.imageTintList = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))
                } else {
                    item.favor.setImageResource(R.drawable.like)
                    val color = when(isDarken(requireActivity())){
                        true -> Color.WHITE
                        false -> Color.BLACK
                    }
                    item.favor.imageTintList = ColorStateList.valueOf(color)
                }
            }
            item.favor.setOnClickListener {
                setMediaCollection(media){
                    if(media.is_collection == null){
                        if(it){
                            media.is_collection = "true"
                        } else {
                            media.is_collection = null
                        }
                    } else {
                        if(it){
                            media.is_collection = null
                        } else {
                            media.is_collection = "true"
                        }
                    }
                    setIsLike()
                }
            }
            setIsLike()

            // 点击暂停/播放
            var clickTime = 0
            val appreciateDoubleClick = Handler(Looper.getMainLooper())
            val appreciateClearClick = Runnable {
                clickTime = 0
                if(player.isPlaying){
                    player.pause()
                } else {
                    player.play()
                }
            }
            item.videoView.setOnClickListener {
                clickTime++

                if(clickTime >= 2){
                    clickTime = 0
                    item.appreciate.callOnClick()
                    appreciateDoubleClick.removeCallbacks(appreciateClearClick)
                } else {
                    appreciateDoubleClick.postDelayed(appreciateClearClick, 500)
                }
            }

            // 更新文本
            item.mediaName.text = media.name
            item.mediaDescription.text = media.album_name
            if(media.description != null){
                item.mediaDescription.text = "${media.album_name} ${media.description}"
            }

            // 更新按钮
            Glide.with(requireContext())
                .load("${media.model_avatar_image}/yswidth300px")
                .into(item.modelAvatar)
            item.modelAvatar.setOnClickListener { startModelActivity(requireContext(), media.bind_model_id) }
            item.share.setOnClickListener {
                player.pause()
                shareTextContent(
                    context = requireContext(),
                    text = "来自iCoser的分享内容：模特 - ${media.bind_model_id}, 写真集 - ${media.bind_album_id}, 视频 - ${media.name}, 访问链接：https://app.fixeam.com/media?video-id=${media.id}"
                )
            }
            item.album.setOnClickListener { startAlbumActivity(requireContext(), media.bind_album_id) }

            // 更新视频相关组件
            item.playButton.visibility = View.GONE

            // 更新播放进度函数
            fun updateProgressDisplay(currentPosition: Long, updatePlayerPosition: Boolean = false){
                // 获取视频总时长计算百分比
                val duration = player.duration
                val percent = currentPosition.toFloat() / duration.toFloat()

                // 更新进度时间显示
                item.progressText.text = "${formatTime(currentPosition)} / ${formatTime(duration)}"

                // 更新进度条显示
                val currentWidth = item.progressBar.layoutParams.width
                val animator = ValueAnimator.ofInt(currentWidth, (getScreenWidth(requireContext()) * percent).toInt())
                animator.addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Int
                    item.progressBar.layoutParams.width = animatedValue
                    item.progressBar.requestLayout()
                }
                animator.duration = 100
                animator.start()

                // 如果需要则更新视频播放进度
                if(updatePlayerPosition){
                    player.seekTo(currentPosition)
                }
            }

            // 进度条触摸
            item.progressBarContainter.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // 用户开始触摸屏幕增加进度条高度
                            val animator = ValueAnimator.ofInt(item.progressBar.layoutParams.height, (resources.displayMetrics.density * 8).toInt())
                            animator.addUpdateListener { animation ->
                                item.progressBar.layoutParams.height = animation.animatedValue as Int
                                item.progressBar.requestLayout()
                            }
                            animator.duration = 100
                            animator.start()

                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            // 更新进度条的进度
                            val percent = event.x / getScreenWidth(requireContext())
                            val currentPosition = (player.duration * percent).toLong()
                            updateProgressDisplay(currentPosition, true)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            // 用户结束触摸屏幕还原进度条高度
                            val animator = ValueAnimator.ofInt(item.progressBar.layoutParams.height, (resources.displayMetrics.density * 3).toInt())
                            animator.addUpdateListener { animation ->
                                item.progressBar.layoutParams.height = animation.animatedValue as Int
                                item.progressBar.requestLayout()
                            }
                            animator.duration = 100
                            animator.start()

                            return true
                        }
                    }
                    return false
                }
            })

            // 定时器
            var handler: Handler? = null
            var runnable: Runnable? = null
            player.addListener(object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    // 获取存储共享器 从本地存储中读取上次播放的位置
                    if(sharedPreferences != null) {
                        val lastPlayedPosition =
                            sharedPreferences!!.getInt("last_played_position_${media.id}", 0)
                        updateProgressDisplay(lastPlayedPosition.toLong(), true)
                    }
                    super.onTimelineChanged(timeline, reason)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) {
                        item.playButton.visibility = View.GONE
                    } else {
                        item.playButton.visibility = View.VISIBLE
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        item.playButton.visibility = View.VISIBLE
                        item.progressBar.layoutParams = item.progressBar.layoutParams.apply {
                            width = 0
                        }
                        item.progressText.text = "00:00 / 00:00"
                        player.seekTo(0.toLong())
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        item.playButton.visibility = View.GONE
                        item.loading.clearAnimation()
                        item.loading.visibility = View.GONE

                        handler = Handler(Looper.getMainLooper())
                        runnable = object : Runnable {
                            override fun run() {
                                // 更新进度条
                                val currentPosition = player.currentPosition
                                updateProgressDisplay(currentPosition)

                                // 记录播放位置
                                if(sharedPreferences != null){
                                    sharedPreferences!!.edit().putInt("last_played_position_${media.id}",
                                        currentPosition.toInt()
                                    ).apply()
                                }

                                updateAccessLog(accessLogId, (currentPosition / 1000).toInt())

                                handler?.postDelayed(this, 500) // 延迟1秒钟
                            }
                        }
                        handler?.postDelayed(runnable!!, 500)
                    } else {
                        item.playButton.visibility = View.VISIBLE
                        handler?.removeCallbacks(runnable!!)
                        handler = null
                        runnable = null
                    }
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    if(isLoading && !player.isPlaying){
                        val animation = AnimationUtils.loadAnimation(requireContext(),
                            R.anim.loading
                        )
                        item.loading.startAnimation(animation)
                        item.loading.visibility = View.VISIBLE
                    } else {
                        item.loading.clearAnimation()
                        item.loading.visibility = View.GONE

                        if(position == 5){
                            player.play()
                        }
                    }
                    super.onIsLoadingChanged(isLoading)
                }
            })
        }

        override fun onViewRecycled(holder: MyViewHolder) {
            // 释放 AndroidX Media3 ExoPlayer 资源
            holder.binding.videoView.player?.pause()
            holder.binding.videoView.player?.stop()
            holder.binding.videoView.player?.release()
            holder.binding.videoView.player = null

            super.onViewRecycled(holder)
        }
    }

    inner class MyViewHolder(var binding: SmartVideoItemBinding) : RecyclerView.ViewHolder(binding.root)

    // 界面切换时的相关事件
    private var lastIsPlaying = false
    private fun leaveFragment(){
        val viewPager: ViewPager2 = binding.viewPager
        val lastViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
        if (lastViewHolder is MyViewHolder) {
            val player = lastViewHolder.binding.videoView.player

            if (player != null && player.isPlaying) {
                lastIsPlaying = true
                player.pause()
            }
        }
    }
    private fun enterFragment(){
        if(lastIsPlaying){
            val viewPager: ViewPager2 = binding.viewPager
            val lastViewHolder = (viewPager.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(playIndex)
            if (lastViewHolder is MyViewHolder) {
                val player = lastViewHolder.binding.videoView.player

                if (player != null && !isHide) {
                    lastIsPlaying = false
                    player.play()
                }
            }
        }
    }
    private var isHide: Boolean = false
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
            isHide = true
            leaveFragment()
        } else {
            isHide = false
            enterFragment()
        }
        super.onHiddenChanged(hidden)
    }
    override fun onResume() {
        enterFragment()
        super.onResume()
    }
}