package com.sagip.manileno.ui.responder

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var tvIncidentCount: TextView
    private lateinit var btnRefresh: Button

    private var incidents: List<ResponderIncident> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assigned_incidents)

        listAssignedIncidents = findViewById(R.id.listAssignedIncidents)
        tvIncidentCount = findViewById(R.id.tvIncidentCount)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            fetchAssignedIncidents()
        }

        listAssignedIncidents.setOnItemClickListener { _, _, position, _ ->

            val incident = incidents[position]

            val intent = Intent(
                this,
                ResponderIncidentDetailsActivity::class.java
            )

            intent.putExtra("incident_id", incident.incident_id)
            intent.putExtra("incident_type", incident.incident_type)
            intent.putExtra("description", incident.description ?: "")
            intent.putExtra("latitude", incident.latitude ?: 0.0)
            intent.putExtra("longitude", incident.longitude ?: 0.0)
            intent.putExtra(
                "status",
                incident.latest_status ?: incident.status ?: "unknown"
            )

            startActivity(intent)
        }

        fetchAssignedIncidents()
    }

    private fun fetchAssignedIncidents() {

        ApiClient.getClient(this)
            .getResponderIncidents()
            .enqueue(object : Callback<ResponderIncidentsResponse> {

                override fun onResponse(
                    call: Call<ResponderIncidentsResponse>,
                    response: Response<ResponderIncidentsResponse>
                ) {

                    if (
                        response.isSuccessful &&
                        response.body()?.success == true
                    ) {

                        incidents = response.body()?.data ?: emptyList()

                        tvIncidentCount.text =
                            "${incidents.size} incidents"

                        listAssignedIncidents.adapter =
                            IncidentAdapter(incidents)

                    } else {

                        Toast.makeText(
                            this@AssignedIncidentsActivity,
                            "Failed to load incidents",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ResponderIncidentsResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@AssignedIncidentsActivity,
                        t.message ?: "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    inner class IncidentAdapter(
        private val items: List<ResponderIncident>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?
        ): View {

            val incident = items[position]

            val container = LinearLayout(this@AssignedIncidentsActivity)

            container.orientation = LinearLayout.VERTICAL
            container.setPadding(32, 28, 32, 28)

            container.setBackgroundResource(android.R.color.darker_gray)

            val params = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT
            )

            container.layoutParams = params

            val typeText = TextView(this@AssignedIncidentsActivity)
            typeText.text = incident.incident_type
            typeText.textSize = 18f
            typeText.setTypeface(null, android.graphics.Typeface.BOLD)

            val statusText = TextView(this@AssignedIncidentsActivity)

            val status =
                incident.latest_status
                    ?: incident.status
                    ?: "unknown"

            statusText.text = formatStatus(status)

            val descriptionText = TextView(this@AssignedIncidentsActivity)

            descriptionText.text =
                incident.description ?: "No description"

            descriptionText.setPadding(0, 12, 0, 0)

            val openText = TextView(this@AssignedIncidentsActivity)

            openText.text = "OPEN INCIDENT"
            openText.gravity = Gravity.END
            openText.setPadding(0, 20, 0, 0)

            container.addView(typeText)
            container.addView(statusText)
            container.addView(descriptionText)
            container.addView(openText)

            return container
        }
    }

    private fun formatStatus(status: String): String {

        return status
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") {

                it.replaceFirstChar { char ->
                    char.uppercase()
                }
            }
    }
}