package com.example.data.repository

import com.example.data.local.dao.FinanceDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsAllocationEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.WalletEntity
import com.example.domain.model.DefaultCategories
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletType
import com.example.domain.model.WalletWithBalance
import com.example.util.CsvManager
import com.example.util.ParsedCsvTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FinanceRepository(private val dao: FinanceDao) {
    private val seedMutex = Mutex()

    suspend fun ensureDefaultData() {
        seedMutex.withLock {
            if (dao.getWalletCount() == 0) {
                val now = System.currentTimeMillis()
                val defaults = listOf(
                    WalletEntity(name = "Tunai", type = WalletType.CASH, initialBalance = 0L, colorIndex = 0, createdAt = now),
                    WalletEntity(name = "Rekening Bank", type = WalletType.BANK, initialBalance = 0L, colorIndex = 1, createdAt = now + 1),
                    WalletEntity(name = "E-Wallet", type = WalletType.EWALLET, initialBalance = 0L, colorIndex = 2, createdAt = now + 2),
                    WalletEntity(name = "Tabungan", type = WalletType.SAVINGS, initialBalance = 0L, colorIndex = 5, createdAt = now + 3),
                )
                defaults.forEach { dao.insertWallet(it) }
            }
            if (dao.getCategoryCount() == 0) {
                val expenseEntities = DefaultCategories.expenseCategories.map {
                    CategoryEntity(name = it, type = TransactionType.EXPENSE, isDefault = true)
                }
                val incomeEntities = DefaultCategories.incomeCategories.map {
                    CategoryEntity(name = it, type = TransactionType.INCOME, isDefault = true)
                }
                dao.insertCategories(expenseEntities + incomeEntities)
            }
        }
    }

    fun observeWalletsWithBalance(periodStartMillis: Long, periodEndMillis: Long): Flow<List<WalletWithBalance>> {
        return combine(
            dao.observeWallets(),
            dao.observeAllTransactions(),
            dao.observeSavingsAllocations(),
        ) { wallets, transactions, allocations ->
            computeWalletBalances(wallets, transactions, allocations, periodStartMillis, periodEndMillis)
        }
    }

    fun observeAllTransactions(): Flow<List<TransactionItem>> {
        return combine(dao.observeAllTransactions(), dao.observeWallets()) { txs, wallets ->
            val walletNames = wallets.associate { it.id to it.name }
            txs.map { tx ->
                TransactionItem(
                    id = tx.id,
                    type = tx.type,
                    amount = tx.amount,
                    walletId = tx.walletId,
                    walletName = walletNames[tx.walletId] ?: "Dompet",
                    targetWalletId = tx.targetWalletId,
                    targetWalletName = tx.targetWalletId?.let { walletNames[it] ?: "Dompet Tujuan" },
                    category = tx.category,
                    note = tx.note,
                    timestamp = tx.timestamp,
                )
            }
        }
    }

    fun observeTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionItem>> {
        return combine(
            dao.observeTransactionsInRange(startMillis, endMillis),
            dao.observeWallets(),
        ) { txs, wallets ->
            val walletNames = wallets.associate { it.id to it.name }
            txs.map { tx ->
                TransactionItem(
                    id = tx.id,
                    type = tx.type,
                    amount = tx.amount,
                    walletId = tx.walletId,
                    walletName = walletNames[tx.walletId] ?: "Dompet",
                    targetWalletId = tx.targetWalletId,
                    targetWalletName = tx.targetWalletId?.let { walletNames[it] ?: "Dompet Tujuan" },
                    category = tx.category,
                    note = tx.note,
                    timestamp = tx.timestamp,
                )
            }
        }
    }

    fun observeSavingsGoals(): Flow<List<SavingsGoalItem>> {
        return dao.observeSavingsGoals().map { list ->
            list.map {
                SavingsGoalItem(
                    id = it.id,
                    name = it.name,
                    targetAmount = it.targetAmount,
                    currentAmount = it.currentAmount,
                    deadlineMillis = it.deadlineMillis,
                    linkedWalletId = it.linkedWalletId,
                    colorIndex = it.colorIndex,
                )
            }
        }
    }

    fun observeSavingsAllocations(): Flow<List<SavingsAllocationEntity>> = dao.observeSavingsAllocations()

    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories()

    // --- Wallet Mutations ---
    suspend fun saveWallet(
        id: Long = 0L,
        name: String,
        type: WalletType,
        initialBalance: Long,
        colorIndex: Int,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        if (id == 0L) {
            dao.insertWallet(
                WalletEntity(
                    name = cleanName,
                    type = type,
                    initialBalance = initialBalance,
                    colorIndex = colorIndex,
                ),
            )
        } else {
            dao.updateWallet(
                WalletEntity(
                    id = id,
                    name = cleanName,
                    type = type,
                    initialBalance = initialBalance,
                    colorIndex = colorIndex,
                ),
            )
        }
    }

    suspend fun deleteWallet(walletId: Long): Result<Unit> {
        val txCount = dao.countTransactionsForWallet(walletId)
        if (txCount > 0) {
            return Result.failure(IllegalStateException("Dompet masih memiliki $txCount transaksi. Hapus atau pindahkan transaksi terlebih dahulu."))
        }
        dao.deleteAllocationsByWalletId(walletId)
        dao.deleteWalletById(walletId)
        return Result.success(Unit)
    }

    // --- Transaction Mutations ---
    suspend fun saveTransaction(
        id: Long = 0L,
        type: TransactionType,
        amount: Long,
        walletId: Long,
        targetWalletId: Long?,
        category: String,
        note: String,
        timestamp: Long,
    ): Result<Unit> {
        if (amount <= 0L) return Result.failure(IllegalArgumentException("Nominal harus lebih dari 0"))
        if (type == TransactionType.TRANSFER && (targetWalletId == null || targetWalletId == walletId)) {
            return Result.failure(IllegalArgumentException("Dompet asal dan tujuan harus berbeda"))
        }
        val finalCategory = if (type == TransactionType.TRANSFER) {
            DefaultCategories.TRANSFER_CATEGORY
        } else {
            category.ifBlank { "Lainnya" }
        }
        val entity = TransactionEntity(
            id = id,
            type = type,
            amount = amount,
            walletId = walletId,
            targetWalletId = if (type == TransactionType.TRANSFER) targetWalletId else null,
            category = finalCategory,
            note = note.trim(),
            timestamp = timestamp,
        )
        if (id == 0L) {
            dao.insertTransaction(entity)
        } else {
            dao.updateTransaction(entity)
        }
        return Result.success(Unit)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    // --- Savings Goals Mutations ---
    suspend fun saveSavingsGoal(
        id: Long = 0L,
        name: String,
        targetAmount: Long,
        deadlineMillis: Long?,
        linkedWalletId: Long?,
        colorIndex: Int,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || targetAmount <= 0L) return
        if (id == 0L) {
            dao.insertSavingsGoal(
                SavingsGoalEntity(
                    name = cleanName,
                    targetAmount = targetAmount,
                    currentAmount = 0L,
                    deadlineMillis = deadlineMillis,
                    linkedWalletId = linkedWalletId,
                    colorIndex = colorIndex,
                ),
            )
        } else {
            val existing = dao.getSavingsGoalById(id) ?: return
            dao.updateSavingsGoal(
                existing.copy(
                    name = cleanName,
                    targetAmount = targetAmount,
                    deadlineMillis = deadlineMillis,
                    linkedWalletId = linkedWalletId,
                    colorIndex = colorIndex,
                ),
            )
        }
    }

    suspend fun allocateToSavingsGoal(goalId: Long, walletId: Long, amount: Long, note: String): Result<Unit> {
        if (amount <= 0L) return Result.failure(IllegalArgumentException("Nominal alokasi harus lebih dari 0"))
        val ok = dao.applySavingsAllocation(
            goalId = goalId,
            walletId = walletId,
            amountDelta = amount,
            note = note.trim().ifBlank { "Alokasi tabungan" },
        )
        return if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Target tidak ditemukan"))
    }

    suspend fun withdrawFromSavingsGoal(goalId: Long, walletId: Long, amount: Long, note: String): Result<Unit> {
        if (amount <= 0L) return Result.failure(IllegalArgumentException("Nominal penarikan harus lebih dari 0"))
        val goal = dao.getSavingsGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Target tidak ditemukan"))
        if (amount > goal.currentAmount) {
            return Result.failure(IllegalArgumentException("Nominal penarikan melebihi dana terkumpul"))
        }
        dao.applySavingsAllocation(
            goalId = goalId,
            walletId = walletId,
            amountDelta = -amount,
            note = note.trim().ifBlank { "Penarikan tabungan" },
        )
        return Result.success(Unit)
    }

    suspend fun deleteSavingsGoal(goalId: Long) {
        dao.deleteSavingsGoalAndRestoreWallets(goalId)
    }

    // --- Categories ---
    suspend fun addCategory(name: String, type: TransactionType) {
        val clean = name.trim()
        if (clean.isEmpty() || type == TransactionType.TRANSFER) return
        dao.insertCategory(CategoryEntity(name = clean, type = type, isDefault = false))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        if (!category.isDefault) {
            dao.deleteCategory(category)
        }
    }

    // --- CSV Export & Import ---
    suspend fun exportAllTransactionsToCsv(): String {
        val wallets = dao.getAllWalletsOnce().associate { it.id to it.name }
        val txs = dao.getAllTransactionsOnce().map { tx ->
            TransactionItem(
                id = tx.id,
                type = tx.type,
                amount = tx.amount,
                walletId = tx.walletId,
                walletName = wallets[tx.walletId] ?: "Dompet",
                targetWalletId = tx.targetWalletId,
                targetWalletName = tx.targetWalletId?.let { wallets[it] ?: "Dompet Tujuan" },
                category = tx.category,
                note = tx.note,
                timestamp = tx.timestamp,
            )
        }
        return CsvManager.exportToCsv(txs)
    }

    suspend fun importParsedCsvTransactions(
        items: List<ParsedCsvTransaction>,
        replaceExisting: Boolean,
    ): Int {
        if (items.isEmpty()) return 0
        val existingWallets = dao.getAllWalletsOnce().toMutableList()
        val walletMap = existingWallets.associateBy { it.name.lowercase() }.toMutableMap()

        suspend fun resolveWalletId(name: String): Long {
            val key = name.trim().lowercase()
            walletMap[key]?.let { return it.id }
            val inferredType = when {
                key.contains("bank") || key.contains("bca") || key.contains("bri") || key.contains("mandiri") -> WalletType.BANK
                key.contains("gopay") || key.contains("ovo") || key.contains("dana") || key.contains("ewallet") -> WalletType.EWALLET
                key.contains("tabung") || key.contains("saving") || key.contains("invest") -> WalletType.SAVINGS
                else -> WalletType.CASH
            }
            val newId = dao.insertWallet(
                WalletEntity(
                    name = name.trim(),
                    type = inferredType,
                    initialBalance = 0L,
                    colorIndex = walletMap.size % 6,
                ),
            )
            val created = WalletEntity(id = newId, name = name.trim(), type = inferredType, initialBalance = 0L)
            walletMap[key] = created
            return newId
        }

        val entities = mutableListOf<TransactionEntity>()
        for (item in items) {
            val sourceId = resolveWalletId(item.walletName)
            val targetId = if (item.type == TransactionType.TRANSFER && !item.targetWalletName.isNullOrBlank()) {
                val resolvedTarget = resolveWalletId(item.targetWalletName)
                if (resolvedTarget == sourceId) null else resolvedTarget
            } else {
                null
            }
            if (item.type == TransactionType.TRANSFER && targetId == null) continue
            entities.add(
                TransactionEntity(
                    type = item.type,
                    amount = item.amount,
                    walletId = sourceId,
                    targetWalletId = targetId,
                    category = item.category,
                    note = item.note,
                    timestamp = item.timestamp,
                ),
            )
        }

        if (replaceExisting) {
            dao.replaceAllTransactions(entities)
        } else {
            dao.insertTransactions(entities)
        }
        return entities.size
    }

    companion object {
        fun computeWalletBalances(
            wallets: List<WalletEntity>,
            transactions: List<TransactionEntity>,
            allocations: List<SavingsAllocationEntity>,
            periodStartMillis: Long,
            periodEndMillis: Long,
        ): List<WalletWithBalance> {
            val balanceDelta = mutableMapOf<Long, Long>()
            val periodIncome = mutableMapOf<Long, Long>()
            val periodExpense = mutableMapOf<Long, Long>()

            for (tx in transactions) {
                val inPeriod = tx.timestamp in periodStartMillis..periodEndMillis
                when (tx.type) {
                    TransactionType.INCOME -> {
                        balanceDelta[tx.walletId] = (balanceDelta[tx.walletId] ?: 0L) + tx.amount
                        if (inPeriod) {
                            periodIncome[tx.walletId] = (periodIncome[tx.walletId] ?: 0L) + tx.amount
                        }
                    }
                    TransactionType.EXPENSE -> {
                        balanceDelta[tx.walletId] = (balanceDelta[tx.walletId] ?: 0L) - tx.amount
                        if (inPeriod) {
                            periodExpense[tx.walletId] = (periodExpense[tx.walletId] ?: 0L) + tx.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        balanceDelta[tx.walletId] = (balanceDelta[tx.walletId] ?: 0L) - tx.amount
                        tx.targetWalletId?.let { targetId ->
                            balanceDelta[targetId] = (balanceDelta[targetId] ?: 0L) + tx.amount
                        }
                        // TRANSFER is intentionally excluded from periodIncome and periodExpense!
                    }
                }
            }

            // Savings allocations reduce available wallet balance on deposit (+) and restore on withdraw (-)
            for (alloc in allocations) {
                balanceDelta[alloc.walletId] = (balanceDelta[alloc.walletId] ?: 0L) - alloc.amountDelta
            }

            return wallets.map { wallet ->
                WalletWithBalance(
                    id = wallet.id,
                    name = wallet.name,
                    type = wallet.type,
                    initialBalance = wallet.initialBalance,
                    currentBalance = wallet.initialBalance + (balanceDelta[wallet.id] ?: 0L),
                    colorIndex = wallet.colorIndex,
                    periodIncome = periodIncome[wallet.id] ?: 0L,
                    periodExpense = periodExpense[wallet.id] ?: 0L,
                )
            }
        }
    }
}
