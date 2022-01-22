package com.camo.ip_project.ui


import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Range
import android.util.Size
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
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
import com.camo.ip_project.util.BeatDataType
import com.camo.ip_project.util.Constants.IMAGE_HEIGHT
import com.camo.ip_project.util.Constants.IMAGE_WIDTH
import com.camo.ip_project.util.HRAnalyzer
import com.camo.ip_project.util.LuminosityAnalyzer
import com.camo.ip_project.util.NaiveAnalyzer
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
                if (it.data != null) binding.textView.text = if (it.data==-1) "MC" else it.data.toString()
                else binding.textView.text = "-"
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.beat.collect {
                binding.ivHeart.setImageResource(if (it) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24)
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
            viewModel.resetBeatData()
        } else {
            attachAnalyzer()
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
        imageAnalyzer?.setAnalyzer(cameraExecutor, NaiveAnalyzer { luma ->
            when (luma.beatDataType) {
                BeatDataType.ANALYSIS -> {
                    Timber.d("imgAvg: ${luma.imgAvg}, time: ${luma.time}")
                }
                BeatDataType.FINAL -> {
                    viewModel.setBeats(luma.beats!!)
                    detachAnalyzer()
                    analyse = false
                }
                BeatDataType.Estimated ->{
                    viewModel.setBeats(luma.beats!!)
                }
                BeatDataType.Progress -> {
                    binding.progressBar.setProgress(luma.progress!!, true)
                    Timber.d("${luma.progress!!}")
                }
                BeatDataType.Beat -> {
                    viewModel.beat()
                }
                BeatDataType.Error ->{
                    viewModel.setBeats(-1)
                    detachAnalyzer()
                    analyse = false
                }
            }

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

            val builder = ImageAnalysis.Builder()
            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
//            ext.setCaptureRequestOption(
//                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
//                Range<Int>(16, 17)
//            )
            ext.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
          imageAnalyzer = builder
          .setTargetResolution(Size(IMAGE_WIDTH, IMAGE_HEIGHT))
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

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

