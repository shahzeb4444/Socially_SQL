package com.teamsx.i230610_i230040.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamsx.i230610_i230040.network.*
import kotlinx.coroutines.launch

class FollowViewModel : ViewModel() {

    private val followRepository = FollowRepository()
    private val userRepository = UserRepository()

    private val _followActionResult = MutableLiveData<Resource<FollowActionData>>()
    val followActionResult: LiveData<Resource<FollowActionData>> = _followActionResult

    private val _userProfile = MutableLiveData<Resource<UserProfileDataWrapper>>()
    val userProfile: LiveData<Resource<UserProfileDataWrapper>> = _userProfile

    private val _followers = MutableLiveData<Resource<List<ApiFollowUser>>>()
    val followers: LiveData<Resource<List<ApiFollowUser>>> = _followers

    private val _following = MutableLiveData<Resource<List<ApiFollowUser>>>()
    val following: LiveData<Resource<List<ApiFollowUser>>> = _following

    private val _followRequests = MutableLiveData<Resource<List<ApiFollowRequest>>>()
    val followRequests: LiveData<Resource<List<ApiFollowRequest>>> = _followRequests

    fun sendFollowRequest(fromUid: String, toUid: String) {
        viewModelScope.launch {
            _followActionResult.value = Resource.Loading()
            _followActionResult.value = followRepository.sendFollowRequest(fromUid, toUid)
        }
    }

    fun cancelFollowRequest(fromUid: String, toUid: String) {
        viewModelScope.launch {
            _followActionResult.value = Resource.Loading()
            _followActionResult.value = followRepository.cancelFollowRequest(fromUid, toUid)
        }
    }

    fun acceptFollowRequest(fromUid: String, toUid: String) {
        viewModelScope.launch {
            _followActionResult.value = Resource.Loading()
            _followActionResult.value = followRepository.acceptFollowRequest(fromUid, toUid)
        }
    }

    fun rejectFollowRequest(fromUid: String, toUid: String) {
        viewModelScope.launch {
            _followActionResult.value = Resource.Loading()
            _followActionResult.value = followRepository.rejectFollowRequest(fromUid, toUid)
        }
    }

    fun unfollowUser(fromUid: String, toUid: String) {
        viewModelScope.launch {
            _followActionResult.value = Resource.Loading()
            _followActionResult.value = followRepository.unfollowUser(fromUid, toUid)
        }
    }

    fun getUserProfile(uid: String, currentUid: String? = null) {
        viewModelScope.launch {
            _userProfile.value = Resource.Loading()
            _userProfile.value = userRepository.getUserProfile(uid, currentUid)
        }
    }

    fun getFollowers(uid: String) {
        viewModelScope.launch {
            _followers.value = Resource.Loading()
            _followers.value = followRepository.getFollowers(uid)
        }
    }

    fun getFollowing(uid: String) {
        viewModelScope.launch {
            _following.value = Resource.Loading()
            _following.value = followRepository.getFollowing(uid)
        }
    }

    fun getFollowRequests(uid: String) {
        viewModelScope.launch {
            _followRequests.value = Resource.Loading()
            _followRequests.value = followRepository.getFollowRequests(uid)
        }
    }

    fun clearFollowActionResult() {
        _followActionResult.value = null
    }
}

