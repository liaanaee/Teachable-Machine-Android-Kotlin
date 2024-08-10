package com.example.penulisanilmiah.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.penulisanilmiah.R
import com.example.penulisanilmiah.databinding.ActivityMainBinding
import com.example.penulisanilmiah.helper.getImageUri
import com.example.penulisanilmiah.jenis.JenisActivity
import com.example.penulisanilmiah.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var loadImageButton: Button
    private lateinit var analyzeButton: Button
    private lateinit var imageView: ImageView
    private lateinit var capturepict: Button
    private var currentImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null

    private val labels = arrayOf(
        "Ikan Neon Tetra",
        "Ikan Sapu-Sapu",
        "Ikan Lemon",
        "Ikan Mas Koki",
        "Tidak Diketahui"
    )

    private val CONFIDENCE_THRESHOLD = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUIComponents()
        setupListeners()

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile -> {
                    val intent = Intent(this, DetailActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.info -> {
                    val intent = Intent(this, JenisActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(menuItem)
            }
        }
    }

    private fun initializeUIComponents() {
        imageView = binding.previewImageView
        loadImageButton = binding.galleryButton
        analyzeButton = binding.analyzeButton
        capturepict = binding.cameraButton
    }

    private fun setupListeners() {
        loadImageButton.setOnClickListener {
            handleLoadImage()
        }
        analyzeButton.setOnClickListener {
            selectedBitmap?.let { bitmap ->
                generateOutput(bitmap)
            } ?: Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
        capturepict.setOnClickListener {
            tryCamera()
        }
    }

    private fun tryCamera() {
        val uri = getImageUri(this)
        currentImageUri = uri
        intentCamera.launch(uri)
    }

    private val intentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it))
            selectedBitmap = bitmap
        }
    }

    private fun handleLoadImage() {
        if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openGallery()
        } else {
            requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
        }
        onResult.launch(intent)
    }

    private fun isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission Denied !! Try again", Toast.LENGTH_SHORT).show()
        }
    }

    private val onResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onActivityResultReceived(result)
    }

    private fun onActivityResultReceived(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                imageView.setImageBitmap(bitmap)
                selectedBitmap = bitmap
            } ?: Log.e("TAG", "Error in selecting image")
        }
    }

    private fun generateOutput(bitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val tensorImage = TensorImage.fromBitmap(resizedBitmap)

        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]
                byteBuffer.putFloat((value shr 16 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value shr 8 and 0xFF) / 255.0f)
                byteBuffer.putFloat((value and 0xFF) / 255.0f)
            }
        }

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val model = ModelUnquant.newInstance(this)
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val (maxIndex, maxConfidence) = getMaxIndexWithConfidence(outputFeature0.floatArray)
        val label = if (maxConfidence >= CONFIDENCE_THRESHOLD) {
            labels[maxIndex]
        } else {
            "Tidak Diketahui"
        }

        model.close()

        val imageUri = saveImageToCache(bitmap)
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("label", label)
            putExtra("confidence", maxConfidence)
            putExtra("imageUri", imageUri.toString())
        }
        startActivity(intent)
    }

    private fun getMaxIndexWithConfidence(arr: FloatArray): Pair<Int, Float> {
        var maxIndex = 0
        var max = arr[0]
        for (i in arr.indices) {
            if (arr[i] > max) {
                max = arr[i]
                maxIndex = i
            }
        }
        return Pair(maxIndex, max)
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri {
        val filename = "fish_image_${System.currentTimeMillis()}.png"
        val cacheDir = externalCacheDir ?: cacheDir
        val file = File(cacheDir, filename)
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file.toUri()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.root.removeAllViews()
    }
}
