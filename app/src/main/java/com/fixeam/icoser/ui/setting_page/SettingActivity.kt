package com.fixeam.icoser.ui.setting_page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivitySettingBinding
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
    private lateinit var binding: ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = getString(R.string.setting)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        initOptions()
    }

    private var bestResolutionRatio = 720
    private var colorMode = 0
    private var allowedNotification = 1

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
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
            binding.aOption,
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
            binding.aOption,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.notice,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.notification_push,
                clearMargin = true,
                onClick = {
                    val intent = Intent(this, SetNotificationActivity::class.java)
                    startActivity(intent)
                }
            ),
            binding.aOption,
            this,
            isDarken(this)
        )

        initOptionItem(
            Option(
                iconId = R.drawable.st_storage_port,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.cache_size,
                showHrefIcon = false
            ),
            binding.bOption,
            this,
            isDarken(this)
        )

        // Glide缓存大小
        val sharedPreferences = getSharedPreferences("glide_module", Context.MODE_PRIVATE)
        val cacheSize = sharedPreferences.getInt("disk_cache_size_gb", 10)
        initOptionItem(
            Option(
                iconId = R.drawable.outline_tune,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.set_max_cache_size,
                contentText = "$cacheSize GB",
                showHrefIcon = false
            ),
            binding.bOption,
            this,
            isDarken(this)
        )

        // 添加滑动步进器
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val seekBar = SeekBar(this)
        seekBar.progress = cacheSize
        seekBar.max = 10
        seekBar.min = 1

        if(isDarken(this)){
            layout.setBackgroundColor(Color.BLACK)
        } else {
            layout.setBackgroundColor(Color.WHITE)
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(25, 5 ,25, 5)
        seekBar.layoutParams = layoutParams

        val textView = TextView(this)
        val layoutParams1 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams1.setMargins(60, 0 ,60, 15)
        textView.setTextColor(Color.RED)
        textView.text = "清除缓存后重启应用生效"
        textView.textSize = 12F
        textView.visibility = View.GONE
        textView.layoutParams = layoutParams1

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val contentText = binding.bOption.getChildAt(1).findViewById<TextView>(R.id.content_text)
                contentText.text = "$progress GB"
                sharedPreferences.edit().putInt("disk_cache_size_gb", progress).apply()

                if(progress != cacheSize){
                    textView.visibility = View.VISIBLE
                } else {
                    textView.visibility = View.GONE
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 开始滑动滑块时触发
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 停止滑动滑块时触发
            }
        })

        layout.addView(seekBar)
        layout.addView(textView)
        binding.bOption.addView(layout)

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
            binding.bOption,
            this,
            isDarken(this)
        )

        setColorTheme()
        setBestResolutionRatio()
        setNotificationAllowed()
        setCacheSize()
    }

    override fun onResume() {
        super.onResume()

        setColorTheme()
        setNotificationAllowed()
        setBestResolutionRatio()
        setCacheSize()
    }

    @SuppressLint("SetTextI18n")
    private fun setBestResolutionRatio(){
        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        bestResolutionRatio = sharedPreferences.getInt("best_resolution_ratio", 720)

        val resolutionRatioOptionItem = binding.aOption.getChildAt(0)
        val contentText = resolutionRatioOptionItem.findViewById<TextView>(R.id.content_text)
        contentText.text = "${bestResolutionRatio}p"
    }

    @SuppressLint("SetTextI18n")
    private fun setColorTheme(){
        val sharedPreferences = getSharedPreferences("theme", Context.MODE_PRIVATE)
        colorMode = sharedPreferences.getInt("color_mode", 0)

        val colorModeItem = binding.aOption.getChildAt(1)
        val contentText = colorModeItem.findViewById<TextView>(R.id.content_text)
        contentText.text = when(colorMode){
            0 -> "跟随系统"
            1 -> "亮色模式"
            2 -> "暗色模式"
            else -> "跟随系统"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setNotificationAllowed(){
        val sharedPreferences = getSharedPreferences("notification", Context.MODE_PRIVATE)
        allowedNotification = sharedPreferences.getInt("allow", 1)

        val colorModeItem = binding.aOption.getChildAt(2)
        val contentText = colorModeItem.findViewById<TextView>(R.id.content_text)
        contentText.text = when(allowedNotification){
            0 -> "关闭推送"
            1 -> "允许推送"
            else -> "允许推送"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCacheSize(){
        var totalSize: Long = 0
        totalSize += getGlideCacheSize(this)

        val resolutionRatioOptionItem = binding.bOption.getChildAt(0)
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