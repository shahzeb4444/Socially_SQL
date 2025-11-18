package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen10 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen10)


        val followbtn = findViewById<ImageView>(R.id.feedbutton1)
        val backarrow = findViewById<ImageView>(R.id.leftarrow)

        // ---- Bottom nav → HomeActivity with selected tab ----





        // Keep existing flows
        followbtn.setOnClickListener {
            startActivity(Intent(this, socialhomescreen9::class.java))
            finish()
        }

        backarrow.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }
    }
}
