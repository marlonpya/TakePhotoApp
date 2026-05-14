package com.android.takephotoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var photoUri: Uri? = null

    private lateinit var btnTakePhoto: Button
    private lateinit var tvPermissionsStatus: TextView

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

            if (cameraGranted) {
                tvPermissionsStatus.text = getString(R.string.permissions_ok)
                launchCamera()
            } else {
                val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                if (showRationale) {
                    tvPermissionsStatus.text = getString(R.string.permission_camera_denied)
                } else {
                    tvPermissionsStatus.text = getString(R.string.permission_camera_denied_permanent)
                }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { photoTakenSuccessfully ->
            if (photoTakenSuccessfully) {
                photoUri?.let { uri ->
                    navigateToPreview(uri)
                }
            } else {
                tvPermissionsStatus.text = getString(R.string.photo_cancelled)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        tvPermissionsStatus = findViewById<TextView>(R.id.tvPermissionsStatus)

        updatePermissionsStatus()

        btnTakePhoto.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = buildPermissionsList()

        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            tvPermissionsStatus.text = getString(R.string.permissions_ok)
            launchCamera()
        } else {
            tvPermissionsStatus.text = getString(R.string.checking_permissions)
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun buildPermissionsList(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return permissions
    }

    private fun launchCamera() {
        val photoFile = createPhotoFile()

        if (photoFile == null) {
            tvPermissionsStatus.text = getString(R.string.error_creating_file)
            return
        }

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        takePictureLauncher.launch(photoUri!!)
    }

    private fun createPhotoFile(): File? {
        val baseDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return null

        val courseFolder = File(baseDirectory, "AndroidCoursePictures")
        courseFolder.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PHOTO_$timestamp.jpg"

        return File(courseFolder, fileName)
    }

    private fun navigateToPreview(uri: Uri) {
        val intent = Intent(this, PreviewActivity::class.java)
        intent.putExtra(PreviewActivity.EXTRA_PHOTO_URI, uri.toString())
        startActivity(intent)
    }

    private fun updatePermissionsStatus() {
        val hasCamera = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        tvPermissionsStatus.text = if (hasCamera) {
            getString(R.string.permissions_ok)
        } else {
            "Press the button to request the required permissions."
        }
    }
}
