package com.fixeam.icoserkt

import android.content.Context
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

const val SERVE_HOST = "https://api.fixeam.com/api/"
val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

val retrofit = Retrofit.Builder()
    .client(client)
    .baseUrl(SERVE_HOST)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val ApiNetService: ApiService = retrofit.create(ApiService::class.java)

interface ApiService {
    @GET("carousel")
    fun GetCarousel(): Call<ResponseBody>
    @GET("album")
    fun GetAlbum(@Query("condition") condition: String = "", @Query("access_token") access_token: String = "", @Query("start") start: Int = 0, @Query("number") number: Int = 20): Call<ResponseBody>
    @GET("album?random=true")
    fun GetRecAlbum(@Query("number") number: Int?, @Query("access_token") access_token: String = ""): Call<ResponseBody>
    @GET("album/hot")
    fun GetHot(): Call<ResponseBody>
    @GET("model?withalbum=true")
    fun GetRecModel(): Call<ResponseBody>
    @GET("model")
    fun GetModel(@Query("condition") condition: String?, @Query("access_token") access_token: String = ""): Call<ModelsResponse>
    @GET("login/send-code")
    fun SendVerifyCode(@Query("target") target: String?): Call<SendVerifyCodeResponse>
    @GET("login/bypass")
    fun LoginByPass(@Query("username") username: String?, @Query("password") password: String?): Call<LoginResponse>
    @GET("login/bycode")
    fun LoginByCode(@Query("target") target: String?, @Query("verify-id") verifyId: String?, @Query("code") code: Int?): Call<LoginResponse>
    @GET("login/user/inform")
    fun GetUserInform(@Query("access_token") access_token: String?): Call<UserInformResponse>
    @GET("collection/set")
    fun SetCollectionItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String, @Query("fold") fold: String = ""): Call<ActionResponse>
    @GET("collection/set?isNuCollect=true")
    fun RemoveCollectionItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @GET("forbidden/set")
    fun SetForbiddenItem(@Query("access_token") access_token: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @Headers("Content-Type: application/json")
    @POST("file/infomation")
    fun PostFileAndInformation(@Body requestBody: UrlRequestBody): Call<List<FileInfo>>
    @GET
    fun GetFileInfoByUrl(@Url url: String): Call<FileMeta>
    @GET("file/meta/update")
    fun UpdateFileMeta(@Query("url") url: String, @Query("meta") meta: String): Call<ActionResponse>
    @GET("collection/fold/get")
    fun GetUserCollectionFold(@Query("access_token") access_token: String)
    @GET("collection/get")
    fun GetUserCollection(@Query("access_token") access_token: String)
    @GET("media")
    fun GetMedia(@Query("access_token") access_token: String = "", @Query("number") number: Int = 20, @Query("model-id") model_id: String = "", @Query("album-id") album_id: String = "", @Query("id") id: String = ""): Call<MediaResponse>

    @GET("album/search")
    fun SearchAlbum(@Query("access_token") access_token: String = "", @Query("keyword") keyword: String = ""): Call<SearchAlbumResponse>

    @GET("model/search")
    fun SearchModel(@Query("access_token") access_token: String = "", @Query("keywords") keywords: String = ""): Call<SearchModelResponse>

    @GET("access/log")
    fun AccessLog(@Query("type") type: String, @Query("content") content: String, @Query("device") device: String, @Query("application") application: String = "Android Kotlin", @Query("access_token") access_token: String = ""): Call<AccessLogResponse>

    @GET("access/log?updateStay=true")
    fun UpdateAccessLog(@Query("id") id: Int): Call<UpdateAccessLog>

    @GET("access/log?updateStay=true")
    fun UpdateAccessLogWithStay(@Query("id") id: Int, @Query("stay") stay: Int): Call<UpdateAccessLog>

    @GET("follow")
    fun GetFollow(@Query("access_token") access_token: String, @Query("start") start: Int = 0, @Query("number") number: Int = 20): Call<AlbumsResponse>
}


// 上传和更新访问记录
fun accessLog(context: Context, content: String, type: String, callback: (id: Int) -> Unit){
    var call = ApiNetService.AccessLog(type, content, getSystemInfo(context), "Android Kotlin")
    if(userToken != null){
        call = ApiNetService.AccessLog(type, content, getSystemInfo(context), "Android Kotlin", userToken!!)
    }

    call.enqueue(object : Callback<AccessLogResponse> {
        override fun onResponse(call: Call<AccessLogResponse>, response: Response<AccessLogResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if(responseBody != null && responseBody.result){
                    callback(responseBody.history.id)
                }
            }
        }

        override fun onFailure(call: Call<AccessLogResponse>, t: Throwable) { }
    })
}

fun updateAccessLog(id: Int, stay: Int = -1){
    if(id == 0){
        return
    }
    var call = ApiNetService.UpdateAccessLog(id)
    if(stay > 0){
        call = ApiNetService.UpdateAccessLogWithStay(id, stay)
    }

    call.enqueue(object : Callback<UpdateAccessLog> {
        override fun onResponse(call: Call<UpdateAccessLog>, response: Response<UpdateAccessLog>) { }
        override fun onFailure(call: Call<UpdateAccessLog>, t: Throwable) { }
    })
}