// 예시: com.example.busmaker.data.model.routestation 패키지 내에 (또는 적절한 위치에)

package com.example.busmaker.data.model// 실제 패키지명으로 수정

import com.google.gson.annotations.SerializedName

// 전체 응답을 감싸는 최상위 클래스
data class RouteStationResponse(
    @SerializedName("response")
    val responseData: RouteStationFullData?
)

// "response" 내부의 내용을 담는 클래스
data class RouteStationFullData(
    @SerializedName("header")
    val header: RouteStationHeader?,

    @SerializedName("body")
    val body: RouteStationBodyContent?
)

// "header" 부분을 담는 클래스
data class RouteStationHeader(
    @SerializedName("resultCode")
    val resultCode: String?,

    @SerializedName("resultMsg")
    val resultMsg: String?
)

// "body" 부분을 담는 클래스 (페이징 정보 포함)
data class RouteStationBodyContent(
    @SerializedName("items")
    val itemsContainer: RouteStationItemsContainer?,

    @SerializedName("numOfRows")
    val numOfRows: Int?,

    @SerializedName("pageNo")
    val pageNo: Int?,

    @SerializedName("totalCount")
    val totalCount: Int?
)

// "items" 객체 내부의 "item" 배열을 담는 클래스
data class RouteStationItemsContainer(
    @SerializedName("item") // 실제 JSON 키는 "item"
    val stationList: List<RouteStationItemDetail>?
)

// 개별 경유 정류소 정보를 담는 클래스 ("item" 배열의 요소)
data class RouteStationItemDetail(
    @SerializedName("routeid") // 응답에도 routeid가 포함됨 (요청 파라미터와 동일)
    val routeId: String?,

    @SerializedName("nodeid")
    val nodeId: String?,

    @SerializedName("nodenm")
    val nodeName: String?,

    @SerializedName("nodeno") // 옵션(0)이지만 명세에 있음
    val nodeNo: String?, // 명세에는 문자열, 샘플은 숫자 문자열. 안전하게 String?

    @SerializedName("nodeord")
    val nodeOrder: Int?, // 명세에는 숫자, 샘플도 숫자.

    @SerializedName("gpslati")
    val gpsLati: Double?, // 명세에는 숫자. 안전하게 Double? 또는 String? 후 변환

    @SerializedName("gpslong")
    val gpsLong: Double?, // 명세에는 숫자. 안전하게 Double? 또는 String? 후 변환

    @SerializedName("updowncd") // 옵션(0)이지만 명세에 있음
    val upDownCode: String? // 명세에는 [0:상행, 1:하행]으로 설명. String? 또는 Int? 후 매핑
)