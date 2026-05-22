package com.sagip.manileno.ui.citizen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sagip.manileno.MainActivity
import com.sagip.manileno.R
import com.sagip.manileno.utils.TokenManager
import com.sagip.manileno.offline.OfflineSyncManager
import com.sagip.manileno.utils.NetworkUtils

class CitizenDashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private var googleMap: GoogleMap? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                showUserLocationOnMap()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_citizen_dashboard)

        tokenManager = TokenManager(this)

        val name = tokenManager.getName() ?: "Citizen"

        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvActiveIncident = findViewById<TextView>(R.id.tvActiveIncident)

        val btnMyIncidents = findViewById<Button>(R.id.btnMyIncidents)
        val btnTrackIncident = findViewById<Button>(R.id.btnTrackIncident)
        val btnPolice = findViewById<Button>(R.id.btnPolice)
        val btnMedical = findViewById<Button>(R.id.btnMedical)
        val btnFire = findViewById<Button>(R.id.btnFire)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        tvGreeting.text = "Hello Citizen, $name!"
        tvActiveIncident.text = "No active incident"

        setupMap()

        btnPolice.setOnClickListener {
            openReportIncident("Police")
        }

        btnMedical.setOnClickListener {
            openReportIncident("Medic")
        }

        btnFire.setOnClickListener {
            openReportIncident("Fire")
        }

        btnMyIncidents.setOnClickListener {
            startActivity(Intent(this, MyIncidentsActivity::class.java))
        }

        btnTrackIncident.setOnClickListener {
            startActivity(Intent(this, MyIncidentsActivity::class.java))
        }

        btnLogout.setOnClickListener {
            tokenManager.clearSession()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun openReportIncident(type: String) {
        val intent = Intent(this, ReportIncidentActivity::class.java)
        intent.putExtra("incident_type", type)
        startActivity(intent)
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment

        mapFragment?.getMapAsync { map ->
            googleMap = map

            val manila = LatLng(14.5995, 120.9842)

            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 14f))

            if (hasLocationPermission()) {
                showUserLocationOnMap()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun showUserLocationOnMap() {
        if (!hasLocationPermission()) return

        try {
            val map = googleMap ?: return

            map.isMyLocationEnabled = true

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)

                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(userLocation)
                        .title("Your Location")
                )

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLocation, 16f)
                )
            }
        } catch (_: SecurityException) {
            // Permission was revoked while the app was running.
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    override fun onResume() {
        super.onResume()

        if (NetworkUtils.hasInternetConnection(this)) {
            OfflineSyncManager.syncOfflineIncidents(this)
        }
    }
}