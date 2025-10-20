package com.example.absensiapk.models
import com.google.gson.annotations.SerializedName

data class CheckInResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("karyawan") val karyawanId: Int? = null,
    @SerializedName("tanggal") val tanggal: String? = null,
    @SerializedName("jam_masuk") val jamMasuk: String? = null,
    @SerializedName("status_masuk") val statusMasuk: String? = null,
    @SerializedName("foto_masuk") val fotoMasuk: String? = null,
    @SerializedName("lokasi_masuk_lat") val lokasiMasukLat: Double? = null,
    @SerializedName("lokasi_masuk_long") val lokasiMasukLong: Double? = null,
    @SerializedName("alasan_keterlambatan") val alasanKeterlambatan: String? = null,
    @SerializedName("status_lokasi") val statusLokasi: String? = null,
    @SerializedName("location_warning") val locationWarning: LocationWarning? = null
)

data class LocationWarning(
    @SerializedName("message") val message: String,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("status") val status: String
)
