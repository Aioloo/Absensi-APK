package com.example.absensiapk.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RetrofitClient(val context: Context) {
    companion object {
        const val BASE_URL = "https://absenentitas.pal.co.id/"

        // --- PINDAHKAN FUNGSI INI KE COMPANION OBJECT AGAR BISA DIAKSES COIL ---
        fun getUnsafeOkHttpClient(): OkHttpClient {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory

                val builder = OkHttpClient.Builder()
                builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }

                return builder.build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    // Gunakan fungsi dari companion object
    private val unsafeClient = getUnsafeOkHttpClient()

    private val unsafeAuthClient = getUnsafeOkHttpClient().newBuilder()
        .addInterceptor(JwtInterceptor(context))
        .build()

    // ... (sisa kode ApiService tetap sama)
    val loginApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(unsafeClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val protectedApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(unsafeAuthClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
