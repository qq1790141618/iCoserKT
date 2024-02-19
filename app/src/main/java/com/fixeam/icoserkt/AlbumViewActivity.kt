package com.fixeam.icoserkt

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    private var albumInfo: Albums? = null
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
        toolbar.title = "加载中..."
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
        var call = ApiService.GetAlbum(condition.toString())
        if(userToken != null){
            call = ApiService.GetAlbum(condition.toString(), userToken!!)
        }

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

                    albumInfo = album
                    initImageList()
                    initMoreOptions()
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

                        ApiService.GetFileInfoByUrl("${imageList[index].url}?imageInfo").enqueue(object : Callback<FileMeta> {
                            override fun onResponse(call: Call<FileMeta>, response: Response<FileMeta>) {
                                // 处理响应结果
                                val fileMetaItem = response.body()
                                if(fileMetaItem != null){
                                    val json = Gson().toJson(fileMetaItem)
                                    imageList[index].meta = json
                                    adapter.notifyItemChanged(index)

                                    ApiService.UpdateFileMeta(imageList[index].url, json).enqueue(object : Callback<ActionResponse> {
                                        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) { }
                                        override fun onFailure(call: Call<ActionResponse>, t: Throwable) { }
                                    })
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

    private fun initMoreOptions(){
        // 显示按钮
        val moreButton = findViewById<FloatingActionButton>(R.id.more)
        moreButton.visibility = View.VISIBLE

        moreButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView: View = inflater.inflate(R.layout.album_flash_panel, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()

            // 调整面板内容
            dialogView.findViewById<TextView>(R.id.text_info)?.text = "${albumInfo?.model} ${albumInfo?.name}"
            val posterImage = dialogView.findViewById<ImageView>(R.id.poster_info)
            posterImage.layoutParams.height = (resources.displayMetrics.density * 220).toInt()
            posterImage?.let { it1 ->
                Glide.with(this)
                    .load("${albumInfo?.poster}/short1200px")
                    .placeholder(R.drawable.image_holder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(it1)
            }

            // 绑定关闭事件
            val dialogClose = dialogView.findViewById<MaterialButton>(R.id.close)
            dialogClose.setOnClickListener {
                alertDialog.cancel()
            }

            // 隐藏无关按钮
            val viewAlbums = dialogView.findViewById<MaterialButton>(R.id.view_album)
            viewAlbums.visibility = View.GONE
            val forbbiden = dialogView.findViewById<MaterialButton>(R.id.forbidden)
            forbbiden.visibility = View.GONE

            // 调整按钮操作
            val share = dialogView.findViewById<MaterialButton>(R.id.share)
            share?.setOnClickListener {
                shareTextContent(
                    context = this,
                    text = "来自iCoser的分享内容：模特 - ${albumInfo?.model}, 写真集 - ${albumInfo?.name}, 访问链接：https://app.fixeam.com/album?id=${albumInfo?.id}"
                )
            }
            val collection = dialogView.findViewById<MaterialButton>(R.id.collection)
            if(albumInfo?.is_collection != null){
                collection?.setIconResource(R.drawable.favor_fill)
                collection?.text = getString(R.string.uncollection)
            }
            collection?.setOnClickListener {
                collection.setIconResource(R.drawable.loading2)

                fun collectioncallback() {
                    if(albumInfo?.is_collection != null){
                        collection.setIconResource(R.drawable.favor)
                        albumInfo?.is_collection = null
                        collection.text = getString(R.string.collection)
                    } else {
                        collection.setIconResource(R.drawable.favor_fill)
                        albumInfo?.is_collection = "default"
                        collection.text = getString(R.string.uncollection)
                    }
                }
                fun unlog(){
                    collection.setIconResource(R.drawable.favor)
                }

                albumInfo?.let { it1 -> setAlbumCollection(it1, "default", { collectioncallback() }, { unlog() }) }
            }
            val model = dialogView.findViewById<MaterialButton>(R.id.view_model)
            model?.setOnClickListener {
                val intent = Intent(this@AlbumViewActivity, ModelViewActivity::class.java)
                intent.putExtra("id", albumInfo?.model_id)
                startActivity(intent)
            }

            alertDialog.show()
        }
    }

    private fun setAlbumCollection(album: Albums, fold: String, callback: () -> Unit, unlog: () -> Unit){
        if(userToken != null){
            val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(SERVE_HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val ApiService = retrofit.create(ApiService::class.java)
            var call = ApiService.SetCollectionItem(userToken!!, album.id, "album", fold)
            if(album.is_collection != null){
                call = ApiService.RemoveCollectionItem(userToken!!, album.id, "album")
            }

            call.enqueue(object : Callback<ActionResponse> {
                override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()

                        if(!responseBody?.result!!){
                            Toast.makeText(this@AlbumViewActivity, "操作失败", Toast.LENGTH_SHORT).show()
                            return
                        }

                        callback()

                        Toast.makeText(this@AlbumViewActivity, "操作成功", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                    // 处理请求失败的逻辑
                    Toast.makeText(this@AlbumViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            unlog()
            val intent = Intent(this@AlbumViewActivity, LoginActivity::class.java)
            startActivity(intent)
        }
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
            val gson = Gson()

            // 设置长按分享
            holder.itemView.setOnLongClickListener {

                if(image.meta != null) {
                    val builder = AlertDialog.Builder(this@AlbumViewActivity)
                    val inflater =
                        this@AlbumViewActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val dialogView: View = inflater.inflate(R.layout.image_share, null)
                    builder.setView(dialogView)
                    val alertDialog = builder.create()

                    dialogView.setOnClickListener {
                        alertDialog.cancel()
                    }

                    val imageName = dialogView.findViewById<TextView>(R.id.image_name)
                    val fileName = image.url.substringAfterLast("/")
                    imageName.text = fileName

                    val imageItemView = dialogView.findViewById<ImageView>(R.id.image_view)
                    Glide.with(this@AlbumViewActivity)
                        .load(image.url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageItemView)

                    val shareImage = dialogView.findViewById<MaterialButton>(R.id.share)
                    shareImage.setOnClickListener {
                        shareImageContent(
                            imageUrl = image.url,
                            context = this@AlbumViewActivity
                        )
                    }

                    val downloadImage = dialogView.findViewById<MaterialButton>(R.id.download)
                    downloadImage.text = "${downloadImage.text} (${bytesToReadableSize(image.size)})"
                    downloadImage.setOnClickListener {
                        // 下载
                        saveImageToGallery(this@AlbumViewActivity, imageItemView)
                    }

                    alertDialog.show()
                }

                true
            }

            // 设置图片显示的大小
            if(image.meta != null){
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