package com.fixeam.icoser.ui.main.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.FragmentCollectionBinding
import com.fixeam.icoser.databinding.PublishItemBinding
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.getScreenWidth
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startMediaActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.model.startSearchActivity
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.followAlbumList
import com.fixeam.icoser.network.requestFollowData
import com.fixeam.icoser.network.setForbidden
import com.fixeam.icoser.network.setModelFollowingById
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.ui.image_preview.ImagePreviewActivity

class CollectionFragment : Fragment() {
    private lateinit var binding: FragmentCollectionBinding
    private var adapter: MyListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCollectionBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAppHeaderLayout()

        if(userToken == null){
            startLoginActivity(requireContext())
        } else {
            val imageView = binding.imageLoading
            imageView.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.loading))
            imageView.visibility = View.VISIBLE

            requestFollowData() {
                imageView.clearAnimation()
                imageView.visibility = View.GONE

                setRefreshLayout()
                initFollowList()
            }
        }
    }

    // 设置页头函数
    private fun setAppHeaderLayout(){
        // 设置标题字体
        val typeface = Typeface.createFromAsset(requireContext().assets, "font/JosefinSans-Regular-7.ttf")
        binding.appHeader.topText.typeface = typeface
        // 创建搜索按钮点击
        val homeSearchButton = binding.appHeader.homeSearchButton
        homeSearchButton.setOnClickListener { startSearchActivity(requireContext()) }
    }
    // 设置下拉刷新函数
    @SuppressLint("NotifyDataSetChanged")
    private fun setRefreshLayout(){
        val refreshLayout = binding.refreshLayout
        refreshLayout.visibility = View.VISIBLE
        refreshLayout.setOnRefreshListener {
            requestFollowData(true){
                val followList = binding.followList
                val adapter = followList.adapter
                adapter?.notifyDataSetChanged()

                refreshLayout.finishRefresh()
                Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show()
            }
        }
        refreshLayout.setOnLoadMoreListener {
            val index = followAlbumList.size
            requestFollowData() {
                adapter?.notifyItemInserted(index)
                refreshLayout.finishLoadMore()
            }
        }
    }

    private fun initFollowList() {
        val followList = binding.followList
        followList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        adapter = MyListAdapter()
        followList.adapter = adapter

        followList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                for (index in firstVisibleItemPosition..lastVisibleItemPosition){
                    if(followAlbumList[index].isNew){
                        followAlbumList[index].isNew = false
                        accessLog(requireContext(), followAlbumList[index].id.toString(), "VISIT_ALBUM"){ }
                    }
                }
            }
        })
    }

    private fun setForbiddenIcon(icon: ImageView, position: Int){
        icon.setOnClickListener {
            followAlbumList.removeAt(position)
            val followList = binding.followList
            val adapter = followList.adapter
            adapter?.notifyItemRemoved(position)

            setForbidden(
                requireContext(),
                followAlbumList[position].id,
                "album",
                {
                    // 已经执行完移除操作
                },
                {
                    // 此页面的调用不会出现未登录
                }
            )
        }
    }

    inner class MyListAdapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val binding = PublishItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
        }
        override fun getItemCount(): Int {
            return followAlbumList.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // 修改holder
            val album = followAlbumList[position]
            holder.itemView.setOnClickListener { startAlbumActivity(requireContext(), album.id) }

            // 修改上方内容
            val followButton = holder.binding.following
            val followedButton = holder.binding.followed
            val closeButton = holder.binding.close

            if (album.type == "recommend" || album.type == "push"){
                followButton.visibility = View.VISIBLE
                followButton.setOnClickListener {
                    setModelFollowingById(
                        requireContext(),
                        album.model_id,
                        {
                            followButton.visibility = View.GONE
                            followedButton.visibility = View.VISIBLE
                        },
                        {
                            startLoginActivity(requireContext())
                        }
                    )
                }

                closeButton.visibility = View.VISIBLE
                setForbiddenIcon(closeButton, position)
            } else {
                followButton.visibility = View.GONE
                followedButton.visibility = View.GONE
                closeButton.visibility = View.GONE
            }

            val fromIcon = holder.binding.fromIcon
            val fromText = holder.binding.fromText
            when(album.type){
                "follow" -> {
                    fromIcon.setImageResource(R.drawable.evaluate)
                    fromText.text = getText(R.string.from_follow)
                }
                "recommend" -> {
                    fromIcon.setImageResource(R.drawable.goods_favor)
                    fromText.text = getText(R.string.from_collection)
                }
                else -> {
                    fromIcon.setImageResource(R.drawable.selection)
                    fromText.text = getText(R.string.from_push)
                }
            }

            // 设置模特内容
            Glide.with(requireContext())
                .load("${album.model_avatar_image}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(RoundedCorners(250))
                .into(holder.binding.avatar)
            holder.binding.modelName.text = album.model
            holder.binding.avatar.setOnClickListener { startModelActivity(requireContext(), album.model_id) }
            holder.binding.modelName.setOnClickListener { startModelActivity(requireContext(), album.model_id) }

            // 设置动态内容
            val publishContentText = holder.binding.publishContentText
            publishContentText.text = album.name
            if(album.tags != null){
                publishContentText.text = album.name + ", " + album.tags!!.joinToString(", ")
            }
            val publishContent = holder.binding.publishContentMedia
            publishContent.removeAllViews()
            val publishTime = holder.binding.publishTime
            publishTime.text = "发布于 ${calculateTimeAgo(album.create_time)}"

            // 计算动态内容宽度(3像素的容差)
            val contentWidth = getScreenWidth(requireContext()) - resources.displayMetrics.density * 60 - 3
            val contentGap = resources.displayMetrics.density * 10
            var contentItemWidth = ((contentWidth - contentGap * 2) / 3).toInt()
            var contentItemHeight = ((contentWidth - contentGap * 2) / 3).toInt()

            if(album.media != null){
                if((album.images as List<*>).size + album.media!!.size == 1){
                    contentItemWidth *= 2
                    contentItemHeight *= 3
                }
            } else {
                if((album.images as List<*>).size == 1){
                    contentItemWidth *= 2
                    contentItemHeight *= 3
                }
            }


            // 加入视频
            if(album.media != null){
                for (media in album.media!!){
                    if(publishContent.childCount >= 6){
                        break
                    }

                    // 创建布局
                    val constraintLayout = ConstraintLayout(requireContext())
                    val layoutParams = ConstraintLayout.LayoutParams(
                        contentItemWidth,
                        contentItemHeight
                    )
                    layoutParams.bottomMargin = contentGap.toInt()
                    if((publishContent.childCount + 1) % 3 != 0){
                        layoutParams.rightMargin = contentGap.toInt()
                    }
                    constraintLayout.layoutParams = layoutParams

                    // 加入封面
                    val coverImage = ImageView(requireContext())
                    coverImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    coverImage.id = View.generateViewId()
                    coverImage.setImageResource(R.drawable.image_holder)
                    coverImage.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    constraintLayout.addView(coverImage)
                    Glide.with(requireContext())
                        .load("${media.cover}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(coverImage)

                    // 加入播放按钮
                    val playButtonImage = ImageView(requireContext())
                    playButtonImage.id = View.generateViewId()
                    playButtonImage.setImageResource(R.drawable.video_fill)
                    constraintLayout.addView(playButtonImage)
                    val playButtonImageLayoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val paddingPx = ( contentItemWidth - 80 ) / 2
                    playButtonImage.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                    playButtonImage.layoutParams = playButtonImageLayoutParams
                    val colorFilter = PorterDuffColorFilter(Color.parseColor("#80000000"), PorterDuff.Mode.SRC_IN)
                    playButtonImage.colorFilter = colorFilter

                    // 添加到内容
                    constraintLayout.setOnClickListener { startMediaActivity(requireContext(), media.id) }
                    publishContent.addView(constraintLayout)
                }
            }

            // 加入图片
            for ((index, image) in (album.images as List<String>).withIndex()){
                if(publishContent.childCount >= 6){
                    break
                }

                if(publishContent.childCount == 5){
                    // 创建布局
                    val constraintLayout = ConstraintLayout(requireContext())
                    val layoutParams = ConstraintLayout.LayoutParams(
                        contentItemWidth,
                        contentItemHeight
                    )
                    layoutParams.bottomMargin = contentGap.toInt()
                    if((publishContent.childCount + 1) % 3 != 0){
                        layoutParams.rightMargin = contentGap.toInt()
                    }
                    constraintLayout.layoutParams = layoutParams

                    // 加入图片
                    val imageDisplay = ImageView(requireContext())
                    imageDisplay.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageDisplay.id = View.generateViewId()
                    imageDisplay.setImageResource(R.drawable.image_holder)
                    constraintLayout.addView(imageDisplay)
                    Glide.with(requireContext())
                        .load("${image}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageDisplay)

                    // 显示更多图片按钮
                    val moreImageText = TextView(requireContext())
                    moreImageText.id = View.generateViewId()
                    moreImageText.text = (album.images as List<*>).size.toString() + "+"
                    moreImageText.textSize = 18F
                    val color = ColorStateList.valueOf(Color.parseColor("#ACFFFFFF"))
                    moreImageText.setTextColor(color)

                    constraintLayout.addView(moreImageText)
                    val playButtonImageLayoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    moreImageText.layoutParams = playButtonImageLayoutParams
                    moreImageText.gravity = Gravity.CENTER

                    // 添加到内容
                    constraintLayout.setOnClickListener {
                        val activity = requireActivity() as AppCompatActivity
                        ImagePreviewActivity.start(
                            activity,
                            index,
                            (album.images as ArrayList<String>),
                            imageDisplay
                        )
                    }
                    publishContent.addView(constraintLayout)

                    continue
                }

                val imageDisplay = ImageView(requireContext())
                imageDisplay.scaleType = ImageView.ScaleType.CENTER_CROP
                imageDisplay.id = View.generateViewId()
                imageDisplay.setImageResource(R.drawable.image_holder)

                val layoutParams = ConstraintLayout.LayoutParams(
                    contentItemWidth,
                    contentItemHeight
                )
                layoutParams.bottomMargin = contentGap.toInt()
                if((publishContent.childCount + 1) % 3 != 0){
                    layoutParams.rightMargin = contentGap.toInt()
                }
                imageDisplay.layoutParams = layoutParams

                Glide.with(requireContext())
                    .load("${image}/short500px")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageDisplay)

                imageDisplay.setOnClickListener {
                    val activity = requireActivity() as AppCompatActivity
                    ImagePreviewActivity.start(
                        activity,
                        index,
                        (album.images as ArrayList<String>),
                        imageDisplay
                    )
                }

                publishContent.addView(imageDisplay)
            }
        }
    }

    class MyViewHolder(val binding: PublishItemBinding) : RecyclerView.ViewHolder(binding.root)
}