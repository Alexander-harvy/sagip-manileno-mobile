package com.sagip.manileno.ui.responder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.sagip.manileno.MainActivity
import com.sagip.manileno.R
import com.sagip.manileno.utils.TokenManager
import android.widget.TextView

class ResponderDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responder_dashboard)

        val tokenManager = TokenManager(this)
        val name = tokenManager.getName()
        val contact = tokenManager.getContact()

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvContact = findViewById<TextView>(R.id.tvContact)

        tvName.text = name
        tvContact.text = contact

        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            val tokenManager = TokenManager(this)
            tokenManager.clearSession()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}