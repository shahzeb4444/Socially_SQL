package com.teamsx.i230610_i230040.network

import android.util.Log

class FollowRepository {

    private val api = RetrofitInstance.apiService

    suspend fun sendFollowRequest(fromUid: String, toUid: String): Resource<FollowActionData> {
        return try {
            val response = api.sendFollowRequest(SendFollowRequest(fromUid, toUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    Resource.Success(data)
                } else {
                    Resource.Error("No data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to send follow request"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "sendFollowRequest error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun cancelFollowRequest(fromUid: String, toUid: String): Resource<FollowActionData> {
        return try {
            val response = api.cancelFollowRequest(SendFollowRequest(fromUid, toUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    Resource.Success(data)
                } else {
                    Resource.Error("No data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to cancel request"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "cancelFollowRequest error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun acceptFollowRequest(fromUid: String, toUid: String): Resource<FollowActionData> {
        return try {
            val response = api.acceptFollowRequest(SendFollowRequest(fromUid, toUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    Resource.Success(data)
                } else {
                    Resource.Error("No data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to accept request"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "acceptFollowRequest error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun rejectFollowRequest(fromUid: String, toUid: String): Resource<FollowActionData> {
        return try {
            val response = api.rejectFollowRequest(SendFollowRequest(fromUid, toUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    Resource.Success(data)
                } else {
                    Resource.Error("No data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to reject request"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "rejectFollowRequest error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun unfollowUser(fromUid: String, toUid: String): Resource<FollowActionData> {
        return try {
            val response = api.unfollowUser(SendFollowRequest(fromUid, toUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    Resource.Success(data)
                } else {
                    Resource.Error("No data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to unfollow"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "unfollowUser error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getFollowers(uid: String): Resource<List<ApiFollowUser>> {
        return try {
            val response = api.getFollowers(GetFollowersRequest(uid))
            if (response.isSuccessful && response.body()?.success == true) {
                val followers = response.body()!!.data?.followers ?: emptyList()
                Resource.Success(followers)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to fetch followers"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "getFollowers error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getFollowing(uid: String): Resource<List<ApiFollowUser>> {
        return try {
            val response = api.getFollowing(GetFollowersRequest(uid))
            if (response.isSuccessful && response.body()?.success == true) {
                val following = response.body()!!.data?.following ?: emptyList()
                Resource.Success(following)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to fetch following"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "getFollowing error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getFollowRequests(uid: String): Resource<List<ApiFollowRequest>> {
        return try {
            val response = api.getFollowRequests(GetFollowRequestsRequest(uid))
            if (response.isSuccessful && response.body()?.success == true) {
                val requests = response.body()!!.data?.requests ?: emptyList()
                Resource.Success(requests)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to fetch requests"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "getFollowRequests error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }
}

