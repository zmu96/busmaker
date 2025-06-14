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
        // LocationSource 연결 및 현재 위치 버튼 활성화
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true

        // 위치 추적 모드 설정 (현재 위치 따라가기)
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

        // 출발지~도착지 경로선
        PathOverlay().apply {
            coords = listOf(startLatLng, endLatLng)
            map = naverMap
        }

        // 카메라 위치를 출발지~도착지 모두 보이도록 이동
        val bounds = LatLngBounds.Builder()
            .include(startLatLng)
            .include(endLatLng)
            .build()

        val cameraUpdate = CameraUpdate.fitBounds(bounds, 100)
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
