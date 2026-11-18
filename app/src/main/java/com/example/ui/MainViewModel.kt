package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FreeLimitReachedException
import com.example.data.firebase.IaestudioRepository
import com.example.data.local.VueltaDataSource
import com.example.data.model.FamilyGroup
import com.example.data.model.UserProfile
import com.example.data.model.Vuelta
import com.example.notifications.VueltaReminderScheduler
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
  private val repository: VueltaDataSource,
  private val reminderScheduler: VueltaReminderScheduler,
  private val currentUserId: String,
  private val firebaseUser: FirebaseUser?,
) : ViewModel() {

  private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<UiEvent> = _events.asSharedFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      Log.w(TAG, "Error observing profile", firestoreError ?: error)
      emit(null)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val familyGroup: StateFlow<FamilyGroup?> = userProfile
    .flatMapLatest { profile ->
      val famId = profile?.familyId.orEmpty()
      if (famId.isBlank()) flowOf(null) else repository.observeFamily(famId)
    }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      Log.w(TAG, "Error observing family", firestoreError ?: error)
      emit(null)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val vueltasState: StateFlow<UiState<List<Vuelta>>> = repository.observeVueltas(currentUserId)
    .onEach { list ->
      list.forEach { vuelta ->
        if (!vuelta.completed) {
          reminderScheduler.scheduleForVuelta(vuelta)
        } else {
          reminderScheduler.cancelForVuelta(vuelta.id)
        }
      }
    }
    .map<List<Vuelta>, UiState<List<Vuelta>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing vueltas", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar tus vueltas"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  init {
    if (repository is IaestudioRepository && firebaseUser != null) {
      viewModelScope.launch {
        repository.ensureUserProfile(firebaseUser)
      }
    }
  }

  fun findVueltaById(vueltaId: String): Vuelta? {
    val state = vueltasState.value
    return if (state is UiState.Success) {
      state.data.firstOrNull { it.id == vueltaId }
    } else null
  }

  fun activeVueltasCount(): Int {
    val state = vueltasState.value
    return if (state is UiState.Success) {
      state.data.count { !it.completed && it.ownerId == currentUserId }
    } else 0
  }

  fun saveVuelta(
    existingId: String?,
    title: String,
    templateKey: String,
    notes: String,
    dueDateMillis: Long,
    remind2Days: Boolean,
    remind1Day: Boolean,
    remindSameDay: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    sharedWithFamily: Boolean,
    receiptBase64: String?,
  ) {
    if (_isSaving.value) return
    val profile = userProfile.value
    val isPro = profile?.isProUser == true
    val familyMembers = familyGroup.value?.memberUids.orEmpty()

    viewModelScope.launch {
      _isSaving.value = true
      try {
        val result: Result<Unit> = if (existingId.isNullOrBlank()) {
          repository.createVuelta(
            title = title,
            templateKey = templateKey,
            notes = notes,
            dueDateMillis = dueDateMillis,
            remind2Days = remind2Days,
            remind1Day = remind1Day,
            remindSameDay = remindSameDay,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            sharedWithFamily = sharedWithFamily,
            familyMemberUids = familyMembers,
            receiptBase64 = receiptBase64,
            isProUser = isPro,
            currentActiveCount = activeVueltasCount(),
          ).map { newId ->
            reminderScheduler.scheduleForVuelta(
              Vuelta(
                id = newId,
                ownerId = currentUserId,
                title = title,
                dueDateMillis = dueDateMillis,
                remind2Days = remind2Days,
                remind1Day = remind1Day,
                remindSameDay = remindSameDay,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                completed = false,
              )
            )
          }
        } else {
          repository.updateVuelta(
            vueltaId = existingId,
            title = title,
            templateKey = templateKey,
            notes = notes,
            dueDateMillis = dueDateMillis,
            remind2Days = remind2Days,
            remind1Day = remind1Day,
            remindSameDay = remindSameDay,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            sharedWithFamily = sharedWithFamily,
            familyMemberUids = familyMembers,
            receiptBase64 = receiptBase64,
            isProUser = isPro,
          )
        }

        result.onSuccess {
          _events.tryEmit(UiEvent.ShowMessage(if (existingId.isNullOrBlank()) "¡Vuelta guardada!" else "Vuelta actualizada"))
          _events.tryEmit(UiEvent.VueltaSaved)
        }.onFailure { err ->
          if (err is FreeLimitReachedException) {
            _events.tryEmit(UiEvent.ShowFreeLimitDialog)
          } else {
            _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "No se pudo guardar la vuelta"))
          }
        }
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun toggleCompleted(vuelta: Vuelta) {
    val newCompleted = !vuelta.completed
    viewModelScope.launch {
      repository.toggleCompleted(vuelta.id, newCompleted)
        .onSuccess {
          if (newCompleted) {
            reminderScheduler.cancelForVuelta(vuelta.id)
            _events.tryEmit(UiEvent.ShowMessage("¡Vuelta resuelta! Cero moras"))
          } else {
            reminderScheduler.scheduleForVuelta(vuelta.copy(completed = false))
            _events.tryEmit(UiEvent.ShowMessage("Vuelta marcada como pendiente"))
          }
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "Error al actualizar"))
        }
    }
  }

  fun deleteVuelta(vueltaId: String) {
    viewModelScope.launch {
      repository.deleteVuelta(vueltaId)
        .onSuccess {
          reminderScheduler.cancelForVuelta(vueltaId)
          _events.tryEmit(UiEvent.ShowMessage("Vuelta eliminada"))
          _events.tryEmit(UiEvent.VueltaDeleted)
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "No se pudo eliminar"))
        }
    }
  }

  fun activateProTrial() {
    viewModelScope.launch {
      repository.setPlanTier(currentUserId, "pro")
        .onSuccess {
          _events.tryEmit(UiEvent.ShowMessage("¡IAEstudio Pro activado! Vueltas ilimitadas y familia habilitada"))
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "No se pudo activar Pro"))
        }
    }
  }

  fun switchToFreePlan() {
    viewModelScope.launch {
      repository.setPlanTier(currentUserId, "free")
        .onSuccess {
          _events.tryEmit(UiEvent.ShowMessage("Cambiado al plan Gratis (hasta 5 activas)"))
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "Error al cambiar plan"))
        }
    }
  }

  fun createFamilyGroup(name: String) {
    viewModelScope.launch {
      repository.createFamilyGroup(name)
        .onSuccess { group ->
          _events.tryEmit(UiEvent.ShowMessage("Grupo \"${group.name}\" creado · Código ${group.inviteCode}"))
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "No se pudo crear el grupo"))
        }
    }
  }

  fun joinFamilyByCode(code: String) {
    viewModelScope.launch {
      repository.joinFamilyByCode(code)
        .onSuccess {
          _events.tryEmit(UiEvent.ShowMessage("¡Te uniste al grupo familiar!"))
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "Código inválido o no encontrado"))
        }
    }
  }

  fun leaveFamilyGroup(familyId: String) {
    viewModelScope.launch {
      repository.leaveFamilyGroup(familyId)
        .onSuccess {
          _events.tryEmit(UiEvent.ShowMessage("Saliste del grupo familiar"))
        }
        .onFailure { err ->
          _events.tryEmit(UiEvent.ShowMessage(err.localizedMessage ?: "No se pudo salir del grupo"))
        }
    }
  }

  private companion object {
    const val TAG = "MainViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
