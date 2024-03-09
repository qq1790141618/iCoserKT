package com.fixeam.icoser.ui.forbidden_page

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityForbbidenBinding
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.startAlbumActivity
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.startModelActivity
import com.fixeam.icoser.network.getUserForbidden
import com.fixeam.icoser.network.removeForbidden
import com.fixeam.icoser.network.userForbidden
import com.fixeam.icoser.network.userToken

class ForbiddenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForbbidenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForbbidenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            startLoginActivity(this)
        }

        initPage()
    }

    private fun initPage(){
        // 设置加载动画
        val imageView = binding.imageLoading
        val animation = AnimationUtils.loadAnimation(this, R.anim.loading)
        imageView.startAnimation(animation)
        imageView.visibility = View.VISIBLE

        // 设置导航栏
        val toolbar: Toolbar = binding.toolbar
        toolbar.title = "加载中..."
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 再次获取用户屏蔽内容
        getUserForbidden(this){
            imageView.clearAnimation()
            imageView.visibility = View.GONE
            toolbar.title = getString(R.string.forbidden)

            initForbiddenList()
        }
    }

    private fun initForbiddenList(){
        binding.aOption.removeAllViews()

        for ((index, forbidden) in userForbidden.withIndex()){
            val content = forbidden.content
            val contentType = when(forbidden.type){
                "album" -> "写真集"
                "model" -> "模特"
                else -> "写真集"
            }
            val name =  when(forbidden.type){
                "album" -> content["model_name"].toString() + " " + content["name"].toString()
                "model" -> content["name"].toString()
                else -> content["name"].toString()
            }
            val iconId = when(forbidden.type){
                "album" -> R.drawable.album
                "model" -> R.drawable.friend
                else -> R.drawable.album
            }
            val leftImageUrl = when(forbidden.type){
                "album" -> content["poster"].toString()
                "model" -> content["avatar_image"].toString()
                else -> null
            }

            initOptionItem(
                Option(
                    iconId = iconId,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                    textId = -1,
                    text = name,
                    contentText = contentType,
                    leftImageUrl = leftImageUrl,
                    showHrefIcon = false,
                    clearMargin = index == userForbidden.size - 1,
                    showRemoveButton = true,
                    onClick = {
                        if(forbidden.type == "album"){
                            startAlbumActivity(this, forbidden.res_id.toInt(), true)
                        }
                        if(forbidden.type == "model"){
                            startModelActivity(this, forbidden.res_id.toInt(), true)
                        }
                    },
                    onRemove = {
                        val builder = AlertDialog.Builder(this)
                        builder.setMessage("确认解除对内容($contentType $name)的屏蔽吗?")

                        builder.setPositiveButton("确定") { _, _ ->
                            removeForbidden(this, forbidden.id){
                                initPage()
                            }
                        }
                        builder.setNegativeButton("取消") { _, _ -> }

                        val alertDialog = builder.create()
                        alertDialog.show()
                    }
                ),
                binding.aOption,
                this,
                isDarken(this)
            )
        }
    }
}