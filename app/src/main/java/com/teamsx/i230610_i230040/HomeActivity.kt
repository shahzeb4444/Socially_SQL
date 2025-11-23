package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.utils.UserPreferences
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DEST = "start_dest"
    }

    private val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Wire bottom nav to controller once
        bottom.setupWithNavController(navController)

        // Intercept only the center (+) item; delegate the rest to NavigationUI
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_create -> {
                    startActivity(Intent(this, CreatePostActivity::class.java))
                    // return false so selection stays on current tab
                    false
                }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Handle deep link / extras on first launch
        handleProfileDeepLink(intent, navController, bottom)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, 0)
            insets
        }

        // Load and set the profile icon from UserPreferences (Base64)
        loadProfileIconIntoBottomNav(bottom)

        // Cleanup expired stories
        cleanupExpiredStories()
    }

    private fun cleanupExpiredStories() {
        lifecycleScope.launch {
            try {
                RetrofitInstance.apiService.cleanupExpiredStories()
                // Silent cleanup - no need to show result to user
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // keep latest extras available via getIntent()
        setIntent(intent)

        // We already have the layout set, just fetch the existing NavController and BottomNav
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Handle deep link / extras when Activity is re-used (singleTop / clearTop)
        handleProfileDeepLink(intent, navController, bottom)
    }

    // ===== Helpers =====

    /**
     * Handles intents that request opening another user's profile.
     * Works on both fresh starts (onCreate) and reuses (onNewIntent).
     */
    private fun handleProfileDeepLink(
        intent: Intent,
        navController: androidx.navigation.NavController,
        bottom: BottomNavigationView
    ) {
        // Handle notification clicks to open NotificationsYouFragment
        if (intent.getBooleanExtra("open_notifications", false)) {
            val notificationType = intent.getStringExtra("notification_type")

            // Navigate to notifications fragment
            navController.navigate(R.id.nav_notifications)
            bottom.selectedItemId = R.id.nav_notifications

            // Clear the intent extras
            intent.removeExtra("open_notifications")
            intent.removeExtra("notification_type")
            return
        }

        if (intent.getBooleanExtra("open_user_profile", false)) {
            val userId = intent.getStringExtra("user_id")
            val username = intent.getStringExtra("username")

            if (!userId.isNullOrEmpty() && !username.isNullOrEmpty()) {
                val args = Bundle().apply {
                    putString("user_id", userId)
                    putString("username", username)
                }
                navController.navigate(R.id.other_user_profile, args)

                // prevent re-triggering on configuration changes
                intent.removeExtra("open_user_profile")
                intent.removeExtra("user_id")
                intent.removeExtra("username")
            }
        } else {
            // Optional: open a specific tab when launched with an extra
            intent.getIntExtra(EXTRA_START_DEST, 0)
                .takeIf { it != 0 }
                ?.let { destId -> bottom.selectedItemId = destId }
        }
    }

    private fun loadProfileIconIntoBottomNav(bottom: BottomNavigationView) {
        // disable tint so we show real colors
        bottom.itemIconTintList = null

        val user = userPreferences.getUser()
        if (user == null || user.profileImageUrl.isNullOrEmpty()) {
            val ph = BitmapFactory.decodeResource(resources, R.drawable.profile_login_splash_small)
            setProfileIconBitmap(bottom, ph)
            return
        }

        try {
            val base64String = if (user.profileImageUrl.startsWith("data:")) {
                user.profileImageUrl.substringAfter("base64,")
            } else {
                user.profileImageUrl
            }

            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            if (bitmap != null) {
                setProfileIconBitmap(bottom, bitmap)
            } else {
                val ph = BitmapFactory.decodeResource(resources, R.drawable.profile_login_splash_small)
                setProfileIconBitmap(bottom, ph)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val ph = BitmapFactory.decodeResource(resources, R.drawable.profile_login_splash_small)
            setProfileIconBitmap(bottom, ph)
        }
    }

    private fun setProfileIconBitmap(bottom: BottomNavigationView, bitmap: Bitmap) {
        val item = bottom.menu.findItem(R.id.nav_profile)
        item.icon = circularIconFrom(bitmap, sizeDp = 24)
    }

    private fun dp(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun circularIconFrom(src: Bitmap, sizeDp: Int = 24): Drawable {
        val size = dp(sizeDp)
        val scaled = Bitmap.createScaledBitmap(src, size, size, true)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        return BitmapDrawable(resources, output)
    }
}
