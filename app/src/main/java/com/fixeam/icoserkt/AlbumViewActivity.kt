package com.fixeam.icoserkt

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlbumViewActivity : AppCompatActivity() {
    private var albumImages: MutableList<String> = mutableListOf()
    private var imageList: List<FileInfo> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_view)

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
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置悬浮按钮
        val toUpButton: FloatingActionButton = findViewById(R.id.to_up)
        val list: RecyclerView = findViewById(R.id.image_list)
        toUpButton.setOnClickListener {
            list.smoothScrollToPosition(0)
        }

        val id = intent.getIntExtra("id", -1)
        requireAlbumContent(id)
    }

    private fun requireAlbumContent(id: Int){
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)

        var condition = JSONArray()
        condition.put(JSONArray().apply {
            put("id")
            put(id.toString())
        })
        val call = ApiService.GetAlbum(condition.toString())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)

                    if(!albumsResponse.result){
                        return
                    }

                    val album: Albums = albumsResponse.data[0]
                    albumImages = album.images as MutableList<String>

                    val imageView = findViewById<ImageView>(R.id.image_loading)
                    imageView.clearAnimation()
                    imageView.visibility = View.GONE

                    val toolbar: Toolbar = findViewById(R.id.toolbar)
                    toolbar.title = album.name
                    toolbar.subtitle = album.model

                    if(album.media != null){
                        val goToVideo = findViewById<MaterialButton>(R.id.go_to_video)
                        goToVideo.visibility = View.VISIBLE
                    }

                    initImageList()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@AlbumViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initImageList(){
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)

        val urls = Gson().toJson(albumImages)
        val urlRequestBody = UrlRequestBody(urls)
        val call = ApiService.PostFileAndInformation(urlRequestBody)

        call.enqueue(object : Callback<List<FileInfo>> {
            override fun onResponse(call: Call<List<FileInfo>>, response: Response<List<FileInfo>>) {
                // 处理响应结果
                val fileInfoList = response.body()
                if (fileInfoList != null) {
                    val urlComparator = compareBy<FileInfo> { fileInfo ->
                        albumImages.indexOf(fileInfo.url)
                    }
                    imageList = fileInfoList.sortedWith(urlComparator)

                    val list: RecyclerView = findViewById(R.id.image_list)
                    list.layoutManager = LinearLayoutManager(this@AlbumViewActivity, LinearLayoutManager.VERTICAL, false)
                    val adapter = listAdapter()
                    list.adapter = adapter
                    list.setItemViewCacheSize(16)
                    list.setHasTransientState(true)

                    for ((index, _) in fileInfoList.withIndex()){
                        if(imageList[index].meta != null){
                            continue
                        }

                        val retrofit = Retrofit.Builder()
                            .client(client)
                            .baseUrl(SERVE_HOST)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        val ApiService = retrofit.create(com.fixeam.icoserkt.ApiService::class.java)
                        val call = ApiService.GetFileInfoByUrl("${imageList[index].url}?imageInfo")

                        call.enqueue(object : Callback<FileMeta> {
                            override fun onResponse(call: Call<FileMeta>, response: Response<FileMeta>) {
                                // 处理响应结果
                                val fileMetaItem = response.body()
                                if(fileMetaItem != null){
                                    imageList[index].meta = Gson().toJson(fileMetaItem)
                                    adapter.notifyItemChanged(index)
                                }
                            }

                            override fun onFailure(call: Call<FileMeta>, t: Throwable) {
                                // 处理请求失败
                                Toast.makeText(this@AlbumViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onFailure(call: Call<List<FileInfo>>, t: Throwable) {
                // 处理请求失败
                Toast.makeText(this@AlbumViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
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

    inner class listAdapter : RecyclerView.Adapter<viewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
            val itemView = LayoutInflater.from(this@AlbumViewActivity).inflate(R.layout.margin_image, parent, false)
            return viewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return albumImages.size
        }
        override fun onBindViewHolder(holder: viewHolder, position: Int) {
            // 修改holder
            val image = imageList[position]
            val imageView = holder.itemView.findViewById<ImageView>(R.id.image_content)
            val imageDisplayWidth = getScreenWidth(this@AlbumViewActivity) - (resources.displayMetrics.density * 12 * 2).toInt()

            // 设置图片显示的大小
            if(image.meta != null){
                val gson = Gson()
                val fileMeta = gson.fromJson(image.meta, FileMeta::class.java)

                val imageDisplayHeight = imageDisplayWidth.toFloat() / fileMeta.width.toInt() * fileMeta.height.toInt()
                imageView.layoutParams = FrameLayout.LayoutParams(
                    imageDisplayWidth,
                    imageDisplayHeight.toInt()
                )

                Glide.with(this@AlbumViewActivity)
                    .load(image.url + "/short1200px")
                    .placeholder(R.drawable.image_holder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(
                        imageDisplayWidth,
                        imageDisplayHeight.toInt()
                    )
                    .into(imageView)
            }
        }
    }
    class viewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }
}