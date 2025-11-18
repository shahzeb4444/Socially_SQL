package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen3)

        val filledcancel = findViewById<ImageView>(R.id.filledcancel)
        val cleartext = findViewById<TextView>(R.id.cleartext)

        val goToSearchTab: () -> Unit = {
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_search)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        filledcancel.setOnClickListener { goToSearchTab() }
        cleartext.setOnClickListener { goToSearchTab() }
    }
}
