package com.fixeam.icoser.ui.collection_page

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityMediaFavorBinding
import com.fixeam.icoser.databinding.MediaFavorItemBinding
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startMediaActivity
import com.fixeam.icoser.network.getUserMediaLike
import com.fixeam.icoser.network.userMediaLike
import com.fixeam.icoser.network.userToken

class MediaFavorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaFavorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaFavorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            startLoginActivity(this)
        }

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = getString(R.string.media_favor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 创建内容列表
        binding.imageLoading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.loading))
        binding.imageLoading.visibility = View.VISIBLE
        getUserMediaLike{
            binding.imageLoading.clearAnimation()
            binding.imageLoading.visibility = View.GONE

            binding.list.layoutManager = GridLayoutManager(this, 3)
            val adapter = MyAdapter()
            binding.list.adapter = adapter
        }
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemCount(): Int {
            return userMediaLike.size
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val binding = MediaFavorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val media = userMediaLike[position]
            val item = holder.binding
            Glide.with(this@MediaFavorActivity)
                .load("${media.content.cover}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(item.cover)
            item.description.text = calculateTimeAgo(media.time)
            item.root.setOnClickListener {
                startMediaActivity(this@MediaFavorActivity, isMyFavor = true, startFrom = position)
            }
        }
    }

    class MyViewHolder(var binding: MediaFavorItemBinding): RecyclerView.ViewHolder(binding.root)
}