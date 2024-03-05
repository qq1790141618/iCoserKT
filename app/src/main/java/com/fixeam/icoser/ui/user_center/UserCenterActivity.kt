package com.fixeam.icoser.ui.user_center

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.Editable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fixeam.icoser.R
import com.fixeam.icoser.model.CascaderItem
import com.fixeam.icoser.model.Option
import com.fixeam.icoser.model.copyToClipboard
import com.fixeam.icoser.model.createCascader
import com.fixeam.icoser.model.initOptionItem
import com.fixeam.icoser.model.isDarken
import com.fixeam.icoser.model.setOptionItemPress
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.model.userFragment
import com.fixeam.icoser.network.ApiNetService
import com.fixeam.icoser.network.FileUploadResponse
import com.fixeam.icoser.network.setUserInform
import com.fixeam.icoser.network.userInform
import com.fixeam.icoser.network.userToken
import com.fixeam.icoser.ui.login_page.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class UserCenterActivity : AppCompatActivity() {
    private lateinit var resultLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_center)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置导航栏
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.user_inform)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // 获取登录状态
        if(userToken == null){
            onBackPressed()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // 设置文件选择启动器
        resultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // 处理选中的文件
                startAvatarCrop(uri)
            }
        }

        initPage()
    }

    @SuppressLint("InflateParams")
    private fun initPage(){
        // 显示头像
        initAvatar()
        // 显示昵称
        initNickname()
        // 设置头像选项点击
        val avatarOption = findViewById<LinearLayout>(R.id.avatar_option)
        setOptionItemPress(avatarOption, isDarken(this)){
            resultLauncher.launch("image/*")
        }
        // 监听昵称编辑
        val nicknameOption = findViewById<LinearLayout>(R.id.nickname_option)
        setOptionItemPress(nicknameOption, isDarken(this)){
            val builder = AlertDialog.Builder(this)
            val textInput = layoutInflater.inflate(R.layout.text_input, null)
            textInput.findViewById<TextInputLayout>(R.id.label).hint = getString(R.string.user_nickname)
            val textEdit = textInput.findViewById<TextInputEditText>(R.id.edit)
            textEdit.text = Editable.Factory.getInstance().newEditable(userInform?.nickname)

            val dialogView: View = textInput
            builder.setView(dialogView)
            val alertDialog = builder.create()

            val closeButton = textInput.findViewById<MaterialButton>(R.id.close)
            closeButton.setOnClickListener {
                alertDialog.cancel()
            }
            val confirmButton = textInput.findViewById<MaterialButton>(R.id.confirm)
            confirmButton.setOnClickListener {
                val name = textEdit.text.toString()
                if(name != ""){
                    confirmButton.setIconResource(R.drawable.loading2)
                    closeButton.isEnabled = false
                    confirmButton.isEnabled = false

                    fun closeDialog(){
                        confirmButton.icon = null
                        closeButton.isEnabled = true
                        confirmButton.isEnabled = true
                        alertDialog.cancel()
                    }

                    val inform = Gson().toJson(mapOf("nickname" to name))
                    setUserInform(
                        userToken!!,
                        inform,
                        {
                            if(userInform != null){
                                userInform!!.nickname = name
                                userFragment?.initUserCard(userInform!!)
                            }
                            initNickname()
                            Toast.makeText(this@UserCenterActivity, "保存成功", Toast.LENGTH_SHORT).show()
                            closeDialog()
                        },
                        {
                            initNickname()
                            Toast.makeText(this@UserCenterActivity, "保存失败", Toast.LENGTH_SHORT).show()
                            closeDialog()
                        }
                    )
                } else {
                    Toast.makeText(this@UserCenterActivity, "用户昵称不能为空", Toast.LENGTH_SHORT).show()
                }
            }

            alertDialog.show()
        }
        // 添加不可编辑内容
        val aOptionsContainer = findViewById<LinearLayout>(R.id.a_option)
        initOptionItem(
            Option(
                iconId = R.drawable.id,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.username,
                contentText = userInform?.username,
                showHrefIcon = false,
                onClick = {
                    userInform?.username?.let { copyToClipboard(this, it){} }
                }
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.identity,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_identity,
                contentText = userInform?.identity,
                showHrefIcon = false
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.phone,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_phone,
                contentText = userInform?.phone,
                showHrefIcon = false,
                onClick = {
                    userInform?.phone?.let { copyToClipboard(this, it){} }
                }
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.mail,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_mail,
                contentText = userInform?.mail,
                showHrefIcon = false,
                clearMargin = true,
                onClick = {
                    userInform?.mail?.let { copyToClipboard(this, it){} }
                }
            ),
            aOptionsContainer,
            this,
            isDarken(this)
        )
        // 更多选项
        val bOptionsContainer = findViewById<LinearLayout>(R.id.b_option)
        initOptionItem(
            Option(
                iconId = R.drawable.location,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_location,
                contentText = userInform?.location,
                onClick = {
                    openCitySelector()
                }
            ),
            bOptionsContainer,
            this,
            isDarken(this)
        )
        initOptionItem(
            Option(
                iconId = R.drawable.birthday,
                iconColor = ColorStateList.valueOf(Color.parseColor("#a9aeb8")),
                textId = R.string.user_birthday,
                contentText = userInform?.birthday,
                clearMargin = true,
                onClick = {
                    openDateSelector()
                }
            ),
            bOptionsContainer,
            this,
            isDarken(this)
        )
        setCityName()
    }

    private fun initAvatar(){
        val avatar = findViewById<ImageView>(R.id.avatar)
        Glide.with(this)
            .load(userInform?.header)
            .placeholder(R.drawable.image_holder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(avatar)
    }

    private fun initNickname(){
        val nicknameTextView = findViewById<TextView>(R.id.nickname)
        nicknameTextView.text = userInform?.nickname
    }

    private fun initBirthday(){
        val bOptionsContainer = findViewById<LinearLayout>(R.id.b_option)
        if(bOptionsContainer.childCount < 2){
            return
        }
        val item = bOptionsContainer.getChildAt(1) as LinearLayout
        val contentText = item.findViewById<TextView>(R.id.content_text)
        contentText.text = userInform?.birthday
    }

    private fun startAvatarCrop(resourceUri: Uri) {
        val backgroundColor = when(isDarken(this)){
            true -> R.color.black
            false -> R.color.white
        }
        val primaryColor = when(isDarken(this)){
            true -> R.color.white
            false -> R.color.black
        }
        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(this@UserCenterActivity, backgroundColor))
            setStatusBarColor(ContextCompat.getColor(this@UserCenterActivity, backgroundColor))
            setToolbarWidgetColor(ContextCompat.getColor(this@UserCenterActivity, primaryColor))
        }
        UCrop.of(resourceUri, createImageFileUri())
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(600, 600)
            .withOptions(options)
            .start(this)
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFileUri(): Uri {
        // 创建图片文件名称
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // 创建空白图片文件
        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        // 获取文件的URI
        return FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            // 裁剪成功
            val resultUri: Uri? = UCrop.getOutput(data!!)
            if (resultUri != null) {
                uploadAvatar(resultUri){
                    val inform = Gson().toJson(mapOf("header" to it.url))
                    setUserInform(
                        userToken!!,
                        inform,
                        {
                            if(userInform != null){
                                userInform!!.header = it.url
                                userFragment?.initUserCard(userInform!!)
                            }
                            initAvatar()
                            Toast.makeText(this@UserCenterActivity, "头像修改成功", Toast.LENGTH_SHORT).show()
                        },
                        {
                            Toast.makeText(this@UserCenterActivity, "头像修改失败", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(this@UserCenterActivity, "图片裁剪失败", Toast.LENGTH_SHORT).show()
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.printStackTrace()
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    @SuppressLint("Recycle")
    private fun uploadAvatar(uri: Uri, callback: (FileUploadResponse) -> Unit){
        if(userToken == null){
            return
        }

        val inputStream = contentResolver.openInputStream(uri)
        val file = getFileName(uri)?.let { File(cacheDir, it) }
        if (file != null) {
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val requestFile = RequestBody.create(MediaType.parse("image/jpg"), file)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val call = ApiNetService.uploadFile(userToken!!, body)
            call.enqueue(object : Callback<FileUploadResponse> {
                override fun onResponse(call: Call<FileUploadResponse>, response: Response<FileUploadResponse>) {
                    if (response.isSuccessful) {
                        // 处理成功的响应
                        response.body()?.let { callback(it) }
                    } else {
                        Toast.makeText(this@UserCenterActivity, "文件上传失败: " + response.errorBody(), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FileUploadResponse>, t: Throwable) {
                    // 处理失败的请求，例如网络错误
                    Toast.makeText(this@UserCenterActivity, "文件上传失败: " + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun loadLocalItemsFromJson(): List<CascaderItem> {
        val jsonString = try {
            assets.open("city.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return listOf()
        }

        val listType = object : TypeToken<List<CascaderItem>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }

    private fun setCityName(){
        val localItems = loadLocalItemsFromJson()
        val text = userInform?.location?.let { getLocationNameFromCityJson(localItems, it) }

        val bOptionsContainer = findViewById<LinearLayout>(R.id.b_option)
        if(bOptionsContainer.childCount < 1){
            return
        }
        val item = bOptionsContainer.getChildAt(0) as LinearLayout
        val contentText = item.findViewById<TextView>(R.id.content_text)
        contentText.text = text
    }

    private fun getLocationNameFromCityJson(localItems: List<CascaderItem>, id: String): String?{
        for (item in localItems){
            if(item.value == id){
                return item.name
            }
            if(item.children != null){
                val f = getLocationNameFromCityJson(item.children, id)
                if(f != null){
                    return item.name + "/" + f
                }
            }
        }
        return null
    }

    private fun openCitySelector(){
        val regions = loadLocalItemsFromJson()
        createCascader(
            findViewById(R.id.user_center),
            this,
            regions,
            userInform?.location,
            "请选择地区："
        ){
            if(it != null){
                val inform = Gson().toJson(mapOf("location" to it))
                setUserInform(
                    userToken!!,
                    inform,
                    {
                        if(userInform != null){
                            userInform!!.location = it
                        }
                        setCityName()
                        Toast.makeText(this@UserCenterActivity, "修改成功", Toast.LENGTH_SHORT).show()
                    },
                    {
                        Toast.makeText(this@UserCenterActivity, "修改失败", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun openDateSelector(){
        var defaultYear = 2000
        var defaultMonth = 1
        var defaultDayOfMonth = 1
        val parts = userInform?.birthday?.split("-")
        if(parts != null){
            defaultYear = parts[0].toInt()
            defaultMonth = parts[1].toInt() - 1
            defaultDayOfMonth = parts[2].toInt()
        }

        // 创建DatePickerDialog实例，并设置指定的默认日期
        val datePickerDialog = DatePickerDialog(this@UserCenterActivity, { view, selectedYear, selectedMonth, selectedDayOfMonth ->
            var birthday = "$selectedYear-"
            birthday += if(selectedMonth + 1 < 10){
                "0${selectedMonth + 1}-"
            } else {
                "${selectedMonth + 1}-"
            }
            birthday += if(selectedDayOfMonth < 10){
                "0$selectedDayOfMonth"
            } else {
                "$selectedDayOfMonth"
            }

            val inform = Gson().toJson(mapOf("birthday" to birthday))
            setUserInform(
                userToken!!,
                inform,
                {
                    if(userInform != null){
                        userInform!!.birthday = birthday
                    }
                    initBirthday()
                    Toast.makeText(this@UserCenterActivity, "修改成功", Toast.LENGTH_SHORT).show()
                },
                {
                    initBirthday()
                    Toast.makeText(this@UserCenterActivity, "修改失败", Toast.LENGTH_SHORT).show()
                }
            )
        }, defaultYear, defaultMonth, defaultDayOfMonth)

        datePickerDialog.show()
    }
}