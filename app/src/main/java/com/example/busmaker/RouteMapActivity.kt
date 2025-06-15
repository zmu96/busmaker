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

        // 전달받은 좌표
        val startLat = intent.getDoubleExtra("startLat", 37.5665)
        val startLng = intent.getDoubleExtra("startLng", 126.9780)
        val endLat = intent.getDoubleExtra("endLat", 37.5665)
        val endLng = intent.getDoubleExtra("endLng", 126.9780)

        startLatLng = LatLng(startLat, startLng)
        endLatLng = LatLng(endLat, endLng)

        locationSource = FusedLocationSource(this, 1000)

        // 길찾기 버튼 연결 (기존)
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

        // 출발지/도착지 마커
        Marker().apply {
            position = startLatLng
            captionText = "출발지"
            map = naverMap
        }
        Marker().apply {
            position = endLatLng
            captionText = "도착지"
            map = naverMap
        }

        // --- segmentsJson 받아서 파싱 ---
        val segmentsJson = intent.getStringExtra("segmentsJson")
        val segments: List<RouteSegment> = if (!segmentsJson.isNullOrEmpty()) {
            val listType = object : TypeToken<List<RouteSegment>>() {}.type
            Gson().fromJson(segmentsJson, listType)
        } else {
            emptyList()
        }

        // === [경로선 및 마커 동적 처리] ===
        // 모든 경유지 좌표 리스트
        val pathCoords = mutableListOf<LatLng>()
        pathCoords.add(startLatLng)

        // 경로 시각화: 구간별로 폴리라인/마커/색상
        var prevLatLng = startLatLng

        segments.forEachIndexed { idx, seg ->
            // 각 구간의 lat/lng가 있으면 마커/좌표 추가
            if (seg.lat != null && seg.lng != null) {
                val point = LatLng(seg.lat, seg.lng)
                pathCoords.add(point)
                // 구간 타입별 마커
                Marker().apply {
                    position = point
                    captionText = seg.stationName ?: seg.type
                    // 타입별 색상 (예시)
                    iconTintColor = when (seg.type) {
                        "도보" -> android.graphics.Color.DKGRAY
                        "일반" -> android.graphics.Color.parseColor("#4CAF50") // 초록
                        "직행" -> android.graphics.Color.parseColor("#F44336") // 빨강
                        "하차" -> android.graphics.Color.LTGRAY
                        else -> seg.color
                    }
                    map = naverMap
                }
            }
            // === 폴리라인(구간별) ===
            // 다음 좌표를 미리 계산
            val nextLatLng: LatLng? = when {
                idx < segments.lastIndex && segments[idx + 1].lat != null && segments[idx + 1].lng != null ->
                    LatLng(segments[idx + 1].lat!!, segments[idx + 1].lng!!)
                idx == segments.lastIndex -> endLatLng
                else -> null
            }
            // 폴리라인: prevLatLng ~ nextLatLng 구간
            if (nextLatLng != null) {
                PathOverlay().apply {
                    coords = listOf(prevLatLng, nextLatLng)
                    color = when (seg.type) {
                        "도보" -> android.graphics.Color.parseColor("#757575")
                        "일반" -> android.graphics.Color.parseColor("#4CAF50")
                        "직행" -> android.graphics.Color.parseColor("#F44336")
                        else -> seg.color
                    }
                    width = 8
                    map = naverMap
                }
                prevLatLng = nextLatLng
            }
        }

        // 마지막에 도착지점이 안들어가 있으면 추가
        if (pathCoords.last() != endLatLng) {
            pathCoords.add(endLatLng)
        }

        // --- 카메라: 전체 지점 포함 ---
        val boundsBuilder = LatLngBounds.Builder()
            .include(startLatLng)
            .include(endLatLng)
        segments.forEach {
            if (it.lat != null && it.lng != null)
                boundsBuilder.include(LatLng(it.lat, it.lng))
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
