package com.example.busmaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.busmaker.data.api.NaverApiClient
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var etStart: EditText
    private lateinit var etEnd: EditText
    private lateinit var btnSearch: Button
    private lateinit var recentStartContainer: LinearLayout
    private lateinit var recentEndContainer: LinearLayout

    private val PREFS_NAME = "recent_places"
    private val KEY_STARTS = "recent_starts"
    private val KEY_ENDS = "recent_ends"
    private val MAX_RECENT = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etStart = findViewById(R.id.et_start)
        etEnd = findViewById(R.id.et_end)
        btnSearch = findViewById(R.id.btn_search)
        recentStartContainer = findViewById(R.id.recentStartContainer)
        recentEndContainer = findViewById(R.id.recentEndContainer)

        // 앱 실행 시 SharedPreferences에서 최근 기록 불러오기
        val recentStartList = loadRecentPlaces(KEY_STARTS)
        val recentEndList = loadRecentPlaces(KEY_ENDS)

        // 최근 기록 버튼 생성
        createRecentButtons(recentStartList, recentStartContainer, etStart)
        createRecentButtons(recentEndList, recentEndContainer, etEnd)

        btnSearch.setOnClickListener {
            val startSearchTerm = etStart.text.toString().trim()
            val endSearchTerm = etEnd.text.toString().trim()

            if (startSearchTerm.isNotBlank() && endSearchTerm.isNotBlank()) {
                // 검색 성공 시 최근 기록 저장
                saveRecentPlace(KEY_STARTS, startSearchTerm)
                saveRecentPlace(KEY_ENDS, endSearchTerm)

                // 최근 기록 다시 불러와서 버튼 갱신
                val updatedStartList = loadRecentPlaces(KEY_STARTS)
                val updatedEndList = loadRecentPlaces(KEY_ENDS)
                createRecentButtons(updatedStartList, recentStartContainer, etStart)
                createRecentButtons(updatedEndList, recentEndContainer, etEnd)

                // 실제 검색 API 호출 함수 실행
                searchPlaces(startSearchTerm, endSearchTerm)
            } else {
                Toast.makeText(this, "출발지와 도착지를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createRecentButtons(
        items: List<String>,
        container: LinearLayout,
        targetEditText: EditText
    ) {
        container.removeAllViews()
        for (item in items) {
            val button = Button(this).apply {
                text = item
                textSize = 14f
                setPadding(12, 6, 12, 6)
                setOnClickListener {
                    targetEditText.setText(item)
                }
            }
            container.addView(button)
        }
    }

    private fun saveRecentPlace(key: String, place: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val list = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // 중복 제거 및 최대 개수 제한
        if (list.contains(place)) {
            list.remove(place)
        }
        list.add(place)
        while (list.size > MAX_RECENT) {
            // 오래된 것부터 제거 (순서 보장 안되므로 Set -> List 변환 후)
            val listOrdered = list.toList()
            val toRemove = listOrdered.firstOrNull()
            if (toRemove != null) list.remove(toRemove)
        }
        prefs.edit().putStringSet(key, list).apply()
    }

    private fun loadRecentPlaces(key: String): List<String> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getStringSet(key, emptySet())?.toList()?.reversed() ?: emptyList()
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
                    val startMapXLong = startItem.mapx.toLongOrNull()
                    val startMapYLong = startItem.mapy.toLongOrNull()
                    val endMapXLong = endItem.mapx.toLongOrNull()
                    val endMapYLong = endItem.mapy.toLongOrNull()

                    if (startMapXLong != null && startMapYLong != null && endMapXLong != null && endMapYLong != null) {
                        val calculatedStartLng = startMapXLong / 10000000.0
                        val calculatedStartLat = startMapYLong / 10000000.0
                        val calculatedEndLng = endMapXLong / 10000000.0
                        val calculatedEndLat = endMapYLong / 10000000.0

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
                Log.e("SearchActivity", "API 호출 또는 처리 오류: ${e.message}", e)
                Toast.makeText(this@SearchActivity, "오류가 발생했습니다: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
