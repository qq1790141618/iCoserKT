package com.fixeam.icoser.ui.setting_page

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar

class SetColorThemeActivity : AppCompatActivity() {
    private var colorMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_item)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.color_theme)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        initOptions()
        val sharedPreferences = getSharedPreferences("theme", Context.MODE_PRIVATE)
        colorMode = sharedPreferences.getInt("color_mode", 0)
        setColorModeRatio(colorMode, false)
    }

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val optionLayout = findViewById<LinearLayout>(R.id.option_layout)
        val ratios = listOf(
            "跟随系统",
            "亮色模式",
            "暗色模式"
        )

        for (i in ratios.indices) {
            val ratio = ratios[i]

            initOptionItem(
                Option(
                    iconId = -1,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                    text = ratio,
                    showHrefIcon = false,
                    onClick = {
                        colorMode = i
                        setColorModeRatio(i, true)
                    }
                ),
                optionLayout,
                this,
                isDarken(this)
            )
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun setColorModeRatio(index: Int, set: Boolean){
        val optionLayout = findViewById<LinearLayout>(R.id.option_layout)
        val checkedIconId = R.drawable.check
        val childCount = optionLayout.childCount
        for (idx in 0..< childCount){
            val optionItem = optionLayout.getChildAt(idx) as LinearLayout
            val icon = optionItem.findViewById<ImageView>(R.id.left_icon)
            icon.setImageResource(checkedIconId)
            if(idx == index){
                icon.visibility = View.VISIBLE
            } else {
                icon.visibility = View.INVISIBLE
            }
        }

        if(!set){
            return
        }
        val sharedPreferences = getSharedPreferences("theme", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("color_mode", index).apply()
        Handler().postDelayed({
            when(index){
                0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }, 500)
    }
}