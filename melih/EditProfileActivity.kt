package com.melihberat.mobiluyg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.melihberat.mobiluyg.Network.ApiClient
import com.melihberat.mobiluyg.Network.model.UpdateProfileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var avatarImage: ImageView
    private lateinit var etName: EditText

    private var selectedUri: Uri? = null
    private var pendingCameraUri: Uri? = null
    private var currentUsername: String = ""

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                setAvatarFromUri(uri)
                selectedUri = uri
            }
        } else {
            pendingCameraUri?.let { contentResolver.delete(it, null, null) }
        }
        pendingCameraUri = null
    }

    private val pickFromGallery = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            setAvatarFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        avatarImage = findViewById(R.id.avatarImageEdit)
        etName = findViewById(R.id.etNameEdit)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveEdit)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBarEdit)

        currentUsername = intent.getStringExtra("username") ?: ""
        etName.setText(currentUsername)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<ImageView>(R.id.btnEditAvatarEdit).setOnClickListener {
            showAvatarPicker()
        }

        btnSave.setOnClickListener { saveChanges() }
    }

    private fun showAvatarPicker() {
        val items = arrayOf("Kamera", "Galeri")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Profil fotoğrafı ekle")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickFromGallery.launch(arrayOf("image/*"))
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun openCamera() {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val imagesDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

        if (imagesDir == null) {
            Toast.makeText(this, "Depolama alanına erişilemiyor", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(imagesDir, "IMG_$time.jpg")
        val authority = "$packageName.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, photoFile)
        pendingCameraUri = uri

        takePicture.launch(uri)
    }

    private fun setAvatarFromUri(uri: Uri) {
        avatarImage.setImageURI(uri)
    }

    private fun saveChanges() {
        val newName = etName.text.toString().trim()
        if (newName.isBlank()) {
            Toast.makeText(this, "İsim boş olamaz", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = ApiClient.api(this@EditProfileActivity)
                api.updateProfile(UpdateProfileRequest(newName))
            } catch (_: Exception) { }

            // 🔹 Kullanıcı adını kalıcı olarak kaydet
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("username_local", newName)
                .commit()

            val data = Intent().apply {
                putExtra("newName", newName)
                selectedUri?.let { putExtra("newAvatarUri", it.toString()) }
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
