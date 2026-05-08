package com.sagip.manileno.ui.responder

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.ResponderIncident
import com.sagip.manileno.network.ResponderIncidentsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssignedIncidentsActivity : AppCompatActivity() {

    private lateinit var listAssignedIncidents: ListView
    private var incidents: List<ResponderIncident> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assigned_incidents)

        listAssignedIncidents = findViewById(R.id.listAssignedIncidents)

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            fetchAssignedIncidents()
        }
    }

    private fun fetchAssignedIncidents() {
        ApiClient.getClient(this).getResponderIncidents()
            .enqueue(object : Callback<ResponderIncidentsResponse> {

                override fun onResponse(
                    call: Call<ResponderIncidentsResponse>,
                    response: Response<ResponderIncidentsResponse>
                ) {

                    if (response.isSuccessful && response.body()?.success == true) {
                        incidents = response.body()?.data ?: emptyList()

                        val incidents = response.body()?.data ?: emptyList()

                        val displayList = incidents.map {
                            "${it.incident_type}\n${it.status}\n${it.description ?: ""}"
                        }


                        listAssignedIncidents.adapter = ArrayAdapter(
                            this@AssignedIncidentsActivity,
                            android.R.layout.simple_list_item_1,
                            displayList
                        )

                        listAssignedIncidents.setOnItemClickListener { _, _, position, _ ->
                            val selected = incidents[position]

                            val intent = Intent(
                                this@AssignedIncidentsActivity,
                                ResponderIncidentDetailsActivity::class.java
                            )
                            incidents.forEach {
                                android.util.Log.d("ASSIGNED_DEBUG", "ID: ${it.incident_id}, STATUS: ${it.status}")
                            }
                            intent.putExtra("incident_id", selected.incident_id)
                            intent.putExtra("incident_type", selected.incident_type)
                            intent.putExtra("description", selected.description)
                            intent.putExtra("latitude", selected.latitude ?: 0.0)
                            intent.putExtra("longitude", selected.longitude ?: 0.0)
                            intent.putExtra("status", selected.latest_status ?: selected.status ?: "unknown")

                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@AssignedIncidentsActivity,
                            "Failed to load assigned incidents",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponderIncidentsResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AssignedIncidentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        fetchAssignedIncidents()
    }
}