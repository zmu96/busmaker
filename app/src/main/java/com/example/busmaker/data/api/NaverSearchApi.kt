package com.example.busmaker.data.api

import com.example.busmaker.data.model.PlaceSearchResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverSearchApi {
    @Headers(
        "X-Naver-Client-Id: 4OwhNbGr_18lg33cBA3o",
        "X-Naver-Client-Secret: W2A3UnENf1"
    )

    @GET("v1/search/local.json")
    suspend fun searchPlace(
        @Query("query") query: String,
        @Query("display") display: Int = 1
    ): PlaceSearchResponse
}
