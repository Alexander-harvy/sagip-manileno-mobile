package com.sagip.manileno.ui.citizen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.CreateIncidentRequest
import com.sagip.manileno.network.GenericResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Handler
import android.os.Looper

class ReportIncidentActivity : AppCompatActivity() {

    private lateinit var inputType: EditText
    private lateinit var inputDescription: EditText
    private lateinit var btnSubmit: Button

    private val minAccuracyMeters = 20f
    private val locationTimeoutMs = 15000L

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                submitIncidentWithLocation()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_incident)

        inputType = findViewById(R.id.inputType)
        inputDescription = findViewById(R.id.inputDescription)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            if (hasLocationPermission()) {
                submitIncidentWithLocation()
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

    private fun submitIncidentWithLocation() {
        val type = inputType.text.toString().trim()
        val description = inputDescription.text.toString().trim()

        if (type.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            btnSubmit.isEnabled = true
            Toast.makeText(this, "Please turn on Location/GPS", Toast.LENGTH_LONG).show()
            return
        }

        val lastKnownLocation =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            createAndSubmitIncident(type, description, lastKnownLocation)
            return
        }

        val provider = if (gpsEnabled) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

        var bestLocation: Location? = null

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                    bestLocation = location
                }

                if (location.hasAccuracy() && location.accuracy <= minAccuracyMeters) {
                    locationManager.removeUpdates(this)
                    createAndSubmitIncident(type, description, location)
                }
            }
        }

        locationManager.requestLocationUpdates(
            provider,
            1000L,
            1f,
            listener
        )

        Handler(Looper.getMainLooper()).postDelayed({
            locationManager.removeUpdates(listener)

            val fallbackLocation =
                bestLocation
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (fallbackLocation != null) {
                createAndSubmitIncident(type, description, fallbackLocation)
            } else {
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Unable to get accurate location", Toast.LENGTH_LONG).show()
            }
        }, locationTimeoutMs)
    }

    private fun createAndSubmitIncident(
        type: String,
        description: String,
        location: Location
    ) {
        val request = CreateIncidentRequest(
            incident_type = type,
            description = description,
            latitude = location.latitude,
            longitude = location.longitude
        )

        submitIncident(request)
    }

    private fun submitIncident(request: CreateIncidentRequest) {
        ApiClient.getClient(this).createIncident(request)
            .enqueue(object : Callback<GenericResponse> {

                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    btnSubmit.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@ReportIncidentActivity,
                            "Incident reported successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ReportIncidentActivity,
                            response.body()?.message ?: "Failed to report incident",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    btnSubmit.isEnabled = true
                    Toast.makeText(
                        this@ReportIncidentActivity,
                        t.message ?: "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}