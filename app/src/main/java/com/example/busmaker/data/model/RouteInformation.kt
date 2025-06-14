package com.example.busmaker.data.model

data class RouteInformation(
    val label: String = "추천 경로",    // "고정한 경로" 등 표시
    val totalTime: String,            // "2시간 21분" 등
    val timeRangeAndFare: String,     // "오후 4:09 - 오후 6:30 | 3,500원"
    val segments: List<RouteSegment>, // 각 구간별(도보/버스/환승) 정보
    val pinFixed: Boolean = false     // 고정 경로 여부 (핀 표시)
)

data class RouteSegment(
    val type: String, // "도보", "일반", "직행" 등
    val summary: String, // "일반  | 범화터널입구, 물푸레마을"
    val detail: String, // "390번 | 1분 (1정류장) [여유]"
    val color: Int      // 색상(예: Color.parseColor("#4CAF50"))
)

//※ segment 개수(최대 4개)로 제한해도 되고, 리스트 동적 생성도 가능