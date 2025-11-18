package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.graphics.BitmapFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_START_DEST = "start_dest"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

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

        // Load and set the profile icon from Firebase (Base64)
        loadProfileIconIntoBottomNav(bottom)
        cleanExpiredStories()


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
    private fun cleanExpiredStories() {
        val currentTime = System.currentTimeMillis()

        db.child("stories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key ?: continue

                    for (storySnap in userSnap.children) {
                        val story = storySnap.getValue(Story::class.java)
                        if (story != null && currentTime > story.expiresAt) {
                            // Delete expired story
                            db.child("stories").child(userId).child(story.storyId).removeValue()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Silently fail
            }
        })
    }

    private fun loadProfileIconIntoBottomNav(bottom: BottomNavigationView) {
        // disable tint so we show real colors
        bottom.itemIconTintList = null

        val uid = auth.currentUser?.uid
        if (uid == null) {
            val ph = BitmapFactory.decodeResource(resources, R.drawable.profile_login_splash_small)
            setProfileIconBitmap(bottom, ph)
            return
        }

        db.child("users").child(uid).child("photoBase64").get()
            .addOnSuccessListener { snap ->
                val b64 = snap.getValue(String::class.java)
                if (!b64.isNullOrEmpty()) {
                    val bmp = ImageUtils.loadBase64ImageOptimized(b64, 100)
                    if (bmp != null) {
                        setProfileIconBitmap(bottom, bmp)
                        return@addOnSuccessListener
                    }
                }
                val ph = BitmapFactory.decodeResource(resources, R.drawable.profile_login_splash_small)
                setProfileIconBitmap(bottom, ph)
            }
            .addOnFailureListener {
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
