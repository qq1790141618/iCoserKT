package com.fixeam.icoserkt

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
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
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.youth.banner.Banner
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {
    // 猜你喜欢的推荐列表内容
    private var albumList: MutableList<Albums> = mutableListOf()
    private var albumIsLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        val typeface = Typeface.createFromAsset(requireContext().assets, "font/JosefinSans-Regular-7.ttf")
        view.findViewById<TextView>(R.id.top_text).setTypeface(typeface)
        view.findViewById<TextView>(R.id.hot_text).setTypeface(typeface)
        view.findViewById<TextView>(R.id.model_text).setTypeface(typeface)
        view.findViewById<TextView>(R.id.like_text).setTypeface(typeface)

        // 加载轮播图片
        requestCarouselData()
        // 加载热门项目
        requestHotData()
        // 加载模特推荐
        requestModelData()
        // 加载瀑布流推荐
        showAlbumLoading()
        requestLikesData(50)
        initLikeList()

        // 监听页面滚动
        val toUpButton: FloatingActionButton = view.findViewById(R.id.to_up)
        val scrollView = view.findViewById<NestedScrollView>(R.id.home_scroll_view)
        toUpButton.setOnClickListener {
            scrollView.fling(0)
            val animator = ValueAnimator.ofInt(scrollView.scrollY, 0)
            animator.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                scrollView.scrollTo(0, value)
            }
            animator.duration = 500
            animator.start()

            view.findViewById<RecyclerView?>(R.id.like_list)?.scrollToPosition(0)
        }
        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY >= v.getChildAt(0).measuredHeight - v.measuredHeight - 30) {
                val likeList: RecyclerView = view.findViewById(R.id.like_list)
                likeList.setNestedScrollingEnabled(true)
                scrollView.isNestedScrollingEnabled = false
            }
        })
    }

    override fun onResume() {
        val scrollView = view?.findViewById<NestedScrollView>(R.id.home_scroll_view)
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)

        if (scrollView != null) {
            if (scrollView.scrollY >= scrollView.getChildAt(0).measuredHeight - scrollView.measuredHeight - 30) {
                likeList?.setNestedScrollingEnabled(true)
                scrollView.isNestedScrollingEnabled = false
            }
        }

        super.onResume()
    }

    private fun showAlbumLoading(){
        val imageView: ImageView? = view?.findViewById(R.id.like_loading)
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
        imageView?.startAnimation(animation)
        imageView?.visibility = View.VISIBLE
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

    private fun requestCarouselData() {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val bannerHeight = (dpWidth - 30) / 2
        val bannerHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            bannerHeight.toFloat(),
            resources.displayMetrics
        ).toInt()

        val bannerPlaceHolder: ImageView? = view?.findViewById(R.id.banner_placeholder)
        val layoutParams = bannerPlaceHolder?.layoutParams
        layoutParams?.height = bannerHeightPx
        bannerPlaceHolder?.layoutParams = layoutParams

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.GetCarousel()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val CarouselResponse = Gson().fromJson(responseBody, CarouselResponse::class.java)
                    if (CarouselResponse.result) {
                        val carousels = CarouselResponse.data

                        // 初始化Banner和适配器
                        val banner: Banner<Carousel, BannerImageAdapter<Carousel>>? = view?.findViewById(R.id.banner)
                        banner?.setAdapter(object : BannerImageAdapter<Carousel>(carousels) {
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
                        ?.addBannerLifecycleObserver(this@HomeFragment)
                        ?.setLoopTime(5000)
                        ?.setScrollTime(900)
                        ?.setIndicator(CircleIndicator(requireContext()))
                        ?.setOnBannerListener { carousel, position ->
                            val albumId = carousels[position].link.content.id
                            openAlbumView(albumId)
                        }

                        layoutParams?.height = 0
                        bannerPlaceHolder?.layoutParams = layoutParams
                        val bannerLayoutParams = banner?.layoutParams
                        bannerLayoutParams?.height = bannerHeightPx
                        banner?.layoutParams = bannerLayoutParams
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

    private fun requestHotData() {
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.GetHot()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val AlbumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                    if (AlbumsResponse.result) {
                        val linearLayout: LinearLayout? = view?.findViewById(R.id.hot_content)
                        val count = linearLayout?.childCount
                        val albums = count?.let { AlbumsResponse.data.take(it) }
                        if (albums != null) {
                            for ((index, album) in albums.withIndex()) {
                                val cardView = linearLayout?.getChildAt(index) as CardView
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
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun requestModelData() {
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.GetRecModel()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val modelsResponse = Gson().fromJson(responseBody, ModelsResponse::class.java)
                    if (modelsResponse.result) {
                        val models = modelsResponse.data
                        val homeContent: LinearLayout? = view?.findViewById(R.id.home_content)

                        // 遍历并创建模特卡片
                        for ((index, model) in models.withIndex()) {
                            val modelCardView = layoutInflater.inflate(R.layout.model_card, homeContent, false)
                            val modelCardVertical = modelCardView.findViewById<LinearLayout>(R.id.model_card_vertical)

                            // 修改模特名称
                            val modelNameTextView = modelCardVertical.getChildAt(0) as TextView
                            modelNameTextView.text = model.name
                            if(model.other_name != null){
                                modelNameTextView.text = model.name + model.other_name
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

                                val modelAlbumCardImage = modelAlbumCardLayout.getChildAt(0) as ImageView
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

                            homeContent?.addView(modelCardView, index + 4)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initLikeList(){
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)

        likeList?.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        likeList?.adapter = likeListAdapter()
        likeList?.setHasFixedSize(true)
        likeList?.isFocusable = false
        likeList?.setHasTransientState(true)
        likeList?.setNestedScrollingEnabled(false)

        likeList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)
                val lastVisibleItem = lastVisibleItemPositions.max() ?: 0
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItem + 2000 > totalItemCount) {
                    requestLikesData(30)
                }

                val firstVisibleItemPositions = layoutManager.findFirstVisibleItemPositions(null)
                val firstVisibleItem = firstVisibleItemPositions.min() ?: 0
                if (firstVisibleItem == 0) {
                    likeList.setNestedScrollingEnabled(false)
                    val scrollView = view?.findViewById<NestedScrollView>(R.id.home_scroll_view)
                    scrollView?.isNestedScrollingEnabled = true
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshLikeList(loadedNumber: Int) {
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)
        val adapter = likeList?.adapter as? likeListAdapter

        if(loadedNumber == 0){
            adapter?.notifyDataSetChanged()
        } else {
            adapter?.notifyItemInserted(albumList.size - loadedNumber)
        }
    }

    private fun requestLikesData(number: Int){
        albumIsLoading = true

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        var call = ApiService.GetRecAlbum(number)
        if(userToken != null){
            call = ApiService.GetRecAlbum(number, userToken!!)
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

                    val imageView: ImageView? = view?.findViewById<ImageView>(R.id.like_loading)
                    imageView?.clearAnimation()
                    imageView?.visibility = View.GONE

                    albumIsLoading = false

                    initLikeListHeight()
                    refreshLikeList(albumsResponse.data.size)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class likeListAdapter : RecyclerView.Adapter<likeViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): likeViewHolder {
            val itemView = LayoutInflater.from(context).inflate(R.layout.album_flash_card, parent, false)
            return likeViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return albumList.size
        }
        override fun onBindViewHolder(holder: likeViewHolder, position: Int) {
            // 修改holder
            val album = albumList[position]
            holder.itemView.setOnClickListener{
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
                likeButton.iconTint = ColorStateList.valueOf(Color.RED)
            }
            likeButton.setOnClickListener{
                if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
                    likeButton.iconTint = ColorStateList.valueOf(Color.WHITE)
                } else {
                    likeButton.iconTint = ColorStateList.valueOf(Color.BLACK)
                }

                likeButton.setIconResource(R.drawable.loading)
                val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
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
                        likeButton.iconTint = ColorStateList.valueOf(Color.RED)
                        album.is_collection = "like"
                    }
                }
                fun unlog(){
                    likeButton.clearAnimation()
                    likeButton.setIconResource(R.drawable.like)
                }

                setAlbumCollection(album, "like", { callback() }, { unlog() })
            }

            // 更多按钮操作
            val moreButton = holder.itemView.findViewById<MaterialButton>(R.id.more_button)
            holder.itemView.setOnLongClickListener {
                moreButton.callOnClick()
                true
            }
            moreButton.setOnClickListener{
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
                    collection?.text = getString(R.string.uncollection)
                }
                collection?.setOnClickListener {
                    collection.setIconResource(R.drawable.loading2)

                    fun collectioncallback() {
                        if(album.is_collection != null){
                            collection.setIconResource(R.drawable.favor)
                            album.is_collection = null
                            collection.text = getString(R.string.collection)
                        } else {
                            collection.setIconResource(R.drawable.favor_fill)
                            album.is_collection = "default"
                            collection.text = getString(R.string.uncollection)
                        }
                    }
                    fun unlog(){
                        collection.setIconResource(R.drawable.favor)
                    }

                    setAlbumCollection(album, "default", { collectioncallback() }, { unlog() })
                }
                val share = overCard?.findViewById<MaterialButton>(R.id.share)
                share?.setOnClickListener {
                    shareTextContent(
                        context = requireContext(),
                        text = "来自iCoser的分享内容：模特 - ${album.model}, 写真集 - ${album.name}, 访问链接：https://app.fixeam.com/album?id=${album.id}"
                    )
                }
                val forbbiden = overCard?.findViewById<MaterialButton>(R.id.forbidden)
                forbbiden?.setOnClickListener {
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
                            dialogForbiddenAlbums.setIcon(null)
                            Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                            alertDialog.cancel()
                            val blockOverlay = holder.itemView.findViewById<LinearLayout>(R.id.block_overlay)
                            blockOverlay.visibility = View.VISIBLE
                            holder.itemView.setOnClickListener(null)
                            closeOverCard()
                        }
                        fun unlog(){
                            dialogForbiddenAlbums.setIcon(null)
                        }

                        setForbidden(album.id, "album", { callback() }, { unlog() })
                    }
                    val dialogForbiddenModel = dialogView.findViewById<MaterialButton>(R.id.forbidden_model)
                    dialogForbiddenModel.setOnClickListener {
                        dialogForbiddenModel.setIconResource(R.drawable.loading2)

                        fun callback() {
                            dialogForbiddenModel.setIcon(null)
                            Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                            alertDialog.cancel()
                            val blockOverlay = holder.itemView.findViewById<LinearLayout>(R.id.block_overlay)
                            blockOverlay.visibility = View.VISIBLE
                            holder.itemView.setOnClickListener(null)
                            closeOverCard()
                        }
                        fun unlog(){
                            dialogForbiddenModel.setIcon(null)
                        }

                        setForbidden(album.model_id, "model", { callback() }, { unlog() })
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
    }

    class likeViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    private fun setForbidden(id: Int, type: String, callback: () -> Unit, unlog: () -> Unit){
        if(userToken != null){
            val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(SERVE_HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val ApiService = retrofit.create(ApiService::class.java)
            var call = ApiService.SetForbiddenItem(userToken!!, id, "album")

            call.enqueue(object : Callback<ActionResponse> {
                override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()

                        if(!responseBody?.result!!){
                            Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                            return
                        }

                        callback()
                    }
                }

                override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                    // 处理请求失败的逻辑
                    Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            unlog()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
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
                            Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                            return
                        }

                        callback()

                        Toast.makeText(requireContext(), "操作成功", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                    // 处理请求失败的逻辑
                    Toast.makeText(requireContext(), "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            unlog()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
    }
}

data class CarouselResponse(
    val result: Boolean,
    val data: List<Carousel>
)
data class Carousel(
    val id: Int,
    val serial: Int,
    val url: String,
    val link: Link
)
data class Link(
    val type: String,
    val content: Content
)
data class Content(
    val id: Int,
    val name: String,
    val model_id: Int,
    val model: String
)

