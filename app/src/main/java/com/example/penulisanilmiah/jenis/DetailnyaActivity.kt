package com.example.penulisanilmiah.jenis

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.penulisanilmiah.R

class DetailnyaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailnya)

        val name = intent.getStringExtra("NAME")
        val description = intent.getStringExtra("DESCRIPTION")
        val photoId = intent.getIntExtra("PHOTO", 0)

        val detailTitle: TextView = findViewById(R.id.textViewTitleDetail)
        val detailDesc: TextView = findViewById(R.id.textViewDescriptionDetail)
        val detailImage: ImageView = findViewById(R.id.imageViewDetail)

        detailTitle.text = name
        detailDesc.text = description
        detailImage.setImageResource(photoId)
    }
}