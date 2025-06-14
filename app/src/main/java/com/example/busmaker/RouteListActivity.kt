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
import com.example.busmaker.data.model.RouteSegment
import kotlinx.coroutines.launch

class RouteListActivity : AppCompatActivity() {

    private val serviceKey = "VIyokumz54z0pgrPhQPYtDCSTJjSgC9K0yTZPT8O3T7IAvHghfXxcof7hZT7RYiG77D83lUKqeciZMuaXYfKRg=="
    private lateinit var routeListAdapter: RouteListAdapter
    private val routeInfoList = mutableListOf<RouteInformation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_list)

        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        Log.d("RouteListActivity", "Received Coordinates - Start: ($startLat, $startLng), End: ($endLat, $endLng)")

        val rv = findViewById<RecyclerView>(R.id.rvRouteList)
        rv.layoutManager = LinearLayoutManager(this)
        routeListAdapter = RouteListAdapter(routeInfoList) { info ->
            val intent = Intent(this@RouteListActivity, RouteMapActivity::class.java).apply {
                putExtra("startLat", startLat)
                putExtra("startLng", startLng)
                putExtra("endLat", endLat)
                putExtra("endLng", endLng)
                // 필요하다면 info 객체도 추가
            }
            startActivity(intent)
        }
        rv.adapter = routeListAdapter

        findNearbyStationsAndRoutes(startLat, startLng, endLat, endLng)
    }

    private fun findNearbyStationsAndRoutes(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        lifecycleScope.launch {
            try {
                // 1. 출발지 근처 정류소 여러 개 조회
                val startStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = startLat,
                    longitude = startLng
                )
                // 2. 도착지 근처 정류소 여러 개 조회
                val endStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = endLat,
                    longitude = endLng
                )

                if (startStationApiResponse.isSuccessful && endStationApiResponse.isSuccessful) {
                    val startActualResponse = startStationApiResponse.body()
                    val endActualResponse = endStationApiResponse.body()

                    val startStations: List<StationItemDetail> =
                        startActualResponse?.responseData?.body?.itemsContainer?.stationList?.take(3) ?: emptyList()
                    val endStations: List<StationItemDetail> =
                        endActualResponse?.responseData?.body?.itemsContainer?.stationList?.take(3) ?: emptyList()
                    Log.d("RouteListActivity", "startStations: $startStations")
                    Log.d("RouteListActivity", "endStations: $endStations")

                    val newRouteInfoList = mutableListOf<RouteInformation>()

                    // 출발지 근처 정류소별로 노선 탐색 (★cityCode, nodeId를 각각 적용!)
                    for (startStation in startStations) {
                        val cityCodeStr = startStation.cityCode
                        val cityCode = cityCodeStr?.toIntOrNull() ?: continue   // null이거나 Int 변환 실패 시 skip!
                        val startStationId = startStation.nodeId ?: continue
                        val startStationName = startStation.nodeName ?: continue

                        Log.d("출발정류소", "$startStationName ($startStationId, $cityCode)")

                        // 1. 출발 정류소의 경유 노선 목록 조회
                        val startRoutesApiResponse = BusApiClient.service.getThroughRoutesByStation(
                            serviceKey = serviceKey,
                            cityCode = cityCode,
                            nodeId = startStationId
                        )

                        if (startRoutesApiResponse.isSuccessful) {
                            val startRoutesActualResponse = startRoutesApiResponse.body()
                            val startRouteItems: List<RouteItemDetail>? =
                                startRoutesActualResponse?.responseData?.body?.itemsContainer?.routeList

                            if (startRouteItems != null) {
                                for (route in startRouteItems) {
                                    val routeId = route.routeId
                                    val busNumber = route.routeNo
                                    val routeType = route.routeType

                                    if (routeId != null && busNumber != null) {
                                        val stationsOnRouteApiResponse = BusApiClient.service.getStationsByRouteId(
                                            serviceKey = serviceKey,
                                            cityCode = cityCode, // ★동적으로!
                                            routeId = routeId
                                        )

                                        if (stationsOnRouteApiResponse.isSuccessful) {
                                            val stationsOnRouteActualResponse = stationsOnRouteApiResponse.body()
                                            val stations: List<RouteStationItemDetail>? =
                                                stationsOnRouteActualResponse?.responseData?.body?.itemsContainer?.stationList

                                            if (stations != null) {
                                                // ★ 출발 후보 중 실제 노선에 포함된 nodeId (cityCode까지 일치하는 경우만!)
                                                val routeNodeIdList = stations.mapNotNull { it.nodeId }

                                                val matchedStart = startStations.firstOrNull {
                                                    it.cityCode?.toIntOrNull() == cityCode && routeNodeIdList.contains(it.nodeId)
                                                }
                                                val matchedEnd = endStations.firstOrNull {
                                                    it.cityCode?.toIntOrNull() == cityCode && routeNodeIdList.contains(it.nodeId)
                                                }


                                                val startIdx = if (matchedStart != null)
                                                    stations.indexOfFirst { it.nodeId == matchedStart.nodeId }
                                                else -1
                                                val endIdx = if (matchedEnd != null)
                                                    stations.indexOfFirst { it.nodeId == matchedEnd.nodeId }
                                                else -1

                                                val endStationId = if (endIdx != -1) stations[endIdx].nodeId else null
                                                val endStationName = if (endIdx != -1) stations[endIdx].nodeName else null

                                                Log.d("경로탐색", "routeId=$routeId ($busNumber), 정류장 수=${stations.size}")
                                                Log.d("경로탐색", "startIdx=$startIdx, endIdx=$endIdx")
                                                if (startIdx != -1) {
                                                    Log.d("경로탐색", "startStation=${stations[startIdx]}")
                                                }
                                                if (endIdx != -1) {
                                                    Log.d("경로탐색", "endStation=${stations[endIdx]}")
                                                }

                                                // 순서상 출발정류장이 먼저 등장해야 함
                                                if (startIdx != -1 && endIdx != -1 && startIdx < endIdx && endStationId != null && endStationName != null) {
                                                    val stationCount = endIdx - startIdx
                                                    val totalMinutes = stationCount * 2
                                                    val hours = totalMinutes / 60
                                                    val minutes = totalMinutes % 60
                                                    val totalTimeStr = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"

                                                    val busTypeDisplay = when (routeType?.trim()) {
                                                        "일반버스", "간선버스", "지선버스", "마을버스", "일반", "간선" -> "일반"
                                                        "좌석버스", "직행좌석버스", "급행버스", "광역급행버스", "직행", "좌석" -> "직행"
                                                        else -> routeType ?: "버스"
                                                    }
                                                    val busColor = when (busTypeDisplay) {
                                                        "일반" -> android.graphics.Color.parseColor("#4CAF50")
                                                        "직행" -> android.graphics.Color.parseColor("#F44336")
                                                        else -> android.graphics.Color.parseColor("#2196F3")
                                                    }

                                                    val label = if (newRouteInfoList.isEmpty()) "추천 경로" else "경로 ${newRouteInfoList.size + 1}"

                                                    // segment(구간) 정보 추가
                                                    val segmentsList = mutableListOf<RouteSegment>()
                                                    segmentsList.add(
                                                        RouteSegment(
                                                            type = busTypeDisplay,
                                                            summary = "$busTypeDisplay | ${stations[startIdx].nodeName} 승차",
                                                            detail = "$busNumber | ${totalMinutes}분 (${stationCount}정류장)",
                                                            color = busColor
                                                        )
                                                    )
                                                    segmentsList.add(
                                                        RouteSegment(
                                                            type = "하차",
                                                            summary = "$endStationName 하차",
                                                            detail = "",
                                                            color = android.graphics.Color.parseColor("#BBBBBB")
                                                        )
                                                    )

                                                    val timeRangeAndFareStr = "버스 $busNumber (${stations[startIdx].nodeName} → $endStationName) | 요금 정보 없음"

                                                    newRouteInfoList.add(
                                                        RouteInformation(
                                                            label = label,
                                                            totalTime = totalTimeStr,
                                                            timeRangeAndFare = timeRangeAndFareStr,
                                                            segments = segmentsList,
                                                            pinFixed = false
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    runOnUiThread {
                        Log.d("RouteListActivity", "UI 업데이트 전 newRouteInfoList 크기: ${newRouteInfoList.size}")
                        routeInfoList.clear()
                        routeInfoList.addAll(newRouteInfoList.distinctBy { it.timeRangeAndFare })
                        routeListAdapter.notifyDataSetChanged()

                        if (routeInfoList.isEmpty()) {
                            Toast.makeText(this@RouteListActivity, "두 정류소를 모두 지나는 노선이 없습니다.", Toast.LENGTH_SHORT).show()
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
