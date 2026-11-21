package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.local.CategoryEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FundEntryEntity
import com.example.data.local.HouseholdEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.MonthArchiveEntity
import com.example.data.local.ShoppingItemEntity
import com.example.data.local.SinkingFundEntity
import com.example.data.local.TransactionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
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

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUserIdOrNull(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "id_${System.currentTimeMillis()}" }

    fun observeHouseholdsForUser(uid: String): Flow<List<HouseholdDoc>> = flow {
        require(uid.isNotBlank())
        val path = "households"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { it.toObjects(HouseholdDoc::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMembers(householdId: String, uid: String): Flow<List<FamilyMemberDoc>> =
        observeSubcollection(householdId, "members", uid, FamilyMemberDoc::class.java)

    fun observeCategories(householdId: String, uid: String): Flow<List<CategoryDoc>> =
        observeSubcollection(householdId, "categories", uid, CategoryDoc::class.java)

    fun observeTransactions(householdId: String, uid: String): Flow<List<TransactionDoc>> =
        observeSubcollection(householdId, "transactions", uid, TransactionDoc::class.java)

    fun observeFunds(householdId: String, uid: String): Flow<List<SinkingFundDoc>> =
        observeSubcollection(householdId, "funds", uid, SinkingFundDoc::class.java)

    fun observeFundEntries(householdId: String, uid: String): Flow<List<FundEntryDoc>> =
        observeSubcollection(householdId, "fundEntries", uid, FundEntryDoc::class.java)

    fun observeShoppingItems(householdId: String, uid: String): Flow<List<ShoppingItemDoc>> =
        observeSubcollection(householdId, "shoppingItems", uid, ShoppingItemDoc::class.java)

    fun observeInventoryItems(householdId: String, uid: String): Flow<List<InventoryItemDoc>> =
        observeSubcollection(householdId, "inventoryItems", uid, InventoryItemDoc::class.java)

    fun observeMonthArchives(householdId: String, uid: String): Flow<List<MonthArchiveDoc>> =
        observeSubcollection(householdId, "monthArchives", uid, MonthArchiveDoc::class.java)

    private fun <T> observeSubcollection(
        householdId: String,
        subcollection: String,
        uid: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val safeHhId = sanitizeId(householdId)
        val path = "households/$safeHhId/$subcollection"
        emitAll(
            db.collection("households").document(safeHhId).collection(subcollection)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { it.toObjects(clazz, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String?,
        householdId: String,
        role: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeHhId = sanitizeId(householdId)
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = FirestorePayloadMapper.nonNullMap(
                    "displayName" to displayName.trim().ifBlank { "مستخدم" }.take(80),
                    "email" to email?.take(120)?.takeIf { it.isNotBlank() },
                    "householdId" to safeHhId,
                    "role" to role,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = FirestorePayloadMapper.nonNullMap(
                    "userId" to uid,
                    "displayName" to displayName.trim().ifBlank { "مستخدم" }.take(80),
                    "email" to email?.take(120)?.takeIf { it.isNotBlank() },
                    "householdId" to safeHhId,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertHousehold(entity: HouseholdEntity): Result<Unit> {
        var path = "households"
        return try {
            val uid = requireUserId()
            val hhId = sanitizeId(entity.id)
            val docRef = db.collection("households").document(hhId)
            path = docRef.path
            val memberUids = entity.memberUidsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                .ifEmpty { listOf(uid) }
                .let { if (uid in it) it else it + uid }
                .take(20)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = FirestorePayloadMapper.nonNullMap(
                    "name" to entity.name.trim().ifBlank { "أسرتي" }.take(80),
                    "currencyCode" to entity.currencyCode.take(10),
                    "inviteCode" to sanitizeId(entity.inviteCode).take(24),
                    "memberUids" to memberUids,
                    "monthlyBudgetPiasters" to entity.monthlyBudgetPiasters.coerceAtLeast(0L),
                    "preventBudgetOverflow" to entity.preventBudgetOverflow,
                    "rolloverEnabled" to entity.rolloverEnabled,
                    "carriedOverPiasters" to entity.carriedOverPiasters.coerceAtLeast(0L),
                    "alertThreshold1" to entity.alertThreshold1.coerceIn(1, 100),
                    "alertThreshold2" to entity.alertThreshold2.coerceIn(1, 100),
                    "activeYearMonth" to entity.activeYearMonth.take(7),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val inviteCode = sanitizeId(entity.inviteCode).take(24)
                val payload = FirestorePayloadMapper.nonNullMap(
                    "ownerId" to uid,
                    "name" to entity.name.trim().ifBlank { "أسرتي" }.take(80),
                    "currencyCode" to entity.currencyCode.take(10),
                    "inviteCode" to inviteCode,
                    "memberUids" to memberUids,
                    "monthlyBudgetPiasters" to entity.monthlyBudgetPiasters.coerceAtLeast(0L),
                    "preventBudgetOverflow" to entity.preventBudgetOverflow,
                    "rolloverEnabled" to entity.rolloverEnabled,
                    "carriedOverPiasters" to entity.carriedOverPiasters.coerceAtLeast(0L),
                    "alertThreshold1" to entity.alertThreshold1.coerceIn(1, 100),
                    "alertThreshold2" to entity.alertThreshold2.coerceIn(1, 100),
                    "activeYearMonth" to entity.activeYearMonth.take(7),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val inviteRef = db.collection("householdInvites").document(inviteCode)
                inviteRef.set(
                    FirestorePayloadMapper.nonNullMap(
                        "householdId" to hhId,
                        "householdName" to entity.name.trim().ifBlank { "أسرتي" }.take(80),
                        "ownerId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun joinHouseholdByInviteCode(code: String, memberName: String, email: String?): Result<String> {
        var path = "householdInvites"
        return try {
            val uid = requireUserId()
            val safeCode = sanitizeId(code.trim().uppercase())
            val inviteRef = db.collection("householdInvites").document(safeCode)
            path = inviteRef.path
            val inviteSnap = inviteRef.get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("كود الدعوة غير صحيح أو غير موجود"))
            }
            val householdId = inviteSnap.getString("householdId")
                ?: return Result.failure(IllegalStateException("بيانات الدعوة غير مكتملة"))
            val hhRef = db.collection("households").document(householdId)
            path = hhRef.path
            hhRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val hhSnap = hhRef.get().await()
            val memberUids = (hhSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: listOf(uid)
            val memberRef = hhRef.collection("members").document(uid)
            memberRef.set(
                FirestorePayloadMapper.nonNullMap(
                    "householdId" to householdId,
                    "userId" to uid,
                    "name" to memberName.trim().ifBlank { "عضو الأسرة" }.take(60),
                    "email" to email?.take(120)?.takeIf { it.isNotBlank() },
                    "role" to "MEMBER",
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            upsertUserProfile(memberName, email, householdId, "MEMBER")
            Result.success(householdId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertMember(entity: FamilyMemberEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "members", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "userId" to entity.userId.takeIf { it.isNotBlank() }?.take(128),
                "name" to entity.name.trim().ifBlank { "فرد الأسرة" }.take(60),
                "email" to entity.email?.takeIf { it.isNotBlank() }?.take(120),
                "role" to entity.role,
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertCategory(entity: CategoryEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "categories", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "name" to entity.name.trim().ifBlank { "تصنيف" }.take(50),
                "iconKey" to entity.iconKey.take(40),
                "colorHex" to entity.colorHex,
                "budgetPiasters" to entity.budgetPiasters.coerceAtLeast(0L),
                "sortOrder" to entity.sortOrder.coerceAtLeast(0),
                "customCategory" to entity.customCategory,
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertTransaction(entity: TransactionEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "transactions", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "type" to entity.type,
                "amountPiasters" to entity.amountPiasters.coerceAtLeast(1L),
                "categoryId" to sanitizeId(entity.categoryId),
                "categoryName" to entity.categoryName.take(60),
                "incomeSource" to entity.incomeSource?.takeIf { it.isNotBlank() }?.take(40),
                "merchant" to entity.merchant?.takeIf { it.isNotBlank() }?.take(80),
                "notes" to entity.notes?.takeIf { it.isNotBlank() }?.take(300),
                "dateEpochMillis" to entity.dateEpochMillis.coerceAtLeast(1L),
                "yearMonth" to entity.yearMonth.take(7),
                "receiptUri" to entity.receiptUri?.takeIf { it.isNotBlank() }?.take(500),
                "addedByUid" to uid,
                "addedByName" to entity.addedByName.take(60).ifBlank { "مستخدم" },
                "updatedByName" to entity.updatedByName.take(60).ifBlank { "مستخدم" },
                "sourceShoppingItemId" to entity.sourceShoppingItemId?.takeIf { it.isNotBlank() }?.take(128),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertFund(entity: SinkingFundEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "funds", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "name" to entity.name.trim().ifBlank { "صندوق ادخار" }.take(60),
                "iconKey" to entity.iconKey.take(40),
                "targetPiasters" to entity.targetPiasters.coerceAtLeast(1L),
                "savedPiasters" to entity.savedPiasters.coerceAtLeast(0L),
                "targetDateMillis" to entity.targetDateMillis.coerceAtLeast(1L),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertFundEntry(entity: FundEntryEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "fundEntries", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "fundId" to sanitizeId(entity.fundId),
                "entryType" to entity.entryType,
                "amountPiasters" to entity.amountPiasters.coerceAtLeast(1L),
                "note" to entity.note?.takeIf { it.isNotBlank() }?.take(200),
                "addedByName" to entity.addedByName.take(60).ifBlank { "مستخدم" },
                "dateEpochMillis" to entity.dateEpochMillis.coerceAtLeast(1L),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertShoppingItem(entity: ShoppingItemEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "shoppingItems", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "name" to entity.name.trim().ifBlank { "منتج" }.take(80),
                "frequency" to entity.frequency,
                "quantityLabel" to entity.quantityLabel.trim().ifBlank { "1" }.take(40),
                "expectedPricePiasters" to entity.expectedPricePiasters.coerceAtLeast(0L),
                "purchased" to entity.purchased,
                "convertedExpenseId" to entity.convertedExpenseId?.takeIf { it.isNotBlank() }?.take(128),
                "categoryId" to sanitizeId(entity.categoryId),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertInventoryItem(entity: InventoryItemEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "inventoryItems", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "name" to entity.name.trim().ifBlank { "صنف" }.take(80),
                "quantity" to entity.quantity.coerceAtLeast(0.0),
                "minThreshold" to entity.minThreshold.coerceAtLeast(0.0),
                "unit" to entity.unit.trim().ifBlank { "عبوة" }.take(20),
                "expiryEpochMillis" to entity.expiryEpochMillis.coerceAtLeast(0L),
                "lastActionNote" to entity.lastActionNote?.takeIf { it.isNotBlank() }?.take(120),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun upsertMonthArchive(entity: MonthArchiveEntity, memberUids: List<String>): Result<Unit> =
        safeSubWrite(entity.householdId, "monthArchives", entity.id) { uid, hhId ->
            val uids = ensureUid(memberUids, uid)
            FirestorePayloadMapper.nonNullMap(
                "householdId" to hhId,
                "yearMonth" to entity.yearMonth.take(7),
                "totalIncomePiasters" to entity.totalIncomePiasters.coerceAtLeast(0L),
                "totalExpensePiasters" to entity.totalExpensePiasters.coerceAtLeast(0L),
                "budgetPiasters" to entity.budgetPiasters.coerceAtLeast(0L),
                "rolloverPiasters" to entity.rolloverPiasters.coerceAtLeast(0L),
                "closedAtMillis" to entity.closedAtMillis.coerceAtLeast(1L),
                "memberUids" to uids,
                "updatedAt" to FieldValue.serverTimestamp()
            ) to mapOf("createdAt" to FieldValue.serverTimestamp())
        }

    suspend fun deleteSubDoc(householdId: String, subcollection: String, docId: String): Result<Unit> {
        val safeHhId = sanitizeId(householdId)
        val safeDocId = sanitizeId(docId)
        val path = "households/$safeHhId/$subcollection/$safeDocId"
        return try {
            requireUserId()
            db.collection("households").document(safeHhId).collection(subcollection).document(safeDocId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun ensureUid(memberUids: List<String>, uid: String): List<String> {
        val cleaned = memberUids.filter { it.isNotBlank() }
        return (if (uid in cleaned) cleaned else cleaned + uid).take(20)
    }

    private suspend fun safeSubWrite(
        householdId: String,
        subcollection: String,
        docId: String,
        payloadBuilder: (uid: String, safeHhId: String) -> Pair<Map<String, Any>, Map<String, Any>>
    ): Result<Unit> {
        val safeHhId = sanitizeId(householdId)
        val safeDocId = sanitizeId(docId)
        val path = "households/$safeHhId/$subcollection/$safeDocId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("households").document(safeHhId).collection(subcollection).document(safeDocId)
            val (mutableFields, createOnlyFields) = payloadBuilder(uid, safeHhId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updateFields = mutableFields.toMutableMap().apply {
                    remove("householdId")
                    remove("addedByUid")
                    remove("addedByName")
                }
                docRef.update(updateFields).await()
            } else {
                docRef.set(mutableFields + createOnlyFields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
