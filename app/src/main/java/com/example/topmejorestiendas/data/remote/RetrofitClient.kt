package com.example.topmejorestiendas.data.remote

import com.example.topmejorestiendas.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton de Retrofit. Lee la URL del backend desde BuildConfig.BACKEND_URL,
 * que a su vez se configura en local.properties → build.gradle.kts.
 *
 * Para desarrollo local con el emulador de Android:
 *   BACKEND_URL=http://10.0.2.2:3000
 *
 * Para producción en Render:
 *   BACKEND_URL=https://ubitop-api.onrender.com
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.HEADERS
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Helper para construir el header Authorization Bearer de forma segura.
     */
    fun bearerToken(token: String) = "Bearer $token"
}
