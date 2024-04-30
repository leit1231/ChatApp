package com.example.chatapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.User
import com.example.chatapp.UserAdapter
import com.example.chatapp.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var countrySpinner: Spinner
    private var currentUserUid: String? = null
    private var selectedCountry: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference
        userList = ArrayList()
        currentUserUid = mAuth.currentUser?.uid
        adapter = UserAdapter(requireActivity(), userList, currentUserUid, selectedCountry)
        userRecyclerView = binding.userRecyclerView
        countrySpinner = binding.countrySpinner

        userRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        userRecyclerView.adapter = adapter

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.countries_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            countrySpinner.adapter = adapter
        }

        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val country = parent?.getItemAtPosition(position).toString()
                selectedCountry = country
                updateSelectedCountryInDatabase(selectedCountry)
                loadUsersForCurrentUser(selectedCountry)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        getCurrentUserCountry { userCountry ->
            loadUsersForCurrentUser(userCountry)
        }

        return root
    }

    private fun updateSelectedCountryInDatabase(country: String) {
        val userUid = mAuth.currentUser?.uid
        if (userUid != null) {
            mDbRef.child("users").child(userUid).child("selectedCountry").setValue(country)
                .addOnSuccessListener {
                    loadUsersForCurrentUser(selectedCountry)
                }
                .addOnFailureListener { exception ->
                }
        }
    }

    private fun getCurrentUserCountry(onSuccess: (String) -> Unit) {
        val userUid = mAuth.currentUser?.uid
        if (userUid != null) {
            mDbRef.child("users").child(userUid).child("userCountry")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userCountry = snapshot.getValue(String::class.java)
                        if (userCountry != null) {
                            onSuccess(userCountry)
                        } else {
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }

    private fun loadUsersForCurrentUser(userCountry: String) {
        val userUid = mAuth.currentUser?.uid
        if (userUid != null) {
            mDbRef.child("users").child(userUid).child("selectedCountry")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val selectedCountry = snapshot.getValue(String::class.java)
                        if (selectedCountry != null) {
                            adapter.selectedCountry = selectedCountry
                            adapter.notifyDataSetChanged()
                            loadUsers(userCountry, selectedCountry)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }

    private fun loadUsers(userCountry: String, selectedCountry: String) {
        val currentUserUid = mAuth.currentUser?.uid
        mDbRef.child("users")
            .orderByChild("userCountry")
            .equalTo(selectedCountry)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (currentUser != null && currentUser.uid != currentUserUid) {
                            userList.add(currentUser)
                        }
                    }
                    val filteredUsers = userList.filter { it.userCountry == userCountry }
                    userList.clear()
                    userList.addAll(filteredUsers)
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
