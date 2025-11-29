package com.melihberat.mobiluyg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class OtpValidationActivity : AppCompatActivity() {

    private lateinit var et1: TextInputEditText
    private lateinit var et2: TextInputEditText
    private lateinit var et3: TextInputEditText
    private lateinit var et4: TextInputEditText
    private lateinit var btnContinue: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_validation)

        et1 = findViewById(R.id.etCode1)
        et2 = findViewById(R.id.etCode2)
        et3 = findViewById(R.id.etCode3)
        et4 = findViewById(R.id.etCode4)
        btnContinue = findViewById(R.id.btnOtpContinue)


        val filter = arrayOf<InputFilter>(InputFilter.LengthFilter(1))
        listOf(et1, et2, et3, et4).forEach { it.filters = filter }


        setAutoMove(et1, et2)
        setAutoMove(et2, et3)
        setAutoMove(et3, et4)


        btnContinue.isEnabled = false


        val all = listOf(et1, et2, et3, et4)
        all.forEach {
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    btnContinue.isEnabled = all.all { edit -> !edit.text.isNullOrEmpty() }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }


        et1.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et1, InputMethodManager.SHOW_IMPLICIT)


        btnContinue.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setAutoMove(current: TextInputEditText, next: TextInputEditText) {
        current.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    next.requestFocus()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
