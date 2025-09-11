package com.example.absensiapk.models
import com.google.gson.annotations.SerializedName

data class AttendanceData(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("karyawan") val karyawanId: Int? = null,
    @SerializedName("tanggal") val tanggal: String? = null,


    @SerializedName("jam_masuk") val jamMasuk: String? = null,
    @SerializedName("status_masuk") val statusMasuk: String? = null,
    @SerializedName("foto_masuk") val fotoMasuk: String? = null,
    @SerializedName("lokasi_masuk_lat") val lokasiMasukLat: Double? = null,
    @SerializedName("lokasi_masuk_long") val lokasiMasukLong: Double? = null,

    @SerializedName("jam_keluar") val jamKeluar: String? = null,
    @SerializedName("status_keluar") val statusKeluar: String? = null,
    @SerializedName("foto_keluar") val fotoKeluar: String? = null,
    @SerializedName("lokasi_keluar_lat") val lokasiKeluarLat: Double? = null,
    @SerializedName("lokasi_keluar_long") val lokasiKeluarLong: Double? = null,

    @SerializedName("jam_absen") val jamAbsen: String? = null,
    @SerializedName("status_absen") val statusAbsen: String? = null,
    @SerializedName("foto_absen") val fotoAbsen: String? = null,
    @SerializedName("lokasi_absen_lat") val lokasiAbsenLat: Double? = null,
    @SerializedName("lokasi_absen_long") val lokasiAbsenLong: Double? = null
)