package com.fixeam.icoser.ui.collection_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.R
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.Collection
import com.fixeam.icoser.network.getUserCollection
import com.fixeam.icoser.network.getUserCollectionFold
import com.fixeam.icoser.network.removeUserCollectionFold
import com.fixeam.icoser.network.setAlbumCollection
import com.fixeam.icoser.network.userCollection
import com.fixeam.icoser.network.userCollectionFold
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.painter.GlideBlurTransformation
import com.fixeam.icoser.ui.album_page.AlbumViewActivity
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton

class CollectionViewActivity : AppCompatActivity() {
    private var isOpenFoldSelect: Boolean = false
    private var selectFoldIndex: Int = 0
    private var selectFoldName: String = ""
    private val selectFoldContent: MutableList<Collection> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_view)

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
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        list.adapter = MyAdapter()

        // 安装页面
        initPage()
    }

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
        getUserCollectionFold(this){
            getUserCollection(this){
                imageView.clearAnimation()
                imageView.visibility = View.GONE
                toolbar.title = getString(R.string.my_collection)

                val collectionFoldSelect = findViewById<TextView>(R.id.collection_fold_select)
                collectionFoldSelect.text = "默认收藏夹"
                val foldSelect = findViewById<LinearLayout>(R.id.select)
                foldSelect.setOnClickListener {
                    if(isOpenFoldSelect){
                        closeFoldSelect()
                    } else {
                        openFoldSelect()
                    }
                }
                initList(selectFoldIndex)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initList(index: Int){
        val foldItem = userCollectionFold[index]
        selectFoldIndex = index
        selectFoldName = foldItem.name
        
        val collectionFoldSelect = findViewById<TextView>(R.id.collection_fold_select)
        collectionFoldSelect.text = when(foldItem.name){
            "default" -> { "默认收藏夹" }
            "like" -> { "我喜欢" }
            else -> { foldItem.name }
        }

        selectFoldContent.clear()
        for (collect in userCollection){
            if(collect.fold == foldItem.name){
                selectFoldContent.add(collect)
            }
        }

        val list = findViewById<RecyclerView>(R.id.list)
        list.adapter?.notifyDataSetChanged()
    }

    private fun openFoldSelect(){
        val collectionFoldSelect = findViewById<TextView>(R.id.collection_fold_select)
        val collectionFoldSelectIcon = findViewById<ImageView>(R.id.collection_fold_select_icon)
        val overlay = findViewById<View>(R.id.overlay)
        val foldContent = findViewById<FlexboxLayout>(R.id.fold_content)

        val openColor = Color.parseColor("#FF7D00")
        collectionFoldSelect.setTextColor(openColor)
        collectionFoldSelectIcon.imageTintList = ColorStateList.valueOf(openColor)
        collectionFoldSelectIcon.setImageResource(R.drawable.triangle_up_fill)

        overlay.visibility = View.VISIBLE
        foldContent.visibility = View.VISIBLE
        foldContent.removeAllViews()

        for ((index, foldItem) in userCollectionFold.withIndex()){
            val tag = layoutInflater.inflate(R.layout.tag, foldContent, false) as CardView
            val text = tag.findViewById<TextView>(R.id.text)
            val keyword = when(foldItem.name){
                "default" -> { "默认收藏夹" }
                "like" -> { "我喜欢" }
                else -> { foldItem.name }
            }
            text.text = keyword
            text.textSize = 12f

            if(index != selectFoldIndex){
                tag.setOnClickListener {
                    initList(index)
                    closeFoldSelect()
                }
            } else {
                tag.setCardBackgroundColor(openColor)
                text.setTextColor(Color.WHITE)
            }

            foldContent.addView(tag)
        }

        isOpenFoldSelect = true
    }

    private fun closeFoldSelect(){
        val collectionFoldSelect = findViewById<TextView>(R.id.collection_fold_select)
        val collectionFoldSelectIcon = findViewById<ImageView>(R.id.collection_fold_select_icon)
        val overlay = findViewById<View>(R.id.overlay)
        val foldContent = findViewById<FlexboxLayout>(R.id.fold_content)

        var closeColor = Color.parseColor("#000000")
        if(isDarken(this)){
            closeColor = Color.parseColor("#FFFFFF")
        }
        collectionFoldSelect.setTextColor(closeColor)
        collectionFoldSelectIcon.imageTintList = ColorStateList.valueOf(closeColor)
        collectionFoldSelectIcon.setImageResource(R.drawable.triangle_down_fill)

        overlay.visibility = View.GONE
        foldContent.visibility = View.GONE

        isOpenFoldSelect = false
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                0
            } else if (position <= selectFoldContent.size) {
                1
            } else {
                2
            }
        }
        override fun getItemCount(): Int {
            return selectFoldContent.size + 2
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return when (viewType) {
                0 -> {
                    val view = MaterialButton(this@CollectionViewActivity)
                    view.text = "移除此收藏夹"
                    view.textSize = 13F
                    view.setTextColor(Color.WHITE)
                    view.setBackgroundColor(Color.parseColor("#F76560"))

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(50, 50, 50, 0)
                    view.layoutParams = layoutParams

                    MyViewHolder(view)
                }
                1 -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.album_item, parent, false)
                    MyViewHolder(view)
                }
                2 -> {
                    val view = TextView(this@CollectionViewActivity)
                    MyViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> {
                    val builder = AlertDialog.Builder(this@CollectionViewActivity)
                    val selectFold = userCollectionFold[selectFoldIndex]

                    if(selectFold.name == "default" || selectFold.name == "like"){
                        holder.itemView.setOnClickListener {
                            builder.setMessage("此收藏夹不可被移除!")
                            builder.setPositiveButton("确定") { _, _ -> }
                            val alertDialog = builder.create()
                            alertDialog.show()
                        }
                    } else {
                        holder.itemView.setOnClickListener {
                            builder.setMessage("确定移除此收藏夹吗? 收藏夹内所有内容将会被清空!")
                            builder.setPositiveButton("确定") { _, _ ->
                                removeUserCollectionFold(this@CollectionViewActivity, selectFold.id){
                                    initPage()
                                }
                            }
                            builder.setNegativeButton("取消") { _, _ -> }
                            val alertDialog = builder.create()
                            alertDialog.show()
                        }
                    }
                }
                1 -> {
                    val collectionItem = selectFoldContent[position - 1]
                    val album = collectionItem.content
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@CollectionViewActivity, AlbumViewActivity::class.java)
                        intent.putExtra("id", album.id)
                        startActivity(intent)
                    }

                    // 设置删除事件
                    val removeButton =  holder.itemView.findViewById<ImageView>(R.id.close)
                    removeButton.visibility = View.VISIBLE
                    removeButton.setOnClickListener {
                        val builder = AlertDialog.Builder(this@CollectionViewActivity)
                        builder.setMessage("确定移除此写真集的收藏吗? ")
                        builder.setPositiveButton("确定") { _, _ ->
                            setAlbumCollection(
                                this@CollectionViewActivity,
                                album,
                                callback = {
                                    selectFoldContent.removeAt(position - 1)
                                    val list = findViewById<RecyclerView>(R.id.list)
                                    list.adapter?.notifyItemRemoved(position)
                                },
                                unLog = { }
                            )
                        }
                        builder.setNegativeButton("取消") { _, _ -> }
                        val alertDialog = builder.create()
                        alertDialog.show()
                    }

                    // 修改海报图
                    val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
                    Glide.with(this@CollectionViewActivity)
                        .load("${album.poster}/short500px")
                        .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@CollectionViewActivity)))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(posterBackground)
                    val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
                    Glide.with(this@CollectionViewActivity)
                        .load("${album.poster}/short1200px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(poster)

                    // 修改图片数量
                    val picNumber = holder.itemView.findViewById<TextView>(R.id.text)
                    picNumber.text = "${(album.images as MutableList<String>).size}P"

                    // 修改写真集名
                    val name = holder.itemView.findViewById<TextView>(R.id.name)
                    name.text = "${album.model_name} ${album.name}"

                    // 添加图片
                    val imagePreview = holder.itemView.findViewById<LinearLayout>(R.id.image_preview)
                    imagePreview.removeAllViews()
                    for ((index, image) in (album.images as MutableList<String>).withIndex()){
                        if(index >= 4){
                            break
                        }

                        val cardView = CardView(this@CollectionViewActivity)
                        val layoutParams = ViewGroup.MarginLayoutParams(
                            (resources.displayMetrics.density * 36).toInt(), // 设置宽度为屏幕宽度的四分之一
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        layoutParams.rightMargin = (resources.displayMetrics.density * 5).toInt() // 设置右边距
                        cardView.layoutParams = layoutParams
                        cardView.cardElevation = 0F
                        cardView.radius = resources.displayMetrics.density * 3 // 设置圆角半径

                        val imageView = ImageView(this@CollectionViewActivity)
                        imageView.id = View.generateViewId()
                        val imageLayoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        imageView.layoutParams = imageLayoutParams
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                        Glide.with(this@CollectionViewActivity)
                            .load("${image}/yswidth300px")
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView)

                        cardView.addView(imageView)
                        if(imagePreview.childCount < 5){
                            imagePreview.addView(cardView, index)
                        }
                    }

                }
                2 -> {
                    val textView = holder.itemView as TextView
                    textView.setPadding(0, 35, 0, 50)
                    textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    if(selectFoldContent.size > 0){
                        textView.text = "已经到底了哦~"
                    } else {
                        textView.text = "此收藏夹下没有内容~"
                    }
                }
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}