package com.example.party.ui

import androidx.lifecycle.ViewModel
import com.example.party.model.Discoteca
import com.example.party.model.mockDiscotecas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PartyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(mockDiscotecas)
    val uiState: StateFlow<List<Discoteca>> = _uiState

    private val _likedDiscos = MutableStateFlow<List<Discoteca>>(emptyList())
    val likedDiscos: StateFlow<List<Discoteca>> = _likedDiscos

    fun onLike(discoteca: Discoteca) {
        _likedDiscos.value = _likedDiscos.value + discoteca
        _uiState.value = _uiState.value.filter { it.id != discoteca.id }
    }

    fun onDislike(discoteca: Discoteca) {
        _uiState.value = _uiState.value.filter { it.id != discoteca.id }
    }
}
