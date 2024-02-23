package com.fixeam.icoserkt

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectionFragment : Fragment() {
    private val albumList: MutableList<Albums> = mutableListOf()
    private var isFinished: Boolean = false
    private var albumLoading: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(userToken == null){
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        } else {
            // 设置加载动画
            val imageView = view.findViewById<ImageView>(R.id.image_loading)
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
            imageView.startAnimation(animation)
            imageView.visibility = View.VISIBLE

            requestFollowData {
                imageView.clearAnimation()
                imageView.visibility = View.GONE
                initFollowList()
            }
        }
    }

    private fun requestFollowData(callback: () -> Unit) {
        albumLoading = true
        val call = ApiNetService.GetFollow(userToken!!, albumList.size, 20)

        call.enqueue(object : Callback<AlbumsResponse> {
            override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    responseBody?.let {
                        albumList.addAll(it.data)
                        if(it.data.size < 20){
                            isFinished = true
                        }
                    }

                    callback()
                    albumLoading = false
                }
            }

            override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initFollowList() {
        val followList = view?.findViewById<RecyclerView>(R.id.follow_list)
        if (followList != null) {
            followList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            val adapter = MyListAdapter()
            followList.adapter = adapter

            followList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount

                    if (lastVisibleItem + 5 > totalItemCount && !isFinished && !albumLoading) {
                        val index = albumList.size
                        requestFollowData {
                            adapter.notifyItemInserted(index)
                        }
                    }
                }
            })
        }
    }

    inner class MyListAdapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.publish_item, parent, false)
            return MyViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return albumList.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // 修改holder
            val album = albumList[position]
            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext(), AlbumViewActivity::class.java)
                intent.putExtra("id", album.id)
                startActivity(intent)
            }

            // 检测是否为推荐，如果是则创建推荐提示，关闭已关注提示
            if (album.type != "follow"){
                holder.itemView.findViewById<LinearLayout>(R.id.from_collection).visibility = View.VISIBLE
                holder.itemView.findViewById<LinearLayout>(R.id.from_follow).visibility = View.GONE

                val followButton = holder.itemView.findViewById<MaterialButton>(R.id.following)
                val followedButton = holder.itemView.findViewById<MaterialButton>(R.id.followed)
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
            val contentWidth = getScreenWidth(requireContext()) - resources.displayMetrics.density * 54 - 3
            val contentGap = resources.displayMetrics.density * 10
            val contentItemSize = ((contentWidth - contentGap * 2) / 3).toInt()

            // 加入视频
            if(album.media != null){
                for (media in album.media!!){
                    if(publishContent.childCount >= 6){
                        break
                    }

                    // 创建布局
                    val constraintLayout = ConstraintLayout(requireContext())
                    val layoutParams = ConstraintLayout.LayoutParams(
                        contentItemSize,
                        contentItemSize
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
                    val paddingPx = ( contentItemSize - 80 ) / 2
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
            for (image in (album.images as List<String>)){
                if(publishContent.childCount >= 6){
                    break
                }

                if(publishContent.childCount == 5){
                    // 创建布局
                    val constraintLayout = ConstraintLayout(requireContext())
                    val layoutParams = ConstraintLayout.LayoutParams(
                        contentItemSize,
                        contentItemSize
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
                    val moreImage = TextView(requireContext())
                    moreImage.id = View.generateViewId()
                    moreImage.text = (album.images as List<*>).size.toString() + "张"
                    moreImage.textSize = 20F
                    val color = ColorStateList.valueOf(Color.parseColor("#ACFFFFFF"))
                    moreImage.setTextColor(color)
                    moreImage.typeface = Typeface.DEFAULT_BOLD

                    constraintLayout.addView(moreImage)
                    val playButtonImageLayoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val paddingPx = ( contentItemSize - 80 ) / 2
                    moreImage.setPadding(paddingPx - 30, paddingPx, paddingPx - 30, paddingPx)
                    moreImage.layoutParams = playButtonImageLayoutParams

                    // 添加到内容
                    constraintLayout.setOnClickListener {

                    }
                    publishContent.addView(constraintLayout)

                    continue
                }

                val imageDisplay = ImageView(requireContext())
                imageDisplay.scaleType = ImageView.ScaleType.CENTER_CROP
                imageDisplay.id = View.generateViewId()
                imageDisplay.setImageResource(R.drawable.image_holder)

                val layoutParams = ConstraintLayout.LayoutParams(
                    contentItemSize,
                    contentItemSize
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

                publishContent.addView(imageDisplay)
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}