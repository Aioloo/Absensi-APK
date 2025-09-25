package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class TimeOffData(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("karyawan") val karyawanId: Int? = null,
    val jenis: String? = null,
    @SerializedName("tanggal_mulai") val tanggalMulai: String? = null,
    @SerializedName("tanggal_selesai") val tanggalSelesai: String? = null,
    val alasan: String? = null,
    val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)