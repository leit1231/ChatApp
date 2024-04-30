package com.example.chatapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityMainBinding
import com.example.chatapp.fragments.ChatFragment
import com.example.chatapp.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth = FirebaseAuth.getInstance()

        val chatFragment = ChatFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .commit()

        binding.BNV.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.chats -> {
                    val chatFragment = ChatFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, chatFragment)
                        .commit()
                    true
                }
                R.id.profile -> {
                    val profileFragment = ProfileFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

//    override fun onPause() {
//        super.onPause()
//        val currentUser = mAuth.currentUser
//        currentUser?.let {
//            updateUserOnlineStatus(it.uid, false)
//        }
//    }

    override fun onResume() {
        super.onResume()
        val currentUser = mAuth.currentUser
        currentUser?.let {
            updateUserOnlineStatus(it.uid, true)
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        val currentUser = mAuth.currentUser
//        currentUser?.let {
//            updateUserOnlineStatus(it.uid, false)
//        }
//    }

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
