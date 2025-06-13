package com.example.busmaker.data.api // 패키지 선언이 맞는지 확인하세요.

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor // HttpLoggingInterceptor import 확인
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.reflect.KProperty // KProperty import 추가 (lazy delegate 오류 관련)

object BusApiClient {
    private const val BASE_URL = "http://apis.data.go.kr/1613000/"

    // HttpLoggingInterceptor 설정
    // Interceptor는 OkHttp의 인터페이스이므로 HttpLoggingInterceptor 인스턴스를 직접 사용합니다.
    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient에 인터셉터 추가
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 여기서 loggingInterceptor 인스턴스를 직접 전달
        .build()

    // lazy 델리게이트의 getValue 시그니처 문제를 해결하기 위해 명시적 타입 지정 또는 컨텍스트 확인
    // 보통은 아래와 같이 BusApiService 타입을 명시해주면 해결됩니다.
    val service: BusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient) // 커스텀 OkHttpClient 사용
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BusApiService::class.java)
    }
}