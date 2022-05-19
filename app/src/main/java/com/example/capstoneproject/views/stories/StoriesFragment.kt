package com.example.capstoneproject.views.stories

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.raassh.dicodingstoryapp.R
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.api.ListStoryItem
import com.raassh.dicodingstoryapp.data.database.StoryDatabase
import com.example.capstoneproject.data.paging.ListStoriesAdapter
import com.example.capstoneproject.data.paging.LoadingStateAdapter
import com.example.capstoneproject.data.repository.StoryRepository
import com.raassh.dicodingstoryapp.databinding.StoriesFragmentBinding
import com.raassh.dicodingstoryapp.databinding.StoryItemBinding
import com.example.capstoneproject.views.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class StoriesFragment : Fragment() {
    private var token = ""
    private var newStoryAdded = false

    private val viewModel by viewModels<StoriesViewModel> {
        StoriesViewModel.Factory(
            StoryRepository(
                StoryDatabase.getDatabase(context as Context),
                ApiConfig.getApiService(),
                getString(R.string.auth, token)
            )
        )
    }

    private var binding: StoriesFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val args = StoriesFragmentArgs.fromBundle(arguments as Bundle)
        token = args.token
        newStoryAdded = args.newStoryAdded
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) {
            (activity as AppCompatActivity).supportActionBar?.apply {
                show()
                setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = StoriesFragmentBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = if (activity?.applicationContext
                ?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            LinearLayoutManager(context)
        } else {
            GridLayoutManager(context, 2)
        }

        val listStoriesAdapter = ListStoriesAdapter().apply {
            setOnItemClickCallback(object : ListStoriesAdapter.OnItemClickCallback {
                override fun onItemClicked(
                    story: ListStoryItem,
                    storyBinding: StoryItemBinding
                ) {
                    val extras = FragmentNavigatorExtras(
                        storyBinding.storyImage to getString(
                            R.string.story_image,
                            story.id
                        ),
                        storyBinding.storyUser to getString(R.string.story_user, story.id)
                    )

                    view.findNavController().navigate(
                        R.id.action_storiesFragment_to_storyDetailFragment,
                        bundleOf(
                            "story" to story
                        ),
                        null,
                        extras
                    )
                }
            })
        }

        binding?.apply {
            listStory.apply {
                setHasFixedSize(true)
                this.layoutManager = layoutManager
                addItemDecoration(
                    DividerItemDecoration(
                        context,
                        layoutManager.orientation
                    )
                )

                adapter = listStoriesAdapter.withLoadStateFooter(
                    footer = LoadingStateAdapter {
                        listStoriesAdapter.retry()
                    }
                )
            }

            addNew.setOnClickListener {
                goToNewStory()
            }

            retryButtonInitial.setOnClickListener {
                listStoriesAdapter.retry()
            }

            offlineButtonInitial.setOnClickListener {
                listStory.isVisible = true
                emptyText.isVisible = true
                retryButtonInitial.isVisible = false
                errorTextInitial.isVisible = false
                offlineButtonInitial.isVisible = false
            }
        }

        viewModel.stories.observe(viewLifecycleOwner) {
            postponeEnterTransition()

            binding?.apply {
                viewLifecycleOwner.lifecycleScope.launch {
                    listStoriesAdapter.submitDataWithCallback(it) {
                        if (newStoryAdded) {
                            newStoryAdded = false
                            listStory.scrollToPosition(0)
                        }

                        emptyText.text = if (listStoriesAdapter.itemCount > 0) {
                            ""
                        } else {
                            getString(R.string.story_list_is_empty)
                        }
                    }
                }

                (view.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            listStoriesAdapter.loadStateFlow.collectLatest { loadStates ->
                binding?.apply {
                    listStory.isVisible = loadStates.refresh is LoadState.NotLoading
                    emptyText.isVisible = loadStates.refresh is LoadState.NotLoading
                    progressBarInitial.isVisible = loadStates.refresh is LoadState.Loading
                    loadingTextInitial.isVisible = loadStates.refresh is LoadState.Loading
                    retryButtonInitial.isVisible = loadStates.refresh is LoadState.Error
                    errorTextInitial.isVisible = loadStates.refresh is LoadState.Error
                    offlineButtonInitial.isVisible = loadStates.refresh is LoadState.Error
                }
            }
        }
    }

    private fun goToNewStory() {
        val navigateAction = StoriesFragmentDirections
            .actionStoriesFragmentToNewStoryFragment()
        navigateAction.token = token

        findNavController().navigate(navigateAction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.map).isVisible = true
    }
}