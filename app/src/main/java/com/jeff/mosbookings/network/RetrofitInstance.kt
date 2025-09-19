package com.jeff.mosbookings.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object RetrofitInstance {
    private const val BASE_URL = "https://hotel-api-nu2k.onrender.com/"
    private const val TAG = "API_LOGS"

    val api: ApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val requestInterceptor = Interceptor { chain ->
            val request = chain.request()
            val requestBody = request.body
            
            Log.d(TAG, "🚀 REQUEST: ${request.method} ${request.url}")
            Log.d(TAG, "📋 Headers: ${request.headers}")
            
            if (requestBody != null) {
                Log.d(TAG, "📦 Request Body Type: ${requestBody.contentType()}")
                Log.d(TAG, "📦 Request Body Length: ${requestBody.contentLength()}")
            }
            
            val response = chain.proceed(request)
            
            Log.d(TAG, "📥 RESPONSE: ${response.code} ${response.message}")
            Log.d(TAG, "📋 Response Headers: ${response.headers}")
            Log.d(TAG, "⏱️ Response Time: ${response.receivedResponseAtMillis - response.sentRequestAtMillis}ms")
            
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(requestInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}