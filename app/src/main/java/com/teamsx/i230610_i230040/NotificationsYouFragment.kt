package com.teamsx.i230610_i230040

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import android.util.Base64
import android.graphics.BitmapFactory
import android.util.Log
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.FollowViewModel

class NotificationsYouFragment : Fragment() {

    private val followViewModel: FollowViewModel by viewModels()
    private val userPreferences by lazy { UserPreferences(requireContext()) }

    private lateinit var rvReq: RecyclerView
    private lateinit var rvAct: RecyclerView

    private val requests = mutableListOf<FollowRequest>()
    private val activities = mutableListOf<FollowActivity>()

    private lateinit var reqAdapter: RequestsAdapter
    private lateinit var actAdapter: ActivityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            inflater.inflate(R.layout.fragment_notifications_you, container, false)
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error inflating layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            view.findViewById<TextView>(R.id.followingtext)?.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            rvReq = view.findViewById(R.id.rvFollowRequests)
            rvReq.layoutManager = LinearLayoutManager(requireContext())
            reqAdapter = RequestsAdapter(requests,
                onAccept = { req -> acceptRequest(req) },
                onReject = { req -> rejectRequest(req) }
            )
            rvReq.adapter = reqAdapter

            rvAct = view.findViewById(R.id.rvFollowActivity)
            rvAct.layoutManager = LinearLayoutManager(requireContext())
            actAdapter = ActivityAdapter(activities)
            rvAct.adapter = actAdapter

            setupObservers()
            loadFollowRequests()
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "Error loading notifications", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            loadFollowRequests()
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error in onResume", e)
        }
    }

    private fun setupObservers() {
        try {
            followViewModel.followRequests.observe(viewLifecycleOwner) { resource ->
                when (resource) {
                    is Resource.Success -> {
                        requests.clear()
                        resource.data?.forEach { apiRequest ->
                            // Skip requests with null or empty fromUid
                            if (apiRequest.fromUid.isNullOrEmpty()) {
                                Log.w("NotificationsYou", "Skipping request with null/empty fromUid")
                                return@forEach
                            }

                            try {
                                requests.add(
                                    FollowRequest(
                                        fromUid = apiRequest.fromUid ?: "",
                                        fromUsername = apiRequest.username ?: "Unknown",
                                        fromPhotoBase64 = apiRequest.profileImageUrl,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("NotificationsYou", "Error creating FollowRequest", e)
                            }
                        }
                        Log.d("NotificationsYou", "Loaded ${requests.size} follow requests")
                        reqAdapter.notifyDataSetChanged()
                    }
                    is Resource.Error -> {
                        Log.e("NotificationsYou", "Error loading requests: ${resource.message}")
                        Toast.makeText(requireContext(), resource.message ?: "Failed to load requests", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        // Show loading if needed
                    }
                    else -> {}
                }
            }

            followViewModel.followActionResult.observe(viewLifecycleOwner) { resource ->
                when (resource) {
                    is Resource.Success -> {
                        Toast.makeText(requireContext(), resource.data?.message ?: "Action completed", Toast.LENGTH_SHORT).show()
                        loadFollowRequests() // Reload the list
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message ?: "Action failed", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        // Show loading
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error setting up observers", e)
        }
    }

    private fun loadFollowRequests() {
        try {
            val currentUid = userPreferences.getUser()?.uid
            if (currentUid.isNullOrEmpty()) {
                Log.e("NotificationsYou", "User UID is null or empty")
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }
            Log.d("NotificationsYou", "Loading follow requests for user: $currentUid")
            followViewModel.getFollowRequests(currentUid)
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error loading follow requests", e)
            Toast.makeText(requireContext(), "Error loading requests", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acceptRequest(req: FollowRequest) {
        try {
            val currentUid = userPreferences.getUser()?.uid
            if (currentUid.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }
            followViewModel.acceptFollowRequest(req.fromUid, currentUid)
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error accepting request", e)
            Toast.makeText(requireContext(), "Error accepting request", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectRequest(req: FollowRequest) {
        try {
            val currentUid = userPreferences.getUser()?.uid
            if (currentUid.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }
            followViewModel.rejectFollowRequest(req.fromUid, currentUid)
        } catch (e: Exception) {
            Log.e("NotificationsYou", "Error rejecting request", e)
            Toast.makeText(requireContext(), "Error rejecting request", Toast.LENGTH_SHORT).show()
        }
    }
}

/* ----------------- Adapters ----------------- */

private class RequestsAdapter(
    private val data: MutableList<FollowRequest>,
    private val onAccept: (FollowRequest) -> Unit,
    private val onReject: (FollowRequest) -> Unit
) : RecyclerView.Adapter<RequestsVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestsVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return RequestsVH(v, onAccept, onReject)
    }
    override fun onBindViewHolder(holder: RequestsVH, position: Int) = holder.bind(data[position])
    override fun getItemCount(): Int = data.size
}

private class RequestsVH(
    itemView: View,
    private val onAccept: (FollowRequest) -> Unit,
    private val onReject: (FollowRequest) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val avatar = itemView.findViewById<ShapeableImageView>(R.id.imgAvatar)
    private val username = itemView.findViewById<TextView>(R.id.txtUsername)
    private val msg = itemView.findViewById<TextView>(R.id.txtMessage)
    private val accept = itemView.findViewById<android.widget.Button>(R.id.btnAccept)
    private val reject = itemView.findViewById<android.widget.Button>(R.id.btnReject)

    fun bind(req: FollowRequest) {
        username.text = req.fromUsername.ifBlank { req.fromUid.take(8) }
        msg.text = "requested to follow you"
        if (!req.fromPhotoBase64.isNullOrEmpty()) {
            runCatching {
                val bytes = Base64.decode(req.fromPhotoBase64, Base64.DEFAULT)
                avatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }.onFailure {
                avatar.setImageResource(R.drawable.profile_login_splash_small)
            }
        } else {
            avatar.setImageResource(R.drawable.profile_login_splash_small)
        }
        accept.setOnClickListener { onAccept(req) }
        reject.setOnClickListener { onReject(req) }
    }
}

private class ActivityAdapter(
    private val data: MutableList<FollowActivity>
) : RecyclerView.Adapter<ActivityVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_activity, parent, false)
        return ActivityVH(v)
    }
    override fun onBindViewHolder(holder: ActivityVH, position: Int) = holder.bind(data[position])
    override fun getItemCount(): Int = data.size
}

private class ActivityVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val avatar = itemView.findViewById<ShapeableImageView>(R.id.imgAvatar)
    private val line = itemView.findViewById<TextView>(R.id.txtLine)

    fun bind(act: FollowActivity) {
        line.text = "${act.fromUsername.ifBlank { act.fromUid.take(8) }} followed you"
        if (!act.fromPhotoBase64.isNullOrEmpty()) {
            runCatching {
                val bytes = Base64.decode(act.fromPhotoBase64, Base64.DEFAULT)
                avatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }.onFailure {
                avatar.setImageResource(R.drawable.profile_login_splash_small)
            }
        } else {
            avatar.setImageResource(R.drawable.profile_login_splash_small)
        }
    }
}
