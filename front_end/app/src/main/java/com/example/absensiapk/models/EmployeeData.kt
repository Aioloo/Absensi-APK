// File: models/KaryawanData.kt
package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class KaryawanData(
    val id: Int,
    val nama: String,
    val divisi: String?,
    val email: String?,
    @SerializedName("foto_profil")
    val fotoProfil: String?
)