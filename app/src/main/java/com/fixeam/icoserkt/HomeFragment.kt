package com.fixeam.icoserkt

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // 猜你喜欢的推荐列表内容
    private var albumList: MutableList<Albums> = mutableListOf()
    private var albumIsLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
        requestLikesData()
        initLikeList()

        // 监听页面滚动
        val toUpButton: FloatingActionButton = view.findViewById(R.id.to_up)
        val scrollView = view.findViewById<ScrollView>(R.id.home_scroll_view)
        toUpButton.setOnClickListener {
            val animator = ValueAnimator.ofInt(scrollView.scrollY, 0)
            animator.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                scrollView.scrollTo(0, value)
            }
            animator.duration = 500
            animator.start()
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollView.scrollY
            val contentHeight = scrollView.getChildAt(0).height
            if (contentHeight - scrollY - scrollView.height <= 5000 && !albumIsLoading) {
                if (!scrollView.canScrollVertically(1)){
                    // 设置加载动画
                    val imageView: ImageView = view.findViewById<ImageView>(R.id.like_loading)
                    val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.loading)
                    imageView.startAnimation(animation)
                    imageView.visibility = View.VISIBLE
                }
                requestLikesData()
            }
        }
    }

    private fun openAlbumView(id: Int){
        val intent = Intent(requireContext(), AlbumViewActivity::class.java)
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
                                Toast.makeText(requireContext(), "模特：${model.id}", Toast.LENGTH_SHORT).show()
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
        likeList?.setNestedScrollingEnabled(false)
        likeList?.setItemViewCacheSize(20)
        likeList?.setHasTransientState(true)
    }

    private fun refreshLikeList() {
        val likeList: RecyclerView? = view?.findViewById(R.id.like_list)
        val adapter = likeList?.adapter as? likeListAdapter

        adapter?.notifyItemInserted(albumList.size - 4)
    }

    private fun requestLikesData(){
        albumIsLoading = true

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.GetRecAlbum()

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

                    refreshLikeList()
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
            val imageView = holder.itemView.findViewById<ImageView>(R.id.image_view)
            Glide.with(requireContext())
                .load("${albumList[position].images}?short500px")
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)

            val textView = holder.itemView.findViewById<TextView>(R.id.text_view)
            textView.text = "${albumList[position].model} ${albumList[position].name}"

            holder.itemView.setOnClickListener{
                openAlbumView(albumList[position].id)
            }
        }
    }
    class likeViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
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

