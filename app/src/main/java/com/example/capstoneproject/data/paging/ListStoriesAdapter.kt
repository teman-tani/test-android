package com.example.capstoneproject.data.paging

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.raassh.dicodingstoryapp.R
import com.example.capstoneproject.data.api.ListStoryItem
import com.raassh.dicodingstoryapp.databinding.StoryItemBinding
import kotlinx.coroutines.delay

class ListStoriesAdapter :
    PagingDataAdapter<ListStoryItem, ListStoriesAdapter.ViewHolder>(STORY_COMPARATOR) {
    private var onItemClickCallback: OnItemClickCallback? = null

    interface OnItemClickCallback {
        fun onItemClicked(story: ListStoryItem, storyBinding: StoryItemBinding)
    }

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    inner class ViewHolder(val binding: StoryItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            StoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { story ->
            holder.itemView.setOnClickListener {
                onItemClickCallback?.onItemClicked(story, holder.binding)
            }

            holder.binding.apply {
                storyImage.loadImage(story.photoUrl, object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })

                storyImage.contentDescription = holder.itemView.context.getString(
                    R.string.stories_content_description, story.name
                )

                storyImage.transitionName =
                    holder.itemView.context.getString(R.string.story_image, story.id)
                storyUser.text =
                    holder.itemView.context.getString(R.string.stories_user, story.name)
                storyUser.transitionName =
                    holder.itemView.context.getString(R.string.story_user, story.id)
            }
        }
    }

    suspend fun submitDataWithCallback(data: PagingData<ListStoryItem>, callback: () -> Unit) {
        submitData(data)

        // really hacky, but without this sometimes the callback is called too fast
        delay(200)

        callback.invoke()
    }

    companion object {
        val STORY_COMPARATOR = object : DiffUtil.ItemCallback<ListStoryItem>() {
            override fun areItemsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem) =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem) =
                oldItem.name == newItem.name && oldItem.description == newItem.description
                        && oldItem.photoUrl == newItem.photoUrl
        }
    }
}