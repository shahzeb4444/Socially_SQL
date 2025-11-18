package com.teamsx.i230610_i230040

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class PresenceApplication : Application() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var isInForeground = false
    private var lastKnownUserId: String? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            isInForeground = true
            auth.currentUser?.uid?.let { markOnline(it) }
        }

        override fun onStop(owner: LifecycleOwner) {
            isInForeground = false
            auth.currentUser?.uid?.let { markOffline(it) }
        }
    }

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUserId = firebaseAuth.currentUser?.uid
        if (newUserId != lastKnownUserId) {
            lastKnownUserId?.let { markOffline(it) }
        }
        lastKnownUserId = newUserId

        if (isInForeground && newUserId != null) {
            markOnline(newUserId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        auth.addAuthStateListener(authListener)
        lastKnownUserId = auth.currentUser?.uid
    }

    override fun onTerminate() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        auth.removeAuthStateListener(authListener)
        super.onTerminate()
    }

    private fun markOnline(userId: String) {
        if (userId.isBlank()) return
        val userStatusRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userStatusRef.child("isOnline").setValue(true)
        userStatusRef.child("isOnline").onDisconnect().setValue(false)
        userStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    private fun markOffline(userId: String) {
        if (userId.isBlank()) return
        val userStatusRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userStatusRef.child("isOnline").setValue(false)
        userStatusRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
    }
}