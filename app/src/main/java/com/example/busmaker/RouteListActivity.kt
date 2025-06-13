package com.example.busmaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busmaker.adapter.RouteListAdapter
import com.example.busmaker.data.api.BusApiClient
import com.example.busmaker.data.model.RouteInformation
import com.example.busmaker.data.model.StationItemDetail
import com.example.busmaker.data.model.RouteItemDetail
import com.example.busmaker.data.model.RouteStationItemDetail
import kotlinx.coroutines.launch

class RouteListActivity : AppCompatActivity() {

    // RouteListActivity.kt
// ...
// 일반 인증키 (Decoding) 값을 사용합니다.
    private val serviceKey = "VIyokumz54z0pgrPhQPYtDCSTJjSgC9K0yTZPT8O3T7IAvHghfXxcof7hZT7RYiG77D83lUKqeciZMuaXYfKRg=="
    // ...


    // RecyclerView 어댑터를 멤버 변수로 선언하여 나중에 업데이트할 수 있도록 합니다.
    private lateinit var routeListAdapter: RouteListAdapter
    private val routeInfoList = mutableListOf<RouteInformation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_list)

        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        Log.d("RouteListActivity", "Received Coordinates - Start: ($startLat, $startLng), End: ($endLat, $endLng)") // 로그 추가

        val rv = findViewById<RecyclerView>(R.id.rv_routes)
        rv.layoutManager = LinearLayoutManager(this)
        // 어댑터 초기화 (빈 리스트로 시작)
        routeListAdapter = RouteListAdapter(routeInfoList) { info ->
            // 경로 클릭 시 RouteMapActivity 이동
            val intent = Intent(this@RouteListActivity, RouteMapActivity::class.java).apply {
                putExtra("startLat", startLat)
                putExtra("startLng", startLng)
                putExtra("endLat", endLat)
                putExtra("endLng", endLng)
                // 필요하다면 선택된 경로 정보(info)도 전달할 수 있습니다.
                // 예: putExtra("ROUTE_SUMMARY", info.summary)
            }
            startActivity(intent)
        }
        rv.adapter = routeListAdapter

        findNearbyStationsAndRoutes(startLat, startLng, endLat, endLng)
    }

    private fun findNearbyStationsAndRoutes(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        lifecycleScope.launch {
            try {
                // 1. 출발지 근처 정류소 조회
                val startStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = startLat,
                    longitude = startLng
                )
                // 2. 도착지 근처 정류소 조회
                val endStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = endLat,
                    longitude = endLng
                )

                if (startStationApiResponse.isSuccessful && endStationApiResponse.isSuccessful) {
                    val startActualResponse = startStationApiResponse.body()
                    val endActualResponse = endStationApiResponse.body()

                    // 실제 데이터 모델 구조에 맞춰 접근 (StationItemDetail의 리스트를 가져온다고 가정)
                    // ?. 연산자는 null 안전 호출을 위해 중요
                    val startStation: StationItemDetail? = startActualResponse?.responseData?.body?.itemsContainer?.stationList?.firstOrNull()
                    val endStation: StationItemDetail? = endActualResponse?.responseData?.body?.itemsContainer?.stationList?.firstOrNull()

                    if (startStation?.nodeId != null && startStation.nodeName != null &&
                        endStation?.nodeId != null && endStation.nodeName != null) {

                        val startStationId = startStation.nodeId
                        val endStationId = endStation.nodeId

                        Log.d("출발정류소", "${startStation.nodeName} (${startStationId})")
                        Log.d("도착정류소", "${endStation.nodeName} (${endStationId})")

                        val cityCode = 25 // 도시 코드는 필요에 따라 동적으로 변경

                        // 3. 출발 정류소 경유 노선 조회
                        val startRoutesApiResponse = BusApiClient.service.getThroughRoutesByStation(
                            serviceKey = serviceKey,
                            cityCode = cityCode,
                            nodeId = startStationId
                        )
                        // 4. 도착 정류소 경유 노선 조회
                        val endRoutesApiResponse = BusApiClient.service.getThroughRoutesByStation(
                            serviceKey = serviceKey,
                            cityCode = cityCode,
                            nodeId = endStationId
                        )

                        if (startRoutesApiResponse.isSuccessful && endRoutesApiResponse.isSuccessful) {
                            val startRoutesActualResponse = startRoutesApiResponse.body()
                            val endRoutesActualResponse = endRoutesApiResponse.body()

                            // RouteItemDetail의 리스트를 가져온다고 가정
                            val startRouteItems: List<RouteItemDetail>? = startRoutesActualResponse?.responseData?.body?.itemsContainer?.routeList
                            val endRouteItems: List<RouteItemDetail>? = endRoutesActualResponse?.responseData?.body?.itemsContainer?.routeList

                            if (startRouteItems != null && endRouteItems != null) {
                                val commonRoutes = startRouteItems.filter { startRoute ->
                                    endRouteItems.any { endRoute ->
                                        endRoute.routeId == startRoute.routeId && startRoute.routeId != null
                                    }
                                }

                                val newRouteInfoList = mutableListOf<RouteInformation>()

                                if (commonRoutes.isNotEmpty()) {
                                    for (commonRoute in commonRoutes) {
                                        val routeId = commonRoute.routeId
                                        val busNumber = commonRoute.routeNo // 또는 routeName 등 실제 속성명

                                        if (routeId != null && busNumber != null) {
                                            // 5. 공통 노선의 정류소 목록 조회
                                            val stationsOnRouteApiResponse = BusApiClient.service.getStationsByRouteId(
                                                serviceKey = serviceKey,
                                                cityCode = cityCode,
                                                routeId = routeId
                                            )

                                            if (stationsOnRouteApiResponse.isSuccessful) {
                                                val stationsOnRouteActualResponse = stationsOnRouteApiResponse.body()
                                                // RouteStationItemDetail의 리스트를 가져온다고 가정
                                                val stations: List<RouteStationItemDetail>? = stationsOnRouteActualResponse?.responseData?.body?.itemsContainer?.stationList

                                                if (stations != null) {
                                                    val startIdx = stations.indexOfFirst { it.nodeId == startStationId }
                                                    val endIdx = stations.indexOfFirst { it.nodeId == endStationId }

                                                    // 출발지가 도착지보다 먼저 등장해야 정상 구간!
                                                    if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                                                        val stationCount = endIdx - startIdx
                                                        val totalTime = stationCount * 2 // 정류장당 2분(임시)
                                                        val transferCount = 0 // 환승 미구현(단일 노선만)
                                                        val summary = "버스 $busNumber (${stations[startIdx].nodeName} → ${stations[endIdx].nodeName})"

                                                        newRouteInfoList.add(
                                                            RouteInformation(
                                                                totalTime,
                                                                transferCount,
                                                                summary
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 기존 리스트를 갱신하고 어댑터에 반영
                                    runOnUiThread {
                                        routeInfoList.clear()
                                        routeInfoList.addAll(newRouteInfoList)
                                        routeListAdapter.notifyDataSetChanged()

                                        if (newRouteInfoList.isEmpty()) {
                                            Toast.makeText(this@RouteListActivity, "두 정류소를 모두 지나는 노선이 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    runOnUiThread {
                                        Toast.makeText(this@RouteListActivity, "두 정류소를 모두 지나는 노선이 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RouteListActivity, "정류소 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("findNearbyStationsAndRoutes", e.toString())
                runOnUiThread {
                    Toast.makeText(this@RouteListActivity, "API 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
