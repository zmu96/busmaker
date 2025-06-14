package com.example.busmaker.data.api

import androidx.compose.ui.input.key.type
import com.example.busmaker.data.model.StationItemDetail // 실제 StationItemDetail 모델 경로
import com.example.busmaker.data.model.StationItemsContainer // 실제 StationItemsContainer 모델 경로
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class StationItemsDeserializer : JsonDeserializer<StationItemsContainer> {
    override fun deserialize(
        json: JsonElement?, // 이 json은 $.response.body.items 에 해당하는 객체입니다.
        typeOfT: Type?,     // 이 typeOfT는 StationItemsContainer::class.java 입니다.
        context: JsonDeserializationContext
    ): StationItemsContainer {
        val stationList = mutableListOf<StationItemDetail>()

        // 1. $.response.body.items 자체가 null이거나 JsonNull인 경우
        if (json == null || json.isJsonNull) {
            return StationItemsContainer(stationList = emptyList())
        }

        // 2. $.response.body.items가 빈 문자열 "" 인 경우 (API가 이렇게 응답할 수도 있다면)
        if (json.isJsonPrimitive && json.asJsonPrimitive.isString && json.asString.isEmpty()) {
            return StationItemsContainer(stationList = emptyList())
        }

        // 3. $.response.body.items가 객체인 경우 (정상적인 경우)
        if (json.isJsonObject) {
            val itemsObject = json.asJsonObject // $.response.body.items 객체

            // "item" 필드를 가져옵니다. (이것이 단일 객체 또는 배열일 수 있습니다)
            val itemElement = itemsObject.get("item")

            if (itemElement != null && !itemElement.isJsonNull) {
                if (itemElement.isJsonArray) {
                    // "item"이 배열인 경우: [{...}, {...}]
                    // 각 요소를 StationItemDetail로 변환하여 리스트에 추가
                    val listType = object : TypeToken<List<StationItemDetail>>() {}.type
                    val deserializedList: List<StationItemDetail> = context.deserialize(itemElement, listType)
                    stationList.addAll(deserializedList)
                } else if (itemElement.isJsonObject) {
                    // "item"이 단일 객체인 경우: {...}
                    // 해당 객체를 StationItemDetail로 변환하여 리스트에 추가
                    val deserializedItem: StationItemDetail = context.deserialize(itemElement, StationItemDetail::class.java)
                    stationList.add(deserializedItem)
                }
                // itemElement가 문자열 "" 등으로 오는 경우에 대한 방어 코드를 추가할 수도 있습니다.
                // else if (itemElement.isJsonPrimitive && itemElement.asJsonPrimitive.isString && itemElement.asString.isEmpty()) {
                //    // 이 경우 stationList는 비어있게 됩니다.
                // }
            }
            // "item" 필드가 없거나 null이면 stationList는 비어있게 됩니다.
        }
        // 이 Deserializer는 $.response.body.items 가 객체라고 가정하므로,
        // json.isJsonArray() 인 경우는 여기서 직접 처리하지 않습니다.
        // 만약 $.response.body.items 자체가 배열로 올 수 있다면, 모델 구조가 달라져야 합니다.

        return StationItemsContainer(stationList = stationList)
    }
}