package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class LoginResponse (
    val token : String,
    @SerializedName("karyawan_id")
    val karyawanId : Int

)