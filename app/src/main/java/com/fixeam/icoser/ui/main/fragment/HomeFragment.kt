package com.fixeam.icoser.ui.main.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.AlbumFlashCardBinding
import com.fixeam.icoser.databinding.AlbumFlashPanelBinding
import com.fixeam.icoser.databinding.ForbiddenOptionBinding
import com.fixeam.icoser.databinding.FragmentHomeBinding
import com.fixeam.icoser.databinding.FragmentHomeTopBinding
import com.fixeam.icoser.model.changeBackgroundDim
import com.fixeam.icoser.model.checkForUpdate
import com.fixeam.icoser.model.hotData
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.shareTextContent
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.model.startRecommendActivity
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.network.Carousel
import com.fixeam.icoser.network.Models
import com.fixeam.icoser.network.accessLog
import com.fixeam.icoser.network.openCollectionSelector
import com.fixeam.icoser.network.requestCarouselData
import com.fixeam.icoser.network.requestHotData
import com.fixeam.icoser.network.requestLikesData
import com.fixeam.icoser.network.requestRecommendModelData
import com.fixeam.icoser.network.setAlbumCollection
import com.fixeam.icoser.network.setForbidden
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.fixeam.icoser.ui.search_page.SearchActivity
import com.fixeam.icoser.ui.update_dialog.UpdateActivity
import com.youth.banner.Banner
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var albumList: MutableList<Albums> = mutableListOf()
    private var albumIsLoading: Boolean = false
    private var typeface: Typeface? = null
    private var isLoadMore: Boolean = false
    private var isRefreshing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMainList()
        setLikeList()
        setToUp()
        setAppHeaderLayout()
        setRefreshLayout()
    }

    // 设置置顶按钮
    private fun setToUp(){
        binding.toUp.setOnClickListener {
            binding.mainList.smoothScrollToPosition(0)
        }
    }
    // 设置页头函数
    private fun setAppHeaderLayout(){
        // 设置标题字体
        typeface = Typeface.createFromAsset(requireContext().assets, "font/JosefinSans-Regular-7.ttf")
        binding.appHeader.topText.typeface = typeface
        // 创建搜索按钮点击
        val homeSearchButton = binding.appHeader.homeSearchButton
        homeSearchButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }
    }
    // 下拉刷新设置函数
    private fun setRefreshLayout(){
        val refreshLayout = binding.refreshLayout
        refreshLayout.setOnRefreshListener {
            isRefreshing = true
            albumIsLoading = true

            carouselData = listOf()
            hotData = listOf()
            models = listOf()
            albumList.clear()

            requestLikesData(requireContext(), 50){
                refreshLayout.finishRefresh()
                Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show()

                isRefreshing = false
                albumIsLoading = false

                Handler().postDelayed({
                    checkForUpdate(requireContext()){
                        if(it){
                            val intent = Intent(requireContext(), UpdateActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }, 1000)
            }
            refreshMainList(0)
        }
    }
    // 关闭启动图
    private fun closeLaunchImage(){
        requireActivity().findViewById<LinearLayout>(R.id.launch_image).visibility = View.GONE
    }

    // 轮播图模块相关变量和函数
    private var carouselData: List<Carousel> = listOf()
    private fun setCarouselHeight(banner: Banner<Carousel, BannerImageAdapter<Carousel>>){
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density

        val bannerHeight = (dpWidth - 30) / 2
        val bannerHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            bannerHeight,
            resources.displayMetrics
        ).toInt()

        val bannerLayoutParams = banner.layoutParams
        bannerLayoutParams.height = bannerHeightPx
        banner.layoutParams = bannerLayoutParams
    }
    private fun setCarouselData(binding: FragmentHomeTopBinding) {
        if(carouselData.isNotEmpty()){
            initCarouselData(binding)
            return
        }
        requestCarouselData(requireContext()){
            carouselData = it
            initCarouselData(binding)
        }
    }
    private fun initCarouselData(binding: FragmentHomeTopBinding){
        val banner: Banner<Carousel, BannerImageAdapter<Carousel>> = binding.root.findViewById(R.id.banner)
        setCarouselHeight(banner)

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
                startAlbumActivity(requireContext(), albumId)
                accessLog(requireContext(), albumId.toString(), "CLICK_CAROUSEL"){ }
            }

        binding.carouselCard.hot.setOnClickListener { startRecommendActivity(requireContext(), 0) }
        binding.carouselCard.news.setOnClickListener { startRecommendActivity(requireContext(), 1) }
    }

    // 热门模块相关变量和函数
    private var hotDataTake: List<Albums> = listOf()
    private fun setHotData(binding: FragmentHomeTopBinding) {
        binding.hotText.typeface = typeface
        if(hotDataTake.isNotEmpty()){
            initHotData(binding)
            return
        }
        requestHotData(requireContext()){
            hotDataTake = hotData.take(4)
            initHotData(binding)
        }
    }
    private fun initHotData(binding: FragmentHomeTopBinding){
        val linearLayout: LinearLayout = binding.hotCard.hotContent

        for ((index, album) in hotDataTake.withIndex()) {
            val cardView = linearLayout.getChildAt(index) as CardView
            val imageView = cardView.getChildAt(0) as ImageView
            val textView = cardView.getChildAt(1) as TextView
            Glide.with(requireContext())
                .load(album.poster + "?imageMogr2/thumbnail/300x/format/webp/interlace/1/quality/90")
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
            textView.text = album.name

            cardView.setOnClickListener {
                startAlbumActivity(requireContext(), album.id)
            }
        }
    }

    // 模特推荐模块相关变量和函数
    private var models: List<Models> = listOf()
    private fun setModelData(binding: FragmentHomeTopBinding) {
        binding.modelText.typeface = typeface

        if(models.isNotEmpty()){
            initModelData(binding)
            return
        }
        requestRecommendModelData(requireContext()){
            models = it
            initModelData(binding)
        }
    }
    private fun initModelData(binding: FragmentHomeTopBinding){
        val modelView: LinearLayout = binding.modelView
        modelView.removeAllViews()
        for (model in models) {
            if(model.album.size < 4){
                continue
            }
            modelView.addView(initModelCard(model, modelView))
        }
    }
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun initModelCard(model: Models, root: ViewGroup): View{
        // 创建模特卡片
        val modelCardView = layoutInflater.inflate(R.layout.model_card, root, false)
        val modelCardVertical = modelCardView.findViewById<LinearLayout>(R.id.model_card_vertical)

        // 修改模特名称
        val modelNameTextView = modelCardVertical.getChildAt(0) as TextView
        modelNameTextView.text = model.name
        if(model.other_name != null){
            modelNameTextView.text = "${model.name}${model.other_name}"
        }
        modelNameTextView.setOnClickListener {
            startModelActivity(requireContext(), model.id)
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
                startAlbumActivity(requireContext(), album.album_id)
            }
        }

        return modelCardView
    }

    // 首页主回收列表布局相关变量及函数
    private val viewTypeInsertion = 0
    private val viewTypeItem = 1
    private var adapter: HomeListAdapter? = null
    private fun initMainList(){
        val mainList: RecyclerView = binding.mainList

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        mainList.layoutManager = layoutManager

        adapter = HomeListAdapter()
        mainList.adapter = adapter
        mainList.setHasFixedSize(true)
        mainList.isFocusable = false
        mainList.setHasTransientState(true)

        mainList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)
                val lastVisibleItem = lastVisibleItemPositions.max()
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItem + 10 > totalItemCount && !albumIsLoading) {
                    isLoadMore = true
                    albumIsLoading = true
                    requestLikesData(requireContext(), 30){
                        isLoadMore = false
                        albumIsLoading = false
                        albumList.addAll(it)
                        refreshMainList(it.size)
                    }
                }
            }
        })
    }
    private fun setLikeList(){
        albumIsLoading = true
        val handle = Handler()

        requestLikesData(requireContext(), 50){
            albumIsLoading = false
            albumList.addAll(it)

            closeLaunchImage()
            refreshMainList(0)

            handle.postDelayed({
                checkForUpdate(requireContext()){
                    if(it){
                        val intent = Intent(requireContext(), UpdateActivity::class.java)
                        startActivity(intent)
                    }
                }
            }, 1000)
        }

        // 延迟3秒关闭加载页面
        handle.postDelayed({
            closeLaunchImage()
        }, 3000)
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMainList(loadedNumber: Int) {
        if(loadedNumber == 0){
            adapter?.notifyDataSetChanged()
        } else {
            adapter?.notifyItemInserted(albumList.size - loadedNumber - 1)
        }
    }

    // 首页主回收列表适配器
    inner class HomeListAdapter: RecyclerView.Adapter<HomeViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                viewTypeInsertion
            } else {
                viewTypeItem
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
            return when (viewType) {
                viewTypeInsertion -> {
                    val binding = FragmentHomeTopBinding.inflate(LayoutInflater.from(parent.context), parent, false)

                    val view = binding.root
                    val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.isFullSpan = true
                    view.layoutParams = layoutParams

                    HomeViewHolder(binding)
                }
                viewTypeItem -> {
                    val binding = AlbumFlashCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    HomeViewHolder(binding)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemCount(): Int {
            return albumList.size + 1
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
            val item = holder.itemView
            when (holder.itemViewType) {
                viewTypeInsertion -> {
                    val binding = holder.binding as FragmentHomeTopBinding
                    // 加载轮播图片
                    setCarouselData(binding)
                    // 加载热门项目
                    setHotData(binding)
                    // 加载模特推荐
                    setModelData(binding)
                    // 设置猜你喜欢字体
                    binding.likeText.typeface = typeface
                }
                viewTypeItem -> {
                    // 绑定瀑布流布局数据
                    val album = albumList[position - 1]
                    val binding = holder.binding as AlbumFlashCardBinding
                    // 显示已屏蔽遮罩层
                    if(album.isForbidden){
                        binding.blockOverlay.visibility = View.VISIBLE
                        item.setOnClickListener(null)
                    } else {
                        binding.blockOverlay.visibility = View.GONE
                        item.setOnClickListener{
                            accessLog(requireContext(), "${album.images}", "CLICK_RECOMMEND"){ }
                            startAlbumActivity(requireContext(), album.id)
                        }
                    }

                    // 设置主要内容
                    Glide.with(requireContext())
                        .load("${album.images}/short1200px")
                        .placeholder(R.drawable.image_holder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.imageView)
                    binding.textView.text = "${album.model} ${album.name}"

                    // 喜欢按钮操作
                    val likeButton = binding.likeButton
                    if(album.is_collection != null){
                        likeButton.setIconResource(R.drawable.like_fill)
                        likeButton.iconTint = ColorStateList.valueOf(Color.parseColor("#FDCDC5"))
                    } else {
                        likeButton.setIconResource(R.drawable.like)
                        if (isDarken(requireActivity())) {
                            likeButton.iconTint = ColorStateList.valueOf(Color.WHITE)
                        } else {
                            likeButton.iconTint = ColorStateList.valueOf(Color.BLACK)
                        }
                    }
                    likeButton.setOnClickListener{
                        if (isDarken(requireActivity())) {
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
                    item.setOnLongClickListener {
                        binding.moreButton.callOnClick()
                        true
                    }
                    binding.moreButton.setOnClickListener{
                        fun createForbiddenOverlay(){
                            album.isForbidden = true
                            binding.blockOverlay.visibility = View.VISIBLE
                            item.setOnClickListener(null)
                        }
                        initAlbumFlashPanel(album){ createForbiddenOverlay() }
                    }
                }
            }
        }
    }
    class HomeViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    // 打开猜你喜欢的写真集更多按钮
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun initAlbumFlashPanel(album: Albums, forbiddenCallback: () -> Unit){
        changeBackgroundDim(true, requireActivity())
        val flashPanelBinding = AlbumFlashPanelBinding.inflate(LayoutInflater.from(requireContext()), null, false)
//        val flashPanel = layoutInflater.inflate(R.layout.album_flash_panel, null)
        val flashPanel = flashPanelBinding.root
        val popupWindow = PopupWindow(
            flashPanel,
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.animationStyle = R.style.PopupAnimation
        popupWindow.showAtLocation(
            view?.findViewById(R.id.home_fragment),
            Gravity.BOTTOM,
            0,
            0
        )
        popupWindow.setOnDismissListener {
            changeBackgroundDim(false, requireActivity())
        }

        // 调整面板内容
        flashPanelBinding.textInfo.text = "${album.model} ${album.name}"
        Glide.with(requireContext())
            .load("${album.poster}/short500px")
            .placeholder(R.drawable.image_holder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(flashPanelBinding.posterInfo)

        // 调整按钮操作
        flashPanelBinding.close.setOnClickListener {
            popupWindow.dismiss()
        }
        flashPanelBinding.viewAlbum.setOnClickListener {
            startAlbumActivity(requireContext(), album.id)
        }
        val collection = flashPanelBinding.collection
        if(album.is_collection != null){
            collection.setIconResource(R.drawable.favor_fill)
            collection.text = "${getString(R.string.uncollection)}(${album.is_collection})"
        }
        collection.setOnClickListener {
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
        flashPanelBinding.share.setOnClickListener {
            shareTextContent(
                context = requireContext(),
                text = "来自iCoser的分享内容：模特 - ${album.model}, 写真集 - ${album.name}, 访问链接：https://app.fixeam.com/album?id=${album.id}"
            )
        }
        flashPanelBinding.forbidden.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val forbiddenOptionBinding = ForbiddenOptionBinding.inflate(LayoutInflater.from(requireContext()), null, false)
            val dialogView: View = forbiddenOptionBinding.root

            builder.setView(dialogView)
            val alertDialog = builder.create()
            alertDialog.show()

            forbiddenOptionBinding.close.setOnClickListener {
                alertDialog.cancel()
            }
            val dialogForbiddenAlbums = forbiddenOptionBinding.forbiddenAlbum
            dialogForbiddenAlbums.setOnClickListener {
                dialogForbiddenAlbums.setIconResource(R.drawable.loading2)

                fun callback() {
                    dialogForbiddenAlbums.icon = null
                    Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                    alertDialog.cancel()

                    forbiddenCallback()
                    popupWindow.dismiss()
                }
                fun unLog(){
                    dialogForbiddenAlbums.icon = null
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                }

                setForbidden(requireContext(), album.id, "album", { callback() }, { unLog() })
            }
            val dialogForbiddenModel = forbiddenOptionBinding.forbiddenModel
            dialogForbiddenModel.setOnClickListener {
                dialogForbiddenModel.setIconResource(R.drawable.loading2)

                fun callback() {
                    dialogForbiddenModel.icon = null
                    Toast.makeText(requireContext(), "操作成功, 后续将不会加载相关内容", Toast.LENGTH_SHORT).show()
                    alertDialog.cancel()

                    forbiddenCallback()
                    popupWindow.dismiss()
                }
                fun unLog(){
                    dialogForbiddenModel.icon = null
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                }

                setForbidden(requireContext(), album.model_id, "model", { callback() }, { unLog() })
            }
        }
        flashPanelBinding.viewModel.setOnClickListener {
            startModelActivity(requireContext(), album.model_id)
        }
    }
}



