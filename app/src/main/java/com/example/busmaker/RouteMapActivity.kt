package com.example.busmaker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource

class RouteMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var startLatLng: LatLng
    private lateinit var endLatLng: LatLng

    // 위치 소스 선언 (퍼미션 요청 코드 1000)
    private lateinit var locationSource: FusedLocationSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_map)

        // 전달받은 좌표 (없으면 기본 서울 좌표)
        val startLat = intent.getDoubleExtra("startLat", 37.5665) // 서울 시청 위도
        val startLng = intent.getDoubleExtra("startLng", 126.9780) // 서울 시청 경도
        val endLat = intent.getDoubleExtra("endLat", 37.5665)
        val endLng = intent.getDoubleExtra("endLng", 126.9780)

        startLatLng = LatLng(startLat, startLng)
        endLatLng = LatLng(endLat, endLng)

        // LocationSource 초기화 (퍼미션 요청 코드 1000)
        locationSource = FusedLocationSource(this, 1000)

        // 길찾기 버튼 연결
        val btnSearchRoute = findViewById<ImageButton>(R.id.btnSearchRoute)
        btnSearchRoute.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // MapFragment에서 비동기 지도 준비 호출
        val mapFragment = supportFragmentManager.findFragmentById(R.id.naver_map_fragment) as MapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        // 위치 소스, 현재 위치 버튼, 트래킹 모드
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        // --- 출발지/도착지 LatLng ---
        // (이미 위에서 받아온 startLatLng, endLatLng 사용)
        // val startLatLng = LatLng(startLat, startLng)
        // val endLatLng = LatLng(endLat, endLng)

        // --- 정류장 좌표 ---
        val busBoardingLat = intent.getDoubleExtra("busBoardingLat", 0.0)
        val busBoardingLng = intent.getDoubleExtra("busBoardingLng", 0.0)
        val busBoardingName = intent.getStringExtra("busBoardingName") ?: ""
        val busAlightLat = intent.getDoubleExtra("busAlightLat", 0.0)
        val busAlightLng = intent.getDoubleExtra("busAlightLng", 0.0)
        val busAlightName = intent.getStringExtra("busAlightName") ?: ""

        // --- 마커(출발/도착) ---
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

        // --- 승차 정류장 마커 ---
        if (busBoardingLat != 0.0 && busBoardingLng != 0.0) {
            Marker().apply {
                position = LatLng(busBoardingLat, busBoardingLng)
                captionText = busBoardingName.ifBlank { "승차 정류장" }
                iconTintColor = android.graphics.Color.BLUE
                map = naverMap
            }
        }

        // --- 하차 정류장 마커 ---
        if (busAlightLat != 0.0 && busAlightLng != 0.0) {
            Marker().apply {
                position = LatLng(busAlightLat, busAlightLng)
                captionText = busAlightName.ifBlank { "하차 정류장" }
                iconTintColor = android.graphics.Color.RED
                map = naverMap
            }
        }

        // --- [NEW] 경로선(폴리라인) : 출발→승차→하차→도착 순서로 꺾어서 그리기 ---
        val pathCoords = mutableListOf<LatLng>()
        pathCoords.add(startLatLng)
        if (busBoardingLat != 0.0 && busBoardingLng != 0.0) {
            pathCoords.add(LatLng(busBoardingLat, busBoardingLng))
        }
        if (busAlightLat != 0.0 && busAlightLng != 0.0) {
            pathCoords.add(LatLng(busAlightLat, busAlightLng))
        }
        pathCoords.add(endLatLng)

        PathOverlay().apply {
            coords = pathCoords
            color = android.graphics.Color.parseColor("#2196F3") // 원하는 색상
            width = 8
            map = naverMap
        }

        // --- 카메라: 4개 지점 모두 보이도록 ---
        val bounds = LatLngBounds.Builder()
            .include(startLatLng)
            .include(endLatLng)
            .apply {
                if (busBoardingLat != 0.0 && busBoardingLng != 0.0) include(LatLng(busBoardingLat, busBoardingLng))
                if (busAlightLat != 0.0 && busAlightLng != 0.0) include(LatLng(busAlightLat, busAlightLng))
            }
            .build()
        val cameraUpdate = com.naver.maps.map.CameraUpdate.fitBounds(bounds, 100)
        naverMap.moveCamera(cameraUpdate)
    }



    // 퍼미션 결과를 LocationSource에 전달
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (::locationSource.isInitialized) {
            locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
