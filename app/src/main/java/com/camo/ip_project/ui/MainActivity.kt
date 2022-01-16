package com.camo.ip_project.ui


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.camo.ip_project.R
import com.camo.ip_project.databinding.ActivityMainBinding
import com.camo.ip_project.ui.viewmodels.MainActivityVM
import com.camo.ip_project.util.LuminosityAnalyzer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val viewModel: MainActivityVM by viewModels()
    private lateinit var binding: ActivityMainBinding

    private var camera: Camera? = null

    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var analyse = false

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(
            LayoutInflater.from(this)
        )
        setContentView(binding.root)

        Timber.i("Main Activity launched")

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.cameraCaptureButton.setOnClickListener { toggleAnalysis() }

        cameraExecutor = Executors.newSingleThreadExecutor()

//        analyzer =
        setListeners()
    }

    private fun setListeners() {
        lifecycleScope.launchWhenStarted {
            viewModel.beatState.collect {
                binding.textView.text = it.data.toString()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun toggleAnalysis() {
        if (analyse) {
            detachAnalyzer()
            camera?.cameraControl?.enableTorch(false)
        } else {
            attachAnalyzer()
            camera?.cameraControl?.enableTorch(true)
        }
        analyse = !analyse
    }

    private fun bindCameraUseCase() {
        try {
            // Unbind use cases before rebinding
            cameraProvider?.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

        } catch (exc: Exception) {
            Timber.e("Use case binding failed $exc")
        }
    }

    private fun attachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
        imageAnalyzer?.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
            viewModel.setBeats(luma)
        })
    }

    private fun detachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER).build()

            bindCameraUseCase()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

