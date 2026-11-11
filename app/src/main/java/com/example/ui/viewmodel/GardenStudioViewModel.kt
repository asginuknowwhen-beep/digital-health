package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.GardenDesign
import com.example.data.model.GardenPreferences
import com.example.data.model.PlacedPlant
import com.example.data.repository.GardenAiRepository
import com.example.data.repository.GardenRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface LayoutGenUiState {
    data object Idle : LayoutGenUiState
    data object Loading : LayoutGenUiState
    data class Error(val message: String) : LayoutGenUiState
}

sealed interface VisualGenUiState {
    data object Idle : VisualGenUiState
    data class Loading(val targetLabel: String) : VisualGenUiState
    data class Success(val filePath: String) : VisualGenUiState
    data class Error(val message: String) : VisualGenUiState
}

sealed interface CompanionUiState {
    data object Idle : CompanionUiState
    data object Loading : CompanionUiState
    data class Success(val suggestions: List<PlacedPlant>) : CompanionUiState
    data class Error(val message: String) : CompanionUiState
}

class GardenStudioViewModel(
    private val firestoreRepository: GardenRepository,
    private val aiRepository: GardenAiRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _preferences = MutableStateFlow(GardenPreferences())
    val preferences: StateFlow<GardenPreferences> = _preferences.asStateFlow()

    private val _referencePhoto = MutableStateFlow<Bitmap?>(null)
    val referencePhoto: StateFlow<Bitmap?> = _referencePhoto.asStateFlow()

    private val _layoutGenState = MutableStateFlow<LayoutGenUiState>(LayoutGenUiState.Idle)
    val layoutGenState: StateFlow<LayoutGenUiState> = _layoutGenState.asStateFlow()

    private val _activeGarden = MutableStateFlow<GardenDesign?>(null)
    val activeGarden: StateFlow<GardenDesign?> = _activeGarden.asStateFlow()

    private val _studioError = MutableStateFlow<String?>(null)
    val studioError: StateFlow<String?> = _studioError.asStateFlow()

    private val _plantVisualState = MutableStateFlow<VisualGenUiState>(VisualGenUiState.Idle)
    val plantVisualState: StateFlow<VisualGenUiState> = _plantVisualState.asStateFlow()

    private val _conceptVisualState = MutableStateFlow<VisualGenUiState>(VisualGenUiState.Idle)
    val conceptVisualState: StateFlow<VisualGenUiState> = _conceptVisualState.asStateFlow()

    private val _companionState = MutableStateFlow<CompanionUiState>(CompanionUiState.Idle)
    val companionState: StateFlow<CompanionUiState> = _companionState.asStateFlow()

    private var observeJob: Job? = null

    fun updatePreferences(transform: (GardenPreferences) -> GardenPreferences) {
        _preferences.value = transform(_preferences.value)
    }

    fun setReferencePhoto(bitmap: Bitmap?) {
        _referencePhoto.value = bitmap
    }

    fun loadGarden(gardenId: String) {
        observeJob?.cancel()
        _studioError.value = null
        _plantVisualState.value = VisualGenUiState.Idle
        _conceptVisualState.value = VisualGenUiState.Idle
        _companionState.value = CompanionUiState.Idle

        observeJob = viewModelScope.launch {
            firestoreRepository.observeGarden(currentUserId, gardenId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Failed to observe garden $gardenId", error)
                    _studioError.value = error.localizedMessage ?: "Failed to load garden."
                }
                .collect { design ->
                    _activeGarden.value = design
                }
        }
    }

    fun generateAndSaveGarden(onCreated: (String) -> Unit) {
        val prefs = _preferences.value
        val photo = _referencePhoto.value
        viewModelScope.launch {
            _layoutGenState.value = LayoutGenUiState.Loading
            val aiResult = aiRepository.generateGardenLayout(prefs, photo)
            if (aiResult.isFailure) {
                val msg = aiResult.exceptionOrNull()?.localizedMessage
                    ?: "Could not generate garden layout. Please try again."
                _layoutGenState.value = LayoutGenUiState.Error(msg)
                return@launch
            }
            val generated = aiResult.getOrThrow()
            val saveResult = firestoreRepository.createGarden(generated)
            if (saveResult.isFailure) {
                val msg = saveResult.exceptionOrNull()?.localizedMessage
                    ?: "Layout generated, but failed to save to cloud."
                _layoutGenState.value = LayoutGenUiState.Error(msg)
                return@launch
            }
            val newId = saveResult.getOrThrow()
            _activeGarden.value = generated.copy(id = newId, userId = currentUserId)
            _layoutGenState.value = LayoutGenUiState.Idle
            onCreated(newId)
        }
    }

    fun movePlantInMemory(plantId: String, newX: Double, newY: Double) {
        val current = _activeGarden.value ?: return
        val clampedX = newX.coerceIn(0.06, 0.94)
        val clampedY = newY.coerceIn(0.06, 0.94)
        val updatedPlants = current.plants.map { plant ->
            if (plant.id == plantId) plant.copy(x = clampedX, y = clampedY) else plant
        }
        _activeGarden.value = current.copy(plants = updatedPlants)
    }

    fun persistActiveGarden() {
        val current = _activeGarden.value ?: return
        if (current.id.isBlank()) return
        viewModelScope.launch {
            val result = firestoreRepository.updateGarden(current)
            if (result.isFailure) {
                _studioError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save garden changes."
            }
        }
    }

    fun updateGardenTitle(newTitle: String) {
        val current = _activeGarden.value ?: return
        val trimmed = newTitle.trim().take(120)
        if (trimmed.isBlank()) return
        val updated = current.copy(title = trimmed)
        _activeGarden.value = updated
        persistGarden(updated)
    }

    fun removePlant(plantId: String) {
        val current = _activeGarden.value ?: return
        val updated = current.copy(plants = current.plants.filterNot { it.id == plantId })
        _activeGarden.value = updated
        persistGarden(updated)
    }

    fun addOrSwapPlant(targetPlantId: String?, newPlant: PlacedPlant) {
        val current = _activeGarden.value ?: return
        val updatedList = if (targetPlantId != null) {
            current.plants.map { existing ->
                if (existing.id == targetPlantId) {
                    newPlant.copy(
                        id = existing.id,
                        x = existing.x,
                        y = existing.y,
                    )
                } else existing
            }
        } else {
            val offset = (current.plants.size % 5) * 0.08
            current.plants + newPlant.copy(
                x = (0.35 + offset).coerceIn(0.12, 0.88),
                y = (0.45 + offset).coerceIn(0.12, 0.88),
            )
        }
        val updated = current.copy(plants = updatedList)
        _activeGarden.value = updated
        _companionState.value = CompanionUiState.Idle
        persistGarden(updated)
    }

    fun requestCompanionSuggestions(focusNote: String) {
        val current = _activeGarden.value ?: return
        viewModelScope.launch {
            _companionState.value = CompanionUiState.Loading
            val result = aiRepository.suggestCompanionPlants(current, focusNote)
            if (result.isSuccess) {
                _companionState.value = CompanionUiState.Success(result.getOrThrow())
            } else {
                _companionState.value = CompanionUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to suggest companion plants."
                )
            }
        }
    }

    fun generatePlantVisual(plant: PlacedPlant) {
        val current = _activeGarden.value ?: return
        viewModelScope.launch {
            _plantVisualState.value = VisualGenUiState.Loading(plant.commonName)
            val result = aiRepository.generatePlantVisual(plant, current.style)
            if (result.isFailure) {
                _plantVisualState.value = VisualGenUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to generate plant visual."
                )
                return@launch
            }
            val path = result.getOrThrow()
            val updatedPlants = current.plants.map {
                if (it.id == plant.id) it.copy(localVisualPath = path) else it
            }
            val updatedGarden = current.copy(plants = updatedPlants)
            _activeGarden.value = updatedGarden
            _plantVisualState.value = VisualGenUiState.Success(path)
            persistGarden(updatedGarden)
        }
    }

    fun generateGardenConceptVisual() {
        val current = _activeGarden.value ?: return
        viewModelScope.launch {
            _conceptVisualState.value = VisualGenUiState.Loading(current.title)
            val result = aiRepository.generateGardenConceptVisual(current, _referencePhoto.value)
            if (result.isFailure) {
                _conceptVisualState.value = VisualGenUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to render garden visual."
                )
                return@launch
            }
            val path = result.getOrThrow()
            val updatedGarden = current.copy(conceptVisualPath = path)
            _activeGarden.value = updatedGarden
            _conceptVisualState.value = VisualGenUiState.Success(path)
            persistGarden(updatedGarden)
        }
    }

    fun loadBitmap(path: String): Bitmap? = aiRepository.loadBitmapFromPath(path)

    fun resetPlantVisualState() {
        _plantVisualState.value = VisualGenUiState.Idle
    }

    fun resetConceptVisualState() {
        _conceptVisualState.value = VisualGenUiState.Idle
    }

    fun clearStudioError() {
        _studioError.value = null
    }

    private fun persistGarden(design: GardenDesign) {
        if (design.id.isBlank()) return
        viewModelScope.launch {
            val result = firestoreRepository.updateGarden(design)
            if (result.isFailure) {
                _studioError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save changes."
            }
        }
    }

    companion object {
        private const val TAG = "GardenStudioVM"

        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GardenStudioViewModel(
                    firestoreRepository = GardenRepository(db),
                    aiRepository = GardenAiRepository(app.applicationContext),
                    currentUserId = currentUserId,
                )
            }
        }
    }
}
