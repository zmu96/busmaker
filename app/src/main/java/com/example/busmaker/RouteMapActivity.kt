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

        // 출발지 마커
        Marker().apply {
            position = startLatLng
            captionText = "출발지"
            map = naverMap
        }
        // 도착지 마커
        Marker().apply {
            position = endLatLng
            captionText = "도착지"
            map = naverMap
        }

        // ★★ 중간 정류장 마커 추가 ★★
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

        // 전체 경로 좌표 리스트 생성 (출발지 + 경유지 + 도착지)
        val pathCoords = mutableListOf<LatLng>()
        pathCoords.add(startLatLng) // 출발지 먼저 추가

        // 경유지 마커 및 좌표 추가 (segments 기반)
        segments.forEach { seg ->
            if (seg.lat != null && seg.lng != null) {
                val point = LatLng(seg.lat, seg.lng)
                pathCoords.add(point)
                // 이미 위에서 마커 찍었으니 여기선 생략 가능
            }
        }

        // 도착지 좌표 추가 (만약 경유지 중에 도착지가 없을 경우)
        if (pathCoords.last() != endLatLng) {
            pathCoords.add(endLatLng)
        }

        // 하나의 PathOverlay로 경로 그리기 (출발지-경유지-도착지 전부 연결)
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
        // ★ 중간 정류장도 카메라 영역에 포함
        if (midLatArr != null && midLngArr != null && midLatArr.size == midLngArr.size) {
            for (i in midLatArr.indices) {
                boundsBuilder.include(LatLng(midLatArr[i], midLngArr[i]))
            }
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
