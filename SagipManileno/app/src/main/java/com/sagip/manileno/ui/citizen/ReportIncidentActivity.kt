package com.sagip.manileno.ui.citizen

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.CreateIncidentRequest
import com.sagip.manileno.network.GenericResponse
import com.sagip.manileno.offline.OfflineIncident
import com.sagip.manileno.offline.OfflineIncidentDatabase
import com.sagip.manileno.offline.OfflineSyncManager
import com.sagip.manileno.utils.Constants
import com.sagip.manileno.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportIncidentActivity : AppCompatActivity() {

    private lateinit var tvIncidentType: TextView
    private lateinit var inputDescription: EditText
    private lateinit var tvGpsStatus: TextView
    private lateinit var btnSos: Button

    private var selectedIncidentType = ""

    private val minAccuracyMeters = 25f
    private val locationTimeoutMs = 15000L
    private val sosHoldDurationMs = 3000L

    private val handler = Handler(Looper.getMainLooper())
    private var sosTriggered = false

    private var pendingOfflineRequest: CreateIncidentRequest? = null

    private val sosRunnable = Runnable {
        sosTriggered = true

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

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                updateGpsStatusPreview()

                if (sosTriggered) {
                    submitIncidentWithLocation()
                }
            } else {
                resetSosButton()
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingOfflineRequest

            if (request == null) {
                resetSosButton()
                return@registerForActivityResult
            }

            val smsSent = if (granted) {
                sendSmsFallback(request)
            } else {
                false
            }

            Toast.makeText(
                this,
                if (smsSent) {
                    "Offline alert saved. SMS send request submitted."
                } else {
                    "Offline alert saved, but SMS was not sent."
                },
                Toast.LENGTH_LONG
            ).show()

            pendingOfflineRequest = null
            resetSosButton()
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_incident)

        tvIncidentType = findViewById(R.id.tvIncidentType)
        inputDescription = findViewById(R.id.inputDescription)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        btnSos = findViewById(R.id.btnSos)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnChangeType = findViewById<Button>(R.id.btnChangeType)

        selectedIncidentType = intent.getStringExtra("incident_type") ?: ""
        updateIncidentTypeLabel()

        btnBack.setOnClickListener {
            finish()
        }

        btnChangeType.setOnClickListener {
            showIncidentTypeDialog()
        }

        btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sosTriggered = false
                    btnSos.text = "HOLD..."
                    handler.postDelayed(sosRunnable, sosHoldDurationMs)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(sosRunnable)

                    if (!sosTriggered) {
                        btnSos.text = "SOS"
                        Toast.makeText(
                            this,
                            "Hold SOS for 3 seconds to submit",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    true
                }

                else -> false
            }
        }

        if (hasLocationPermission()) {
            updateGpsStatusPreview()
        } else {
            tvGpsStatus.text = "GPS PERMISSION REQUIRED"
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        if (NetworkUtils.hasInternetConnection(this)) {
            OfflineSyncManager.syncOfflineIncidents(this)
        }
    }

    private fun updateIncidentTypeLabel() {
        tvIncidentType.text = if (selectedIncidentType.isNotEmpty()) {
            "INCIDENT TYPE: ${selectedIncidentType.uppercase()}"
        } else {
            "INCIDENT TYPE: NOT SELECTED"
        }
    }

    private fun showIncidentTypeDialog() {
        val incidentTypes = arrayOf("Police", "Medic", "Fire")

        AlertDialog.Builder(this)
            .setTitle("Select Incident Type")
            .setItems(incidentTypes) { _, which ->
                selectedIncidentType = incidentTypes[which]
                updateIncidentTypeLabel()
            }
            .show()
    }

    private fun normalizeIncidentType(type: String): String {
        return when (type.trim().lowercase()) {
            "medical" -> "medic"
            "medic" -> "medic"
            "police" -> "police"
            "fire" -> "fire"
            else -> type.trim().lowercase()
        }
    }

    private fun updateGpsStatusPreview() {
        if (!hasLocationPermission()) {
            tvGpsStatus.text = "GPS PERMISSION REQUIRED"
            return
        }

        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            tvGpsStatus.text = if (location != null && location.hasAccuracy()) {
                "GPS READY : ACCURACY ~ ${location.accuracy.toInt()}m"
            } else {
                "GPS READY : WAITING FOR ACCURACY"
            }
        } catch (_: SecurityException) {
            tvGpsStatus.text = "GPS PERMISSION REQUIRED"
        }
    }

    private fun submitIncidentWithLocation() {
        val description = inputDescription.text.toString().trim()

        if (selectedIncidentType.isEmpty()) {
            resetSosButton()
            Toast.makeText(this, "Please select incident type", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            resetSosButton()
            Toast.makeText(this, "Please enter incident description", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasLocationPermission()) {
            resetSosButton()
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            return
        }

        btnSos.isEnabled = false
        btnSos.text = "SENDING..."

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            resetSosButton()
            Toast.makeText(this, "Please turn on Location/GPS", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (
                lastKnownLocation != null &&
                lastKnownLocation.hasAccuracy() &&
                lastKnownLocation.accuracy <= minAccuracyMeters
            ) {
                createAndHandleIncident(description, lastKnownLocation)
                return
            }

            val provider = if (gpsEnabled) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            var bestLocation: Location? = lastKnownLocation
            var hasSubmitted = false

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (hasSubmitted) return

                    if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                        bestLocation = location

                        if (location.hasAccuracy()) {
                            tvGpsStatus.text =
                                "GPS READY : ACCURACY ~ ${location.accuracy.toInt()}m"
                        }
                    }

                    if (location.hasAccuracy() && location.accuracy <= minAccuracyMeters) {
                        hasSubmitted = true
                        locationManager.removeUpdates(this)
                        createAndHandleIncident(description, location)
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
                if (hasSubmitted) return@postDelayed

                hasSubmitted = true
                locationManager.removeUpdates(listener)

                val fallbackLocation =
                    bestLocation
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (fallbackLocation != null) {
                    createAndHandleIncident(description, fallbackLocation)
                } else {
                    resetSosButton()
                    Toast.makeText(this, "Unable to get accurate location", Toast.LENGTH_LONG)
                        .show()
                }
            }, locationTimeoutMs)

        } catch (_: SecurityException) {
            resetSosButton()
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAndHandleIncident(
        description: String,
        location: Location
    ) {
        val request = CreateIncidentRequest(
            incident_type = normalizeIncidentType(selectedIncidentType),
            description = description,
            latitude = location.latitude,
            longitude = location.longitude,
            source = "api"
        )

        handleIncidentSubmission(request)
    }

    private fun handleIncidentSubmission(request: CreateIncidentRequest) {
        if (NetworkUtils.hasInternetConnection(this)) {
            submitIncident(request)
        } else {
            saveOfflineIncident(request)
            pendingOfflineRequest = request

            if (hasSmsPermission()) {
                val smsSent = sendSmsFallback(request)

                Toast.makeText(
                    this,
                    if (smsSent) {
                        "Offline alert saved. SMS send request submitted."
                    } else {
                        "Offline alert saved, but SMS failed."
                    },
                    Toast.LENGTH_LONG
                ).show()

                pendingOfflineRequest = null
                resetSosButton()
                finish()
            } else {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    private fun saveOfflineIncident(request: CreateIncidentRequest) {
        val db = OfflineIncidentDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            db.offlineIncidentDao().insertIncident(
                OfflineIncident(
                    incident_type = request.incident_type,
                    description = request.description,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    source = "offline_sync",
                    sms_sent = hasSmsPermission()
                )
            )
        }
    }

    private fun sendSmsFallback(request: CreateIncidentRequest): Boolean {
        return try {
            val message =
                "SAGIP ALERT | ${request.incident_type} | ${request.description} | " +
                        "${request.latitude},${request.longitude}"

            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                Constants.EMERGENCY_SMS_NUMBER,
                null,
                parts,
                null,
                null
            )

            true
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Offline report saved, but SMS failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    private fun submitIncident(request: CreateIncidentRequest) {
        ApiClient.getClient(this).createIncident(request)
            .enqueue(object : Callback<GenericResponse> {

                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    resetSosButton()

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
                    saveOfflineIncident(request)
                    pendingOfflineRequest = request

                    if (hasSmsPermission()) {
                        val smsSent = sendSmsFallback(request)

                        Toast.makeText(
                            this@ReportIncidentActivity,
                            if (smsSent) {
                                "Network failed. Offline alert saved. SMS send request submitted."
                            } else {
                                "Network failed. Offline alert saved, but SMS failed."
                            },
                            Toast.LENGTH_LONG
                        ).show()

                        pendingOfflineRequest = null
                        resetSosButton()
                        finish()
                    } else {
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                }
            })
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

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun resetSosButton() {
        btnSos.isEnabled = true
        btnSos.text = "SOS"
    }
}