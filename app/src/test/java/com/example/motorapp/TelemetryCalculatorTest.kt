package com.example.motorapp

import com.example.motorapp.domain.calculator.TelemetryCalculator
import com.example.motorapp.domain.model.LeanDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelemetryCalculatorTest {

    private lateinit var calculator: TelemetryCalculator

    @Before
    fun setUp() {
        calculator = TelemetryCalculator()
    }

    @Test
    fun testFirstMeasurementInitialization() {
        val data = calculator.processSensorSample(0f, 0f, 9.8f, 0.02f)
        assertEquals(0f, data.leanAngleDegrees, 0.1f)
        assertEquals(LeanDirection.CENTER, data.leanDirection)
    }

    @Test
    fun testLeftLeanCalculation() {
        // Initial sample establishing gravity
        calculator.processSensorSample(0f, 0f, 9.8f, 0.02f)
        // Simulate leaning left (positive X tilt under gravity)
        val data = calculator.processSensorSample(3f, 0f, 9.3f, 0.02f)
        assertTrue(data.leanAngleDegrees > 0f)
        assertEquals(LeanDirection.LEFT, data.leanDirection)
    }

    @Test
    fun testResetPeaksAndTare() {
        calculator.processSensorSample(0f, 0f, 9.8f, 0.02f)
        calculator.processSensorSample(5f, 0f, 8.5f, 0.02f)
        calculator.resetPeaksAndTare()

        val dataAfterReset = calculator.processSensorSample(0f, 0f, 9.8f, 0.02f)
        assertEquals(0f, dataAfterReset.maxLeftLeanDegrees, 0.1f)
        assertEquals(0f, dataAfterReset.maxGForce, 0.1f)
    }
}
