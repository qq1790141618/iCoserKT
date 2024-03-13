package com.fixeam.icoser.ui.about_page

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityAboutBinding
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.checkForUpdate
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.openUrlInBrowser
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startUpdateActivity

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 设置字体
        binding.appName.typeface = Typeface.createFromAsset(assets, "font/JosefinSans-Regular-7.ttf")

        // 设置备案号点击
        binding.beian.setOnClickListener {
            openUrlInBrowser(this, "https://beian.miit.gov.cn/")
        }

        // 添加选项
        initOptions()
    }

    private var clickTime = 0

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = packageInfo.versionName

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
            binding.optionList,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.cpu,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.running_platform,
                contentText = "Android ≥9",
                showHrefIcon = false,
                onClick = {
                    clickTime++
                    if(clickTime >= 5){
                        Toast.makeText(this, "别戳了, 这里什么都没有!", Toast.LENGTH_SHORT).show()
                        clickTime = 0
                    }
                }
            ),
            binding.optionList,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.android,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.sdk_version,
                contentText = "Api 31",
                showHrefIcon = false,
                onClick = {
                    clickTime++
                    if(clickTime >= 5){
                        Toast.makeText(this, "别戳了, 这里什么都没有!", Toast.LENGTH_SHORT).show()
                        clickTime = 0
                    }
                }
            ),
            binding.optionList,
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
                        sharedPreferences.edit().putInt("do_not_alert_version", -1).apply()
                    }
                    checkForUpdate(this@AboutActivity){
                        if(doNotAlertVersion >= 0){
                            sharedPreferences.edit().putInt("do_not_alert_version", doNotAlertVersion).apply()
                        }
                        if(it){
                            startUpdateActivity(this)
                        } else {
                            Toast.makeText(this, "当前已经是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ),
            binding.optionList,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.discover,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.official_website,
                onClick = {
                    openUrlInBrowser(this, "https://app.fixeam.com/")
                }
            ),
            binding.optionList,
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
                    openUrlInBrowser(this, "https://fixeam.com/")
                }
            ),
            binding.optionList,
            this,
            isDarken(this)
        )
    }
}