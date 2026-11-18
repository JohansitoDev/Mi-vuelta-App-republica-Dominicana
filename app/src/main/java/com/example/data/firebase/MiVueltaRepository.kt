package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.VueltaDataSource
import com.example.data.model.FREE_PLAN_ACTIVE_LIMIT
import com.example.data.model.FamilyGroup
import com.example.data.model.UserProfile
import com.example.data.model.Vuelta
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FreeLimitReachedException :
  IllegalStateException("Has alcanzado el límite de $FREE_PLAN_ACTIVE_LIMIT vueltas activas del plan Gratis.")

class MiVueltaRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : VueltaDataSource {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Debes iniciar sesión con Google para acceder a tus vueltas.")
  }

  suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val docPath = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Usuario Mi Vuelta").take(80)
      val safeEmail = (user.email ?: "").take(120)
      if (!snap.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "email" to safeEmail,
          "planTier" to "free",
          "familyId" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        docRef.update(
          mapOf(
            "displayName" to safeName,
            "email" to safeEmail,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    if (userId.isBlank()) {
      emit(null)
      return@flow
    }
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  override fun observeFamily(familyId: String): Flow<FamilyGroup?> = flow {
    if (familyId.isBlank()) {
      emit(null)
      return@flow
    }
    val path = "families/$familyId"
    emitAll(
      db.collection("families").document(familyId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(FamilyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          emit(null)
        }
    )
  }

  override fun observeVueltas(userId: String): Flow<List<Vuelta>> = flow {
    if (userId.isBlank()) {
      emit(emptyList())
      return@flow
    }
    val path = "vueltas"
    emitAll(
      db.collection(path)
        .whereArrayContains("memberUids", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Vuelta::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<Vuelta> { it.completed }.thenBy { it.dueDateMillis })
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
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
  ): Result<String> {
    var docPath = "vueltas"
    return try {
      val uid = requireUserId()
      if (!isProUser && currentActiveCount >= FREE_PLAN_ACTIVE_LIMIT) {
        return Result.failure(FreeLimitReachedException())
      }
      val cleanTitle = title.trim().take(100)
      require(cleanTitle.isNotEmpty()) { "Escribe qué vuelta tienes que hacer." }
      require(dueDateMillis > 0L) { "Selecciona una fecha válida." }
      val cleanNotes = notes.trim().take(300)
      val cleanTemplate = templateKey.trim().take(40)
      val ownerName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Yo").take(80)

      val members = if (sharedWithFamily && isProUser && familyMemberUids.isNotEmpty()) {
        (listOf(uid) + familyMemberUids).distinct().take(10)
      } else {
        listOf(uid)
      }

      val docRef = db.collection("vueltas").document()
      docPath = docRef.path

      val payload = buildMap<String, Any> {
        put("ownerId", uid)
        put("ownerName", ownerName)
        put("memberUids", members)
        put("title", cleanTitle)
        put("templateKey", cleanTemplate)
        put("notes", cleanNotes)
        put("dueDateMillis", dueDateMillis)
        put("remind2Days", remind2Days)
        put("remind1Day", remind1Day)
        put("remindSameDay", remindSameDay)
        put("reminderHour", reminderHour.coerceIn(0, 23))
        put("reminderMinute", reminderMinute.coerceIn(0, 59))
        put("completed", false)
        put("sharedWithFamily", sharedWithFamily && isProUser && members.size > 1)
        if (!receiptBase64.isNullOrBlank()) {
          require(receiptBase64.length <= 250_000) { "La foto del recibo es demasiado grande." }
          put("receiptBase64", receiptBase64)
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }

      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
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
  ): Result<Unit> {
    var docPath = "vueltas/$vueltaId"
    return try {
      val uid = requireUserId()
      require(vueltaId.isNotBlank()) { "ID de vuelta inválido." }
      val cleanTitle = title.trim().take(100)
      require(cleanTitle.isNotEmpty()) { "Escribe qué vuelta tienes que hacer." }
      require(dueDateMillis > 0L) { "Selecciona una fecha válida." }

      val members = if (sharedWithFamily && isProUser && familyMemberUids.isNotEmpty()) {
        (listOf(uid) + familyMemberUids).distinct().take(10)
      } else {
        listOf(uid)
      }

      val docRef = db.collection("vueltas").document(vueltaId)
      docPath = docRef.path

      val updates = buildMap<String, Any> {
        put("memberUids", members)
        put("title", cleanTitle)
        put("templateKey", templateKey.trim().take(40))
        put("notes", notes.trim().take(300))
        put("dueDateMillis", dueDateMillis)
        put("remind2Days", remind2Days)
        put("remind1Day", remind1Day)
        put("remindSameDay", remindSameDay)
        put("reminderHour", reminderHour.coerceIn(0, 23))
        put("reminderMinute", reminderMinute.coerceIn(0, 59))
        put("sharedWithFamily", sharedWithFamily && isProUser && members.size > 1)
        put("receiptBase64", (receiptBase64 ?: "").take(250_000))
        put("updatedAt", FieldValue.serverTimestamp())
      }

      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  override suspend fun toggleCompleted(vueltaId: String, completed: Boolean): Result<Unit> {
    val docPath = "vueltas/$vueltaId"
    return try {
      requireUserId()
      require(vueltaId.isNotBlank()) { "ID de vuelta inválido." }
      val updates = mapOf(
        "completed" to completed,
        "completedAtMillis" to if (completed) System.currentTimeMillis() else 0L,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("vueltas").document(vueltaId).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  override suspend fun deleteVuelta(vueltaId: String): Result<Unit> {
    val docPath = "vueltas/$vueltaId"
    return try {
      requireUserId()
      require(vueltaId.isNotBlank()) { "ID de vuelta inválido." }
      db.collection("vueltas").document(vueltaId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  override suspend fun setPlanTier(userId: String, tier: String): Result<Unit> {
    val docPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "No autorizado para modificar otro usuario." }
      require(tier == "free" || tier == "pro") { "Plan inválido." }
      db.collection("users").document(uid).update(
        mapOf(
          "planTier" to tier,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  override suspend fun createFamilyGroup(groupName: String): Result<FamilyGroup> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val cleanName = groupName.trim().ifEmpty { "Mi Familia" }.take(60)
    val familyId = "fam_${uid.take(8)}_${System.currentTimeMillis()}"
    val inviteCode = generateInviteCode()
    return try {
      val familyPayload = mapOf(
        "ownerId" to uid,
        "name" to cleanName,
        "inviteCode" to inviteCode,
        "memberUids" to listOf(uid),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("families").document(familyId).set(familyPayload).await()

      val invitePayload = mapOf(
        "familyId" to familyId,
        "createdBy" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
      )
      db.collection("family_invites").document(inviteCode).set(invitePayload).await()

      db.collection("users").document(uid).update(
        mapOf(
          "familyId" to familyId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      Result.success(
        FamilyGroup(
          id = familyId,
          ownerId = uid,
          name = cleanName,
          inviteCode = inviteCode,
          memberUids = listOf(uid),
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "families/$familyId")
      Result.failure(e)
    }
  }

  override suspend fun joinFamilyByCode(rawCode: String): Result<String> {
    val code = rawCode.trim().uppercase()
    if (!code.matches(Regex("^[A-Z0-9]{6,12}$"))) {
      return Result.failure(IllegalArgumentException("El código debe tener 6 a 12 letras o números."))
    }
    return try {
      val uid = requireUserId()
      val inviteSnap = db.collection("family_invites").document(code).get().await()
      if (!inviteSnap.exists()) {
        return Result.failure(IllegalStateException("No encontramos ningún grupo con ese código."))
      }
      val familyId = inviteSnap.getString("familyId")
        ?: return Result.failure(IllegalStateException("Código de invitación inválido."))

      db.collection("families").document(familyId).update(
        mapOf(
          "memberUids" to FieldValue.arrayUnion(uid),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      db.collection("users").document(uid).update(
        mapOf(
          "familyId" to familyId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      Result.success(familyId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "family_invites/$code")
      Result.failure(e)
    }
  }

  override suspend fun leaveFamilyGroup(familyId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      require(familyId.isNotBlank())
      db.collection("families").document(familyId).update(
        mapOf(
          "memberUids" to FieldValue.arrayRemove(uid),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      db.collection("users").document(uid).update(
        mapOf(
          "familyId" to "",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "families/$familyId")
      Result.failure(e)
    }
  }

  private fun generateInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return "MV" + (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
  }
}
