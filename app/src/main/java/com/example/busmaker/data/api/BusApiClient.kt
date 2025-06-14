package com.example.busmaker.data.api // 패키지 선언

// 필요한 import 문들
import com.example.busmaker.data.model.RouteItemsContainer // ★★★ RouteItemsContainer 모델 import 추가 ★★★
import com.example.busmaker.data.model.StationItemsContainer
import com.google.gson.GsonBuilder // ★★★ GsonBuilder import 추가 ★★★
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BusApiClient {
    private const val BASE_URL = "http://apis.data.go.kr/1613000/"

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // BusApiService 인터페이스는 같은 패키지 내에 있다고 가정
    // RouteItemsContainerDeserializer 클래스도 같은 패키지 내에 있다고 가정

    val service: BusApiService by lazy {
        // ★★★ 커스텀 Deserializer를 포함한 Gson 인스턴스 생성 ★★★
        val gson = GsonBuilder()
            .registerTypeAdapter(RouteItemsContainer::class.java, RouteItemsContainerDeserializer())
            .registerTypeAdapter(StationItemsContainer::class.java, StationItemsDeserializer()) // ★★★ 이 줄 추가 ★★★
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient) // 커스텀 OkHttpClient 사용
            // ★★★ 수정된 Gson 인스턴스를 사용하여 ConverterFactory 생성 ★★★
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BusApiService::class.java)
    }
}