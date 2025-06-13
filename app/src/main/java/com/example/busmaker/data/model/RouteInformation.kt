package com.example.busmaker.data.model

data class RouteInformation(
    val totalTime: Int,       // 총 소요 시간 (분)
    val transferCount: Int,   // 환승 횟수
    val routeSummary: String  // 예: "버스 720 → 302"
)
