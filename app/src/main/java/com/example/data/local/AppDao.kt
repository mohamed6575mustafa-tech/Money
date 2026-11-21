package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM households LIMIT 1")
    fun observePrimaryHousehold(): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    suspend fun getHouseholdById(id: String): HouseholdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHousehold(entity: HouseholdEntity)

    @Query("SELECT * FROM family_members WHERE householdId = :householdId ORDER BY updatedAtMillis ASC")
    fun observeMembers(householdId: String): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(items: List<FamilyMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(item: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteMember(id: String)

    @Query("SELECT * FROM categories WHERE householdId = :householdId ORDER BY sortOrder ASC")
    fun observeCategories(householdId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE householdId = :householdId ORDER BY sortOrder ASC")
    suspend fun getCategoriesSync(householdId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(item: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("SELECT * FROM transactions WHERE householdId = :householdId ORDER BY dateEpochMillis DESC")
    fun observeTransactions(householdId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE householdId = :householdId ORDER BY dateEpochMillis DESC")
    suspend fun getTransactionsSync(householdId: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(item: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("SELECT * FROM sinking_funds WHERE householdId = :householdId ORDER BY targetDateMillis ASC")
    fun observeFunds(householdId: String): Flow<List<SinkingFundEntity>>

    @Query("SELECT * FROM sinking_funds WHERE householdId = :householdId")
    suspend fun getFundsSync(householdId: String): List<SinkingFundEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFunds(items: List<SinkingFundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFund(item: SinkingFundEntity)

    @Query("DELETE FROM sinking_funds WHERE id = :id")
    suspend fun deleteFund(id: String)

    @Query("SELECT * FROM fund_entries WHERE householdId = :householdId ORDER BY dateEpochMillis DESC")
    fun observeFundEntries(householdId: String): Flow<List<FundEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFundEntries(items: List<FundEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFundEntry(item: FundEntryEntity)

    @Query("SELECT * FROM shopping_items WHERE householdId = :householdId ORDER BY purchased ASC, updatedAtMillis DESC")
    fun observeShoppingItems(householdId: String): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItems(items: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItem(id: String)

    @Query("SELECT * FROM inventory_items WHERE householdId = :householdId ORDER BY quantity ASC")
    fun observeInventoryItems(householdId: String): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryItems(items: List<InventoryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteInventoryItem(id: String)

    @Query("SELECT * FROM month_archives WHERE householdId = :householdId ORDER BY yearMonth DESC")
    fun observeMonthArchives(householdId: String): Flow<List<MonthArchiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthArchives(items: List<MonthArchiveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthArchive(item: MonthArchiveEntity)

    @Transaction
    suspend fun convertShoppingItemToExpense(
        shoppingItem: ShoppingItemEntity,
        expense: TransactionEntity
    ) {
        upsertTransaction(expense)
        upsertShoppingItem(
            shoppingItem.copy(
                purchased = true,
                convertedExpenseId = expense.id,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    @Transaction
    suspend fun recordFundMovement(
        updatedFund: SinkingFundEntity,
        entry: FundEntryEntity
    ) {
        upsertFund(updatedFund)
        upsertFundEntry(entry)
    }

    @Transaction
    suspend fun closeMonthAtomically(
        archive: MonthArchiveEntity,
        updatedHousehold: HouseholdEntity
    ) {
        upsertMonthArchive(archive)
        upsertHousehold(updatedHousehold)
    }

    @Query("DELETE FROM households")
    suspend fun clearHouseholds()
}
