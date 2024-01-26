package com.fixeam.icoserkt

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.youth.banner.Banner
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

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
        // 加载瀑布流推荐
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "iCoser_Android_Application_By_Kotlin")
                .build()
            chain.proceed(authenticatedRequest)
        }
        .build()

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
                                    .load(data.url)
                                    .into(holder.imageView)
                            }
                        })
                        ?.addBannerLifecycleObserver(this@HomeFragment)
                        ?.setLoopTime(5000)
                        ?.setScrollTime(900)
                        ?.setIndicator(CircleIndicator(requireContext()))
                        ?.setOnBannerListener { carousel, position ->
                            val albumId = carousels[position].link.content.id
                            Toast.makeText(requireContext(), "点击的写真集ID是${albumId}", Toast.LENGTH_SHORT).show()
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
                                    .load(album.poster)
                                    .placeholder(R.drawable.image_holder)
                                    .into(imageView)
                                textView.text = album.name
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
