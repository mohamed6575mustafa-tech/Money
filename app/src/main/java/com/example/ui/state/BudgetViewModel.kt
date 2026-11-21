package com.example.ui.state

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.MoneyUtils
import com.example.core.ReportExporter
import com.example.data.BudgetRepository
import com.example.data.ai.FinancialAdvisorRepository
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.domain.BudgetTransaction
import com.example.domain.Category
import com.example.domain.DashboardSnapshot
import com.example.domain.FamilyMember
import com.example.domain.FamilyRole
import com.example.domain.FundEntry
import com.example.domain.FundEntryType
import com.example.domain.IncomeSource
import com.example.domain.InventoryItem
import com.example.domain.MonthArchive
import com.example.domain.ShoppingFrequency
import com.example.domain.ShoppingItem
import com.example.domain.SinkingFund
import com.example.domain.ThemeModePreference
import com.example.domain.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String
)

sealed interface AiAdvisorUiState {
    data object Idle : AiAdvisorUiState
    data object Loading : AiAdvisorUiState
    data class Streaming(val partialText: String) : AiAdvisorUiState
    data class Error(val message: String) : AiAdvisorUiState
}

data class FeedbackBanner(
    val message: String,
    val isError: Boolean = false
)

data class BudgetUiState(
    val isLoading: Boolean = true,
    val snapshot: DashboardSnapshot? = null,
    val categories: List<Category> = emptyList(),
    val transactions: List<BudgetTransaction> = emptyList(),
    val funds: List<SinkingFund> = emptyList(),
    val fundEntries: List<FundEntry> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val archives: List<MonthArchive> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val syncError: String? = null
)

class BudgetViewModel(
    private val repository: BudgetRepository,
    private val preferencesRepository: PreferencesRepository,
    private val advisorRepository: FinancialAdvisorRepository,
    private val isCloudMode: Boolean,
    private val appContext: Context
) : ViewModel() {

    private val _feedback = MutableStateFlow<FeedbackBanner?>(null)
    val feedback: StateFlow<FeedbackBanner?> = _feedback.asStateFlow()

    private val _aiState = MutableStateFlow<AiAdvisorUiState>(AiAdvisorUiState.Idle)
    val aiState: StateFlow<AiAdvisorUiState> = _aiState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private var aiJob: Job? = null

    val isOnline: StateFlow<Boolean> = observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), checkNetworkNow())

    private data class CoreBudgetSlice(
        val snapshot: DashboardSnapshot?,
        val categories: List<Category>,
        val transactions: List<BudgetTransaction>,
        val funds: List<SinkingFund>,
        val fundEntries: List<FundEntry>
    )

    private data class SecondaryBudgetSlice(
        val shoppingItems: List<ShoppingItem>,
        val inventoryItems: List<InventoryItem>,
        val members: List<FamilyMember>,
        val archives: List<MonthArchive>,
        val preferences: UserPreferences
    )

    private val coreSliceFlow = combine(
        repository.observeDashboardSnapshot(),
        repository.observeCategories(),
        repository.observeTransactions(),
        repository.observeFunds(),
        repository.observeFundEntries()
    ) { snapshot, categories, transactions, funds, fundEntries ->
        CoreBudgetSlice(snapshot, categories, transactions, funds, fundEntries)
    }

    private val secondarySliceFlow = combine(
        repository.observeShoppingItems(),
        repository.observeInventoryItems(),
        repository.observeMembers(),
        repository.observeMonthArchives(),
        preferencesRepository.preferencesFlow
    ) { shopping, inventory, members, archives, prefs ->
        SecondaryBudgetSlice(shopping, inventory, members, archives, prefs)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        coreSliceFlow,
        secondarySliceFlow,
        repository.observeCloudSyncFeed()
    ) { core, sec, syncError ->
        BudgetUiState(
            isLoading = false,
            snapshot = core.snapshot,
            categories = core.categories,
            transactions = core.transactions,
            funds = core.funds,
            fundEntries = core.fundEntries,
            shoppingItems = sec.shoppingItems,
            inventoryItems = sec.inventoryItems,
            members = sec.members,
            archives = sec.archives,
            preferences = sec.preferences,
            syncError = syncError
        )
    }.catch { e ->
        if (e is CancellationException) throw e
        emit(BudgetUiState(isLoading = false, syncError = e.localizedMessage ?: "حدث خطأ غير متوقع"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = BudgetUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            repository.ensureInitialized(defaultCurrency = prefs.currencyCode)
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun showSuccess(msg: String) {
        _feedback.value = FeedbackBanner(msg, isError = false)
    }

    private fun showError(msg: String) {
        _feedback.value = FeedbackBanner(msg, isError = true)
    }

    fun saveTransaction(
        existingId: String?,
        type: TransactionType,
        amountText: String,
        categoryId: String,
        categoryName: String,
        incomeSource: IncomeSource?,
        merchant: String?,
        notes: String?,
        dateEpochMillis: Long,
        receiptUri: String?
    ) {
        val piasters = MoneyUtils.parseAmountToPiasters(amountText)
        if (piasters == null || piasters <= 0L) {
            showError("يرجى إدخال مبلغ صحيح أكبر من صفر")
            return
        }
        viewModelScope.launch {
            val actor = uiState.value.preferences.activeMemberName
            val res = repository.saveTransaction(
                existingId = existingId,
                type = type,
                amountPiasters = piasters,
                categoryId = categoryId,
                categoryName = categoryName,
                incomeSource = incomeSource,
                merchant = merchant,
                notes = notes,
                dateEpochMillis = dateEpochMillis,
                receiptUri = receiptUri,
                actorName = actor
            )
            res.onSuccess {
                showSuccess(if (type == TransactionType.EXPENSE) "تم حفظ المصروف بنجاح" else "تم تسجيل الدخل بنجاح")
            }.onFailure {
                showError(it.message ?: "تعذر حفظ العملية")
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onSuccess { showSuccess("تم حذف العملية وتحديث الميزانية") }
                .onFailure { showError(it.message ?: "تعذر حذف العملية") }
        }
    }

    fun saveCategory(existingId: String?, name: String, colorHex: Long, budgetText: String, iconKey: String = "category") {
        if (name.trim().isEmpty()) {
            showError("يرجى إدخال اسم التصنيف")
            return
        }
        val budgetPiasters = MoneyUtils.parseNonNegativeToPiasters(budgetText)
        if (budgetPiasters == null) {
            showError("يرجى إدخال ميزانية صحيحة للتصنيف")
            return
        }
        viewModelScope.launch {
            repository.saveCategory(
                existingId = existingId,
                name = name,
                iconKey = iconKey,
                colorHex = colorHex,
                budgetPiasters = budgetPiasters,
                customCategory = existingId == null || uiState.value.categories.firstOrNull { it.id == existingId }?.customCategory == true
            ).onSuccess {
                showSuccess("تم حفظ التصنيف بنجاح")
            }.onFailure {
                showError(it.message ?: "تعذر حفظ التصنيف")
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
                .onSuccess { showSuccess("تم حذف التصنيف") }
                .onFailure { showError(it.message ?: "تعذر حذف التصنيف") }
        }
    }

    fun updateBudgetSettings(
        monthlyBudgetText: String,
        preventOverflow: Boolean,
        rolloverEnabled: Boolean,
        threshold1: Int,
        threshold2: Int,
        currencyCode: String
    ) {
        val budgetPiasters = MoneyUtils.parseNonNegativeToPiasters(monthlyBudgetText)
        if (budgetPiasters == null) {
            showError("يرجى إدخال قيمة ميزانية صحيحة")
            return
        }
        viewModelScope.launch {
            preferencesRepository.setCurrencyCode(currencyCode)
            repository.updateBudgetSettings(
                monthlyBudgetPiasters = budgetPiasters,
                preventOverflow = preventOverflow,
                rolloverEnabled = rolloverEnabled,
                threshold1 = threshold1,
                threshold2 = threshold2,
                currencyCode = currencyCode
            ).onSuccess {
                showSuccess("تم تحديث إعدادات الميزانية الشهرية")
            }.onFailure {
                showError(it.message ?: "تعذر تحديث الميزانية")
            }
        }
    }

    fun closeCurrentMonth() {
        viewModelScope.launch {
            repository.closeCurrentMonth()
                .onSuccess { archive ->
                    val currency = uiState.value.snapshot?.household?.currencyCode ?: "EGP"
                    showSuccess(
                        "تم إغلاق شهر ${MoneyUtils.yearMonthLabelAr(archive.yearMonth)} وترحيل ${MoneyUtils.formatCurrency(archive.rolloverPiasters, currency)}"
                    )
                }
                .onFailure { showError(it.message ?: "تعذر إغلاق الشهر") }
        }
    }

    fun addSinkingFund(name: String, targetText: String, targetDateMillis: Long) {
        if (name.trim().isEmpty()) {
            showError("يرجى إدخال اسم المخصص")
            return
        }
        val target = MoneyUtils.parseAmountToPiasters(targetText)
        if (target == null) {
            showError("يرجى إدخال مبلغ مستهدف صحيح")
            return
        }
        viewModelScope.launch {
            repository.addSinkingFund(name, "savings", target, targetDateMillis)
                .onSuccess { showSuccess("تم إنشاء صندوق الادخار بنجاح") }
                .onFailure { showError(it.message ?: "تعذر إنشاء الصندوق") }
        }
    }

    fun recordFundMovement(fund: SinkingFund, entryType: FundEntryType, amountText: String, note: String?) {
        val piasters = MoneyUtils.parseAmountToPiasters(amountText)
        if (piasters == null) {
            showError("يرجى إدخال مبلغ صحيح")
            return
        }
        viewModelScope.launch {
            val actor = uiState.value.preferences.activeMemberName
            repository.recordFundMovement(fund, entryType, piasters, note, actor)
                .onSuccess {
                    showSuccess(if (entryType == FundEntryType.DEPOSIT) "تم الإيداع في ${fund.name} (لا يُحتسب كمصروف)" else "تم السحب من ${fund.name}")
                }
                .onFailure { showError(it.message ?: "تعذر تنفيذ الحركة") }
        }
    }

    fun saveShoppingItem(
        existingId: String?,
        name: String,
        frequency: ShoppingFrequency,
        quantityLabel: String,
        expectedPriceText: String,
        categoryId: String,
        purchased: Boolean = false,
        convertedExpenseId: String? = null
    ) {
        if (name.trim().isEmpty()) {
            showError("يرجى إدخال اسم المنتج")
            return
        }
        val pricePiasters = MoneyUtils.parseNonNegativeToPiasters(expectedPriceText) ?: 0L
        viewModelScope.launch {
            repository.saveShoppingItem(existingId, name, frequency, quantityLabel, pricePiasters, categoryId, purchased, convertedExpenseId)
                .onSuccess { showSuccess("تم تحديث قائمة المشتريات") }
                .onFailure { showError(it.message ?: "تعذر حفظ المنتج") }
        }
    }

    fun toggleShoppingPurchased(item: ShoppingItem) {
        viewModelScope.launch {
            repository.saveShoppingItem(
                existingId = item.id,
                name = item.name,
                frequency = item.frequency,
                quantityLabel = item.quantityLabel,
                expectedPricePiasters = item.expectedPricePiasters,
                categoryId = item.categoryId,
                purchased = !item.purchased,
                convertedExpenseId = item.convertedExpenseId
            )
        }
    }

    fun convertShoppingToExpense(item: ShoppingItem, actualPriceText: String) {
        val amountPiasters = MoneyUtils.parseAmountToPiasters(actualPriceText)
        if (amountPiasters == null) {
            showError("يرجى إدخال السعر الفعلي لتحويله لمصروف")
            return
        }
        viewModelScope.launch {
            val catName = uiState.value.categories.firstOrNull { it.id == item.categoryId }?.name ?: "التموين والبقالة"
            val actor = uiState.value.preferences.activeMemberName
            repository.convertShoppingItemToExpense(item, amountPiasters, catName, actor)
                .onSuccess { showSuccess("تم تحويل \"${item.name}\" إلى مصروف فعلي وربطه بالميزانية") }
                .onFailure { showError(it.message ?: "تعذر التحويل") }
        }
    }

    fun deleteShoppingItem(id: String) {
        viewModelScope.launch { repository.deleteShoppingItem(id) }
    }

    fun saveInventoryItem(
        existingId: String?,
        name: String,
        quantityText: String,
        minThresholdText: String,
        unit: String,
        expiryEpochMillis: Long,
        actionNote: String? = null
    ) {
        if (name.trim().isEmpty()) {
            showError("يرجى إدخال اسم الصنف")
            return
        }
        val qty = quantityText.trim().toDoubleOrNull()
        val min = minThresholdText.trim().toDoubleOrNull() ?: 1.0
        if (qty == null || qty < 0.0) {
            showError("يرجى إدخال كمية صحيحة")
            return
        }
        viewModelScope.launch {
            repository.saveInventoryItem(existingId, name, qty, min, unit, expiryEpochMillis, actionNote)
                .onSuccess { showSuccess("تم تحديث مخزون المنزل") }
                .onFailure { showError(it.message ?: "تعذر حفظ المخزون") }
        }
    }

    fun adjustInventoryQuantity(item: InventoryItem, delta: Double) {
        val nextQty = (item.quantity + delta).coerceAtLeast(0.0)
        val note = if (delta < 0) "استهلاك منزلي (${MoneyUtils.formatShortDateAr(System.currentTimeMillis())})"
        else "إعادة شراء (${MoneyUtils.formatShortDateAr(System.currentTimeMillis())})"
        viewModelScope.launch {
            repository.saveInventoryItem(
                existingId = item.id,
                name = item.name,
                quantity = nextQty,
                minThreshold = item.minThreshold,
                unit = item.unit,
                expiryEpochMillis = item.expiryEpochMillis,
                lastActionNote = note
            )
        }
    }

    fun addInventoryToShoppingList(item: InventoryItem) {
        val defaultCat = uiState.value.categories.firstOrNull { it.id.contains("grocer") }?.id
            ?: uiState.value.categories.firstOrNull()?.id ?: "cat_groceries"
        viewModelScope.launch {
            repository.saveShoppingItem(
                existingId = null,
                name = item.name,
                frequency = ShoppingFrequency.AS_NEEDED,
                quantityLabel = "1 ${item.unit}",
                expectedPricePiasters = 0L,
                categoryId = defaultCat
            ).onSuccess {
                showSuccess("تمت إضافة \"${item.name}\" إلى قائمة المشتريات")
            }
        }
    }

    fun deleteInventoryItem(id: String) {
        viewModelScope.launch { repository.deleteInventoryItem(id) }
    }

    fun updateHouseholdName(name: String) {
        viewModelScope.launch {
            repository.updateHouseholdName(name)
                .onSuccess { showSuccess("تم تحديث اسم ملف الأسرة") }
                .onFailure { showError(it.message ?: "تعذر تحديث الاسم") }
        }
    }

    fun saveFamilyMember(existingId: String?, name: String, email: String?, role: FamilyRole) {
        if (name.trim().isEmpty()) {
            showError("يرجى إدخال اسم فرد الأسرة")
            return
        }
        viewModelScope.launch {
            repository.saveFamilyMember(existingId, name, email, role)
                .onSuccess { showSuccess("تم حفظ بيانات العضو بنجاح") }
                .onFailure { showError(it.message ?: "تعذر حفظ العضو") }
        }
    }

    fun deleteFamilyMember(id: String) {
        viewModelScope.launch { repository.deleteFamilyMember(id) }
    }

    fun joinHouseholdByCode(code: String) {
        if (code.trim().length < 4) {
            showError("يرجى إدخال كود الدعوة بشكل صحيح")
            return
        }
        viewModelScope.launch {
            repository.joinHouseholdByInviteCode(code)
                .onSuccess { showSuccess("تم الانضمام إلى الأسرة المشتركة ومزامنة البيانات!") }
                .onFailure { showError(it.message ?: "تعذر الانضمام بالكود") }
        }
    }

    fun setActiveMemberName(name: String) {
        viewModelScope.launch { preferencesRepository.setActiveMemberName(name) }
    }

    fun markWeeklyReviewCompleted() {
        viewModelScope.launch {
            preferencesRepository.markWeeklyReviewCompleted()
            showSuccess("أحسنت! تم إتمام المراجعة المالية الأسبوعية بنجاح")
        }
    }

    fun saveThemeCustomization(primaryArgb: Long, secondaryArgb: Long, accentArgb: Long, mode: ThemeModePreference) {
        viewModelScope.launch {
            preferencesRepository.setThemeColors(primaryArgb, secondaryArgb, accentArgb)
            preferencesRepository.setThemeMode(mode)
            showSuccess("تم حفظ ألوان وهوية التطبيق بنجاح")
        }
    }

    fun resetThemeColors() {
        viewModelScope.launch {
            preferencesRepository.resetThemeColors()
            showSuccess("تمت استعادة الألوان الافتراضية")
        }
    }

    fun setThemeMode(mode: ThemeModePreference) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrencyCode(code)
            val hh = uiState.value.snapshot?.household
            if (hh != null) {
                repository.updateBudgetSettings(
                    hh.monthlyBudgetPiasters,
                    hh.preventBudgetOverflow,
                    hh.rolloverEnabled,
                    hh.alertThreshold1,
                    hh.alertThreshold2,
                    code
                )
            }
            showSuccess("تم تغيير العملة إلى $code")
        }
    }

    fun updateNotificationSettings(budget: Boolean, inventory: Boolean, weekly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationSettings(budget, inventory, weekly)
            showSuccess("تم تحديث إعدادات التنبيهات")
        }
    }

    fun exportCsvReport(title: String, list: List<BudgetTransaction>) {
        val currency = uiState.value.preferences.currencyCode
        ReportExporter.exportCsvAndShare(appContext, title, list, currency)
            .onFailure { showError("تعذر تصدير ملف Excel/CSV: ${it.localizedMessage}") }
    }

    fun exportPdfReport(title: String, periodLabel: String, list: List<BudgetTransaction>) {
        val state = uiState.value
        val currency = state.preferences.currencyCode
        val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPiasters }
        val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPiasters }
        val summaries = state.snapshot?.categorySummaries ?: emptyList()
        ReportExporter.exportPdfAndShare(
            context = appContext,
            title = title,
            periodLabel = periodLabel,
            totalIncomePiasters = income,
            totalExpensePiasters = expense,
            categorySummaries = summaries,
            transactions = list,
            currencyCode = currency
        ).onFailure { showError("تعذر إنشاء تقرير PDF: ${it.localizedMessage}") }
    }

    suspend fun generateBackupJson(): Result<String> = repository.exportBackupJson()

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            repository.importBackupJson(jsonText)
                .onSuccess { count -> showSuccess("تمت استعادة $count عملية مالية بنجاح") }
                .onFailure { showError(it.message ?: "تعذر قراءة ملف النسخة الاحتياطية") }
        }
    }

    fun askAiAdvisor(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        if (!isOnline.value) {
            _aiState.value = AiAdvisorUiState.Error("المساعد المالي الذكي يتطلب اتصالاً بالإنترنت. يرجى التحقق من الشبكة والمحاولة مرة أخرى.")
            return
        }
        aiJob?.cancel()
        val state = uiState.value
        val contextStr = advisorRepository.buildFinancialContext(
            snapshot = state.snapshot,
            funds = state.funds,
            currencyCode = state.preferences.currencyCode
        )
        _aiMessages.value = _aiMessages.value + AiChatMessage("u_${System.currentTimeMillis()}", true, trimmed)
        _aiState.value = AiAdvisorUiState.Loading

        aiJob = viewModelScope.launch {
            var lastText = ""
            try {
                advisorRepository.streamAdvice(trimmed, contextStr).collect { partial ->
                    lastText = partial
                    _aiState.value = AiAdvisorUiState.Streaming(partial)
                }
                if (lastText.isNotBlank()) {
                    _aiMessages.value = _aiMessages.value + AiChatMessage("a_${System.currentTimeMillis()}", false, lastText)
                    _aiState.value = AiAdvisorUiState.Idle
                } else {
                    _aiState.value = AiAdvisorUiState.Error("لم يتم استلام رد من المساعد الذكي.")
                }
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiAdvisorUiState.Error("انتهت مهلة الاتصال بالمساعد الذكي (60 ثانية). حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiAdvisorUiState.Error(e.localizedMessage ?: "تعذر الاتصال بالمساعد المالي الذكي.")
            }
        }
    }

    private fun checkNetworkNow(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun observeConnectivity() = callbackFlow {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(true)
            close()
            return@callbackFlow
        }
        trySend(checkNetworkNow())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(checkNetworkNow()) }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
