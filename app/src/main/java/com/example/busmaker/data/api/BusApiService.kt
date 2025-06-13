package com.example.busmaker.data.api

import com.example.busmaker.data.model.RouteResponse
import com.example.busmaker.data.model.RouteStationResponse
import com.example.busmaker.data.model.StationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BusApiService {
    @GET("BusSttnInfoInqireService/getCrdntPrxmtSttnList")
    suspend fun getNearbyStations(
        @Query("serviceKey") serviceKey: String,
        @Query("gpsLati") latitude: Double,
        @Query("gpsLong") longitude: Double,
        @Query("numOfRows") numOfRows: Int? = 10,
        @Query("pageNo") pageNo: Int? = 1,
        @Query("_type") type: String = "json"
    ): Response<StationResponse> // 반환 타입은 StationResponse (최상위 응답 클래스)

    @GET("BusSttnInfoInqireService/getSttnThrghRouteList")
    suspend fun getThroughRoutesByStation(
        @Query("serviceKey") serviceKey: String,
        @Query("cityCode") cityCode: Int, // API 명세에 따라 타입 확인 (보통 Int 또는 String)
        @Query("nodeid") nodeId: String,
        @Query("numOfRows") numOfRows: Int? = 10,    // 명세의 기본값 또는 null
        @Query("pageNo") pageNo: Int? = 1,        // 명세의 기본값 또는 null
        @Query("_type") type: String = "json"       // JSON 응답 요청
    ): Response<RouteResponse> // ★★★ Retrofit의 Response로 감싸고, 올바르게 정의된 RouteResponse 사용 ★★★

    @GET("BusRouteInfoInqireService/getRouteAcctoThrghSttnList")
    suspend fun getStationsByRouteId(
        @Query("serviceKey") serviceKey: String,
        @Query("cityCode") cityCode: Int,
        @Query("routeId") routeId: String,
        @Query("pageNo") pageNo: Int? = 1,        // 명세 샘플 기본값 또는 null
        @Query("numOfRows") numOfRows: Int? = 10,  // 명세 샘플 기본값 또는 null
        @Query("_type") type: String = "json"
    ): Response<RouteStationResponse> // ★★★ Retrofit의 Response로 감싸고, 올바르게 정의된 RouteStationResponse 사용 ★★★


}