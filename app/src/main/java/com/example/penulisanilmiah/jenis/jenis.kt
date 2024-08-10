package com.example.penulisanilmiah.jenis

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class jenis(
    var name: String,
    var description: String,
    var photo: Int
) : Parcelable