package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.teamsx.i230610_i230040.utils.UserPreferences

class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // your splash layout

        // Wait for 3–5 seconds (adjust as per your design)
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if user is logged in using UserPreferences (not Firebase Auth)
            if (userPreferences.isLoggedIn()) {
                // User already logged in → show profile login screen
                val intent = Intent(this, login_splash::class.java)
                startActivity(intent)
            } else {
                // Not logged in → go to main login screen
                val intent = Intent(this, mainlogin::class.java)
                startActivity(intent)
            }

            finish() // so user can't go back to splash

        }, 3000) // 3 seconds
    }
}
