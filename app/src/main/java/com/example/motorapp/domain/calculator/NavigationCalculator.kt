package com.example.motorapp.domain.calculator

import com.example.motorapp.domain.model.NavigationState
import com.example.motorapp.domain.model.TurnManeuver
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationCalculator {

    fun computeNavigation(
        currentLat: Double,
        currentLng: Double,
        currentHeadingDegrees: Float,
        destLat: Double,
        destLng: Double,
        destName: String = "Destination"
    ): NavigationState {
        val distanceMeters = calculateHaversineDistanceMeters(currentLat, currentLng, destLat, destLng)

        if (distanceMeters < 15) {
            return NavigationState(
                hasActiveDestination = true,
                destinationName = destName,
                destinationLatitude = destLat,
                destinationLongitude = destLng,
                nextManeuver = TurnManeuver.ARRIVED,
                distanceToNextTurnMeters = 0,
                totalDistanceRemainingMeters = 0,
                nextStreetName = "Arrived at Destination"
            )
        }

        val targetBearing = calculateBearing(currentLat, currentLng, destLat, destLng)
        var relativeAngle = targetBearing - currentHeadingDegrees
        while (relativeAngle > 180f) relativeAngle -= 360f
        while (relativeAngle < -180f) relativeAngle += 360f

        val maneuver = when {
            distanceMeters < 25 -> TurnManeuver.ARRIVED
            relativeAngle in -20f..20f -> TurnManeuver.STRAIGHT
            relativeAngle in -60f..-20f -> TurnManeuver.SLIGHT_LEFT
            relativeAngle in -120f..-60f -> TurnManeuver.LEFT
            relativeAngle < -120f -> TurnManeuver.SHARP_LEFT
            relativeAngle in 20f..60f -> TurnManeuver.SLIGHT_RIGHT
            relativeAngle in 60f..120f -> TurnManeuver.RIGHT
            relativeAngle > 120f -> TurnManeuver.SHARP_RIGHT
            else -> TurnManeuver.STRAIGHT
        }

        return NavigationState(
            hasActiveDestination = true,
            destinationName = destName,
            destinationLatitude = destLat,
            destinationLongitude = destLng,
            nextManeuver = maneuver,
            distanceToNextTurnMeters = distanceMeters.roundToInt(),
            totalDistanceRemainingMeters = distanceMeters.roundToInt(),
            nextStreetName = destName
        )
    }

    fun calculateHaversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearingRad = atan2(y, x)
        return ((Math.toDegrees(bearingRad) + 360) % 360).toFloat()
    }
}
