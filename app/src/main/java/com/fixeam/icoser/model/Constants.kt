package com.fixeam.icoser.model

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.ui.main.fragment.CollectionFragment
import com.fixeam.icoser.ui.main.fragment.HomeFragment
import com.fixeam.icoser.ui.main.fragment.SmartVideoFragment
import com.fixeam.icoser.ui.main.fragment.UserFragment
import com.google.android.material.button.MaterialButton
import java.io.OutputStream

// 界面变量
var homeFragment: HomeFragment? = null
var collectionFragment: CollectionFragment? = null
var smartVideoFragment: SmartVideoFragment? = null
var userFragment: UserFragment? = null
var showFragment: Fragment? = null
var hasNotificationProgression: Boolean = true

// 移除共享变量
fun removeSharedPreferencesKey(key: String, context: Context){
    val sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(key)
    editor.apply()
}

// 分享文字
fun shareTextContent(text: String, title: String = "来自iCoser的分享", context: Context) {
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(shareIntent, title))
}

// 仅下载图片
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun downloadImage(imageUrl: String, context: Context) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // 创建下载请求
    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl.substringAfterLast("/"))

    // 将下载请求加入下载队列
    val downloadId = downloadManager.enqueue(request)

    // 注册下载完成的广播接收器
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadIdCompleted = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                if (downloadIdCompleted == downloadId) {
                    val downloadedUri = downloadManager.getUriForDownloadedFile(downloadIdCompleted)
                    Toast.makeText(context, "图片已经保存到相册${imageUrl.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
                }
            }

            // 将 setResultCode 和其它相关方法放在 onReceive 方法内部
            resultCode = Activity.RESULT_OK
            resultData = null
        }
    }

    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
}

// 分享图片调用
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun shareImageContent(imageUrl: String, title: String = "来自iCoser的分享", context: Context) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // 创建下载请求
    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl.substringAfterLast("/"))

    // 将下载请求加入下载队列
    val downloadId = downloadManager.enqueue(request)

    // 注册下载完成的广播接收器
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadIdCompleted = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                if (downloadIdCompleted == downloadId) {
                    val downloadedUri = downloadManager.getUriForDownloadedFile(downloadIdCompleted)
                    context?.let { shareImageUri(downloadedUri, title, it) }
                }
            }

            // 将 setResultCode 和其它相关方法放在 onReceive 方法内部
            resultCode = Activity.RESULT_OK
            resultData = null
        }
    }

    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
}

// 分享图片
fun shareImageUri(imageUri: Uri?, title: String = "来自iCoser的分享", context: Context) {
    imageUri?.let {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}

// 获取屏幕宽度
fun getScreenWidth(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

// 保存图片到相册
fun saveImageToGallery(context: Context, imageView: ImageView) {
    val drawable = imageView.drawable
    if (drawable is BitmapDrawable) {
        val bitmap = drawable.bitmap
        val savedUri = saveBitmapToGallery(context, bitmap)
        if (savedUri != null) {
            // 发送媒体扫描广播，通知相册更新
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = savedUri
            context.sendBroadcast(mediaScanIntent)
            // 提示保存成功
            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
        } else {
            // 提示保存失败
            Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
        }
    } else {
        // 提示无法获取图片
        Toast.makeText(context, "无法获取图片", Toast.LENGTH_SHORT).show()
    }
}

// 保存 Bitmap 到相册
fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    val displayName = "${System.currentTimeMillis()}.png"

    val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    }

    val resolver = context.contentResolver
    var stream: OutputStream? = null
    var uri: Uri? = null

    try {
        // 插入图片
        uri = resolver.insert(imageCollection, contentValues)
        if (uri == null) {
            throw Exception("Failed to create new MediaStore record.")
        }

        // 写入数据
        stream = resolver.openOutputStream(uri)
        if (stream == null) {
            throw Exception("Failed to get output stream.")
        }
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            throw Exception("Failed to save bitmap.")
        }
    } catch (e: Exception) {
        uri = null
        e.printStackTrace()
    } finally {
        stream?.close()
    }

    return uri
}

// 设置状态栏主题
@SuppressLint("ObsoleteSdkInt")
fun setStatusBarColor(activity: Activity, color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activity.window.statusBarColor = color
    }
}

// 设置状态栏文字颜色
@SuppressLint("ObsoleteSdkInt")
fun setStatusBarTextColor(activity: Activity, isDark: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val decorView = activity.window.decorView
        var flags = decorView.systemUiVisibility
        flags = if (isDark) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        decorView.systemUiVisibility = flags
    }
}

// 自动设置状态栏
fun setStatusBar(activity: Activity, lightColor: Int, darkColor: Int){
    val sharedPreferences = activity.getSharedPreferences("theme", Context.MODE_PRIVATE)
    when(sharedPreferences.getInt("color_mode", 0)){
        0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    val currentTheme = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
        setStatusBarColor(activity, darkColor)
        setStatusBarTextColor(activity, false)
    } else {
        setStatusBarColor(activity, lightColor)
        setStatusBarTextColor(activity, true)
    }
}

// 获取是否为深色主题
fun isDarken(activity: Activity): Boolean{
    val currentTheme = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentTheme == Configuration.UI_MODE_NIGHT_YES
}

// 添加列表项
fun initOptionItem(option: Option, root: ViewGroup, activity: Activity, isDark: Boolean){
    val optionItem = activity.layoutInflater.inflate(R.layout.option_item, root, false)

    val textView = optionItem.findViewById<TextView>(R.id.text)
    if(option.textId > 0){
        try {
            textView.text = activity.getString(option.textId)
        } catch (e: Exception){
            e.printStackTrace()
        }
    } else if(textView.text != null){
        textView.text = option.text
    }

    val leftIcon = optionItem.findViewById<ImageView>(R.id.left_icon)
    val leftImage = optionItem.findViewById<ImageView>(R.id.left_image)
    if(option.leftImageUrl != null){
        leftIcon.visibility = View.GONE
        leftImage.visibility = View.VISIBLE

        Glide.with(activity)
            .load("${option.leftImageUrl}/yswidth300px")
            .placeholder(R.drawable.image_holder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(leftImage)
    } else {
        if(option.iconId > 0){
            leftIcon.imageTintList = option.iconColor
            try {
                leftIcon.setImageResource(option.iconId)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    if(!option.showHrefIcon){
        val rightIcon = optionItem.findViewById<ImageView>(R.id.right_icon)
        rightIcon.visibility = View.INVISIBLE
        rightIcon.layoutParams = LinearLayout.LayoutParams(
            60,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    if(option.contentText != null){
        val contentText = optionItem.findViewById<TextView>(R.id.content_text)
        contentText.text = option.contentText
    }

    if(option.clearMargin){
        val layoutParams = LinearLayout.LayoutParams(
            optionItem.layoutParams.width,
            optionItem.layoutParams.height
        )
        layoutParams.bottomMargin = 0
        optionItem.layoutParams = layoutParams
    }

    if(option.showRemoveButton) {
        val removeButton = optionItem.findViewById<MaterialButton>(R.id.remove)
        removeButton.visibility = View.VISIBLE
        removeButton.setOnClickListener {
            option.onRemove()
        }
    }

    setOptionItemPress(optionItem, isDark){
        option.onClick()
        option.onClickWithContext(it)
    }
    root.addView(optionItem)
}

// 设置列表项按下效果
fun setOptionItemPress(view: View, isDark: Boolean, onClick: (View) -> Unit){
    var pressDownColor = Color.parseColor("#F6F6F6")
    var pressUpColor = Color.parseColor("#FFFFFF")
    if(isDark){
        pressDownColor = Color.parseColor("#222222")
        pressUpColor = Color.parseColor("#000000")
    }
    var downTime: Long = 0

    view.setOnTouchListener(object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.setBackgroundColor(pressDownColor)
                    downTime = System.currentTimeMillis()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    view.setBackgroundColor(pressUpColor)
                    val upTime = System.currentTimeMillis()
                    val duration = upTime - downTime
                    if(duration < 300){
                        onClick(view)
                    }
                    return true  // 返回true表示消费了该事件
                }
            }
            return false
        }
    })
}

// 复制到剪贴板
fun copyToClipboard(context: Context, text: String, callback: () -> Unit) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label", text)
    clipboard.setPrimaryClip(clip)
}

// 全局窗口亮度调整
fun changeBackgroundDim(isDim: Boolean, activity: Activity) {
    val window = activity.window
    val layoutParams = window.attributes
    layoutParams.alpha = if (isDim) 0.5f else 1.0f
    window.attributes = layoutParams
}

data class CascaderItem(
    val name: String,
    val value: String,
    val children: List<CascaderItem>?
)
val cascaderListData: MutableList<List<CascaderItem>> = mutableListOf()
// 创建级联选择器
@SuppressLint("InflateParams", "NotifyDataSetChanged")
fun createCascader(root: ViewGroup, activity: Activity, list: List<CascaderItem>, defaultValue: String? = null, title: String? = null, selectedCall: (String?) -> Unit){
    changeBackgroundDim(true, activity)
    val contentView = activity.layoutInflater.inflate(R.layout.cascader_view, null)
    val popupWindow = PopupWindow(
        contentView,
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        true
    )
    popupWindow.animationStyle = R.style.PopupAnimation
    popupWindow.showAtLocation(
        root,
        Gravity.BOTTOM,
        0,
        0
    )
    popupWindow.setOnDismissListener {
        changeBackgroundDim(false, activity)
    }
    val closeButton = contentView.findViewById<MaterialButton>(R.id.close)
    closeButton?.setOnClickListener {
        selectedCall(null)
        popupWindow.dismiss()
    }

    if(title != null){
        val titleTextView = contentView.findViewById<TextView>(R.id.title)
        titleTextView.text = title
    }

    // 初始化一级选择列表
    cascaderListData.clear()
    cascaderListData.add(list)

    // 初始化文本内容
    val textLayout = contentView.findViewById<LinearLayout>(R.id.text_layout)
    textLayout.getChildAt(0).visibility = View.VISIBLE
    val selectionIndex: MutableList<Int> = mutableListOf(0)

    // 列表adapter
    val dataViewPage = contentView.findViewById<ViewPager2>(R.id.data_view_page)
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class CascaderItemListAdapter(private val viewPagerPosition: Int): RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(activity).inflate(R.layout.option_item, parent, false)
            return ViewHolder(itemView)
        }
        override fun getItemCount(): Int {
            return cascaderListData[viewPagerPosition].size
        }
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // 修改holder
            val item = cascaderListData[viewPagerPosition][position]
            holder.itemView.findViewById<ImageView>(R.id.right_icon).visibility = View.INVISIBLE
            val leftIcon = holder.itemView.findViewById<ImageView>(R.id.left_icon)
            val textViewLevel = textLayout.getChildAt(viewPagerPosition) as TextView
            leftIcon.setImageResource(R.drawable.check)
            if(position == selectionIndex[viewPagerPosition]){
                leftIcon.visibility = View.VISIBLE
                textViewLevel.visibility = View.VISIBLE
                textViewLevel.text = cascaderListData[viewPagerPosition][position].name
                textViewLevel.setOnClickListener {
                    setCascaderActiveIndexStyle(contentView, activity, viewPagerPosition)
                    dataViewPage.setCurrentItem(viewPagerPosition, true)
                }
            } else {
                leftIcon.visibility = View.INVISIBLE
            }
            val textView = holder.itemView.findViewById<TextView>(R.id.text)
            textView.text = item.name

            holder.itemView.setOnClickListener {
                selectionIndex[viewPagerPosition] = position
                textViewLevel.text = cascaderListData[viewPagerPosition][position].name

                if(item.children != null){
                    if(viewPagerPosition != cascaderListData.size - 1){
                        val tempList: MutableList<List<CascaderItem>> = mutableListOf()
                        for (s in 0..viewPagerPosition){
                            tempList.add(cascaderListData[s])
                        }
                        cascaderListData.clear()
                        cascaderListData.addAll(tempList)
                    }
                    selectionIndex.add(0)
                    cascaderListData.add(item.children)
                    dataViewPage.adapter?.notifyDataSetChanged()
                    dataViewPage.setCurrentItem(viewPagerPosition + 1, true)
                    setCascaderActiveIndexStyle(contentView, activity, viewPagerPosition + 1)

                    for(r in viewPagerPosition + 1..<textLayout.childCount){
                        textLayout.getChildAt(r).visibility = View.GONE
                        textLayout.getChildAt(r).setOnClickListener {  }
                    }
                } else {
                    selectedCall(cascaderListData[viewPagerPosition][position].value)
                    popupWindow.dismiss()
                }
            }
        }
    }
    class ViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class ViewPagerAdapter : RecyclerView.Adapter<ViewPagerViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
                val view = RecyclerView(activity)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                view.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                return ViewPagerViewHolder(view)
            }

            override fun getItemCount(): Int {
                return cascaderListData.size
            }

            override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
                val item = holder.itemView as RecyclerView
                item.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                val adapter = CascaderItemListAdapter(position)
                item.adapter = adapter
                item.scrollToPosition(selectionIndex[position])
            }
        }

    // 初始化ViewPager
    val adapter = ViewPagerAdapter()
    dataViewPage.adapter = adapter
    dataViewPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            setCascaderActiveIndexStyle(contentView, activity, position)
        }
    })

    // 初始化已选择项目
    if(defaultValue != null){
        selectionIndex.clear()
        selectionIndex.addAll(calculateCascaderIndex(list, defaultValue))

        var lastGroup: CascaderItem = list[selectionIndex[0]]
        setCascaderActiveIndexStyle(contentView, activity, selectionIndex.size - 1)

        for (index in selectionIndex.indices){
            dataViewPage.setCurrentItem(index, true)
            val itemText = textLayout.getChildAt(index) as TextView
            itemText.visibility = View.VISIBLE
            itemText.text = lastGroup.name
            itemText.setOnClickListener {
                dataViewPage.setCurrentItem(index, true)
                setCascaderActiveIndexStyle(contentView, activity, index)
            }

            if (lastGroup.children != null) {
                cascaderListData.add(lastGroup.children!!)
                dataViewPage.adapter?.notifyDataSetChanged()

                lastGroup = lastGroup.children!![selectionIndex[index + 1]]
            } else {
                break
            }
        }
    }
}

fun setCascaderActiveIndexStyle(cascaderView: View, activity: Activity, activeIndex: Int){
    val textLayout = cascaderView.findViewById<LinearLayout>(R.id.text_layout)
    setCascaderBlock(
        cascaderView,
        activeIndex,
        (activity.resources.displayMetrics.density * 72).toInt()
    )
    for (index in 0..<textLayout.childCount){
        val itemText = textLayout.getChildAt(index) as TextView
        if(activeIndex == index){
            itemText.setTextColor(Color.parseColor("#618dff"))
        } else {
            if(isDarken(activity)){
                itemText.setTextColor(Color.WHITE)
            } else {
                itemText.setTextColor(Color.BLACK)
            }
        }
    }
}

fun setCascaderBlock(cascaderView: View, index: Int, itemWidth: Int){
    val block = cascaderView.findViewById<LinearLayout>(R.id.level_block)
    val moveDistance = itemWidth * index
    block.animate().translationX(moveDistance.toFloat()).setDuration(300).start()
}

fun calculateCascaderIndex(items: List<CascaderItem>, value: String): List<Int>{
    val resultList: MutableList<Int> = mutableListOf()
    for ((index, item) in items.withIndex()){
        if(item.value == value){
            resultList.add(index)
        }
        if(item.children != null){
            val f = calculateCascaderIndex(item.children, value)
            if(f.isNotEmpty()){
                resultList.add(index)
                resultList.addAll(f)
            }
        }
    }
    return resultList
}

// 列表视图
class CustomArrayAdapter(context: Context, resource: Int, objects: List<String>, private val textSize: Float, private val height: Int, private val isCenter: Boolean = false) : ArrayAdapter<String>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        view.layoutParams.height = (context.resources.displayMetrics.density * height).toInt()
        if(isCenter){
            view.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        return view
    }
}

