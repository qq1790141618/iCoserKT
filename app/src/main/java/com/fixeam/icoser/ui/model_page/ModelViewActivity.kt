package com.fixeam.icoser.ui.model_page

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityModelViewBinding
import com.fixeam.icoser.databinding.AlbumItemBinding
import com.fixeam.icoser.model.AlbumViewHolder
import com.fixeam.icoser.model.changeBackgroundDim
import com.fixeam.icoser.model.createAlbumBinding
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.shareTextContent
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startMediaActivity
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.Models
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.requestAlbumData
import com.fixeam.icoser.network.requestMediaData
import com.fixeam.icoser.network.requestModelData
import com.fixeam.icoser.network.setForbidden
import com.fixeam.icoser.network.setModelFollowing
import com.fixeam.icoser.network.updateAccessLog
import com.fixeam.icoser.ui.image_preview.ImagePreviewActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray

class ModelViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModelViewBinding
    private var modelInfo: Models? = null
    private var albumList: MutableList<Albums> = mutableListOf()
    private var isFinished: Boolean = false
    private var albumLoading: Boolean = false
    private var doNotSetToken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelViewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置加载动画
        val imageView = binding.imageLoading
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置悬浮按钮
        binding.toUp.setOnClickListener { binding.albumList.smoothScrollToPosition(0) }

        val id = intent.getIntExtra("id", -1)
        doNotSetToken = intent.getBooleanExtra("doNotSetToken", false)
        setModelContent(id)
    }

    private var accessLogId: Int = 0

    private fun setModelContent(id: Int){
        val condition = JSONArray()
        condition.put(JSONArray().apply {
            put("id")
            put(id.toString())
        })
        requestModelData(this, condition.toString(), doNotSetToken){ models ->
            if(models.isNotEmpty()){
                modelInfo = models[0]
                accessLog(this@ModelViewActivity, modelInfo!!.id.toString(), "VISIT_MODEL"){
                    accessLogId = it
                }
                setAlbumContent(id, true)
                setMedia(id)
                if(!doNotSetToken){
                    setFollowButton()
                }
            }
        }
    }

    override fun onPause() {
        updateAccessLog(accessLogId)
        super.onPause()
    }

    override fun onStop() {
        updateAccessLog(accessLogId)
        super.onStop()
    }

    private fun setFollowButton(){
        val following = binding.following
        following.visibility = View.VISIBLE

        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)

        following.setOnClickListener {
            following.startAnimation(animation)
            following.setImageResource(R.drawable.loading)

            fun collectionCallback() {
                following.clearAnimation()
                if(modelInfo!!.is_collection != null){
                    following.setImageResource(R.drawable.like)
                    if(isDarken(this@ModelViewActivity)){
                        following.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                    } else {
                        following.imageTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
                    }

                    modelInfo!!.is_collection = null
                } else {
                    following.setImageResource(R.drawable.like_fill)
                    following.imageTintList = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))

                    modelInfo!!.is_collection = true
                }
            }

            fun unLog(){
                following.clearAnimation()
                following.setImageResource(R.drawable.like)
                startLoginActivity(this@ModelViewActivity)
            }

            setModelFollowing(this@ModelViewActivity, modelInfo!!, { collectionCallback() }, { unLog() })
        }

        if(modelInfo!!.is_collection != null){
            following.setImageResource(R.drawable.like_fill)
            following.imageTintList = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))
        }
    }

    private fun setAlbumContent(id: Int, create: Boolean = false){
        albumLoading = true
        val getNumber = 20

        val condition = JSONArray()
        condition.put(JSONArray().apply {
            put("model_id")
            put(id.toString())
        })
        requestAlbumData(this, condition.toString(), doNotSetToken, albumList.size, getNumber){
            binding.imageLoading.clearAnimation()
            binding.imageLoading.visibility = View.GONE

            albumList.addAll(it)
            if(it.size < getNumber){
                isFinished = true
            }

            if(create){
                initModelBox()
                initAlbumList()
            } else {
                updateAlbumList(it.size)
            }
            albumLoading = false
        }
    }

    private fun initModelBox(){
        binding.titleOverlay.alpha = 0f
        binding.name.text = when(modelInfo?.other_name){
            null -> modelInfo?.name
            else -> "${modelInfo?.name} ${modelInfo?.other_name}"
        }

        if(modelInfo?.avatar_image != null){
            val avatar = binding.avatar
            Glide.with(this)
                .load("${modelInfo?.avatar_image}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(RoundedCorners(250))
                .into(avatar)
            Glide.with(this)
                .load("${modelInfo?.avatar_image}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(RoundedCorners(250))
                .into(binding.avatar2)
            avatar.setOnClickListener {
                val header: ArrayList<String> = arrayListOf(modelInfo?.avatar_image!!)
                ImagePreviewActivity.start(
                    this,
                    0,
                    header,
                    binding.avatar2
                )
            }
        }
        if(modelInfo?.background_image != null){
            val backgroundView = binding.backgroundView
            Glide.with(this)
                .load("${modelInfo?.background_image}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(backgroundView)
            binding.toolbar.navigationIcon?.setTint(Color.WHITE)
            binding.name.setTextColor(Color.WHITE)
            binding.more.iconTint = ColorStateList.valueOf(Color.WHITE)
        }

        if(modelInfo?.tags != null){
            val gson = Gson()
            val listType = object : TypeToken<List<String>>() {}.type
            val stringList: List<String> = gson.fromJson(modelInfo?.tags, listType)

            val linearLayout = binding.tagBox

            stringList.forEach {
                val chip = layoutInflater.inflate(R.layout.tag, linearLayout, false)
                chip.findViewById<TextView>(R.id.text).text = it
                linearLayout.addView(chip)
            }
        }

        binding.more.setOnClickListener { createModelOptions() }
    }

    private fun setMedia(modelId: Int){
        requestMediaData(
            context = this,
            modelId = modelId
        ){
            if(it.isNotEmpty()){
                binding.goToVideo.visibility = View.VISIBLE
                binding.goToVideo.setOnClickListener { startMediaActivity(this, modelId = modelId) }
            }
        }
    }

    private fun initAlbumList(){
        val albumListView: RecyclerView = binding.albumList
        albumListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        albumListView.adapter = AlbumsAdapter()

        albumListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                changeModelBoxHeight(recyclerView.computeVerticalScrollOffset())

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItem + 10 > totalItemCount && !isFinished && !albumLoading) {
                    modelInfo?.id?.let { setAlbumContent(it) }
                }
            }
        })
    }

    private fun changeModelBoxHeight(scrollOffset: Int){
        // 变化组件
        val modelBox = binding.modelBox
        val avatar = binding.avatar
        val avatar2 = binding.avatar2
        val tagBox = binding.tagBox
        val titleOverlay = binding.titleOverlay
        val name = binding.name
        val toolbar = binding.toolbar
        val more = binding.more

        // 尺寸范围
        val avatarScale = resources.displayMetrics.density * 50
        val avatarMax = resources.displayMetrics.density * 80
        val modelBoxFirstScale = resources.displayMetrics.density * 50
        val modelBoxThenScale = resources.displayMetrics.density * 80
        val modelBoxMax = resources.displayMetrics.density * 180

        // 变化阶段
        val animStartScroll = 100
        val animFirstScroll = 600
        val animThenScroll = 1300
        val firstOverScroll = animFirstScroll - animStartScroll // 第一阶段滚动的总距离
        val thenOverScroll = animThenScroll - animFirstScroll // 后置阶段滚动的总距离

        // 第一阶段
        if(scrollOffset in animStartScroll + 1..<animFirstScroll){
            val offsetRatioFirst = (scrollOffset.toFloat() - animStartScroll) / firstOverScroll // 第一阶段滚动比例： ( 滚动距离 - 第一阶段初始位置 ) / 第一阶段滚动的总距离
            val avatarSize = (avatarMax - avatarScale * offsetRatioFirst).toInt() // 第一阶段头像大小 ( 头像最大尺寸 - 头像总缩小尺寸 * 缩小比例(第一阶段滚动比例) ) . 转整形
            val modelBoxHeight = (modelBoxMax - modelBoxFirstScale * offsetRatioFirst).toInt() // 第一阶段盒子大小 ( 盒子最大尺寸 - 盒子第一阶段小尺寸 * 缩小比例(第一阶段滚动比例) ) . 转整形

            avatar.layoutParams.height = avatarSize
            avatar.layoutParams.width = avatarSize
            avatar.requestLayout()
            avatar2.layoutParams.height = avatarSize
            avatar2.layoutParams.width = avatarSize
            avatar2.requestLayout()

            tagBox.alpha = 1.0f - offsetRatioFirst
            tagBox.requestLayout()

            modelBox.layoutParams.height = modelBoxHeight
            modelBox.requestLayout()

            // 初始形态
            titleOverlay.alpha = 0f
            name.translationX = 0f
            avatar.translationX = 0f
            avatar.translationY = 0f
            avatar2.translationX = 0f
            avatar2.translationY = 0f
            if(modelInfo?.background_image != null && !isDarken(this)){
                toolbar.navigationIcon?.setTint(Color.WHITE)
                name.setTextColor(Color.WHITE)
                more.iconTint = ColorStateList.valueOf(Color.WHITE)
            }
        }
        // 第二阶段
        val nameX = resources.displayMetrics.density * 30 / 2
        val avatarX = -(name.width / 2 + resources.displayMetrics.density * 15)
        val avatarY = resources.displayMetrics.density * -40.5f
        if(scrollOffset in animFirstScroll + 1..animThenScroll){
            val offsetRatioThen = (scrollOffset.toFloat() - animFirstScroll) / thenOverScroll // 后置阶段滚动比例： ( 滚动距离 - 第一阶段结束位置 ) / 第二阶段滚动的总距离
            val modelBoxHeight = (modelBoxMax - modelBoxFirstScale - modelBoxThenScale * offsetRatioThen).toInt() // 第一阶段盒子大小 ( 盒子最大尺寸 - 盒子第一阶段小尺寸 * 缩小比例(第一阶段滚动比例) ) . 转整形
            modelBox.layoutParams.height = modelBoxHeight
            modelBox.requestLayout()

            titleOverlay.alpha = offsetRatioThen
            tagBox.alpha = 0f

            name.translationX = nameX * offsetRatioThen
            avatar.translationX = avatarX * offsetRatioThen
            avatar.translationY = avatarY * offsetRatioThen
            avatar2.translationX = avatarX * offsetRatioThen
            avatar2.translationY = avatarY * offsetRatioThen
            if(modelInfo?.background_image != null && !isDarken(this)){
                val color16 = Integer.toHexString((255 * (1 - offsetRatioThen)).toInt())
                val colorString = when(color16.length){
                    2 -> "#$color16$color16$color16"
                    else -> "#0${color16}0${color16}0${color16}"
                }
                toolbar.navigationIcon?.setTint(Color.parseColor(colorString))
                name.setTextColor(Color.parseColor(colorString))
                more.iconTint = ColorStateList.valueOf(Color.parseColor(colorString))
            }
        }
        // 最终形态
        if(scrollOffset > animThenScroll - 20){
            modelBox.layoutParams.height = (modelBoxMax - modelBoxFirstScale - modelBoxThenScale).toInt()
            titleOverlay.alpha = 1f

            name.translationX = nameX
            avatar.translationX = avatarX
            avatar.translationY = avatarY
            avatar2.translationX = avatarX
            avatar2.translationY = avatarY
            if(modelInfo?.background_image != null){
                if(!isDarken(this)){
                    toolbar.navigationIcon?.setTint(Color.BLACK)
                    name.setTextColor(Color.BLACK)
                    more.iconTint = ColorStateList.valueOf(Color.BLACK)
                }
            }
        }
    }

    private fun createModelOptions(){
        changeBackgroundDim(true, this)
        val flashPanel = layoutInflater.inflate(R.layout.model_flash_panel, null)
        val popupWindow = PopupWindow(
            flashPanel,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.showAtLocation(
            binding.root,
            Gravity.BOTTOM,
            0,
            0
        )
        popupWindow.setOnDismissListener {
            changeBackgroundDim(false, this)
        }

        // 调整按钮操作
        flashPanel.findViewById<MaterialButton>(R.id.close)?.setOnClickListener { popupWindow.dismiss() }
        flashPanel.findViewById<MaterialButton>(R.id.share)?.setOnClickListener {
            shareTextContent(
                context = this,
                text = "来自iCoser分享的模特 - ${modelInfo?.name}, 访问链接：https://app.fixeam.com/model?id=${modelInfo?.id}"
            )
        }
        flashPanel.findViewById<MaterialButton>(R.id.forbidden).setOnClickListener {
            createSimpleDialog(this, "确定屏蔽此模特吗? 您将不会收到任何与此模特有关的内容推送.", true){
                setForbidden(
                    this,
                    modelInfo?.id!!,
                    "model",
                    {
                        popupWindow.dismiss()
                        onBackPressed()
                    },
                    {
                        startLoginActivity(this)
                    })
            }
        }
    }

    private fun updateAlbumList(loadedNumber: Int){
        binding.albumList.adapter?.notifyItemInserted(albumList.size - loadedNumber)
    }

    inner class AlbumsAdapter: RecyclerView.Adapter<AlbumViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
            val binding = AlbumItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlbumViewHolder(binding)
        }
        override fun getItemCount(): Int {
            return albumList.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
            // 修改holder
            val album = albumList[position]
            createAlbumBinding(this@ModelViewActivity, album, holder.itemView, holder.binding)
        }
    }
}