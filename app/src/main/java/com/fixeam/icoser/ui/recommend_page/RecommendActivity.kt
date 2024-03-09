package com.fixeam.icoser.ui.recommend_page

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityRecommendBinding
import com.fixeam.icoser.model.createAlbumCard
import com.fixeam.icoser.model.hotData
import com.fixeam.icoser.model.newsData
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.requestHotData
import com.fixeam.icoser.network.requestNewData

class RecommendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecommendBinding
    private var displayType = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecommendBinding.inflate(layoutInflater)
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

        val type = intent.getIntExtra("type", 0)
        displayType = type
        setTab(type)

        // 获取数据
        setData()
    }

    private fun initViewPage(){
        val viewPager = binding.contentViewpage
        val adapter = ViewPagerAdapter()
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setTab(position)
            }
        })
        viewPager.setCurrentItem(displayType, false)
    }

    inner class ViewPagerAdapter : RecyclerView.Adapter<ViewPagerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
            val view = RecyclerView(this@RecommendActivity)
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            view.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            return ViewPagerViewHolder(view)
        }

        override fun getItemCount(): Int {
            return 2
        }

        override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
            val item = holder.itemView as RecyclerView
            item.layoutManager = LinearLayoutManager(this@RecommendActivity, LinearLayoutManager.VERTICAL, false)
            when(position){
                0 -> {
                    item.adapter = HotAdapter()
                }
                1 -> {
                    item.adapter = NewsAdapter()
                }
            }
        }
    }

    inner class ViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class HotAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(this@RecommendActivity).inflate(R.layout.album_item, parent, false)
            return ViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return hotData.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val album = hotData[position]
            createAlbumCard(this@RecommendActivity, album, holder.itemView, "hot")
        }
    }

    inner class NewsAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(this@RecommendActivity).inflate(R.layout.album_item, parent, false)
            return ViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return newsData.size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val album = newsData[position]
            createAlbumCard(this@RecommendActivity, album, holder.itemView, "new")
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun setTab(type: Int){
        binding.hot.setOnClickListener {
            setTab(0)
        }
        binding.news.setOnClickListener {
            setTab(1)
        }

        if(type > 1){
            binding.contentViewpage.setCurrentItem(0, true)
        } else {
            binding.contentViewpage.setCurrentItem(type, true)
        }

        val large = 17f
        val small = 15f

        when(type){
            0 -> {
                binding.hot.textSize = large
                binding.hot.typeface = Typeface.DEFAULT_BOLD
                binding.news.textSize = small
                binding.news.typeface = Typeface.DEFAULT
            }
            1 -> {
                binding.news.textSize = large
                binding.news.typeface = Typeface.DEFAULT_BOLD
                binding.hot.textSize = small
                binding.hot.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun setData() {
        if(hotData.isNotEmpty()){
            requestNewData(this){
                initPageData()
            }
            return
        }
        requestHotData(this){
            requestNewData(this){
                initPageData()
            }
        }
    }

    private fun initPageData(){
        val imageView =  binding.imageLoading
        imageView.clearAnimation()
        imageView.visibility = View.GONE
        initViewPage()
    }
}