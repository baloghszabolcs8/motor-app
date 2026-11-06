package com.example.motorapp.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.repository.OsmRouteRepository
import com.example.motorapp.data.repository.P2pRideShareRepository
import com.example.motorapp.data.repository.SensorRepository
import com.example.motorapp.domain.calculator.NavigationCalculator
import com.example.motorapp.domain.model.NavigationState
import com.example.motorapp.domain.model.PeerRider
import com.example.motorapp.domain.model.TelemetryData
import com.example.motorapp.domain.model.TurnManeuver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    MINIMALIST_HUD,
    MAP_AND_RIDE
}

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorRepository = SensorRepository(application)
    private val p2pRideShareRepository = P2pRideShareRepository(application)
    private val osmRouteRepository = OsmRouteRepository()
    private val navigationCalculator = NavigationCalculator()

    val telemetryState: StateFlow<TelemetryData> = sensorRepository.telemetryState
    val connectedPeers: StateFlow<Map<String, PeerRider>> = p2pRideShareRepository.connectedPeers
    val isP2pSharing: StateFlow<Boolean> = p2pRideShareRepository.isSharing

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _activeScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    init {
        viewModelScope.launch {
            sensorRepository.observeTelemetry().collectLatest { telemetry ->
                updateNavigationState(telemetry.headingDegrees)

                _currentLocation.value?.let { loc ->
                    if (isP2pSharing.value) {
                        p2pRideShareRepository.broadcastLocationAndTelemetry(
                            riderName = "MotorRider",
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            leanAngle = telemetry.leanAngleDegrees,
                            speedKmH = if (telemetry.gpsSpeedKmH > 0) telemetry.gpsSpeedKmH else telemetry.calculatedSpeedKmH
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            sensorRepository.observeLocation().collectLatest { location ->
                _currentLocation.value = location
                updateNavigationState(telemetryState.value.headingDegrees)
            }
        }
    }

    private fun updateNavigationState(heading: Float) {
        val loc = _currentLocation.value ?: return
        val nav = _navigationState.value
        if (!nav.hasActiveDestination) return

        if (nav.steps.isNotEmpty() && nav.currentStepIndex < nav.steps.size) {
            val currentStep = nav.steps[nav.currentStepIndex]
            val distanceToStep = navigationCalculator.calculateHaversineDistanceMeters(
                loc.latitude, loc.longitude,
                currentStep.latitude, currentStep.longitude
            ).toInt()

            if (distanceToStep < 20 && nav.currentStepIndex + 1 < nav.steps.size) {
                val nextIndex = nav.currentStepIndex + 1
                val nextStep = nav.steps[nextIndex]
                _navigationState.value = nav.copy(
                    currentStepIndex = nextIndex,
                    nextManeuver = nextStep.maneuver,
                    distanceToNextTurnMeters = nextStep.distanceMeters,
                    nextStreetName = nextStep.streetName
                )
            } else {
                _navigationState.value = nav.copy(
                    distanceToNextTurnMeters = distanceToStep
                )
            }
        } else {
            val updatedNav = navigationCalculator.computeNavigation(
                currentLat = loc.latitude,
                currentLng = loc.longitude,
                currentHeadingDegrees = heading,
                destLat = nav.destinationLatitude,
                destLng = nav.destinationLongitude,
                destName = nav.destinationName
            )
            _navigationState.value = updatedNav.copy(
                routePoints = nav.routePoints,
                steps = nav.steps
            )
        }
    }

    fun setDestination(latitude: Double, longitude: Double, name: String = "Destination") {
        val loc = _currentLocation.value
        val startLat = loc?.latitude ?: latitude
        val startLng = loc?.longitude ?: longitude

        _navigationState.value = NavigationState(
            hasActiveDestination = true,
            destinationName = name,
            destinationLatitude = latitude,
            destinationLongitude = longitude,
            isLoadingRoute = true
        )

        viewModelScope.launch {
            val result = osmRouteRepository.fetchRoute(startLat, startLng, latitude, longitude)
            val currentNav = _navigationState.value
            if (result != null && result.routePoints.isNotEmpty()) {
                val firstStep = result.steps.firstOrNull()
                _navigationState.value = currentNav.copy(
                    routePoints = result.routePoints,
                    steps = result.steps,
                    nextManeuver = firstStep?.maneuver ?: TurnManeuver.STRAIGHT,
                    distanceToNextTurnMeters = firstStep?.distanceMeters ?: result.totalDistanceMeters,
                    totalDistanceRemainingMeters = result.totalDistanceMeters,
                    nextStreetName = firstStep?.streetName ?: "Road",
                    isLoadingRoute = false
                )
            } else {
                // Fallback to straight line calculation if offline/network error
                val fallbackNav = navigationCalculator.computeNavigation(
                    currentLat = startLat,
                    currentLng = startLng,
                    currentHeadingDegrees = telemetryState.value.headingDegrees,
                    destLat = latitude,
                    destLng = longitude,
                    destName = name
                )
                _navigationState.value = fallbackNav.copy(isLoadingRoute = false)
            }
        }
    }

    fun clearDestination() {
        _navigationState.value = NavigationState()
    }

    fun setActiveScreen(screen: AppScreen) {
        _activeScreen.value = screen
    }

    fun resetTelemetry() {
        sensorRepository.resetTelemetry()
    }

    fun toggleP2pRideShare(riderName: String = "MotorRider") {
        if (isP2pSharing.value) {
            p2pRideShareRepository.stopSharing()
        } else {
            p2pRideShareRepository.startSharing(riderName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pRideShareRepository.stopSharing()
    }
}
