package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen17 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen17)

        val cameraicon = findViewById<ImageView>(R.id.g4)
        val cancelicon = findViewById<ImageView>(R.id.cancelicon)

        cancelicon.setOnClickListener {
            // Go back to your new single-activity host with bottom navigation
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        cameraicon.setOnClickListener {
            val intent = Intent(this, socialhomescreen15::class.java)
            startActivity(intent)
        }
    }
}
