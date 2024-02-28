package com.fixeam.icoserkt.ui.setting_page

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.MenuPopupWindow
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.model.Option
import com.fixeam.icoserkt.model.isDarken
import com.fixeam.icoserkt.model.setStatusBar
import java.lang.reflect.Field


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

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)

        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        bestResolutionRatio = sharedPreferences.getInt("best_resolution_ratio", 720)

        initOptionItem(
            Option(
                iconId = R.drawable.display,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.default_display,
                contentText = "${bestResolutionRatio}p",
                onClickWithContext = {
                    val intent = Intent(this, SetDefaultResolutionRatioActivity::class.java)
                    intent.putExtra("bestResolutionRatio", bestResolutionRatio)
                    startActivity(intent)
                }
            ),
            aOptionsContainer
        )
    }

    private fun initOptionItem(option: Option, root: ViewGroup){
        val optionItem = layoutInflater.inflate(R.layout.option_item, root, false)

        val textView = optionItem.findViewById<TextView>(R.id.text)
        textView.text = getString(option.textId)

        if(option.iconId > 0){
            val leftIcon = optionItem.findViewById<ImageView>(R.id.left_icon)
            leftIcon.setImageResource(option.iconId)
            leftIcon.imageTintList = option.iconColor
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

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        val sharedPreferences = getSharedPreferences("video_progress", Context.MODE_PRIVATE)
        bestResolutionRatio = sharedPreferences.getInt("best_resolution_ratio", 720)
        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)
        val resolutionRatioOptionItem = aOptionsContainer.getChildAt(0)
        val contentText = resolutionRatioOptionItem.findViewById<TextView>(R.id.content_text)
        contentText.text = "${bestResolutionRatio}p"
    }
}