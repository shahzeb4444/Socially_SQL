package com.teamsx.i230610_i230040.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamsx.i230610_i230040.Story
import com.teamsx.i230610_i230040.StoryGroup
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.network.StoryRepository
import kotlinx.coroutines.launch

class StoryViewModel : ViewModel() {

    private val repository = StoryRepository(RetrofitInstance.apiService)

    private val _createStoryState = MutableLiveData<Resource<Story>?>()
    val createStoryState: LiveData<Resource<Story>?> = _createStoryState

    private val _feedStoriesState = MutableLiveData<Resource<List<StoryGroup>>?>()
    val feedStoriesState: LiveData<Resource<List<StoryGroup>>?> = _feedStoriesState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun createStory(
        storyId: String,
        userId: String,
        imageBase64: String,
        timestamp: Long,
        expiresAt: Long,
        isCloseFriendsOnly: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _createStoryState.value = com.teamsx.i230610_i230040.network.Resource.Loading()

            val result = repository.createStory(
                storyId = storyId,
                userId = userId,
                imageBase64 = imageBase64,
                timestamp = timestamp,
                expiresAt = expiresAt,
                isCloseFriendsOnly = isCloseFriendsOnly
            )

            _createStoryState.value = result
            _isLoading.value = false
        }
    }

    fun getFeedStories(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _feedStoriesState.value = com.teamsx.i230610_i230040.network.Resource.Loading()

            val result = repository.getFeedStories(userId)

            _feedStoriesState.value = result
            _isLoading.value = false
        }
    }

    fun markStoryViewed(storyId: String, viewerId: String) {
        viewModelScope.launch {
            repository.markStoryViewed(storyId, viewerId)
        }
    }

    fun clearCreateStoryState() {
        _createStoryState.postValue(null)
    }
}

