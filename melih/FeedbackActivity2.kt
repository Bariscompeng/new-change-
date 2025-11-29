package com.melihberat.mobiluyg

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class FeedbackActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback2)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val etFeedback = findViewById<EditText>(R.id.etFeedback)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bottomNav.menu.findItem(R.id.nav_profile).isChecked = true

        btnSend.setOnClickListener {
            val text = etFeedback.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Lütfen bir geri bildirim yazın", Toast.LENGTH_SHORT).show()
            } else {
                // Şimdilik sadece mesaj gösteriyoruz
                Toast.makeText(this, "Geri bildiriminiz için teşekkürler!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
