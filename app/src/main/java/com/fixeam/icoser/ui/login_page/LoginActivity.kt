package com.fixeam.icoser.ui.login_page

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityLoginBinding
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.requestPasswordLogin
import com.fixeam.icoser.network.requestVerifyCodeLogin
import com.fixeam.icoser.network.sendCode
import com.fixeam.icoser.network.verifyTokenAndGetUserInform


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var loginMode: Int = 2
    private var verifyId: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.parseColor("#F2F3FF"), Color.parseColor("#0D0C00"))

        // 绑定按钮事件
        binding.loginWithAccountAndPassword.changeModeTo1.setOnClickListener { changeToMode(1) }
        binding.loginWithVerifyCode.changeModeTo2.setOnClickListener { changeToMode(2) }
        binding.loginWithVerifyCode.sendVerifyCode.setOnClickListener { sendVerifyCode() }
        binding.loginWithAccountAndPassword.login1.setOnClickListener { loginWithPassword() }
        binding.loginWithVerifyCode.login2.setOnClickListener { loginWithVerifyCode() }

        // 设置默认的登录模式
        changeToMode(1)
        // 获取用户手机号
        requestPermission(Manifest.permission.READ_PHONE_STATE)
        requestPermission(Manifest.permission.READ_PHONE_NUMBERS)
        requestPermission(Manifest.permission.READ_SMS)
    }

    // 权限请求启动器
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ _: Boolean -> }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermission(permission: String, callback: () -> Unit = {}) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission,
            ) == PackageManager.PERMISSION_GRANTED -> {
                callback()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                permission
            ) -> {
                requestPermissionLauncher.launch(
                    permission
                )
            }

            else -> {
                requestPermissionLauncher.launch(
                    permission
                )
            }
        }
    }

    // 切换模式
    private fun changeToMode(number: Int){
        loginMode = number
        when(number){
            1->{
                binding.loginWithVerifyCode.root.visibility = View.VISIBLE
                binding.loginWithAccountAndPassword.root.visibility = View.GONE
            }
            2->{
                binding.loginWithAccountAndPassword.root.visibility = View.VISIBLE
                binding.loginWithVerifyCode.root.visibility = View.GONE
            }
        }
    }
    // 发送验证码
    private fun sendVerifyCode(){
        val targetInput = binding.loginWithVerifyCode.target
        val target = targetInput.text?.toString()?.trim()
        val sendButton = binding.loginWithVerifyCode.sendVerifyCode
        sendButton.isEnabled = false
        sendButton.text = "发送中..."

        sendCode(this, target){
            if (it != null) {
                val countDownTimer = object : CountDownTimer(60000, 1000) {
                    @SuppressLint("SetTextI18n")
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsRemaining = millisUntilFinished / 1000
                        sendButton.text = secondsRemaining.toString() + getString(R.string.second_later_resend)
                    }
                    override fun onFinish() {
                        sendButton.text = getString(R.string.send_verify_code)
                        sendButton.isEnabled = true
                    }
                }
                verifyId = it
                countDownTimer.start()
            } else {
                sendButton.text = getString(R.string.send_verify_code)
                sendButton.isEnabled = true
            }
        }
    }
    // 账号密码登录
    private fun loginWithPassword(){
        val account = binding.loginWithAccountAndPassword.account.text?.toString()?.trim()
        val password = binding.loginWithAccountAndPassword.password.text?.toString()?.trim()
        val login1 = binding.loginWithAccountAndPassword.login1
        login1.isEnabled = false
        login1.text = "登录中..."

        requestPasswordLogin(this, account, password){
            if(it != null){
                saveTokenAndGetUserInform(it)
                login1.text = getString(R.string.login)
                login1.isEnabled = true
            } else {
                login1.text = getString(R.string.login)
                login1.isEnabled = true
            }
        }
    }
    // 验证码登录
    private fun loginWithVerifyCode(){
        val target = binding.loginWithVerifyCode.target.text?.toString()?.trim()
        val codeText = binding.loginWithVerifyCode.code.text?.toString()?.trim()
        val login2 = binding.loginWithVerifyCode.login2
        login2.isEnabled = false
        login2.text = "登录中..."

        requestVerifyCodeLogin(this, target, verifyId, codeText){
            if(it != null){
                saveTokenAndGetUserInform(it)
                login2.text = getString(R.string.login)
                login2.isEnabled = true
            } else {
                login2.text = getString(R.string.login)
                login2.isEnabled = true
            }
        }
    }
    // 保存token并获取用户信息
    private fun saveTokenAndGetUserInform(accessToken: String){
        val sharedPref = getSharedPreferences("user", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("access_token", accessToken)
        editor.apply()
        verifyTokenAndGetUserInform(accessToken, this)
        onBackPressed()
    }
}