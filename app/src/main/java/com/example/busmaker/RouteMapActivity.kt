package com.example.busmaker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.geometry.LatLngBounds


class RouteMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var startLatLng: LatLng
    private lateinit var endLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_map)

        // 전달받은 좌표
        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        startLatLng = LatLng(startLat, startLng)
        endLatLng = LatLng(endLat, endLng)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.naver_map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.map_container, it).commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        // 마커 추가
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

        // 경로 선
        PathOverlay().apply {
            coords = listOf(startLatLng, endLatLng)
            map = naverMap
        }

        // 카메라 이동
        val cameraUpdate = CameraUpdate.fitBounds(
            LatLngBounds.Builder().include(startLatLng).include(endLatLng).build(),
            100 // 패딩
        )
        naverMap.moveCamera(cameraUpdate)
    }
}
