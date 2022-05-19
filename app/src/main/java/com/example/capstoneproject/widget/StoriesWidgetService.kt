package com.example.capstoneproject.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.example.capstoneproject.widget.StoriesRemoteViewsFactory

class StoriesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(p0: Intent?): RemoteViewsFactory =
        StoriesRemoteViewsFactory(this.applicationContext)
}