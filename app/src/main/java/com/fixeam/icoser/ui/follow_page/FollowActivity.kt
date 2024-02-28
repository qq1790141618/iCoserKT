package com.fixeam.icoser.ui.follow_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.R
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.Models
import com.fixeam.icoser.network.getUserFollow
import com.fixeam.icoser.network.getUserForbidden
import com.fixeam.icoser.network.removeForbidden
import com.fixeam.icoser.network.setModelFollowing
import com.fixeam.icoser.network.userFollow
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.painter.GlideBlurTransformation
import com.fixeam.icoser.ui.album_page.AlbumViewActivity
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.fixeam.icoser.ui.model_page.ModelViewActivity
import com.google.android.material.button.MaterialButton

class FollowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        initPage()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initPage(isRefresh: Boolean = false){
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
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 再次获取用户关注
        getUserFollow(this){
            imageView.clearAnimation()
            imageView.visibility = View.GONE
            toolbar.title = getString(R.string.my_following)

            val list = findViewById<RecyclerView>(R.id.list)
            if(isRefresh){
                list.adapter?.notifyDataSetChanged()
            } else {
                list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                list.adapter = MyAdapter()
            }
        }
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position < userFollow.size) {
                0
            } else {
                2
            }
        }
        override fun getItemCount(): Int {
            return userFollow.size + 1
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return when (viewType) {
                0 -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.model_item, parent, false)
                    MyViewHolder(view)
                }
                2 -> {
                    val view = TextView(this@FollowActivity)
                    MyViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> {
                    val model = userFollow[position].content

                    // 创建点击事件
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@FollowActivity, ModelViewActivity::class.java)
                        intent.putExtra("id", model.id)
                        startActivity(intent)
                    }

                    // 更新头像
                    val avatar = holder.itemView.findViewById<ImageView>(R.id.avatar)
                    Glide.with(this@FollowActivity)
                        .load("${model.avatar_image}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(avatar)

                    // 更新名称
                    val name = holder.itemView.findViewById<TextView>(R.id.name)
                    if (model.other_name != null) {
                        name.text = model.name + model.other_name
                    } else {
                        name.text = model.name
                    }

                    // 更新写真集数量及更新时间
                    val number = holder.itemView.findViewById<TextView>(R.id.number)
                    number.text = "写真集数量 ${model.count} 套"
                    val time = holder.itemView.findViewById<TextView>(R.id.time)
                    time.text = "关注时间 ${calculateTimeAgo(userFollow[position].time)}"

                    // 更新关注按钮
                    val following = holder.itemView.findViewById<MaterialButton>(R.id.following)
                    val unfollow = holder.itemView.findViewById<MaterialButton>(R.id.unfollow)
                    following.visibility = View.GONE
                    unfollow.visibility = View.VISIBLE

                    unfollow.setOnClickListener {
                        val builder = AlertDialog.Builder(this@FollowActivity)
                        builder.setMessage("确认取消关注模特 ${name.text} 吗?")

                        builder.setPositiveButton("确定") { _, _ ->
                            setModelFollowing(this@FollowActivity, model, { initPage(true) }, { })
                        }
                        builder.setNegativeButton("取消") { _, _ -> }

                        val alertDialog = builder.create()
                        alertDialog.show()
                    }
                }
                2 -> {
                    val textView = holder.itemView as TextView
                    textView.setPadding(0, 35, 0, 50)
                    textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    textView.text = "已经到底了哦~"
                }
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}