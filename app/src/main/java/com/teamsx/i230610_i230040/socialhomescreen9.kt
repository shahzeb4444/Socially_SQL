package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class socialhomescreen9 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen9)

        val bottomnavsearch = findViewById<ImageView>(R.id.bottomnavsearch)
        val bottomnavhome = findViewById<ImageView>(R.id.bottomnavhome)
        val bottomnavcreate = findViewById<ImageView>(R.id.bottomnavcreate)
        val bottomnavlike = findViewById<ImageView>(R.id.bottomnavlike)
        val bottomnavprofile = findViewById<ImageView>(R.id.bottomnavicon)
        val unfollowbtn = findViewById<ImageView>(R.id.feedbutton1)
        val backarrow = findViewById<ImageView>(R.id.leftarrow)

        // ---- Bottom nav → HomeActivity with selected tab ----
        bottomnavhome.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }

        bottomnavsearch.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_search)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }

        bottomnavcreate.setOnClickListener {
            // Keep behavior: open your existing create activity
            startActivity(
                Intent(this, socialhomescreen17::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }

        bottomnavlike.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_notifications)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }

        bottomnavprofile.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_profile)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }

        // Keep existing flows
        unfollowbtn.setOnClickListener {
            startActivity(Intent(this, socialhomescreen10::class.java))
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
