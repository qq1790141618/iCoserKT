package com.fixeam.icoser.ui.history_page

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityHistoryBinding
import com.fixeam.icoser.databinding.HistoryItemBinding
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startMediaActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.clearUserHistory
import com.fixeam.icoser.network.getUserHistory
import com.fixeam.icoser.network.userForbidden
import com.fixeam.icoser.network.userHistory
import com.fixeam.icoser.network.userHistoryList
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.painter.GlideBlurTransformation

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
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
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 创建内容列表
        binding.historyList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val adapter = MyAdapter()
        binding.historyList.adapter = adapter

        // 创建下拉刷新
        val refreshLayout = binding.refreshLayout
        binding.refreshLayout.setOnRefreshListener {
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
        binding.toUp.setOnClickListener { binding.historyList.smoothScrollToPosition(0) }
        binding.clearHistory.setOnClickListener {
            createSimpleDialog(this, "确认清除所有的浏览历史记录吗?", true){
                clearUserHistory(this){
                    if(it){
                        getUserHistory(this, false){
                            adapter.notifyDataSetChanged()
                            initTimeRange()
                        }
                    }
                }
            }
        }

        // 安装页面
        initPage()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initPage(){
        // 设置加载动画
        val imageView = binding.imageLoading
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE
        binding.toolbar.title = "加载中..."

        // 再次获取用户收藏内容
        getUserHistory(this){
            imageView.clearAnimation()
            imageView.visibility = View.GONE
            binding.toolbar.title = getString(R.string.my_history)

            binding.historyList.adapter?.notifyDataSetChanged()
            initTimeRange()
        }
    }

    data class RangeIndex(
        var start: Int = 0,
        var end: Int = 0
    )

    @SuppressLint("SetTextI18n")
    private fun initTimeRange(){
        val list = binding.historyList

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

            val textView = binding.timeRangeView.getChildAt(index) as TextView
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
        val menuBlockHeight = binding.menuBlock.menuBlock.height
        val moveDistance = menuBlockHeight * index
        binding.menuBlock.menuBlock.animate().translationY(moveDistance.toFloat()).setDuration(300).start()
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemCount(): Int {
            return userHistoryList.size
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val historyItem = userHistoryList[position]
            val item = holder.binding
            val id = (historyItem.content["id"] as Double).toInt()

            holder.itemView.setOnClickListener {
                var doNotSetToken = false
                for (forbid in userForbidden){
                    if(forbid.res_id.toInt() == id){
                        doNotSetToken = true
                    }
                }
                when(historyItem.type){
                    "VISIT_ALBUM" -> startAlbumActivity(this@HistoryActivity, id, doNotSetToken)
                    "VISIT_MODEL" -> startModelActivity(this@HistoryActivity, id, doNotSetToken)
                    "VISIT_MEDIA" -> startMediaActivity(this@HistoryActivity, id)
                }
            }

            val imageUrl = when(historyItem.type){
                "VISIT_ALBUM" -> historyItem.content["poster"]
                "VISIT_MODEL" -> historyItem.content["avatar_image"]
                "VISIT_MEDIA" -> historyItem.content["cover"]
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

            Glide.with(this@HistoryActivity)
                .load("${imageUrl}/short500px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@HistoryActivity)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(item.posterBackground)
            Glide.with(this@HistoryActivity)
                .load("${imageUrl}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(item.poster)

            item.tag.text.text = tagText
            item.name.text = nameText
            item.time.text = historyItem.time

            item.close.setOnClickListener {
                clearUserHistory(this@HistoryActivity, historyItem.id){
                    if(it){
                        userHistoryList.removeAt(position)
                        binding.historyList.adapter?.notifyItemRemoved(position)
                    }
                }
            }
        }
    }

    class MyViewHolder(var binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root)
}