package com.example.busmaker.data.model

import android.graphics.Color
import kotlin.math.abs

data class RouteInformation(
    val label: String = "추천 경로",
    var totalTime: String,
    val timeRangeAndFare: String,
    val segments: MutableList<RouteSegment>,
    val pinFixed: Boolean = false,

    val walkTimeToStartStopMin: Int = 0,
    val busTravelTimeMin: Int = 0,
    val walkTimeFromEndStopMin: Int = 0,
    var numericTotalTimeMin: Int = 0,

    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,

    // ★ 추가: 중간 정류장 위도/경도 리스트
    val stationLatList: List<Double>? = null,
    val stationLngList: List<Double>? = null
)

data class RouteSegment(
    val type: String, // "도보", "일반", "직행", "하차" 등
    val summary: String,
    val detail: String,
    val color: Int,
    val iconResId: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val stationName: String? = null
)

// 이하 RouteSegmentFactory 등 기존 그대로 사용
object RouteSegmentFactory {
    fun createWalkSegment(minutes: Int): RouteSegment {
        return RouteSegment(
            type = "도보",
            summary = "도보 ${minutes}분",
            detail = "",
            color = Color.parseColor("#757575")
        )
    }

    fun createBusSegment(
        busTypeDisplay: String,
        busNumber: String,
        startStopName: String,
        travelMinutes: Int,
        stationCount: Int,
        busColor: Int,
        lat: Double? = null,
        lng: Double? = null,
        stationName: String? = null
    ): RouteSegment {
        return RouteSegment(
            type = busTypeDisplay,
            summary = "$busTypeDisplay | $startStopName 승차",
            detail = "$busNumber | ${travelMinutes}분 (${stationCount}정류장)",
            color = busColor,
            lat = lat,
            lng = lng,
            stationName = stationName
        )
    }

    fun createAlightSegment(
        endStopName: String,
        lat: Double? = null,
        lng: Double? = null,
        stationName: String? = null
    ): RouteSegment {
        return RouteSegment(
            type = "하차",
            summary = "$endStopName 하차",
            detail = "",
            color = Color.parseColor("#BBBBBB"),
            lat = lat,
            lng = lng,
            stationName = stationName
        )
    }
}
