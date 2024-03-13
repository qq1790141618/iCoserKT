package com.fixeam.icoser.ui.main.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.FragmentUserBinding
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.createSimpleDialog
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.startLoginActivity
import com.fixeam.icoser.model.userFragment
import com.fixeam.icoser.network.UserInform
import com.fixeam.icoser.network.userCollection
import com.fixeam.icoser.network.userFollow
import com.fixeam.icoser.network.userHistory
import com.fixeam.icoser.network.userInform
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.ui.about_page.AboutActivity
import com.fixeam.icoser.ui.account_center.AccountCenterActivity
import com.fixeam.icoser.ui.collection_page.CollectionViewActivity
import com.fixeam.icoser.ui.collection_page.MediaFavorActivity
import com.fixeam.icoser.ui.follow_page.FollowActivity
import com.fixeam.icoser.ui.forbidden_page.ForbiddenActivity
import com.fixeam.icoser.ui.history_page.HistoryActivity
import com.fixeam.icoser.ui.image_preview.ImagePreviewActivity
import com.fixeam.icoser.ui.setting_page.SettingActivity
import com.fixeam.icoser.ui.user_center.UserCenterActivity

class UserFragment : Fragment() {
    private lateinit var binding: FragmentUserBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userCard.goLogin.setOnClickListener { startLoginActivity(requireContext()) }
        binding.userCard.logOut.setOnClickListener {
            createSimpleDialog(requireContext(), "确认退出此用户的登录状态吗? 用户在此设备上的信息将会被清空。", true){ logout() }
        }

        userInform?.let { initUserCard(it) }

        initOptions()
    }

    @SuppressLint("SetTextI18n")
    fun initUserCard(userInform: UserInform) {
        val avatar = binding.userCard.avatarImage
        Glide.with(requireContext())
            .load(userInform.header)
            .placeholder(R.drawable.image_holder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(avatar)
        avatar.setOnClickListener {
            val header: ArrayList<String> = arrayListOf(userInform.header)
            val activity = requireActivity() as AppCompatActivity
            ImagePreviewActivity.start(
                activity,
                0,
                header,
                avatar
            )
        }

        binding.userCard.nickname.text = userInform.nickname
        binding.userCard.uid.visibility = View.VISIBLE
        binding.userCard.uid.text = "UID${userInform.id}"
        binding.userCard.goLogin.visibility = View.GONE
        binding.userCard.logOut.visibility = View.VISIBLE
    }

    private fun logout(){
        requireContext().getSharedPreferences("user", Context.MODE_PRIVATE).edit().remove("access_token").apply()
        userInform = null
        userToken = null

        binding.userCard.avatarImage.setImageResource(R.drawable.image_holder)
        binding.userCard.nickname.text = getString(R.string.un_log_user)
        binding.userCard.uid.visibility = View.GONE
        binding.userCard.uid.text = ""
        binding.userCard.myFollow.text = 0.toString()
        binding.userCard.myCollection.text = 0.toString()
        binding.userCard.myHistory.text = 0.toString()
        binding.userCard.goLogin.visibility = View.VISIBLE
        binding.userCard.logOut.visibility = View.GONE
    }

    private fun initOptions() {
        binding.userCard.myFollowBlock.setOnClickListener {
            val intent = Intent(requireContext(), FollowActivity::class.java)
            startActivity(intent)
        }
        binding.userCard.myCollectionBlock.setOnClickListener {
            val intent = Intent(requireContext(), CollectionViewActivity::class.java)
            startActivity(intent)
        }
        binding.userCard.myHistoryBlock.setOnClickListener {
            val intent = Intent(requireContext(), HistoryActivity::class.java)
            startActivity(intent)
        }

        initOptionItem(
            Option(
                iconId = R.drawable.video,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F53F3F")),
                textId = R.string.media_favor,
                clearMargin = true,
                onClick = {
                    val intent = Intent(requireContext(), MediaFavorActivity::class.java)
                    startActivity(intent)
                }
            ),
            binding.aOption,
            requireActivity(),
            isDarken(requireActivity())
        )
        initOptionItem(
            Option(
                iconId = R.drawable.profile,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_inform,
                onClick = {
                    val intent = Intent(requireContext(), UserCenterActivity::class.java)
                    startActivity(intent)
                }
            ),
            binding.bOption,
            requireActivity(),
            isDarken(requireActivity())
        )
        initOptionItem(
            Option(
                iconId = R.drawable.safe,
                iconColor = ColorStateList.valueOf(Color.parseColor("#4CD263")),
                textId = R.string.safe_center,
                onClick = {
                    val intent = Intent(requireContext(), AccountCenterActivity::class.java)
                    startActivity(intent)
                }
            ),
            binding.bOption,
            requireActivity(),
            isDarken(requireActivity())
        )
        initOptionItem(
            Option(
                iconId = R.drawable.warn,
                iconColor = ColorStateList.valueOf(Color.parseColor("#F7BA1E")),
                textId = R.string.forbidden,
                onClick = {
                    val intent = Intent(requireContext(), ForbiddenActivity::class.java)
                    startActivity(intent)
                }
            ),
            binding.bOption,
            requireActivity(),
            isDarken(requireActivity())
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
            binding.bOption,
            requireActivity(),
            isDarken(requireActivity())
        )
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
            binding.cOption,
            requireActivity(),
            isDarken(requireActivity())
        )

        setFollowNumber(userFollow.size)
        setCollectionNumber(userCollection.size)
        if(userHistory != null){
            var number = 0
            for (timeRange in userHistory?.time_range!!){
                number += timeRange.count
            }
            userFragment?.setHistoryNumber(number)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setFollowNumber(number: Int) {
        if(number <= 0){
            return
        }
        binding.userCard.myFollow.text = number.toString()
    }

    @SuppressLint("SetTextI18n")
    fun setCollectionNumber(number: Int) {
        if(number <= 0){
            return
        }
        binding.userCard.myCollection.text = number.toString()
    }

    @SuppressLint("SetTextI18n")
    fun setHistoryNumber(number: Int) {
        if(number <= 0){
            return
        }
        binding.userCard.myHistory.text = number.toString()
    }
}