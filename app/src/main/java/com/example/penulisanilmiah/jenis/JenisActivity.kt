package com.example.penulisanilmiah.jenis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.penulisanilmiah.R

class JenisActivity : AppCompatActivity() {

    private lateinit var jenisIkan: RecyclerView
    private val list = ArrayList<jenis>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jenis)
        jenisIkan = findViewById(R.id.jenisIkan)
        jenisIkan.setHasFixedSize(true)

        list.addAll(getListJenis())
        showRecyclerList()
    }

    private fun getListJenis(): ArrayList<jenis> {
        val dataName = resources.getStringArray(R.array.fish_name)
        val dataDescription = resources.getStringArray(R.array.data_description)
        val dataPhoto = resources.obtainTypedArray(R.array.data_photo)
        val listIkan = ArrayList<jenis>()
        for (i in dataName.indices) {
            val ikann = jenis(dataName[i], dataDescription[i], dataPhoto.getResourceId(i, -1))
            listIkan.add(ikann)
        }
        return listIkan
    }

    private fun showRecyclerList() {
        jenisIkan.layoutManager = LinearLayoutManager(this)
        val listIkanAdapter = ListJenisAdapter(list)
        jenisIkan.adapter = listIkanAdapter
        listIkanAdapter.setOnItemClickCallback(object : ListJenisAdapter.OnItemClickCallback(){
            override fun onItemClicked(data: jenis) {
                val intent = Intent(this@JenisActivity, DetailnyaActivity::class.java)
                intent.putExtra("NAME", data.name)
                intent.putExtra("DESCRIPTION", data.description)
                intent.putExtra("PHOTO", data.photo)
                startActivity(intent)
            }
        })
    }
}