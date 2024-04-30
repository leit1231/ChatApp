package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.chatapp.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        mAuth = FirebaseAuth.getInstance()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
        binding.btnLogin.setOnClickListener{
            val email = binding.ETEmail.text.toString()
            val password = binding.ETPass.text.toString()

            login(email, password);
        }
    }

    private fun login(email: String, password: String){
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    user?.let {
                        updateUserOnlineStatus(it.uid, true)
                    }
                    val intent = Intent(this@Login, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    val exception = task.exception
                    if (exception != null) {
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                Toast.makeText(this@Login, "Неверный email", Toast.LENGTH_SHORT).show()
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                if (exception.errorCode == "ERROR_WRONG_PASSWORD") {
                                    Toast.makeText(this@Login, "Неверный пароль", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@Login, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                Toast.makeText(this@Login, "Ошибка аунтефикации", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
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