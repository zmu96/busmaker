// 예시: com.example.busmaker.data.model.route 패키지 내에 (또는 적절한 위치에)

package com.example.busmaker.data.model // 실제 패키지명으로 수정

import com.google.gson.annotations.SerializedName

// 전체 응답을 감싸는 최상위 클래스 (파일 이름이 RouteResponse.kt 라면 이 이름을 사용)
data class RouteResponse(
    @SerializedName("response")
    val responseData: RouteFullData? // 프로퍼티 이름을 response에서 변경하거나 그대로 사용
)

// "response" 내부의 내용을 담는 클래스
data class RouteFullData(
    @SerializedName("header")
    val header: RouteHeader?,

    @SerializedName("body")
    val body: RouteBodyContent?
)

// "header" 부분을 담는 클래스
data class RouteHeader(
    @SerializedName("resultCode")
    val resultCode: String?,

    @SerializedName("resultMsg")
    val resultMsg: String?
)

// "body" 부분을 담는 클래스 (페이징 정보 포함)
data class RouteBodyContent(
    @SerializedName("items")
    val itemsContainer: RouteItemsContainer?,

    @SerializedName("numOfRows")
    val numOfRows: Int?,

    @SerializedName("pageNo")
    val pageNo: Int?,

    @SerializedName("totalCount")
    val totalCount: Int?
)

// "items" 객체 내부의 "item" 배열을 담는 클래스
data class RouteItemsContainer(
    @SerializedName("item") // 실제 JSON 키는 "item"
    val routeList: List<RouteItemDetail>?
)

// 개별 경유 노선 정보를 담는 클래스 ("item" 배열의 요소)
data class RouteItemDetail(
    @SerializedName("routeid")
    val routeId: String?,

    @SerializedName("routeno")
    val routeNo: String?, // 명세에는 문자열이지만, 실제 숫자만 온다면 Int?도 고려 가능

    @SerializedName("routetp")
    val routeType: String?,

    @SerializedName("endnodenm")
    val endNodeName: String?,

    @SerializedName("startnodenm")
    val startNodeName: String?
)