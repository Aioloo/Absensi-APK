package com.example.absensiapk.models

import com.google.gson.annotations.SerializedName

data class AbsenceCountData(
    @SerializedName("total_kehadiran") val totalKehadiran: Int
)