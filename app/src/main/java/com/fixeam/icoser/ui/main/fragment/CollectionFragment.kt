package com.fixeam.icoser.ui.main.fragment

import android.annotation.SuppressLint
import android.content.Intent
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
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.getScreenWidth
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.followAlbumList
import com.fixeam.icoser.network.requestFollowData
import com.fixeam.icoser.network.setForbidden
import com.fixeam.icoser.network.setModelFollowingById
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.ui.album_page.AlbumViewActivity
import com.fixeam.icoser.ui.image_preview.ImagePreviewActivity
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.fixeam.icoser.ui.media_page.MediaViewActivity
import com.fixeam.icoser.ui.model_page.ModelViewActivity
import com.fixeam.icoser.ui.search_page.SearchActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class CollectionFragment : Fragment() {
    private var adapter: MyListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置标题字体
        val title = view.findViewById<TextView>(R.id.top_text)
        title.typeface = Typeface.createFromAsset(requireContext().assets, "font/JosefinSans-Regular-7.ttf")

        // 创建搜索按钮点击
        val homeSearchButton = view.findViewById<ImageView>(R.id.home_search_button)
        homeSearchButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        if(userToken == null){
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        } else {
            val imageView = view.findViewById<ImageView>(R.id.image_loading)
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
            imageView.startAnimation(animation)
            imageView.visibility = View.VISIBLE

            requestFollowData(requireContext()) {
                imageView.clearAnimation()
                imageView.visibility = View.GONE

                val refreshLayout = view.findViewById<SmartRefreshLayout>(R.id.refreshLayout)
                refreshLayout.visibility = View.VISIBLE
                refreshLayout.setOnRefreshListener {
                    requestFollowData(requireContext(), true){
                        val followList = view.findViewById<RecyclerView>(R.id.follow_list)
                        val adapter = followList.adapter
                        adapter?.notifyDataSetChanged()

                        refreshLayout.finishRefresh()
                        Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show()
                    }
                }
                refreshLayout.setOnLoadMoreListener {
                    val index = followAlbumList.size
                    requestFollowData(requireContext()) {
                        adapter?.notifyItemInserted(index)
                        refreshLayout.finishLoadMore()
                    }
                }

                initFollowList()
            }
        }
    }

    private fun initFollowList() {
        val followList = view?.findViewById<RecyclerView>(R.id.follow_list)
        if (followList != null) {
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
    }

    private fun setForbiddenIcon(icon: ImageView, position: Int){
        icon.setOnClickListener {
            followAlbumList.removeAt(position)
            val followList = view?.findViewById<RecyclerView>(R.id.follow_list)
            val adapter = followList?.adapter
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
            val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.publish_item, parent, false)
            return MyViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return followAlbumList.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // 修改holder
            val album = followAlbumList[position]
            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext(), AlbumViewActivity::class.java)
                intent.putExtra("id", album.id)
                startActivity(intent)
            }

            // 修改上方内容
            val followButton = holder.itemView.findViewById<MaterialButton>(R.id.following)
            val followedButton = holder.itemView.findViewById<MaterialButton>(R.id.followed)
            val closeButton = holder.itemView.findViewById<ImageView>(R.id.close)

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
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
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

            val fromIcon = holder.itemView.findViewById<ImageView>(R.id.from_icon)
            val fromText = holder.itemView.findViewById<TextView>(R.id.from_text)
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
            val avatar = holder.itemView.findViewById<ImageView>(R.id.avatar)
            Glide.with(requireContext())
                .load("${album.model_avatar_image}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(RoundedCorners(250))
                .into(avatar)
            val modelName = holder.itemView.findViewById<TextView>(R.id.model_name)
            modelName.text = album.model
            
            avatar.setOnClickListener {
                val intent = Intent(requireContext(), ModelViewActivity::class.java)
                intent.putExtra("id", album.model_id)
                startActivity(intent)
            }
            modelName.setOnClickListener {
                val intent = Intent(requireContext(), ModelViewActivity::class.java)
                intent.putExtra("id", album.model_id)
                startActivity(intent)
            }

            // 设置动态内容
            val publishContentText = holder.itemView.findViewById<TextView>(R.id.publish_content_text)
            publishContentText.text = album.name
            if(album.tags != null){
                publishContentText.text = album.name + ", " + album.tags!!.joinToString(", ")
            }
            val publishContent = holder.itemView.findViewById<FlexboxLayout>(R.id.publish_content_media)
            publishContent.removeAllViews()
            val publishTime = holder.itemView.findViewById<TextView>(R.id.publish_time)
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
                    constraintLayout.setOnClickListener {
                        val intent = Intent(requireContext(), MediaViewActivity::class.java)
                        intent.putExtra("id", media.id)
                        startActivity(intent)
                    }
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

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}