package com.example.chatapp

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var messageRef: CollectionReference
    private lateinit var mAuth: FirebaseAuth

    private var receiverRoom: String? = null
    private var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth = FirebaseAuth.getInstance()

        val currentUser = mAuth.currentUser
        currentUser?.let {
            updateUserOnlineStatus(it.uid, false)
        }

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val photo = intent.getStringExtra("photo")
        val isUserOnline = intent.getBooleanExtra("online", true)
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        firestore = FirebaseFirestore.getInstance()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        messageRecyclerView = binding.chatRecyclerView
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)
        messageRecyclerView.adapter = messageAdapter

        val userName = name
        val userAvatar = photo
        binding.textName.text = userName
        userAvatar?.let { avatarUrl ->
            Picasso.get().load(avatarUrl)
                .transform(UserAdapter.RoundedTransformation())
                .into(binding.imageAvatar)
        }

        val userOnline = isUserOnline
        print(userOnline)
        if (userOnline) {
            binding.imageOnlineStatus.setImageResource(R.drawable.online_icon)
        } else {
            binding.imageOnlineStatus.setImageResource(R.drawable.ofline_icon)
        }

        messageRef = firestore.collection("chats").document(senderRoom!!)
            .collection("messages")

        messageRef.orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (documentChange in snapshot.documentChanges) {
                        val message = documentChange.document.toObject(Message::class.java)
                        when (documentChange.type) {
                            DocumentChange.Type.ADDED -> {
                                messageList.add(message)
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                            }
                            else -> {}
                        }
                    }
                } else {
                    Log.d("ChatActivity", "Current data: null")
                }
            }


        messageBox = binding.messageBox
        sendButton = binding.sendButton
        sendButton.setOnClickListener {
            val messageText = messageBox.text.toString()
            val senderUid = FirebaseAuth.getInstance().currentUser?.uid
            val receiverUid = intent.getStringExtra("uid")
            val messageObject = Message(messageText, senderUid, System.currentTimeMillis(), receiverUid)

            firestore.collection("chats").document(receiverRoom!!)
                .collection("messages").add(messageObject)
                .addOnSuccessListener { documentReference ->
                    messageBox.text.clear()
                }
                .addOnFailureListener { e ->
                }

            if (senderUid != receiverUid) {
                firestore.collection("chats").document(senderRoom!!)
                    .collection("messages").add(messageObject)
                    .addOnSuccessListener { documentReference ->
                    }
                    .addOnFailureListener { e ->
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentUser = mAuth.currentUser
        currentUser?.let {
            updateUserOnlineStatus(it.uid, true)
        }
    }

    private fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        val userStatusRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("online")

        userStatusRef.setValue(isOnline)
            .addOnSuccessListener {
            }
            .addOnFailureListener { exception ->
            }
    }
}
