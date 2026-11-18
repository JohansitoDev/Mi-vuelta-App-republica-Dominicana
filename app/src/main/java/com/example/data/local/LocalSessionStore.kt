package com.example.data.local

import android.content.Context
import com.example.data.model.FREE_PLAN_ACTIVE_LIMIT
import com.example.data.model.FamilyGroup
import com.example.data.model.UserProfile
import com.example.data.model.Vuelta
import com.example.ui.theme.AppThemeMode
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

sealed interface AppSession {
  data object SignedOut : AppSession
  data class GoogleCloud(
    val uid: String,
    val displayName: String,
    val email: String,
  ) : AppSession
  data class LocalAccount(
    val uid: String,
    val displayName: String,
    val email: String,
    val isGuest: Boolean,
  ) : AppSession
}

class ThemePreferenceStore(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _themeMode = MutableStateFlow(readStoredTheme())
  val themeMode = _themeMode.asStateFlow()

  private fun readStoredTheme(): AppThemeMode {
    val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.BLACK.name)
    return AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.LIGHT
  }

  fun setThemeMode(mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    _themeMode.value = mode
  }

  fun toggleTheme() {
    setThemeMode(if (_themeMode.value == AppThemeMode.BLACK) AppThemeMode.LIGHT else AppThemeMode.BLACK)
  }

  private companion object {
    const val PREFS_NAME = "iaestudio_theme_prefs"
    const val KEY_THEME_MODE = "theme_mode"
  }
}

class LocalSessionStore(context: Context) {
  private val appContext = context.applicationContext
  private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _activeLocalSession = MutableStateFlow(readSavedLocalSession())
  val activeLocalSession = _activeLocalSession.asStateFlow()

  private fun readSavedLocalSession(): AppSession.LocalAccount? {
    val uid = prefs.getString(KEY_ACTIVE_UID, null) ?: return null
    val name = prefs.getString(KEY_ACTIVE_NAME, "Invitado") ?: "Invitado"
    val email = prefs.getString(KEY_ACTIVE_EMAIL, "") ?: ""
    val isGuest = prefs.getBoolean(KEY_ACTIVE_GUEST, true)
    return AppSession.LocalAccount(uid = uid, displayName = name, email = email, isGuest = isGuest)
  }

  fun signInAsGuest(): AppSession.LocalAccount {
    val session = AppSession.LocalAccount(
      uid = GUEST_UID,
      displayName = "Invitado",
      email = "",
      isGuest = true,
    )
    saveActiveSession(session)
    return session
  }

  fun createLocalAccount(name: String, email: String, password: String): Result<AppSession.LocalAccount> {
    val cleanName = name.trim()
    val cleanEmail = email.trim().lowercase()
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Escribe tu nombre (mínimo 2 letras)."))
    }
    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      return Result.failure(IllegalArgumentException("Escribe un correo electrónico válido."))
    }
    if (password.length < 6) {
      return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres."))
    }

    val accounts = readAccountsJson()
    if (accounts.has(cleanEmail)) {
      return Result.failure(IllegalStateException("Ya existe una cuenta con ese correo. Intenta iniciar sesión."))
    }

    val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
    val hash = sha256("$saltHex:$password")
    val uid = "local_${sha256(cleanEmail).take(16)}"

    val record = JSONObject().apply {
      put("uid", uid)
      put("name", cleanName)
      put("email", cleanEmail)
      put("salt", saltHex)
      put("hash", hash)
    }
    accounts.put(cleanEmail, record)
    prefs.edit().putString(KEY_ACCOUNTS_JSON, accounts.toString()).apply()

    val session = AppSession.LocalAccount(
      uid = uid,
      displayName = cleanName,
      email = cleanEmail,
      isGuest = false,
    )
    saveActiveSession(session)
    return Result.success(session)
  }

  fun signInLocalAccount(email: String, password: String): Result<AppSession.LocalAccount> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isEmpty() || password.isEmpty()) {
      return Result.failure(IllegalArgumentException("Ingresa tu correo y contraseña."))
    }
    val accounts = readAccountsJson()
    if (!accounts.has(cleanEmail)) {
      return Result.failure(IllegalStateException("No encontramos una cuenta con ese correo."))
    }
    val record = accounts.getJSONObject(cleanEmail)
    val salt = record.optString("salt", "")
    val expectedHash = record.optString("hash", "")
    if (sha256("$salt:$password") != expectedHash) {
      return Result.failure(IllegalStateException("Contraseña incorrecta."))
    }
    val session = AppSession.LocalAccount(
      uid = record.getString("uid"),
      displayName = record.getString("name"),
      email = cleanEmail,
      isGuest = false,
    )
    saveActiveSession(session)
    return Result.success(session)
  }

  fun signOutLocal() {
    prefs.edit()
      .remove(KEY_ACTIVE_UID)
      .remove(KEY_ACTIVE_NAME)
      .remove(KEY_ACTIVE_EMAIL)
      .remove(KEY_ACTIVE_GUEST)
      .apply()
    _activeLocalSession.value = null
  }

  private fun saveActiveSession(session: AppSession.LocalAccount) {
    prefs.edit()
      .putString(KEY_ACTIVE_UID, session.uid)
      .putString(KEY_ACTIVE_NAME, session.displayName)
      .putString(KEY_ACTIVE_EMAIL, session.email)
      .putBoolean(KEY_ACTIVE_GUEST, session.isGuest)
      .apply()
    _activeLocalSession.value = session
  }

  private fun readAccountsJson(): JSONObject {
    val raw = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return JSONObject()
    return try {
      JSONObject(raw)
    } catch (_: Exception) {
      JSONObject()
    }
  }

  private fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  companion object {
    const val GUEST_UID = "local_guest"
    private const val PREFS_NAME = "iaestudio_local_auth"
    private const val KEY_ACCOUNTS_JSON = "accounts_json"
    private const val KEY_ACTIVE_UID = "active_uid"
    private const val KEY_ACTIVE_NAME = "active_name"
    private const val KEY_ACTIVE_EMAIL = "active_email"
    private const val KEY_ACTIVE_GUEST = "active_guest"
  }
}

interface VueltaDataSource {
  fun observeUserProfile(userId: String): Flow<UserProfile?>
  fun observeFamily(familyId: String): Flow<FamilyGroup?>
  fun observeVueltas(userId: String): Flow<List<Vuelta>>

  suspend fun createVuelta(
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
    familyMemberUids: List<String>,
    receiptBase64: String?,
    isProUser: Boolean,
    currentActiveCount: Int,
  ): Result<String>

  suspend fun updateVuelta(
    vueltaId: String,
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
    familyMemberUids: List<String>,
    receiptBase64: String?,
    isProUser: Boolean,
  ): Result<Unit>

  suspend fun toggleCompleted(vueltaId: String, completed: Boolean): Result<Unit>
  suspend fun deleteVuelta(vueltaId: String): Result<Unit>
  suspend fun setPlanTier(userId: String, tier: String): Result<Unit>
  suspend fun createFamilyGroup(groupName: String): Result<FamilyGroup>
  suspend fun joinFamilyByCode(rawCode: String): Result<String>
  suspend fun leaveFamilyGroup(familyId: String): Result<Unit>
}

class LocalVueltaRepository(
  context: Context,
  private val uid: String,
  private val displayName: String,
  private val email: String,
) : VueltaDataSource {

  private val dataFile = File(context.applicationContext.filesDir, "vueltas_${uid}.json")
  private val profileFile = File(context.applicationContext.filesDir, "profile_${uid}.json")

  private val vueltasFlow = MutableStateFlow(loadVueltasFromDisk())
  private val profileFlow = MutableStateFlow(loadProfileFromDisk())

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow.asStateFlow()

  override fun observeFamily(familyId: String): Flow<FamilyGroup?> = flowOf(null)

  override fun observeVueltas(userId: String): Flow<List<Vuelta>> =
    vueltasFlow.map { list ->
      list.sortedWith(compareBy<Vuelta> { it.completed }.thenBy { it.dueDateMillis })
    }

  override suspend fun createVuelta(
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
    familyMemberUids: List<String>,
    receiptBase64: String?,
    isProUser: Boolean,
    currentActiveCount: Int,
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!isProUser && currentActiveCount >= FREE_PLAN_ACTIVE_LIMIT) {
      return@withContext Result.failure(
        IllegalStateException("Has alcanzado el límite de $FREE_PLAN_ACTIVE_LIMIT vueltas activas del plan Gratis.")
      )
    }
    val cleanTitle = title.trim().take(100)
    if (cleanTitle.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("Escribe qué vuelta tienes que hacer."))
    }
    val newId = "loc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val newVuelta = Vuelta(
      id = newId,
      ownerId = uid,
      ownerName = displayName,
      memberUids = listOf(uid),
      title = cleanTitle,
      templateKey = templateKey.trim().take(40),
      notes = notes.trim().take(300),
      dueDateMillis = dueDateMillis,
      remind2Days = remind2Days,
      remind1Day = remind1Day,
      remindSameDay = remindSameDay,
      reminderHour = reminderHour.coerceIn(0, 23),
      reminderMinute = reminderMinute.coerceIn(0, 59),
      completed = false,
      sharedWithFamily = false,
      receiptBase64 = receiptBase64?.take(250_000),
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now(),
    )
    val updated = vueltasFlow.value + newVuelta
    saveVueltasToDisk(updated)
    vueltasFlow.value = updated
    Result.success(newId)
  }

  override suspend fun updateVuelta(
    vueltaId: String,
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
    familyMemberUids: List<String>,
    receiptBase64: String?,
    isProUser: Boolean,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanTitle = title.trim().take(100)
    if (cleanTitle.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("Escribe qué vuelta tienes que hacer."))
    }
    val updated = vueltasFlow.value.map { existing ->
      if (existing.id == vueltaId) {
        existing.copy(
          title = cleanTitle,
          templateKey = templateKey.trim().take(40),
          notes = notes.trim().take(300),
          dueDateMillis = dueDateMillis,
          remind2Days = remind2Days,
          remind1Day = remind1Day,
          remindSameDay = remindSameDay,
          reminderHour = reminderHour.coerceIn(0, 23),
          reminderMinute = reminderMinute.coerceIn(0, 59),
          receiptBase64 = receiptBase64?.takeIf { it.isNotBlank() }?.take(250_000),
          updatedAt = Timestamp.now(),
        )
      } else existing
    }
    saveVueltasToDisk(updated)
    vueltasFlow.value = updated
    Result.success(Unit)
  }

  override suspend fun toggleCompleted(vueltaId: String, completed: Boolean): Result<Unit> =
    withContext(Dispatchers.IO) {
      val updated = vueltasFlow.value.map { existing ->
        if (existing.id == vueltaId) {
          existing.copy(
            completed = completed,
            completedAtMillis = if (completed) System.currentTimeMillis() else null,
            updatedAt = Timestamp.now(),
          )
        } else existing
      }
      saveVueltasToDisk(updated)
      vueltasFlow.value = updated
      Result.success(Unit)
    }

  override suspend fun deleteVuelta(vueltaId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val updated = vueltasFlow.value.filterNot { it.id == vueltaId }
    saveVueltasToDisk(updated)
    vueltasFlow.value = updated
    Result.success(Unit)
  }

  override suspend fun setPlanTier(userId: String, tier: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val updated = profileFlow.value.copy(planTier = tier, updatedAt = Timestamp.now())
      saveProfileToDisk(updated)
      profileFlow.value = updated
      Result.success(Unit)
    }

  override suspend fun createFamilyGroup(groupName: String): Result<FamilyGroup> =
    Result.failure(IllegalStateException("Conecta tu cuenta de Google en Perfil para crear grupos familiares en la nube."))

  override suspend fun joinFamilyByCode(rawCode: String): Result<String> =
    Result.failure(IllegalStateException("Conecta tu cuenta de Google en Perfil para unirte a un grupo familiar."))

  override suspend fun leaveFamilyGroup(familyId: String): Result<Unit> = Result.success(Unit)

  private fun loadProfileFromDisk(): UserProfile {
    if (!profileFile.exists()) {
      return UserProfile(
        id = uid,
        userId = uid,
        displayName = displayName,
        email = email,
        planTier = "free",
      )
    }
    return try {
      val json = JSONObject(profileFile.readText())
      UserProfile(
        id = uid,
        userId = uid,
        displayName = json.optString("displayName", displayName),
        email = json.optString("email", email),
        planTier = json.optString("planTier", "free"),
      )
    } catch (_: Exception) {
      UserProfile(id = uid, userId = uid, displayName = displayName, email = email, planTier = "free")
    }
  }

  private fun saveProfileToDisk(profile: UserProfile) {
    val json = JSONObject().apply {
      put("displayName", profile.displayName)
      put("email", profile.email)
      put("planTier", profile.planTier)
    }
    profileFile.writeText(json.toString())
  }

  private fun loadVueltasFromDisk(): List<Vuelta> {
    if (!dataFile.exists()) return emptyList()
    return try {
      val arr = JSONArray(dataFile.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            Vuelta(
              id = obj.getString("id"),
              ownerId = obj.optString("ownerId", uid),
              ownerName = obj.optString("ownerName", displayName),
              memberUids = listOf(uid),
              title = obj.getString("title"),
              templateKey = obj.optString("templateKey", "otra"),
              notes = obj.optString("notes", ""),
              dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
              remind2Days = obj.optBoolean("remind2Days", true),
              remind1Day = obj.optBoolean("remind1Day", true),
              remindSameDay = obj.optBoolean("remindSameDay", true),
              reminderHour = obj.optInt("reminderHour", 9),
              reminderMinute = obj.optInt("reminderMinute", 0),
              completed = obj.optBoolean("completed", false),
              sharedWithFamily = false,
              receiptBase64 = obj.optString("receiptBase64", "").takeIf { it.isNotBlank() },
              completedAtMillis = obj.optLong("completedAtMillis", 0L).takeIf { it > 0L },
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveVueltasToDisk(list: List<Vuelta>) {
    val arr = JSONArray()
    list.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("ownerId", v.ownerId)
          put("ownerName", v.ownerName)
          put("title", v.title)
          put("templateKey", v.templateKey)
          put("notes", v.notes)
          put("dueDateMillis", v.dueDateMillis)
          put("remind2Days", v.remind2Days)
          put("remind1Day", v.remind1Day)
          put("remindSameDay", v.remindSameDay)
          put("reminderHour", v.reminderHour)
          put("reminderMinute", v.reminderMinute)
          put("completed", v.completed)
          if (!v.receiptBase64.isNullOrBlank()) put("receiptBase64", v.receiptBase64)
          if (v.completedAtMillis != null && v.completedAtMillis > 0L) {
            put("completedAtMillis", v.completedAtMillis)
          }
        }
      )
    }
    dataFile.writeText(arr.toString())
  }
}
