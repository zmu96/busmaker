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
import com.example.busmaker.utils.WalkingTimeEstimator
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

    private fun findNearbyStationsAndRoutes(
        userStartLat: Double, // 출발지 위도
        userStartLng: Double, // 출발지 경도
        userEndLat: Double,   // 도착지 위도
        userEndLng: Double    // 도착지 경도
    ) {
        lifecycleScope.launch {
            try {
                // 1. 출발지·도착지 근처 정류소 여러 개 조회
                val startStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = userStartLat,
                    longitude = userStartLng
                )
                val endStationApiResponse = BusApiClient.service.getNearbyStations(
                    serviceKey = serviceKey,
                    latitude = userEndLat,
                    longitude = userEndLng
                )

                if (startStationApiResponse.isSuccessful && endStationApiResponse.isSuccessful) {
                    val startActualResponse = startStationApiResponse.body()
                    val endActualResponse = endStationApiResponse.body()

                    // 주변 정류소(최대 3개)
                    val startCandidateStations: List<StationItemDetail> =
                        startActualResponse?.responseData?.body?.itemsContainer?.stationList?.take(3) ?: emptyList()
                    val endCandidateStations: List<StationItemDetail> =
                        endActualResponse?.responseData?.body?.itemsContainer?.stationList?.take(3) ?: emptyList()

                    val newRouteInfoList = mutableListOf<RouteInformation>()

                    // nodeId로 StationItemDetail에서 좌표 찾는 함수
                    fun findLatLngByNodeId(nodeId: String?, candidates: List<StationItemDetail>): Pair<Double?, Double?> {
                        val station = candidates.firstOrNull { it.nodeId == nodeId }
                        val lat = station?.gpsLati
                        val lng = station?.gpsLong
                        return lat to lng
                    }

                    // 출발지 근처 정류소별로 노선 탐색
                    for (actualStartBusStop in startCandidateStations) {
                        val cityCodeStr = actualStartBusStop.cityCode
                        val cityCode = cityCodeStr?.toIntOrNull() ?: continue
                        val startStationId = actualStartBusStop.nodeId ?: continue

                        // 1. 해당 정류소를 경유하는 노선 조회
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
                                            cityCode = cityCode,
                                            routeId = routeId
                                        )

                                        if (stationsOnRouteApiResponse.isSuccessful) {
                                            val stationsOnRouteActualResponse = stationsOnRouteApiResponse.body()
                                            val stations: List<RouteStationItemDetail>? =
                                                stationsOnRouteActualResponse?.responseData?.body?.itemsContainer?.stationList

                                            if (stations != null) {
                                                val routeNodeIdList = stations.mapNotNull { it.nodeId }
                                                val matchedStart = startCandidateStations.firstOrNull {
                                                    it.cityCode?.toIntOrNull() == cityCode && routeNodeIdList.contains(it.nodeId)
                                                }
                                                val matchedEnd = endCandidateStations.firstOrNull {
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

                                                // ★★ 직통 경로 조건: 출발정류장 인덱스 < 도착정류장 인덱스
                                                if (startIdx != -1 && endIdx != -1 && startIdx < endIdx && endStationId != null && endStationName != null) {
                                                    val identifiedStartStation = stations[startIdx]
                                                    val identifiedEndStation = stations[endIdx]

                                                    // 1. 출발지 → 실제 탑승 정류장 도보 시간
                                                    val (startBusStopLat, startBusStopLng) = findLatLngByNodeId(
                                                        identifiedStartStation.nodeId,
                                                        startCandidateStations
                                                    )
                                                    val walkTimeToStartStopMin =
                                                        if (startBusStopLat != null && startBusStopLng != null)
                                                            WalkingTimeEstimator.estimateWalkingTimeBetweenCoordinates(
                                                                userStartLat, userStartLng,
                                                                startBusStopLat, startBusStopLng
                                                            )
                                                        else 0

                                                    // 2. 버스 이동 시간 (기존: 정류장 당 2분)
                                                    val stationCount = endIdx - startIdx
                                                    val busTravelTimeMin = stationCount * 2

                                                    // 3. 실제 하차 정류장 → 도착지 도보 시간
                                                    val (endBusStopLat, endBusStopLng) = findLatLngByNodeId(
                                                        identifiedEndStation.nodeId,
                                                        endCandidateStations
                                                    )
                                                    val walkTimeFromEndStopMin =
                                                        if (endBusStopLat != null && endBusStopLng != null)
                                                            WalkingTimeEstimator.estimateWalkingTimeBetweenCoordinates(
                                                                endBusStopLat, endBusStopLng,
                                                                userEndLat, userEndLng
                                                            )
                                                        else 0

                                                    // 4. 전체 소요 시간 합산
                                                    val numericTotalTimeMin = walkTimeToStartStopMin + busTravelTimeMin + walkTimeFromEndStopMin
                                                    val totalHours = numericTotalTimeMin / 60
                                                    val totalRemainderMinutes = numericTotalTimeMin % 60
                                                    val totalTimeStrForDisplay = if (totalHours > 0) {
                                                        "${totalHours}시간 ${totalRemainderMinutes}분"
                                                    } else {
                                                        "${totalRemainderMinutes}분"
                                                    }

                                                    // UI용 추가 정보 (기존 방식 그대로)
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

                                                    val segmentsList = mutableListOf<RouteSegment>()
                                                    segmentsList.add(
                                                        RouteSegment(
                                                            type = "도보",
                                                            summary = "출발지 → 승차 정류장 도보",
                                                            detail = "${walkTimeToStartStopMin}분",
                                                            color = android.graphics.Color.parseColor("#888888")
                                                        )
                                                    )
                                                    segmentsList.add(
                                                        RouteSegment(
                                                            type = busTypeDisplay,
                                                            summary = "$busTypeDisplay | ${stations[startIdx].nodeName} 승차",
                                                            detail = "$busNumber | ${busTravelTimeMin}분 (${stationCount}정류장)",
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
                                                    segmentsList.add(
                                                        RouteSegment(
                                                            type = "도보",
                                                            summary = "하차 정류장 → 도착지 도보",
                                                            detail = "${walkTimeFromEndStopMin}분",
                                                            color = android.graphics.Color.parseColor("#888888")
                                                        )
                                                    )

                                                    val timeRangeAndFareStr =
                                                        "버스 $busNumber (${stations[startIdx].nodeName} → $endStationName) | 요금 1450원"

                                                    newRouteInfoList.add(
                                                        RouteInformation(
                                                            label = label,
                                                            totalTime = totalTimeStrForDisplay,
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
