package com.teamsx.i230610_i230040

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.network.SaveFCMTokenRequest
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.utils.NotificationPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // Request notification permission (Android 13+)
        NotificationPermissionHelper.requestNotificationPermission(this)

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

            // Get and save FCM token
            getFCMTokenAndSave(user.uid)

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

    private fun getFCMTokenAndSave(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "FCM Token: $token")

                // Save locally
                userPreferences.saveFCMToken(token)

                // Save to server
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = SaveFCMTokenRequest(uid, token)
                        val response = RetrofitInstance.apiService.saveFCMToken(request)

                        Log.d("FCM", "Response code: ${response.code()}")
                        Log.d("FCM", "Response successful: ${response.isSuccessful}")

                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body == null) {
                                Log.e("FCM", "Response body is null")
                            } else {
                                Log.d("FCM", "Response success: ${body.success}, error: ${body.error}")
                                if (body.success) {
                                    Log.d("FCM", "Token saved to server successfully")
                                } else {
                                    Log.e("FCM", "Failed to save token: ${body.error}")
                                }
                            }
                        } else {
                            Log.e("FCM", "HTTP error: ${response.code()}, message: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FCM", "Error saving token: ${e.message}", e)
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("FCM", "Failed to get FCM token", task.exception)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NotificationPermissionHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Notifications", "POST_NOTIFICATIONS permission granted")
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("Notifications", "POST_NOTIFICATIONS permission denied")
                Toast.makeText(this, "Please enable notifications in settings to receive updates", Toast.LENGTH_LONG).show()
            }
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
