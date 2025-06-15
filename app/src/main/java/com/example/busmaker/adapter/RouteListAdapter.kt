package com.example.busmaker.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.example.busmaker.R
import com.example.busmaker.data.model.RouteInformation
import android.content.Intent
import com.google.gson.Gson


class RouteListAdapter(
    private val routes: List<RouteInformation>,
    private val onStartNavigation: (RouteInformation) -> Unit
) : RecyclerView.Adapter<RouteListAdapter.RouteViewHolder>() {

    class RouteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvTotalTime: TextView = view.findViewById(R.id.tvTotalTime)
        val tvTimeRangeAndFare: TextView = view.findViewById(R.id.tvTimeRangeAndFare)
        val transportBar: LinearLayout = view.findViewById(R.id.transportBar)
        val ivPin: ImageView = view.findViewById(R.id.ivPin)
        val tvSegment1: TextView = view.findViewById(R.id.tvSegment1)
        val tvSegment2: TextView = view.findViewById(R.id.tvSegment2)
        val tvSegment3: TextView = view.findViewById(R.id.tvSegment3)
        val tvSegment4: TextView = view.findViewById(R.id.tvSegment4)
        val btnStartNavigation: Button = view.findViewById(R.id.btnStartNavigation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun getItemCount(): Int = routes.size

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]

        holder.tvLabel.text = route.label
        holder.tvTotalTime.text = route.totalTime
        holder.tvTimeRangeAndFare.text = route.timeRangeAndFare

        // 핀 표시
        holder.ivPin.visibility = if (route.pinFixed) View.VISIBLE else View.GONE

        // 구간 텍스트(최대 4개)
        val segments = route.segments
        holder.tvSegment1.text = segments.getOrNull(0)?.summary ?: ""
        holder.tvSegment2.text = segments.getOrNull(1)?.detail ?: ""
        holder.tvSegment3.text = segments.getOrNull(2)?.summary ?: ""
        holder.tvSegment4.text = segments.getOrNull(3)?.summary ?: ""

        // ★★★ transportBar: 색상 바 + 구간 텍스트를 함께 표시 ★★★
        holder.transportBar.removeAllViews()
        segments.forEach { seg ->
            // 1. 바+텍스트를 감싸는 LinearLayout(세로)
            val barWithText = LinearLayout(holder.transportBar.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                gravity = android.view.Gravity.CENTER
            }

            // 2. 컬러 바 (상단)
            val colorBar = View(holder.transportBar.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    16 // 바 높이(px) - 필요시 dp 변환
                )
                setBackgroundColor(seg.color)
            }
            barWithText.addView(colorBar)

            // 3. 구간 텍스트 (하단)
            val labelText = when {
                seg.type == "도보" -> "도보(${seg.detail})"
                seg.type == "하차" -> "하차"
                seg.type == "일반" || seg.type == "직행" || seg.type == "버스" -> {
                    // "2분" 혹은 "8분" 등만 추출
                    // seg.detail이 "68 | 2분 (1정류장)" 이런 식이면 시간만 뽑기
                    // detail에서 "분" 앞 숫자만 추출
                    val minuteRegex = Regex("(\\d+)분")
                    val match = minuteRegex.find(seg.detail)
                    val timeStr = match?.value ?: ""
                    "버스($timeStr)"
                }
                else -> seg.type
            }
            val labelView = TextView(holder.transportBar.context).apply {
                text = labelText
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(Color.DKGRAY)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 4, 0, 0) // 위 여백
                // 필요시 bold/배경 등 조정 가능
            }
            barWithText.addView(labelView)

            // transportBar(가로) 안에 barWithText(세로)를 추가
            holder.transportBar.addView(barWithText)
        }

        // 안내시작 버튼 클릭 리스너
        holder.btnStartNavigation.setOnClickListener {
            // 디버깅 로그 (생략 가능)
            val boardingSegment = route.segments.find { it.type != "도보" && it.type != "하차" }
            val alightSegment = route.segments.find { it.type == "하차" }
            android.util.Log.d(
                "RouteDebug",
                "start=(${route.startLat},${route.startLng}), end=(${route.endLat},${route.endLng}), " +
                        "boarding=(${boardingSegment?.lat},${boardingSegment?.lng},${boardingSegment?.stationName}), " +
                        "alight=(${alightSegment?.lat},${alightSegment?.lng},${alightSegment?.stationName})"
            )

            val context = holder.itemView.context
            val segmentsJson = Gson().toJson(route.segments) // 전체 구간을 JSON으로!

            val intent = Intent(context, com.example.busmaker.RouteMapActivity::class.java).apply {
                putExtra("startLat", route.startLat ?: 0.0)
                putExtra("startLng", route.startLng ?: 0.0)
                putExtra("endLat", route.endLat ?: 0.0)
                putExtra("endLng", route.endLng ?: 0.0)
                putExtra("segmentsJson", segmentsJson)   // ★ 구간 정보 전체 전달
            }
            context.startActivity(intent)
        }

    }
}
