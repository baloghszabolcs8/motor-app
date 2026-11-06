package com.example.motorapp.domain.model

enum class TurnManeuver {
    NONE,
    STRAIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    U_TURN,
    ARRIVED
}

data class RouteStep(
    val maneuver: TurnManeuver,
    val distanceMeters: Int,
    val streetName: String,
    val latitude: Double,
    val longitude: Double
)

data class NavigationState(
    val hasActiveDestination: Boolean = false,
    val destinationName: String = "",
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val nextManeuver: TurnManeuver = TurnManeuver.NONE,
    val distanceToNextTurnMeters: Int = 0,
    val totalDistanceRemainingMeters: Int = 0,
    val nextStreetName: String = "",
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val steps: List<RouteStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isLoadingRoute: Boolean = false
)
