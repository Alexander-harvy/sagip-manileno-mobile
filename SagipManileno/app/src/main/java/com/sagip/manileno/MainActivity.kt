package com.sagip.manileno

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import com.sagip.manileno.utils.TokenManager
import com.sagip.manileno.ui.citizen.CitizenDashboardActivity
import com.sagip.manileno.ui.responder.ResponderDashboardActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tokenManager = TokenManager(this)

        val token = tokenManager.getToken()
        val role = tokenManager.getRole()

        if (token != null && role != null) {
            if (role == "user") {
                startActivity(Intent(this, CitizenDashboardActivity::class.java))
            } else {
                startActivity(Intent(this,ResponderDashboardActivity::class.java))
            }
            finish()
        }

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

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            if (selectedRole == "Citizen") {
                loginCitizen(identifier, password, btnLogin)
            } else {
                loginResponder(identifier, password, btnLogin)
            }
        }
    }

    private fun loginCitizen(contactNo: String, password: String, btnLogin: Button) {
        val request = CitizenLoginRequest(contact_no = contactNo, password = password)

        ApiClient.getClient(this).loginCitizen(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.isEnabled = true
                    if (response.isSuccessful) {
                        val token = response.body()?.data?.token

                        if (token != null) {
                            val tokenManager = TokenManager(this@MainActivity)
                            tokenManager.saveToken(token)
                            tokenManager.saveRole("user")

                            val user = response.body()?.data?.user

                            tokenManager.saveUser(
                                "${user?.first_name} ${user?.last_name}",
                                user?.contact_no ?: ""
                            )

                            startActivity(Intent(this@MainActivity, CitizenDashboardActivity::class.java))
                            finish()
                            return
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Citizen Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                }
            })
    }

    private fun loginResponder(employeeNo: String, password: String, btnLogin: Button) {
        val request = ResponderLoginRequest(employee_no = employeeNo, password = password)

        ApiClient.getClient(this).loginResponder(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.isEnabled = true
                    if (response.isSuccessful) {
                        btnLogin.isEnabled = true
                        val token = response.body()?.data?.token

                        if (token != null) {
                            val tokenManager = TokenManager(this@MainActivity)
                            tokenManager.saveToken(token)
                            tokenManager.saveRole("responder")

                            val responder = response.body()?.data?.responder

                            tokenManager.saveUser(
                                "${responder?.first_name} ${responder?.last_name}",
                                responder?.contact_no ?: ""
                            )

                            startActivity(Intent(this@MainActivity, ResponderDashboardActivity::class.java))
                            finish()
                            return
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Responder Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                }
            })
    }
}