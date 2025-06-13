package com.example.busmaker.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NaverApiClient {
    private const val BASE_URL = "https://openapi.naver.com/"

    val api: NaverSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverSearchApi::class.java)
    }
}
