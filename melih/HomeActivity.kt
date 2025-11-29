package com.melihberat.mobiluyg

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.melihberat.mobiluyg.Network.ApiClient
import com.melihberat.mobiluyg.Network.model.SchoolDto
import com.melihberat.mobiluyg.Network.model.DepartmentDto
import com.melihberat.mobiluyg.Network.model.LessonDto
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var bottom: BottomNavigationView

    private lateinit var tvWelcome: TextView
    private lateinit var cardFaculty: MaterialCardView
    private lateinit var cardDepartment: MaterialCardView
    private lateinit var cardLesson: MaterialCardView
    private lateinit var cardExamType: MaterialCardView
    private lateinit var cardGetir: MaterialCardView

    private lateinit var tvFacultyValue: TextView
    private lateinit var tvDepartmentValue: TextView
    private lateinit var tvLessonValue: TextView
    private lateinit var tvExamTypeValue: TextView

    // DB’den gelecek listeler
    private var schools: List<SchoolDto> = emptyList()
    private var departments: List<DepartmentDto> = emptyList()
    private var lessons: List<LessonDto> = emptyList()
    private val examTypes = listOf("Vize", "Final", "Bütünleme")

    // Seçili öğeler
    private var selectedSchool: SchoolDto? = null
    private var selectedDepartment: DepartmentDto? = null
    private var selectedLesson: LessonDto? = null
    private var selectedExamType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // View referansları
        tvWelcome = findViewById(R.id.tvWelcome)

        cardFaculty = findViewById(R.id.cardFaculty)
        cardDepartment = findViewById(R.id.cardDepartment)
        cardLesson = findViewById(R.id.cardLesson)
        cardExamType = findViewById(R.id.cardExamType)
        cardGetir = findViewById(R.id.cardGetir)

        tvFacultyValue = findViewById(R.id.tvFacultyValue)
        tvDepartmentValue = findViewById(R.id.tvDepartmentValue)
        tvLessonValue = findViewById(R.id.tvLessonValue)
        tvExamTypeValue = findViewById(R.id.tvExamTypeValue)

        val username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME)
            ?.takeIf { it.isNotBlank() } ?: "Melih"
        tvWelcome.text = "Hoş geldin $username"

        // ----- Kart tıklamaları -----

        // 1) Okul (fakülte gibi)
        cardFaculty.setOnClickListener {
            lifecycleScope.launch {
                val api = ApiClient.api(this@HomeActivity)
                try {
                    if (schools.isEmpty()) {
                        schools = api.getSchools()
                    }
                    if (schools.isEmpty()) {
                        Toast.makeText(
                            this@HomeActivity,
                            "Kayıtlı okul bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    showSchoolDialog()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Okul listesi alınamadı: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // 2) Bölüm (seçili okula göre)
        cardDepartment.setOnClickListener {
            val school = selectedSchool
            if (school == null) {
                Toast.makeText(this, "Önce fakülte/okul seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val id = school.id
                if (id == null) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Seçilen okulun id bilgisi yok",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val api = ApiClient.api(this@HomeActivity)
                try {
                    departments = api.getDepartmentsBySchool(id)
                    if (departments.isEmpty()) {
                        Toast.makeText(
                            this@HomeActivity,
                            "Bu okulda bölüm bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    showDepartmentDialog()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Bölüm listesi alınamadı: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // 3) Ders (seçili bölüme göre)
        cardLesson.setOnClickListener {
            val dep = selectedDepartment
            if (dep == null) {
                Toast.makeText(this, "Önce bölüm seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val id = dep.id
                if (id == null) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Seçilen bölümün id bilgisi yok",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val api = ApiClient.api(this@HomeActivity)
                try {
                    lessons = api.getCoursesByDepartment(id)
                    if (lessons.isEmpty()) {
                        Toast.makeText(
                            this@HomeActivity,
                            "Bu bölümde ders bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    showLessonDialog()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Ders listesi alınamadı: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // 4) Sınav türü (şimdilik local liste)
        cardExamType.setOnClickListener {
            showExamTypeDialog()
        }

        // 5) Getir butonu – seçilen derse göre soru çek
        cardGetir.setOnClickListener {
            val school = selectedSchool
            val dep = selectedDepartment
            val lesson = selectedLesson
            val examType = selectedExamType

            if (school == null || dep == null || lesson == null || examType == null) {
                Toast.makeText(
                    this,
                    "Lütfen tüm alanları seçin",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val lessonId = lesson.id
            if (lessonId == null) {
                Toast.makeText(
                    this,
                    "Seçilen dersin id bilgisi yok",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val api = ApiClient.api(this@HomeActivity)
                    val questions = api.getQuestions(lessonId.toString())
                    Toast.makeText(
                        this@HomeActivity,
                        "${school.name} / ${dep.name ?: "İsimsiz bölüm"}\n" +
                                "${lesson.title} ($examType) - Soru sayısı: ${questions.size}",
                        Toast.LENGTH_LONG
                    ).show()

                    // İleride: Soru listesi activity'si açılabilir
                    // startActivity(Intent(this@HomeActivity, QuestionListActivity::class.java).apply { ... })

                } catch (e: Exception) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Sorular alınamadı: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Bottom navigation
        bottom = findViewById(R.id.bottomNav)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_fav -> {
                    startActivity(
                        Intent(this, FavoritesActivity::class.java)
                            .putExtra(LoginActivity.EXTRA_USERNAME, username)
                    )
                    true
                }
                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                            .putExtra(LoginActivity.EXTRA_USERNAME, username)
                    )
                    true
                }
                else -> false
            }
        }
        bottom.menu.findItem(R.id.nav_home).isChecked = true
    }

    override fun onResume() {
        super.onResume()
        bottom.menu.findItem(R.id.nav_home).isChecked = true
    }

    // ---------- Dialog yardımcıları ----------

    private fun showSchoolDialog() {
        val names = schools.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Fakülte / Okul seçin")
            .setItems(names) { _, which ->
                selectedSchool = schools[which]
                selectedDepartment = null
                selectedLesson = null

                tvFacultyValue.text = selectedSchool?.name
                tvDepartmentValue.text = "Seçiniz"
                tvLessonValue.text = "Seçiniz"
            }
            .show()
    }

    private fun showDepartmentDialog() {
        val names = departments.map { it.name ?: "İsimsiz bölüm" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Bölüm seçin")
            .setItems(names) { _, which ->
                selectedDepartment = departments[which]
                selectedLesson = null

                tvDepartmentValue.text = selectedDepartment?.name ?: "İsimsiz bölüm"
                tvLessonValue.text = "Seçiniz"
            }
            .show()
    }

    private fun showLessonDialog() {
        val names = lessons.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Ders seçin")
            .setItems(names) { _, which ->
                selectedLesson = lessons[which]
                tvLessonValue.text = selectedLesson?.title
            }
            .show()
    }

    private fun showExamTypeDialog() {
        val types = examTypes.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Sınav türü seçin")
            .setItems(types) { _, which ->
                selectedExamType = types[which]
                tvExamTypeValue.text = selectedExamType
            }
            .show()
    }
}
