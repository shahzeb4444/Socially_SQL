package com.teamsx.i230610_i230040.network

import android.util.Log

class UserRepository {

    private val api = RetrofitInstance.apiService

    suspend fun searchUsers(query: String, currentUserUid: String = ""): Resource<List<ApiFollowUser>> {
        return try {
            val response = api.searchUsers(SearchUsersRequest(query, currentUserUid))
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()!!.data?.users ?: emptyList()
                Resource.Success(users)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to search users"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "searchUsers error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getUserProfile(uid: String, currentUid: String? = null): Resource<UserProfileDataWrapper> {
        return try {
            Log.d("UserRepository", "getUserProfile called with uid=$uid, currentUid=$currentUid")
            val response = api.getUserProfile(GetUserProfileRequest(uid, currentUid))
            Log.d("UserRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("UserRepository", "Response body success: ${body?.success}, error: ${body?.error}, hasData: ${body?.data != null}")

                if (body?.success == true) {
                    val data = body.data
                    if (data != null) {
                        Log.d("UserRepository", "Profile data received: user=${data.user.username}, relationship=${data.relationship}")
                        Resource.Success(data)
                    } else {
                        Log.e("UserRepository", "Data is null in successful response")
                        Resource.Error("No profile data returned")
                    }
                } else {
                    val errorMsg = body?.error ?: "Failed to fetch profile"
                    Log.e("UserRepository", "API error: $errorMsg, code: ${response.code()}")
                    Resource.Error(errorMsg)
                }
            } else {
                Log.e("UserRepository", "HTTP error: ${response.code()}, message: ${response.message()}")
                Resource.Error("HTTP error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "getUserProfile exception", e)
            e.printStackTrace()
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun updateUserProfile(
        uid: String,
        fullName: String? = null,
        bio: String? = null,
        profileImageUrl: String? = null,
        coverImageUrl: String? = null
    ): Resource<ApiUserProfile> {
        return try {
            val request = UpdateProfileRequest(uid, fullName, bio, profileImageUrl, coverImageUrl)
            val response = api.updateUserProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.data?.user
                if (user != null) {
                    Resource.Success(user)
                } else {
                    Resource.Error("No user data returned")
                }
            } else {
                val errorMsg = response.body()?.error ?: "Failed to update profile"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "updateUserProfile error", e)
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }
}

