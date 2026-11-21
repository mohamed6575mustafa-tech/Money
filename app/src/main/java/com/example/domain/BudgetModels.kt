package com.example.domain

enum class FamilyRole(val labelAr: String, val canEditExpenses: Boolean, val canManageBudget: Boolean, val canManageFamily: Boolean) {
    ADMIN("مدير الأسرة", true, true, true),
    EXPENSE_MANAGER("مسؤول المصروفات", true, true, false),
    MEMBER("مستخدم عادي", true, false, false),
    VIEW_ONLY("للعرض فقط", false, false, false);

    companion object {
        fun fromRaw(raw: String?): FamilyRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class TransactionType {
    EXPENSE, INCOME;

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class IncomeSource(val labelAr: String) {
    SALARY("راتب شهري"),
    EXTRA("دخل إضافي"),
    BONUS("مكافأة / حافز"),
    IRREGULAR("دخل غير منتظم");

    companion object {
        fun fromRaw(raw: String?): IncomeSource =
            entries.firstOrNull { it.name == raw } ?: SALARY
    }
}

enum class FundEntryType(val labelAr: String) {
    DEPOSIT("إيداع"),
    WITHDRAWAL("سحب");

    companion object {
        fun fromRaw(raw: String?): FundEntryType =
            entries.firstOrNull { it.name == raw } ?: DEPOSIT
    }
}

enum class ShoppingFrequency(val labelAr: String) {
    START_OF_MONTH("مشتريات أول الشهر"),
    WEEKLY("مشتريات أسبوعية"),
    MONTHLY("مشتريات شهرية"),
    AS_NEEDED("حسب الحاجة");

    companion object {
        fun fromRaw(raw: String?): ShoppingFrequency =
            entries.firstOrNull { it.name == raw } ?: WEEKLY
    }
}

enum class ThemeModePreference(val labelAr: String) {
    SYSTEM("تلقائي حسب النظام"),
    LIGHT("الوضع النهاري"),
    DARK("الوضع الليلي");

    companion object {
        fun fromRaw(raw: String?): ThemeModePreference =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

data class SmartAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity
)

data class Household(
    val id: String,
    val ownerId: String,
    val name: String,
    val currencyCode: String,
    val inviteCode: String,
    val memberUids: List<String>,
    val monthlyBudgetPiasters: Long,
    val preventBudgetOverflow: Boolean,
    val rolloverEnabled: Boolean,
    val carriedOverPiasters: Long,
    val alertThreshold1: Int,
    val alertThreshold2: Int,
    val activeYearMonth: String
)

data class FamilyMember(
    val id: String,
    val householdId: String,
    val userId: String,
    val name: String,
    val email: String?,
    val role: FamilyRole
)

data class Category(
    val id: String,
    val householdId: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val budgetPiasters: Long,
    val sortOrder: Int,
    val customCategory: Boolean
)

data class BudgetTransaction(
    val id: String,
    val householdId: String,
    val type: TransactionType,
    val amountPiasters: Long,
    val categoryId: String,
    val categoryName: String,
    val incomeSource: IncomeSource?,
    val merchant: String?,
    val notes: String?,
    val dateEpochMillis: Long,
    val yearMonth: String,
    val receiptUri: String?,
    val addedByUid: String,
    val addedByName: String,
    val updatedByName: String,
    val sourceShoppingItemId: String?
)

data class SinkingFund(
    val id: String,
    val householdId: String,
    val name: String,
    val iconKey: String,
    val targetPiasters: Long,
    val savedPiasters: Long,
    val targetDateMillis: Long
) {
    val remainingPiasters: Long get() = (targetPiasters - savedPiasters).coerceAtLeast(0L)
    val progressPercent: Float
        get() = if (targetPiasters <= 0L) 0f else (savedPiasters.toFloat() / targetPiasters.toFloat()).coerceIn(0f, 1f)
}

data class FundEntry(
    val id: String,
    val householdId: String,
    val fundId: String,
    val entryType: FundEntryType,
    val amountPiasters: Long,
    val note: String?,
    val addedByName: String,
    val dateEpochMillis: Long
)

data class ShoppingItem(
    val id: String,
    val householdId: String,
    val name: String,
    val frequency: ShoppingFrequency,
    val quantityLabel: String,
    val expectedPricePiasters: Long,
    val purchased: Boolean,
    val convertedExpenseId: String?,
    val categoryId: String
)

data class InventoryItem(
    val id: String,
    val householdId: String,
    val name: String,
    val quantity: Double,
    val minThreshold: Double,
    val unit: String,
    val expiryEpochMillis: Long,
    val lastActionNote: String?
)

data class MonthArchive(
    val id: String,
    val householdId: String,
    val yearMonth: String,
    val totalIncomePiasters: Long,
    val totalExpensePiasters: Long,
    val budgetPiasters: Long,
    val rolloverPiasters: Long,
    val closedAtMillis: Long
)

data class CategorySpendingSummary(
    val category: Category,
    val spentPiasters: Long,
    val budgetPiasters: Long,
    val sharePercent: Float
)

data class DashboardSnapshot(
    val household: Household,
    val activeYearMonth: String,
    val totalIncomePiasters: Long,
    val effectiveBudgetPiasters: Long,
    val totalExpensePiasters: Long,
    val remainingBudgetPiasters: Long,
    val budgetUsagePercent: Float,
    val previousMonthExpensePiasters: Long,
    val monthDeltaPercent: Float?,
    val categorySummaries: List<CategorySpendingSummary>,
    val recentTransactions: List<BudgetTransaction>,
    val alerts: List<SmartAlert>
)
