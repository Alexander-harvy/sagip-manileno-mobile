package com.sagip.manileno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.network.ApiClient
import com.sagip.manileno.network.CitizenLoginRequest
import com.sagip.manileno.network.LoginResponse
import com.sagip.manileno.network.ResponderLoginRequest
import com.sagip.manileno.ui.citizen.CitizenDashboardActivity
import com.sagip.manileno.ui.responder.ResponderDashboardActivity
import com.sagip.manileno.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var inputIdentifier: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        val token = tokenManager.getToken()
        val role = tokenManager.getRole()

        if (token != null && role != null) {
            routeByRole(role)
            finish()
            return
        }

        inputIdentifier = findViewById(R.id.inputIdentifier)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val identifier = inputIdentifier.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            loginCitizenFirst(identifier, password)
        }
    }

    private fun loginCitizenFirst(identifier: String, password: String) {
        val request = CitizenLoginRequest(
            contact_no = identifier,
            password = password
        )

        ApiClient.getClient(this).loginCitizen(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    val data = response.body()?.data
                    val token = data?.token
                    val user = data?.user

                    if (response.isSuccessful && token != null && user != null) {
                        tokenManager.saveToken(token)
                        tokenManager.saveRole("user")
                        tokenManager.saveUser(
                            "${user.first_name} ${user.last_name}",
                            user.contact_no
                        )

                        routeByRole("user")
                        finish()
                    } else {
                        loginResponderFallback(identifier, password)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    loginResponderFallback(identifier, password)
                }
            })
    }

    private fun loginResponderFallback(identifier: String, password: String) {
        val request = ResponderLoginRequest(
            employee_no = identifier,
            password = password
        )

        ApiClient.getClient(this).loginResponder(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    btnLogin.isEnabled = true

                    val data = response.body()?.data
                    val token = data?.token
                    val responder = data?.responder

                    if (response.isSuccessful && token != null && responder != null) {
                        tokenManager.saveToken(token)
                        tokenManager.saveRole("responder")
                        tokenManager.saveUser(
                            "${responder.first_name} ${responder.last_name}",
                            responder.contact_no
                        )

                        routeByRole("responder")
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid login credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        t.message ?: "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun routeByRole(role: String) {
        val intent = when (role) {
            "user" -> Intent(this, CitizenDashboardActivity::class.java)
            "responder" -> Intent(this, ResponderDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
    }
}