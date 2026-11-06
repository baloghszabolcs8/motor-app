package com.example.motorapp

import com.example.motorapp.domain.calculator.NavigationCalculator
import com.example.motorapp.domain.model.TurnManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationCalculatorTest {

    private val calculator = NavigationCalculator()

    @Test
    fun testHaversineDistance() {
        // Distance between Budapest center and Budapest Airport ~ 17 km
        val distance = calculator.calculateHaversineDistanceMeters(
            47.4979, 19.0402,
            47.4369, 19.2556
        )
        assertTrue(distance > 15000 && distance < 20000)
    }

    @Test
    fun testTurnDirectionLeft() {
        // Pointing North (0°), destination is West (-90°) -> TURN LEFT
        val navState = calculator.computeNavigation(
            currentLat = 47.4979, currentLng = 19.0402,
            currentHeadingDegrees = 0f,
            destLat = 47.4979, destLng = 19.0000
        )
        assertEquals(TurnManeuver.LEFT, navState.nextManeuver)
        assertTrue(navState.distanceToNextTurnMeters > 0)
    }

    @Test
    fun testTurnDirectionRight() {
        // Pointing North (0°), destination is East (+90°) -> TURN RIGHT
        val navState = calculator.computeNavigation(
            currentLat = 47.4979, currentLng = 19.0402,
            currentHeadingDegrees = 0f,
            destLat = 47.4979, destLng = 19.1000
        )
        assertEquals(TurnManeuver.RIGHT, navState.nextManeuver)
    }
}
