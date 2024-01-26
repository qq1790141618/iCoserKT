package com.fixeam.icoserkt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class AlbumViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_view)

        val id = intent.getIntExtra("id", -1)
        Toast.makeText(this, "ID：$id", Toast.LENGTH_SHORT).show()
    }
}