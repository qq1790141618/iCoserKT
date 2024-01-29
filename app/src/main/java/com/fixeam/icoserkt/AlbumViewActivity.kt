package com.fixeam.icoserkt

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlbumViewActivity : AppCompatActivity() {
    private var albumImages: MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_view)

        // 设置加载动画
        val imageView = findViewById<ImageView>(R.id.image_loading)
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView?.startAnimation(animation)
        imageView?.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置悬浮按钮
        val toUpButton: FloatingActionButton = findViewById(R.id.to_up)
        val webView: WebView = findViewById(R.id.image_list)
        toUpButton.setOnClickListener {
            val animator = ValueAnimator.ofInt(webView.scrollY, 0)
            animator.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                webView.scrollTo(0, value)
            }
            animator.duration = 500
            animator.start()
        }

        val id = intent.getIntExtra("id", -1)
        requireAlbumContent(id)
    }

    private fun requireAlbumContent(id: Int){
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)

        var condition = JSONArray()
        condition.put(JSONArray().apply {
            put("id")
            put(id.toString())
        })
        val call = ApiService.GetAlbum(condition.toString())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)

                    if(!albumsResponse.result){
                        return
                    }

                    val album: Albums = albumsResponse.data[0]
                    albumImages = album.images as MutableList<String>

                    val imageView = findViewById<ImageView>(R.id.image_loading)
                    imageView.clearAnimation()
                    imageView.visibility = View.GONE

                    val toolbar: Toolbar = findViewById(R.id.toolbar)
                    toolbar.title = album.name
                    toolbar.subtitle = album.model

                    initImageList()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@AlbumViewActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initImageList(){
        val webView = this@AlbumViewActivity.findViewById<WebView>(R.id.image_list)
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("file:///android_asset/html/image_list.html")

        val json = Gson().toJson(albumImages)
        webView.postDelayed({
            webView.evaluateJavascript("initList($json);", null)
        }, 3000)
    }
}