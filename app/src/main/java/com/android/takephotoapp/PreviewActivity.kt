package com.android.takephotoapp

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class PreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHOTO_URI = "com.android.takephotoapp.EXTRA_PHOTO_URI"
    }

    private lateinit var ivCapturedPhoto: ImageView
    private lateinit var tvFilePath: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preview)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardInfo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        ivCapturedPhoto = findViewById<ImageView>(R.id.ivCapturedPhoto)
        tvFilePath = findViewById<TextView>(R.id.tvFilePath)
        btnBack = findViewById<Button>(R.id.btnBack)

        loadPhotoFromIntent()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadPhotoFromIntent() {
        val uriAsString = intent.getStringExtra(EXTRA_PHOTO_URI)

        if (uriAsString == null) {
            tvFilePath.text = getString(R.string.photo_not_available)
            return
        }

        val uri = Uri.parse(uriAsString)

        ivCapturedPhoto.setImageURI(null)
        ivCapturedPhoto.setImageURI(uri)

        displayFilePath(uri)
    }

    private fun displayFilePath(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "photo.jpg"

        val baseDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File(baseDirectory, "AndroidCoursePictures/$fileName")

        val fullPath = if (photoFile.exists()) {
            photoFile.absolutePath
        } else {
            uri.toString()
        }

        tvFilePath.text = fullPath
    }
}
