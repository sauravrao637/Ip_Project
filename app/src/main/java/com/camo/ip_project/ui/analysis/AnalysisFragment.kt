/****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 *                                                                                       *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.ui.analysis

import android.content.SharedPreferences
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Range
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
import com.camo.ip_project.databinding.FragmentAnalysisBinding
import com.camo.ip_project.ui.BaseActivity
import com.camo.ip_project.ui.Utility.OUTPUT_DATA
import com.camo.ip_project.util.RAnalyzer
import com.camo.ip_project.util.Status
import com.jjoe64.graphview.series.LineGraphSeries
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
    This fragment is responsible for handling the hrv analysis using the camerax api
 */
@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class AnalysisFragment : Fragment() {

    private lateinit var baseActivity: BaseActivity
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisFragmentViewModel by viewModels()

    //camera
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        val root: View = binding.root
        baseActivity = activity as BaseActivity
        sharedPreferences = baseActivity.sharedPreferences
        Timber.d("dbg: ${allowDebugging()}")
        setupUi()
        listeners()
        return root
    }

    private fun setupUi() {
        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.graphView.apply {
            viewport.isXAxisBoundsManual = true
            viewport.setMinX(0.0)
            viewport.setMaxX(10.0)
            addSeries(viewModel.mSeries1)
            viewport.setDrawBorder(true)
        }
        binding.graphView2.apply {
            viewport.setDrawBorder(true)
            viewport.isScrollable = true
            viewport.isScalable = true
        }
    }

    private fun listeners() {
        binding.cameraCaptureButton.setOnClickListener {
            viewModel.toggleAnalysis()
        }
        lifecycleScope.launchWhenStarted {
            viewModel.analysisState.collect {
                if (it) {
                    withContext(Dispatchers.Main) {
                        attachAnalyzer()
                        binding.cameraCaptureButton.text = requireContext().getText(R.string.cancel)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        detachAnalyzer()
                        binding.cameraCaptureButton.text = requireContext().getText(R.string.start)
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.analysedData.collect {
                when (it.status) {
                    Status.ERROR -> {
                        withContext(Dispatchers.Main) {
                            binding.graphView.visibility = View.GONE
                            binding.graphView2.visibility = View.GONE
                            Toast.makeText(context, it.errorInfo, Toast.LENGTH_SHORT).show()
                        }
                        binding.cameraCaptureButton.isEnabled = true
                    }
                    Status.IDLE -> {
                        binding.cameraCaptureButton.isEnabled = true
                        binding.result.apply {
                            tvBpm.text = ""
                            tvRmssd.text = ""
                            tvSdnn.text = ""
                            tvNni.text = ""
                        }
                        binding.graphView.visibility = View.VISIBLE
                        binding.graphView2.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Hold a sec", Toast.LENGTH_SHORT).show()
                        }
                        binding.cameraCaptureButton.isEnabled = false
                    }
                    Status.SUCCESS -> {
                        (activity as BaseActivity).writeToTxtFile(OUTPUT_DATA, it.data!!.outText)
                        binding.result.apply {
                            tvBpm.text = it.data.bpm.toString()
                            tvRmssd.text = it.data.rmssd.toString()
                            tvSdnn.text = it.data.sd.toString()
                            tvNni.text = it.data.NNI.toString()
                        }
                        binding.cameraCaptureButton.isEnabled = true
                        binding.graphView.visibility = View.GONE
                        binding.graphView2.visibility = View.VISIBLE
                        binding.graphView2.removeAllSeries()
                        binding.graphView2.addSeries(LineGraphSeries(it.data.resampledData))
                    }
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.analysisProgress.collect {
                withContext(Dispatchers.Main) {
                    binding.progressBar.setProgress(it, true)
                }
            }
        }
    }

    private fun attachAnalyzer() {
        imageAnalyzer?.clearAnalyzer()
        imageAnalyzer?.setAnalyzer(
            cameraExecutor,
            RAnalyzer(
                progressListener = { progress -> viewModel.updateProgress(progress) },
                endListener = {
                    viewModel.processingComplete()
                },
                signalListener = { rAvg, t ->
                    viewModel.signalListener(rAvg, t)
                },
                errorListener = { error ->
                    viewModel.errorInAnalysis(error)
                },
                cameraStabilizingTime = baseActivity.getPreferredCST()
            )
        )
    }

    private fun allowDebugging() = baseActivity.allowDebugging()
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
            ext.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
//            To set frame rates
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 1)
            imageAnalyzer =
                builder
//                .setTargetResolution(Size(Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT))
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