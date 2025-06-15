package com.example.busmaker.data.model

import android.graphics.Color // Color 사용을 위해 import
import kotlin.math.abs

data class RouteInformation(
    val label: String = "추천 경로",
    var totalTime: String, // UI 표시용 총 소요 시간 문자열 (예: "2시간 21분")
    val timeRangeAndFare: String, // 예: "오후 4:09 - 오후 6:30 | 3,500원" (이 부분은 나중에 실제 시간 계산으로 채울 수 있음)
    val segments: MutableList<RouteSegment>, // 변경: 도보 구간 추가를 위해 MutableList로 변경
    val pinFixed: Boolean = false,

    // --- 새로 추가된 필드들 ---
    val walkTimeToStartStopMin: Int = 0,    // 출발지 -> 탑승 정류장까지 도보 시간 (분)
    val busTravelTimeMin: Int = 0,          // 순수 버스 이동 시간 (분)
    val walkTimeFromEndStopMin: Int = 0,    // 하차 정류장 -> 목적지까지 도보 시간 (분)
    var numericTotalTimeMin: Int = 0,        // 총 예상 소요 시간 (분, 숫자형, walk + bus + walk)

    // ★ 추가!
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null
)

data class RouteSegment(
    val type: String, // "도보", "일반", "직행", "하차" 등
    val summary: String, // 구간 요약 (예: "도보 5분", "일반 | 정류장 A 승차")
    val detail: String, // 구간 상세 (예: "390번 | 15분 (5정류장)")
    val color: Int, // 구간 표시 색상
    val iconResId: Int? = null, // (선택 사항) 구간 타입별 아이콘 리소스 ID (예: R.drawable.ic_walk, R.drawable.ic_bus)
    val lat: Double? = null,   // ← 추가! 정류장 마커용
    val lng: Double? = null,   // ← 추가!
    val stationName: String? = null  // ← 추가!
)

// RouteSegment 생성을 위한 헬퍼 함수 (선택 사항이지만 유용함)
object RouteSegmentFactory {
    fun createWalkSegment(minutes: Int): RouteSegment {
        return RouteSegment(
            type = "도보",
            summary = "도보 ${minutes}분",
            detail = "", // 도보 구간은 특별한 상세 정보가 없을 수 있음
            color = Color.parseColor("#757575") // 예시 도보 색상 (회색 계열)
            // iconResId = R.drawable.ic_walk // 실제 아이콘 리소스가 있다면 추가
        )
    }

    fun createBusSegment(
        busTypeDisplay: String,
        busNumber: String,
        startStopName: String,
        travelMinutes: Int,
        stationCount: Int,
        busColor: Int,
        lat: Double? = null,        // ← 추가 정류장 마커 표시
        lng: Double? = null,        // ← 추가
        stationName: String? = null // ← 추가
    ): RouteSegment {
        return RouteSegment(
            type = busTypeDisplay, // "일반", "직행" 등
            summary = "$busTypeDisplay | $startStopName 승차",
            detail = "$busNumber | ${travelMinutes}분 (${stationCount}정류장)",
            color = busColor,
            lat = lat,
            lng = lng,
            stationName = stationName
            // iconResId = R.drawable.ic_bus // 실제 아이콘 리소스가 있다면 추가
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