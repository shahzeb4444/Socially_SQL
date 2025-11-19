package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teamsx.i230610_i230040.network.Resource

class mainlogin : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mainlogin)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Views
        val emailEt = findViewById<EditText>(R.id.usernamefield)
        val passwordEt = findViewById<EditText>(R.id.passwordfield)
        val loginBtn = findViewById<Button>(R.id.loginbutton)
        val signupTv = findViewById<TextView>(R.id.signup)
        val back = findViewById<ImageView>(R.id.back)

        // Create ProgressBar programmatically if not in XML
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }

        back.setOnClickListener { finish() }

        // Go to Sign Up
        signupTv.setOnClickListener {
            startActivity(Intent(this, second_page::class.java))
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            loginBtn.isEnabled = !isLoading
            loginBtn.text = if (isLoading) "Logging in..." else "Login"
        }

        // Observe login result
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to login splash screen
                    val intent = Intent(this, login_splash::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, result.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {
                    // Loading state handled by isLoading observer
                }
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Log in button click
        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass = passwordEt.text.toString()

            // Validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter a valid email"
                emailEt.requestFocus()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                passwordEt.error = "Password must be at least 6 characters"
                passwordEt.requestFocus()
                return@setOnClickListener
            }

            // Call ViewModel login
            viewModel.login(email, pass)
        }
    }
}