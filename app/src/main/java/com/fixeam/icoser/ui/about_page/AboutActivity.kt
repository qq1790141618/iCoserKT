package com.fixeam.icoser.ui.about_page

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.R
import com.fixeam.icoser.ui.update_dialog.UpdateActivity
import com.fixeam.icoser.model.checkForUpdate
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置字体
        val appName = findViewById<TextView>(R.id.app_name)
        val typeface = Typeface.createFromAsset(assets, "font/JosefinSans-Regular-7.ttf")
        appName.typeface = typeface

        initOptions()

        val beian = findViewById<TextView>(R.id.beian)
        beian.setOnClickListener {
            val url = "https://beian.miit.gov.cn"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private var clickTime = 0

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val optionsContainer = findViewById<LinearLayout>(R.id.option_list)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = packageInfo.versionName

        Log.d("checkForUpdate", "$version")

        initOptionItem(
            Option(
                iconId = R.drawable.logo,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.app_version,
                contentText = version,
                showHrefIcon = false,
                onClick = {
                    clickTime++
                    if(clickTime >= 5){
                        Toast.makeText(this, "别戳了, 这里什么都没有!", Toast.LENGTH_SHORT).show()
                        clickTime = 0
                    }
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.cpu,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.running_platform,
                contentText = "Android 9-14",
                showHrefIcon = false,
                onClick = {
                    clickTime++
                    if(clickTime >= 5){
                        Toast.makeText(this, "别戳了, 这里什么都没有!", Toast.LENGTH_SHORT).show()
                        clickTime = 0
                    }
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.android,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.sdk_version,
                contentText = "Api 28-34",
                showHrefIcon = false,
                onClick = {
                    clickTime++
                    if(clickTime >= 5){
                        Toast.makeText(this, "别戳了, 这里什么都没有!", Toast.LENGTH_SHORT).show()
                        clickTime = 0
                    }
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.pull_up,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.update,
                onClick = {
                    val sharedPreferences = getSharedPreferences("version", MODE_PRIVATE)
                    val doNotAlertVersion = sharedPreferences.getInt("do_not_alert_version", -1)
                    if(doNotAlertVersion >= 0){
                        sharedPreferences.edit().putInt("do_not_alert_version", -1)
                    }
                    checkForUpdate(this@AboutActivity){
                        if(doNotAlertVersion >= 0){
                            sharedPreferences.edit().putInt("do_not_alert_version", doNotAlertVersion)
                        }
                        if(it){
                            val intent = Intent(this, UpdateActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "当前已经是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.discover,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.official_website,
                onClick = {
                    val url = "https://app.fixeam.com"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.link,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.fixeam_official_website,
                clearMargin = true,
                onClick = {
                    val url = "https://fixeam.com"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            ),
            optionsContainer,
            this,
            isDarken(this)
        )
    }
}