package com.teamsx.i230610_i230040.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamsx.i230610_i230040.network.ApiUserProfile
import com.teamsx.i230610_i230040.network.GetUserProfileRequest
import com.teamsx.i230610_i230040.network.RetrofitInstance
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _profile = MutableLiveData<ApiUserProfile?>()
    val profile: LiveData<ApiUserProfile?> = _profile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserProfile(uid: String, currentUid: String?) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val request = GetUserProfileRequest(uid, currentUid)
                val response = RetrofitInstance.apiService.getUserProfile(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data?.user != null) {
                        _profile.value = body.data.user
                    } else {
                        _error.value = body?.error ?: "Failed to load profile"
                    }
                } else {
                    _error.value = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

