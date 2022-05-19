package com.example.capstoneproject.views.storieswithmaps

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.raassh.dicodingstoryapp.R
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.api.ListStoryItem
import com.raassh.dicodingstoryapp.data.database.StoryDatabase
import com.example.capstoneproject.data.repository.StoryRepository
import com.raassh.dicodingstoryapp.databinding.FragmentStoriesWithMapsBinding
import com.raassh.dicodingstoryapp.misc.showSnackbar
import com.raassh.dicodingstoryapp.misc.visibility

class StoriesWithMapsFragment : Fragment() {
    private var token = ""

    private val viewModel by viewModels<StoriesWithMapViewModel> {
        StoriesWithMapViewModel.Factory(
            StoryRepository(
                StoryDatabase.getDatabase(context as Context),
                ApiConfig.getApiService(),
                getString(R.string.auth, token)
            )
        )
    }

    private var binding: FragmentStoriesWithMapsBinding? = null

    private var focusedStory: ListStoryItem? = null

    private val callback = OnMapReadyCallback { googleMap ->
        setMapStyle(googleMap)

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
        }

        googleMap.setOnInfoWindowClickListener {
            val story = it.tag as ListStoryItem

            focusedStory = story
            findNavController().navigate(
                StoriesWithMapsFragmentDirections.actionStoriesWithMapsFragmentToStoryDetailFragment(
                    story
                )
            )
        }

        viewModel.stories.observe(viewLifecycleOwner) {
            googleMap.clear()

            it.forEach { story ->
                val latLng = LatLng(story.lat, story.lon)
                val marker = googleMap.addMarker(
                    MarkerOptions().position(latLng)
                        .title(getString(R.string.stories_content_description, story.name))
                        .snippet(getString(R.string.marker_snippet))
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                )
                marker?.tag = story
            }

            val story = focusedStory ?: it[(1..it.size).random() - 1]
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(story.lat, story.lon), 7f)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStoriesWithMapsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        token = StoriesWithMapsFragmentArgs.fromBundle(
            arguments as Bundle
        ).token

        viewModel.apply {
            isLoading.observe(viewLifecycleOwner) {
                showLoading(it)
            }

            error.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { message ->
                    binding?.root?.let { root ->
                        if (message.isEmpty()) {
                            showSnackbar(
                                root,
                                getString(R.string.generic_error),
                                getString(R.string.retry)
                            ) {
                                viewModel.getAllStories()
                            }
                        } else {
                            showSnackbar(root, message, getString(R.string.retry)) {
                                viewModel.getAllStories()
                            }
                        }
                    }
                }
            }
        }

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            context?.let {
                val success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        it,
                        R.raw.map_style
                    )
                )

                if (!success) {
                    Log.e(TAG, "Style parsing failed.")
                }
            }
        } catch (exception: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", exception)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding?.apply {
            storiesLoadingGroup.visibility = visibility(isLoading)
        }
    }

    companion object {
        private const val TAG = "StoriesWithMapsFragment"
    }
}