package com.example.ui

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

sealed interface UiEvent {
  data class ShowMessage(val message: String) : UiEvent
  data object ShowFreeLimitDialog : UiEvent
  data object VueltaSaved : UiEvent
  data object VueltaDeleted : UiEvent
}
