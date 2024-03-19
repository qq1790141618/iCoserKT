package com.fixeam.icoser.network

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.fixeam.icoser.R
import com.fixeam.icoser.model.getSystemInfo
import com.fixeam.icoser.model.hotData
import com.fixeam.icoser.model.newsData
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
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

val retrofit: Retrofit = Retrofit.Builder()
    .client(client)
    .baseUrl(SERVE_HOST)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val ApiNetService: ApiService = retrofit.create(ApiService::class.java)

interface ApiService {
    @GET("carousel")
    fun getCarousel(): Call<CarouselResponse>
    @GET("album")
    fun getAlbum(@Query("condition") condition: String = "", @Query("access_token") accessToken: String = "", @Query("start") start: Int = 0, @Query("number") number: Int = 20): Call<AlbumsResponse>
    @GET("album/hot")
    fun getHot(): Call<AlbumsResponse>
    @GET("model?withalbum=true")
    fun getRecModel(): Call<ModelsResponse>
    @GET("album?random=true")
    fun getRecAlbum(@Query("number") number: Int?, @Query("access_token") accessToken: String = ""): Call<AlbumsResponse>
    @GET("search/hot")
    fun getHotSearch(): Call<HotSearchKeywordResponse>
    @GET("model")
    fun getModel(@Query("condition") condition: String?, @Query("access_token") accessToken: String = ""): Call<ModelsResponse>
    @GET("login/send-code")
    fun sendVerifyCode(@Query("target") target: String?): Call<SendVerifyCodeResponse>
    @GET("login/bypass")
    fun loginByPass(@Query("username") username: String?, @Query("password") password: String?): Call<LoginResponse>
    @GET("login/bycode")
    fun loginByCode(@Query("target") target: String?, @Query("verify-id") verifyId: String?, @Query("code") code: Int?): Call<LoginResponse>
    @GET("login/user/inform")
    fun getUserInform(@Query("access_token") accessToken: String?): Call<UserInformResponse>
    @GET("collection/set")
    fun setCollectionItem(@Query("access_token") accessToken: String, @Query("id") id: Int, @Query("type") type: String, @Query("fold") fold: String = ""): Call<ActionResponse>
    @GET("collection/set?isNuCollect=true")
    fun removeCollectionItem(@Query("access_token") accessToken: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @GET("forbidden/set")
    fun setForbiddenItem(@Query("access_token") accessToken: String, @Query("id") id: Int, @Query("type") type: String): Call<ActionResponse>
    @GET("forbidden/set?isNuForbidden=true")
    fun removeForbiddenItem(@Query("access_token") accessToken: String, @Query("id") id: Int): Call<ActionResponse>
    @GET("forbidden/get")
    fun getUserForbidden(@Query("access_token") accessToken: String): Call<ForbiddenResponse>
    @Headers("Content-Type: application/json")
    @POST("file/infomation")
    fun postFileAndInformation(@Body requestBody: UrlRequestBody): Call<List<FileInfo>>
    @GET
    fun getFileInfoByUrl(@Url url: String): Call<FileMeta>
    @GET("file/meta/update")
    fun updateFileMeta(@Query("url") url: String, @Query("meta") meta: String): Call<ActionResponse>
    @GET("collection/fold/get")
    fun getUserCollectionFold(@Query("access_token") accessToken: String): Call<CollectionFoldResponse>
    @GET("collection/fold/set")
    fun setUserCollectionFold(@Query("access_token") accessToken: String, @Query("name") name: String): Call<ActionResponse>
    @GET("collection/fold/set?isDelete=true")
    fun removeCollectionFold(@Query("access_token") accessToken: String, @Query("id") id: Int): Call<ActionResponse>
    @GET("collection/get")
    fun getUserCollection(@Query("access_token") accessToken: String): Call<CollectionResponse>
    @GET("collection/get?type=model")
    fun getUserFollow(@Query("access_token") accessToken: String): Call<FollowResponse>
    @GET("collection/get?type=media")
    fun getUserMediaLike(@Query("access_token") accessToken: String): Call<MediaLikeResponse>
    @GET("access/history")
    fun getUserHistory(@Query("access_token") accessToken: String, @Query("start") start: Int = 0, @Query("number") number: Int = 50): Call<HistoryResponse>
    @GET("access/history/clear")
    fun clearUserHistory(@Query("access_token") accessToken: String): Call<ActionResponse>
    @GET("access/history/clear")
    fun clearUserHistoryById(@Query("access_token") accessToken: String, @Query("id") start: Int): Call<ActionResponse>
    @GET("media")
    fun getMedia(@Query("access_token") accessToken: String = "", @Query("number") number: Int = 20, @Query("model-id") modelId: String = "", @Query("album-id") albumId: String = "", @Query("id") id: String = ""): Call<MediaResponse>
    @GET("album/search")
    fun searchAlbum(@Query("access_token") accessToken: String = "", @Query("keyword") keyword: String = ""): Call<SearchAlbumResponse>
    @GET("model/search")
    fun searchModel(@Query("access_token") accessToken: String = "", @Query("keywords") keywords: String = ""): Call<SearchModelResponse>
    @GET("access/log")
    fun accessLog(@Query("type") type: String, @Query("content") content: String, @Query("device") device: String, @Query("application") application: String = "Android Kotlin", @Query("access_token") accessToken: String = ""): Call<AccessLogResponse>
    @GET("access/log?updateStay=true")
    fun updateAccessLog(@Query("id") id: Int): Call<UpdateAccessLog>
    @GET("access/log?updateStay=true")
    fun updateAccessLogWithStay(@Query("id") id: Int, @Query("stay") stay: Int): Call<UpdateAccessLog>
    @GET("follow")
    fun getFollow(@Query("access_token") accessToken: String, @Query("start") start: Int = 0, @Query("number") number: Int = 20): Call<AlbumsResponse>
    @GET("app/version/latest?platform=android")
    fun getLatestVersion(@Query("version_type") type: String): Call<PackageInfo>
    @Multipart
    @POST("file/upload")
    fun uploadFile(
        @Query("access_token") accessToken: String,
        @Part file: MultipartBody.Part
    ): Call<FileUploadResponse>
    @GET("login/user/change/inform")
    fun setUserInform(@Query("access_token") accessToken: String, @Query("inform") inform: String): Call<ActionResponse>
    @GET("comment?type=Like&content=1&res_type=Media")
    fun appreciate(@Query("res_id") resId: Int, @Query("access_token") accessToken: String = ""): Call<AppreciateResponse>
    @GET("comment?cancel=cancel")
    fun appreciateCancel(@Query("id") id: Int): Call<AppreciateResponse>
}

// 上传和更新访问记录
fun accessLog(context: Context, content: String, type: String, callback: (id: Int) -> Unit){
    if(context.getString(R.string.app_type) != "release"){
        return
    }
    var call = ApiNetService.accessLog(type, content, getSystemInfo(context), "Android Kotlin")
    if(userToken != null){
        call = ApiNetService.accessLog(type, content, getSystemInfo(context), "Android Kotlin", userToken!!)
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
    var call = ApiNetService.updateAccessLog(id)
    if(stay > 0){
        call = ApiNetService.updateAccessLogWithStay(id, stay)
    }

    call.enqueue(object : Callback<UpdateAccessLog> {
        override fun onResponse(call: Call<UpdateAccessLog>, response: Response<UpdateAccessLog>) { }
        override fun onFailure(call: Call<UpdateAccessLog>, t: Throwable) { }
    })
}
// 获取最新的写真集
fun requestNewData(context: Context, callback: (List<Albums>) -> Unit) {
    val call = ApiNetService.getAlbum(number = 30)
    call.enqueue(object : Callback<AlbumsResponse> {
        override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    newsData = responseBody.data.take(30)
                    callback(newsData)
                }
            }
        }

        override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {}
    })
}
// 获取热门的写真集
fun requestHotData(context: Context, callback: (List<Albums>) -> Unit) {
    val call = ApiNetService.getHot()
    call.enqueue(object : Callback<AlbumsResponse> {
        override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    hotData = responseBody.data.take(30)
                    callback(hotData)
                }
            }
        }

        override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取轮播图数据
fun requestCarouselData(context: Context, callback: (List<Carousel>) -> Unit) {
    val call = ApiNetService.getCarousel()
    call.enqueue(object : Callback<CarouselResponse> {
        override fun onResponse(call: Call<CarouselResponse>, response: Response<CarouselResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    val carouselData = responseBody.data
                    callback(carouselData)
                }
            } else {
                Toast.makeText(context, "轮播图数据加载失败", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<CarouselResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取推荐模特信息
fun requestRecommendModelData(context: Context, callback: (List<Models>) -> Unit) {
    val call = ApiNetService.getRecModel()

    call.enqueue(object : Callback<ModelsResponse> {
        override fun onResponse(call: Call<ModelsResponse>, response: Response<ModelsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(responseBody.data)
                }
            }
        }

        override fun onFailure(call: Call<ModelsResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取流推荐写真集信息
fun requestLikesData(context: Context, number: Int, callback: (List<Albums>) -> Unit){
    var call = ApiNetService.getRecAlbum(number)
    if(userToken != null){
        call = ApiNetService.getRecAlbum(number, userToken!!)
    }

    call.enqueue(object : Callback<AlbumsResponse> {
        override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if(responseBody == null || !responseBody.result){
                    return
                }
                callback(responseBody.data)
            }
        }

        override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取写真集信息
fun requestAlbumData(context: Context, condition: String = "", doNotSetToken: Boolean = false, start: Int = 0, number: Int = 20, callback: (List<Albums>) -> Unit) {
    var call = ApiNetService.getAlbum(condition = condition, start = start, number = number)
    if(userToken != null && !doNotSetToken){
        call = ApiNetService.getAlbum(condition = condition, accessToken = userToken!!, start = start, number = number)
    }
    call.enqueue(object : Callback<AlbumsResponse> {
        override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result && responseBody.data.isNotEmpty()) {
                    callback(responseBody.data)
                }
            }
        }

        override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取写真集图片文件信息
fun requestAlbumImageInfo(context: Context, albumImages: List<String>, callback: (List<FileInfo>) -> Unit) {
    val urls = Gson().toJson(albumImages)
    val urlRequestBody = UrlRequestBody(urls)
    val call = ApiNetService.postFileAndInformation(urlRequestBody)

    call.enqueue(object : Callback<List<FileInfo>> {
        override fun onResponse(call: Call<List<FileInfo>>, response: Response<List<FileInfo>>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    callback(responseBody)
                }
            }
        }

        override fun onFailure(call: Call<List<FileInfo>>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取COS图片信息
fun requestImageInfo(context: Context, image: String, callback: (FileMeta) -> Unit) {
    val call = ApiNetService.getFileInfoByUrl("$image?imageInfo")

    call.enqueue(object : Callback<FileMeta> {
        override fun onResponse(call: Call<FileMeta>, response: Response<FileMeta>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    callback(responseBody)
                }
            }
        }

        override fun onFailure(call: Call<FileMeta>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 上传图片文件信息
fun uploadImageInfo(image: String, json: String) {
    val call = ApiNetService.updateFileMeta(image, json)
    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) { }
        override fun onFailure(call: Call<ActionResponse>, t: Throwable) { }
    })
}
// 获取热搜词
fun requestHotSearchKeyword(context: Context, callback: (List<String>) -> Unit) {
    val call = ApiNetService.getHotSearch()

    call.enqueue(object : Callback<HotSearchKeywordResponse> {
        override fun onResponse(call: Call<HotSearchKeywordResponse>, response: Response<HotSearchKeywordResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    callback(responseBody.keyword)
                }
            }
        }

        override fun onFailure(call: Call<HotSearchKeywordResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取写真集搜索结果
fun requestAlbumSearch(context: Context, keyword: String, callback: (List<Albums>, List<String>?) -> Unit) {
    var call = ApiNetService.searchAlbum(
        keyword = keyword
    )
    if(userToken != null){
        call = ApiNetService.searchAlbum(
            accessToken = userToken!!,
            keyword = keyword
        )
    }
    call.enqueue(object : Callback<SearchAlbumResponse> {
        override fun onResponse(call: Call<SearchAlbumResponse>, response: Response<SearchAlbumResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(responseBody.data, responseBody.keywords)
                }
            }
        }

        override fun onFailure(call: Call<SearchAlbumResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取模特搜索结果
fun requestModelSearch(context: Context, keywords: List<String>?, callback: (List<Models>) -> Unit) {
    val keywordsString = Gson().toJson(keywords)
    var call = ApiNetService.searchModel(
        keywords = keywordsString
    )
    if(userToken != null){
        call = ApiNetService.searchModel(
            accessToken = userToken!!,
            keywords = keywordsString
        )
    }
    call.enqueue(object : Callback<SearchModelResponse> {
        override fun onResponse(call: Call<SearchModelResponse>, response: Response<SearchModelResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(responseBody.data)
                }
            }
        }

        override fun onFailure(call: Call<SearchModelResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取视频
fun requestMediaData(context: Context, modelId: Int = -1, albumId: Int = -1, id: Int = -1, number: Int = 9999, callback: (List<Media>) -> Unit) {
    var call = ApiNetService.getMedia(number = number)
    if(id > 0){
        call = ApiNetService.getMedia(id = id.toString(), number = number)
    }
    if(albumId > 0){
        call = ApiNetService.getMedia(albumId = albumId.toString(), number = number)
    }
    if(modelId > 0){
        call = ApiNetService.getMedia(modelId = modelId.toString(), number = number)
    }

    call.enqueue(object : Callback<MediaResponse> {
        @SuppressLint("SetTextI18n")
        override fun onResponse(call: Call<MediaResponse>, response: Response<MediaResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(responseBody.data)
                } else {
                    callback(listOf())
                }
            } else {
                Toast.makeText(context, "请求失败", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<MediaResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 获取模特信息
fun requestModelData(context: Context, condition: String = "", doNotSetToken: Boolean = false, callback: (List<Models>) -> Unit) {
    var call = ApiNetService.getModel(condition = condition)
    if(userToken != null && !doNotSetToken){
        call = ApiNetService.getModel(condition = condition, accessToken = userToken!!)
    }
    call.enqueue(object : Callback<ModelsResponse> {
        override fun onResponse(call: Call<ModelsResponse>, response: Response<ModelsResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result && responseBody.data.isNotEmpty()) {
                    callback(responseBody.data)
                }
            }
        }

        override fun onFailure(call: Call<ModelsResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

// 登录相关正则表达式
val phonePattern = Regex("""^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$""")
val mailPattern = Regex("""^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$""")
// 发送验证码函数
fun sendCode(context: Context, target: String?, callback: (String?) -> Unit) {
    if(target.isNullOrEmpty()){
        Toast.makeText(context, "请输入手机号或者邮箱", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }

    if(!target.matches(phonePattern) && !target.matches(mailPattern)){
        Toast.makeText(context, "请输入正确的11位手机号或者电子邮箱", Toast.LENGTH_SHORT).show()
        return
    }

    val call = ApiNetService.sendVerifyCode(target)
    call.enqueue(object : Callback<SendVerifyCodeResponse> {
        override fun onResponse(call: Call<SendVerifyCodeResponse>, response: Response<SendVerifyCodeResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if(responseBody.result){
                        callback(responseBody.verify_id)
                    } else {
                        callback(null)
                    }
                    if(responseBody.message != null){
                        Toast.makeText(context, responseBody.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        override fun onFailure(call: Call<SendVerifyCodeResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 账号密码登录函数
fun requestPasswordLogin(context: Context, account: String?, password: String?, callback: (String?) -> Unit) {
    if(account.isNullOrEmpty() || password.isNullOrEmpty()){
        Toast.makeText(context, "账号或者密码不能为空", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(account.length < 6){
        Toast.makeText(context, "账号格式不正确，请输入正确的" + context.getString(R.string.username_phone_or_mail), Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(password.length < 6){
        Toast.makeText(context, "密码格式不正确，请输入正确的" + context.getString(R.string.password), Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    val call = ApiNetService.loginByPass(account, password)
    call.enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if(responseBody.result && responseBody.token != null){
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        callback(responseBody.token)
                    } else {
                        callback(null)
                    }

                    if(responseBody.message != null){
                        Toast.makeText(context, responseBody.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 验证码登录函数
fun requestVerifyCodeLogin(context: Context, target: String?, verifyId: String?, codeText: String?, callback: (String?) -> Unit) {
    if(target.isNullOrEmpty()){
        Toast.makeText(context, "请输入手机号或者邮箱", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(!target.matches(phonePattern) && !target.matches(mailPattern)){
        Toast.makeText(context, "请输入正确的11位手机号或者电子邮箱", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(verifyId == null){
        Toast.makeText(context, "请先发送验证码", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(codeText.isNullOrEmpty()){
        Toast.makeText(context, "请输入验证码", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    if(codeText.length != 6){
        Toast.makeText(context, "请输入正确的验证码", Toast.LENGTH_SHORT).show()
        callback(null)
        return
    }
    val code = codeText.toIntOrNull()
    val call = ApiNetService.loginByCode(target, verifyId, code)

    call.enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if(responseBody.result && responseBody.token != null){
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        callback(responseBody.token)
                    } else {
                        callback(null)
                    }

                    if(responseBody.message != null){
                        Toast.makeText(context, responseBody.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}
// 点赞
fun appreciate(resId: Int, callback: (Boolean, Int?) -> Unit){
    var call = ApiNetService.appreciate(resId)
    if(userToken != null){
        call = ApiNetService.appreciate(resId, userToken!!)
    }

    call.enqueue(object : Callback<AppreciateResponse> {
        override fun onResponse(call: Call<AppreciateResponse>, response: Response<AppreciateResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(true, responseBody.id)
                } else {
                    callback(false, 0)
                }
            }
        }

        override fun onFailure(call: Call<AppreciateResponse>, t: Throwable) {
            callback(false, 0)
        }
    })
}
fun appreciateCancel(id: Int, callback: (Boolean) -> Unit){
    val call = ApiNetService.appreciateCancel(id)

    call.enqueue(object : Callback<AppreciateResponse> {
        override fun onResponse(call: Call<AppreciateResponse>, response: Response<AppreciateResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        }

        override fun onFailure(call: Call<AppreciateResponse>, t: Throwable) {
            callback(false)
        }
    })
}