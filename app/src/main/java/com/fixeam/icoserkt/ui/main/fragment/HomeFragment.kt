package com.fixeam.icoserkt.ui.main.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoserkt.ui.login_page.LoginActivity
import com.fixeam.icoserkt.ui.model_page.ModelViewActivity
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.ui.recommend_page.RecommendActivity
import com.fixeam.icoserkt.ui.search_page.SearchActivity
import com.fixeam.icoserkt.model.closeOverCard
import com.fixeam.icoserkt.model.openOverCard
import com.fixeam.icoserkt.model.overCard
import com.fixeam.icoserkt.model.shareTextContent
import com.fixeam.icoserkt.network.Albums
import com.fixeam.icoserkt.network.AlbumsResponse
import com.fixeam.icoserkt.network.ApiNetService
import com.fixeam.icoserkt.network.Carousel
import com.fixeam.icoserkt.network.CarouselResponse
import com.fixeam.icoserkt.network.Models
import com.fixeam.icoserkt.network.ModelsResponse
import com.fixeam.icoserkt.network.accessLog
import com.fixeam.icoserkt.network.openCollectionSelector
import com.fixeam.icoserkt.network.setAlbumCollection
import com.fixeam.icoserkt.network.setForbidden
import com.fixeam.icoserkt.network.userToken
import com.fixeam.icoserkt.ui.album_page.AlbumViewActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.youth.banner.Banner
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeFragment : Fragment() {
    // 猜你喜欢的推荐列表内容
    private var albumList: MutableList<Albums> = mutableListOf()
    private var albumIsLoading: Boolean = false
    private var typeface: Typeface? = null
    private var isLoadMore: Boolean = false
    private var isRefreshing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置标题字体
        typeface = Typeface.createFromAsset(requireContext().assets, "font/JosefinSans-Regular-7.ttf")
        view.findViewById<TextView>(R.id.top_text).typeface = typeface

        // 加载瀑布流推荐
        initLikeList()
        requestLikesData(50){
            closeLaunchImage()
        }

        // 创建置顶事件
        val toUpButton: FloatingActionButton = view.findViewById(R.id.to_up)
        toUpButton.setOnClickListener {
            view.findViewById<RecyclerView?>(R.id.like_list)?.smoothScrollToPosition(0)
        }

        // 创建搜索按钮点击
        val homeSearchButton = view.findViewById<ImageView>(R.id.home_search_button)
        homeSearchButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        // 延迟3秒关闭加载页面
        Handler().postDelayed({
            closeLaunchImage()
        }, 3000)

        // 设置下拉刷新
        val refreshLayout = view.findViewById<SmartRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            isRefreshing = true
            requestLikesData(50){
                // 清除已有数据
                carouselData = listOf()
                hotData = listOf()
                models = listOf()
                albumList.clear()
                refreshLayout.finishRefresh()
                Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show()
                isRefreshing = false
            }
            refreshLikeList(0)
        }
    }

    private fun initLikeListHeight(){
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = likeList?.layoutParams
        layoutParams?.height = screenHeight - (resources.displayMetrics.density * 130).toInt()
    }

    private fun openAlbumView(id: Int){
        val intent = Intent(requireContext(), AlbumViewActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    private fun openModelView(id: Int){
        val intent = Intent(requireContext(), ModelViewActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    private var carouselData: List<Carousel> = listOf()

    private fun requestCarouselData(rView: View) {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val bannerHeight = (dpWidth - 30) / 2
        val bannerHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            bannerHeight,
            resources.displayMetrics
        ).toInt()

        val banner: Banner<Carousel, BannerImageAdapter<Carousel>> = rView.findViewById(R.id.banner)
        val bannerLayoutParams = banner.layoutParams
        bannerLayoutParams?.height = bannerHeightPx
        banner.layoutParams = bannerLayoutParams
        
        if(carouselData.isNotEmpty()){
            initCarouselData(rView)
        }

        val call = ApiNetService.GetCarousel()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val carouselResponse = Gson().fromJson(responseBody, CarouselResponse::class.java)
                    if (carouselResponse.result) {
                        carouselData = carouselResponse.data
                        initCarouselData(rView)
                    } else {
                        // 处理错误情况
                        Toast.makeText(requireContext(), "轮播图数据加载失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 处理错误情况
                    Toast.makeText(requireContext(), "轮播图数据加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理网络请求失败的情况
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun initCarouselData(rView: View){
        val banner: Banner<Carousel, BannerImageAdapter<Carousel>> = rView.findViewById(R.id.banner)
        banner.setAdapter(object : BannerImageAdapter<Carousel>(carouselData) {
            override fun onBindView(
                holder: BannerImageHolder,
                data: Carousel,
                position: Int,
                size: Int
            ) {
                // 图片加载实现
                Glide.with(holder.itemView)
                    .load(data.url + "?imageMogr2/thumbnail/1000x/format/webp/interlace/1/quality/90")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imageView)
            }
        })
            .addBannerLifecycleObserver(this@HomeFragment)
            .setLoopTime(5000)
            .setScrollTime(900)
            .setIndicator(CircleIndicator(requireContext()))
            .setOnBannerListener { _, position ->
                val albumId = carouselData[position].link.content.id
                openAlbumView(albumId)
                accessLog(requireContext(), albumId.toString(), "CLICK_CAROUSEL"){ }
            }

        val hotButton = rView.findViewById<MaterialButton>(R.id.hot)
        hotButton.setOnClickListener { openRec(0) }
        val newsButton = rView.findViewById<MaterialButton>(R.id.news)
        newsButton.setOnClickListener { openRec(1) }
    }

    private fun openRec(type: Int){
        val intent = Intent(requireContext(), RecommendActivity::class.java)
        intent.putExtra("type", type)
        startActivity(intent)
    }

    private var hotData: List<Albums> = listOf()

    private fun requestHotData(rView: View) {
        rView.findViewById<TextView>(R.id.hot_text).typeface = typeface

        if(hotData.isNotEmpty()){
            initHotData(rView)
        }

        val call = ApiNetService.GetHot()
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                    if (albumsResponse.result) {
                        val linearLayout: LinearLayout = rView.findViewById(R.id.hot_content)
                        val count = linearLayout.childCount
                        hotData = count.let { albumsResponse.data.take(it) }

                        initHotData(rView)
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initHotData(rView: View){
        val linearLayout: LinearLayout = rView.findViewById(R.id.hot_content)

        for ((index, album) in hotData.withIndex()) {
            val cardView = linearLayout.getChildAt(index) as CardView
            val imageView = cardView.getChildAt(0) as ImageView
            val textView = cardView.getChildAt(1) as TextView
            Glide.with(requireContext())
                .load(album.poster + "?imageMogr2/thumbnail/300x/format/webp/interlace/1/quality/90")
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
            textView.text = album.name

            // 设置点击打开写真集预览页面
            cardView.setOnClickListener {
                openAlbumView(album.id)
            }
        }
    }

    private var models: List<Models> = listOf()

    private fun requestModelData(view: View) {
        view.findViewById<TextView>(R.id.model_text).typeface = typeface

        if(models.isNotEmpty()){
            initModelData(view)
            return
        }

        val call = ApiNetService.GetRecModel()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val modelsResponse = Gson().fromJson(responseBody, ModelsResponse::class.java)
                    if (modelsResponse.result) {
                        models = modelsResponse.data
                        initModelData(view)
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun initModelData(rview: View){
        val modelView: LinearLayout = rview.findViewById(R.id.model_view)
        modelView.removeAllViews()

        // 遍历并创建模特卡片
        for (model in models) {
            val modelCardView = layoutInflater.inflate(R.layout.model_card, modelView, false)
            val modelCardVertical = modelCardView.findViewById<LinearLayout>(R.id.model_card_vertical)

            // 修改模特名称
            val modelNameTextView = modelCardVertical.getChildAt(0) as TextView
            modelNameTextView.text = model.name
            if(model.other_name != null){
                modelNameTextView.text = "${model.name}${model.other_name}"
            }
            modelNameTextView.setOnClickListener {
                openModelView(model.id)
            }

            // 修改写真集
            val modelAlbumCardListContainer = modelCardVertical.getChildAt(1) as LinearLayout
            val modelAlbumCardListCount = modelAlbumCardListContainer.childCount

            // 遍历并修改写真集图片及其名称
            val modelAlbums = model.album
            for ((idx, album) in modelAlbums.withIndex()) {
                if(idx > modelAlbumCardListCount){
                    break
                }
                val modelAlbumCardLayout = modelAlbumCardListContainer.getChildAt(idx) as LinearLayout

                val modelAlbumCard = modelAlbumCardLayout.getChildAt(0) as CardView
                val modelAlbumCardImage = modelAlbumCard.getChildAt(0) as ImageView
                Glide.with(requireContext())
                    .load(album.poster + "?imageMogr2/thumbnail/500x/format/webp/interlace/1/quality/90")
                    .placeholder(R.drawable.image_holder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(modelAlbumCardImage)

                val modelAlbumCardText = modelAlbumCardLayout.getChildAt(1) as TextView
                modelAlbumCardText.text = album.name

                // 创建写真集点击事件
                modelAlbumCardLayout.setOnClickListener{
                    openAlbumView(album.album_id)
                }
            }

            modelView.addView(modelCardView)
        }
    }

    private val viewTypeInsertion = 0
    private val viewTypeItem = 1

    private fun initLikeList(){
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        likeList?.layoutManager = layoutManager
        val adapter = LikeListAdapter()
        likeList?.adapter = adapter
        likeList?.setHasFixedSize(true)
        likeList?.isFocusable = false
        likeList?.setHasTransientState(true)

        likeList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)
                val lastVisibleItem = lastVisibleItemPositions.max()
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItem + 10 > totalItemCount && !albumIsLoading) {
                    isLoadMore = true
                    requestLikesData(30){}
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshLikeList(loadedNumber: Int) {
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)
        val adapter = likeList?.adapter as? LikeListAdapter

        if(loadedNumber == 0){
            adapter?.notifyDataSetChanged()
        } else {
            adapter?.notifyItemInserted(albumList.size - loadedNumber - 1)
        }
    }

    private fun requestLikesData(number: Int, before: () -> Unit){
        albumIsLoading = true

        var call = ApiNetService.GetRecAlbum(number)
        if(userToken != null){
            call = ApiNetService.GetRecAlbum(number, userToken!!)
        }

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)

                    if(!albumsResponse.result){
                        return
                    }
                    before()

                    albumList.addAll(albumsResponse.data)
                    albumIsLoading = false

                    initLikeListHeight()
                    if(isLoadMore) {
                        refreshLikeList(albumsResponse.data.size)
                        isLoadMore = false
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun closeLaunchImage(){
        val launchImage = activity?.findViewById<LinearLayout>(R.id.launch_image)
        if (launchImage != null) {
            launchImage.visibility = View.GONE
        }
    }

    inner class LikeListAdapter : RecyclerView.Adapter<LikeViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                // 第一个位置插入单行布局
                viewTypeInsertion
            } else {
                // 其他位置为瀑布流布局
                viewTypeItem
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
            return when (viewType) {
                viewTypeInsertion -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_home_top, parent, false)
                    val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.isFullSpan = true
                    view.layoutParams = layoutParams
                    view.isFocusable = true
                    LikeViewHolder(view)
                }
                viewTypeItem -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.album_flash_card, parent, false)
                    view.isFocusable = false
                    LikeViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemCount(): Int {
            return albumList.size + 1
        }

        override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
            when (holder.itemViewType) {
                // 修改holder
                viewTypeInsertion -> {
                    // 绑定单行布局数据
                    // 加载轮播图片
                    requestCarouselData(holder.itemView)
                    // 加载热门项目
                    requestHotData(holder.itemView)
                    // 加载模特推荐
                    requestModelData(holder.itemView)

                    holder.itemView.findViewById<TextView>(R.id.like_text).typeface = typeface
                }
                viewTypeItem -> {
                    // 绑定瀑布流布局数据
                    val album = albumList[position - 1]
                    holder.itemView.setOnClickListener{
                        accessLog(requireContext(), "${album.images}", "CLICK_RECOMMEND"){ }
                        openAlbumView(album.id)
                    }

                    val imageView = holder.itemView.findViewById<ImageView>(R.id.image_view)

                    Glide.with(requireContext())
                        .load("${album.images}/short1200px")
                        .placeholder(R.drawable.image_holder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView)

                    val textView = holder.itemView.findViewById<TextView>(R.id.text_view)
                    textView.text = "${album.model} ${album.name}"

                    // 喜欢按钮操作
                    val likeButton = holder.itemView.findViewById<MaterialButton>(R.id.like_button)
                    val currentTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    if(album.is_collection != null){
                        likeButton.setIconResource(R.drawable.like_fill)
                        likeButton.iconTint = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))
                    }
                    likeButton.setOnClickListener{
                        if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
                            likeButton.iconTint = ColorStateList.valueOf(Color.WHITE)
                        } else {
                            likeButton.iconTint = ColorStateList.valueOf(Color.BLACK)
                        }

                        likeButton.setIconResource(R.drawable.loading)
                        val animation = AnimationUtils.loadAnimation(requireContext(),
                            R.anim.loading
                        )
                        likeButton.startAnimation(animation)

                        fun callback() {
                            likeButton.clearAnimation()
                            if(album.is_collection != null){
                                likeButton.setIconResource(R.drawable.like)
                                if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
                                    likeButton.iconTint = ColorStateList.valueOf(Color.WHITE)
                                } else {
                                    likeButton.iconTint = ColorStateList.valueOf(Color.BLACK)
                                }
                                album.is_collection = null
                            } else {
                                likeButton.setIconResource(R.drawable.like_fill)
                                likeButton.iconTint = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))
                                album.is_collection = "like"
                            }
                        }
                        fun unLog(){
                            likeButton.clearAnimation()
                            likeButton.setIconResource(R.drawable.like)
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)
                        }

                        setAlbumCollection(requireContext(), album, "like", { callback() }, { unLog() })
                    }

                    // 更多按钮操作
                    val moreButton = holder.itemView.findViewById<MaterialButton>(R.id.more_button)
                    holder.itemView.setOnLongClickListener {
                        moreButton.callOnClick()
                        true
                    }
                    moreButton.setOnClickListener{
                        fun createForbiddenOverlay(){
                            val blockOverlay = holder.itemView.findViewById<LinearLayout>(R.id.block_overlay)
                            blockOverlay.visibility = View.VISIBLE
                            holder.itemView.setOnClickListener(null)
                        }
                        initAlbumFlashPanel(album){ createForbiddenOverlay() }
                    }
                }
            }
        }

        override fun onViewRecycled(holder: LikeViewHolder) {
            super.onViewRecycled(holder)
        }
    }

    class LikeViewHolder(view: View) : RecyclerView.ViewHolder(view)

    @SuppressLint("SetTextI18n")
    private fun initAlbumFlashPanel(album: Albums, forbiddenCallback: () -> Unit){
        val falshPanel = layoutInflater.inflate(R.layout.album_flash_panel, overCard, false) as LinearLayout
        val container = overCard?.findViewById<LinearLayout>(R.id.content_layout)
        container?.removeAllViews()
        if (falshPanel.parent != null) {  // 判断视图是否已有父视图
            (falshPanel.parent as ViewGroup).removeView(falshPanel)  // 将视图从父视图中移除
        }
        container?.addView(falshPanel)

        // 调整面板内容
        overCard?.findViewById<TextView>(R.id.text_info)?.text = "${album.model} ${album.name}"
        val posterImage = overCard?.findViewById<ImageView>(R.id.poster_info)
        posterImage?.let { it1 ->
            Glide.with(requireContext())
                .load("${album.poster}/short500px")
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(it1)
        }

        // 调整按钮操作
        val closeButton = overCard?.findViewById<MaterialButton>(R.id.close)
        closeButton?.setOnClickListener {
            closeOverCard()
        }
        val viewAlbums = overCard?.findViewById<MaterialButton>(R.id.view_album)
        viewAlbums?.setOnClickListener {
            openAlbumView(album.id)
        }
        val collection = overCard?.findViewById<MaterialButton>(R.id.collection)
        if(album.is_collection != null){
            collection?.setIconResource(R.drawable.favor_fill)
            collection?.text = "${getString(R.string.uncollection)}(${album.is_collection})"
        }
        collection?.setOnClickListener {
            fun unLog(){
                collection.setIconResource(R.drawable.favor)
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }

            if(album.is_collection != null){
                collection.setIconResource(R.drawable.loading2)
                setAlbumCollection(
                    requireContext(),
                    album,
                    album.is_collection!!,
                    {
                        collection.setIconResource(R.drawable.favor)
                        album.is_collection = null
                        collection.text = getString(R.string.collection)
                    },
                    { unLog() }
                )
            } else {
                openCollectionSelector(
                    requireContext(),
                    album,
                    {
                        collection.setIconResource(R.drawable.favor_fill)
                        album.is_collection = it
                        collection.text = "${getString(R.string.uncollection)}($it)"
                    },
                    { unLog() }
                )
            }
        }
        val share = overCard?.findViewById<MaterialButton>(R.id.share)
        share?.setOnClickListener {
            shareTextContent(
                context = requireContext(),
                text = "来自iCoser的分享内容：模特 - ${album.model}, 写真集 - ${album.name}, 访问链接：https://app.fixeam.com/album?id=${album.id}"
            )
        }
        val forbidden = overCard?.findViewById<MaterialButton>(R.id.forbidden)
        forbidden?.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView: View = inflater.inflate(R.layout.forbidden_option, null)

            builder.setView(dialogView)
            val alertDialog = builder.create()
            alertDialog.show()

            val dialogClose = dialogView.findViewById<MaterialButton>(R.id.close)
            dialogClose.setOnClickListener {
                alertDialog.cancel()
            }
            val dialogForbiddenAlbums = dialogView.findViewById<MaterialButton>(R.id.forbidden_album)
            dialogForbiddenAlbums.setOnClickListener {
                dialogForbiddenAlbums.setIconResource(R.drawable.loading2)

                fun callback() {
                    dialogForbiddenAlbums.icon = null
                    Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                    alertDialog.cancel()

                    forbiddenCallback()
                    closeOverCard()
                }
                fun unLog(){
                    dialogForbiddenAlbums.icon = null
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                }

                setForbidden(requireContext(), album.id, "album", { callback() }, { unLog() })
            }
            val dialogForbiddenModel = dialogView.findViewById<MaterialButton>(R.id.forbidden_model)
            dialogForbiddenModel.setOnClickListener {
                dialogForbiddenModel.setIconResource(R.drawable.loading2)

                fun callback() {
                    dialogForbiddenModel.icon = null
                    Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                    alertDialog.cancel()

                    forbiddenCallback()
                    closeOverCard()
                }
                fun unLog(){
                    dialogForbiddenModel.icon = null
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                }

                setForbidden(requireContext(), album.model_id, "model", { callback() }, { unLog() })
            }
        }
        val model = overCard?.findViewById<MaterialButton>(R.id.view_model)
        model?.setOnClickListener {
            openModelView(album.model_id)
        }

        // 显示面板
        overCard!!.visibility = View.VISIBLE

        // 执行面板动画
        openOverCard()
    }
}



