package com.fixeam.icoserkt

import GlideBlurTransformation
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendActivity : AppCompatActivity() {
    var displayType = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val type = intent.getIntExtra("type", 0)
        displayType = type
        setTab(type)

        // 获取数据
        requestHotData()
    }

    private fun initViewPage(){
        val viewPager = findViewById<ViewPager2>(R.id.content_viewpage)
        val adapter = ViewPagerAdapter()
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setTab(position)
            }
        })
        viewPager.setCurrentItem(displayType, false)
    }

    inner class ViewPagerAdapter : RecyclerView.Adapter<RecommendActivity.ViewPagerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendActivity.ViewPagerViewHolder {
            val view = RecyclerView(this@RecommendActivity)
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            view.isVerticalScrollBarEnabled = true
            return ViewPagerViewHolder(view)
        }

        override fun getItemCount(): Int {
            return 2
        }

        override fun onBindViewHolder(holder: RecommendActivity.ViewPagerViewHolder, position: Int) {
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
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // 修改holder
            val album = hotData[position]
            holder.itemView.setOnClickListener {
                val intent = Intent(this@RecommendActivity, AlbumViewActivity::class.java)
                intent.putExtra("id", album.id)
                startActivity(intent)
            }

            // 修改海报图
            val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
            Glide.with(this@RecommendActivity)
                .load("${album.poster}/short500px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@RecommendActivity)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(posterBackground)
            val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
            Glide.with(this@RecommendActivity)
                .load("${album.poster}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(poster)

            // 修改图片数量
            val picNumber = holder.itemView.findViewById<TextView>(R.id.text)
            picNumber.text = "${(album.images as MutableList<String>).size}P"

            // 修改写真集名
            val name = holder.itemView.findViewById<TextView>(R.id.name)
            name.text = "${album.model_name} ${album.name}"

            // 修改附加信息
            val more = holder.itemView.findViewById<LinearLayout>(R.id.more)
            more.visibility = View.VISIBLE
            val viewIcon = holder.itemView.findViewById<ImageView>(R.id.view_icon)
            viewIcon.visibility = View.VISIBLE
            val otherInfo = holder.itemView.findViewById<TextView>(R.id.other_info)
            otherInfo.visibility = View.VISIBLE
            otherInfo.text = "${album.count}次浏览"

            // 添加图片
            val imagePreview = holder.itemView.findViewById<LinearLayout>(R.id.image_preview)
            for ((index, image) in (album.images as MutableList<String>).withIndex()){
                if(index >= 4){
                    break
                }

                val cardView = CardView(this@RecommendActivity)
                val layoutParams = ViewGroup.MarginLayoutParams(
                    (resources.displayMetrics.density * 36).toInt(), // 设置宽度为屏幕宽度的四分之一
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.rightMargin = (resources.displayMetrics.density * 5).toInt() // 设置右边距
                cardView.layoutParams = layoutParams
                cardView.cardElevation = 0F
                cardView.radius = resources.displayMetrics.density * 3 // 设置圆角半径

                val imageView = ImageView(this@RecommendActivity)
                imageView.id = View.generateViewId()
                val imageLayoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.layoutParams = imageLayoutParams
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(this@RecommendActivity)
                    .load("${image}/yswidth300px")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView)

                cardView.addView(imageView)
                if(imagePreview.childCount < 5){
                    imagePreview.addView(cardView, index)
                }
            }
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
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // 修改holder
            val album = newsData[position]
            holder.itemView.setOnClickListener {
                val intent = Intent(this@RecommendActivity, AlbumViewActivity::class.java)
                intent.putExtra("id", album.id)
                startActivity(intent)
            }

            // 修改海报图
            val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
            Glide.with(this@RecommendActivity)
                .load("${album.poster}/short500px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@RecommendActivity)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(posterBackground)
            val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
            Glide.with(this@RecommendActivity)
                .load("${album.poster}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(poster)

            // 修改图片数量
            val picNumber = holder.itemView.findViewById<TextView>(R.id.text)
            picNumber.text = "${(album.images as MutableList<String>).size}P"

            // 修改写真集名
            val name = holder.itemView.findViewById<TextView>(R.id.name)
            name.text = "${album.model} ${album.name}"

            // 修改附加信息
            val more = holder.itemView.findViewById<LinearLayout>(R.id.more)
            more.visibility = View.VISIBLE
            val timeIcon = holder.itemView.findViewById<ImageView>(R.id.time_icon)
            timeIcon.visibility = View.VISIBLE
            val otherInfo = holder.itemView.findViewById<TextView>(R.id.other_info)
            otherInfo.visibility = View.VISIBLE
            otherInfo.text = "${calculateTimeAgo(album.create_time)}"

            // 添加图片
            val imagePreview = holder.itemView.findViewById<LinearLayout>(R.id.image_preview)
            for ((index, image) in (album.images as MutableList<String>).withIndex()){
                if(index >= 4){
                    break
                }

                val cardView = CardView(this@RecommendActivity)
                val layoutParams = ViewGroup.MarginLayoutParams(
                    (resources.displayMetrics.density * 36).toInt(), // 设置宽度为屏幕宽度的四分之一
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.rightMargin = (resources.displayMetrics.density * 5).toInt() // 设置右边距
                cardView.layoutParams = layoutParams
                cardView.cardElevation = 0F
                cardView.radius = resources.displayMetrics.density * 3 // 设置圆角半径

                val imageView = ImageView(this@RecommendActivity)
                imageView.id = View.generateViewId()
                val imageLayoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.layoutParams = imageLayoutParams
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(this@RecommendActivity)
                    .load("${image}/yswidth300px")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView)

                cardView.addView(imageView)
                if(imagePreview.childCount < 5){
                    imagePreview.addView(cardView, index)
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun setTab(type: Int){
        val hotButton = findViewById<TextView>(R.id.hot)
        hotButton.setOnClickListener {
            setTab(0)
        }
        val newsButton = findViewById<TextView>(R.id.news)
        newsButton.setOnClickListener {
            setTab(1)
        }

        val viewPager = findViewById<ViewPager2>(R.id.content_viewpage)
        if(type > 1){
            viewPager.setCurrentItem(0, true)
        } else {
            viewPager.setCurrentItem(type, true)
        }

        val large = 17f
        val small = 15f

        when(type){
            0 -> {
                hotButton.textSize = large
                hotButton.typeface = Typeface.DEFAULT_BOLD
                newsButton.textSize = small
                newsButton.typeface = Typeface.DEFAULT
            }
            1 -> {
                newsButton.textSize = large
                newsButton.typeface = Typeface.DEFAULT_BOLD
                hotButton.textSize = small
                hotButton.typeface = Typeface.DEFAULT
            }
            else -> {
                hotButton.textSize = large
                hotButton.typeface = Typeface.DEFAULT_BOLD
                newsButton.textSize = small
                newsButton.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun requestHotData() {
        if(hotData.isNotEmpty()){
            requestNewData()
        }

        val call = ApiNetService.GetHot()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                    if (albumsResponse.result) {
                        hotData = albumsResponse.data.take(30)

                        requestNewData()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@RecommendActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun requestNewData() {
        if(newsData.isNotEmpty()){
            initPageData()
        }

        val call = ApiNetService.GetAlbum(number = 30)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                    if (albumsResponse.result) {
                        newsData = albumsResponse.data.take(30)

                        initPageData()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@RecommendActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initPageData(){

        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        imageView.clearAnimation()
        imageView.visibility = View.GONE

        initViewPage()
    }
}