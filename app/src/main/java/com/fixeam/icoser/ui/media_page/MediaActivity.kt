package com.fixeam.icoser.ui.media_page

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fixeam.icoser.R
import com.fixeam.icoser.databinding.ActivityMediaBinding
import com.fixeam.icoser.ui.main.fragment.SmartVideoFragment

class MediaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置返回键
        binding.back.setOnClickListener { onBackPressed() }
        // 获取数据
        val isMyFavor = intent.getBooleanExtra("is-my-favor", false)
        val startFrom = intent.getIntExtra("start-from", 0)
        val id = intent.getIntExtra("id", -1)
        val albumId = intent.getIntExtra("album-id", -1)
        val modelId = intent.getIntExtra("model-id", -1)
        // 添加主内容
        val fragment = SmartVideoFragment()
        val bundle = Bundle().apply {
            putBoolean("is-my-favor", isMyFavor)
            putInt("start-from", startFrom)
            putInt("id", id)
            putInt("album-id", albumId)
            putInt("model-id", modelId)
        }
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, fragment)
            commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        if (fragment is SmartVideoFragment) {
            supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
        }
    }
}