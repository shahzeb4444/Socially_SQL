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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class second_page : AppCompatActivity() {

    private var passwordVisible = false
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var avatarImage: ImageView
    private lateinit var cameraIcon: ImageView

    private var photoBase64: String? = null

    // Image picker → preview in circle → hide camera → make Base64
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        avatarImage.setImageURI(uri)
        avatarImage.visibility = View.VISIBLE
        cameraIcon.visibility   = View.GONE

        contentResolver.openInputStream(uri)?.use { input ->
            val bmp = BitmapFactory.decodeStream(input)
            photoBase64 = toBase64(resize(bmp, 512), 80)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.second_page)

        auth = FirebaseAuth.getInstance()

        val back            = findViewById<ImageView>(R.id.leftarrow)
        val avatarContainer = findViewById<FrameLayout>(R.id.whitecircle)
        avatarImage         = findViewById(R.id.avatarImage)
        cameraIcon          = findViewById(R.id.cameralogo)
        val eye             = findViewById<ImageView>(R.id.eyeopen)
        val passEt          = findViewById<EditText>(R.id.passwordtextfield)
        val createBtn       = findViewById<Button>(R.id.createaccountbutton)

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

        createBtn.setOnClickListener { signUp() }
    }

    private fun signUp() {
        val usernameEt = findViewById<EditText>(R.id.usernametextfield)
        val firstEt    = findViewById<EditText>(R.id.firstnametextfield)
        val lastEt     = findViewById<EditText>(R.id.lastnametextfield)
        val dobEt      = findViewById<EditText>(R.id.dobtextfield)
        val emailEt    = findViewById<EditText>(R.id.emailtextfield)
        val passEt     = findViewById<EditText>(R.id.passwordtextfield)

        val displayUsername = usernameEt.text.toString().trim() // show this in UI
        val usernameKey     = toUsernameKey(displayUsername)    // safe DB key
        val first           = firstEt.text.toString().trim()
        val last            = lastEt.text.toString().trim()
        val dob             = dobEt.text.toString().trim()
        val email           = emailEt.text.toString().trim()
        val pass            = passEt.text.toString()

        // Minimal validation
        if (displayUsername.isEmpty()) { usernameEt.error = "Enter a username"; usernameEt.requestFocus(); return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailEt.error = "Enter a valid email"; emailEt.requestFocus(); return }
        if (pass.length < 6) { passEt.error = "Min 6 characters"; passEt.requestFocus(); return }

        // Ensure usernameKey conforms to rules (2–30 chars after sanitizing)
        if (usernameKey.length < 2) {
            toast("Username is too short after removing invalid characters.")
            return
        }

        // 1) Check availability (usernames .read is true in rules)
        db.child("usernames").child(usernameKey).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    toast("Username already taken")
                    return@addOnSuccessListener
                }

                // 2) Create auth account
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user!!.uid

                        // 3) Build profile (store original displayUsername as typed)
                        val profile = UserProfile(
                            username = displayUsername,
                            firstName = first,
                            lastName = last,
                            dob = dob,
                            email = email,
                            photoBase64 = photoBase64,
                            isProfileComplete = true
                        )

                        // 4) Atomic write: /users/{uid} + /usernames/{usernameKey} = uid
                        val updates = hashMapOf<String, Any?>(
                            "/users/$uid" to profile.toMap(),
                            "/usernames/$usernameKey" to uid
                        )
                        db.updateChildren(updates)
                            .addOnSuccessListener { goNext() }
                            .addOnFailureListener { e -> toast("DB write failed: ${e.localizedMessage}") }
                    }
                    .addOnFailureListener { e ->
                        toast(e.localizedMessage ?: "Signup failed")
                    }
            }
            .addOnFailureListener { e ->
                toast("Username check failed: ${e.localizedMessage ?: "check DB rules"}")
            }
    }

    /** Sanitize to a valid RTDB key: lowercase, remove spaces, replace illegal chars with '_' and cap length at 30. */
    private fun toUsernameKey(name: String): String {
        val trimmed = name.trim().lowercase()
        val noSpaces = trimmed.replace("\\s+".toRegex(), "_")
        val safe = noSpaces.replace(Regex("[.#$\\[\\]/]+"), "_")
        return if (safe.length > 30) safe.substring(0, 30) else safe
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
