package com.fixeam.icoser.network

import android.content.Context
import android.widget.Toast
import com.fixeam.icoser.model.getSystemInfo
import com.fixeam.icoser.model.newsData
import com.google.gson.Gson
import okhttp3.MultipartBody
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
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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
    @GET("forbidden/set?isNuForbidden=true")
    fun RemoveForbiddenItem(@Query("access_token") access_token: String, @Query("id") id: Int): Call<ActionResponse>
    @GET("forbidden/get")
    fun GetUserForbidden(@Query("access_token") access_token: String): Call<ForbiddenResponse>
    @Headers("Content-Type: application/json")
    @POST("file/infomation")
    fun PostFileAndInformation(@Body requestBody: UrlRequestBody): Call<List<FileInfo>>
    @GET
    fun GetFileInfoByUrl(@Url url: String): Call<FileMeta>
    @GET("file/meta/update")
    fun UpdateFileMeta(@Query("url") url: String, @Query("meta") meta: String): Call<ActionResponse>
    @GET("collection/fold/get")
    fun GetUserCollectionFold(@Query("access_token") access_token: String): Call<CollectionFoldResponse>
    @GET("collection/fold/set")
    fun SetUserCollectionFold(@Query("access_token") access_token: String, @Query("name") name: String): Call<ActionResponse>
    @GET("collection/fold/set?isDelete=true")
    fun RemoveCollectionFold(@Query("access_token") access_token: String, @Query("id") id: Int): Call<ActionResponse>
    @GET("collection/get")
    fun GetUserCollection(@Query("access_token") access_token: String): Call<CollectionResponse>
    @GET("collection/get?type=model")
    fun GetUserFollow(@Query("access_token") access_token: String): Call<FollowResponse>
    @GET("access/history")
    fun GetUserHistory(@Query("access_token") access_token: String, @Query("start") start: Int = 0, @Query("number") number: Int = 50): Call<HistoryResponse>
    @GET("access/history/clear")
    fun ClearUserHistory(@Query("access_token") access_token: String): Call<ActionResponse>
    @GET("access/history/clear")
    fun ClearUserHistoryById(@Query("access_token") access_token: String, @Query("id") start: Int): Call<ActionResponse>
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
    @GET("app/version/latest?platform=android")
    fun GetLatestVersion(@Query("version_type") type: String): Call<PackageInfo>
    @Multipart
    @POST("file/upload")
    fun uploadFile(
        @Query("access_token") accessToken: String,
        @Part file: MultipartBody.Part
    ): Call<FileUploadResponse>
    @GET("login/user/change/inform")
    fun setUserInform(@Query("access_token") accessToken: String, @Query("inform") inform: String): Call<ActionResponse>
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
                    getUserHistory(context){}
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

// 获取最新的写真集
fun requestNewData(context: Context, callback: () -> Unit) {
    val call = ApiNetService.GetAlbum(number = 30)
    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val albumsResponse = Gson().fromJson(responseBody, AlbumsResponse::class.java)
                if (albumsResponse.result) {
                    newsData = albumsResponse.data.take(30)
                    callback()
                }
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}