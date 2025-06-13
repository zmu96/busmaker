package com.example.busmaker.data.model

data class PlaceSearchResponse(
    val items: List<PlaceItem>
)

data class PlaceItem(
    val title: String,
    val address: String,
    val roadAddress: String,
    val mapx: String, // 경도 (X)
    val mapy: String  // 위도 (Y)
)
