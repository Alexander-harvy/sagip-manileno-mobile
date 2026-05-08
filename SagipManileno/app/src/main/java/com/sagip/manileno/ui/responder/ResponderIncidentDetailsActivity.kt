package com.sagip.manileno.ui.responder

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.GenericResponse
import com.sagip.manileno.network.UpdateStatusRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResponderIncidentDetailsActivity : AppCompatActivity() {

    private var incidentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responder_incident_details)

        incidentId = intent.getIntExtra("incident_id", -1)

        val type = intent.getStringExtra("incident_type") ?: "Incident"
        val description = intent.getStringExtra("description") ?: ""
        val status = intent.getStringExtra("status") ?: "unknown"

        val tvIncidentTitle = findViewById<TextView>(R.id.tvIncidentTitle)
        val tvIncidentDescription = findViewById<TextView>(R.id.tvIncidentDescription)
        val tvIncidentStatus = findViewById<TextView>(R.id.tvIncidentStatus)

        val btnEnRoute = findViewById<Button>(R.id.btnEnRoute)
        val btnOnScene = findViewById<Button>(R.id.btnOnScene)
        val btnResolved = findViewById<Button>(R.id.btnResolved)

        tvIncidentTitle.text = type
        tvIncidentDescription.text = description
        tvIncidentStatus.text = "Status: $status"

        btnEnRoute.setOnClickListener {
            updateStatus("en_route")
        }

        btnOnScene.setOnClickListener {
            updateStatus("on_scene")
        }

        btnResolved.setOnClickListener {
            updateStatus("resolved")
        }
    }

    private fun updateStatus(status: String) {
        if (incidentId == -1) {
            Toast.makeText(this, "Invalid incident", Toast.LENGTH_SHORT).show()
            return
        }

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
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@ResponderIncidentDetailsActivity,
                            "Status updated: $status",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ResponderIncidentDetailsActivity,
                            "Failed to update status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(
                        this@ResponderIncidentDetailsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}