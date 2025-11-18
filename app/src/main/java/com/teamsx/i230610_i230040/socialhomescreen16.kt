package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen16 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen16)
        val canceltext = findViewById<TextView>(R.id.leftarrowlogo)
        val nexttext = findViewById<TextView>(R.id.pluslogo)
        canceltext.setOnClickListener {
            val intent = Intent(this, socialhomescreen11::class.java)
            startActivity(intent)
            finish()
        }
        nexttext.setOnClickListener {
            val intent = Intent(this, socialhomescreen11::class.java)
            startActivity(intent)
            finish()
        }
    }
}