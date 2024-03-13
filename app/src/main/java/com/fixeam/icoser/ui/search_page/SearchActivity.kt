package com.fixeam.icoser.ui.search_page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivitySearchBinding
import com.fixeam.icoser.databinding.AlbumItemBinding
import com.fixeam.icoser.databinding.MessageTextBinding
import com.fixeam.icoser.databinding.ModelItemBinding
import com.fixeam.icoser.model.CustomArrayAdapter
import com.fixeam.icoser.model.calculateTimeAgo
import com.fixeam.icoser.model.createAlbumBinding
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.hotData
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.Models
import com.fixeam.icoser.network.requestAlbumSearch
import com.fixeam.icoser.network.requestHotData
import com.fixeam.icoser.network.requestHotSearchKeyword
import com.fixeam.icoser.network.requestModelSearch
import com.fixeam.icoser.network.setModelFollowing
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private var hotViewList: List<Albums> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置热门搜索数据
        setHotData()
        initHistory()

        // 设置搜索按钮点击事件
        binding.searchButton.setOnClickListener { search() }

        // 设置预搜索词
        setSearchPlaceHolder()
    }

    // 设置热门搜索和热门数据
    private fun setSearchPlaceHolder(){
        requestHotSearchKeyword(this){
            binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    search()
                    return@setOnEditorActionListener true
                }
                false
            }
            val currentKeywordIndex = (1..20).random()
            binding.searchInput.hint = Editable.Factory.getInstance().newEditable(it[currentKeywordIndex % it.size])

            val hotSearchTitle =  binding.searchBefore.hotSearchTitle
            hotSearchTitle.typeface = Typeface.createFromAsset(assets, "font/ZiTiQuanXinYiGuanHeiTi4.0-2.ttf")
            hotSearchTitle.visibility = View.VISIBLE

            val hotSearch = binding.searchBefore.hotSearch
            hotSearch.visibility = View.VISIBLE

            for (keyword in it) {
                val tag = layoutInflater.inflate(R.layout.tag, hotSearch, false) as CardView
                val text = tag.findViewById<TextView>(R.id.text)
                text.text = keyword
                text.textSize = 12f
                tag.radius = 50f
                tag.setCardBackgroundColor(Color.parseColor("#ff9285"))
                text.setTextColor(Color.WHITE)

                tag.setOnClickListener {
                    val searchInput = findViewById<TextInputEditText>(R.id.search_input)
                    searchInput.text = Editable.Factory.getInstance().newEditable(keyword)
                    search()
                }

                hotSearch.addView(tag)
            }
        }
    }
    private fun setHotData() {
        if(hotData.isNotEmpty()){
            initHotViewList()
            return
        }
        requestHotData(this){
            initHotViewList()
        }
    }
    private fun initHotViewList(){
        hotViewList = hotData.take(8)

        val hotViewTitle = binding.searchBefore.hotViewTitle
        val typeface = Typeface.createFromAsset(assets, "font/ZiTiQuanXinYiGuanHeiTi4.0-2.ttf")
        hotViewTitle.typeface = typeface
        hotViewTitle.visibility = View.VISIBLE

        val listView = binding.searchBefore.hotView
        val adapter = CustomArrayAdapter(this, android.R.layout.simple_list_item_1, hotViewList.map {
            "${it.model_name} ${it.name}" }, 12f, 42)
        listView.adapter = adapter
        adapter.setNotifyOnChange(true)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> startAlbumActivity(this, hotViewList[position].id) }
    }

    // 获取和设置历史记录
    private fun readSearchHistory(): List<SearchHistoryItem>{
        // 获取共享内容
        val sharedPreferences = getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val historySetString = sharedPreferences.getString("history_set", "")
        val gson = Gson()

        // 获取本地历史记录
        val historySet: List<SearchHistoryItem> = if (historySetString != "") {
            gson.fromJson(historySetString, object : TypeToken<List<SearchHistoryItem>>() {}.type)
        } else {
            listOf()
        }

        // 返回历史记录集
        return historySet
    }
    private fun saveSearchHistory(keyword: String){
        // 获取共享内容
        val sharedPreferences = getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val historySetString = sharedPreferences.getString("history_set", "")
        val gson = Gson()

        // 获取本地历史记录
        val historySet: MutableList<SearchHistoryItem> = if (historySetString != "") {
            gson.fromJson(historySetString, object : TypeToken<MutableList<SearchHistoryItem>>() {}.type)
        } else {
            mutableListOf()
        }

        // 创建基础数据
        val updatedSet = mutableListOf<SearchHistoryItem>()
        val currentTimeStamp = System.currentTimeMillis()

        // 创建新列表
        val currentSearchHistory = SearchHistoryItem(
            timeStamp = currentTimeStamp,
            keyword = keyword
        )
        updatedSet.add(currentSearchHistory)

        // 遍历并添加新值
        for (searchHistory in historySet){
            if(searchHistory.keyword != keyword){
                updatedSet.add(searchHistory)
            }
            if(updatedSet.size >= 15){
                break
            }
        }

        // 存储新的历史记录并更新
        sharedPreferences.edit().putString("history_set", gson.toJson(updatedSet)).apply()
        initHistory()
    }
    private fun clearSearchHistory(){
        val sharedPreferences = getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("history_set")
        editor.apply()
        initHistory()
    }
    inner class SearchHistoryItem(
        var timeStamp: Long,
        val keyword: String
    )
    private fun initHistory(){
        val searchHistoryTitle = binding.searchBefore.searchHistoryTitle
        val clearHistory = binding.searchBefore.clearHistory
        val searchHistory= binding.searchBefore.searchHistory

        val historySet = readSearchHistory()
        if(historySet.isEmpty()){
            searchHistoryTitle.visibility = View.GONE
            searchHistory.visibility = View.GONE
            return
        }

        searchHistoryTitle.visibility = View.VISIBLE

        clearHistory.setOnClickListener {
            createSimpleDialog(this, "确认清除全部历史记录吗？", true){
                clearSearchHistory()
            }
        }

        searchHistory.visibility = View.VISIBLE
        searchHistory.removeAllViews()

        for (historySetItem in historySet){
            val tag = layoutInflater.inflate(R.layout.tag, searchHistory, false) as CardView
            val text = tag.findViewById<TextView>(R.id.text)
            val keyword = historySetItem.keyword
            text.text = keyword
            text.textSize = 12f

            tag.setOnClickListener {
                val searchInput = findViewById<TextInputEditText>(R.id.search_input)
                searchInput.text = Editable.Factory.getInstance().newEditable(keyword)
                search()
            }

            searchHistory.addView(tag)
        }
    }

    // 执行搜索和显示结果
    private var resultAlbums:List <Albums> = listOf()
    private var resultModels:List <Models> = listOf()
    private fun search(){
        // 获取搜索词
        val searchInput = binding.searchInput
        var keyword = searchInput.text.toString()
        if (keyword.isEmpty()) {
            keyword = searchInput.hint.toString()
            searchInput.text = Editable.Factory.getInstance().newEditable(keyword)
        }

        // 存储历史记录
        saveSearchHistory(keyword)

        // 关闭预显示页面
        val searchBefore = binding.searchBefore.searchBefore
        searchBefore.visibility = View.GONE
        val searchDone = binding.searchDone.searchDone
        searchDone.visibility = View.GONE
        val noResult = binding.noResult
        noResult.visibility = View.GONE

        // 显示加载动画
        binding.imageLoading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.loading))
        binding.imageLoading.visibility = View.VISIBLE

        // 开始搜索
        searchOnAlbum(keyword)
    }
    private fun searchOnAlbum(keyword: String){
        requestAlbumSearch(this, keyword){albums, keywords ->
            resultAlbums = albums
            searchOnModel(keywords)
        }
    }
    private fun searchOnModel(keywords: List<String>?){
        requestModelSearch(this, keywords){models ->
            resultModels = models
            initSearchResult()
        }
    }
    private fun initSearchResult(){
        binding.imageLoading.clearAnimation()
        binding.imageLoading.visibility = View.GONE

        if(resultAlbums.isEmpty() && resultModels.isEmpty()){
            binding.noResult.visibility = View.VISIBLE
            return
        }

        val searchDone = binding.searchDone.searchDone
        searchDone.visibility = View.VISIBLE
        val resultView = binding.searchDone.resultView
        resultView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        resultView.adapter = MyAdapter()
    }

    // 结果列表适配器
    inner class MyAdapter: RecyclerView.Adapter<SearchViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position < resultModels.size) {
                0
            } else if (position < (resultModels.size + resultAlbums.size)) {
                1
            } else {
                2
            }
        }
        override fun getItemCount(): Int {
            return resultModels.size + resultAlbums.size + 1
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            return when (viewType) {
                0 -> {
                    val binding = ModelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    SearchViewHolder(binding)
                }
                1 -> {
                    val binding = AlbumItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    SearchViewHolder(binding)
                }
                2 -> {
                    val binding = MessageTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    SearchViewHolder(binding)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> {
                    // 获取对应的写真集
                    val model = resultModels[position]
                    val item = holder.binding as ModelItemBinding
                    // 创建点击事件
                    holder.itemView.setOnClickListener { startModelActivity(this@SearchActivity, model.id) }
                    // 更新头像
                    Glide.with(this@SearchActivity)
                        .load("${model.avatar_image}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(item.avatar)
                    // 更新名称
                    item.name.text = when(model.other_name){
                        null -> model.name
                        else -> model.name + model.other_name
                    }
                    // 更新写真集数量及更新时间
                    item.number.text = "写真集数量 ${model.total} 套"
                    item.time.text = "最后更新于 ${calculateTimeAgo(model.latest_create_time)}"
                    // 更新关注按钮
                    fun setFollowed(){
                        item.following.visibility = View.GONE
                        item.followed.visibility = View.VISIBLE
                        model.is_collection = true
                    }
                    fun unLog(){
                        startLoginActivity(this@SearchActivity)
                    }
                    if(model.is_collection != null){
                        setFollowed()
                    } else {
                        item.following.setOnClickListener {
                            setModelFollowing(this@SearchActivity, model, { setFollowed() }, { unLog() })
                        }
                    }
                }
                1 -> {
                    val album = resultAlbums[position - resultModels.size]
                    createAlbumBinding(this@SearchActivity, album, holder.itemView, holder.binding as AlbumItemBinding)
                }
                2 -> {
                    val textView = holder.itemView as TextView
                    textView.setPadding(0, 35, 0, 50)
                    textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    textView.text = "已经到底了哦~"
                }
            }
        }
    }
    class SearchViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}