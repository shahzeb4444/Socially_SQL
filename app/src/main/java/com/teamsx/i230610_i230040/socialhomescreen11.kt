package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapShader
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class socialhomescreen11 : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var usernameEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var updateBtn: Button

    private var currentPhotoBase64: String? = null
    private var hasPhotoChanged = false

    // Image picker for profile photo
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                val resized = resizeBitmap(bitmap, 512)
                currentPhotoBase64 = bitmapToBase64(resized, 70)

                // Display the new photo
                val circularBitmap = getCircularBitmap(resized)
                profileImageView.setImageBitmap(circularBitmap)
                hasPhotoChanged = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen11)

        // Initialize views
        profileImageView = findViewById(R.id.profileicon)
        usernameEditText = findViewById(R.id.usernameedittext)
        bioEditText = findViewById(R.id.bioedittext)
        updateBtn = findViewById(R.id.updatebtn)

        val cancelText = findViewById<TextView>(R.id.canceltext)
        val changePhotoText = findViewById<TextView>(R.id.changeprofilephoto)

        // Cancel button
        cancelText.setOnClickListener {
            returnToProfile()
        }

        // Change profile photo
        changePhotoText.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Update button
        updateBtn.setOnClickListener {
            updateProfile()
        }

        // Load current profile data
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state
        updateBtn.isEnabled = false

        db.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    displayCurrentProfile(profile)
                } else {
                    Toast.makeText(this@socialhomescreen11, "Profile not found", Toast.LENGTH_SHORT).show()
                }
                updateBtn.isEnabled = true
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@socialhomescreen11,
                    "Failed to load profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateBtn.isEnabled = true
            }
        })
    }

    private fun displayCurrentProfile(profile: UserProfile) {
        // Set username
        usernameEditText.setText(profile.username)

        // Set bio
        bioEditText.setText(profile.bio)

        // Store current photo
        currentPhotoBase64 = profile.photoBase64

        // Load profile photo
        if (!profile.photoBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(profile.photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val circularBitmap = getCircularBitmap(bitmap)
                    profileImageView.setImageBitmap(circularBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                profileImageView.setImageResource(R.drawable.profile_login_splash)
            }
        } else {
            profileImageView.setImageResource(R.drawable.profile_login_splash)
        }
    }

    private fun updateProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val newUsername = usernameEditText.text.toString().trim()
        val newBio = bioEditText.text.toString().trim()

        // Validate username
        if (newUsername.isEmpty()) {
            usernameEditText.error = "Username cannot be empty"
            usernameEditText.requestFocus()
            return
        }

        if (newUsername.length < 3) {
            usernameEditText.error = "Username must be at least 3 characters"
            usernameEditText.requestFocus()
            return
        }

        // Disable button while updating
        updateBtn.isEnabled = false
        updateBtn.text = "Updating..."

        // First, get the current profile to preserve other fields
        db.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentProfile = snapshot.getValue(UserProfile::class.java)
                if (currentProfile != null) {
                    // Create updated profile with new values
                    val updatedProfile = currentProfile.copy(
                        username = newUsername,
                        bio = newBio,
                        photoBase64 = if (hasPhotoChanged) currentPhotoBase64 else currentProfile.photoBase64
                    )

                    // Update in database
                    db.child("users").child(uid).setValue(updatedProfile.toMap())
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@socialhomescreen11,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            returnToProfile()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@socialhomescreen11,
                                "Failed to update profile: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateBtn.isEnabled = true
                            updateBtn.text = "Update"
                        }
                } else {
                    Toast.makeText(this@socialhomescreen11, "Profile not found", Toast.LENGTH_SHORT).show()
                    updateBtn.isEnabled = true
                    updateBtn.text = "Update"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@socialhomescreen11,
                    "Failed to update profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateBtn.isEnabled = true
                updateBtn.text = "Update"
            }
        })
    }

    private fun returnToProfile() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_profile)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    // Helper functions for image processing
    private fun resizeBitmap(source: Bitmap, maxSize: Int): Bitmap {
        if (source.width <= maxSize && source.height <= maxSize) {
            return source
        }

        val ratio = if (source.width > source.height) {
            maxSize.toFloat() / source.width
        } else {
            maxSize.toFloat() / source.height
        }

        val width = (source.width * ratio).toInt()
        val height = (source.height * ratio).toInt()

        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = 300 // Size in pixels
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }
}