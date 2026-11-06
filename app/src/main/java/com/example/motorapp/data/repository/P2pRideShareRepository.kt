package com.example.motorapp.data.repository

import android.content.Context
import android.util.Log
import com.example.motorapp.domain.model.PeerRider
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class P2pRideShareRepository(context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val serviceId = "com.example.motorapp.RIDE_SHARE"

    private val _connectedPeers = MutableStateFlow<Map<String, PeerRider>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, PeerRider>> = _connectedPeers.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val activeEndpointIds = mutableSetOf<String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val jsonStr = String(bytes, StandardCharsets.UTF_8)
                try {
                    val json = JSONObject(jsonStr)
                    val rider = PeerRider(
                        id = endpointId,
                        name = json.optString("name", "Rider"),
                        latitude = json.optDouble("lat", 0.0),
                        longitude = json.optDouble("lng", 0.0),
                        leanAngle = json.optDouble("lean", 0.0).toFloat(),
                        speedKmH = json.optDouble("speed", 0.0).toFloat(),
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    _connectedPeers.value = _connectedPeers.value + (endpointId to rider)
                } catch (e: Exception) {
                    Log.e("P2pRideShare", "Failed to parse payload", e)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept incoming group ride connections
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                activeEndpointIds.add(endpointId)
                Log.d("P2pRideShare", "Connected to peer: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            activeEndpointIds.remove(endpointId)
            _connectedPeers.value = _connectedPeers.value - endpointId
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection("MotorRider", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            _connectedPeers.value = _connectedPeers.value - endpointId
        }
    }

    fun startSharing(riderName: String = "MotorRider") {
        if (_isSharing.value) return

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            riderName,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            _isSharing.value = true
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        )
    }

    fun stopSharing() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        activeEndpointIds.clear()
        _connectedPeers.value = emptyMap()
        _isSharing.value = false
    }

    fun broadcastLocationAndTelemetry(
        riderName: String,
        latitude: Double,
        longitude: Double,
        leanAngle: Float,
        speedKmH: Float
    ) {
        if (activeEndpointIds.isEmpty()) return

        val json = JSONObject().apply {
            put("name", riderName)
            put("lat", latitude)
            put("lng", longitude)
            put("lean", leanAngle)
            put("speed", speedKmH)
        }

        val payload = Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(activeEndpointIds.toList(), payload)
    }
}
