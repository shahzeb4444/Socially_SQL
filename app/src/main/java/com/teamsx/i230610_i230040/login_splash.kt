package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teamsx.i230610_i230040.utils.UserPreferences

class login_splash : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_splash)

        // Initialize ViewModel and UserPreferences
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        userPreferences = UserPreferences(this)

        val lgnbtn = findViewById<Button>(R.id.loginbutton)
        val switchaccount = findViewById<TextView>(R.id.switchaccounts)
        val profilename = findViewById<TextView>(R.id.profilename)
        val profilepic = findViewById<ImageView>(R.id.profilepic)

        // 1) If not logged in, go to login
        if (!userPreferences.isLoggedIn()) {
            startActivity(Intent(this, mainlogin::class.java))
            finish()
            return
        }

        // 2) Load user from SharedPreferences and fill UI
        val user = userPreferences.getUser()
        if (user != null) {
            profilename.text = user.username

            // Load profile picture
            if (!user.profileImageUrl.isNullOrEmpty()) {
                try {
                    // Check if it's a Base64 string or URL
                    if (user.profileImageUrl.startsWith("data:") || !user.profileImageUrl.startsWith("http")) {
                        // It's Base64
                        val base64String = if (user.profileImageUrl.startsWith("data:")) {
                            user.profileImageUrl.substringAfter("base64,")
                        } else {
                            user.profileImageUrl
                        }
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profilepic.setImageBitmap(getCircularBitmap(bitmap))
                    } else {
                        // It's a URL - you can use Glide here later
                        profilepic.setImageResource(R.drawable.whitecircle)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    profilepic.setImageResource(R.drawable.whitecircle)
                }
            } else {
                profilepic.setImageResource(R.drawable.whitecircle)
            }
        } else {
            Toast.makeText(this, "Couldn't load profile.", Toast.LENGTH_SHORT).show()
        }

        // 3) Buttons
        lgnbtn.setOnClickListener {
            // Go to HomeActivity (BottomNav)
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
            }
            startActivity(intent)
            finish()
        }

        switchaccount.setOnClickListener {
            // Clear user data and go to login
            viewModel.logout()
            startActivity(Intent(this, mainlogin::class.java))
            finish()
        }

        val logoutText = findViewById<TextView>(R.id.logout)
        logoutText.setOnClickListener {
            viewModel.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, mainlogin::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            shader = android.graphics.BitmapShader(
                bitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return output
    }
}
