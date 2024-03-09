package com.fixeam.icoser.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fixeam.icoser.ui.album_page.AlbumViewActivity
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.fixeam.icoser.ui.media_page.MediaViewActivity
import com.fixeam.icoser.ui.model_page.ModelViewActivity
import com.fixeam.icoser.ui.recommend_page.RecommendActivity
import com.fixeam.icoser.ui.search_page.SearchActivity
import com.fixeam.icoser.ui.update_dialog.UpdateActivity

/**
 * 打开模特页面
 * @param context 执行本次操作的上文菜单
 * @param id 模特编号
 * @param [doNotSetToken] 在无Token模式下打开，默认false
 * @return 打开的 Activity Intent
 */
fun startModelActivity(context: Context, id: Int, doNotSetToken: Boolean = false): Intent {
    val intent = Intent(context, ModelViewActivity::class.java)
    intent.putExtra("id", id)
    intent.putExtra("doNotSetToken", doNotSetToken)
    context.startActivity(intent)
    return intent
}
/**
 * 打开写真集页面
 * @param context 执行本次操作的上文菜单
 * @param id 写真集编号
 * @param [doNotSetToken] 在无Token模式下打开，默认false
 * @return 打开的 Activity Intent
 */
fun startAlbumActivity(context: Context, id: Int, doNotSetToken: Boolean = false): Intent {
    val intent = Intent(context, AlbumViewActivity::class.java)
    intent.putExtra("id", id)
    intent.putExtra("doNotSetToken", doNotSetToken)
    context.startActivity(intent)
    return intent
}
/**
 * 打开推荐页面
 * @param context 执行本次操作的上文菜单
 * @param type 推荐类型 0-热门 1-最新
 * @return 打开的 Activity Intent
 */
fun startRecommendActivity(context: Context, type: Int): Intent {
    val intent = Intent(context, RecommendActivity::class.java)
    intent.putExtra("type", type)
    context.startActivity(intent)
    return intent
}
/**
 * 打开视频播放页面
 * @param context 执行本次操作的上文菜单
 * @param [id] 写真集编号，默认-1
 * @param [modelId] 模特编号，默认-1
 * @param [albumId] 写真集编号，默认-1
 * @return 打开的 Activity Intent
 */
fun startMediaActivity(context: Context, id: Int = -1, modelId: Int = -1, albumId: Int = -1): Intent {
    val intent = Intent(context, MediaViewActivity::class.java)
    intent.putExtra("id", id)
    context.startActivity(intent)
    return intent
}
/**
 * 使用系统浏览器打开网页
 * @param context 执行本次操作的上文菜单
 * @param url 需要打开的网页网址
 * @return 打开的 Activity Intent
 */
fun openUrlInBrowser(context: Context, url: String): Intent {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
    return intent
}
/**
 * 打开视频播放页面
 * @param context 执行本次操作的上文菜单
 * @return 打开的 Activity Intent
 */
fun startUpdateActivity(context: Context): Intent {
    val intent = Intent(context, UpdateActivity::class.java)
    context.startActivity(intent)
    return intent
}
/**
 * 打开登录页面
 * @param context 执行本次操作的上文菜单
 * @return 打开的 Activity Intent
 */
fun startLoginActivity(context: Context): Intent {
    val intent = Intent(context, LoginActivity::class.java)
    context.startActivity(intent)
    return intent
}
/**
 * 打开搜索页面
 * @param context 执行本次操作的上文菜单
 * @return 打开的 Activity Intent
 */
fun startSearchActivity(context: Context): Intent {
    val intent = Intent(context, SearchActivity::class.java)
    context.startActivity(intent)
    return intent
}