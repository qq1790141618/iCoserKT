package com.fixeam.icoser.ui.album_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityAlbumViewBinding
import com.fixeam.icoser.model.bytesToReadableSize
import com.fixeam.icoser.model.getScreenWidth
import com.fixeam.icoser.model.saveImageToGallery
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.shareImageContent
import com.fixeam.icoser.model.shareTextContent
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startMediaActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.FileInfo
import com.fixeam.icoser.network.FileMeta
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.checkForUser
import com.fixeam.icoser.network.openCollectionSelector
import com.fixeam.icoser.network.requestAlbumData
import com.fixeam.icoser.network.requestAlbumImageInfo
import com.fixeam.icoser.network.requestImageInfo
import com.fixeam.icoser.network.setAlbumCollection
import com.fixeam.icoser.network.updateAccessLog
import com.fixeam.icoser.network.uploadImageInfo
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import org.json.JSONArray

class AlbumViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlbumViewBinding
    private var albumInfo: Albums? = null
    private var albumImages: MutableList<String> = mutableListOf()
    private var imageList: List<FileInfo> = listOf()
    private var doNotSetToken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 检查网络状态和用户登录
        checkForUser(this)

        // 设置加载动画
        val imageView = binding.imageLoading
        imageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.loading))
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置悬浮按钮
        binding.toUp.setOnClickListener {
            binding.imageList.smoothScrollToPosition(0)
        }

        // 获取参数执行请求
        doNotSetToken = intent.getBooleanExtra("doNotSetToken", false)
        val id = intent.getIntExtra("id", -1)
        requireAlbumContent(id)
    }

    private var accessLogId: Int = 0

    override fun onPause() {
        updateAccessLog(accessLogId)
        super.onPause()
    }

    override fun onStop() {
        updateAccessLog(accessLogId)
        super.onStop()
    }

    private fun requireAlbumContent(id: Int){
        val condition = JSONArray()
        condition.put(JSONArray().apply {
            put("id")
            put(id.toString())
        })
        requestAlbumData(this, condition.toString(), doNotSetToken){
            val album = it[0]
            albumImages = album.images as MutableList<String>

            accessLog(this@AlbumViewActivity, album.id.toString(), "VISIT_ALBUM"){
                accessLogId = it
            }

            binding.imageLoading.clearAnimation()
            binding.imageLoading.visibility = View.GONE

            binding.toolbar.title = album.name
            binding.toolbar.subtitle = album.model

            if(album.media != null && album.media!!.isNotEmpty()){
                binding.goToVideo.visibility = View.VISIBLE
                binding.goToVideo.setOnClickListener {
                    startMediaActivity(
                        this,
                        albumId = album.id
                    )
                }
            }

            albumInfo = album
            initImageList()
            initMoreOptions()
        }
    }

    private fun initImageList(){
        requestAlbumImageInfo(this, albumImages){ fileInfoList ->
            val urlComparator = compareBy<FileInfo> { fileInfo ->
                albumImages.indexOf(fileInfo.url)
            }
            imageList = fileInfoList.sortedWith(urlComparator)

            val list: RecyclerView = binding.imageList
            list.layoutManager = LinearLayoutManager(this@AlbumViewActivity, LinearLayoutManager.VERTICAL, false)
            val adapter = ListAdapter()
            list.adapter = adapter
            list.setItemViewCacheSize(16)
            list.setHasTransientState(true)

            for ((index, fileItem) in fileInfoList.withIndex()){
                if(fileItem.meta == null){
                    requestImageInfo(this, fileItem.url){
                        val json = Gson().toJson(it)
                        fileItem.meta = json
                        adapter.notifyItemChanged(index)
                        uploadImageInfo(fileItem.url, json)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initMoreOptions(){
        // 显示按钮
        val moreButton = binding.more
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
            val forbidden = dialogView.findViewById<MaterialButton>(R.id.forbidden)
            forbidden.visibility = View.GONE

            // 调整按钮操作
            val share = dialogView.findViewById<MaterialButton>(R.id.share)
            share?.setOnClickListener {
                shareTextContent(
                    context = this,
                    text = "来自iCoser的分享内容：模特 - ${albumInfo?.model}, 写真集 - ${albumInfo?.name}, 访问链接：https://app.fixeam.com/album?id=${albumInfo?.id}"
                )
            }
            val collection = dialogView.findViewById<MaterialButton>(R.id.collection)
            if(doNotSetToken){
                collection.visibility = View.GONE
            }
            if(albumInfo?.is_collection != null){
                collection?.setIconResource(R.drawable.favor_fill)
                collection?.text = getString(R.string.uncollection)
            }
            collection?.setOnClickListener {
                fun unLog(){
                    collection.setIconResource(R.drawable.favor)
                    startLoginActivity(this)
                }

                albumInfo?.let { it1 ->
                    if(it1.is_collection != null){
                        collection.setIconResource(R.drawable.loading2)
                        setAlbumCollection(
                            this,
                            it1,
                            it1.is_collection!!,
                            {
                                collection.setIconResource(R.drawable.favor)
                                it1.is_collection = null
                                collection.text = getString(R.string.collection)
                            },
                            { unLog() }
                        )
                    } else {
                        openCollectionSelector(
                            this,
                            it1,
                            {
                                collection.setIconResource(R.drawable.favor_fill)
                                it1.is_collection = it
                                collection.text = "${getString(R.string.uncollection)}($it)"
                            },
                            { unLog() }
                        )
                    }
                }
            }
            val model = dialogView.findViewById<MaterialButton>(R.id.view_model)
            model?.setOnClickListener {
                albumInfo?.model_id?.let { it1 -> startModelActivity(this, it1, doNotSetToken) }
            }

            alertDialog.show()
        }
    }

    inner class ListAdapter : RecyclerView.Adapter<viewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
            val itemView = LayoutInflater.from(this@AlbumViewActivity).inflate(R.layout.margin_image, parent, false)
            return viewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return albumImages.size
        }
        @SuppressLint("NewApi")
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

    class viewHolder(view: View) : RecyclerView.ViewHolder(view)
}