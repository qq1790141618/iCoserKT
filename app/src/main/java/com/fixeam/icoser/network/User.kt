package com.fixeam.icoser.network

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.fixeam.icoser.R
import com.fixeam.icoser.model.removeSharedPreferencesKey
import com.fixeam.icoser.model.sendAlbumNotification
import com.fixeam.icoser.model.userFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 用户变量
var userToken: String? = null
var userInform: UserInform? = null
var userCollection: List<Collection> = listOf()
var userFollow: List<Follow> = listOf()
var userCollectionFold: MutableList<CollectionFold> = mutableListOf()
var userForbidden: List<Forbidden> = listOf()
var userHistory: HistoryResponse? = null
var userHistoryList: MutableList<History> = mutableListOf()
var userInformFailTime = 0

/**
 * 获取用户信息
 * @param access_token 用户访问令牌
 * @param context 执行本次操作的上下文对象
 */
fun verifyTokenAndGetUserInform(access_token: String, context: Context){
    if(userInformFailTime >= 3){
        return
    }

    val call = ApiNetService.GetUserInform(access_token)

    call.enqueue(object : Callback<UserInformResponse> {
        override fun onResponse(call: Call<UserInformResponse>, response: Response<UserInformResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if(responseBody.result && responseBody.inform != null){
                        userInform = responseBody.inform
                        userToken = access_token
                        userFragment?.initUserCard(responseBody.inform)

                        requestFollowData(context, true){
                            for (album in followAlbumList){
                                if(album.type == "follow" && album.isNew){
                                    sendAlbumNotification(context, album)
                                }
                            }
                        }
                        getUserCollectionFold(context){
                            getUserCollection(context) {}
                        }
                        getUserHistory(context){ }
                        getUserFollow(context){ }
                        getUserForbidden(context){ }
                    } else {
                        removeSharedPreferencesKey("access_token", context)
                    }
                }
            }
        }

        override fun onFailure(call: Call<UserInformResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            userInformFailTime++
            verifyTokenAndGetUserInform(access_token, context)
        }
    })
}

/**
 * 获取用户收藏
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 */
fun getUserCollection(context: Context, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.GetUserCollection(userToken!!)

    call.enqueue(object : Callback<CollectionResponse> {
        override fun onResponse(call: Call<CollectionResponse>, response: Response<CollectionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    val collections = responseBody.data
                    userCollection = collections
                    userFragment?.setCollectionNumber(collections.size)
                    initCollectionOfFold(collections)
                }

                callback()
            }
        }

        override fun onFailure(call: Call<CollectionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 将收藏中的收藏夹补全到用户收藏夹
 * @param collections 用户的收藏内容列表
 */
fun initCollectionOfFold(collections: List<Collection>){
    for (collect in collections){
        val fold = collect.fold
        var hasFold = false
        for (collectionFold in userCollectionFold){
            if(collectionFold.name == fold){
                hasFold = true
                break
            }
        }
        if(!hasFold){
            userCollectionFold.add(
                CollectionFold(
                    id = -(userCollectionFold.size + 2),
                    name = fold,
                    cover = null,
                    create_time = collect.time,
                    user_id = userInform?.id.toString(),
                    publish = "public"
                )
            )
        }
    }
}

/**
 * 获取用户收藏夹
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 */
fun getUserCollectionFold(context: Context, callback: () -> Unit){
    if(userToken == null){
        return
    }
    userCollectionFold.clear()
    userCollectionFold.add(
        CollectionFold(
            id = -1,
            name = "default",
            cover = null,
            create_time = null,
            user_id = userInform?.id.toString(),
            publish = "public"
        )
    )
    userCollectionFold.add(
        CollectionFold(
            id = -2,
            name = "like",
            cover = null,
            create_time = null,
            user_id = userInform?.id.toString(),
            publish = "public"
        )
    )

    val call = ApiNetService.GetUserCollectionFold(userToken!!)
    call.enqueue(object : Callback<CollectionFoldResponse> {
        override fun onResponse(call: Call<CollectionFoldResponse>, response: Response<CollectionFoldResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    userCollectionFold.addAll(responseBody.data)
                }

                callback()
            }
        }

        override fun onFailure(call: Call<CollectionFoldResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 设置/添加用户收藏夹
 * @param name 收藏夹的名称
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 */
fun setUserCollectionFold(context: Context, name: String, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.SetUserCollectionFold(userToken!!, name)
    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    callback()
                }
            }
        }

        override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 移除用户收藏夹
 * @param id 收藏夹ID
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 */
fun removeUserCollectionFold(context: Context, id: Int, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.RemoveCollectionFold(userToken!!, id)
    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    callback()
                }
            }
        }

        override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 设置写真集收藏
 * @param album 将要收藏/取消的写真集对象
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 * @param unLog 用户未登录异常的回调函数
 */
fun setAlbumCollection(context: Context, album: Albums, fold: String = "", callback: () -> Unit, unLog: () -> Unit){
    if(userToken != null){
        var call = ApiNetService.SetCollectionItem(userToken!!, album.id, "album", fold)
        if(album.is_collection != null){
            call = ApiNetService.RemoveCollectionItem(userToken!!, album.id, "album")
        }

        call.enqueue(object : Callback<ActionResponse> {
            override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if(!responseBody?.result!!){
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        return
                    }

                    getUserCollection(context) { }
                    callback()
                    Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    } else {
        unLog()
    }
}

/**
 * 设置模特关注
 * @param model 将要关注/取消的模特对象
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 * @param unLog 用户未登录异常的回调函数
 */
fun setModelFollowing(context: Context, model: Models, callback: () -> Unit, unLog: () -> Unit){
    if(userToken != null){
        var call = ApiNetService.SetCollectionItem(userToken!!, model.id, "model")
        if(model.is_collection != null){
            call = ApiNetService.RemoveCollectionItem(userToken!!, model.id, "model")
        }

        call.enqueue(object : Callback<ActionResponse> {
            override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if(!responseBody?.result!!){
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        return
                    }

                    getUserFollow(context){ }
                    callback()
                    Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    } else {
        unLog()
    }
}

/**
 * 使用模特ID设置模特关注
 * @param modelId 将要关注/取消的模特ID
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 * @param unLog 用户未登录异常的回调函数
 */
fun setModelFollowingById(context: Context, modelId: Int, callback: () -> Unit, unLog: () -> Unit){
    if(userToken != null){
        val call = ApiNetService.SetCollectionItem(userToken!!, modelId, "model")

        call.enqueue(object : Callback<ActionResponse> {
            override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if(!responseBody?.result!!){
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        return
                    }

                    getUserFollow(context){ }
                    callback()
                    Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    } else {
        unLog()
    }
}

/**
 * 设置内容屏蔽
 * @param id 将要屏蔽内容的ID
 * @param type 将要屏蔽内容的类型，可选的值为"model"或"album"
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 * @param unLog 用户未登录异常的回调函数
 */
fun setForbidden(context: Context, id: Int, type: String, callback: () -> Unit, unLog: () -> Unit){
    if(userToken != null){
        val call = ApiNetService.SetForbiddenItem(userToken!!, id, type)

        call.enqueue(object : Callback<ActionResponse> {
            override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if(!responseBody?.result!!){
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        return
                    }

                    getUserForbidden(context){}
                    callback()
                }
            }

            override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
                // 处理请求失败的逻辑
                Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    } else {
        unLog()
    }
}

/**
 * 移除对内容的屏蔽
 * @param id 将要屏蔽内容的ID
 * @param context 执行本次操作的上下文对象
 * @param callback 在异步获取到信息后的回调函数
 */
fun removeForbidden(context: Context, id: Int, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.RemoveForbiddenItem(userToken!!, id)
    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(!responseBody?.result!!){
                    Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                    return
                }

                callback()
            }
        }

        override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 打开收藏夹选择器
 *
 * 本函数用于在用户界面中打开一个收藏夹选择器，允许用户从中选择一个收藏夹。
 * 选择器的具体实现依赖于上下文对象`context`以及`album`参数。
 *
 * @param context 执行本次操作的上下文对象，通常指代当前活动或应用的环境。
 * @param album 用于在选择器中展示的相册数据。
 * @param callback 在异步获取到信息后的回调函数。此回调函数接受一个参数：
 *        - name: 用户从选择器中选定的收藏夹名称。此名称用于后续的业务逻辑处理
 * @param unLog 一个无参数的函数，用于执行未登录状态下的特定逻辑
 */
fun openCollectionSelector(context: Context, album: Albums, callback: (name: String) -> Unit, unLog: () -> Unit){
    if(userToken == null){
        unLog()
        return
    }

    val builder = AlertDialog.Builder(context)
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val dialogView: View = inflater.inflate(R.layout.collection_fold_selector, null)
    builder.setView(dialogView)
    val alertDialog = builder.create()

    val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group)
    if(userCollectionFold.isEmpty()){
        getUserCollectionFold(context){
            getUserCollection(context) {
                initCollectionSelectorRadioGroup(context, radioGroup)
            }
        }
    } else {
        initCollectionSelectorRadioGroup(context, radioGroup)
    }

    val createButton = dialogView.findViewById<MaterialButton>(R.id.create)
    createButton.setOnClickListener {
        val builder1 = AlertDialog.Builder(context)
        val textInput = inflater.inflate(R.layout.text_input, null)
        textInput.findViewById<TextInputLayout>(R.id.label).hint = "收藏夹名称"

        val dialogView1: View = textInput
        builder1.setView(dialogView1)
        val alertDialog1 = builder1.create()

        val closeButton1 = textInput.findViewById<MaterialButton>(R.id.close)
        closeButton1.setOnClickListener {
            alertDialog1.cancel()
        }
        val confirmButton1 = textInput.findViewById<MaterialButton>(R.id.confirm)
        confirmButton1.setOnClickListener {
            var name = textInput.findViewById<TextInputEditText>(R.id.edit).text.toString()
            if(name == ""){
                name = "未命名的收藏夹"
            }
            confirmButton1.setIconResource(R.drawable.loading2)
            closeButton1.isEnabled = false
            confirmButton1.isEnabled = false

            setUserCollectionFold(context, name){
                getUserCollectionFold(context){
                    getUserCollection(context) {
                        confirmButton1.icon = null
                        closeButton1.isEnabled = true
                        confirmButton1.isEnabled = true
                        alertDialog1.cancel()

                        initCollectionSelectorRadioGroup(context, radioGroup)
                    }
                }
            }
        }

        alertDialog1.show()
    }
    val closeButton = dialogView.findViewById<MaterialButton>(R.id.close)
    closeButton.setOnClickListener {
        alertDialog.cancel()
    }
    val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirm)
    confirmButton.setOnClickListener {
        val id = radioGroup.checkedRadioButtonId
        val fold = userCollectionFold[id]
        confirmButton.setIconResource(R.drawable.loading2)
        confirmButton.isEnabled = false
        closeButton.isEnabled = false

        setAlbumCollection(context, album, fold.name, {
            confirmButton.icon = null
            confirmButton.isEnabled = true
            closeButton.isEnabled = true
            alertDialog.cancel()

            callback(fold.name)
        }, { unLog() })
    }

    alertDialog.show()
}

/**
 * 向收藏夹单选列表添加单选选项
 * @param context 执行本次操作的上下文对象
 * @param radioGroup 收藏夹单选列表
 */
fun initCollectionSelectorRadioGroup(context: Context, radioGroup: RadioGroup){
    radioGroup.removeAllViews()

    for ((index, fold) in userCollectionFold.withIndex()){
        val radioButton = RadioButton(context)
        radioButton.id = index

        radioButton.text = when(fold.name){
            "default" -> { "默认收藏夹" }
            "like" -> { "我喜欢" }
            else -> { fold.name }
        }

        val params = RadioGroup.LayoutParams(
            RadioGroup.LayoutParams.MATCH_PARENT,
            RadioGroup.LayoutParams.WRAP_CONTENT
        )
        val horizontalMargin = (context.resources.displayMetrics.density * 12).toInt()
        val topMargin = (context.resources.displayMetrics.density * 12).toInt()
        params.setMargins(horizontalMargin, topMargin, horizontalMargin, 0)
        radioButton.layoutParams = params

        radioGroup.addView(radioButton)
    }

    radioGroup.check(radioGroup.getChildAt(userCollectionFold.size - 1).id)
}

/**
 * 获取用户历史记录
 * @param context 执行本次操作的上下文对象
 * @param isRefresh 清空获取到的数据并重新获取
 * @param number 本次获取历史数据的条目数量
 * @param callback 在执行完本次获取请求后的回调函数
 */
fun getUserHistory(context: Context, isRefresh: Boolean = true, number: Int = 50, callback: () -> Unit){
    if(userToken == null){
        return
    }

    var call = ApiNetService.GetUserHistory(userToken!!)
    if(!isRefresh){
        call = ApiNetService.GetUserHistory(userToken!!, userHistoryList.size, number)
    }

    call.enqueue(object : Callback<HistoryResponse> {
        override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    userHistory = responseBody
                    if(isRefresh){
                        userHistoryList.clear()
                    }
                    userHistoryList.addAll(responseBody.history)

                    var total = 0
                    for (timeRange in responseBody.time_range){
                        total += timeRange.count
                    }
                    userFragment?.setHistoryNumber(total)
                }

                callback()
            }
        }

        override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 清除用户历史记录
 * @param context 执行本次操作的上下文对象
 * @param [id] 本次删除的历史记录ID（空 - 清空全部历史记录）
 * @param callback 在执行完本次请求后的回调函数
 */
fun clearUserHistory(context: Context, id: Int = -1, callback: (Boolean) -> Unit){
    if(userToken == null){
        callback(false)
        return
    }

    var call = ApiNetService.ClearUserHistory(userToken!!)
    if(id > 0){
        call = ApiNetService.ClearUserHistoryById(userToken!!, id)
    }

    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    callback(true)
                    Toast.makeText(context, "清除成功", Toast.LENGTH_SHORT).show()
                }

                callback(false)
            }
        }

        override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 获取用户关注
 * @param context 执行本次操作的上下文对象
 * @param callback 在执行完本次请求后的回调函数
 */
fun getUserFollow(context: Context, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.GetUserFollow(userToken!!)

    call.enqueue(object : Callback<FollowResponse> {
        override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    userFollow = responseBody.data
                    userFragment?.setFollowNumber(responseBody.data.size)
                }

                callback()
            }
        }

        override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 获取用户屏蔽内容
 * @param context 执行本次操作的上下文对象
 * @param callback 在执行完本次请求后的回调函数
 */
fun getUserForbidden(context: Context, callback: () -> Unit){
    if(userToken == null){
        return
    }

    val call = ApiNetService.GetUserForbidden(userToken!!)

    call.enqueue(object : Callback<ForbiddenResponse> {
        override fun onResponse(call: Call<ForbiddenResponse>, response: Response<ForbiddenResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()

                if(responseBody != null && responseBody.result){
                    userForbidden = responseBody.data
                    userFragment?.setForbiddenNumber(responseBody.data.size)
                }

                callback()
            }
        }

        override fun onFailure(call: Call<ForbiddenResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

// 关注内容更新获取相关参数
var followAlbumLoading: Boolean = false
var followIsFinished: Boolean = false
val followAlbumList: MutableList<Albums> = mutableListOf()
/**
 * 获取关注内容更新
 * @param context 执行本次操作的上下文对象
 * @param callback 在执行完本次请求后的回调函数
 * @param isRefresh 刷新并重新从最开始获取
 */
fun requestFollowData(context: Context, isRefresh: Boolean = false, callback: () -> Unit) {
    if(userToken == null){
        return
    }

    followAlbumLoading = true
    var start = followAlbumList.size
    if(isRefresh) {
        start = 0
    }
    val call = ApiNetService.GetFollow(userToken!!, start, 20)

    call.enqueue(object : Callback<AlbumsResponse> {
        override fun onResponse(call: Call<AlbumsResponse>, response: Response<AlbumsResponse>) {
            if (response.isSuccessful) {
                if(isRefresh){
                    followAlbumList.clear()
                }

                val responseBody = response.body()
                responseBody?.let {
                    for ((index, item) in it.data.withIndex()){
                        if(index > 1 && item.type == it.data[index - 1].type && item.model_id == it.data[index - 1].model_id && item.type != "follow"){
                            continue
                        }
                        if(index > 1 && item.id == it.data[index - 1].id){
                            continue
                        }
                        followAlbumList.add(item)
                    }
                    if(it.data.size < 20){
                        followIsFinished = true
                    }
                }

                callback()
                followAlbumLoading = false
            }
        }

        override fun onFailure(call: Call<AlbumsResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            Toast.makeText(context, "请求失败：" + t.message, Toast.LENGTH_SHORT).show()
        }
    })
}

/**
 * 更新用户信息
 * @param token 用户访问令牌
 * @param inform 用户信息json对象
 * @param onSuccess 操作成功回调
 * @param onFail 操作失败回调
 */
fun setUserInform(token: String, inform: String, onSuccess: () -> Unit, onFail: () -> Unit){
    if(userInformFailTime >= 3){
        return
    }

    val call = ApiNetService.setUserInform(token, inform)

    call.enqueue(object : Callback<ActionResponse> {
        override fun onResponse(call: Call<ActionResponse>, response: Response<ActionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null && responseBody.result) {
                    onSuccess()
                } else {
                    onFail()
                }
            }
        }

        override fun onFailure(call: Call<ActionResponse>, t: Throwable) {
            // 处理请求失败的逻辑
            onFail()
        }
    })
}