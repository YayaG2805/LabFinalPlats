package com.guevara.diego.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guevara.diego.data.datastore.PreferencesManager
import com.guevara.diego.data.local.AssetEntity
import com.guevara.diego.data.repository.AssetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Estados UI para la pantalla de detalle
 */
sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(
        val asset: AssetEntity,
        val isOnline: Boolean
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

/**
 * ViewModel para la pantalla de detalle de Asset
 */
class DetailViewModel(
    private val assetId: String,
    private val repository: AssetRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val lastSavedDate: StateFlow<String?> = PreferencesManager.lastUpdateFlow(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        loadAssetDetail()
    }

    /**
     * Carga el detalle del asset desde el repository
     */
    fun loadAssetDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading

            repository.getAssetById(assetId)
                .onSuccess { asset ->
                    // Verificar si viene del API o del cache
                    val isOnline = try {
                        val lastUpdate = LocalDateTime.parse(asset.lastUpdated)
                        val now = LocalDateTime.now()
                        val minutesDiff = java.time.Duration.between(lastUpdate, now).toMinutes()
                        minutesDiff < 5 // Si fue actualizado hace menos de 5 minutos
                    } catch (e: Exception) {
                        false
                    }

                    _uiState.value = DetailUiState.Success(
                        asset = asset,
                        isOnline = isOnline
                    )
                }
                .onFailure { error ->
                    _uiState.value = DetailUiState.Error(
                        message = error.message ?: "No se pudo cargar la información"
                    )
                }
        }
    }

    /**
     * Factory para crear el ViewModel con dependencias
     */
    class Factory(
        private val assetId: String,
        private val repository: AssetRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                return DetailViewModel(assetId, repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}