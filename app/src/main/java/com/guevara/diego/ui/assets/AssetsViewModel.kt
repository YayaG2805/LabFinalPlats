package com.guevara.diego.ui.assets

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
import java.time.format.DateTimeFormatter

/**
 * Estados UI para la pantalla de lista
 */
sealed class AssetsUiState {
    object Loading : AssetsUiState()
    data class Success(
        val assets: List<AssetEntity>,
        val isOnline: Boolean
    ) : AssetsUiState()
    data class Error(
        val message: String,
        val hasLocalData: Boolean
    ) : AssetsUiState()
}

/**
 * ViewModel para la pantalla de lista de Assets
 */
class AssetsViewModel(
    private val repository: AssetRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssetsUiState>(AssetsUiState.Loading)
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    val lastSavedDate: StateFlow<String?> = PreferencesManager.lastUpdateFlow(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        loadAssets()
    }

    /**
     * Carga los assets desde el repository
     */
    fun loadAssets() {
        viewModelScope.launch {
            _uiState.value = AssetsUiState.Loading

            repository.getAssets()
                .onSuccess { assets ->
                    // Verificar si vienen del API o del cache
                    val isOnline = assets.firstOrNull()?.let { asset ->
                        // Si la fecha es muy reciente, asumimos que viene del API
                        try {
                            val lastUpdate = LocalDateTime.parse(asset.lastUpdated)
                            val now = LocalDateTime.now()
                            val minutesDiff = java.time.Duration.between(lastUpdate, now).toMinutes()
                            minutesDiff < 5 // Si fue actualizado hace menos de 5 minutos
                        } catch (e: Exception) {
                            false
                        }
                    } ?: false

                    _uiState.value = AssetsUiState.Success(
                        assets = assets,
                        isOnline = isOnline
                    )
                }
                .onFailure {
                    val cachedAssets = repository.getCachedAssets()
                    if (cachedAssets.isNotEmpty()) {
                        _uiState.value = AssetsUiState.Error(
                            message = "Sin conexión a internet",
                            hasLocalData = true
                        )
                        // Mostrar los datos cacheados
                        _uiState.value = AssetsUiState.Success(
                            assets = cachedAssets,
                            isOnline = false
                        )
                    } else {
                        _uiState.value = AssetsUiState.Error(
                            message = "Sin conexión y sin datos guardados",
                            hasLocalData = false
                        )
                    }
                }
        }
    }

    /**
     * Guarda los datos actuales para uso offline
     */
    fun saveForOffline() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is AssetsUiState.Success && currentState.isOnline) {
                // Guardar fecha y hora
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                val formattedDate = now.format(formatter)

                PreferencesManager.saveLastUpdate(context, formattedDate)

                // Los datos ya están en la BD gracias al repository
                // Solo notificamos al usuario (esto se maneja en la UI)
            }
        }
    }

    /**
     * Factory para crear el ViewModel con dependencias
     */
    class Factory(
        private val repository: AssetRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssetsViewModel::class.java)) {
                return AssetsViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}