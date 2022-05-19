package com.example.capstoneproject.views.storydetail

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.raassh.dicodingstoryapp.R
import com.example.capstoneproject.data.api.ListStoryItem
import com.raassh.dicodingstoryapp.databinding.StoryDetailFragmentBinding
import com.raassh.dicodingstoryapp.misc.withDateFormat

class StoryDetailFragment : Fragment() {
    private var binding: StoryDetailFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = StoryDetailFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        val story = StoryDetailFragmentArgs.fromBundle(arguments as Bundle).story
        showStory(story)
    }

    private fun showStory(story: ListStoryItem) {
        binding?.apply {
            storyUser.transitionName = getString(R.string.story_user, story.id)
            storyUser.text = getString(R.string.stories_user, story.name)
            storyUploaded.text =
                getString(R.string.stories_uploaded, story.createdAt.withDateFormat())
            storyDesc.text = getString(R.string.stories_desc, story.description)
            storyImage.transitionName = getString(R.string.story_image, story.id)
            storyImage.contentDescription = getString(
                R.string.stories_content_description, story.name
            )
            storyImage.loadImage(story.photoUrl, object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}