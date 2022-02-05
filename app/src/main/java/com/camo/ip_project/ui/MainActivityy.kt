package com.camo.ip_project.ui


import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.camo.ip_project.R
import com.camo.ip_project.databinding.ActivityyMainBinding
import com.camo.ip_project.ui.Utility.saveHrvData
import com.camo.ip_project.ui.viewmodels.MainActivityyVM
import com.camo.ip_project.util.Constants.IMAGE_HEIGHT
import com.camo.ip_project.util.Constants.IMAGE_WIDTH
import com.camo.ip_project.util.RAnalyzer
import com.camo.ip_project.util.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class MainActivityy : AppCompatActivity() {

    private var preview: Preview? = null
    private val viewModel: MainActivityyVM by viewModels()
    private lateinit var binding: ActivityyMainBinding

    //    camera
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    //    logic
    private var analyse = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityyMainBinding.inflate(
            LayoutInflater.from(this)
        )
        setContentView(binding.root)

        Timber.i("Main Activityy launched")

        startCamera()

        binding.cameraCaptureButton.setOnClickListener { toggleAnalysis() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setListeners()
    }

    private fun setListeners() {
        lifecycleScope.launchWhenStarted {
            viewModel.beatState.collect {
                when (it.status) {
                    Status.IDLE -> {
                        binding.tvBpm.text = "-"
                        binding.cameraCaptureButton.text = "Start"
                    }
                    Status.ERROR -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivityy,
                                it.errorInfo,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.tvBpm.text = "E"
                            endAnalysis()
                        }
                    }
                    Status.LOADING -> {
                        if (it.data != null) {
                            withContext(Dispatchers.Main) {
                                binding.tvBpm.text = it.data.bpm.toString()
                                binding.tvRmssd.text = it.data.rmssd.toString()
                            }
                        } else {
                            binding.tvBpm.text = "..."
                            binding.tvRmssd.text = ""
                        }
                    }
                    Status.SUCCESS -> {
                        withContext(Dispatchers.Main) {
                            binding.tvBpm.text = it.data!!.bpm.toString()
                            binding.tvRmssd.text = it.data.rmssd.toString()
                            endAnalysis()
                        }
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.beat.collect {
                binding.ivHeart.setImageResource(if (it) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.progress.collect{
                binding.progressBar.progress = it
            }
        }
    }

    private fun toggleAnalysis() {
        if (analyse) {
            binding.ivHeart.visibility = View.GONE
            detachAnalyzer()
            viewModel.resetBeatData()
        } else {
            binding.ivHeart.visibility = View.VISIBLE
            attachAnalyzer()
            binding.cameraCaptureButton.text = "Cancel"
        }
        analyse = !analyse
    }

    private fun endAnalysis() {
        analyse = false
        detachAnalyzer()
        binding.cameraCaptureButton.text = "Start"
        binding.ivHeart.visibility = View.GONE
    }

    private fun attachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
//        imageAnalyzer?.setAnalyzer(cameraExecutor, RAnalyzer { redAvgList,timestamps ->
//            saveHrvData(this@MainActivityy,redAvgList, timestamps)
//            lifecycleScope.launchWhenStarted {
//                withContext(Dispatchers.Main) { toggleAnalysis() }
//            }
//        })
    }

    private fun detachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val builder = ImageAnalysis.Builder()
            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
//            To set frame rates
//            ext.setCaptureRequestOption(
//                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
//                Range<Int>(16, 17)
//            )
            ext.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            imageAnalyzer = builder.setTargetResolution(Size(IMAGE_WIDTH, IMAGE_HEIGHT)).build()

            bindCameraUseCase()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCase() {
        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Timber.e("Use case binding failed $exc")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}

