package com.ekenahmetfaruk.vigna_scan.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ekenahmetfaruk.vigna_scan.R
import com.ekenahmetfaruk.vigna_scan.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(requireContext(), "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            try {
                val bitmap =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(
                                requireContext().contentResolver,
                                selectedUri
                            )
                        ) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver,
                            selectedUri
                        )
                    }
                viewModel.analyze(bitmap)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Görüntü yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
        viewModel.loadModels()
        observeUiState()

        binding.btnCapture.setOnClickListener {
            captureImage()
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    bitmap?.let { viewModel.analyze(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Görüntü alınamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null

            val rotation = image.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotation != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            if (rotatedBitmap.config == Bitmap.Config.ARGB_8888) {
                rotatedBitmap
            } else {
                rotatedBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
        } catch (e: Exception) {
            Log.e("CameraFragment", "Bitmap dönüşüm hatası: ${e.message}")
            null
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CameraViewModel.UiState.Idle -> {
                        binding.loadingOverlay.visibility = View.GONE
                    }

                    is CameraViewModel.UiState.Loading -> {
                        binding.loadingOverlay.visibility = View.VISIBLE
                    }

                    is CameraViewModel.UiState.Success -> {
                        binding.loadingOverlay.visibility = View.GONE

                        val imagePath = saveBitmapToTemp(state.bitmap)
                        val model1Result = state.model1Result
                        val model2Result = state.model2Result

                        viewModel.resetState()

                        // Fragment hâlâ bu destination'da mı kontrol et
                        val navController = findNavController()
                        if (navController.currentDestination?.id == R.id.cameraFragment) {
                            val action = CameraFragmentDirections
                                .actionCameraToResult(
                                    model1Result = model1Result,
                                    model2Result = model2Result,
                                    imagePath = imagePath
                                )
                            navController.navigate(action)
                        }
                    }

                    is CameraViewModel.UiState.Error -> {
                        binding.loadingOverlay.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun saveBitmapToTemp(bitmap: Bitmap): String {
        val file = File(requireContext().cacheDir, "captured_leaf.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}