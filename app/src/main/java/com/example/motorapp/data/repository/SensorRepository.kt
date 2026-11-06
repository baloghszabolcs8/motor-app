package com.example.motorapp.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.motorapp.domain.calculator.TelemetryCalculator
import com.example.motorapp.domain.model.TelemetryData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class SensorRepository(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val telemetryCalculator = TelemetryCalculator()

    private val _telemetryState = MutableStateFlow(TelemetryData())
    val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

    private var currentGpsSpeedKmH = 0f
    private var currentHeadingDegrees = 0f

    fun resetTelemetry() {
        telemetryCalculator.resetPeaksAndTare()
        _telemetryState.value = _telemetryState.value.copy(
            maxLeftLeanDegrees = 0f,
            maxRightLeanDegrees = 0f,
            maxGForce = 0f,
            calculatedSpeedKmH = 0f
        )
    }

    fun observeTelemetry(): Flow<TelemetryData> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var previousTimestamp = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (previousTimestamp == 0L) {
                        previousTimestamp = it.timestamp
                        return
                    }

                    val dtSeconds = (it.timestamp - previousTimestamp) / 1_000_000_000.0f
                    previousTimestamp = it.timestamp

                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val updatedTelemetry = telemetryCalculator.processSensorSample(
                        x = x,
                        y = y,
                        z = z,
                        dt = dtSeconds,
                        gpsSpeedKmH = currentGpsSpeedKmH,
                        headingDegrees = currentHeadingDegrees
                    )

                    _telemetryState.value = updatedTelemetry
                    trySend(updatedTelemetry)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    fun observeLocation(): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.hasSpeed()) {
                        currentGpsSpeedKmH = location.speed * 3.6f
                    }
                    if (location.hasBearing()) {
                        currentHeadingDegrees = location.bearing
                    }
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Location permission not granted
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
