package com.fixeam.icoserkt

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 用户变量
var userToken: String? = null
var userInform: UserInform? = null
var userInformFailTime = 0

// 获取用户信息
fun verifyTokenAndGetUserInform(access_token: String, context: Context){
    if(userInformFailTime >= 3){
        return
    }

    val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(SERVE_HOST)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val ApiService = retrofit.create(ApiService::class.java)
    val call = ApiService.GetUserInform(access_token)

    call.enqueue(object : Callback<UserInformResponse> {
        override fun onResponse(call: Call<UserInformResponse>, response: Response<UserInformResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if(responseBody.result && responseBody.inform != null){
                        userInform = responseBody.inform
                        userToken = access_token
                        userFragment?.initUserCard(responseBody.inform)
                    } else {
                        removeSharedPreferencesKey("access_token", context)
                    }
                }
            }
        }

        override fun onFailure(call: Call<UserInformResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            userInformFailTime++
            verifyTokenAndGetUserInform(access_token, context)
        }
    })
}