package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsAllocationEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // --- Wallets ---
    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    fun observeWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    suspend fun getAllWalletsOnce(): List<WalletEntity>

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("DELETE FROM wallets WHERE id = :walletId")
    suspend fun deleteWalletById(walletId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId OR targetWalletId = :walletId")
    suspend fun countTransactionsForWallet(walletId: Long): Int

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE timestamp >= :startMillis AND timestamp <= :endMillis " +
            "ORDER BY timestamp DESC, id DESC",
    )
    fun observeTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Transaction
    suspend fun replaceAllTransactions(transactions: List<TransactionEntity>) {
        deleteAllTransactions()
        if (transactions.isNotEmpty()) {
            insertTransactions(transactions)
        }
    }

    // --- Savings Goals & Allocations ---
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC, id DESC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :goalId LIMIT 1")
    suspend fun getSavingsGoalById(goalId: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteSavingsGoalById(goalId: Long)

    @Query("SELECT * FROM savings_allocations ORDER BY timestamp DESC, id DESC")
    fun observeSavingsAllocations(): Flow<List<SavingsAllocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsAllocation(allocation: SavingsAllocationEntity): Long

    @Query("DELETE FROM savings_allocations WHERE goalId = :goalId")
    suspend fun deleteAllocationsByGoalId(goalId: Long)

    @Query("DELETE FROM savings_allocations WHERE walletId = :walletId")
    suspend fun deleteAllocationsByWalletId(walletId: Long)

    @Transaction
    suspend fun applySavingsAllocation(
        goalId: Long,
        walletId: Long,
        amountDelta: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
    ): Boolean {
        val goal = getSavingsGoalById(goalId) ?: return false
        val updatedAmount = (goal.currentAmount + amountDelta).coerceAtLeast(0L)
        updateSavingsGoal(goal.copy(currentAmount = updatedAmount))
        insertSavingsAllocation(
            SavingsAllocationEntity(
                goalId = goalId,
                walletId = walletId,
                amountDelta = amountDelta,
                note = note,
                timestamp = timestamp,
            ),
        )
        return true
    }

    @Transaction
    suspend fun deleteSavingsGoalAndRestoreWallets(goalId: Long) {
        deleteAllocationsByGoalId(goalId)
        deleteSavingsGoalById(goalId)
    }

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
