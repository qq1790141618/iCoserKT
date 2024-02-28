package com.fixeam.icoser.ui.setting_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.fixeam.icoser.R
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.bytesToReadableSize
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.setting)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        initOptions()
    }

    private var bestResolutionRatio = 720
    private var colorMode = 0

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)
        initOptionItem(
            Option(
                iconId = R.drawable.display,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.default_display,
                onClick = {
                    val intent = Intent(this, SetDefaultResolutionRatioActivity::class.java)
                    intent.putExtra("bestResolutionRatio", bestResolutionRatio)
                    startActivity(intent)
                }
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.light,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.color_theme,
                clearMargin = true,
                onClick = {
                    val intent = Intent(this, SetColorThemeActivity::class.java)
                    startActivity(intent)
                }
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )

        val bOptionsContainer = findViewById<LinearLayout>(R.id.b_option)
        initOptionItem(
            Option(
                iconId = R.drawable.st_storage_port,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.cache_size,
                showHrefIcon = false
            ),
            bOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.clear,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.cache_clear,
                showHrefIcon = false,
                clearMargin = true,
                onClick = {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage("确认清除所有缓存吗? 可能会小幅影响页面加载速度。")

                    builder.setPositiveButton("确定") { _, _ ->
                        clearGlideCache(this)
                    }
                    builder.setNegativeButton("取消") { _, _ -> }

                    val alertDialog = builder.create()
                    alertDialog.show()
                }
            ),
            bOptionsContainer,
            this,
            isDarken(this)
        )

        setColorTheme()
        setBestResolutionRatio()
        setCacheSize()
    }

    override fun onResume() {
        super.onResume()

        setColorTheme()
        setBestResolutionRatio()
        setCacheSize()
    }

    @SuppressLint("SetTextI18n")
    private fun setBestResolutionRatio(){
        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        bestResolutionRatio = sharedPreferences.getInt("best_resolution_ratio", 720)

        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)
        val resolutionRatioOptionItem = aOptionsContainer.getChildAt(0)
        val contentText = resolutionRatioOptionItem.findViewById<TextView>(R.id.content_text)
        contentText.text = "${bestResolutionRatio}p"
    }

    @SuppressLint("SetTextI18n")
    private fun setColorTheme(){
        val sharedPreferences = getSharedPreferences("theme", Context.MODE_PRIVATE)
        colorMode = sharedPreferences.getInt("color_mode", 0)

        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)
        val colorModeItem = aOptionsContainer.getChildAt(1)
        val contentText = colorModeItem.findViewById<TextView>(R.id.content_text)
        contentText.text = when(colorMode){
            0 -> "跟随系统"
            1 -> "亮色模式"
            2 -> "暗色模式"
            else -> "跟随系统"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCacheSize(){
        var totalSize: Long = 0
        totalSize += getGlideCacheSize(this)

        val bOptionsContainer = findViewById<LinearLayout>(R.id.b_option)
        val resolutionRatioOptionItem = bOptionsContainer.getChildAt(0)
        val contentText = resolutionRatioOptionItem.findViewById<TextView>(R.id.content_text)
        contentText.text = bytesToReadableSize(totalSize.toInt())
    }

    private fun getGlideCacheSize(context: Context): Long {
        var totalSize: Long = 0

        try {
            val cacheDir: File? = Glide.getPhotoCacheDir(context)
            val size = cacheDir?.let { getFolderSize(it) }
            if (size != null) {
                totalSize += size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return totalSize
    }

    private fun getFolderSize(directory: File): Long {
        var length: Long = 0
        directory.listFiles()?.let { files ->
            for (file in files) {
                length += if (file.isFile) {
                    file.length()
                } else {
                    getFolderSize(file)
                }
            }
        }
        return length
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun clearGlideCache(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            Glide.get(context).clearDiskCache()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                setCacheSize()
            }
        }
    }
}