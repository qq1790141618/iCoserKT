package com.fixeam.icoser.ui.account_center

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityAccountCenterBinding
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.network.userToken

class AccountCenterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountCenterBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = getString(R.string.safe_center)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            startLoginActivity(this)
        } else {
            val webView = binding.webView
            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.allowFileAccess = true

            webView.webViewClient = WebViewClient()
            webView.webChromeClient = WebChromeClient()
            webView.loadUrl("https://app.fixeam.com/account-center?access_token=$userToken")
            webView.reload()
        }
    }
}