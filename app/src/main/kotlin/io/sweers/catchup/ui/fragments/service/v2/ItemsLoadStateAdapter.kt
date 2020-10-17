package io.sweers.catchup.ui.fragments.service.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import io.sweers.catchup.R
import io.sweers.catchup.databinding.LoadStatusContainerBinding

class ItemsLoadStateAdapter(
  private val adapter: ItemsAdapter
) : LoadStateAdapter<NetworkStateItemViewHolder>() {

  init {
    setHasStableIds(true)
  }

  override fun onBindViewHolder(holder: NetworkStateItemViewHolder, loadState: LoadState) {
    holder.bindTo(loadState)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    loadState: LoadState
  ): NetworkStateItemViewHolder {
    return NetworkStateItemViewHolder(parent) { adapter.retry() }
  }
}

/**
 * A View Holder that can display a loading or have click action.
 * It is used to show the network state of paging.
 */
class NetworkStateItemViewHolder(
  parent: ViewGroup,
  private val retryCallback: () -> Unit
) : RecyclerView.ViewHolder(
  LayoutInflater.from(parent.context).inflate(R.layout.load_status_container, parent, false)
) {
  private val binding = LoadStatusContainerBinding.bind(itemView)
  private val progressBar = binding.progress
  private val errorMsg = binding.errorMessage
  private val retry = binding.retryButton
    .also {
      it.setOnClickListener { retryCallback() }
    }

  fun bindTo(loadState: LoadState) {
    progressBar.isVisible = loadState is Loading
    retry.isVisible = loadState is Error
    errorMsg.isVisible = !(loadState as? Error)?.error?.message.isNullOrBlank()
    errorMsg.text = (loadState as? Error)?.error?.message
  }
}