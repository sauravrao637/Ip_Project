package com.camo.ip_project.ui.home

import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.camo.ip_project.R
import com.camo.ip_project.databinding.FragmentHomeBinding
import com.camo.ip_project.util.Constants
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
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    //    camera
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()

        listeners()
        return root
    }

    private fun listeners() {
        binding.cameraCaptureButton.setOnClickListener { homeViewModel.toggleAnalysis() }

        lifecycleScope.launchWhenStarted {
            homeViewModel.analysisState.collect {
                Timber.d("it $it")
                if (it) {
                    withContext(Dispatchers.Main){
                        attachAnalyzer()
                        binding.cameraCaptureButton.text = requireContext().getText(R.string.cancel)
                    }
                } else {
                    withContext(Dispatchers.Main){
                        detachAnalyzer()
                        binding.cameraCaptureButton.text = requireContext().getText(R.string.start)
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            homeViewModel.analysedData.collect {
                when(it.status){
                    Status.ERROR ->{
                        withContext(Dispatchers.Main){
                            Toast.makeText(context,it.errorInfo,Toast.LENGTH_SHORT).show()
                        }
                    }
                    Status.IDLE ->{

                    }
                    Status.LOADING ->{
                        withContext(Dispatchers.Main){
                            Toast.makeText(context,"Hold a sec",Toast.LENGTH_SHORT).show()
                        }
                    }
                    Status.SUCCESS ->{
                        withContext(Dispatchers.Main){
                            binding.tvRmssd.text = it.data!!.rmssd.toString()
                            binding.tvBpm.text = it.data.bpm.toString()
                        }
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            homeViewModel.analysisProgress.collect {
                withContext(Dispatchers.Main){
                    binding.progressBar.setProgress(it,true)
                }
            }
        }
    }

    private fun attachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
        imageAnalyzer?.setAnalyzer(
            cameraExecutor,
            RAnalyzer(
                progressListener = { progress -> homeViewModel.updateProgress(progress) },
                endListener = {
                    homeViewModel.processingComplete()
                },
                signalListener = { rAvg, t ->
                                 homeViewModel.signalListener(rAvg,t)
                },
                errorListener = {
                    error -> homeViewModel.errorInAnalysis(error)
                }
            )
        )
    }

    private fun detachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
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
//                Range<Int>(30, 30)
//            )
            ext.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            imageAnalyzer =
                builder.setTargetResolution(Size(Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT))
                    .build()

            bindCameraUseCase()
        }, ContextCompat.getMainExecutor(requireContext()))
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

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}