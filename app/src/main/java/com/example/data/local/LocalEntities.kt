package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val currencyCode: String,
    val inviteCode: String,
    val memberUidsCsv: String,
    val monthlyBudgetPiasters: Long,
    val preventBudgetOverflow: Boolean,
    val rolloverEnabled: Boolean,
    val carriedOverPiasters: Long,
    val alertThreshold1: Int,
    val alertThreshold2: Int,
    val activeYearMonth: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val userId: String,
    val name: String,
    val email: String?,
    val role: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val budgetPiasters: Long,
    val sortOrder: Int,
    val customCategory: Boolean,
    val updatedAtMillis: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val type: String,
    val amountPiasters: Long,
    val categoryId: String,
    val categoryName: String,
    val incomeSource: String?,
    val merchant: String?,
    val notes: String?,
    val dateEpochMillis: Long,
    val yearMonth: String,
    val receiptUri: String?,
    val addedByUid: String,
    val addedByName: String,
    val updatedByName: String,
    val sourceShoppingItemId: String?,
    val updatedAtMillis: Long
)

@Entity(tableName = "sinking_funds")
data class SinkingFundEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val iconKey: String,
    val targetPiasters: Long,
    val savedPiasters: Long,
    val targetDateMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "fund_entries")
data class FundEntryEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val fundId: String,
    val entryType: String,
    val amountPiasters: Long,
    val note: String?,
    val addedByName: String,
    val dateEpochMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val frequency: String,
    val quantityLabel: String,
    val expectedPricePiasters: Long,
    val purchased: Boolean,
    val convertedExpenseId: String?,
    val categoryId: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val quantity: Double,
    val minThreshold: Double,
    val unit: String,
    val expiryEpochMillis: Long,
    val lastActionNote: String?,
    val updatedAtMillis: Long
)

@Entity(tableName = "month_archives")
data class MonthArchiveEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val yearMonth: String,
    val totalIncomePiasters: Long,
    val totalExpensePiasters: Long,
    val budgetPiasters: Long,
    val rolloverPiasters: Long,
    val closedAtMillis: Long,
    val updatedAtMillis: Long
)
