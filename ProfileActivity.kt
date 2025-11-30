package com.melihberat.mobiluyg

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.melihberat.mobiluyg.Network.ApiClient
import com.melihberat.mobiluyg.Network.TokenStore

class ProfileActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PREV = "EXTRA_PREV"
        private const val PREFS = "app_prefs"
        private const val KEY_USERNAME = "username_local"
        private const val KEY_REMEMBER = "REMEMBER"
        private const val AVATAR_PREF_PREFIX = "avatar_uri_"
    }

    private lateinit var bottom: BottomNavigationView
    private lateinit var avatarImage: ImageView
    private lateinit var tvUserName: TextView
    private var currentUser: String = ""

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val newName = result.data!!.getStringExtra("newName")
            val newAvatarUri = result.data!!.getStringExtra("newAvatarUri")

            if (!newName.isNullOrBlank()) {
                currentUser = newName
                tvUserName.text = newName
                prefs.edit().putString(KEY_USERNAME, newName).apply()
            }
            if (!newAvatarUri.isNullOrBlank()) {
                val uri = Uri.parse(newAvatarUri)
                setAvatarFromUri(uri)
                saveAvatarUri(currentUser, newAvatarUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvUserName = findViewById(R.id.tvUserName)
        avatarImage = findViewById(R.id.avatarImage)

        // 🔹 1. Önce yerel kayıt (düzenleme sonrası) oku
        val localName = prefs.getString(KEY_USERNAME, null)
        // 🔹 2. Yoksa login/register’dan gelen isim
        val intentName = intent.getStringExtra(LoginActivity.EXTRA_USERNAME)
        currentUser = localName ?: intentName ?: "Kullanıcı"

        tvUserName.text = currentUser
        loadSavedAvatar()

        findViewById<MaterialToolbar>(R.id.topAppBar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bottom = findViewById(R.id.bottomNav)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(this, HomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(LoginActivity.EXTRA_USERNAME, currentUser)
                        }
                    )
                    true
                }
                R.id.nav_fav -> {
                    startActivity(
                        Intent(this, FavoritesActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(LoginActivity.EXTRA_USERNAME, currentUser)
                            putExtra(EXTRA_PREV, "Profile")
                        }
                    )
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
        bottom.menu.findItem(R.id.nav_profile).isChecked = true

        findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java).apply {
                putExtra("username", currentUser)
                loadAvatarUri(currentUser)?.let { putExtra("avatarUri", it) }
            }
            editProfileLauncher.launch(intent)
        }


        findViewById<MaterialButton>(R.id.btnDeleteAccount).setOnClickListener {
            confirmAndDeleteAccount()
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            logout()
        }

        findViewById<MaterialButton>(R.id.btnFeedback).setOnClickListener {
            startActivity(
                Intent(this, FeedbackActivity2::class.java).apply {
                    putExtra(LoginActivity.EXTRA_USERNAME, currentUser)
                }
            )
        }
    }

    private fun confirmAndDeleteAccount() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hesabı Sil")
            .setMessage("“$currentUser” hesabını silmek istediğine emin misin?")
            .setNegativeButton("İptal", null)
            .setPositiveButton("Sil") { _, _ -> performAccountDeletion() }
            .show()
    }

    private fun performAccountDeletion() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.api(this@ProfileActivity)
                val resp = api.deleteMe()

                TokenStore.clearToken(this@ProfileActivity)
                prefs.edit()
                    .putBoolean(KEY_REMEMBER, false)
                    .remove(KEY_USERNAME)
                    .remove(LoginActivity.EXTRA_ROLE)
                    .remove(AVATAR_PREF_PREFIX + currentUser)
                    .apply()

                Toast.makeText(
                    this@ProfileActivity,
                    resp.message.ifBlank { "Hesap silindi" },
                    Toast.LENGTH_SHORT
                ).show()

                val i = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(i)
                finish()

            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401 -> "Oturum geçersiz. Lütfen yeniden giriş yapın."
                    404 -> "Kullanıcı bulunamadı."
                    else -> "Silme başarısız (HTTP ${e.code()})"
                }
                Toast.makeText(this@ProfileActivity, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Ağ/istemci hatası: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun saveAvatarUri(user: String, uriString: String) {
        prefs.edit().putString(AVATAR_PREF_PREFIX + user, uriString).apply()
    }

    private fun loadAvatarUri(user: String): String? {
        return prefs.getString(AVATAR_PREF_PREFIX + user, null)
    }

    private fun loadSavedAvatar() {
        val saved = loadAvatarUri(currentUser) ?: return
        val uri = Uri.parse(saved)
        if (saved.startsWith("file://")) {
            val path = saved.removePrefix("file://")
            val bm = BitmapFactory.decodeFile(path)
            if (bm != null) avatarImage.setImageBitmap(bm)
        } else {
            runCatching { avatarImage.setImageURI(uri) }
        }
    }

    private fun setAvatarFromUri(uri: Uri) {
        avatarImage.setImageURI(uri)
    }

    private fun logout() {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, false)
            .remove(KEY_USERNAME)
            .apply()

        val i = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(i)
        finish()
    }

    override fun onResume() {
        super.onResume()
        bottom.menu.findItem(R.id.nav_profile).isChecked = true
    }
}
