package com.example.busmaker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busmaker.R
import com.example.busmaker.data.model.RouteInformation

class RouteListAdapter(
    private val routeList: List<RouteInformation>,
    private val onItemClick: (RouteInformation) -> Unit // ← 수정
) : RecyclerView.Adapter<RouteListAdapter.RouteViewHolder>() {

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSummary: TextView = itemView.findViewById(R.id.tv_route_summary)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_route_info)

        fun bind(route: RouteInformation) { // ← 수정
            tvSummary.text = route.routeSummary
            tvInfo.text = "소요 시간: ${route.totalTime}분 / 환승 ${route.transferCount}회"

            itemView.setOnClickListener {
                onItemClick(route)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routeList[position])
    }

    override fun getItemCount(): Int = routeList.size
}

