package com.fixeam.icoser.ui.history_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.R
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.clearUserHistory
import com.fixeam.icoser.network.getUserHistory
import com.fixeam.icoser.network.userForbidden
import com.fixeam.icoser.network.userHistory
import com.fixeam.icoser.network.userHistoryList
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.painter.GlideBlurTransformation
import com.fixeam.icoser.ui.album_page.AlbumViewActivity
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.fixeam.icoser.ui.media_page.MediaViewActivity
import com.fixeam.icoser.ui.model_page.ModelViewActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class HistoryActivity : AppCompatActivity() {
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 创建内容列表
        val list = findViewById<RecyclerView>(R.id.history_list)
        list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val adapter = MyAdapter()
        list.adapter = adapter

        // 创建下拉刷新
        val refreshLayout = findViewById<SmartRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            getUserHistory(this, false){
                adapter.notifyDataSetChanged()
                refreshLayout.finishRefresh()
                initTimeRange()
                Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
            }
        }
        refreshLayout.setOnLoadMoreListener {
            val startIndex = userHistoryList.size
            getUserHistory(this, false){
                adapter.notifyItemInserted(startIndex + 1)
                refreshLayout.finishLoadMore()
            }
        }

        // 设置悬浮按钮
        val toUpButton: FloatingActionButton = findViewById(R.id.to_up)
        toUpButton.setOnClickListener {
            list.smoothScrollToPosition(0)
        }
        val clearButton: FloatingActionButton = findViewById(R.id.clear_history)
        clearButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("确认清除所有的浏览历史记录吗?")

            builder.setPositiveButton("确定") { _, _ ->
                clearUserHistory(this){
                    if(it){
                        getUserHistory(this, false){
                            adapter.notifyDataSetChanged()
                            initTimeRange()
                        }
                    }
                }
            }
            builder.setNegativeButton("取消") { _, _ -> }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        // 安装页面
        initPage()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initPage(){
        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "加载中..."

        // 再次获取用户收藏内容
        getUserHistory(this){
            imageView.clearAnimation()
            imageView.visibility = View.GONE
            toolbar.title = getString(R.string.my_history)

            val list = findViewById<RecyclerView>(R.id.history_list)
            list.adapter?.notifyDataSetChanged()
            initTimeRange()
        }
    }

    data class RangeIndex(
        var start: Int = 0,
        var end: Int = 0
    )

    @SuppressLint("SetTextI18n")
    private fun initTimeRange(){
        val list = findViewById<RecyclerView>(R.id.history_list)
        val timeRangeView = findViewById<LinearLayout>(R.id.time_range_view)

        val timeRangePosition: MutableList<RangeIndex> = mutableListOf(
            RangeIndex(),
            RangeIndex(),
            RangeIndex(),
            RangeIndex()
        )

        for ((index, _) in userHistory?.time_range!!.withIndex()){
            val position = 3 - index
            val timeRange = userHistory?.time_range!![position]
            val timeRangePositionItem = timeRangePosition[index]

            if(index == 0){
                timeRangePositionItem.start = 0
                timeRangePositionItem.end = timeRange.count
            } else {
                timeRangePositionItem.start = timeRangePosition[index - 1].end
                if(timeRangePosition[index - 1].end > 0){
                    timeRangePositionItem.start ++
                }
                timeRangePositionItem.end = timeRangePositionItem.start + timeRange.count
            }

            val textView = timeRangeView.getChildAt(index) as TextView
            textView.visibility = View.VISIBLE
            textView.text = timeRange.time_range + "\n" + timeRange.count
            textView.setOnClickListener {
                if(userHistoryList.size < timeRangePositionItem.start){
                    val startIndex = userHistoryList.size
                    getUserHistory(this, false, timeRangePositionItem.end){
                        list.adapter?.notifyItemInserted(startIndex + 1)
                        list.smoothScrollToPosition(timeRangePositionItem.start)
                    }
                } else {
                    list.smoothScrollToPosition(timeRangePositionItem.start)
                }
            }
        }

        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                initMenuBlock(recyclerView, timeRangePosition)
            }
        })
        initMenuBlock(list, timeRangePosition)
    }

    private fun initMenuBlock(recyclerView: RecyclerView, timeRangePosition: MutableList<RangeIndex>){
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        for ((index, timeRangePositionItem) in timeRangePosition.withIndex()){
            if(firstVisibleItemPosition == timeRangePositionItem.start){
                moveMenuBlock(index)
            }
        }
    }

    private fun moveMenuBlock(index: Int){
        val menuBlock = findViewById<ConstraintLayout>(R.id.menu_block)
        val menuBlockHeight = menuBlock.height
        val moveDistance = menuBlockHeight * index
        menuBlock.animate().translationY(moveDistance.toFloat()).setDuration(300).start()
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemCount(): Int {
            return userHistoryList.size
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
            return MyViewHolder(view)
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val historyItem = userHistoryList[position]
            val idDouble: Double = historyItem.content["id"] as Double
            val id = idDouble.toInt()

            holder.itemView.setOnClickListener {
                val intent = when(historyItem.type){
                    "VISIT_ALBUM" -> {
                        Intent(this@HistoryActivity, AlbumViewActivity::class.java)
                    }
                    "VISIT_MODEL" -> {
                        Intent(this@HistoryActivity, ModelViewActivity::class.java)
                    }
                    "VISIT_MEDIA" -> {
                        Intent(this@HistoryActivity, MediaViewActivity::class.java)
                    }
                    else -> {
                        Intent(this@HistoryActivity, AlbumViewActivity::class.java)
                    }
                }
                intent.putExtra("id", id)
                for (forbid in userForbidden){
                    if(forbid.res_id.toInt() == id){
                        intent.putExtra("doNotSetToken", true)
                    }
                }
                startActivity(intent)
            }

            val imageUrl = when(historyItem.type){
                "VISIT_ALBUM" -> historyItem.content["poster"].toString()
                "VISIT_MODEL" -> historyItem.content["avatar_image"].toString()
                "VISIT_MEDIA" -> historyItem.content["cover"].toString()
                else -> ""
            }
            val tagText = when(historyItem.type){
                "VISIT_ALBUM" -> "写真集"
                "VISIT_MODEL" -> "模特"
                "VISIT_MEDIA" -> "小视频"
                else -> "其他"
            }
            val nameText = when(historyItem.type){
                "VISIT_ALBUM" -> historyItem.content["model_name"].toString() + " " + historyItem.content["name"].toString()
                "VISIT_MODEL" -> historyItem.content["name"].toString()
                "VISIT_MEDIA" -> historyItem.content["model_name"].toString() + " " + historyItem.content["name"].toString()
                else -> "其他"
            }

            val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
            Glide.with(this@HistoryActivity)
                .load("${imageUrl}/short500px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@HistoryActivity)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(posterBackground)
            val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
            Glide.with(this@HistoryActivity)
                .load("${imageUrl}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(poster)

            val tagTextView = holder.itemView.findViewById<TextView>(R.id.text)
            tagTextView.text = tagText
            val nameTextView = holder.itemView.findViewById<TextView>(R.id.name)
            nameTextView.text = nameText
            val timeTextView = holder.itemView.findViewById<TextView>(R.id.time)
            timeTextView.text = historyItem.time

            val removeButton = holder.itemView.findViewById<ImageView>(R.id.close)
            removeButton.setOnClickListener {
                clearUserHistory(this@HistoryActivity, historyItem.id){
                    if(it){
                        userHistoryList.removeAt(position)
                        val list = findViewById<RecyclerView>(R.id.history_list)
                        list.adapter?.notifyItemRemoved(position)
                    }
                }
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}