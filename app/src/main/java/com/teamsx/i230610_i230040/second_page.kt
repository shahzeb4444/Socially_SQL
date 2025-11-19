package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teamsx.i230610_i230040.network.Resource
import java.io.ByteArrayOutputStream

class second_page : AppCompatActivity() {

    private var passwordVisible = false
    private lateinit var viewModel: AuthViewModel

    private lateinit var avatarImage: ImageView
    private lateinit var cameraIcon: ImageView

    private var photoBase64: String? = null

    // Image picker → preview in circle → hide camera → make Base64
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        avatarImage.setImageURI(uri)
        avatarImage.visibility = View.VISIBLE
        cameraIcon.visibility = View.GONE

        contentResolver.openInputStream(uri)?.use { input ->
            val bmp = BitmapFactory.decodeStream(input)
            photoBase64 = toBase64(resize(bmp, 512), 80)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.second_page)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val back = findViewById<ImageView>(R.id.leftarrow)
        val avatarContainer = findViewById<FrameLayout>(R.id.whitecircle)
        avatarImage = findViewById(R.id.avatarImage)
        cameraIcon = findViewById(R.id.cameralogo)
        val eye = findViewById<ImageView>(R.id.eyeopen)
        val passEt = findViewById<EditText>(R.id.passwordtextfield)
        val createBtn = findViewById<Button>(R.id.createaccountbutton)

        back.setOnClickListener {
            startActivity(Intent(this, mainlogin::class.java))
            finish()
        }

        // Tap the circle to pick a photo
        avatarContainer.setOnClickListener { pickImage.launch("image/*") }

        // Password eye toggle
        eye.setOnClickListener {
            passwordVisible = !passwordVisible
            passEt.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            eye.setImageResource(if (passwordVisible) R.drawable.eye_slash else R.drawable.eye)
            passEt.setSelection(passEt.text.length)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            createBtn.isEnabled = !isLoading
            createBtn.text = if (isLoading) "Creating Account..." else "Create Account"
        }

        // Observe registration result
        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    goNext()
                }
                is Resource.Error -> {
                    Toast.makeText(this, result.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {
                    // Loading handled by isLoading observer
                }
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        createBtn.setOnClickListener { signUp() }
    }

    private fun signUp() {
        val usernameEt = findViewById<EditText>(R.id.usernametextfield)
        val firstEt = findViewById<EditText>(R.id.firstnametextfield)
        val lastEt = findViewById<EditText>(R.id.lastnametextfield)
        val emailEt = findViewById<EditText>(R.id.emailtextfield)
        val passEt = findViewById<EditText>(R.id.passwordtextfield)

        val username = usernameEt.text.toString().trim()
        val first = firstEt.text.toString().trim()
        val last = lastEt.text.toString().trim()
        val email = emailEt.text.toString().trim()
        val pass = passEt.text.toString()

        val fullName = "$first $last".trim()

        // Validation
        if (username.isEmpty()) {
            usernameEt.error = "Enter a username"
            usernameEt.requestFocus()
            return
        }
        if (username.length < 2) {
            usernameEt.error = "Username must be at least 2 characters"
            usernameEt.requestFocus()
            return
        }
        if (first.isEmpty()) {
            firstEt.error = "Enter your first name"
            firstEt.requestFocus()
            return
        }
        if (last.isEmpty()) {
            lastEt.error = "Enter your last name"
            lastEt.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.error = "Enter a valid email"
            emailEt.requestFocus()
            return
        }
        if (pass.length < 6) {
            passEt.error = "Password must be at least 6 characters"
            passEt.requestFocus()
            return
        }

        // Call ViewModel register
        viewModel.register(email, pass, username, fullName)
    }

    private fun toBase64(bitmap: Bitmap, quality: Int): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun resize(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width
        return Bitmap.createScaledBitmap(src, maxWidth, (src.height * ratio).toInt(), true)
    }

    private fun goNext() {
        val i = Intent(this, login_splash::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
