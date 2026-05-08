package com.sagip.manileno.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.R
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.MyIncidentsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyIncidentsActivity : AppCompatActivity() {

    private lateinit var listIncidents: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_incidents)

        listIncidents = findViewById(R.id.listIncidents)

        fetchMyIncidents()
    }

    private fun fetchMyIncidents() {
        ApiClient.getClient(this).getMyIncidents()
            .enqueue(object : Callback<MyIncidentsResponse> {

                override fun onResponse(
                    call: Call<MyIncidentsResponse>,
                    response: Response<MyIncidentsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val incidents = response.body()?.data ?: emptyList()

                        val displayList = incidents.map {
                            val status = it.latest_status ?: it.status ?: "unknown"
                            "${it.incident_type}\n$status\n${it.description ?: ""}"
                        }

                        listIncidents.adapter = ArrayAdapter(
                            this@MyIncidentsActivity,
                            android.R.layout.simple_list_item_1,
                            displayList
                        )

                        listIncidents.setOnItemClickListener { _, _, position, _ ->
                            val incident = response.body()?.data?.get(position)

                            val intent = Intent(this@MyIncidentsActivity, IncidentDetailsActivity::class.java)
                            intent.putExtra("incident_id", incident?.incident_id)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@MyIncidentsActivity,
                            "Failed to load incidents",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MyIncidentsResponse>, t: Throwable) {
                    Toast.makeText(
                        this@MyIncidentsActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}