package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen13 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen13)

        val cancelicon = findViewById<ImageView>(R.id.cancelicon)
        cancelicon.setOnClickListener {
            // Go back to HomeActivity with the Profile tab selected
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_profile)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }
    }
}
