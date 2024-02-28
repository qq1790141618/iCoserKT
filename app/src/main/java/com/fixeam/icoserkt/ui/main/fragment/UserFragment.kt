package com.fixeam.icoserkt.ui.main.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoserkt.R
import com.fixeam.icoserkt.model.Option
import com.fixeam.icoserkt.model.imageViewInstantiate
import com.fixeam.icoserkt.model.isDarken
import com.fixeam.icoserkt.model.removeSharedPreferencesKey
import com.fixeam.icoserkt.model.userFragment
import com.fixeam.icoserkt.network.UserInform
import com.fixeam.icoserkt.network.userCollection
import com.fixeam.icoserkt.network.userFollow
import com.fixeam.icoserkt.network.userHistory
import com.fixeam.icoserkt.network.userInform
import com.fixeam.icoserkt.network.userToken
import com.fixeam.icoserkt.ui.about_page.AboutActivity
import com.fixeam.icoserkt.ui.login_page.LoginActivity
import com.fixeam.icoserkt.ui.main.activity.mainImagePreview
import com.fixeam.icoserkt.ui.setting_page.SettingActivity
import com.google.android.material.button.MaterialButton

class UserFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goLogin: MaterialButton = view.findViewById(R.id.go_login)
        goLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        val logout = view.findViewById<MaterialButton>(R.id.log_out)
        logout.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("确认退出此用户的登录状态吗? 用户在此设备上的信息将会被清空。")

            builder.setPositiveButton("确定") { _, _ ->
                logout()
            }
            builder.setNegativeButton("取消") { _, _ ->
                // 不退出登录
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        userInform?.let { initUserCard(it) }

        initOptions()
    }

    @SuppressLint("SetTextI18n")
    fun initUserCard(userInform: UserInform) {
        val avatar = view?.findViewById<ImageView>(R.id.avatar_image)
        avatar?.let {
            Glide.with(requireContext())
                .load(userInform.header)
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(it)
        }
        avatar?.setOnClickListener {
            mainImagePreview?.let { it1 ->
                imageViewInstantiate(userInform.header, requireContext(),
                    it1
                )
            }
        }

        val nickname = view?.findViewById<TextView>(R.id.nickname)
        nickname?.text = userInform.nickname
        val uid = view?.findViewById<TextView>(R.id.uid)
        uid?.text = "UID${userInform.id}"

        val goLogin = view?.findViewById<MaterialButton>(R.id.go_login)
        val logout = view?.findViewById<MaterialButton>(R.id.log_out)
        goLogin?.visibility = View.GONE
        logout?.visibility = View.VISIBLE
    }

    private fun logout(){
        removeSharedPreferencesKey("access_token", requireContext())
        userInform = null
        userToken = null

        val avatar = view?.findViewById<ImageView>(R.id.avatar_image)
        avatar?.setImageResource(R.drawable.image_holder)

        val nickname = view?.findViewById<TextView>(R.id.nickname)
        nickname?.text = getString(R.string.un_log_user)

        val goLogin = view?.findViewById<MaterialButton>(R.id.go_login)
        val logout = view?.findViewById<MaterialButton>(R.id.log_out)
        goLogin?.visibility = View.VISIBLE
        logout?.visibility = View.GONE
    }

    private fun initOptions() {
        val aOptionsContainer = view?.findViewById<LinearLayout>(R.id.a_option)
        aOptionsContainer?.let {
            initOptionItem(
                Option(
                    iconId = R.drawable.like,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                    textId = R.string.my_following,
                    onClick = {

                    }
                ),
                it
            )
            initOptionItem(
                Option(
                    iconId = R.drawable.favor,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#FADC6D")),
                    textId = R.string.my_collection,
                    onClick = {

                    }
                ),
                it
            )
            initOptionItem(
                Option(
                    iconId = R.drawable.footprint,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#7BC0FC")),
                    textId = R.string.my_history,
                    clearMargin = true,
                    onClick = {

                    }
                ),
                it
            )
        }
        val bOptionsContainer = view?.findViewById<LinearLayout>(R.id.b_option)
        bOptionsContainer?.let {
            initOptionItem(
                Option(
                    iconId = R.drawable.profile,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                    textId = R.string.user_inform,
                    onClick = {

                    }
                ),
                it
            )
            initOptionItem(
                Option(
                    iconId = R.drawable.safe,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#4CD263")),
                    textId = R.string.safe_center,
                    onClick = {

                    }
                ),
                it
            )
            initOptionItem(
                Option(
                    iconId = R.drawable.warn,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#F7BA1E")),
                    textId = R.string.forbidden,
                    onClick = {

                    }
                ),
                it
            )
            initOptionItem(
                Option(
                    iconId = R.drawable.settings,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#A871E3")),
                    textId = R.string.setting,
                    clearMargin = true,
                    onClick = {
                        val intent = Intent(requireContext(), SettingActivity::class.java)
                        startActivity(intent)
                    }
                ),
                it
            )
        }
        val cOptionsContainer = view?.findViewById<LinearLayout>(R.id.c_option)
        cOptionsContainer?.let {
            initOptionItem(
                Option(
                    iconId = R.drawable.info,
                    iconColor = ColorStateList.valueOf(Color.parseColor("#6AA1FF")),
                    textId = R.string.about,
                    clearMargin = true,
                    onClick = {
                        val intent = Intent(requireContext(), AboutActivity::class.java)
                        startActivity(intent)
                    }
                ),
                it
            )
        }
        setFollowNumber(userFollow.size)
        setCollectionNumber(userCollection.size)
        if(userHistory == null){
            return
        }
        var number = 0
        for (timeRange in userHistory?.time_range!!){
            number += timeRange.count
        }
        userFragment?.setHistoryNumber(number)
    }

    private fun initOptionItem(option: Option, root: ViewGroup){
            val optionItem = layoutInflater.inflate(R.layout.option_item, root, false)

            val textView = optionItem.findViewById<TextView>(R.id.text)
            textView.text = getString(option.textId)

            val leftIcon = optionItem.findViewById<ImageView>(R.id.left_icon)
            leftIcon.setImageResource(option.iconId)
            leftIcon.imageTintList = option.iconColor

            if(!option.showHrefIcon){
                val rightIcon = optionItem.findViewById<ImageView>(R.id.right_icon)
                rightIcon.visibility = View.GONE
            }

            if(option.tagText != null){
                val tagText = optionItem.findViewById<TextView>(R.id.tag_text)
                tagText.text = option.tagText
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
            if(activity?.let { isDarken(it) } == true){
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
    fun setFollowNumber(number: Int) {
        if(number <= 0){
            return
        }
        val aOptionsContainer = view?.findViewById<LinearLayout>(R.id.a_option)
        val followItem = aOptionsContainer?.getChildAt(0) as LinearLayout
        val tagText = followItem.findViewById<TextView>(R.id.tag_text)
        tagText.text = "($number)"
    }

    @SuppressLint("SetTextI18n")
    fun setCollectionNumber(number: Int) {
        if(number <= 0){
            return
        }
        val aOptionsContainer = view?.findViewById<LinearLayout>(R.id.a_option)
        val collectionItem = aOptionsContainer?.getChildAt(1) as LinearLayout
        val tagText = collectionItem.findViewById<TextView>(R.id.tag_text)
        tagText.text = "($number)"
    }

    @SuppressLint("SetTextI18n")
    fun setHistoryNumber(number: Int) {
        if(number <= 0){
            return
        }
        val aOptionsContainer = view?.findViewById<LinearLayout>(R.id.a_option)
        val historyItem = aOptionsContainer?.getChildAt(2) as LinearLayout
        val tagText = historyItem.findViewById<TextView>(R.id.tag_text)
        tagText.text = "($number)"
    }
}