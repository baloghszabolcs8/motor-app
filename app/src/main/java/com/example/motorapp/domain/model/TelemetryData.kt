package com.example.motorapp.domain.model

enum class LeanDirection {
    CENTER,
    LEFT,
    RIGHT
}

data class TelemetryData(
    val leanAngleDegrees: Float = 0f,
    val maxLeftLeanDegrees: Float = 0f,
    val maxRightLeanDegrees: Float = 0f,
    val leanDirection: LeanDirection = LeanDirection.CENTER,
    val gForce: Float = 0f,
    val maxGForce: Float = 0f,
    val linearAccelerationMps2: Float = 0f,
    val calculatedSpeedKmH: Float = 0f,
    val gpsSpeedKmH: Float = 0f,
    val headingDegrees: Float = 0f,
    val rawX: Float = 0f,
    val rawY: Float = 0f,
    val rawZ: Float = 0f
)

data class PeerRider(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val leanAngle: Float = 0f,
    val speedKmH: Float = 0f,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
