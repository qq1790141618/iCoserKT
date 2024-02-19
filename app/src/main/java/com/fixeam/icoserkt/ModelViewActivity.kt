package com.fixeam.icoserkt

import GlideBlurTransformation
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ModelViewActivity : AppCompatActivity() {
    private var modelInfo: Models? = null
    private var albumList: MutableList<Albums> = mutableListOf()
    private var isFinished: Boolean = false
    private var albumLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_view)

        val currentTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
            setStatusBarColor(Color.BLACK)
            setStatusBarTextColor(false)
        } else {
            setStatusBarColor(Color.WHITE)
            setStatusBarTextColor(true)
        }

        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView?.startAnimation(animation)
        imageView?.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val id = intent.getIntExtra("id", -1)
        requireModelContent(id)
    }

    private fun requireModelContent(id: Int){
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)

        val condition = JSONArray()
        condition.put(JSONArray().apply {
            put("id")
            put(id.toString())
        })
        var call = ApiService.GetModel(condition.toString())
        if(userToken != null){
            call = ApiService.GetModel(condition.toString(), userToken!!)
        }

        call.enqueue(object : Callback<ModelsResponse> {
            override fun onResponse(call: Call<ModelsResponse>, response: Response<ModelsResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.result) {
                        modelInfo = responseBody.data[0]
                        requireAlbumContent(id, true)
                    }
                }
            }

            override fun onFailure(call: Call<ModelsResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@ModelViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun requireAlbumContent(id: Int, create: Boolean = false){
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        albumLoading = true

        val condition = JSONArray()
        condition.put(JSONArray().apply {
            put("model_id")
            put(id.toString())
        })
        var call = ApiService.GetAlbum(
            condition = condition.toString(),
            start = albumList.size
        )
        if(userToken != null){
            call = ApiService.GetAlbum(
                condition = condition.toString(),
                access_token = userToken!!,
                start = albumList.size
            )
        }

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)

                    if(!albumsResponse.result){
                        return
                    }

                    albumList.addAll(albumsResponse.data)
                    if(albumsResponse.data.size < 20){
                        isFinished = true
                    }

                    val imageView = findViewById<ImageView>(R.id.image_loading)
                    imageView.clearAnimation()
                    imageView.visibility = View.GONE

                    if(create){
                        initModelBox()
                        initAlbumList()
                    } else {
                        updateAlbumList(albumsResponse.data.size)
                    }
                    albumLoading = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@ModelViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initModelBox(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = modelInfo?.name
        if(modelInfo?.other_name != null){
            toolbar.title = "${modelInfo?.name} ${modelInfo?.other_name}"
        }

        if(modelInfo?.avatar_image != null){
            val avatar = findViewById<ImageView>(R.id.avatar)
            Glide.with(this)
                .load("${modelInfo?.avatar_image}/short500px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(RoundedCorners(250))
                .into(avatar)
        }

        if(modelInfo?.tags != null){
            val gson = Gson()
            val listType = object : TypeToken<List<String>>() {}.type
            val stringList: List<String> = gson.fromJson(modelInfo?.tags, listType)

            val linearLayout = findViewById<LinearLayout>(R.id.tag_box)

            stringList.forEach {
                val chip = layoutInflater.inflate(R.layout.tag, linearLayout, false)
                chip.findViewById<TextView>(R.id.text).text = it
                linearLayout.addView(chip)
            }
        }

    }

    private fun initAlbumList(){
        val albumListView: RecyclerView = findViewById(R.id.album_list)
        albumListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        albumListView.adapter = listAdapter()

        albumListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItem + 10 > totalItemCount && !isFinished && !albumLoading) {
                    modelInfo?.id?.let { requireAlbumContent(it) }
                }
            }
        })
    }

    private fun updateAlbumList(loadedNumber: Int){
        val albumListView: RecyclerView? = findViewById(R.id.album_list)
        val adapter = albumListView?.adapter
        adapter?.notifyItemInserted(albumList.size - loadedNumber)
    }

    inner class listAdapter : RecyclerView.Adapter<viewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
            val itemView = LayoutInflater.from(this@ModelViewActivity).inflate(R.layout.album_item, parent, false)
            return viewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return albumList.size
        }
        override fun onBindViewHolder(holder: viewHolder, position: Int) {
            // 修改holder
            val album = albumList[position]
            holder.itemView.setOnClickListener {
                val intent = Intent(this@ModelViewActivity, AlbumViewActivity::class.java)
                intent.putExtra("id", album.id)
                startActivity(intent)
            }

            // 修改海报图
            val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
            Glide.with(this@ModelViewActivity)
                .load("${album.poster}/short1200px")
                .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@ModelViewActivity)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(posterBackground)
            val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
            Glide.with(this@ModelViewActivity)
                .load("${album.poster}/short1200px")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(poster)

            // 修改图片数量
            val picNumber = holder.itemView.findViewById<TextView>(R.id.text)
            picNumber.text = "${(album.images as MutableList<String>).size}P"

            // 修改写真集名
            val name = holder.itemView.findViewById<TextView>(R.id.name)
            name.text = "${album.model} ${album.name}"

            // 添加图片
            val imagePreview = holder.itemView.findViewById<LinearLayout>(R.id.image_preview)
            for ((index, image) in (album.images as MutableList<String>).withIndex()){
                if(index >= 4){
                    break
                }

                val cardView = CardView(this@ModelViewActivity)
                val layoutParams = ViewGroup.MarginLayoutParams(
                    (resources.displayMetrics.density * 36).toInt(), // 设置宽度为屏幕宽度的四分之一
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.rightMargin = (resources.displayMetrics.density * 5).toInt() // 设置右边距
                cardView.layoutParams = layoutParams
                cardView.cardElevation = 0F
                cardView.radius = resources.displayMetrics.density * 3 // 设置圆角半径

                val imageView = ImageView(this@ModelViewActivity)
                imageView.id = View.generateViewId()
                val imageLayoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.layoutParams = imageLayoutParams
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(this@ModelViewActivity)
                    .load("${image}/short500px")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView)

                cardView.addView(imageView)
                if(imagePreview.childCount < 5){
                    imagePreview.addView(cardView, index)
                }
            }
        }
    }

    class viewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun setStatusBarTextColor(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            if (isDark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }
}