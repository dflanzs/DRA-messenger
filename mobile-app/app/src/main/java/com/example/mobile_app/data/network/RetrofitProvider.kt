package com.example.mobile_app.data.network

import com.example.mobile_app.network.NetworkConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    fun createAuthApiService(
        baseUrl: String = NetworkConfig.DEFAULT_BASE_URL,
        okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
    ): AuthApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}

