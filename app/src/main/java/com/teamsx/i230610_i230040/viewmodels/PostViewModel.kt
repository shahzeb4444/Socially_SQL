package com.teamsx.i230610_i230040.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamsx.i230610_i230040.Post
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.network.PostRepository
import com.teamsx.i230610_i230040.network.RetrofitInstance
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {

    private val repository = PostRepository(RetrofitInstance.apiService)

    private val _createPostState = MutableLiveData<Resource<Post>?>()
    val createPostState: LiveData<Resource<Post>?> = _createPostState

    private val _feedPostsState = MutableLiveData<Resource<List<Post>>?>()
    val feedPostsState: LiveData<Resource<List<Post>>?> = _feedPostsState

    private val _toggleLikeState = MutableLiveData<Resource<Post>?>()
    val toggleLikeState: LiveData<Resource<Post>?> = _toggleLikeState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun createPost(
        postId: String,
        userId: String,
        description: String,
        location: String,
        images: List<String>,
        timestamp: Long
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _createPostState.value = com.teamsx.i230610_i230040.network.Resource.Loading()

            val result = repository.createPost(
                postId = postId,
                userId = userId,
                description = description,
                location = location,
                images = images,
                timestamp = timestamp
            )

            _createPostState.value = result
            _isLoading.value = false
        }
    }

    fun getFeedPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _feedPostsState.value = com.teamsx.i230610_i230040.network.Resource.Loading()

            val result = repository.getFeedPosts(userId)

            _feedPostsState.value = result
            _isLoading.value = false
        }
    }

    fun toggleLike(postId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.toggleLike(postId, userId)
            _toggleLikeState.value = result
        }
    }

    fun clearCreatePostState() {
        _createPostState.postValue(null)
    }

    fun clearToggleLikeState() {
        _toggleLikeState.postValue(null)
    }
}

