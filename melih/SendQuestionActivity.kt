package com.melihberat.mobiluyg

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class SendQuestionActivity : AppCompatActivity() {

    data class SelectedFile(
        val uri: Uri,
        val name: String
    )

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnAddFile: MaterialButton
    private lateinit var btnSendFiles: MaterialButton
    private lateinit var tvFileHeader: TextView
    private lateinit var rvFiles: RecyclerView
    private lateinit var progressUpload: LinearProgressIndicator
    private lateinit var tvProgressLabel: TextView

    private val files = mutableListOf<SelectedFile>()
    private lateinit var adapter: FilesAdapter

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult

        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}

            val name = queryDisplayName(contentResolver, uri) ?: "Dosya"
            if (files.none { it.uri == uri }) {
                files.add(SelectedFile(uri, name))
            }
        }
        adapter.notifyDataSetChanged()
        updateFileHeader()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_question)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBarSend)
        bottomNav = findViewById(R.id.bottomNavSend)
        btnAddFile = findViewById(R.id.btnAddFile)
        btnSendFiles = findViewById(R.id.btnSendFiles)
        tvFileHeader = findViewById(R.id.tvFileHeader)
        rvFiles = findViewById(R.id.rvFiles)
        progressUpload = findViewById(R.id.progressUpload)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    true
                }
                R.id.nav_fav -> {
                    startActivity(Intent(this, FavoritesActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    true
                }
                else -> false
            }
        }
        bottomNav.menu.findItem(R.id.nav_profile).isChecked = true

        adapter = FilesAdapter(files) { position ->
            files.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateFileHeader()
        }
        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = adapter

        btnAddFile.setOnClickListener {
            pickFilesLauncher.launch(arrayOf("*/*"))
        }

        btnSendFiles.setOnClickListener {
            if (files.isEmpty()) {
                Toast.makeText(this, "Lütfen en az bir dosya seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Buraya gerçek upload isteğini bağlayacaksın
            startFakeUpload()
        }

        updateFileHeader()
        resetProgress()
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return null
            return if (it.moveToFirst()) it.getString(nameIndex) else null
        }
    }

    private fun updateFileHeader() {
        tvFileHeader.text = "Yüklenecek dosyalar (${files.size})"
    }

    private fun resetProgress() {
        progressUpload.progress = 0
        tvProgressLabel.text = "Yükleniyor..."
    }

    private fun startFakeUpload() {
        // TODO: Retrofit ile sunucuya dosya yükleme ekle
        progressUpload.progress = 100
        tvProgressLabel.text = "Yükleme tamamlandı"
        Toast.makeText(this, "Dosyalar başarıyla gönderildi", Toast.LENGTH_SHORT).show()
    }

    class FilesAdapter(
        private val items: List<SelectedFile>,
        private val onRemoveClick: (Int) -> Unit
    ) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

        inner class FileViewHolder(val root: android.view.View) :
            RecyclerView.ViewHolder(root) {
            val tvName: TextView = root.findViewById(R.id.tvFileName)
            val ivRemove: android.widget.ImageView = root.findViewById(R.id.ivRemove)
            val ivIcon: android.widget.ImageView = root.findViewById(R.id.ivFileIcon)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_row, parent, false)
            return FileViewHolder(view)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.ivIcon.setImageResource(R.drawable.ic_file_24)

            holder.ivRemove.setOnClickListener {
                val pos = holder.adapterPosition  
                if (pos != RecyclerView.NO_POSITION) {
                    onRemoveClick(pos)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
