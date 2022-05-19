package com.example.capstoneproject.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.raassh.dicodingstoryapp.R
import com.raassh.dicodingstoryapp.data.SessionPreferences
import com.example.capstoneproject.data.api.ApiConfig
import com.example.capstoneproject.data.api.ListStoryItem
import com.example.capstoneproject.views.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal class StoriesRemoteViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {
    private val stories = ArrayList<ListStoryItem>()

    override fun onCreate() {
        //
    }

    override fun onDataSetChanged(): Unit = runBlocking {
        val pref = SessionPreferences.getInstance(context.dataStore)
        val auth = context.getString(R.string.auth, pref.getSavedToken().first())

        try {
            stories.clear()
            stories.addAll(
                ApiConfig.getApiService()
                .getAllStories(auth, 0).listStory)
        } catch (e: Exception) {
            Log.e(TAG, "onResponse: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        //
    }

    override fun getCount() = stories.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_story_item).apply {
            val image = Glide.with(context)
                .asBitmap()
                .load(stories[position].photoUrl)
                .submit()
                .get()

            setImageViewBitmap(R.id.story_image, image)
        }

        val extras = bundleOf(
            StoriesWidget.EXTRA_ITEM to stories[position].name
        )

        val fillIntent = Intent().apply {
            putExtras(extras)
        }

        rv.setOnClickFillInIntent(R.id.story_image, fillIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount() = 1

    override fun getItemId(p0: Int) = 0L

    override fun hasStableIds() = false

    companion object {
        private const val TAG = "StoriesRemoteViewsFacto"
    }
}