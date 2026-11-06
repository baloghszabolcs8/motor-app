package com.example.motorapp.domain.calculator

import com.example.motorapp.domain.model.LeanDirection
import com.example.motorapp.domain.model.TelemetryData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class TelemetryCalculator(
    private val standardGravity: Float = 9.80665f,
    private val lowPassAlpha: Float = 0.85f,
    private val noiseThreshold: Float = 0.25f
) {
    private var gravX = 0f
    private var gravY = 0f
    private var gravZ = 0f
    private var isFirstSample = true

    private var rawAngle = 0f
    private var tareOffset = 0f

    private var maxLeftLean = 0f
    private var maxRightLean = 0f
    private var maxG = 0f

    private var velX = 0f
    private var velY = 0f
    private var velZ = 0f
    private var stationaryCount = 0

    fun resetPeaksAndTare(rawAngleForTare: Float = rawAngle) {
        tareOffset = rawAngleForTare
        maxLeftLean = 0f
        maxRightLean = 0f
        maxG = 0f
        velX = 0f
        velY = 0f
        velZ = 0f
        stationaryCount = 0
    }

    fun processSensorSample(
        x: Float,
        y: Float,
        z: Float,
        dt: Float,
        gpsSpeedKmH: Float = 0f,
        headingDegrees: Float = 0f
    ): TelemetryData {
        if (isFirstSample) {
            gravX = x
            gravY = y
            gravZ = z
            isFirstSample = false
            rawAngle = computeRoll(gravX, gravY, gravZ)
            return TelemetryData(
                rawX = x, rawY = y, rawZ = z,
                gpsSpeedKmH = gpsSpeedKmH,
                headingDegrees = headingDegrees
            )
        }

        // Low-pass filter for gravity isolation
        gravX = lowPassAlpha * gravX + (1f - lowPassAlpha) * x
        gravY = lowPassAlpha * gravY + (1f - lowPassAlpha) * y
        gravZ = lowPassAlpha * gravZ + (1f - lowPassAlpha) * z

        // Dynamic linear acceleration
        var linX = x - gravX
        var linY = y - gravY
        var linZ = z - gravZ

        if (abs(linX) < noiseThreshold) linX = 0f
        if (abs(linY) < noiseThreshold) linY = 0f
        if (abs(linZ) < noiseThreshold) linZ = 0f

        val accelMag = sqrt(linX * linX + linY * linY + linZ * linZ)
        val currentG = accelMag / standardGravity
        if (currentG > maxG) maxG = currentG

        rawAngle = computeRoll(gravX, gravY, gravZ)
        val calibratedAngle = rawAngle - tareOffset

        val direction = when {
            calibratedAngle > 1.5f -> LeanDirection.LEFT
            calibratedAngle < -1.5f -> LeanDirection.RIGHT
            else -> LeanDirection.CENTER
        }

        if (calibratedAngle > maxLeftLean) maxLeftLean = calibratedAngle
        if (calibratedAngle < maxRightLean) maxRightLean = calibratedAngle

        if (dt > 0f) {
            velX += linX * dt
            velY += linY * dt
            velZ += linZ * dt

            if (accelMag == 0f) {
                stationaryCount++
                if (stationaryCount > 15) {
                    velX = 0f; velY = 0f; velZ = 0f
                } else {
                    velX *= 0.9f; velY *= 0.9f; velZ *= 0.9f
                }
            } else {
                stationaryCount = 0
            }
        }

        val calculatedSpeedKmH = sqrt(velX * velX + velY * velY + velZ * velZ) * 3.6f

        return TelemetryData(
            leanAngleDegrees = abs(calibratedAngle),
            maxLeftLeanDegrees = maxLeftLean,
            maxRightLeanDegrees = abs(maxRightLean),
            leanDirection = direction,
            gForce = currentG,
            maxGForce = maxG,
            linearAccelerationMps2 = accelMag,
            calculatedSpeedKmH = calculatedSpeedKmH,
            gpsSpeedKmH = gpsSpeedKmH,
            headingDegrees = headingDegrees,
            rawX = x, rawY = y, rawZ = z
        )
    }

    private fun computeRoll(gx: Float, gy: Float, gz: Float): Float {
        val vertical = sqrt(gy * gy + gz * gz)
        return (atan2(gx.toDouble(), vertical.toDouble()) * (180.0 / PI)).toFloat()
    }
}
