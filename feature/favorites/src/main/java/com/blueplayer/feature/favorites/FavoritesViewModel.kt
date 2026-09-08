package com.blueplayer.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.FavoritesRepository

class FavoritesViewModel(
    val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val favorites = favoritesRepository.favorites

    fun play(index: Int) {
        val list = favorites.value
        if (list.isNotEmpty()) playerController.playTracks(list, index)
    }
}

class FavoritesViewModelFactory(
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FavoritesViewModel(favoritesRepository, playerController) as T
}