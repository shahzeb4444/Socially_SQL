package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OtherUserProfileFragment : Fragment() {

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_USERNAME = "username"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var usernameTextView: TextView
    private lateinit var fullNameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var postsCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var followingCountTextView: TextView
    private lateinit var followButtonBg: ImageView
    private lateinit var followLabel: TextView
    private lateinit var backArrow: ImageView
    private lateinit var postsGridRecyclerView: RecyclerView

    private lateinit var profileGridAdapter: ProfileGridAdapter
    private val userPosts = mutableListOf<Post>()

    private var userId: String? = null
    private var username: String? = null

    private enum class Rel { NONE, REQUESTED, INCOMING_REQUEST, FOLLOWING, FOLLOWED_BY }
    private var currentRel: Rel = Rel.NONE
    private var relListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
            username = it.getString(ARG_USERNAME)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_other_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)
        setupClickListeners()
        setupPostsGrid()

        observeRelationship()
        loadUserProfile()
        loadUserPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val me = auth.currentUser?.uid
        val other = userId
        if (me != null && other != null) {
            relListener?.let {
                db.child("relationships").child(me).child(other).removeEventListener(it)
            }
        }
    }

    private fun initViews(view: View) {
        profileImageView = view.findViewById(R.id.profileicon)
        usernameTextView = view.findViewById(R.id.jacobtext)
        fullNameTextView = view.findViewById(R.id.profileusername)
        bioTextView = view.findViewById(R.id.d1)
        postsCountTextView = view.findViewById(R.id.noposts)
        followersCountTextView = view.findViewById(R.id.nofollowers)
        followingCountTextView = view.findViewById(R.id.nofollowing)
        followButtonBg = view.findViewById(R.id.feedbutton1)
        followLabel = view.findViewById(R.id.followLabel)
        backArrow = view.findViewById(R.id.leftarrow)
        postsGridRecyclerView = view.findViewById(R.id.postsGridRecyclerView)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val clickFollow: (View) -> Unit = { onFollowClicked() }
        followButtonBg.setOnClickListener(clickFollow)
        followLabel.setOnClickListener(clickFollow)
    }

    private fun setupPostsGrid() {
        postsGridRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        profileGridAdapter = ProfileGridAdapter(
            posts = userPosts,
            onPostClick = { post ->
                openPostView(post)
            }
        )

        postsGridRecyclerView.adapter = profileGridAdapter
    }

    private fun loadUserPosts() {
        val uid = userId ?: return

        db.child("userPosts").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userPosts.clear()

                val postIds = snapshot.children.mapNotNull { it.key }

                if (postIds.isEmpty()) {
                    profileGridAdapter.notifyDataSetChanged()
                    return
                }

                val tempPosts = mutableListOf<Post>()
                var processedCount = 0

                for (postId in postIds) {
                    db.child("posts").child(postId).get()
                        .addOnSuccessListener { postSnapshot ->
                            val post = postSnapshot.getValue(Post::class.java)
                            if (post != null) {
                                synchronized(tempPosts) {
                                    tempPosts.add(post)
                                }
                            }

                            processedCount++
                            if (processedCount == postIds.size) {
                                // Sort by timestamp (newest first)
                                tempPosts.sortByDescending { it.timestamp }
                                userPosts.clear()
                                userPosts.addAll(tempPosts)
                                profileGridAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == postIds.size) {
                                tempPosts.sortByDescending { it.timestamp }
                                userPosts.clear()
                                userPosts.addAll(tempPosts)
                                profileGridAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load posts",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun openPostView(post: Post) {
        val intent = Intent(requireContext(), PostViewActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }

    private fun observeRelationship() {
        val me = auth.currentUser?.uid ?: return
        val other = userId ?: return
        val ref = db.child("relationships").child(me).child(other)
        relListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentRel = when (snapshot.getValue(String::class.java)) {
                    "requested" -> Rel.REQUESTED
                    "incoming_request" -> Rel.INCOMING_REQUEST
                    "following" -> Rel.FOLLOWING
                    "followed_by" -> Rel.FOLLOWED_BY
                    else -> Rel.NONE
                }
                renderFollowButton()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(relListener as ValueEventListener)
    }

    private fun renderFollowButton() {
        when (currentRel) {
            Rel.NONE, Rel.FOLLOWED_BY -> followLabel.text = "Follow"
            Rel.REQUESTED -> followLabel.text = "Requested"
            Rel.INCOMING_REQUEST -> followLabel.text = "Accept"
            Rel.FOLLOWING -> followLabel.text = "Following"
        }
    }

    private fun onFollowClicked() {
        val me = auth.currentUser?.uid ?: run { toast("Login required"); return }
        val other = userId ?: return
        when (currentRel) {
            Rel.NONE, Rel.FOLLOWED_BY -> sendFollowRequest(me, other)
            Rel.REQUESTED -> cancelFollowRequest(me, other)
            Rel.INCOMING_REQUEST -> acceptRequest(other /* requester */, me /* I am target */)
            Rel.FOLLOWING -> unfollow(me, other)
        }
    }

    private fun sendFollowRequest(fromUid: String, toUid: String) {
        // fetch my username/photo for rich request (optional: cache this)
        db.child("users").child(fromUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val meProfile = s.getValue(UserProfile::class.java)
                val req = FollowRequest(
                    fromUid = fromUid,
                    fromUsername = meProfile?.username ?: fromUid.take(8),
                    fromPhotoBase64 = meProfile?.photoBase64,
                    timestamp = System.currentTimeMillis()
                )
                val updates = hashMapOf<String, Any?>(
                    "/follow_requests/$toUid/$fromUid" to req,
                    "/relationships/$fromUid/$toUid" to "requested",
                    "/relationships/$toUid/$fromUid" to "incoming_request"
                )
                db.updateChildren(updates).addOnSuccessListener { renderFollowButton() }
                    .addOnFailureListener { toast("Failed to request") }
            }
            override fun onCancelled(error: DatabaseError) { toast("Failed to request") }
        })
    }

    private fun cancelFollowRequest(fromUid: String, toUid: String) {
        val updates = hashMapOf<String, Any?>(
            "/follow_requests/$toUid/$fromUid" to null,
            "/relationships/$fromUid/$toUid" to "none",
            "/relationships/$toUid/$fromUid" to "none"
        )
        db.updateChildren(updates).addOnSuccessListener { followLabel.text = "Follow" }
            .addOnFailureListener { toast("Failed to cancel") }
    }

    private fun acceptRequest(requesterUid: String, targetUid: String) {
        val actId = db.child("follow_activity").child(targetUid).push().key!!
        db.child("users").child(requesterUid).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(s: DataSnapshot) {
                val reqProfile = s.getValue(UserProfile::class.java)
                val updates = hashMapOf<String, Any?>(
                    "/follows/$requesterUid/following/$targetUid" to true,
                    "/follows/$targetUid/followers/$requesterUid" to true,
                    "/relationships/$requesterUid/$targetUid" to "following",
                    "/relationships/$targetUid/$requesterUid" to "followed_by",
                    "/follow_requests/$targetUid/$requesterUid" to null,
                    "/follow_activity/$targetUid/$actId" to FollowActivity(
                        fromUid = requesterUid,
                        fromUsername = reqProfile?.username ?: requesterUid.take(8),
                        fromPhotoBase64 = reqProfile?.photoBase64,
                        type = "followed_you",
                        timestamp = System.currentTimeMillis()
                    )
                )
                db.updateChildren(updates).addOnSuccessListener {
                    db.child("users").child(targetUid).child("followersCount").runTransaction(incrementBy(1))
                    db.child("users").child(requesterUid).child("followingCount").runTransaction(incrementBy(1))
                    followLabel.text = "Following"
                }.addOnFailureListener { toast("Failed to accept") }
            }
            override fun onCancelled(error: DatabaseError) { toast("Failed to accept") }
        })
    }

    private fun unfollow(fromUid: String, toUid: String) {
        val updates = hashMapOf<String, Any?>(
            "/follows/$fromUid/following/$toUid" to null,
            "/follows/$toUid/followers/$fromUid" to null,
            "/relationships/$fromUid/$toUid" to "none",
            "/relationships/$toUid/$fromUid" to "none"
        )
        db.updateChildren(updates).addOnSuccessListener {
            db.child("users").child(toUid).child("followersCount").runTransaction(incrementBy(-1))
            db.child("users").child(fromUid).child("followingCount").runTransaction(incrementBy(-1))
            followLabel.text = "Follow"
        }.addOnFailureListener { toast("Failed to unfollow") }
    }

    private fun incrementBy(delta: Int) = object : Transaction.Handler {
        override fun doTransaction(mutableData: MutableData): Transaction.Result {
            val cur = (mutableData.getValue(Int::class.java) ?: 0)
            var next = cur + delta
            if (next < 0) next = 0
            mutableData.value = next
            return Transaction.success(mutableData)
        }
        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
    }

    // ==== Profile rendering ====
    private fun loadUserProfile() {
        val uid = userId ?: return
        db.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) displayProfile(profile) else toast("Profile not found")
            }
            override fun onCancelled(error: DatabaseError) { toast("Failed: ${error.message}") }
        })
    }

    private fun displayProfile(profile: UserProfile) {
        usernameTextView.text = profile.username
        val fullName = "${profile.firstName} ${profile.lastName}".trim()
        fullNameTextView.text = if (fullName.isNotEmpty()) fullName else "User"
        bioTextView.text = if (profile.bio.isNotEmpty()) profile.bio else "No bio yet"
        postsCountTextView.text = profile.postsCount.toString()
        followersCountTextView.text = formatCount(profile.followersCount)
        followingCountTextView.text = profile.followingCount.toString()

        if (!profile.photoBase64.isNullOrEmpty()) {
            val bmp = ImageUtils.loadBase64ImageOptimized(profile.photoBase64, 300)
            if (bmp != null) profileImageView.setImageBitmap(ImageUtils.getCircularBitmap(bmp, 300))
            else setDefaultProfileImage()
        } else setDefaultProfileImage()
    }

    private fun setDefaultProfileImage() {
        profileImageView.setImageResource(R.drawable.profile_login_splash)
    }

    private fun formatCount(count: Int): String =
        when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}