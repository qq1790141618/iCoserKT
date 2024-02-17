package com.fixeam.icoserkt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

var homeFragment: HomeFragment? = null
var smartVideoFragment: SmartVideoFragment? = null
var searchFragment: SearchFragment? = null
var userFragment:   UserFragment? = null
var showFragment: Fragment? = null
var imagePreview: ConstraintLayout? = null
var overCard: ConstraintLayout? = null

const val SERVE_HOST = "https://api.fixeam.com/api/"
val client: OkHttpClient = OkHttpClient.Builder()
.connectTimeout(10, TimeUnit.SECONDS)
.readTimeout(10, TimeUnit.SECONDS)
.writeTimeout(10, TimeUnit.SECONDS)
.addInterceptor { chain ->
    val originalRequest = chain.request()
    val authenticatedRequest = originalRequest.newBuilder()
        .header("Authorization", "iCoser_Android_Application_By_Kotlin")
        .build()
    chain.proceed(authenticatedRequest)
}
.build()
interface ApiService {
    @GET("carousel")
    fun GetCarousel(): Call<ResponseBody>
    @GET("album")
    fun GetAlbum(@Query("condition") condition: String?): Call<ResponseBody>
    @GET("album?random=true")
    fun GetRecAlbum(@Query("number") number: Int?, @Query("access_token") access_token: String = ""): Call<ResponseBody>
    @GET("album/hot")
    fun GetHot(): Call<ResponseBody>
    @GET("model?withalbum=true")
    fun GetRecModel(): Call<ResponseBody>
    @GET("login/send-code")
    fun SendVerifyCode(@Query("target") target: String?): Call<SendVerifyCodeResponse>
    @GET("login/bypass")
    fun LoginByPass(@Query("username") username: String?, @Query("password") password: String?): Call<LoginResponse>
    @GET("login/bycode")
    fun LoginByCode(@Query("target") target: String?, @Query("verify-id") verifyId: String?, @Query("code") code: Int?): Call<LoginResponse>
    @GET("login/user/inform")
    fun GetUserInform(@Query("access_token") access_token: String?): Call<UserInformResponse>
    @GET("collection/set")
    fun SetCollectionItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String, @Query("fold") fold: String): Call<ActionResponse>
    @GET("collection/set?isNuCollect=true")
    fun RemoveCollectionItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @GET("forbidden/set")
    fun SetForbiddenItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @Headers("Content-Type: application/json")
    @POST("file/infomation")
    fun PostFileAndInformation(@Body requestBody: UrlRequestBody): Call<List<FileInfo>>
    @GET
    fun GetFileInfoByUrl(@Url url: String): Call<FileMeta>
}
data class ActionResponse(
    val result: Boolean
)
data class LoginResponse(
    val result: Boolean,
    val token: String?,
    val message: String?
)
data class UserInformResponse(
    val result: Boolean,
    val message: String?,
    val inform: UserInform?
)
data class UserInform(
    val id: Int,
    val identity: String,
    val identityLevel: Int?,
    val username: String,
    val phone: String,
    val mail: String,
    val header: String,
    val nickname: String,
    val register: String,
    val comment_disabled: Int,
    val location: String,
    val birthday: String
)
data class SendVerifyCodeResponse(
    val result: Boolean,
    val verify_id: String?,
    val effectiveTime: Int?,
    val message: String?
)
data class AlbumsResponse(
    val result: Boolean,
    val data: List<Albums>
)
data class Albums(
    var id: Int,
    var album_id: Int,
    var name: String,
    var poster: String,
    var model: String,
    var model_id: Int,
    var model_name: String,
    var other_model: MutableList<String>?,
    var tags: MutableList<String>?,
    var images: Any,
    var download: AlbumDownload?,
    var create_time: String,
    var media: MutableList<Media>,
    var is_collection: String?
)
data class AlbumDownload(
    var url: String,
    var password: String,
    var encryption: Boolean
)
data class FileInfo(
    var id: Int,
    var name: String,
    var size: Long,
    var url: String,
    var mime: String,
    var meta: String?,
    var suffix: String,
    var time: String,
    var userid: String,
    var violation: String?
)
data class FileMeta(
    var width: String,
    var height: String,
    var format: String?,
    var size: String?,
    var md5: String?,
    var frame_count: String?,
    var bit_depth: String?,
    var horizontal_dpi: String?,
    var vertical_dpi: String?
)
data class UrlRequestBody(
    val url: String
)
data class ModelsResponse(
    val result: Boolean,
    val data: List<Models>
)
data class Models(
    var id: Int,
    var name: String,
    var other_name: String?,
    var avatar_image: String,
    var background_image: String?,
    var tags: String,
    var birthday: String?,
    var social: String,
    var total: String,
    var latest_create_time: String,
    var status: String,
    var album: MutableList<Albums>
)
data class Media(
    var id: Int,
    var name: String,
    var description: String?,
    var size: Int,
    var duration: Double,
    var mime: String?,
    var suffix: String,
    var resource: String,
    var resource_files: Any?,
    var cover: String,
    var width: Int,
    var height: Int,
    var format: MutableList<MediaFormatItem>,
    var bind_album_id: Int,
    var bind_model_id: Int,
    var create_time: String,
    var create_user: String,
    var status: String,
)
data class MediaFormatItem(
    var url: String,
    var part: MutableList<String>?,
    var resolution_ratio: String
)

var userToken: String? = null
var userInform: UserInform? = null
var userInformFailTime = 0
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
fun removeSharedPreferencesKey(key: String, context: Context){
    val sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(key)
    editor.apply()
}

fun closeOverCard(){
    val card = overCard!!.findViewById<CardView>(R.id.overlay_card)

    val slideAnimation = ObjectAnimator.ofFloat(card, "translationY", 0f, overCard!!.height.toFloat())
    slideAnimation.duration = 500
    slideAnimation.start()

    val startColor = Color.parseColor("#AE000000")
    val endColor = Color.parseColor("#00000000")

    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
    colorAnimation.duration = 500
    colorAnimation.addUpdateListener { animator ->
        val color = animator.animatedValue as Int
        overCard?.setBackgroundColor(color)
    }

    colorAnimation.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            overCard?.visibility = View.GONE
        }
    })

    colorAnimation.start()
}

fun openOverCard(){
    val card = overCard!!.findViewById<CardView>(R.id.overlay_card)
    val slideAnimation = ObjectAnimator.ofFloat(card, "translationY", overCard!!.height.toFloat(), 0f)
    slideAnimation.duration = 500
    slideAnimation.start()

    val startColor = Color.parseColor("#AE000000")
    val endColor = Color.parseColor("#00000000")

    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), endColor, startColor)
    colorAnimation.duration = 500
    colorAnimation.addUpdateListener { animator ->
        val color = animator.animatedValue as Int
        overCard?.setBackgroundColor(color)
    }

    colorAnimation.start()
}

fun shareTextContent(text: String, title: String = "来自iCoser的分享", context: Context) {
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(shareIntent, title))
}

fun getScreenWidth(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}