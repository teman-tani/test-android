package com.example.capstoneproject.views.cameraview

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.databinding.FragmentCameraBinding
import com.raassh.dicodingstoryapp.misc.showSnackbar
import com.raassh.dicodingstoryapp.misc.timeStamp

class CameraFragment : Fragment() {
    private var binding: FragmentCameraBinding? = null

    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
        startCamera()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            captureImage.setOnClickListener {
                takePhoto()
            }

            switchCamera.setOnClickListener {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                startCamera()
            }
        }
    }

    private fun takePhoto() {
        val image = imageCapture ?: return

        val name = "$timeStamp.jpg"

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context?.contentResolver as ContentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Images.Media.TITLE, name)
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }
            )
            .build()

        image.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context as Context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setFragmentResult(
                        CAMERA_RESULT, bundleOf(
                            PICTURE to outputFileResults.savedUri,
                            IS_BACK_CAMERA to (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        )
                    )

                    findNavController().navigateUp()
                }

                override fun onError(exception: ImageCaptureException) {
                    binding?.root?.let {
                        showSnackbar(it, getString(R.string.take_picture_fail))
                    }
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context as Context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding?.viewFinder?.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                binding?.root?.let {
                    showSnackbar(it, getString(R.string.camera_fail))
                }
            }
        }, ContextCompat.getMainExecutor(context as Context))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val CAMERA_RESULT = "camera_result"
        const val PICTURE = "picture"
        const val IS_BACK_CAMERA = "is_back_camera"
    }
}