package com.ekenahmetfaruk.vigna_scan.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekenahmetfaruk.vigna_scan.ml.ModelManager
import com.ekenahmetfaruk.vigna_scan.ml.ModelResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val modelManager: ModelManager
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val model1Result: ModelResult,
            val model2Result: ModelResult,
            val bitmap: Bitmap
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun loadModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                modelManager.loadModels()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Model yüklenemedi: ${e.message}")
            }
        }
    }

    fun analyze(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val results = withContext(Dispatchers.IO) {
                    modelManager.runBothModels(bitmap)
                }
                _uiState.value = UiState.Success(
                    model1Result = results.first,
                    model2Result = results.second,
                    bitmap = bitmap
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Analiz başarısız: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        modelManager.release()
    }
}