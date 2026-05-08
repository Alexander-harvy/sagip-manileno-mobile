package com.sagip.manileno.ui.citizen

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.R
import com.sagip.manileno.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IncidentDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_details)

        val listStatus = findViewById<ListView>(R.id.listStatus)
        val incidentId = intent.getIntExtra("incident_id", -1)

        ApiClient.getClient(this).getStatusHistory(incidentId)
            .enqueue(object : Callback<StatusHistoryResponse> {

                override fun onResponse(
                    call: Call<StatusHistoryResponse>,
                    response: Response<StatusHistoryResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {

                        val history = response.body()?.data ?: emptyList()

                        val display = history.map {
                            buildString {
                                append(it.status)
                                append("\n")
                                append(it.responder_name ?: "No responder yet")
                                append("\n")
                                append(it.created_at)
                            }
                        }

                        listStatus.adapter = ArrayAdapter(
                            this@IncidentDetailsActivity,
                            android.R.layout.simple_list_item_1,
                            display
                        )
                    }
                }

                override fun onFailure(call: Call<StatusHistoryResponse>, t: Throwable) {
                    Toast.makeText(this@IncidentDetailsActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}