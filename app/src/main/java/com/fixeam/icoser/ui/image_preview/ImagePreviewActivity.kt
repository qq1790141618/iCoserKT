package com.fixeam.icoser.ui.image_preview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.batchat.preview.AlphaCallback
import com.batchat.preview.PreviewPictureView
import com.fixeam.icoser.R
import com.fixeam.icoser.model.downloadImage
import com.fixeam.icoser.model.shareImageContent
import com.google.android.material.button.MaterialButton

class ImagePreviewActivity : AppCompatActivity(), AlphaCallback {
    private var mPreviewPictureView: PreviewPictureView<String>? =null
    private var disPlayerUrl: String = ""

    @SuppressLint("NewApi", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        // 初始化预览控件
        mPreviewPictureView = findViewById(R.id.mPreviewPictureView)
        mPreviewPictureView?.start(index?:0,data?: arrayListOf(),this)

        // 获取内部的ViewPager2对象
        val viewPager = mPreviewPictureView?.getChildAt(0) as? ViewPager2
        val positionView = findViewById<TextView>(R.id.position_view)
        // 添加页面切换监听器
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                positionView.text = "${position + 1} / ${data?.size}"
                val url = data?.get(position)
                url?.let {
                    disPlayerUrl = it
                }
            }
        })

        disPlayerUrl = data?.get(intent.getIntExtra("index",0)).toString()
        // 设置下载和分享事件
        val download = findViewById<MaterialButton>(R.id.download)
        download.setOnClickListener {
            downloadImage(
                imageUrl = disPlayerUrl,
                context = this@ImagePreviewActivity
            )
        }
        val share = findViewById<MaterialButton>(R.id.share)
        share.setOnClickListener {
            shareImageContent(
                imageUrl = disPlayerUrl,
                context = this@ImagePreviewActivity
            )
        }
    }

    // 需要传递过来的数据
    private val data by lazy {
        intent?.getStringArrayListExtra("data")
    }

    // 需要传递过来的数据
    private val index by lazy {
        intent?.getIntExtra("index",0)
    }

    // 这个是必要的，因为共享元素的返回动画
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAfterTransition()
    }

    companion object{
        // 启动方法，作为参考
        fun start(activity: AppCompatActivity,index:Int,list: ArrayList<String>,view: View){
            // 构建共享元素的集合，可以多个，但需要注意一一对应
            // 详情请看文档 https://developer.android.com/guide/navigation/navigation-animate-transitions?hl=zh-cn
            val mPair: Array<androidx.core.util.Pair<View, String>?> = arrayOfNulls(1)

            ViewCompat.setTransitionName(view, "CONTENT")
            mPair[0] = androidx.core.util.Pair(view, "CONTENT")


            val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity, *mPair
            )
            val intent = Intent(activity, ImagePreviewActivity::class.java)
            intent.putStringArrayListExtra("data", list)
            intent.putExtra("index", index)
            // ActivityCompat是android支持库中用来适应不同android版本的
            ActivityCompat.startActivity(activity, intent, activityOptions.toBundle())
        }
    }

    override fun onChangeAlphaCallback(alpha: Float) {
        findViewById<View>(R.id.view).alpha = alpha
    }

    override fun onChangeClose() {
        onBackPressed()
    }
}