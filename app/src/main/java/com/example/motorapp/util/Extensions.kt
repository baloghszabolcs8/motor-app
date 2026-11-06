package com.example.motorapp.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.util.GeoPoint
import java.util.Locale

fun Int.formatDistance(): String = when {
    this >= 10_000 -> "${this / 1000} km"
    this >= 1_000 -> "${"%.1f".format(Locale.US, this / 1000.0f)} km"
    else -> "$this m"
}

fun Float.formatDecimal(digits: Int = 1): String =
    "%.${digits}f".format(Locale.US, this)

fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

fun Location.toLatLng() = LatLng(latitude, longitude)
