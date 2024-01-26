package com.fixeam.icoserkt

import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

const val SERVE_HOST = "https://api.fixeam.com/api/"
interface ApiService {
    @GET("carousel")
    fun GetCarousel(): Call<ResponseBody>
    @GET("album/hot")
    fun GetHot(): Call<ResponseBody>
    @GET("model")
    fun GetHot(@Query("withalbum") withalbum: Boolean): Call<ResponseBody>
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
    var other_model: MutableList<String>,
    var tags: MutableList<String>,
    var images: MutableList<String>,
    var create_time: String
)
data class ModelsResponse(
    val result: Boolean,
    val data: List<Models>
)
data class Models(
    var id: Int,
    var name: String,
    var other_name: String,
    var avatar_image: String,
    var background_image: String,
    var model_weight: String,
    var tags: JSONArray,
    var birthday: String,
    var social: JSONArray,
    var total: String,
    var album: MutableList<Albums>
)
