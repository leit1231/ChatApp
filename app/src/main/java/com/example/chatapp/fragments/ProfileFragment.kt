package com.example.chatapp.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.chatapp.Login
import com.example.chatapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import java.util.UUID

class ProfileFragment : Fragment() {

    private lateinit var textProfile: TextView
    private lateinit var imageProfile: ImageView
    private lateinit var edittextName: EditText
    private lateinit var spinnerCountry: Spinner
    private lateinit var buttonSave: Button
    private lateinit var buttonLogout: Button

    private lateinit var userRef: DatabaseReference
    private var currentUser: FirebaseUser? = null

    private var profileChanged = false

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        textProfile = view.findViewById(R.id.text_profile)
        imageProfile = view.findViewById(R.id.image_profile)
        edittextName = view.findViewById(R.id.edittext_name)
        spinnerCountry = view.findViewById(R.id.countrySpinner)
        buttonSave = view.findViewById(R.id.button_save)
        buttonLogout = view.findViewById(R.id.button_logout)
        val countriesArray = resources.getStringArray(R.array.countries_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countriesArray)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerCountry.adapter = adapter

        edittextName.addTextChangedListener(profileTextWatcher)
        spinnerCountry.setOnItemSelectedListener(profileItemSelectedListener)

        currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val name = dataSnapshot.child("name").getValue(String::class.java)
                        val profileImageUrl = dataSnapshot.child("profileImage").getValue(String::class.java)
                        val country = dataSnapshot.child("userCountry").getValue(String::class.java)
                        edittextName.setText(name)
                        Picasso.get().load(profileImageUrl).transform(RoundedTransformation()).into(imageProfile)
                        val position = (spinnerCountry.adapter as ArrayAdapter<String>).getPosition(country)
                        spinnerCountry.setSelection(position)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }

        imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        buttonSave.setOnClickListener {
            if (profileChanged) {
                saveProfileChanges()
            }
        }

        buttonLogout.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let {
                updateUserOnlineStatus(it.uid, false)
            }
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireActivity(), Login::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private val profileTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            profileChanged = true
            updateButtonState()
        }
    }

    private val profileItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            profileChanged = true
            updateButtonState()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun updateButtonState() {
        buttonSave.isEnabled = profileChanged
    }

    private fun saveProfileChanges() {
        val newName = edittextName.text.toString()
        val newCountry = spinnerCountry.selectedItem.toString()

        currentUser?.let { user ->
            val userMap = HashMap<String, Any>()
            userMap["name"] = newName
            userMap["userCountry"] = newCountry
            userRef.updateChildren(userMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        profileChanged = false
                        updateButtonState()
                        Toast.makeText(requireContext(), "Профиль успешно обновлен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при обновлении профиля", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    inner class RoundedTransformation : Transformation {
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                uploadImageToFirebaseStorage(imageUri)
            }
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    currentUser?.let { user ->
                        userRef.child("profileImage").setValue(imageUrl)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Изображение успешно загружено", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Ошибка при обновлении ссылки на изображение", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show()
            }
    }


    fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
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
