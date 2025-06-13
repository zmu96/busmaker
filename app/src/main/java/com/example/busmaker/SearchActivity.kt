package com.example.busmaker

import android.content.Intent
import android.os.Bundle
import android.util.Log // 로그 추가
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.busmaker.data.api.NaverApiClient
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var etStart: EditText
    private lateinit var etEnd: EditText
    private lateinit var btnSearch: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etStart = findViewById(R.id.et_start)
        etEnd = findViewById(R.id.et_end)
        btnSearch = findViewById(R.id.btn_search)

        btnSearch.setOnClickListener {
            val startSearchTerm = etStart.text.toString() // 변수명 변경 (start -> startSearchTerm)
            val endSearchTerm = etEnd.text.toString()     // 변수명 변경 (end -> endSearchTerm)

            if (startSearchTerm.isNotBlank() && endSearchTerm.isNotBlank()) {
                searchPlaces(startSearchTerm, endSearchTerm)
            } else {
                Toast.makeText(this, "출발지와 도착지를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchPlaces(startQuery: String, endQuery: String) {
        lifecycleScope.launch {
            try {
                // 출발지 검색
                val startNaverResponse = NaverApiClient.api.searchPlace(startQuery)
                val startItem = startNaverResponse.items.firstOrNull()

                // 도착지 검색
                val endNaverResponse = NaverApiClient.api.searchPlace(endQuery)
                val endItem = endNaverResponse.items.firstOrNull()

                if (startItem != null && endItem != null) {
                    // 출발지 좌표 계산
                    val startMapXStr = startItem.mapx
                    val startMapYStr = startItem.mapy
                    var calculatedStartLat: Double? = null
                    var calculatedStartLng: Double? = null

                    Log.d("SearchActivity", "출발지 API 응답 - mapx: $startMapXStr, mapy: $startMapYStr")
                    try {
                        val startMapXLong = startMapXStr.toLongOrNull()
                        val startMapYLong = startMapYStr.toLongOrNull()
                        if (startMapXLong != null && startMapYLong != null) {
                            calculatedStartLng = startMapXLong / 10000000.0
                            calculatedStartLat = startMapYLong / 10000000.0
                            Log.d("SearchActivity", "출발지 계산된 좌표 - Lat: $calculatedStartLat, Lng: $calculatedStartLng")
                        } else {
                            Log.e("SearchActivity", "출발지 mapx 또는 mapy를 Long으로 변환 실패")
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("SearchActivity", "출발지 좌표 문자열 변환 오류", e)
                    }

                    // 도착지 좌표 계산
                    val endMapXStr = endItem.mapx
                    val endMapYStr = endItem.mapy
                    var calculatedEndLat: Double? = null
                    var calculatedEndLng: Double? = null

                    Log.d("SearchActivity", "도착지 API 응답 - mapx: $endMapXStr, mapy: $endMapYStr")
                    try {
                        val endMapXLong = endMapXStr.toLongOrNull()
                        val endMapYLong = endMapYStr.toLongOrNull()
                        if (endMapXLong != null && endMapYLong != null) {
                            calculatedEndLng = endMapXLong / 10000000.0
                            calculatedEndLat = endMapYLong / 10000000.0
                            Log.d("SearchActivity", "도착지 계산된 좌표 - Lat: $calculatedEndLat, Lng: $calculatedEndLng")
                        } else {
                            Log.e("SearchActivity", "도착지 mapx 또는 mapy를 Long으로 변환 실패")
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("SearchActivity", "도착지 좌표 문자열 변환 오류", e)
                    }

                    // 두 좌표가 모두 성공적으로 계산되었는지 확인
                    if (calculatedStartLat != null && calculatedStartLng != null && calculatedEndLat != null && calculatedEndLng != null) {
                        val intent = Intent(this@SearchActivity, RouteListActivity::class.java).apply {
                            putExtra("startLat", calculatedStartLat)
                            putExtra("startLng", calculatedStartLng)
                            putExtra("endLat", calculatedEndLat)
                            putExtra("endLng", calculatedEndLng)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@SearchActivity, "장소의 좌표를 변환하는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    var errorMessage = "장소를 찾을 수 없습니다."
                    if (startItem == null) errorMessage += " (출발지)"
                    if (endItem == null) errorMessage += " (도착지)"
                    Toast.makeText(this@SearchActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SearchActivity", "API 호출 또는 처리 오류: ${e.message}", e) // 스택 트레이스도 로깅
                Toast.makeText(this@SearchActivity, "오류가 발생했습니다: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}