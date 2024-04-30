package com.example.chatapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.chatapp.databinding.ActivitySignUpBinding
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var storageRef: StorageReference
    private val PICK_IMAGE_REQUEST = 1
    private var selectedCountry: String = ""
    private var userCountry: String = ""
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        supportActionBar?.hide()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageRef = FirebaseStorage.getInstance().reference
        val countriesArray = resources.getStringArray(R.array.countries_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countriesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectYourCountry.adapter = adapter
        binding.selectCountry.adapter = adapter

        binding.textGoLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        binding.btnSignUp.setOnClickListener{
            val name = binding.ETName.text.toString()
            val email = binding.ETEmail.text.toString()
            val password = binding.ETPass.text.toString()
            val selectedYourCountry = binding.selectYourCountry.selectedItem.toString()
            val selectedCountry = binding.selectCountry.selectedItem.toString()
            signUp(name, email, password, selectedYourCountry, selectedCountry)
        }
        binding.selectImage.setOnClickListener {
            chooseImageFromGallery()
        }
    }

    private fun signUp(
        name: String,
        email: String,
        password: String,
        selectedYourCountry: String,
        selectedCountry: String
    ) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid
                    if (imageUrl != null) {
                        addUsertoDatabase(uid!!, imageUrl!!, selectedYourCountry, selectedCountry)
                    } else {
                        Toast.makeText(this@SignUp, "Please select an image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SignUp, "Error", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun chooseImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImageUri = data.data
            val uid = mAuth.currentUser?.uid
            if (selectedImageUri != null && uid != null) {
                uploadImage(uid, selectedImageUri)
            }
        }
    }

    private fun uploadImage(uid: String, selectedImageUri: Uri) {
        val imageRef = storageRef.child("$uid.jpg")

        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    imageUrl = uri.toString()
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(imageUrl)
                            .transform(RoundedTransformation())
                            .into(binding.selectImage)
                    }
                }
            }
            .addOnFailureListener { exception ->
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

    private fun addUsertoDatabase(uid: String, imageUrl: String, selectedYourCountry: String, selectedCountry: String) {
        mDbRef = FirebaseDatabase.getInstance().getReference()

        val name = binding.ETName.text.toString()
        val email = binding.ETEmail.text.toString()

        val userData = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "selectedCountry" to selectedCountry,
            "userCountry" to selectedYourCountry,
            "profileImage" to imageUrl
        )

        mDbRef.child("users").child(uid).setValue(userData)
            .addOnSuccessListener {
                val intent = Intent(this@SignUp, MainActivity::class.java)
                finish()
                startActivity(intent)
            }
            .addOnFailureListener { exception ->
            }
    }
}



