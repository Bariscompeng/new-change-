package com.melihberat.mobiluyg

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.melihberat.mobiluyg.Network.ApiClient
import com.melihberat.mobiluyg.Network.ApiResult
import com.melihberat.mobiluyg.Network.safeCall
import com.melihberat.mobiluyg.Network.model.ChangePasswordRequest
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var tilPass: TextInputLayout
    private lateinit var tilPass2: TextInputLayout
    private lateinit var etPass: TextInputEditText
    private lateinit var etPass2: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        tilPass = findViewById(R.id.tilPass)
        tilPass2 = findViewById(R.id.tilPass2)
        etPass = findViewById(R.id.etPass)
        etPass2 = findViewById(R.id.etPass2)
        btnSave = findViewById(R.id.btnSave)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validate() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etPass.addTextChangedListener(watcher)
        etPass2.addTextChangedListener(watcher)

        btnSave.setOnClickListener {
            if (!validate()) return@setOnClickListener

            val newPass = etPass.text?.toString().orEmpty()

            lifecycleScope.launch {
                val result = safeCall {
                    ApiClient.api(this@ResetPasswordActivity)
                        .changePassword(ChangePasswordRequest(newPass))
                }

                when (result) {
                    is ApiResult.Success -> {
                        // Admin özel şifresini de yerelde güncel tut (LoginActivity bunu okuyacak)
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("ADMIN_SECRET", newPass)
                            .apply()

                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Şifre başarıyla değiştirildi",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Şifre değiştirilemedi: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun validate(): Boolean {
        val p1 = etPass.text?.toString().orEmpty()
        val p2 = etPass2.text?.toString().orEmpty()

        tilPass.error = if (p1.length < 4) "En az 4 karakter" else null
        tilPass2.error = when {
            p2.isEmpty() -> "Tekrar giriniz"
            p1 != p2 -> "Şifreler uyuşmuyor"
            else -> null
        }

        val ok = tilPass.error == null && tilPass2.error == null
        btnSave.isEnabled = ok
        return ok
    }
}
