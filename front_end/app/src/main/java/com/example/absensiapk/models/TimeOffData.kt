package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class TimeOffData(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("karyawan")
    val karyawanId: Int? = null,
    @SerializedName("jenis")
    val jenis: String? = null,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String? = null,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String? = null,
    @SerializedName("alasan")
    val alasan: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)