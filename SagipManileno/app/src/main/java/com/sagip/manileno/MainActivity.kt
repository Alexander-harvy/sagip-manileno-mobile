package com.sagip.manileno

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<Spinner>(R.id.spinnerRole)
        val inputIdentifier = findViewById<EditText>(R.id.inputIdentifier)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val roles = arrayOf("Citizen", "Responder")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        btnLogin.setOnClickListener {

            val identifier = inputIdentifier.text.toString()
            val password = inputPassword.text.toString()
            val selectedRole = spinner.selectedItem.toString()

            if (selectedRole == "Citizen") {
                loginCitizen(identifier, password)
            } else {
                loginResponder(identifier, password)
            }
        }
    }

    private fun loginCitizen(contactNo: String, password: String) {
        val request = CitizenLoginRequest(contact_no = contactNo, password = password)

        ApiClient.apiService.loginCitizen(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val token = response.body()?.data?.token
                        val user = response.body()?.data?.user

                        Log.d("LOGIN_UI", "Citizen token: $token")
                        Log.d("LOGIN_UI", "Citizen user: $user")
                    } else {
                        Toast.makeText(this@MainActivity, "Citizen Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loginResponder(employeeNo: String, password: String) {
        val request = ResponderLoginRequest(employee_no = employeeNo, password = password)

        ApiClient.apiService.loginResponder(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val token = response.body()?.data?.token
                        val responder = response.body()?.data?.responder

                        Log.d("LOGIN_UI", "Responder token: $token")
                        Log.d("LOGIN_UI", "Responder data: $responder")
                    } else {
                        Toast.makeText(this@MainActivity, "Responder Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}