package com.example.absensiapk.api

import com.example.absensiapk.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Multipart
    @POST("api/absensi/checkin/")
    suspend fun checkIn(
        @Part("karyawan") karyawan: RequestBody,
        @Part("jam_masuk") jamMasuk: RequestBody,
        @Part("status_masuk") statusMasuk: RequestBody,
        @Part fotoMasuk: MultipartBody.Part,
        @Part("lokasi_masuk_lat") lokasiMasukLat: RequestBody,
        @Part("lokasi_masuk_long") lokasiMasukLong: RequestBody
    ): Response<AttendanceData>

    @Multipart
    @PATCH("api/absensi/{id}/checkout/")
    suspend fun checkOut(
        @Path("id") id: Int,
        @Part("jam_keluar") jamKeluar: RequestBody,
        @Part("status_keluar") statusKeluar: RequestBody,
        @Part fotoKeluar: MultipartBody.Part,
        @Part("lokasi_keluar_lat") lokasiKeluarLat: RequestBody,
        @Part("lokasi_keluar_long") lokasiKeluarLong: RequestBody
    ): Response<AttendanceData>

    @Multipart
    @POST("api/absensi/absen/")
    suspend fun absen(
        @Part("karyawan") karyawan: RequestBody,
        @Part("jam_absen") jamAbsen: RequestBody,
        @Part("status_absen") statusAbsen: RequestBody,
        @Part fotoAbsen: MultipartBody.Part,
        @Part("lokasi_absen_lat") lokasiAbsenLat: RequestBody,
        @Part("lokasi_absen_long") lokasiAbsenLong: RequestBody
    ): Response<AttendanceData>

    @GET("api/absensi/history/")
    suspend fun getAttendanceHistory(@Query("karyawan_id") karyawanId: Int): Response<List<AttendanceData>>

    @GET("api/karyawan/")
    suspend fun getEmployeeList(): Response<List<KaryawanData>>

    @GET("api/karyawan/{id}/")
    suspend fun getKaryawanDetail(@Path("id") id: Int): Response<KaryawanData>

    @GET("api/absensi/counts")
    suspend fun getAbsenceCount(): Response<AbsenceCountData>
}