package com.example.busmaker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.example.busmaker.data.model.RouteSegment

class RouteMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var startLatLng: LatLng
    private lateinit var endLatLng: LatLng
    private lateinit var locationSource: FusedLocationSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_map)

        // 출발/도착 좌표 받아오기 (기본값은 서울 시청)
        val startLat = intent.getDoubleExtra("startLat", 37.5665)
        val startLng = intent.getDoubleExtra("startLng", 126.9780)
        val endLat = intent.getDoubleExtra("endLat", 37.5665)
        val endLng = intent.getDoubleExtra("endLng", 126.9780)

        startLatLng = LatLng(startLat, startLng)
        endLatLng = LatLng(endLat, endLng)

        locationSource = FusedLocationSource(this, 1000)

        val btnSearchRoute = findViewById<ImageButton>(R.id.btnSearchRoute)
        btnSearchRoute.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.naver_map_fragment) as MapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        // 출발지(지도상의 출발 위치) 마커
        Marker().apply {
            position = startLatLng
            captionText = "출발지"
            iconTintColor = android.graphics.Color.parseColor("#4CAF50")
            map = naverMap
        }
        // 도착지(지도상의 도착 위치) 마커
        Marker().apply {
            position = endLatLng
            captionText = "도착지"
            iconTintColor = android.graphics.Color.parseColor("#4CAF50")
            map = naverMap
        }

        // ★★ 중간 정류장 마커 추가(연한 초록) ★★
        val midLatArr = intent.getDoubleArrayExtra("stationLatList")
        val midLngArr = intent.getDoubleArrayExtra("stationLngList")
        if (midLatArr != null && midLngArr != null && midLatArr.size == midLngArr.size) {
            for (i in midLatArr.indices) {
                val point = LatLng(midLatArr[i], midLngArr[i])
                Marker().apply {
                    position = point
                    captionText = "경유 정류장"
                    iconTintColor = android.graphics.Color.parseColor("#A5D6A7")
                    map = naverMap
                }
            }
        }

        // segmentsJson 받아서 파싱 (경유지 포함)
        val segmentsJson = intent.getStringExtra("segmentsJson")
        val segments: List<RouteSegment> = if (!segmentsJson.isNullOrEmpty()) {
            val listType = object : TypeToken<List<RouteSegment>>() {}.type
            Gson().fromJson(segmentsJson, listType)
        } else {
            emptyList()
        }

        // ★ 승차/하차 정류장 마커 추가 ★
        val boardingSegment = segments.find { it.type == "일반" || it.type == "직행" }
        val alightSegment = segments.find { it.type == "하차" }
        // 승차 정류장 (진한 초록)
        if (boardingSegment?.lat != null && boardingSegment.lng != null) {
            Marker().apply {
                position = LatLng(boardingSegment.lat, boardingSegment.lng)
                captionText = boardingSegment.stationName ?: "승차 정류장"
                iconTintColor = android.graphics.Color.parseColor("#388E3C") // 진한 초록
                map = naverMap
            }
        }
        // 하차 정류장 (진한 초록)
        if (alightSegment?.lat != null && alightSegment.lng != null) {
            Marker().apply {
                position = LatLng(alightSegment.lat, alightSegment.lng)
                captionText = alightSegment.stationName ?: "하차 정류장"
                iconTintColor = android.graphics.Color.parseColor("#388E3C") // 진한 초록
                map = naverMap
            }
        }

        // ★★★ 전체 경로 좌표 리스트 생성: 출발지 → 승차정류장 → 경유정류장들 → 하차정류장 → 도착지 ★★★
        val pathCoords = mutableListOf<LatLng>()
        pathCoords.add(startLatLng) // 출발지

        if (boardingSegment?.lat != null && boardingSegment.lng != null) {
            pathCoords.add(LatLng(boardingSegment.lat, boardingSegment.lng)) // 승차정류장
        }

        // 경유 정류장들(순서대로)
        if (midLatArr != null && midLngArr != null && midLatArr.size == midLngArr.size) {
            for (i in midLatArr.indices) {
                pathCoords.add(LatLng(midLatArr[i], midLngArr[i]))
            }
        }

        if (alightSegment?.lat != null && alightSegment.lng != null) {
            pathCoords.add(LatLng(alightSegment.lat, alightSegment.lng)) // 하차정류장
        }

        pathCoords.add(endLatLng) // 도착지

        // 폴리라인(경로) 그리기
        PathOverlay().apply {
            coords = pathCoords
            color = android.graphics.Color.parseColor("#4CAF50")
            width = 8
            map = naverMap
        }

        // 카메라 영역 설정 (모든 좌표가 화면에 보이도록)
        val boundsBuilder = LatLngBounds.Builder()
            .include(startLatLng)
            .include(endLatLng)
        segments.forEach {
            if (it.lat != null && it.lng != null) {
                boundsBuilder.include(LatLng(it.lat, it.lng))
            }
        }
        if (midLatArr != null && midLngArr != null && midLatArr.size == midLngArr.size) {
            for (i in midLatArr.indices) {
                boundsBuilder.include(LatLng(midLatArr[i], midLngArr[i]))
            }
        }
        if (boardingSegment?.lat != null && boardingSegment.lng != null) {
            boundsBuilder.include(LatLng(boardingSegment.lat, boardingSegment.lng))
        }
        if (alightSegment?.lat != null && alightSegment.lng != null) {
            boundsBuilder.include(LatLng(alightSegment.lat, alightSegment.lng))
        }
        val bounds = boundsBuilder.build()
        val cameraUpdate = CameraUpdate.fitBounds(bounds, 100)
        naverMap.moveCamera(cameraUpdate)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (::locationSource.isInitialized) {
            locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
