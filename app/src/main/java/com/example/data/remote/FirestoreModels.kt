package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val householdId: String = "",
    val role: String = "ADMIN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HouseholdDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val currencyCode: String = "EGP",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val monthlyBudgetPiasters: Long = 0L,
    val preventBudgetOverflow: Boolean = false,
    val rolloverEnabled: Boolean = true,
    val carriedOverPiasters: Long = 0L,
    val alertThreshold1: Long = 70L,
    val alertThreshold2: Long = 90L,
    val activeYearMonth: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyMemberDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val userId: String? = null,
    val name: String = "",
    val email: String? = null,
    val role: String = "MEMBER",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val iconKey: String = "category",
    val colorHex: Long = 0xFF10B981,
    val budgetPiasters: Long = 0L,
    val sortOrder: Long = 0L,
    val customCategory: Boolean = false,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val type: String = "EXPENSE",
    val amountPiasters: Long = 0L,
    val categoryId: String = "",
    val categoryName: String = "",
    val incomeSource: String? = null,
    val merchant: String? = null,
    val notes: String? = null,
    val dateEpochMillis: Long = 0L,
    val yearMonth: String = "",
    val receiptUri: String? = null,
    val addedByUid: String = "",
    val addedByName: String = "",
    val updatedByName: String = "",
    val sourceShoppingItemId: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SinkingFundDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val iconKey: String = "savings",
    val targetPiasters: Long = 0L,
    val savedPiasters: Long = 0L,
    val targetDateMillis: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FundEntryDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val fundId: String = "",
    val entryType: String = "DEPOSIT",
    val amountPiasters: Long = 0L,
    val note: String? = null,
    val addedByName: String = "",
    val dateEpochMillis: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShoppingItemDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val frequency: String = "WEEKLY",
    val quantityLabel: String = "1",
    val expectedPricePiasters: Long = 0L,
    val purchased: Boolean = false,
    val convertedExpenseId: String? = null,
    val categoryId: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InventoryItemDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val minThreshold: Double = 0.0,
    val unit: String = "عبوة",
    val expiryEpochMillis: Long = 0L,
    val lastActionNote: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthArchiveDoc(
    @DocumentId val id: String = "",
    val householdId: String = "",
    val yearMonth: String = "",
    val totalIncomePiasters: Long = 0L,
    val totalExpensePiasters: Long = 0L,
    val budgetPiasters: Long = 0L,
    val rolloverPiasters: Long = 0L,
    val closedAtMillis: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object FirestorePayloadMapper {
    @Suppress("UNCHECKED_CAST")
    fun nonNullMap(vararg pairs: Pair<String, Any?>): Map<String, Any> =
        pairs.toMap().filterValues { it != null } as Map<String, Any>

    fun serverTs(): Any = FieldValue.serverTimestamp()
}
