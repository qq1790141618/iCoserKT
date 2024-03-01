package com.fixeam.icoser.ui.setting_page

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.hasNotificationProgression
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.google.android.material.snackbar.Snackbar

class SetNotificationActivity : AppCompatActivity() {
    private var allowedNotification = 1
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
        val sharedPreferences = getSharedPreferences("notification", Context.MODE_PRIVATE)
        allowedNotification = sharedPreferences.getInt("allow", 1)
        setColorModeRatio(1 - allowedNotification, false)
    }

    @SuppressLint("CommitPrefEdits")
    private fun initOptions() {
        val optionLayout = findViewById<LinearLayout>(R.id.option_layout)
        val ratios = listOf(
            "允许推送",
            "关闭推送"
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
                        allowedNotification = 1 - i
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
        val sharedPreferences = getSharedPreferences("notification", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if(hasNotificationProgression){
            editor.putInt("allow", 1 - index).apply()
        } else {
            if(1 - index == 0){
                editor.putInt("allow", 1 - index).apply()
            } else {
                val snackBar = Snackbar.make(requireViewById(R.id.set_item), "无法开启消息推送, 请检查是否具有相应权限。", Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction(getString(R.string.confirm)) { }.show()
                setColorModeRatio(1, false)
            }
        }
    }
}