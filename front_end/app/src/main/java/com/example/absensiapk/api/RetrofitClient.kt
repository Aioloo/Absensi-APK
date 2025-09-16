package com.example.absensiapk.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient(private val context: Context) {
    companion object{
        const val BASE_URL = "http://10.185.0.48:8000/"
    }

    private val loginClient = OkHttpClient.Builder().build()

    private val authClient = OkHttpClient.Builder()
        .addInterceptor(JwtInterceptor(context))
        .build()

    // Service untuk login (menggunakan loginClient)
    val loginApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(loginClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Service untuk API yang dilindungi (menggunakan authClient)
    val protectedApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}