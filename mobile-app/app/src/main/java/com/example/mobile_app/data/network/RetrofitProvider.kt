package com.example.mobile_app.data.network

import android.content.Context
import com.example.mobile_app.network.NetworkConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {
    fun createAuthApiService(
        context: Context? = null,
        baseUrl: String = NetworkConfig.resolveBaseUrl(context),
        okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
    ): AuthApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }
}

