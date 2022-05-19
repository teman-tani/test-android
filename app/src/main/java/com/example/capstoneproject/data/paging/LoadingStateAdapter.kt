package com.example.capstoneproject.data.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.raassh.dicodingstoryapp.databinding.LoadingBinding

class LoadingStateAdapter(private val retry: () -> Unit) :
    LoadStateAdapter<LoadingStateAdapter.ViewHolder>() {
    class ViewHolder(private val binding: LoadingBinding, retry: () -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.retryButton.setOnClickListener { retry.invoke() }
        }

        fun bind(loadState: LoadState) {
            binding.apply {
                if (loadState is LoadState.Error) {
                    errorText.text = loadState.error.localizedMessage
                }

                progressBar.isVisible = loadState is LoadState.Loading
                loadingText.isVisible = loadState is LoadState.Loading
                retryButton.isVisible = loadState is LoadState.Error
                errorText.isVisible = loadState is LoadState.Error
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val binding = LoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding, retry)
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }
}