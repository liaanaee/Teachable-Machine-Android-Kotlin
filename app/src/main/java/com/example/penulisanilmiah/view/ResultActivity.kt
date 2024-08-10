package com.example.penulisanilmiah.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.penulisanilmiah.R
import com.example.penulisanilmiah.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resultView()
    }

    private fun resultView() {
        val label = intent.getStringExtra("label")
        val imageUri = intent.getStringExtra("imageUri")

        binding.resultText.text = getString(R.string.hasil_analisis)
        binding.tvLabel.text = getString(R.string.analysis_type, label)
        binding.imageView.load(imageUri)
    }
}