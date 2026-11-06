package com.example.motorapp.data.repository

import android.util.Log
import com.example.motorapp.domain.model.RouteStep
import com.example.motorapp.domain.model.TurnManeuver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RouteResult(
    val routePoints: List<Pair<Double, Double>>,
    val steps: List<RouteStep>,
    val totalDistanceMeters: Int
)

class OsmRouteRepository {

    suspend fun fetchRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$destLng,$destLat?overview=full&geometries=geojson&steps=true"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MotorAppTelemetry/1.0 (https://github.com/motorapp/motorapp)")
            }

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val routes = root.getJSONArray("routes")
                if (routes.length() > 0) {
                    val primaryRoute = routes.getJSONObject(0)
                    val totalDistance = primaryRoute.optDouble("distance", 0.0).toInt()

                    // Parse Polyline Geometry (Coordinates array: [lng, lat])
                    val geometry = primaryRoute.getJSONObject("geometry")
                    val coords = geometry.getJSONArray("coordinates")
                    val points = mutableListOf<Pair<Double, Double>>()
                    for (i in 0 until coords.length()) {
                        val coordPair = coords.getJSONArray(i)
                        val lng = coordPair.getDouble(0)
                        val lat = coordPair.getDouble(1)
                        points.add(Pair(lat, lng))
                    }

                    // Parse Navigation Steps
                    val legs = primaryRoute.getJSONArray("legs")
                    val stepList = mutableListOf<RouteStep>()
                    if (legs.length() > 0) {
                        val legSteps = legs.getJSONObject(0).getJSONArray("steps")
                        for (i in 0 until legSteps.length()) {
                            val stepObj = legSteps.getJSONObject(i)
                            val stepDistance = stepObj.optDouble("distance", 0.0).toInt()
                            val streetName = stepObj.optString("name", "Road")

                            val maneuverObj = stepObj.getJSONObject("maneuver")
                            val type = maneuverObj.optString("type", "")
                            val modifier = maneuverObj.optString("modifier", "")
                            val locationArr = maneuverObj.getJSONArray("location")
                            val stepLng = locationArr.getDouble(0)
                            val stepLat = locationArr.getDouble(1)

                            val maneuver = parseManeuver(type, modifier)
                            stepList.add(
                                RouteStep(
                                    maneuver = maneuver,
                                    distanceMeters = stepDistance,
                                    streetName = if (streetName.isNotBlank()) streetName else "Road",
                                    latitude = stepLat,
                                    longitude = stepLng
                                )
                            )
                        }
                    }

                    return@withContext RouteResult(
                        routePoints = points,
                        steps = stepList,
                        totalDistanceMeters = totalDistance
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OsmRouteRepository", "Error fetching OSRM road route", e)
        }
        return@withContext null
    }

    private fun parseManeuver(type: String, modifier: String): TurnManeuver {
        if (type == "arrive") return TurnManeuver.ARRIVED
        return when (modifier) {
            "straight" -> TurnManeuver.STRAIGHT
            "slight left" -> TurnManeuver.SLIGHT_LEFT
            "left" -> TurnManeuver.LEFT
            "sharp left" -> TurnManeuver.SHARP_LEFT
            "slight right" -> TurnManeuver.SLIGHT_RIGHT
            "right" -> TurnManeuver.RIGHT
            "sharp right" -> TurnManeuver.SHARP_RIGHT
            "uturn" -> TurnManeuver.U_TURN
            else -> TurnManeuver.STRAIGHT
        }
    }
}
