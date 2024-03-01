package com.fixeam.icoser.ui.update_dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.fixeam.icoser.R
import com.fixeam.icoser.model.bytesToReadableSize
import com.fixeam.icoser.model.newVersion
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.DownloadManager
import com.fixeam.icoser.network.DownloadState
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.File

class UpdateActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置关闭按钮
        val closeButton = findViewById<ImageView>(R.id.close)
        closeButton.setOnClickListener {
            onBackPressed()
        }
        val nextButton = findViewById<MaterialButton>(R.id.next)
        nextButton.setOnClickListener {
            doNotAlertVersion()
        }

        // 设置更新版本名称
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = packageInfo.versionName
        val versionChange = findViewById<TextView>(R.id.version_change)
        versionChange.text = "${getString(R.string.version_change)} $version => ${newVersion?.version} (大小: ${
            newVersion?.package_size?.let {
            bytesToReadableSize(
                it
            )
        }})"

        // 设置更新按钮事件
        val updateButton = findViewById<MaterialButton>(R.id.update)
        updateButton.setOnClickListener {
            updateDownload()
        }
        val viewUpdateLogButton = findViewById<TextView>(R.id.view_update_log)
        viewUpdateLogButton.setOnClickListener {
            val url = "https://update.fixeam.com/android?ver=${newVersion?.version_id}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun doNotAlertVersion() {
        val sharedPreferences = getSharedPreferences("version", AppCompatActivity.MODE_PRIVATE)
        newVersion?.let { sharedPreferences.edit().putInt("do_not_alert_version", it.version_id) }
        onBackPressed()
    }

    @SuppressLint("SetTextI18n")
    private fun startDownload(url: String, outputFile: File, onComplete: () -> Unit) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val progressText = findViewById<TextView>(R.id.progress_text)

        lifecycleScope.launchWhenCreated {
            DownloadManager.download(
                url,
                outputFile
            ).collect {
                when (it) {
                    is DownloadState.InProgress -> {
                        progressBar.progress = it.progress
                        progressText.text = "${it.progress}%"
                    }
                    is DownloadState.Success -> {
                        onComplete()
                    }
                    is DownloadState.Error -> {
                        Log.d("~~~", "download error: ${it.throwable}.")
                    }
                }
            }
        }
    }

    private fun updateDownload(){
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(downloadFolder,
            newVersion?.package_resource?.substringAfterLast("/") ?: "app-package.apk"
        )

        // 更新界面显示
        val buttons = findViewById<MaterialButtonToggleGroup>(R.id.buttons)
        buttons.visibility = View.GONE
        val viewUpdateLogButton = findViewById<TextView>(R.id.view_update_log)
        viewUpdateLogButton.visibility = View.GONE
        val versionChange = findViewById<TextView>(R.id.version_change)
        versionChange.visibility = View.GONE
        val progress = findViewById<LinearLayout>(R.id.progress)
        progress.visibility = View.VISIBLE

        // 执行下载事件
        newVersion?.package_resource?.let { startDownload(it, outputFile){
            val install = findViewById<MaterialButton>(R.id.install)
            install.visibility = View.VISIBLE
            progress.visibility = View.GONE

            val tip = findViewById<TextView>(R.id.tip)
            tip.text = "软件包下载已经完成!"

            install.setOnClickListener {
                val uri = FileProvider.getUriForFile(this, "$packageName.provider", outputFile)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = uri
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)
            }
        } }
    }
}