package com.fixeam.icoserkt

import android.media.MediaFormat
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
    @GET("album?random=true&number=4")
    fun GetRecAlbum(): Call<ResponseBody>
    @GET("album/hot")
    fun GetHot(): Call<ResponseBody>
    @GET("model?withalbum=true")
    fun GetRecModel(): Call<ResponseBody>
}
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
)
data class AlbumDownload(
    var url: String,
    var password: String,
    var encryption: Boolean
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
    var format: MutableList<MediaFormat>,
    var bind_album_id: Int,
    var bind_model_id: Int,
    var create_time: String,
    var create_user: String,
    var status: String,
)

data class MediaFormat(
    var url: String,
    var part: MutableList<String>?,
    var resolution_ratio: String
)
