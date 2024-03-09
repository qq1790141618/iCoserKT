package com.fixeam.icoser.ui.collection_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityCollectionViewBinding
import com.fixeam.icoser.model.CustomArrayAdapter
import com.fixeam.icoser.model.createAlbumCard
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.network.Collection
import com.fixeam.icoser.network.getUserCollection
import com.fixeam.icoser.network.getUserCollectionFold
import com.fixeam.icoser.network.removeUserCollectionFold
import com.fixeam.icoser.network.setAlbumCollection
import com.fixeam.icoser.network.userCollection
import com.fixeam.icoser.network.userCollectionFold
import com.fixeam.icoser.network.userToken
import com.google.android.material.button.MaterialButton

class CollectionViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCollectionViewBinding
    private var isOpenFoldSelect: Boolean = false
    private var selectFoldIndex: Int = 0
    private var selectFoldName: String = ""
    private val selectFoldContent: MutableList<Collection> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            startLoginActivity(this)
        }

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 创建内容列表
        binding.list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.list.adapter = MyAdapter()

        // 安装页面
        initPage()
    }

    private fun initPage(){
        // 设置加载动画
        val imageView = binding.imageLoading
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        binding.toolbar.title = "加载中..."

        // 再次获取用户收藏内容
        getUserCollectionFold(this){
            getUserCollection(this){
                imageView.clearAnimation()
                imageView.visibility = View.GONE
                binding.toolbar.title = getString(R.string.my_collection)

                binding.collectionFoldSelect.text = "默认收藏夹"
                binding.select.setOnClickListener {
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

        binding.collectionFoldSelect.text = when(foldItem.name){
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

        binding.list.adapter?.notifyDataSetChanged()
    }

    private fun openFoldSelect(){
        val openColor = Color.parseColor("#FF7D00")
        binding.collectionFoldSelect.setTextColor(openColor)
        binding.collectionFoldSelectIcon.imageTintList = ColorStateList.valueOf(openColor)
        binding.collectionFoldSelectIcon.setImageResource(R.drawable.triangle_up_fill)
        binding.overlay.visibility = View.VISIBLE
        binding.overlay.setOnClickListener {
            false
        }
        binding.foldContent.visibility = View.VISIBLE

        val allOfFold: MutableList<String> = mutableListOf()
        for (foldItem in userCollectionFold){
            val keyword = when(foldItem.name){
                "default" -> { "默认收藏夹" }
                "like" -> { "我喜欢" }
                else -> { foldItem.name }
            }

            allOfFold.add(keyword)
        }

        val adapter = CustomArrayAdapter(this, android.R.layout.simple_list_item_1, allOfFold.map {
            it }, 12f, 42, true)
        binding.foldContent.adapter = adapter
        adapter.setNotifyOnChange(true)
        binding.foldContent.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            initList(position)
            closeFoldSelect()
        }
        isOpenFoldSelect = true
    }

    private fun closeFoldSelect(){
        var closeColor = Color.parseColor("#000000")
        if(isDarken(this)){
            closeColor = Color.parseColor("#FFFFFF")
        }
        binding.collectionFoldSelect.setTextColor(closeColor)
        binding.collectionFoldSelectIcon.imageTintList = ColorStateList.valueOf(closeColor)
        binding.collectionFoldSelectIcon.setImageResource(R.drawable.triangle_down_fill)
        binding.overlay.visibility = View.GONE
        binding.foldContent.visibility = View.GONE
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
                    createAlbumCard(this@CollectionViewActivity, album, holder.itemView, "normal", true){
                        createSimpleDialog(this@CollectionViewActivity, "确定移除此写真集的收藏吗? ", true){
                            setAlbumCollection(
                                this@CollectionViewActivity,
                                album,
                                callback = {
                                    selectFoldContent.removeAt(position - 1)
                                    binding.list.adapter?.notifyItemRemoved(position)
                                },
                                unLog = { }
                            )
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