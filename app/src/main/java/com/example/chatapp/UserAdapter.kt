package com.example.chatapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class UserAdapter(val activity: Activity, val userList: ArrayList<User>, val currentUserUid: String?, var selectedCountry: String): RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val USER_OFFLINE_THRESHOLD = 60000
    var userLastActiveTimeMap = HashMap<String, Long>()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName = itemView.findViewById<TextView>(R.id.text_name)
        val textLastMessage = itemView.findViewById<TextView>(R.id.text_last_message)
        val imageAvatar = itemView.findViewById<ImageView>(R.id.image_avatar)
        val imageOnlineStatus = itemView.findViewById<ImageView>(R.id.image_online_status)

    }

    private var userOnlineStatusMap = HashMap<String, Boolean>()

    private fun updateUserStatus(userId: String) {
        val currentTime = System.currentTimeMillis()
        val lastActiveTime = userLastActiveTimeMap[userId] ?: 0
        val isUserOnline = currentTime - lastActiveTime <= USER_OFFLINE_THRESHOLD
        userOnlineStatusMap[userId] = isUserOnline
    }

    fun updateUserStatusForUser(userId: String) {
        userLastActiveTimeMap[userId] = System.currentTimeMillis()
        updateUserStatus(userId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View  = LayoutInflater.from(activity).inflate(R.layout.user_layout, parent, false)
        val holder = UserViewHolder(view)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val currentUser = userList[position]
                currentUser.uid?.let { it1 -> updateUserStatusForUser(it1) }
                notifyItemChanged(position)
            }
        }
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        val userStatusRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUser.uid!!)
            .child("online")

        userStatusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isOnline = dataSnapshot.getValue(Boolean::class.java) ?: false

                if (isOnline) {
                    holder.imageOnlineStatus.setImageResource(R.drawable.online_icon)
                } else {
                    holder.imageOnlineStatus.setImageResource(R.drawable.ofline_icon)
                }

                holder.itemView.setOnClickListener{

                    currentUser.uid?.let { it1 -> updateUserStatusForUser(it1) }

                    val intent = Intent(activity, ChatActivity::class.java)

                    intent.putExtra("name", currentUser.name)
                    intent.putExtra("uid", currentUser.uid)
                    intent.putExtra("photo", currentUser.profileImage)
                    intent.putExtra("online", isOnline)
                    activity.startActivity(intent)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

        if (currentUser.uid != currentUserUid || currentUser.selectedCountry == selectedCountry) {
            holder.textName.text = currentUser.name
            holder.textLastMessage.text = currentUser.lastMessage ?: ""

            currentUser.profileImage?.let { profileImage ->
                Picasso.get().load(profileImage)
                    .transform(RoundedTransformation())
                    .into(holder.imageAvatar)
            }

            holder.itemView.visibility = View.VISIBLE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
    }


    class RoundedTransformation : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)

            val x = (source.width - size) / 2
            val y = (source.height - size) / 2

            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }

            val bitmap = Bitmap.createBitmap(size, size, source.config)

            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.shader = shader
            paint.isAntiAlias = true

            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)

            squaredBitmap.recycle()
            return bitmap
        }

        override fun key(): String {
            return "rounded"
        }
    }

}
