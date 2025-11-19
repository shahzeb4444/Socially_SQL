package com.teamsx.i230610_i230040

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teamsx.i230610_i230040.models.User
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userPreferences = UserPreferences(application)

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Login LiveData
    private val _loginResult = MutableLiveData<Resource<User>>()
    val loginResult: LiveData<Resource<User>> = _loginResult

    // Registration LiveData
    private val _registerResult = MutableLiveData<Resource<User>>()
    val registerResult: LiveData<Resource<User>> = _registerResult

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        // Load user from SharedPreferences on initialization
        _currentUser.value = userPreferences.getUser()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = Resource.Loading()

            val result = authRepository.login(email, password)
            _loginResult.value = result
            _isLoading.value = false

            if (result is Resource.Success && result.data != null) {
                // Save user to SharedPreferences
                userPreferences.saveUser(result.data)
                _currentUser.value = result.data
            } else if (result is Resource.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun register(email: String, password: String, username: String, fullName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registerResult.value = Resource.Loading()

            val result = authRepository.register(email, password, username, fullName)
            _registerResult.value = result
            _isLoading.value = false

            if (result is Resource.Success && result.data != null) {
                // Save user to SharedPreferences
                userPreferences.saveUser(result.data)
                _currentUser.value = result.data
            } else if (result is Resource.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun logout() {
        userPreferences.clearUser()
        _currentUser.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun isLoggedIn(): Boolean {
        return userPreferences.isLoggedIn()
    }

    fun getCurrentUserFromPrefs(): User? {
        return userPreferences.getUser()
    }
}


