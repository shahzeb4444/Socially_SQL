package com.teamsx.i230610_i230040

import com.teamsx.i230610_i230040.models.User
import com.teamsx.i230610_i230040.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepository {

    private val apiService = RetrofitInstance.apiService

    // Register new user
    suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String,
        profileImageUrl: String? = null
    ): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(email, password, username, fullName, profileImageUrl)
            val response = apiService.register(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Resource.Success(body.data.user.toUser())
                } else {
                    Resource.Error(body?.error ?: "Registration failed")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error. Please check your connection.")
        } catch (e: Exception) {
            Resource.Error("An error occurred: ${e.localizedMessage}")
        }
    }

    // Login user
    suspend fun login(
        email: String,
        password: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Resource.Success(body.data.user.toUser())
                } else {
                    Resource.Error(body?.error ?: "Login failed")
                }
            } else {
                Resource.Error("Server error: ${response.code()} - ${response.message()}")
            }
        } catch (e: java.net.UnknownHostException) {
            Resource.Error("Cannot reach server at ${RetrofitInstance.CURRENT_BASE}\n\nCheck:\n1. XAMPP is running\n2. IP is correct\n3. Phone on same WiFi (nuisb.edu.pk)")
        } catch (e: java.net.ConnectException) {
            Resource.Error("Connection refused to ${RetrofitInstance.CURRENT_BASE}\n\nCheck:\n1. Apache running in XAMPP\n2. Firewall allows port 80\n3. If using emulator, use 10.0.2.2")
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("Connection timeout. Server took too long to respond.")
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.message ?: "Unknown network issue"}")
        } catch (e: Exception) {
            Resource.Error("Error: ${e.javaClass.simpleName} - ${e.localizedMessage}")
        }
    }
}
