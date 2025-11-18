package com.teamsx.i230610_i230040

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Base64
import android.graphics.BitmapFactory

class NotificationsYouFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var rvReq: RecyclerView
    private lateinit var rvAct: RecyclerView

    private val requests = mutableListOf<FollowRequest>()
    private val activities = mutableListOf<FollowActivity>()

    private lateinit var reqAdapter: RequestsAdapter
    private lateinit var actAdapter: ActivityAdapter

    private var reqListener: ValueEventListener? = null
    private var actListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_notifications_you, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.followingtext).setOnClickListener {
            // back to "Following" tab
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        rvReq = view.findViewById(R.id.rvFollowRequests)
        rvReq.layoutManager = LinearLayoutManager(requireContext())
        reqAdapter = RequestsAdapter(requests,
            onAccept = { req -> accept(req) },
            onReject = { req -> reject(req) }
        )
        rvReq.adapter = reqAdapter

        rvAct = view.findViewById(R.id.rvFollowActivity)
        rvAct.layoutManager = LinearLayoutManager(requireContext())
        actAdapter = ActivityAdapter(activities)
        rvAct.adapter = actAdapter

        observeRequests()
        observeActivity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val uid = auth.currentUser?.uid ?: return
        reqListener?.let { db.child("follow_requests").child(uid).removeEventListener(it) }
        actListener?.let { db.child("follow_activity").child(uid).removeEventListener(it) }
    }

    private fun observeRequests() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.child("follow_requests").child(uid)
        reqListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requests.clear()
                for (c in snapshot.children) {
                    c.getValue(FollowRequest::class.java)?.let { requests.add(it) }
                }
                requests.sortByDescending { it.timestamp }
                reqAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(reqListener as ValueEventListener)
    }

    private fun observeActivity() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.child("follow_activity").child(uid)
        actListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activities.clear()
                for (c in snapshot.children) {
                    c.getValue(FollowActivity::class.java)?.let { activities.add(it) }
                }
                activities.sortByDescending { it.timestamp }
                actAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(actListener as ValueEventListener)
    }

    private fun accept(req: FollowRequest) {
        val targetUid = auth.currentUser?.uid ?: return
        val requesterUid = req.fromUid

        val actId = db.child("follow_activity").child(targetUid).push().key!!

        val updates = hashMapOf<String, Any?>(
            // link follows
            "/follows/$requesterUid/following/$targetUid" to true,
            "/follows/$targetUid/followers/$requesterUid" to true,
            // relationship swap
            "/relationships/$requesterUid/$targetUid" to "following",
            "/relationships/$targetUid/$requesterUid" to "followed_by",
            // remove the request
            "/follow_requests/$targetUid/$requesterUid" to null,
            // add permanent activity
            "/follow_activity/$targetUid/$actId" to FollowActivity(
                fromUid = requesterUid,
                fromUsername = req.fromUsername,
                fromPhotoBase64 = req.fromPhotoBase64,
                type = "followed_you",
                timestamp = System.currentTimeMillis()
            )
        )

        db.updateChildren(updates).addOnSuccessListener {
            // bump counts
            db.child("users").child(targetUid).child("followersCount").runTransaction(incrementBy(1))
            db.child("users").child(requesterUid).child("followingCount").runTransaction(incrementBy(1))
        }
    }

    private fun reject(req: FollowRequest) {
        val targetUid = auth.currentUser?.uid ?: return
        val requesterUid = req.fromUid
        val updates = hashMapOf<String, Any?>(
            "/follow_requests/$targetUid/$requesterUid" to null,
            "/relationships/$requesterUid/$targetUid" to "none",
            "/relationships/$targetUid/$requesterUid" to "none"
        )
        db.updateChildren(updates)
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
            }
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
            }
        }
    }
}
