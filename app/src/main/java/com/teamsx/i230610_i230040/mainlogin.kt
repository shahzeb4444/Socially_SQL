package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class mainlogin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mainlogin)

        // Views (adjust IDs if your XML uses different ones)
        val emailEt    = findViewById<EditText>(R.id.usernamefield)       // your "Username" field should be email
        val passwordEt = findViewById<EditText>(R.id.passwordfield)
        val loginBtn   = findViewById<Button>(R.id.loginbutton)
        val signupTv   = findViewById<TextView>(R.id.signup)
        val back       = findViewById<ImageView>(R.id.back)

        auth = FirebaseAuth.getInstance()

        back.setOnClickListener { finish() }

        // Go to Sign Up
        signupTv.setOnClickListener {
            startActivity(Intent(this, second_page::class.java))
        }

        // Log in
        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass  = passwordEt.text.toString()

            // basic validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEt.error = "Enter a valid email"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                passwordEt.error = "Min 6 characters"
                return@setOnClickListener
            }

            // disable to prevent double taps
            loginBtn.isEnabled = false

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                loginBtn.isEnabled = true
                if (task.isSuccessful) {
                    // go to your main/home screen
                    val intent = Intent(this, login_splash::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}