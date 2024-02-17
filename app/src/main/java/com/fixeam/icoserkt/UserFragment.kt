package com.fixeam.icoserkt

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UserFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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

            builder.setPositiveButton("确定") { dialog, which ->
                logout()
            }
            builder.setNegativeButton("取消") { dialog, which ->
                // 不退出登录
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

        userInform?.let { initUserCard(it) }
    }

    fun initUserCard(userInform: UserInform) {
        val avatar = view?.findViewById<ImageView>(R.id.avatar_image)
        avatar?.let {
            Glide.with(requireContext())
                .load("${userInform.header}")
                .placeholder(R.drawable.image_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(it)
        }
        avatar?.setOnClickListener {
            imageViewInstantiate(userInform.header)
        }

        val nickname = view?.findViewById<TextView>(R.id.nickname)
        nickname?.text = userInform.nickname

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

     fun imageViewInstantiate(url: String){
        val imageViewPrev = imagePreview?.findViewById<AppCompatImageView>(R.id.image_view_prev)
        imageViewPrev?.let {
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageViewPrev)
        }
        imagePreview?.visibility = View.VISIBLE
    }
}