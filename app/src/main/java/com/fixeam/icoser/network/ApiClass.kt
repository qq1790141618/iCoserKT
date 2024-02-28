package com.fixeam.icoser.network

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
    var media: MutableList<Media>?,
    var is_collection: String?,
    var count: Int?,
    var type: String?,
    var model_avatar_image: String?
)
data class AlbumDownload(
    var url: String,
    var password: String,
    var encryption: Boolean
)
data class FileInfo(
    var id: Int,
    var name: String,
    var size: Int,
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
    var count: Int,
    var latest_create_time: String,
    var status: String,
    var album: MutableList<Albums>,
    var is_collection: Boolean?
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
    var model_avatar_image: String,
    var album_name: String,
    var model_name: String
)
data class MediaResponse(
    val result: Boolean,
    val data: List<Media>
)
data class MediaFormatItem(
    var url: String,
    var part: MutableList<String>?,
    var resolution_ratio: String
)
data class CarouselResponse(
    val result: Boolean,
    val data: List<Carousel>
)
data class Carousel(
    val id: Int,
    val serial: Int,
    val url: String,
    val link: Link
)
data class Link(
    val type: String,
    val content: Content
)
data class Content(
    val id: Int,
    val name: String,
    val model_id: Int,
    val model: String
)

data class SearchAlbumResponse(
    val result: Boolean,
    val data: List<Albums>,
    val keywords: List<String>
)

data class SearchModelResponse(
    val result: Boolean,
    val data: List<Models>,
    val keywords: List<String>
)

data class AccessLogResponse(
    val result: Boolean,
    val history: HistoryItem,
    val message: String?
)

data class HistoryItem(
    val id: Int,
    val stay: Int,
    val device: String,
    val application: String,
    val type: String,
    val content: String,
    val error: String?,
    val time: String,
    val user_id: String,
    val ip: String
)

data class UpdateAccessLog(
    val result: Int
)

data class PackageInfo(
    val package_id: Int,
    val platform: String,
    val version: String,
    val version_id: Int,
    val version_type: String,
    val publish: String,
    val package_resource: String,
    val package_size: Int,
    val package_update_information: String?,
    val package_update_info: String?
)

data class HistoryResponse(
    val result: Boolean,
    val history: List<History>,
    val time_range: List<HistoryTimeRange>
)

data class History(
    val id: Int,
    val type: String,
    val content: Any,
    val time: String,
    val stay: Int
)

data class HistoryTimeRange(
    val time_range: String,
    val count: Int
)

data class CollectionResponse(
    val result: Boolean,
    val data: List<Collection>
)

data class Collection(
    val id: Int,
    val type: String,
    val content: Albums,
    val fold: String,
    val res_id: Int,
    val time: String
)

data class CollectionFoldResponse(
    val result: Boolean,
    val data: List<CollectionFold>
)

data class CollectionFold(
    val id: Int,
    val name: String,
    val cover: String?,
    val create_time: String?,
    val user_id: String,
    val publish: String
)

data class FollowResponse(
    val result: Boolean,
    val data: List<Follow>
)

data class Follow(
    val id: Int,
    val type: String,
    val content: Models,
    val fold: String,
    val res_id: Int,
    val time: String
)

data class ForbiddenResponse(
    val result: Boolean,
    val data: List<Forbidden>
)

data class Forbidden(
    val id: Int,
    val type: String,
    val content: Map<String, Any>,
    val res_id: String,
    val time: String,
    val user_id: String
)