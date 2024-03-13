package com.fixeam.icoser.ui.update_dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityUpdateBinding
import com.fixeam.icoser.model.bytesToReadableSize
import com.fixeam.icoser.model.newVersion
import com.fixeam.icoser.model.openUrlInBrowser
import com.fixeam.icoser.model.setStatusBar
import com.fixeam.icoser.network.DownloadManager
import com.fixeam.icoser.network.DownloadState
import java.io.File

class UpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateBinding
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置颜色主题
        setStatusBar(this, Color.WHITE, Color.BLACK)

        // 设置关闭按钮
        binding.close.setOnClickListener { onBackPressed() }
        binding.next.setOnClickListener { doNotAlertVersion() }

        // 设置更新版本名称
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = packageInfo.versionName
        binding.versionChange.text = "${getString(R.string.version_change)} $version => ${newVersion?.version} (大小: ${
            newVersion?.package_size?.let {
            bytesToReadableSize(
                it.toLong()
            )
        }})"

        // 设置更新按钮事件
        binding.update.setOnClickListener { updateDownload() }
        binding.viewUpdateLog.setOnClickListener { openUrlInBrowser(this, "https://update.fixeam.com/android?ver=${newVersion?.version_id}") }
    }

    private fun doNotAlertVersion() {
        val sharedPreferences = getSharedPreferences("version", MODE_PRIVATE)
        newVersion?.let { sharedPreferences.edit().putInt("do_not_alert_version", it.version_id).apply() }
        onBackPressed()
    }

    @SuppressLint("SetTextI18n")
    private fun startDownload(url: String, outputFile: File, onComplete: () -> Unit) {
        val progressBar = binding.progressBar
        val progressText = binding.progressText

        lifecycleScope.launchWhenCreated {
            DownloadManager.download(
                url,
                outputFile
            ).collect {
                when (it) {
                    is DownloadState.InProgress -> {
                        progressBar.progress = it.progress
                        progressText.text = "${it.progress}% ${bytesToReadableSize(it.downloadedBytes)} / ${bytesToReadableSize(it.totalBytes)}"
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
        binding.buttons.visibility = View.GONE
        binding.viewUpdateLog.visibility = View.GONE
        binding.versionChange.visibility = View.GONE
        binding.progress.visibility = View.VISIBLE

        // 执行下载事件
        newVersion?.package_resource?.let { startDownload(it, outputFile){
            binding.install.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
            binding.tip.text = "软件包下载已经完成!"

            binding.install.setOnClickListener {
                val uri = FileProvider.getUriForFile(this, "$packageName.provider", outputFile)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = uri
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)

                finish()
            }
        } }
    }
}