package com.example.busmaker.utils // 본인의 패키지 경로에 맞게 수정

import kotlin.math.*

// 기존 DistanceCalculator object (위에서 이미 만듦)
object DistanceCalculator {

    private const val EARTH_RADIUS_KM = 6371.0 // 지구 반지름 (킬로미터 단위)

    /**
     * 두 지점의 위도, 경도를 사용하여 Haversine 공식으로 거리를 계산합니다.
     *
     * @param lat1 지점 1의 위도 (십진수 도 단위, 예: 37.5665)
     * @param lon1 지점 1의 경도 (십진수 도 단위, 예: 126.9780)
     * @param lat2 지점 2의 위도
     * @param lon2 지점 2의 경도
     * @return 두 지점 간의 거리 (미터 단위)
     */
    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // 위도와 경도를 라디안으로 변환
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // 위도와 경도의 차이
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        // Haversine 공식 적용
        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        // 거리 계산 (킬로미터 단위) 후 미터 단위로 변환
        return EARTH_RADIUS_KM * c * 1000
    }
}

// --- 여기에 새로운 WalkingTimeEstimator object 추가 ---
object WalkingTimeEstimator {

    // 평균 도보 속도 설정 (분당 미터 단위)
    // 예: 분당 70미터 (시속 4.2km에 해당)
    // 이 값은 서비스의 특성이나 대상 사용자에 맞춰 조정할 수 있습니다.
    private const val AVERAGE_WALKING_SPEED_METERS_PER_MINUTE = 70.0

    /**
     * 주어진 거리에 대한 예상 도보 시간을 계산합니다.
     *
     * @param distanceInMeters 도보 거리 (미터 단위).
     *                         DistanceCalculator.calculateDistanceInMeters() 등을 통해
     *                         미리 계산된 값이어야 합니다.
     * @return 예상 도보 시간 (분 단위, 소수점 이하 올림 처리된 정수)
     */
    fun estimateWalkingTimeFromDistance(distanceInMeters: Double): Int {
        // 거리가 0 이하인 경우 도보 시간은 0분으로 처리
        if (distanceInMeters <= 0) {
            return 0
        }

        // 도보 시간 계산 (분 단위)
        val timeInMinutesDecimal = distanceInMeters / AVERAGE_WALKING_SPEED_METERS_PER_MINUTE

        // 계산된 시간을 올림하여 정수 분으로 반환 (예: 3.2분 -> 4분)
        return ceil(timeInMinutesDecimal).toInt()
    }

    /**
     * 두 지점의 좌표를 직접 받아 예상 도보 시간을 계산합니다.
     * 내부적으로 DistanceCalculator를 사용하여 거리를 계산합니다.
     *
     * @param startLat 출발지 위도
     * @param startLon 출발지 경도
     * @param endLat 도착지 위도
     * @param endLon 도착지 경도
     * @return 예상 도보 시간 (분 단위, 소수점 이하 올림 처리된 정수)
     */
    fun estimateWalkingTimeBetweenCoordinates(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Int {
        // 1. DistanceCalculator를 사용해 두 좌표 간의 거리를 계산
        val distance = DistanceCalculator.calculateDistanceInMeters(startLat, startLon, endLat, endLon)

        // 2. 계산된 거리로 도보 시간 추정
        return estimateWalkingTimeFromDistance(distance)
    }
}