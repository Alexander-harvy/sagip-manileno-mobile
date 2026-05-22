package com.sagip.manileno.ui.responder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.GenericResponse
import com.sagip.manileno.network.UpdateStatusRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResponderIncidentDetailsActivity : AppCompatActivity() {

    private var incidentId: Int = -1
    private var incidentLatitude: Double = 0.0
    private var incidentLongitude: Double = 0.0
    private var currentStatus: String = "unknown"

    private lateinit var tvIncidentTitle: TextView
    private lateinit var tvIncidentDescription: TextView
    private lateinit var tvIncidentStatus: TextView
    private lateinit var tvCoordinates: TextView

    private lateinit var btnEnRoute: Button
    private lateinit var btnOnScene: Button
    private lateinit var btnResolved: Button
    private lateinit var btnStartNavigation: Button

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responder_incident_details)

        bindViews()
        readIntentData()
        renderIncidentData()
        setupMap()
        setupButtons()
        updateStatusButtonState()
    }

    private fun bindViews() {
        val btnBack = findViewById<TextView>(R.id.btnBack)

        tvIncidentTitle = findViewById(R.id.tvIncidentTitle)
        tvIncidentDescription = findViewById(R.id.tvIncidentDescription)
        tvIncidentStatus = findViewById(R.id.tvIncidentStatus)
        tvCoordinates = findViewById(R.id.tvCoordinates)

        btnEnRoute = findViewById(R.id.btnEnRoute)
        btnOnScene = findViewById(R.id.btnOnScene)
        btnResolved = findViewById(R.id.btnResolved)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun readIntentData() {
        incidentId = intent.getIntExtra("incident_id", -1)
        incidentLatitude = intent.getDoubleExtra("latitude", 0.0)
        incidentLongitude = intent.getDoubleExtra("longitude", 0.0)
        currentStatus = intent.getStringExtra("status") ?: "unknown"
    }

    private fun renderIncidentData() {
        val type = intent.getStringExtra("incident_type") ?: "Incident"
        val description = intent.getStringExtra("description") ?: "No description"

        tvIncidentTitle.text = type
        tvIncidentDescription.text = description
        tvIncidentStatus.text = "Status: ${formatStatus(currentStatus)}"

        tvCoordinates.text = if (hasValidCoordinates()) {
            "Coordinates: $incidentLatitude, $incidentLongitude"
        } else {
            "Coordinates: unavailable"
        }
    }

    private fun setupButtons() {
        btnEnRoute.setOnClickListener {
            updateStatus("en_route")
        }

        btnOnScene.setOnClickListener {
            updateStatus("on_scene")
        }

        btnResolved.setOnClickListener {
            updateStatus("resolved")
        }

        btnStartNavigation.setOnClickListener {
            startNavigation()
        }
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment

        mapFragment?.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true

            if (hasValidCoordinates()) {
                val incidentPoint = LatLng(incidentLatitude, incidentLongitude)

                map.addMarker(
                    MarkerOptions()
                        .position(incidentPoint)
                        .title(intent.getStringExtra("incident_type") ?: "Incident Location")
                        .snippet(formatStatus(currentStatus))
                )

                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(incidentPoint, 16f)
                )
            } else {
                val manila = LatLng(14.5995, 120.9842)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(manila, 13f)
                )
            }
        }
    }

    private fun startNavigation() {
        if (!hasValidCoordinates()) {
            Toast.makeText(this, "Incident location unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("google.navigation:q=$incidentLatitude,$incidentLongitude")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (_: Exception) {
            val fallbackUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=$incidentLatitude,$incidentLongitude"
            )
            startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }

    private fun updateStatus(status: String) {
        if (incidentId == -1) {
            Toast.makeText(this, "Invalid incident", Toast.LENGTH_SHORT).show()
            return
        }

        setStatusButtonsEnabled(false)

        val request = UpdateStatusRequest(
            incident_id = incidentId,
            status = status
        )

        ApiClient.getClient(this).updateResponderStatus(request)
            .enqueue(object : Callback<GenericResponse> {

                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    setStatusButtonsEnabled(true)

                    if (response.isSuccessful && response.body()?.success == true) {
                        currentStatus = status
                        tvIncidentStatus.text = "Status: ${formatStatus(currentStatus)}"
                        updateStatusButtonState()

                        Toast.makeText(
                            this@ResponderIncidentDetailsActivity,
                            "Status updated: ${formatStatus(status)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ResponderIncidentDetailsActivity,
                            response.body()?.message ?: "Failed to update status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    setStatusButtonsEnabled(true)

                    Toast.makeText(
                        this@ResponderIncidentDetailsActivity,
                        t.message ?: "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateStatusButtonState() {
        btnEnRoute.isEnabled = currentStatus == "responder_assigned"
        btnOnScene.isEnabled = currentStatus == "en_route"
        btnResolved.isEnabled = currentStatus == "on_scene"

        if (currentStatus == "resolved") {
            setStatusButtonsEnabled(false)
        }
    }

    private fun setStatusButtonsEnabled(enabled: Boolean) {
        btnEnRoute.isEnabled = enabled
        btnOnScene.isEnabled = enabled
        btnResolved.isEnabled = enabled
    }

    private fun hasValidCoordinates(): Boolean {
        return incidentLatitude != 0.0 && incidentLongitude != 0.0
    }

    private fun formatStatus(status: String): String {
        return status
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}