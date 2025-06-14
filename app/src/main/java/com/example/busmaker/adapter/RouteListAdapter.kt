package com.example.busmaker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.busmaker.R
import com.example.busmaker.data.model.RouteInformation

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

        // transportBar(구간 색상 표시, 동적으로 View 추가)
        holder.transportBar.removeAllViews()
        segments.forEach { seg ->
            val v = View(holder.transportBar.context)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            v.layoutParams = params
            v.setBackgroundColor(seg.color)
            holder.transportBar.addView(v)
        }

        // 안내시작 버튼 클릭 리스너
        holder.btnStartNavigation.setOnClickListener {
            onStartNavigation(route)
        }
    }
}
