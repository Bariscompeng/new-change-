package com.melihberat.mobiluyg

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.melihberat.mobiluyg.Network.ApiClient
import com.melihberat.mobiluyg.Network.TokenStore
import com.melihberat.mobiluyg.Network.model.RegisterRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etRegPassword: TextInputEditText
    private lateinit var btnDoRegister: MaterialButton
    private lateinit var btnGoLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etRegPassword = findViewById(R.id.etRegPassword)
        btnDoRegister = findViewById(R.id.btnDoRegister)
        btnGoLogin = findViewById(R.id.btnGoLogin)

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnDoRegister.setOnClickListener {
            doRegister()
        }
    }

    private fun doRegister() {
        val name = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etRegPassword.text?.toString()?.trim().orEmpty()

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 4) {
            Toast.makeText(this, "Şifre en az 4 karakter olmalı", Toast.LENGTH_SHORT).show()
            return
        }

        btnDoRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ApiClient.api(this@RegisterActivity)
                val resp = api.register(RegisterRequest(name, email, password))

                // Token kaydet (backend akışı bozulmasın)
                TokenStore.saveTokenSync(this@RegisterActivity, resp.token)

                Toast.makeText(this@RegisterActivity, "Kayıt başarılı! Lütfen giriş yapın.", Toast.LENGTH_SHORT).show()

                // ✅ Kayıt sonrası LoginActivity’e yönlendirme
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                intent.putExtra("email", email) // istersen email alanını otomatik doldurmak için
                startActivity(intent)
                finish()

            } catch (e: HttpException) {
                when (e.code()) {
                    409 -> Toast.makeText(
                        this@RegisterActivity,
                        "Bu e-posta zaten kayıtlı",
                        Toast.LENGTH_LONG
                    ).show()
                    else -> Toast.makeText(
                        this@RegisterActivity,
                        "Kayıt başarısız (HTTP ${e.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Ağ/istemci hatası: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnDoRegister.isEnabled = true
            }
        }
    }
}

