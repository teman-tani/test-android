package com.example.capstoneproject.data.repository

import androidx.lifecycle.LiveData
import androidx.paging.*
import com.example.capstoneproject.data.api.ApiService
import com.example.capstoneproject.data.api.ListStoryItem
import com.raassh.dicodingstoryapp.data.database.StoryDatabase
import com.example.capstoneproject.data.paging.StoryRemoteMediator
import okhttp3.MultipartBody
import okhttp3.RequestBody

class StoryRepository(
    private val database: StoryDatabase,
    private val apiService: ApiService,
    private val auth: String
) {
    fun getStoriesPaged(): LiveData<PagingData<ListStoryItem>> {
        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(
                pageSize = 5
            ),
            remoteMediator = StoryRemoteMediator(database, apiService, auth),
            pagingSourceFactory = {
                database.getStoryDao().getAllStories()
            }
        ).liveData
    }

    suspend fun getStoriesWithLocation() =
        apiService.getAllStories(auth, 1).listStory

    suspend fun addNewStory(
        multipart: MultipartBody.Part,
        params: HashMap<String, RequestBody>
    ) =
        !apiService.addStory(multipart, params, auth).error
}