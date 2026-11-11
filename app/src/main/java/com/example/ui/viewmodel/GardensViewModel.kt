package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.GardenDesign
import com.example.data.repository.GardenRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GardensUiState {
    data object Loading : GardensUiState
    data class Success(val gardens: List<GardenDesign>) : GardensUiState
    data class Error(val message: String) : GardensUiState
}

class GardensViewModel(
    private val repository: GardenRepository,
    private val currentUserId: String,
) : ViewModel() {

    val gardensState: StateFlow<GardensUiState> = repository.observeGardens(currentUserId)
        .map<List<GardenDesign>, GardensUiState> { GardensUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing gardens", rootError)
            emit(GardensUiState.Error(rootError.message ?: "Failed to load saved gardens."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GardensUiState.Loading,
        )

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun deleteGarden(gardenId: String) {
        viewModelScope.launch {
            val result = repository.deleteGarden(gardenId)
            if (result.isFailure) {
                _actionError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete garden."
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    companion object {
        private const val TAG = "GardensVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GardensViewModel(GardenRepository(db), currentUserId)
            }
        }
    }
}
