package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class CreatePostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_post)

        // Handle back press - CORRECT WAY
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        val btnPost = findViewById<LinearLayout>(R.id.btnPost)
        val btnStory = findViewById<LinearLayout>(R.id.btnStory)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)

        btnPost.setOnClickListener {
            startActivity(Intent(this, CreatePostScreen::class.java))
        }

        btnStory.setOnClickListener {
            startActivity(Intent(this, socialhomescreen15::class.java))
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}