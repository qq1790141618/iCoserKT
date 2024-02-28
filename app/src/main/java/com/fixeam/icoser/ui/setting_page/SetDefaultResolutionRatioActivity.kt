package com.fixeam.icoser.ui.setting_page

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar

class SetDefaultResolutionRatioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_item)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.default_display)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        initOptions()
        val bestResolutionRatio = intent.getIntExtra("bestResolutionRatio", 720)
        setBestResolutionRatio(when(bestResolutionRatio){
            2160 -> 0
            1440 -> 1
            1080 -> 2
            720 -> 3
            540 -> 4
            else -> 2
        }, false)
    }

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val optionLayout = findViewById<LinearLayout>(R.id.option_layout)
        val ratios = listOf(
            R.string.ratio_2160p,
            R.string.ratio_1440p,
            R.string.ratio_1080p,
            R.string.ratio_720p,
            R.string.ratio_540p
        )


        for (i in ratios.indices) {
            val ratio = ratios[i]

            initOptionItem(
                Option(
                    iconId = -1,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                    textId = ratio,
                    showHrefIcon = false,
                    onClick = { setBestResolutionRatio(i, true) }
                ),
                optionLayout,
                this,
                isDarken(this)
            )
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun setBestResolutionRatio(index: Int, set: Boolean){
        val optionLayout = findViewById<LinearLayout>(R.id.option_layout)
        val checkedIconId = R.drawable.check
        val childCount = optionLayout.childCount
        for (idx in 0..< childCount){
            val optionItem = optionLayout.getChildAt(idx) as LinearLayout
            val icon = optionItem.findViewById<ImageView>(R.id.left_icon)
            icon.setImageResource(checkedIconId)
            val text = optionItem.findViewById<TextView>(R.id.text)
            if(idx == index){
                icon.visibility = View.VISIBLE
            } else {
                icon.visibility = View.INVISIBLE
            }
        }

        if(!set){
            return
        }
        val bestResolutionRatio = when(index){
            0 -> 2160
            1 -> 1440
            2 -> 1080
            3 -> 720
            4 -> 540
            else -> 1080
        }
        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("best_resolution_ratio", bestResolutionRatio).apply()
    }
}