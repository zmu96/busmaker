package com.example.busmaker.data.api

import com.example.busmaker.data.model.RouteItemsContainer
import com.example.busmaker.data.model.RouteItemDetail
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class RouteItemsContainerDeserializer : JsonDeserializer<RouteItemsContainer> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): RouteItemsContainer {
        // json == null 이나 JsonNull이면 빈 컨테이너 반환
        if (json == null || json.isJsonNull) {
            return RouteItemsContainer(routeList = emptyList())
        }

        // 빈 문자열("") 처리
        if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) {
            return RouteItemsContainer(routeList = emptyList())
        }

        // item이 배열인지, 객체인지 구분해서 파싱
        val routeList = mutableListOf<RouteItemDetail>()

        if (json.isJsonArray) {
            // [ {...}, {...} ] 형태
            val listType = object : TypeToken<List<RouteItemDetail>>() {}.type
            routeList.addAll(context.deserialize(json, listType))
        } else if (json.isJsonObject) {
            // { ... } 또는 {"item": ... } 형태
            val obj = json.asJsonObject

            // 실제 item 필드가 있으면 그걸 파싱, 아니면 객체 자체를 파싱
            val itemElement = obj.get("item")
            if (itemElement != null && !itemElement.isJsonNull) {
                if (itemElement.isJsonArray) {
                    val listType = object : TypeToken<List<RouteItemDetail>>() {}.type
                    routeList.addAll(context.deserialize(itemElement, listType))
                } else if (itemElement.isJsonObject) {
                    routeList.add(context.deserialize(itemElement, RouteItemDetail::class.java))
                }
            } else {
                // 그냥 객체 자체가 RouteItemDetail라면
                routeList.add(context.deserialize(json, RouteItemDetail::class.java))
            }
        }

        return RouteItemsContainer(routeList = routeList)
    }
}
