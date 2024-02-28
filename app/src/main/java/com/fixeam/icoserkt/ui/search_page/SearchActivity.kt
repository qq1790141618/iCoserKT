package com.fixeam.icoserkt.ui.search_page

import com.fixeam.icoserkt.painter.GlideBlurTransformation
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.model.calculateTimeAgo
import com.fixeam.icoserkt.model.hotData
import com.fixeam.icoserkt.model.setStatusBar
import com.fixeam.icoserkt.network.Albums
import com.fixeam.icoserkt.network.AlbumsResponse
import com.fixeam.icoserkt.network.ApiNetService
import com.fixeam.icoserkt.network.Models
import com.fixeam.icoserkt.network.SearchAlbumResponse
import com.fixeam.icoserkt.network.SearchModelResponse
import com.fixeam.icoserkt.network.setModelFollowing
import com.fixeam.icoserkt.network.userToken
import com.fixeam.icoserkt.ui.album_page.AlbumViewActivity
import com.fixeam.icoserkt.ui.login_page.LoginActivity
import com.fixeam.icoserkt.ui.model_page.ModelViewActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {
    private var hotViewList: List<Albums> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        requestHotData()
        initHistory()

        // 设置搜索按钮点击事件
        val searchButton = findViewById<MaterialButton>(R.id.search_button)
        searchButton.setOnClickListener {
            search()
        }

        // 设置预搜索词
        setSearchPlaceHolder()
    }

    private fun setSearchPlaceHolder(){
        val keywordList: List<String> = listOf(
            "黑丝皮裙",
            "纯欲吊带",
            "iMiss",
            "XiuRen",
            "女仆私房",
            "JK制服"
        )

        val searchInput = findViewById<TextInputEditText>(R.id.search_input)
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                search()
                return@setOnEditorActionListener true
            }
            false
        }

        var currentKeywordIndex = (1..20).random()

        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                searchInput.hint = Editable.Factory.getInstance().newEditable(keywordList[currentKeywordIndex % keywordList.size])
                currentKeywordIndex++
                handler.postDelayed(this, 5000) // 5秒后再次执行
            }
        }

        handler.post(runnable)
    }

    private fun requestHotData() {
        if(hotData.isNotEmpty()){
            initHotViewList()
        }

        val call = ApiNetService.GetHot()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                    if (albumsResponse.result) {
                        hotData = albumsResponse.data.take(30)
                        initHotViewList()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@SearchActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initHotViewList(){
        hotViewList = hotData.take(8)

        val hotViewTitle = findViewById<TextView>(R.id.hot_view_title)
        val typeface = Typeface.createFromAsset(assets, "font/ZiTiQuanXinYiGuanHeiTi4.0-2.ttf")
        hotViewTitle.typeface = typeface
        hotViewTitle.visibility = View.VISIBLE

        val listView = findViewById<ListView>(R.id.hot_view)
        val adapter = CustomArrayAdapter(this, android.R.layout.simple_list_item_1, hotViewList.map {
            "${it.model_name} ${it.name}" }, 12f, 42)
        listView.adapter = adapter
        adapter.setNotifyOnChange(true)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = Intent(this@SearchActivity, AlbumViewActivity::class.java)
            intent.putExtra("id", hotViewList[position].id)
            startActivity(intent)
        }
    }

    inner class CustomArrayAdapter(context: Context, resource: Int, objects: List<String>, private val textSize: Float, private val height: Int) : ArrayAdapter<String>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            view.layoutParams.height = (resources.displayMetrics.density * height).toInt()
            return view
        }
    }

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
        val searchHistoryTitle = findViewById<LinearLayout>(R.id.search_history_title)
        val clearHistory = findViewById<ImageView>(R.id.clear_history)
        val searchHistory= findViewById<FlexboxLayout>(R.id.search_history)

        val historySet = readSearchHistory()
        if(historySet.isEmpty()){
            searchHistoryTitle.visibility = View.GONE
            searchHistory.visibility = View.GONE
            return
        }


        searchHistoryTitle.visibility = View.VISIBLE

        clearHistory.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("确认清除全部历史记录吗？")

            builder.setPositiveButton("确定") { _, _ ->
                clearSearchHistory()
            }
            builder.setNegativeButton("取消") { _, _ ->
                // 不清除历史记录
            }

            val alertDialog = builder.create()
            alertDialog.show()
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

    private fun search(){
        // 获取搜索词
        val searchInput = findViewById<TextInputEditText>(R.id.search_input)
        var keyword = searchInput.text.toString()
        if (keyword.isEmpty()) {
            keyword = searchInput.hint.toString()
            searchInput.text = Editable.Factory.getInstance().newEditable(keyword)
        }

        // 存储历史记录
        saveSearchHistory(keyword)

        // 关闭预显示页面
        val searchBefore = findViewById<LinearLayout>(R.id.search_before)
        searchBefore.visibility = View.GONE
        val searchDone = findViewById<LinearLayout>(R.id.search_done)
        searchDone.visibility = View.GONE
        val noResult = findViewById<TextView>(R.id.no_result)
        noResult.visibility = View.GONE

        // 显示加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 开始搜索
        searchOnAlbum(keyword)
    }

    private var resultAlbums:List <Albums> = listOf()
    private var resultModels:List <Models> = listOf()

    private fun searchOnAlbum(keyword: String){
        var call = ApiNetService.SearchAlbum(
            keyword = keyword
        )
        if(userToken != null){
            call = ApiNetService.SearchAlbum(
                access_token = userToken!!,
                keyword = keyword
            )
        }

        call.enqueue(object : Callback<SearchAlbumResponse> {
            override fun onResponse(call: Call<SearchAlbumResponse>, response: Response<SearchAlbumResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.result) {
                        resultAlbums = responseBody.data
                        searchOnModel(responseBody.keywords)
                    }
                }
            }

            override fun onFailure(call: Call<SearchAlbumResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@SearchActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchOnModel(keywords: List<String>){
        val keywordsString = Gson().toJson(keywords)
        var call = ApiNetService.SearchModel(
            keywords = keywordsString
        )
        if(userToken != null){
            call = ApiNetService.SearchModel(
                access_token = userToken!!,
                keywords = keywordsString
            )
        }

        call.enqueue(object : Callback<SearchModelResponse> {
            override fun onResponse(call: Call<SearchModelResponse>, response: Response<SearchModelResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.result) {
                        resultModels = responseBody.data
                        initSearchResult()
                    }
                }
            }

            override fun onFailure(call: Call<SearchModelResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@SearchActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initSearchResult(){
        // 关闭加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        imageView.clearAnimation()
        imageView.visibility = View.GONE

        // 检测结果是否为空
        if(resultAlbums.isEmpty() && resultModels.isEmpty()){
            val noResult = findViewById<TextView>(R.id.no_result)
            noResult.visibility = View.VISIBLE
            return
        }

        // 打开结果页面
        val searchDone = findViewById<LinearLayout>(R.id.search_done)
        searchDone.visibility = View.VISIBLE
        val resultView = findViewById<RecyclerView>(R.id.result_view)
        resultView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        resultView.adapter = MyAdapter()
    }

    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
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
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return when (viewType) {
                0 -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.model_item, parent, false)
                    MyViewHolder(view)
                }
                1 -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.album_item, parent, false)
                    MyViewHolder(view)
                }
                2 -> {
                    val view = TextView(this@SearchActivity)
                    MyViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> {
                    val model = resultModels[position]

                    // 创建点击事件
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@SearchActivity, ModelViewActivity::class.java)
                        intent.putExtra("id", model.id)
                        startActivity(intent)
                    }

                    // 更新头像
                    val avatar = holder.itemView.findViewById<ImageView>(R.id.avatar)
                    Glide.with(this@SearchActivity)
                        .load("${model.avatar_image}/short500px")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(avatar)

                    // 更新名称
                    val name = holder.itemView.findViewById<TextView>(R.id.name)
                    if(model.other_name != null){
                        name.text = model.name + model.other_name
                    } else {
                        name.text = model.name
                    }

                    // 更新写真集数量及更新时间
                    val number = holder.itemView.findViewById<TextView>(R.id.number)
                    number.text = "写真集数量 ${model.total} 套"
                    val time = holder.itemView.findViewById<TextView>(R.id.time)
                    time.text = "最后更新于 ${calculateTimeAgo(model.latest_create_time)}"

                    // 更新关注按钮
                    val following = holder.itemView.findViewById<MaterialButton>(R.id.following)
                    val followed = holder.itemView.findViewById<MaterialButton>(R.id.followed)
                    fun setFollowed(){
                        following.visibility = View.GONE
                        followed.visibility = View.VISIBLE
                        model.is_collection = true
                    }
                    fun unLog(){
                        val intent = Intent(this@SearchActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }

                    if(model.is_collection != null){
                        setFollowed()
                    } else {
                        following.setOnClickListener {
                            setModelFollowing(this@SearchActivity, model, { setFollowed() }, { unLog() })
                        }
                    }
                }
                1 -> {
                    val album = resultAlbums[position - resultModels.size]

                    // 创建点击事件
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@SearchActivity, AlbumViewActivity::class.java)
                        intent.putExtra("id", album.id)
                        startActivity(intent)
                    }

                    // 修改海报图
                    val posterBackground = holder.itemView.findViewById<ImageView>(R.id.poster_background)
                    Glide.with(this@SearchActivity)
                        .load("${album.poster}/short500px")
                        .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this@SearchActivity)))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(posterBackground)
                    val poster = holder.itemView.findViewById<ImageView>(R.id.poster)
                    Glide.with(this@SearchActivity)
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
                    for ((index, image) in (album.images as MutableList<String>).withIndex()){
                        if(index >= 4){
                            break
                        }

                        val cardView = CardView(this@SearchActivity)
                        val layoutParams = ViewGroup.MarginLayoutParams(
                            (resources.displayMetrics.density * 36).toInt(), // 设置宽度为屏幕宽度的四分之一
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        layoutParams.rightMargin = (resources.displayMetrics.density * 5).toInt() // 设置右边距
                        cardView.layoutParams = layoutParams
                        cardView.cardElevation = 0F
                        cardView.radius = resources.displayMetrics.density * 3 // 设置圆角半径

                        val imageView = ImageView(this@SearchActivity)
                        imageView.id = View.generateViewId()
                        val imageLayoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        imageView.layoutParams = imageLayoutParams
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                        Glide.with(this@SearchActivity)
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
                    textView.text = "已经到底了哦~"
                }
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}