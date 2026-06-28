package com.example.topmejorestiendas

import android.app.Application
import android.util.Base64
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.intercept.Interceptor
import coil.request.ImageResult

class TopTiendasApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(Base64Interceptor())
            }
            .build()
    }
}

class Base64Interceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data is String && data.startsWith("data:image")) {
            try {
                val base64String = data.substringAfter(",")
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val newRequest = request.newBuilder().data(bytes).build()
                return chain.proceed(newRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return chain.proceed(request)
    }
}
