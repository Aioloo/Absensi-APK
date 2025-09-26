// File: models/KaryawanData.kt
package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class KaryawanData(
    val id: Int,
    val nama: String,
    val divisi: String?,
    val email: String?,
    @SerializedName("foto_profil")
    val fotoProfil: String?,
    @SerializedName("jatah_cuti_per_bulan") val jatahCutiPerBulan: Int?,
    @SerializedName("sisa_cuti") val sisaCutiPerBulan: Int?,
    @SerializedName("perusahaan") val perusahaan: String?
)