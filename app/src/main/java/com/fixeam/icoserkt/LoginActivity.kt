package com.fixeam.icoserkt

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    private var loginMode: Int = 1
    private var verifyId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val currentTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
            setStatusBarColor(Color.BLACK)
            setStatusBarTextColor(false)
        } else {
            setStatusBarColor(Color.WHITE)
            setStatusBarTextColor(true)
        }

        // 绑定按钮事件
        val changeModeButton1 = findViewById<MaterialButton>(R.id.change_mode_to_1)
        val changeModeButton2 = findViewById<MaterialButton>(R.id.change_mode_to_2)
        changeModeButton1.setOnClickListener {
            changeToMode(1)
        }
        changeModeButton2.setOnClickListener {
            changeToMode(2)
        }
        val sendButton = findViewById<MaterialButton>(R.id.send_verify_code)
        sendButton.setOnClickListener {
            sendVerifyCode()
        }
        val login1 = findViewById<MaterialButton>(R.id.login_1)
        val login2 = findViewById<MaterialButton>(R.id.login_2)
        login1.setOnClickListener {
            loginWithPassword()
        }
        login2.setOnClickListener {
            loginWithVerifyCode()
        }
    }

    private fun changeToMode(number: Int){
        loginMode = number
        val mode1 = findViewById<LinearLayout>(R.id.mode_1)
        val mode2 = findViewById<LinearLayout>(R.id.mode_2)

        when(number){
            1->{
                mode2.visibility = View.VISIBLE
                mode1.visibility = View.GONE
            }
            2->{
                mode1.visibility = View.VISIBLE
                mode2.visibility = View.GONE
            }
        }
    }

    private fun sendVerifyCode(){
        val targetInput = findViewById<TextInputEditText>(R.id.target)
        val target = targetInput.text?.toString()?.trim()
        if(target.isNullOrEmpty()){
            Toast.makeText(this@LoginActivity, "请输入手机号或者邮箱", Toast.LENGTH_SHORT).show()
            return
        }

        val phonePattern = Regex("""^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$""")
        val mailPattern = Regex("""^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$""")

        if(!target.matches(phonePattern) && !target.matches(mailPattern)){
            Toast.makeText(this@LoginActivity, "请输入正确的11位手机号或者电子邮箱", Toast.LENGTH_SHORT).show()
            return
        }

        val sendButton = findViewById<MaterialButton>(R.id.send_verify_code)
        sendButton.isEnabled = false
        sendButton.text = "发送中..."

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.SendVerifyCode(target)

        val countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                sendButton.text = secondsRemaining.toString() + getString(R.string.second_later_resend)
            }

            override fun onFinish() {
                sendButton.text = getString(R.string.send_verify_code)
                sendButton.isEnabled = true
            }
        }

        call.enqueue(object : Callback<SendVerifyCodeResponse> {
            override fun onResponse(call: Call<SendVerifyCodeResponse>, response: Response<SendVerifyCodeResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        if(responseBody.result){
                            verifyId = responseBody.verify_id
                            countDownTimer.start()
                        } else {
                            sendButton.text = getString(R.string.send_verify_code)
                            sendButton.isEnabled = true
                        }

                        if(responseBody.message != null){
                            Toast.makeText(this@LoginActivity, responseBody.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<SendVerifyCodeResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@LoginActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loginWithPassword(){
        val accountInput = findViewById<TextInputEditText>(R.id.account)
        val account = accountInput.text?.toString()?.trim()

        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val password = passwordInput.text?.toString()?.trim()

        if(account.isNullOrEmpty() || password.isNullOrEmpty()){
            Toast.makeText(this@LoginActivity, "账号或者密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if(account.length < 6){
            Toast.makeText(this@LoginActivity, "账号格式不正确，请输入正确的" + getString(R.string.username_phone_or_mail), Toast.LENGTH_SHORT).show()
            return
        }
        if(password.length < 6){
            Toast.makeText(this@LoginActivity, "密码格式不正确，请输入正确的" + getString(R.string.password), Toast.LENGTH_SHORT).show()
            return
        }

        val login1 = findViewById<MaterialButton>(R.id.login_1)
        login1.isEnabled = false
        login1.text = "登录中..."

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.LoginByPass(account, password)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        if(responseBody.result && responseBody.token != null){
                            Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                            saveTokenAndGetUserInform(responseBody.token)
                        }

                        login1.text = getString(R.string.login)
                        login1.isEnabled = true
                        if(responseBody.message != null){
                            Toast.makeText(this@LoginActivity, responseBody.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@LoginActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loginWithVerifyCode(){
        val targetInput = findViewById<TextInputEditText>(R.id.target)
        val target = targetInput.text?.toString()?.trim()
        if(target.isNullOrEmpty()){
            Toast.makeText(this@LoginActivity, "请输入手机号或者邮箱", Toast.LENGTH_SHORT).show()
            return
        }

        val phonePattern = Regex("""^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$""")
        val mailPattern = Regex("""^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$""")

        if(!target.matches(phonePattern) && !target.matches(mailPattern)){
            Toast.makeText(this@LoginActivity, "请输入正确的11位手机号或者电子邮箱", Toast.LENGTH_SHORT).show()
            return
        }

        if(verifyId == null){
            Toast.makeText(this@LoginActivity, "请先发送验证码", Toast.LENGTH_SHORT).show()
            return
        }

        val codeInput = findViewById<TextInputEditText>(R.id.code)
        val codeText = codeInput.text?.toString()?.trim()
        if(codeText.isNullOrEmpty()){
            Toast.makeText(this@LoginActivity, "请输入验证码", Toast.LENGTH_SHORT).show()
            return
        }
        if(codeText.length != 6){
            Toast.makeText(this@LoginActivity, "请输入正确的验证码", Toast.LENGTH_SHORT).show()
            return
        }


        val code = codeText.toIntOrNull()

        val login2 = findViewById<MaterialButton>(R.id.login_1)
        login2.isEnabled = false
        login2.text = "登录中..."

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(SERVE_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val ApiService = retrofit.create(ApiService::class.java)
        val call = ApiService.LoginByCode(target, verifyId, code)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        if(responseBody.result && responseBody.token != null){
                            Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                            saveTokenAndGetUserInform(responseBody.token)
                        }

                        login2.text = getString(R.string.login)
                        login2.isEnabled = true
                        if(responseBody.message != null){
                            Toast.makeText(this@LoginActivity, responseBody.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(this@LoginActivity, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun saveTokenAndGetUserInform(access_token: String){
        val sharedPref = getSharedPreferences("user", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("access_token", access_token)
        editor.apply()
        verifyTokenAndGetUserInform(access_token, this)
        onBackPressed()
    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun setStatusBarTextColor(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            if (isDark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }
}