package com.fixeam.icoserkt.ui.setting_page

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.model.Option
import com.fixeam.icoserkt.model.isDarken
import com.fixeam.icoserkt.model.setStatusBar

class SetDefaultResolutionRatioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_default_resolution_ratio)

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
                optionLayout
            )
        }
    }

    private fun initOptionItem(option: Option, root: ViewGroup){
        val optionItem = layoutInflater.inflate(R.layout.option_item, root, false)

        val textView = optionItem.findViewById<TextView>(R.id.text)
        textView.text = getString(option.textId)

        val leftIcon = optionItem.findViewById<ImageView>(R.id.left_icon)
        if(option.iconId > 0){
            leftIcon.setImageResource(option.iconId)
            leftIcon.imageTintList = option.iconColor
        } else {
            leftIcon.visibility = View.INVISIBLE
        }

        if(!option.showHrefIcon){
            val rightIcon = optionItem.findViewById<ImageView>(R.id.right_icon)
            rightIcon.visibility = View.GONE
        }

        if(option.tagText != null){
            val tagText = optionItem.findViewById<TextView>(R.id.tag_text)
            tagText.text = option.tagText
        }

        if(option.contentText != null){
            val contentText = optionItem.findViewById<TextView>(R.id.content_text)
            contentText.text = option.contentText
        }

        if(option.clearMargin){
            val layoutParams = LinearLayout.LayoutParams(
                optionItem.layoutParams.width,
                optionItem.layoutParams.height
            )
            layoutParams.bottomMargin = 0
            optionItem.layoutParams = layoutParams
        }

        var pressDownColor = Color.parseColor("#F6F6F6")
        var pressUpColor = Color.parseColor("#FFFFFF")
        if(isDarken(this)){
            pressDownColor = Color.parseColor("#222222")
            pressUpColor = Color.parseColor("#000000")
        }
        var downTime: Long = 0

        optionItem.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        optionItem.setBackgroundColor(pressDownColor)
                        downTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        optionItem.setBackgroundColor(pressUpColor)
                        val upTime = System.currentTimeMillis()
                        val duration = upTime - downTime
                        if(duration < 300){
                            option.onClick()
                            option.onClickWithContext(optionItem)
                        }
                        return true  // 返回true表示消费了该事件
                    }
                }
                return false
            }
        })

        root.addView(optionItem)
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