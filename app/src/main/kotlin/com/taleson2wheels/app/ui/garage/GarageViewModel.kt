package com.taleson2wheels.app.ui.garage

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.data.remote.dto.MotorcycleInput
import com.taleson2wheels.app.data.repository.GarageRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

/** The add/edit form shown in the bottom sheet. `id == null` means "add new". */
data class GarageEditorState(
    val id: String? = null,
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val cc: String = "",
    val color: String = "",
    val nickname: String = "",
    val imageUrl: String? = null,
    val isUploading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = id != null
    val canSave: Boolean
        get() = make.isNotBlank() && model.isNotBlank() && !isSaving && !isUploading
}

@Immutable
data class GarageUiState(
    val isLoading: Boolean = true,
    val bikes: List<MotorcycleDto> = emptyList(),
    val error: String? = null,
    val editor: GarageEditorState? = null,
    val deletingId: String? = null,
)

/** Lists, adds, edits, and removes the rider's motorcycles, with image upload. */
class GarageViewModel(
    private val garageRepository: GarageRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(GarageUiState())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = garageRepository.list()) {
                is ApiResult.Success -> uiState = uiState.copy(isLoading = false, bikes = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    // ── Editor lifecycle ──────────────────────────────────────────────────────
    fun startAdd() { uiState = uiState.copy(editor = GarageEditorState()) }

    fun startEdit(bike: MotorcycleDto) {
        uiState = uiState.copy(
            editor = GarageEditorState(
                id = bike.id,
                make = bike.make,
                model = bike.model,
                year = if (bike.year > 0) bike.year.toString() else "",
                cc = if (bike.cc > 0) bike.cc.toString() else "",
                color = bike.color.orEmpty(),
                nickname = bike.nickname.orEmpty(),
                imageUrl = bike.imageUrl,
            ),
        )
    }

    fun dismissEditor() { uiState = uiState.copy(editor = null) }

    private fun editEditor(block: (GarageEditorState) -> GarageEditorState) {
        uiState.editor?.let { uiState = uiState.copy(editor = block(it).copy(error = null)) }
    }

    fun onMakeChange(v: String) = editEditor { it.copy(make = v) }
    fun onModelChange(v: String) = editEditor { it.copy(model = v) }
    fun onYearChange(v: String) = editEditor { it.copy(year = v.filter(Char::isDigit).take(4)) }
    fun onCcChange(v: String) = editEditor { it.copy(cc = v.filter(Char::isDigit).take(5)) }
    fun onColorChange(v: String) = editEditor { it.copy(color = v) }
    fun onNicknameChange(v: String) = editEditor { it.copy(nickname = v) }

    fun uploadImage(bytes: ByteArray, filename: String, mimeType: String) {
        val editor = uiState.editor ?: return
        viewModelScope.launch {
            uiState = uiState.copy(editor = editor.copy(isUploading = true, error = null))
            when (val r = uploadRepository.uploadImage(bytes, filename, mimeType, type = "motorcycle")) {
                is ApiResult.Success ->
                    uiState = uiState.copy(editor = uiState.editor?.copy(isUploading = false, imageUrl = r.data))
                is ApiResult.Failure ->
                    uiState = uiState.copy(editor = uiState.editor?.copy(isUploading = false, error = r.error.userMessage))
            }
        }
    }

    fun save() {
        val editor = uiState.editor ?: return
        if (!editor.canSave) return
        val input = MotorcycleInput(
            make = editor.make.trim(),
            model = editor.model.trim(),
            year = editor.year.toIntOrNull() ?: 0,
            cc = editor.cc.toIntOrNull() ?: 0,
            color = editor.color.trim().ifBlank { null },
            nickname = editor.nickname.trim().ifBlank { null },
            imageUrl = editor.imageUrl,
        )
        viewModelScope.launch {
            uiState = uiState.copy(editor = editor.copy(isSaving = true, error = null))
            val result = if (editor.id == null) {
                garageRepository.create(input)
            } else {
                garageRepository.update(editor.id, input)
            }
            when (result) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(editor = null, bikes = upsert(uiState.bikes, result.data))
                }
                is ApiResult.Failure ->
                    uiState = uiState.copy(editor = uiState.editor?.copy(isSaving = false, error = result.error.userMessage))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(deletingId = id, error = null)
            when (val r = garageRepository.delete(id)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(deletingId = null, bikes = uiState.bikes.filterNot { it.id == id })
                is ApiResult.Failure ->
                    uiState = uiState.copy(deletingId = null, error = r.error.userMessage)
            }
        }
    }

    private fun upsert(list: List<MotorcycleDto>, bike: MotorcycleDto): List<MotorcycleDto> =
        if (list.any { it.id == bike.id }) {
            list.map { if (it.id == bike.id) bike else it }
        } else {
            list + bike
        }
}
