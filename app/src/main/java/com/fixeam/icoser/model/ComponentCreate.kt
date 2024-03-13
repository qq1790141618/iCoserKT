package com.fixeam.icoser.model
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fixeam.icoser.databinding.AlbumItemBinding
import com.fixeam.icoser.network.Albums
import com.fixeam.icoser.painter.GlideBlurTransformation

class AlbumViewHolder(var binding: AlbumItemBinding) : RecyclerView.ViewHolder(binding.root)
@SuppressLint("SetTextI18n")
fun createAlbumBinding(context: Context, album: Albums, card: View, binding: AlbumItemBinding, type: String = "normal", remove: Boolean = false, removeCallback: () -> Unit = {}){
    // 创建点击事件
    card.setOnClickListener { startAlbumActivity(context, album.id) }
    // 修改海报图
    Glide.with(context)
        .load("${album.poster}/short500px")
        .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(context)))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(binding.posterBackground)
    Glide.with(context)
        .load("${album.poster}/short1200px")
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(binding.poster)
    // 修改图片数量
    binding.tag.text.text = "${(album.images as MutableList<*>).size}P"
    // 修改写真集名
    binding.name.text = when(album.model){
        null -> "${album.model_name} ${album.name}"
        else -> "${album.model} ${album.name}"
    }
    // 修改附加信息
    if(type == "new" || type == "hot") {
        binding.more.visibility = View.VISIBLE
        binding.otherInfo.visibility = View.VISIBLE
    }
    if(type == "new"){
        binding.timeIcon.visibility = View.VISIBLE
        binding.otherInfo.text = calculateTimeAgo(album.create_time)
    }
    if(type == "hot" && album.count != null){
        binding.viewIcon.visibility = View.VISIBLE
        binding.otherInfo.text = "${album.count}次浏览"
    }
    // 添加图片
    val dp = context.resources.displayMetrics.density
    binding.imagePreview.removeAllViews()
    for ((index, image) in (album.images as MutableList<*>).withIndex()){
        if(index >= 4){
            break
        }

        val cardView = CardView(context)
        val layoutParams = ViewGroup.MarginLayoutParams(
            (dp * 36).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.rightMargin = (dp * 5).toInt()
        cardView.layoutParams = layoutParams
        cardView.cardElevation = 0F
        cardView.radius = dp * 3

        val imageView = ImageView(context)
        imageView.id = View.generateViewId()
        val imageLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.layoutParams = imageLayoutParams
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        Glide.with(context)
            .load("${image}/yswidth300px")
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)

        cardView.addView(imageView)
        if(binding.imagePreview.childCount < 5){
            binding.imagePreview.addView(cardView, index)
        }
    }
    // 创建移除事件
    if(remove){
        binding.close.visibility = View.VISIBLE
        binding.close.setOnClickListener { removeCallback() }
    }
}
fun createSimpleDialog(context: Context, message: String, showCancel: Boolean = false, confirm: () -> Unit = {}){
    val builder = AlertDialog.Builder(context)
    builder.setMessage(message)

    builder.setPositiveButton("确定") { _, _ -> confirm() }
    if(showCancel){
        builder.setNegativeButton("取消") { _, _ -> }
    }

    val alertDialog = builder.create()
    alertDialog.show()
}