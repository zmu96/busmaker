package com.example.busmaker.data.model // 실제 패키지명

import com.google.gson.annotations.SerializedName

// 전체 응답을 감싸는 최상위 클래스 (파일 이름이 StationResponse.kt 이므로 이 이름을 사용)
data class StationResponse(
    @SerializedName("response")
    val responseData: StationFullData? // 프로퍼티 이름을 response에서 변경하여 혼동 방지, 또는 그대로 response 사용
)

// "response" 내부의 내용을 담는 클래스
data class StationFullData(
    @SerializedName("header")
    val header: StationHeader?,

    @SerializedName("body")
    val body: StationBodyContent?
)

// "header" 부분을 담는 클래스
data class StationHeader(
    @SerializedName("resultCode")
    val resultCode: String?,

    @SerializedName("resultMsg")
    val resultMsg: String?
)

// "body" 부분을 담는 클래스 (페이징 정보 포함)
data class StationBodyContent(
    @SerializedName("items")
    val itemsContainer: StationItemsContainer?, // 프로퍼티 이름 변경

    @SerializedName("numOfRows")
    val numOfRows: Int?,

    @SerializedName("pageNo")
    val pageNo: Int?,

    @SerializedName("totalCount")
    val totalCount: Int?
)

// "items" 객체 내부의 "item" 배열을 담는 클래스
data class StationItemsContainer(
    @SerializedName("item") // 실제 JSON 키는 "item"
    val stationList: List<StationItemDetail>? // 프로퍼티 이름을 stationList 등으로 명확히
)

// 개별 정류소 정보를 담는 클래스 ("item" 배열의 요소)
data class StationItemDetail(
    @SerializedName("nodeid")
    val nodeId: String?,    // 정류소 ID

    @SerializedName("nodenm")
    val nodeName: String?,    // 정류소 이름

    @SerializedName("gpslati")
    val gpsLati: Double?,   // 위도 (타입 주의: 실제 응답이 문자열이면 String?으로 변경 후 변환)

    @SerializedName("gpslong")
    val gpsLong: Double?,    // 경도 (타입 주의)

    @SerializedName("citycode") // 추가된 필드
    val cityCode: String?, // 또는 Int? (실제 JSON 타입에 따라)

    @SerializedName("nodeno") // JSON 예시에 있는 추가 필드 (필요하다면)
    val nodeNo: Int?
)