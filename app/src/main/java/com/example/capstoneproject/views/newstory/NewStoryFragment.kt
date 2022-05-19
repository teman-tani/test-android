package com.example.capstoneproject.views.newstory

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.customviews.EditTextWithValidation
import com.example.capstoneproject.data.api.ApiConfig
import com.raassh.dicodingstoryapp.data.database.StoryDatabase
import com.example.capstoneproject.data.repository.StoryRepository
import com.raassh.dicodingstoryapp.databinding.NewStoryFragmentBinding
import com.raassh.dicodingstoryapp.misc.*
import com.example.capstoneproject.views.cameraview.CameraFragment
import java.io.File

class NewStoryFragment : Fragment() {
    private val viewModel by viewModels<NewStoryViewModel> {
        NewStoryViewModel.Factory(
            StoryRepository(
                StoryDatabase.getDatabase(context as Context),
                ApiConfig.getApiService(),
                getString(R.string.auth, token)
            )
        )
    }

    private var binding: NewStoryFragmentBinding? = null

    private var imgFile: File? = null
    private var token = ""

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location: Location? = null
    private val cts = CancellationTokenSource()

    private val launcherPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!allPermissionGranted()) {
            binding?.root?.let {
                showSnackbar(it, getString(R.string.permission_denied))
            }

            findNavController().navigateUp()
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            val selectedImage = it.data?.data as Uri
            imgFile = uriToFile(selectedImage, context as Context)
            binding?.previewImage?.setImageURI(selectedImage)

            binding?.root?.let { root ->
                showSnackbar(root, getString(R.string.load_picture_success))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.apply {
            show()
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = NewStoryFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading(false)
        token = NewStoryFragmentArgs.fromBundle(arguments as Bundle).token

        if (!allPermissionGranted()) {
            launcherPermissionRequest.launch(REQUIRED_PERMISSIONS)
        }

        setFragmentResultListener(CameraFragment.CAMERA_RESULT) { _, bundle ->
            showCameraResult(bundle)
        }

        binding?.apply {
            descriptionInput.setValidationCallback(object : EditTextWithValidation.InputValidation {
                override val errorMessage: String
                    get() = getString(R.string.desc_validation_message)

                override fun validate(input: String) = input.isNotEmpty()
            })

            cameraButton.setOnClickListener {
                findNavController().navigate(R.id.action_newStoryFragment_to_cameraFragment)
            }

            galleryButton.setOnClickListener {
                pickImageFromGallery()
            }

            addButton.setOnClickListener {
                addStory()
            }

            locationText.visibility = View.GONE

            locationButton.setOnClickListener {
                getLocation()
            }
        }

        viewModel.apply {
            isLoading.observe(viewLifecycleOwner) {
                showLoading(it)
            }

            isSuccess.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { success ->
                    if (success) {
                        storyAdded()
                    }
                }
            }

            error.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { message ->
                    binding?.root?.let { root ->
                        if (message.isEmpty()) {
                            showSnackbar(root, getString(R.string.generic_error))
                        } else {
                            showSnackbar(root, message)
                        }
                    }
                }
            }
        }

        activity?.let {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
        }
    }

    private fun showCameraResult(bundle: Bundle) {
        val uri = bundle.getParcelable<Uri>(CameraFragment.PICTURE) as Uri
        val isBackCamera = bundle.get(CameraFragment.IS_BACK_CAMERA) as Boolean

        imgFile = uriToFile(uri, context as Context)
        val result = rotateBitmap(
            BitmapFactory.decodeFile(imgFile?.path),
            isBackCamera
        )

        binding?.apply {
            previewImage.setImageBitmap(result)
            showSnackbar(root, getString(R.string.take_picture_success))
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }

        val chooser = Intent.createChooser(intent, getString(R.string.chooser_title))
        launcherIntentGallery.launch(chooser)
    }

    private fun addStory() {
        hideSoftKeyboard(activity as FragmentActivity)

        with(binding ?: return) {
            if (!descriptionInput.validateInput() || imgFile == null) {
                showSnackbar(root, getString(R.string.validation_error))
                return
            }

            viewModel.addNewStory(
                imgFile as File,
                descriptionInput.text.toString(),
                location
            )
        }
    }

    private fun storyAdded() {
        val action = NewStoryFragmentDirections.actionNewStoryFragmentToStoriesFragment().apply {
            token = this@NewStoryFragment.token
            newStoryAdded = true
        }

        findNavController().navigate(action)
        binding?.root?.let {
            showSnackbar(it, getString(R.string.upload_success))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        cts.cancel()
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity?.baseContext as Context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLoading(isLoading: Boolean) {
        binding?.apply {
            uploadGroup.visibility = visibility(!isLoading)
            uploadLoadingGroup.visibility = visibility(isLoading)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        showLoading(true)
        binding?.loadingText?.text = getString(R.string.location_loading)

        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cts.token
        )
            .addOnSuccessListener {
                showLoading(false)
                binding?.loadingText?.text = getString(R.string.uploading)

                location = it
                binding?.locationText?.apply {
                    text = getString(R.string.location, location?.latitude, location?.longitude)
                    visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                showLoading(false)
                binding?.loadingText?.text = getString(R.string.uploading)

                binding?.root?.let { root ->
                    showSnackbar(root, it.localizedMessage ?: getString(R.string.generic_error))
                }
            }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}