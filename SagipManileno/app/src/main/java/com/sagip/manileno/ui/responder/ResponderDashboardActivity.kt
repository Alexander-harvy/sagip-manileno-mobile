package com.sagip.manileno.ui.responder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sagip.manileno.MainActivity
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.ResponderIncident
import com.sagip.manileno.network.ResponderIncidentsResponse
import com.sagip.manileno.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResponderDashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    private lateinit var tvGreeting: TextView
    private lateinit var tvAssignedCount: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var tvResolvedCount: TextView
    private lateinit var tvIncidentType: TextView
    private lateinit var tvIncidentStatus: TextView
    private lateinit var tvIncidentDescription: TextView
    private lateinit var btnOpenIncident: Button

    private var googleMap: GoogleMap? = null
    private var currentIncident: ResponderIncident? = null

    private val activeStatuses = setOf(
        "responder_assigned",
        "en_route",
        "on_scene"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responder_dashboard)

        tokenManager = TokenManager(this)

        bindViews()
        setupUserInfo()
        setupButtons()
        setupMap()
        fetchResponderIncidents()
    }

    override fun onResume() {
        super.onResume()
        fetchResponderIncidents()
    }

    private fun bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvAssignedCount = findViewById(R.id.tvAssignedCount)
        tvActiveCount = findViewById(R.id.tvActiveCount)
        tvResolvedCount = findViewById(R.id.tvResolvedCount)
        tvIncidentType = findViewById(R.id.tvIncidentType)
        tvIncidentStatus = findViewById(R.id.tvIncidentStatus)
        tvIncidentDescription = findViewById(R.id.tvIncidentDescription)
        btnOpenIncident = findViewById(R.id.btnOpenIncident)
    }

    private fun setupUserInfo() {
        val responderName = tokenManager.getName() ?: "Responder"
        tvGreeting.text = "Hello Responder,\n$responderName!"
    }

    private fun setupButtons() {
        val btnIncidentLogs = findViewById<Button>(R.id.btnIncidentLogs)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnIncidentLogs.setOnClickListener {
            startActivity(Intent(this, AssignedIncidentsActivity::class.java))
        }

        btnOpenIncident.setOnClickListener {
            val incident = currentIncident

            if (incident == null) {
                Toast.makeText(this, "No active incident", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            openIncidentDetails(incident)
        }

        btnLogout.setOnClickListener {
            tokenManager.clearSession()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun fetchResponderIncidents() {
        ApiClient.getClient(this).getResponderIncidents()
            .enqueue(object : Callback<ResponderIncidentsResponse> {

                override fun onResponse(
                    call: Call<ResponderIncidentsResponse>,
                    response: Response<ResponderIncidentsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val incidents = response.body()?.data ?: emptyList()
                        renderIncidentStats(incidents)
                        renderCurrentIncident(incidents)
                        renderIncidentMarkers(incidents)
                    } else {
                        Toast.makeText(
                            this@ResponderDashboardActivity,
                            "Failed to load responder incidents",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponderIncidentsResponse>, t: Throwable) {
                    Toast.makeText(
                        this@ResponderDashboardActivity,
                        t.message ?: "Failed to load responder incidents",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun renderIncidentStats(incidents: List<ResponderIncident>) {
        val assigned = incidents.size
        val active = incidents.count {
            getIncidentStatus(it) in activeStatuses
        }
        val resolved = incidents.count {
            getIncidentStatus(it) == "resolved"
        }

        tvAssignedCount.text = "Assigned : $assigned"
        tvActiveCount.text = "Active : $active"
        tvResolvedCount.text = "Resolved : $resolved"
    }

    private fun renderCurrentIncident(incidents: List<ResponderIncident>) {
        currentIncident = incidents.firstOrNull {
            getIncidentStatus(it) in activeStatuses
        }

        val incident = currentIncident

        if (incident == null) {
            tvIncidentType.text = "No active incident"
            tvIncidentStatus.text = "-"
            tvIncidentDescription.text = "-"
            btnOpenIncident.isEnabled = false
            return
        }

        btnOpenIncident.isEnabled = true
        tvIncidentType.text = incident.incident_type
        tvIncidentStatus.text = formatStatus(getIncidentStatus(incident))
        tvIncidentDescription.text = incident.description ?: "No description"
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment

        mapFragment?.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true

            val manila = LatLng(14.5995, 120.9842)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 13f))

            showResponderLocation()
        }
    }

    private fun showResponderLocation() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val responderLocation = LatLng(it.latitude, it.longitude)

                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(responderLocation)
                            .title("Responder Location")
                    )

                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(responderLocation, 15f)
                    )
                }
            }
    }

    private fun renderIncidentMarkers(incidents: List<ResponderIncident>) {
        val map = googleMap ?: return

        map.clear()

        currentIncident?.let { incident ->
            val lat = incident.latitude
            val lng = incident.longitude

            if (lat != null && lng != null) {
                val point = LatLng(lat, lng)

                map.addMarker(
                    MarkerOptions()
                        .position(point)
                        .title(incident.incident_type)
                        .snippet(formatStatus(getIncidentStatus(incident)))
                )

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
            }
        }

        showResponderLocation()
    }

    private fun openIncidentDetails(incident: ResponderIncident) {
        val intent = Intent(this, ResponderIncidentDetailsActivity::class.java)

        intent.putExtra("incident_id", incident.incident_id)
        intent.putExtra("incident_type", incident.incident_type)
        intent.putExtra("description", incident.description ?: "")
        intent.putExtra("latitude", incident.latitude ?: 0.0)
        intent.putExtra("longitude", incident.longitude ?: 0.0)
        intent.putExtra("status", getIncidentStatus(incident))

        startActivity(intent)
    }

    private fun getIncidentStatus(incident: ResponderIncident): String {
        return incident.latest_status ?: incident.status ?: "unknown"
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