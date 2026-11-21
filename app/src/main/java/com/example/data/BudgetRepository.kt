package com.example.data

import android.util.Log
import com.example.core.MoneyUtils
import com.example.data.local.AppDao
import com.example.data.local.CategoryEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FundEntryEntity
import com.example.data.local.HouseholdEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.MonthArchiveEntity
import com.example.data.local.ShoppingItemEntity
import com.example.data.local.SinkingFundEntity
import com.example.data.local.TransactionEntity
import com.example.data.remote.CloudSyncRepository
import com.example.domain.AlertSeverity
import com.example.domain.BudgetTransaction
import com.example.domain.Category
import com.example.domain.CategorySpendingSummary
import com.example.domain.DashboardSnapshot
import com.example.domain.FamilyMember
import com.example.domain.FamilyRole
import com.example.domain.FundEntry
import com.example.domain.FundEntryType
import com.example.domain.Household
import com.example.domain.IncomeSource
import com.example.domain.InventoryItem
import com.example.domain.MonthArchive
import com.example.domain.ShoppingFrequency
import com.example.domain.ShoppingItem
import com.example.domain.SinkingFund
import com.example.domain.SmartAlert
import com.example.domain.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class BudgetRepository(
    private val dao: AppDao,
    private val cloudSync: CloudSyncRepository,
    private val currentUserId: String?,
    private val currentUserName: String?,
    private val currentUserEmail: String?
) {
    private fun uidOrLocal(): String = currentUserId ?: "local_user"

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    suspend fun ensureInitialized(defaultCurrency: String = "EGP", householdName: String = "أسرتي") {
        val current = dao.getHouseholdById("hh_${uidOrLocal()}") ?: run {
            // Check if any household already exists locally
            val anyHh = currentHouseholdSync()
            anyHh
        }
        if (current != null) {
            if (currentUserId != null) {
                syncLocalSeedToCloudIfNeeded(current)
            }
            return
        }

        val now = System.currentTimeMillis()
        val hhId = "hh_${uidOrLocal()}"
        val inviteCode = "FAM${(1000..9999).random()}"
        val memberUids = if (currentUserId != null) currentUserId else "local_user"
        val ownerName = currentUserName?.takeIf { it.isNotBlank() } ?: "مدير الأسرة"

        val household = HouseholdEntity(
            id = hhId,
            ownerId = uidOrLocal(),
            name = householdName,
            currencyCode = defaultCurrency,
            inviteCode = inviteCode,
            memberUidsCsv = memberUids,
            monthlyBudgetPiasters = 0L,
            preventBudgetOverflow = false,
            rolloverEnabled = true,
            carriedOverPiasters = 0L,
            alertThreshold1 = 70,
            alertThreshold2 = 90,
            activeYearMonth = MoneyUtils.currentYearMonth(now),
            updatedAtMillis = now
        )
        dao.upsertHousehold(household)

        val adminMember = FamilyMemberEntity(
            id = "mem_${uidOrLocal()}",
            householdId = hhId,
            userId = uidOrLocal(),
            name = ownerName,
            email = currentUserEmail,
            role = FamilyRole.ADMIN.name,
            updatedAtMillis = now
        )
        dao.upsertMember(adminMember)

        val defaultCategories = buildDefaultCategories(hhId, now)
        dao.upsertCategories(defaultCategories)

        val defaultFunds = buildDefaultFunds(hhId, now)
        dao.upsertFunds(defaultFunds)

        if (currentUserId != null) {
            val uids = listOf(currentUserId)
            cloudSync.upsertHousehold(household)
            cloudSync.upsertUserProfile(ownerName, currentUserEmail, hhId, FamilyRole.ADMIN.name)
            cloudSync.upsertMember(adminMember, uids)
            defaultCategories.forEach { cloudSync.upsertCategory(it, uids) }
            defaultFunds.forEach { cloudSync.upsertFund(it, uids) }
        }
    }

    private suspend fun syncLocalSeedToCloudIfNeeded(hh: HouseholdEntity) {
        val uid = currentUserId ?: return
        val uids = hh.memberUidsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .let { if (uid in it) it else it + uid }
        val updatedHh = hh.copy(memberUidsCsv = uids.joinToString(","))
        dao.upsertHousehold(updatedHh)
        cloudSync.upsertHousehold(updatedHh)
        cloudSync.upsertUserProfile(
            currentUserName?.ifBlank { "مستخدم" } ?: "مستخدم",
            currentUserEmail,
            updatedHh.id,
            FamilyRole.ADMIN.name
        )
    }

    private suspend fun currentHouseholdSync(): HouseholdEntity? {
        var found: HouseholdEntity? = null
        // Read via categories/household lookup safely
        dao.getHouseholdById("hh_${uidOrLocal()}")?.let { found = it }
        return found
    }

    fun observeHousehold(): Flow<Household?> =
        dao.observePrimaryHousehold().map { it?.toDomain() }

    @Suppress("OPT_IN_USAGE")
    fun observeMembers(): Flow<List<FamilyMember>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeMembers(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeCategories(): Flow<List<Category>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeCategories(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeTransactions(): Flow<List<BudgetTransaction>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeTransactions(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeFunds(): Flow<List<SinkingFund>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeFunds(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeFundEntries(): Flow<List<FundEntry>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeFundEntries(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeShoppingItems(): Flow<List<ShoppingItem>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeShoppingItems(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeInventoryItems(): Flow<List<InventoryItem>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeInventoryItems(hhId).map { list -> list.map { it.toDomain() } }
        }

    @Suppress("OPT_IN_USAGE")
    fun observeMonthArchives(): Flow<List<MonthArchive>> =
        observeHouseholdId().flatMapLatest { hhId ->
            if (hhId == null) flowOf(emptyList())
            else dao.observeMonthArchives(hhId).map { list -> list.map { it.toDomain() } }
        }

    fun observeCloudSyncFeed(): Flow<String?> {
        val uid = currentUserId ?: return flowOf(null)
        return cloudSync.observeHouseholdsForUser(uid)
            .onEach { docs ->
                val primary = docs.firstOrNull()
                if (primary != null) {
                    val entity = HouseholdEntity(
                        id = primary.id,
                        ownerId = primary.ownerId,
                        name = primary.name,
                        currencyCode = primary.currencyCode,
                        inviteCode = primary.inviteCode,
                        memberUidsCsv = primary.memberUids.joinToString(","),
                        monthlyBudgetPiasters = primary.monthlyBudgetPiasters,
                        preventBudgetOverflow = primary.preventBudgetOverflow,
                        rolloverEnabled = primary.rolloverEnabled,
                        carriedOverPiasters = primary.carriedOverPiasters,
                        alertThreshold1 = primary.alertThreshold1.toInt(),
                        alertThreshold2 = primary.alertThreshold2.toInt(),
                        activeYearMonth = primary.activeYearMonth,
                        updatedAtMillis = primary.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                    dao.upsertHousehold(entity)
                }
            }
            .flatMapLatest { docs ->
                val hhId = docs.firstOrNull()?.id ?: return@flatMapLatest flowOf(null)
                combine(
                    cloudSync.observeCategories(hhId, uid).onEach { list ->
                        if (list.isNotEmpty()) {
                            dao.upsertCategories(list.map {
                                CategoryEntity(
                                    id = it.id,
                                    householdId = it.householdId,
                                    name = it.name,
                                    iconKey = it.iconKey,
                                    colorHex = it.colorHex,
                                    budgetPiasters = it.budgetPiasters,
                                    sortOrder = it.sortOrder.toInt(),
                                    customCategory = it.customCategory,
                                    updatedAtMillis = it.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            })
                        }
                    },
                    cloudSync.observeTransactions(hhId, uid).onEach { list ->
                        if (list.isNotEmpty()) {
                            dao.upsertTransactions(list.map {
                                TransactionEntity(
                                    id = it.id,
                                    householdId = it.householdId,
                                    type = it.type,
                                    amountPiasters = it.amountPiasters,
                                    categoryId = it.categoryId,
                                    categoryName = it.categoryName,
                                    incomeSource = it.incomeSource,
                                    merchant = it.merchant,
                                    notes = it.notes,
                                    dateEpochMillis = it.dateEpochMillis,
                                    yearMonth = it.yearMonth,
                                    receiptUri = it.receiptUri,
                                    addedByUid = it.addedByUid,
                                    addedByName = it.addedByName,
                                    updatedByName = it.updatedByName,
                                    sourceShoppingItemId = it.sourceShoppingItemId,
                                    updatedAtMillis = it.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            })
                        }
                    },
                    cloudSync.observeFunds(hhId, uid).onEach { list ->
                        if (list.isNotEmpty()) {
                            dao.upsertFunds(list.map {
                                SinkingFundEntity(
                                    id = it.id,
                                    householdId = it.householdId,
                                    name = it.name,
                                    iconKey = it.iconKey,
                                    targetPiasters = it.targetPiasters,
                                    savedPiasters = it.savedPiasters,
                                    targetDateMillis = it.targetDateMillis,
                                    updatedAtMillis = it.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            })
                        }
                    },
                    cloudSync.observeShoppingItems(hhId, uid).onEach { list ->
                        if (list.isNotEmpty()) {
                            dao.upsertShoppingItems(list.map {
                                ShoppingItemEntity(
                                    id = it.id,
                                    householdId = it.householdId,
                                    name = it.name,
                                    frequency = it.frequency,
                                    quantityLabel = it.quantityLabel,
                                    expectedPricePiasters = it.expectedPricePiasters,
                                    purchased = it.purchased,
                                    convertedExpenseId = it.convertedExpenseId,
                                    categoryId = it.categoryId,
                                    updatedAtMillis = it.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            })
                        }
                    },
                    cloudSync.observeInventoryItems(hhId, uid).onEach { list ->
                        if (list.isNotEmpty()) {
                            dao.upsertInventoryItems(list.map {
                                InventoryItemEntity(
                                    id = it.id,
                                    householdId = it.householdId,
                                    name = it.name,
                                    quantity = it.quantity,
                                    minThreshold = it.minThreshold,
                                    unit = it.unit,
                                    expiryEpochMillis = it.expiryEpochMillis,
                                    lastActionNote = it.lastActionNote,
                                    updatedAtMillis = it.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            })
                        }
                    }
                ) { _, _, _, _, _ -> null as String? }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val msg = (fsError ?: error).localizedMessage ?: "تعذر مزامنة البيانات السحابية"
                Log.w(TAG, "Cloud sync stream error", fsError ?: error)
                emit(msg)
            }
    }

    fun observeDashboardSnapshot(): Flow<DashboardSnapshot?> =
        combine(
            observeHousehold(),
            observeCategories(),
            observeTransactions(),
            observeInventoryItems(),
            observeFunds()
        ) { household, categories, transactions, inventory, funds ->
            if (household == null) return@combine null
            val activeYm = household.activeYearMonth
            val prevYm = MoneyUtils.previousYearMonth(activeYm)
            val activeTx = transactions.filter { it.yearMonth == activeYm }
            val totalIncome = activeTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPiasters }
            val totalExpense = activeTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPiasters }
            val prevExpense = transactions.filter { it.yearMonth == prevYm && it.type == TransactionType.EXPENSE }
                .sumOf { it.amountPiasters }

            val categoryBudgetsSum = categories.sumOf { it.budgetPiasters }
            val baseBudget = if (household.monthlyBudgetPiasters > 0L) household.monthlyBudgetPiasters else categoryBudgetsSum
            val effectiveBudget = (baseBudget + household.carriedOverPiasters).coerceAtLeast(0L)
            val remaining = effectiveBudget - totalExpense
            val usagePct = if (effectiveBudget > 0L) (totalExpense.toFloat() / effectiveBudget.toFloat()) * 100f else 0f
            val deltaPct = if (prevExpense > 0L) ((totalExpense - prevExpense).toFloat() / prevExpense.toFloat()) * 100f else null

            val expensesByCat = activeTx.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountPiasters } }

            val catSummaries = categories.map { cat ->
                val spent = expensesByCat[cat.id] ?: 0L
                val share = if (totalExpense > 0L) (spent.toFloat() / totalExpense.toFloat()) * 100f else 0f
                CategorySpendingSummary(
                    category = cat,
                    spentPiasters = spent,
                    budgetPiasters = cat.budgetPiasters,
                    sharePercent = share
                )
            }

            val alerts = buildSmartAlerts(household, effectiveBudget, totalExpense, usagePct, catSummaries, inventory, funds)

            DashboardSnapshot(
                household = household,
                activeYearMonth = activeYm,
                totalIncomePiasters = totalIncome,
                effectiveBudgetPiasters = effectiveBudget,
                totalExpensePiasters = totalExpense,
                remainingBudgetPiasters = remaining,
                budgetUsagePercent = usagePct,
                previousMonthExpensePiasters = prevExpense,
                monthDeltaPercent = deltaPct,
                categorySummaries = catSummaries,
                recentTransactions = transactions.take(8),
                alerts = alerts
            )
        }

    private fun buildSmartAlerts(
        household: Household,
        effectiveBudget: Long,
        totalExpense: Long,
        usagePct: Float,
        catSummaries: List<CategorySpendingSummary>,
        inventory: List<InventoryItem>,
        funds: List<SinkingFund>
    ): List<SmartAlert> {
        val alerts = mutableListOf<SmartAlert>()
        if (effectiveBudget > 0L) {
            when {
                usagePct >= 100f -> alerts += SmartAlert(
                    "budget_100",
                    "تم الوصول إلى 100% من الميزانية",
                    "المصروفات (${MoneyUtils.formatCurrency(totalExpense, household.currencyCode)}) وصلت أو تجاوزت الميزانية المحددة.",
                    AlertSeverity.CRITICAL
                )
                usagePct >= household.alertThreshold2 -> alerts += SmartAlert(
                    "budget_t2",
                    "تنبيه: استهلاك ${usagePct.toInt()}% من الميزانية",
                    "اقتربت من الحد الأقصى (${household.alertThreshold2}%) للميزانية الشهرية.",
                    AlertSeverity.CRITICAL
                )
                usagePct >= household.alertThreshold1 -> alerts += SmartAlert(
                    "budget_t1",
                    "متابعة الميزانية (${usagePct.toInt()}%)",
                    "تجاوز الاستهلاك نسبة ${household.alertThreshold1}% من الميزانية المتاحة.",
                    AlertSeverity.WARNING
                )
            }
        }
        catSummaries.filter { it.budgetPiasters > 0 && it.spentPiasters > it.budgetPiasters }.take(2).forEach { cat ->
            alerts += SmartAlert(
                "cat_over_${cat.category.id}",
                "تجاوز ميزانية ${cat.category.name}",
                "تم إنفاق ${MoneyUtils.formatCurrency(cat.spentPiasters, household.currencyCode)} من أصل ${MoneyUtils.formatCurrency(cat.budgetPiasters, household.currencyCode)}.",
                AlertSeverity.WARNING
            )
        }
        val lowStockCount = inventory.count { it.quantity <= it.minThreshold }
        if (lowStockCount > 0) {
            alerts += SmartAlert(
                "inv_low",
                "نقص في مخزون المنزل ($lowStockCount أصناف)",
                "توجد أصناف وصلت للحد الأدنى ويوصى بإضافتها لقائمة المشتريات.",
                AlertSeverity.WARNING
            )
        }
        val expiringSoon = inventory.count { it.expiryEpochMillis > 0L && MoneyUtils.daysUntil(it.expiryEpochMillis) <= 7L }
        if (expiringSoon > 0) {
            alerts += SmartAlert(
                "inv_exp",
                "تنبيه صلاحية المنتجات ($expiringSoon)",
                "هناك منتجات في المخزون قريبة من تاريخ انتهاء الصلاحية خلال 7 أيام.",
                AlertSeverity.CRITICAL
            )
        }
        val upcomingFund = funds.firstOrNull { it.remainingPiasters > 0L && MoneyUtils.daysUntil(it.targetDateMillis) in 0..30 }
        if (upcomingFund != null) {
            alerts += SmartAlert(
                "fund_due_${upcomingFund.id}",
                "تذكير هدف ادخاري: ${upcomingFund.name}",
                "متبقي ${MoneyUtils.formatCurrency(upcomingFund.remainingPiasters, household.currencyCode)} قبل موعد الهدف.",
                AlertSeverity.INFO
            )
        }
        return alerts
    }

    private fun observeHouseholdId(): Flow<String?> =
        dao.observePrimaryHousehold().map { it?.id }.distinctUntilChanged()

    suspend fun saveTransaction(
        existingId: String?,
        type: TransactionType,
        amountPiasters: Long,
        categoryId: String,
        categoryName: String,
        incomeSource: IncomeSource?,
        merchant: String?,
        notes: String?,
        dateEpochMillis: Long,
        receiptUri: String?,
        actorName: String,
        sourceShoppingItemId: String? = null
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val yearMonth = MoneyUtils.currentYearMonth(dateEpochMillis)

        if (type == TransactionType.EXPENSE && hh.preventBudgetOverflow && yearMonth == hh.activeYearMonth) {
            val cats = dao.getCategoriesSync(hh.id)
            val baseBudget = if (hh.monthlyBudgetPiasters > 0L) hh.monthlyBudgetPiasters else cats.sumOf { it.budgetPiasters }
            val effectiveBudget = baseBudget + hh.carriedOverPiasters
            if (effectiveBudget > 0L) {
                val existingTx = dao.getTransactionsSync(hh.id)
                val spentExcludingThis = existingTx
                    .filter { it.yearMonth == hh.activeYearMonth && it.type == TransactionType.EXPENSE.name && it.id != existingId }
                    .sumOf { it.amountPiasters }
                if (spentExcludingThis + amountPiasters > effectiveBudget) {
                    return Result.failure(IllegalStateException("تم تفعيل منع تجاوز الميزانية: هذا المصروف يتجاوز الرصيد المتاح."))
                }
            }
        }

        val now = System.currentTimeMillis()
        val txId = existingId ?: newId("tx")
        val entity = TransactionEntity(
            id = txId,
            householdId = hh.id,
            type = type.name,
            amountPiasters = amountPiasters,
            categoryId = categoryId,
            categoryName = categoryName,
            incomeSource = incomeSource?.name,
            merchant = merchant?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            dateEpochMillis = dateEpochMillis,
            yearMonth = yearMonth,
            receiptUri = receiptUri?.takeIf { it.isNotBlank() },
            addedByUid = uidOrLocal(),
            addedByName = actorName.ifBlank { "مستخدم" },
            updatedByName = actorName.ifBlank { "مستخدم" },
            sourceShoppingItemId = sourceShoppingItemId,
            updatedAtMillis = now
        )
        dao.upsertTransaction(entity)
        if (currentUserId != null) {
            cloudSync.upsertTransaction(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        dao.deleteTransaction(id)
        if (currentUserId != null) {
            cloudSync.deleteSubDoc(hh.id, "transactions", id)
        }
        return Result.success(Unit)
    }

    suspend fun saveCategory(
        existingId: String?,
        name: String,
        iconKey: String,
        colorHex: Long,
        budgetPiasters: Long,
        customCategory: Boolean
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val existingList = dao.getCategoriesSync(hh.id)
        val sortOrder = existingList.firstOrNull { it.id == existingId }?.sortOrder ?: existingList.size
        val entity = CategoryEntity(
            id = existingId ?: newId("cat"),
            householdId = hh.id,
            name = name.trim(),
            iconKey = iconKey,
            colorHex = colorHex,
            budgetPiasters = budgetPiasters.coerceAtLeast(0L),
            sortOrder = sortOrder,
            customCategory = customCategory,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertCategory(entity)
        if (currentUserId != null) {
            cloudSync.upsertCategory(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        dao.deleteCategory(id)
        if (currentUserId != null) {
            cloudSync.deleteSubDoc(hh.id, "categories", id)
        }
        return Result.success(Unit)
    }

    suspend fun updateBudgetSettings(
        monthlyBudgetPiasters: Long,
        preventOverflow: Boolean,
        rolloverEnabled: Boolean,
        threshold1: Int,
        threshold2: Int,
        currencyCode: String
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val updated = hh.copy(
            monthlyBudgetPiasters = monthlyBudgetPiasters.coerceAtLeast(0L),
            preventBudgetOverflow = preventOverflow,
            rolloverEnabled = rolloverEnabled,
            alertThreshold1 = threshold1.coerceIn(1, 99),
            alertThreshold2 = threshold2.coerceIn(threshold1 + 1, 100),
            currencyCode = currencyCode,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertHousehold(updated)
        if (currentUserId != null) {
            cloudSync.upsertHousehold(updated)
        }
        return Result.success(Unit)
    }

    suspend fun updateHouseholdName(name: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val updated = hh.copy(
            name = name.trim().ifBlank { "أسرتي" },
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertHousehold(updated)
        if (currentUserId != null) {
            cloudSync.upsertHousehold(updated)
        }
        return Result.success(Unit)
    }

    suspend fun closeCurrentMonth(): Result<MonthArchive> {
        val hh = currentHouseholdOrThrow()
        val txs = dao.getTransactionsSync(hh.id).filter { it.yearMonth == hh.activeYearMonth }
        val income = txs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountPiasters }
        val expense = txs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountPiasters }
        val cats = dao.getCategoriesSync(hh.id)
        val baseBudget = if (hh.monthlyBudgetPiasters > 0L) hh.monthlyBudgetPiasters else cats.sumOf { it.budgetPiasters }
        val effectiveBudget = baseBudget + hh.carriedOverPiasters
        val rollover = if (hh.rolloverEnabled) (effectiveBudget - expense).coerceAtLeast(0L) else 0L
        val now = System.currentTimeMillis()

        val archive = MonthArchiveEntity(
            id = "arch_${hh.activeYearMonth.replace("-", "_")}",
            householdId = hh.id,
            yearMonth = hh.activeYearMonth,
            totalIncomePiasters = income,
            totalExpensePiasters = expense,
            budgetPiasters = effectiveBudget,
            rolloverPiasters = rollover,
            closedAtMillis = now,
            updatedAtMillis = now
        )
        val updatedHh = hh.copy(
            activeYearMonth = MoneyUtils.nextYearMonth(hh.activeYearMonth),
            carriedOverPiasters = rollover,
            updatedAtMillis = now
        )
        dao.closeMonthAtomically(archive, updatedHh)
        if (currentUserId != null) {
            cloudSync.upsertMonthArchive(archive, memberUidsList(hh))
            cloudSync.upsertHousehold(updatedHh)
        }
        return Result.success(archive.toDomain())
    }

    suspend fun addSinkingFund(name: String, iconKey: String, targetPiasters: Long, targetDateMillis: Long): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val entity = SinkingFundEntity(
            id = newId("fund"),
            householdId = hh.id,
            name = name.trim(),
            iconKey = iconKey,
            targetPiasters = targetPiasters,
            savedPiasters = 0L,
            targetDateMillis = targetDateMillis,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertFund(entity)
        if (currentUserId != null) {
            cloudSync.upsertFund(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun recordFundMovement(
        fund: SinkingFund,
        entryType: FundEntryType,
        amountPiasters: Long,
        note: String?,
        actorName: String
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val newSaved = if (entryType == FundEntryType.DEPOSIT) {
            fund.savedPiasters + amountPiasters
        } else {
            if (amountPiasters > fund.savedPiasters) {
                return Result.failure(IllegalArgumentException("المبلغ المطلوب سحبه أكبر من الرصيد المدخر في الصندوق."))
            }
            fund.savedPiasters - amountPiasters
        }
        val now = System.currentTimeMillis()
        val updatedFund = SinkingFundEntity(
            id = fund.id,
            householdId = hh.id,
            name = fund.name,
            iconKey = fund.iconKey,
            targetPiasters = fund.targetPiasters,
            savedPiasters = newSaved,
            targetDateMillis = fund.targetDateMillis,
            updatedAtMillis = now
        )
        val entry = FundEntryEntity(
            id = newId("fe"),
            householdId = hh.id,
            fundId = fund.id,
            entryType = entryType.name,
            amountPiasters = amountPiasters,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            addedByName = actorName.ifBlank { "مستخدم" },
            dateEpochMillis = now,
            updatedAtMillis = now
        )
        dao.recordFundMovement(updatedFund, entry)
        if (currentUserId != null) {
            val uids = memberUidsList(hh)
            cloudSync.upsertFund(updatedFund, uids)
            cloudSync.upsertFundEntry(entry, uids)
        }
        return Result.success(Unit)
    }

    suspend fun saveShoppingItem(
        existingId: String?,
        name: String,
        frequency: ShoppingFrequency,
        quantityLabel: String,
        expectedPricePiasters: Long,
        categoryId: String,
        purchased: Boolean = false,
        convertedExpenseId: String? = null
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val entity = ShoppingItemEntity(
            id = existingId ?: newId("shop"),
            householdId = hh.id,
            name = name.trim(),
            frequency = frequency.name,
            quantityLabel = quantityLabel.trim().ifBlank { "1" },
            expectedPricePiasters = expectedPricePiasters.coerceAtLeast(0L),
            purchased = purchased,
            convertedExpenseId = convertedExpenseId,
            categoryId = categoryId,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertShoppingItem(entity)
        if (currentUserId != null) {
            cloudSync.upsertShoppingItem(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun convertShoppingItemToExpense(
        item: ShoppingItem,
        actualAmountPiasters: Long,
        categoryName: String,
        actorName: String
    ): Result<Unit> {
        if (item.convertedExpenseId != null) {
            return Result.failure(IllegalStateException("تم تحويل هذا المنتج إلى مصروف فعلي مسبقاً لتجنب التكرار."))
        }
        val hh = currentHouseholdOrThrow()
        val now = System.currentTimeMillis()
        val txId = newId("tx")
        val expense = TransactionEntity(
            id = txId,
            householdId = hh.id,
            type = TransactionType.EXPENSE.name,
            amountPiasters = actualAmountPiasters,
            categoryId = item.categoryId,
            categoryName = categoryName,
            incomeSource = null,
            merchant = "قائمة المشتريات",
            notes = "${item.name} (${item.quantityLabel})",
            dateEpochMillis = now,
            yearMonth = MoneyUtils.currentYearMonth(now),
            receiptUri = null,
            addedByUid = uidOrLocal(),
            addedByName = actorName.ifBlank { "مستخدم" },
            updatedByName = actorName.ifBlank { "مستخدم" },
            sourceShoppingItemId = item.id,
            updatedAtMillis = now
        )
        val itemEntity = ShoppingItemEntity(
            id = item.id,
            householdId = hh.id,
            name = item.name,
            frequency = item.frequency.name,
            quantityLabel = item.quantityLabel,
            expectedPricePiasters = item.expectedPricePiasters,
            purchased = true,
            convertedExpenseId = txId,
            categoryId = item.categoryId,
            updatedAtMillis = now
        )
        dao.convertShoppingItemToExpense(itemEntity, expense)
        if (currentUserId != null) {
            val uids = memberUidsList(hh)
            cloudSync.upsertTransaction(expense, uids)
            cloudSync.upsertShoppingItem(itemEntity, uids)
        }
        return Result.success(Unit)
    }

    suspend fun deleteShoppingItem(id: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        dao.deleteShoppingItem(id)
        if (currentUserId != null) {
            cloudSync.deleteSubDoc(hh.id, "shoppingItems", id)
        }
        return Result.success(Unit)
    }

    suspend fun saveInventoryItem(
        existingId: String?,
        name: String,
        quantity: Double,
        minThreshold: Double,
        unit: String,
        expiryEpochMillis: Long,
        lastActionNote: String?
    ): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val entity = InventoryItemEntity(
            id = existingId ?: newId("inv"),
            householdId = hh.id,
            name = name.trim(),
            quantity = quantity.coerceAtLeast(0.0),
            minThreshold = minThreshold.coerceAtLeast(0.0),
            unit = unit.trim().ifBlank { "عبوة" },
            expiryEpochMillis = expiryEpochMillis.coerceAtLeast(0L),
            lastActionNote = lastActionNote?.trim()?.takeIf { it.isNotBlank() },
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertInventoryItem(entity)
        if (currentUserId != null) {
            cloudSync.upsertInventoryItem(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun deleteInventoryItem(id: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        dao.deleteInventoryItem(id)
        if (currentUserId != null) {
            cloudSync.deleteSubDoc(hh.id, "inventoryItems", id)
        }
        return Result.success(Unit)
    }

    suspend fun saveFamilyMember(existingId: String?, name: String, email: String?, role: FamilyRole): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        val entity = FamilyMemberEntity(
            id = existingId ?: newId("mem"),
            householdId = hh.id,
            userId = existingId ?: "",
            name = name.trim(),
            email = email?.trim()?.takeIf { it.isNotBlank() },
            role = role.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertMember(entity)
        if (currentUserId != null) {
            cloudSync.upsertMember(entity, memberUidsList(hh))
        }
        return Result.success(Unit)
    }

    suspend fun deleteFamilyMember(id: String): Result<Unit> {
        val hh = currentHouseholdOrThrow()
        dao.deleteMember(id)
        if (currentUserId != null) {
            cloudSync.deleteSubDoc(hh.id, "members", id)
        }
        return Result.success(Unit)
    }

    suspend fun joinHouseholdByInviteCode(code: String): Result<Unit> {
        if (currentUserId == null) {
            return Result.failure(IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً للانضمام إلى أسرة مشتركة."))
        }
        val res = cloudSync.joinHouseholdByInviteCode(
            code = code,
            memberName = currentUserName ?: "عضو الأسرة",
            email = currentUserEmail
        )
        return res.map { }
    }

    suspend fun exportBackupJson(): Result<String> = runCatching {
        val hh = currentHouseholdOrThrow()
        val categories = dao.getCategoriesSync(hh.id)
        val txs = dao.getTransactionsSync(hh.id)
        val funds = dao.getFundsSync(hh.id)

        val root = JSONObject()
        root.put("app", "masrouf_albait")
        root.put("version", 1)
        root.put("householdName", hh.name)
        root.put("currencyCode", hh.currencyCode)
        root.put("monthlyBudgetPiasters", hh.monthlyBudgetPiasters)
        root.put("activeYearMonth", hh.activeYearMonth)

        val catArr = JSONArray()
        categories.forEach { c ->
            catArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconKey", c.iconKey)
                put("colorHex", c.colorHex)
                put("budgetPiasters", c.budgetPiasters)
                put("sortOrder", c.sortOrder)
                put("customCategory", c.customCategory)
            })
        }
        root.put("categories", catArr)

        val txArr = JSONArray()
        txs.forEach { t ->
            txArr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amountPiasters", t.amountPiasters)
                put("categoryId", t.categoryId)
                put("categoryName", t.categoryName)
                put("incomeSource", t.incomeSource ?: "")
                put("merchant", t.merchant ?: "")
                put("notes", t.notes ?: "")
                put("dateEpochMillis", t.dateEpochMillis)
                put("yearMonth", t.yearMonth)
                put("addedByName", t.addedByName)
            })
        }
        root.put("transactions", txArr)

        val fundArr = JSONArray()
        funds.forEach { f ->
            fundArr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("iconKey", f.iconKey)
                put("targetPiasters", f.targetPiasters)
                put("savedPiasters", f.savedPiasters)
                put("targetDateMillis", f.targetDateMillis)
            })
        }
        root.put("funds", fundArr)
        root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String): Result<Int> = runCatching {
        val hh = currentHouseholdOrThrow()
        val root = JSONObject(jsonText)
        require(root.optString("app") == "masrouf_albait") { "ملف النسخة الاحتياطية غير متوافق مع تطبيق مصروف البيت" }
        val now = System.currentTimeMillis()
        var importedCount = 0

        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            val entity = CategoryEntity(
                id = o.getString("id"),
                householdId = hh.id,
                name = o.getString("name"),
                iconKey = o.optString("iconKey", "category"),
                colorHex = o.optLong("colorHex", 0xFF10B981),
                budgetPiasters = o.optLong("budgetPiasters", 0L),
                sortOrder = o.optInt("sortOrder", i),
                customCategory = o.optBoolean("customCategory", false),
                updatedAtMillis = now
            )
            dao.upsertCategory(entity)
        }

        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val entity = TransactionEntity(
                id = o.getString("id"),
                householdId = hh.id,
                type = o.optString("type", TransactionType.EXPENSE.name),
                amountPiasters = o.getLong("amountPiasters"),
                categoryId = o.getString("categoryId"),
                categoryName = o.getString("categoryName"),
                incomeSource = o.optString("incomeSource").takeIf { it.isNotBlank() },
                merchant = o.optString("merchant").takeIf { it.isNotBlank() },
                notes = o.optString("notes").takeIf { it.isNotBlank() },
                dateEpochMillis = o.getLong("dateEpochMillis"),
                yearMonth = o.getString("yearMonth"),
                receiptUri = null,
                addedByUid = uidOrLocal(),
                addedByName = o.optString("addedByName", "مستخدم"),
                updatedByName = o.optString("addedByName", "مستخدم"),
                sourceShoppingItemId = null,
                updatedAtMillis = now
            )
            dao.upsertTransaction(entity)
            importedCount++
        }
        importedCount
    }

    private suspend fun currentHouseholdOrThrow(): HouseholdEntity {
        val hh = dao.getHouseholdById("hh_${uidOrLocal()}")
        if (hh != null) return hh
        ensureInitialized()
        return dao.getHouseholdById("hh_${uidOrLocal()}")
            ?: throw IllegalStateException("تعذر تهيئة ملف الأسرة المحلي")
    }

    private fun memberUidsList(hh: HouseholdEntity): List<String> {
        val uid = currentUserId ?: return emptyList()
        val list = hh.memberUidsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return if (uid in list) list else list + uid
    }

    private fun buildDefaultCategories(hhId: String, now: Long): List<CategoryEntity> {
        val specs = listOf(
            Triple("living", "مصاريف المعيشة والاحتياجات الأساسية", 0xFF064E3B to "home"),
            Triple("groceries", "التموين والبقالة", 0xFF10B981 to "shopping_basket"),
            Triple("produce", "الخضار والفاكهة والبيض", 0xFF22C55E to "eco"),
            Triple("meat", "اللحوم والبروتينات", 0xFFEF4444 to "restaurant"),
            Triple("dairy", "الألبان", 0xFF3B82F6 to "local_drink"),
            Triple("cleaning", "المنظفات ومستلزمات المنزل", 0xFF06B6D4 to "cleaning_services"),
            Triple("utilities", "الكهرباء والمياه والغاز والإنترنت", 0xFFF59E0B to "bolt"),
            Triple("bills", "الالتزامات والفواتير", 0xFF8B5CF6 to "receipt_long"),
            Triple("transport", "المواصلات", 0xFF6366F1 to "directions_car"),
            Triple("education", "التعليم", 0xFFEC4899 to "school"),
            Triple("health", "الصحة", 0xFF14B8A6 to "medical_services"),
            Triple("misc", "مصروفات متنوعة", 0xFF64748B to "category")
        )
        return specs.mapIndexed { idx, (key, name, colorIcon) ->
            CategoryEntity(
                id = "cat_$key",
                householdId = hhId,
                name = name,
                iconKey = colorIcon.second,
                colorHex = colorIcon.first,
                budgetPiasters = 0L,
                sortOrder = idx,
                customCategory = false,
                updatedAtMillis = now
            )
        }
    }

    private fun buildDefaultFunds(hhId: String, now: Long): List<SinkingFundEntity> {
        val oneYearLater = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, 1)
        }.timeInMillis
        val defaults = listOf(
            Triple("clothes", "مخصص الملابس", "checkroom" to 500_000L),
            Triple("sacrifice", "مخصص الأضحية", "volunteer_activism" to 1_500_000L),
            Triple("travel", "مخصص الإجازات والسفر", "flight" to 1_000_000L),
            Triple("education_fund", "مخصص التعليم", "school" to 1_200_000L),
            Triple("occasions", "مخصص المناسبات", "celebration" to 600_000L),
            Triple("health_emergency", "صندوق الطوارئ الصحية", "health_and_safety" to 1_000_000L),
            Triple("general_emergency", "صندوق الطوارئ العام", "shield" to 2_000_000L)
        )
        return defaults.map { (key, name, iconTarget) ->
            SinkingFundEntity(
                id = "fund_$key",
                householdId = hhId,
                name = name,
                iconKey = iconTarget.first,
                targetPiasters = iconTarget.second,
                savedPiasters = 0L,
                targetDateMillis = oneYearLater,
                updatedAtMillis = now
            )
        }
    }

    private companion object {
        const val TAG = "BudgetRepository"
    }
}

private fun HouseholdEntity.toDomain() = Household(
    id = id,
    ownerId = ownerId,
    name = name,
    currencyCode = currencyCode,
    inviteCode = inviteCode,
    memberUids = memberUidsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
    monthlyBudgetPiasters = monthlyBudgetPiasters,
    preventBudgetOverflow = preventBudgetOverflow,
    rolloverEnabled = rolloverEnabled,
    carriedOverPiasters = carriedOverPiasters,
    alertThreshold1 = alertThreshold1,
    alertThreshold2 = alertThreshold2,
    activeYearMonth = activeYearMonth
)

private fun FamilyMemberEntity.toDomain() = FamilyMember(
    id = id,
    householdId = householdId,
    userId = userId,
    name = name,
    email = email,
    role = FamilyRole.fromRaw(role)
)

private fun CategoryEntity.toDomain() = Category(
    id = id,
    householdId = householdId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    budgetPiasters = budgetPiasters,
    sortOrder = sortOrder,
    customCategory = customCategory
)

private fun TransactionEntity.toDomain() = BudgetTransaction(
    id = id,
    householdId = householdId,
    type = TransactionType.fromRaw(type),
    amountPiasters = amountPiasters,
    categoryId = categoryId,
    categoryName = categoryName,
    incomeSource = incomeSource?.let { IncomeSource.fromRaw(it) },
    merchant = merchant,
    notes = notes,
    dateEpochMillis = dateEpochMillis,
    yearMonth = yearMonth,
    receiptUri = receiptUri,
    addedByUid = addedByUid,
    addedByName = addedByName,
    updatedByName = updatedByName,
    sourceShoppingItemId = sourceShoppingItemId
)

private fun SinkingFundEntity.toDomain() = SinkingFund(
    id = id,
    householdId = householdId,
    name = name,
    iconKey = iconKey,
    targetPiasters = targetPiasters,
    savedPiasters = savedPiasters,
    targetDateMillis = targetDateMillis
)

private fun FundEntryEntity.toDomain() = FundEntry(
    id = id,
    householdId = householdId,
    fundId = fundId,
    entryType = FundEntryType.fromRaw(entryType),
    amountPiasters = amountPiasters,
    note = note,
    addedByName = addedByName,
    dateEpochMillis = dateEpochMillis
)

private fun ShoppingItemEntity.toDomain() = ShoppingItem(
    id = id,
    householdId = householdId,
    name = name,
    frequency = ShoppingFrequency.fromRaw(frequency),
    quantityLabel = quantityLabel,
    expectedPricePiasters = expectedPricePiasters,
    purchased = purchased,
    convertedExpenseId = convertedExpenseId,
    categoryId = categoryId
)

private fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id,
    householdId = householdId,
    name = name,
    quantity = quantity,
    minThreshold = minThreshold,
    unit = unit,
    expiryEpochMillis = expiryEpochMillis,
    lastActionNote = lastActionNote
)

private fun MonthArchiveEntity.toDomain() = MonthArchive(
    id = id,
    householdId = householdId,
    yearMonth = yearMonth,
    totalIncomePiasters = totalIncomePiasters,
    totalExpensePiasters = totalExpensePiasters,
    budgetPiasters = budgetPiasters,
    rolloverPiasters = rolloverPiasters,
    closedAtMillis = closedAtMillis
)
