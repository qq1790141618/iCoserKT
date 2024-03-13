package com.fixeam.icoser.ui.follow_page

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityFollowBinding
import com.fixeam.icoser.databinding.MessageTextBinding
import com.fixeam.icoser.databinding.ModelItemBinding
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.getUserFollow
import com.fixeam.icoser.network.setModelFollowing
import com.fixeam.icoser.network.userFollow
import com.fixeam.icoser.network.userToken

class FollowActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            startLoginActivity(this)
        }

        initPage()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initPage(isRefresh: Boolean = false){
        // 设置加载动画
        val imageView = binding.imageLoading
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 再次获取用户关注
        getUserFollow(this){
            imageView.clearAnimation()
            imageView.visibility = View.GONE
            toolbar.title = getString(R.string.my_following)

            if(isRefresh){
                binding.list.adapter?.notifyDataSetChanged()
            } else {
                binding.list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                binding.list.adapter = MyAdapter()
            }
        }
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position < userFollow.size) {
                0
            } else {
                1
            }
        }
        override fun getItemCount(): Int {
            return userFollow.size + 1
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return when (viewType) {
                0 -> {
                    val binding = ModelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    MyViewHolder(binding)
                }
                1 -> {
                    val binding = MessageTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    MyViewHolder(binding)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> {
                    // 获取模特对象
                    val model = userFollow[position].content
                    val item = holder.binding as ModelItemBinding
                    // 创建点击事件
                    holder.itemView.setOnClickListener {
                        startModelActivity(
                            this@FollowActivity,
                            model.id
                        )
                    }
                    // 更新头像
                    Glide.with(this@FollowActivity)
                        .load("${model.avatar_image}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(item.avatar)
                    // 更新名称
                    val name = when (model.other_name) {
                        null -> model.name
                        else -> model.name + model.other_name
                    }
                    item.name.text = name
                    // 更新写真集数量及更新时间
                    item.number.text = "写真集数量 ${model.count} 套"
                    item.time.text = "关注时间 ${calculateTimeAgo(userFollow[position].time)}"
                    // 更新关注按钮
                    item.following.visibility = View.GONE
                    item.unfollow.visibility = View.VISIBLE
                    item.unfollow.setOnClickListener {
                        createSimpleDialog(
                            this@FollowActivity,
                            "确认取消关注模特 $name 吗?",
                            true
                        ) {
                            setModelFollowing(this@FollowActivity, model, { initPage(true) }, { })
                        }
                    }
                }
                1 -> {
                    val textView = holder.itemView as TextView
                    textView.setPadding(0, 35, 0, 50)
                    textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    textView.text = "已经到底了哦~"
                }
            }
        }
    }
    class MyViewHolder(val binding: ViewBinding): RecyclerView.ViewHolder(binding.root)
}