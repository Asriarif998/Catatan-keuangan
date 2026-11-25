package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsAllocationEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.FinanceRepository
import com.example.domain.model.CategoryBreakdown
import com.example.domain.model.CyclePeriod
import com.example.domain.model.DailyPoint
import com.example.domain.model.MonthlyTrendPoint
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletType
import com.example.domain.model.WalletWithBalance
import com.example.util.CsvManager
import com.example.util.CsvParseResult
import com.example.util.CycleDateHelper
import com.example.util.ParsedCsvTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilters(
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val walletIdFilter: Long? = null,
    val categoryFilter: String? = null,
)

data class DompetKuUiState(
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cycleStartDay: Int = PreferencesManager.DEFAULT_CYCLE_START_DAY,
    val currentCycle: CyclePeriod = CycleDateHelper.getCyclePeriod(PreferencesManager.DEFAULT_CYCLE_START_DAY),
    val wallets: List<WalletWithBalance> = emptyList(),
    val cycleTransactions: List<TransactionItem> = emptyList(),
    val filteredTransactions: List<TransactionItem> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
    val savingsGoals: List<SavingsGoalItem> = emptyList(),
    val savingsAllocations: List<SavingsAllocationEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalWalletBalance: Long = 0L,
    val totalAllocatedSavings: Long = 0L,
    val periodIncome: Long = 0L,
    val periodExpense: Long = 0L,
    val netCashflow: Long = 0L,
    val expenseByCategory: List<CategoryBreakdown> = emptyList(),
    val incomeByCategory: List<CategoryBreakdown> = emptyList(),
    val dailyExpenses: List<DailyPoint> = emptyList(),
    val dailyIncomes: List<DailyPoint> = emptyList(),
    val spendingTrends: List<MonthlyTrendPoint> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: FinanceRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _cycleOffset = MutableStateFlow(0)
    private val _filters = MutableStateFlow(TransactionFilters())
    private val _isSeeded = MutableStateFlow(false)

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultData()
            _isSeeded.value = true
        }
    }

    private val cycleFlow = combine(
        preferencesManager.cycleStartDayFlow,
        _cycleOffset,
    ) { startDay, offset ->
        CycleDateHelper.getCyclePeriod(cycleStartDay = startDay, offsetFromCurrent = offset)
    }

    private val sixMonthCyclesFlow = combine(
        preferencesManager.cycleStartDayFlow,
        _cycleOffset,
    ) { startDay, offset ->
        startDay to CycleDateHelper.getLastNCyclePeriods(
            cycleStartDay = startDay,
            count = 6,
            endOffset = offset,
        )
    }

    private val sixMonthTransactionsFlow = sixMonthCyclesFlow.flatMapLatest { (_, cycles) ->
        repository.observeTransactionsInRange(cycles.first().startMillis, cycles.last().endMillis)
    }

    private val walletsFlow = cycleFlow.flatMapLatest { cycle ->
        repository.observeWalletsWithBalance(cycle.startMillis, cycle.endMillis)
    }

    private val cycleTransactionsFlow = cycleFlow.flatMapLatest { cycle ->
        repository.observeTransactionsInRange(cycle.startMillis, cycle.endMillis)
    }

    private val baseConfigFlow = combine(
        _isSeeded,
        preferencesManager.themeModeFlow,
        preferencesManager.cycleStartDayFlow,
        cycleFlow,
        _filters,
    ) { seeded, themeMode, cycleStartDay, cycle, filters ->
        BaseConfig(seeded, themeMode, cycleStartDay, cycle, filters)
    }

    private val primaryDataFlow = combine(
        walletsFlow,
        cycleTransactionsFlow,
        repository.observeSavingsGoals(),
        repository.observeSavingsAllocations(),
        repository.observeCategories(),
    ) { wallets, txs, goals, allocations, categories ->
        LoadedData(wallets, txs, goals, allocations, categories)
    }

    private val dataFlow = combine(
        primaryDataFlow,
        sixMonthCyclesFlow,
        sixMonthTransactionsFlow,
    ) { primary, (startDay, sixCycles), sixMonthTxs ->
        primary to buildMonthlyTrends(sixCycles, sixMonthTxs, startDay)
    }

    val uiState: StateFlow<DompetKuUiState> = combine(baseConfigFlow, dataFlow) { cfg, (data, trends) ->
        val txs = data.transactions
        val periodIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val periodExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val filtered = applyFilters(txs, cfg.filters)
        val expenseBreakdown = buildCategoryBreakdown(txs, TransactionType.EXPENSE)
        val incomeBreakdown = buildCategoryBreakdown(txs, TransactionType.INCOME)
        val dailyExpenses = buildDailyPoints(txs, cfg.cycle, TransactionType.EXPENSE)
        val dailyIncomes = buildDailyPoints(txs, cfg.cycle, TransactionType.INCOME)

        DompetKuUiState(
            isReady = cfg.seeded,
            themeMode = cfg.themeMode,
            cycleStartDay = cfg.cycleStartDay,
            currentCycle = cfg.cycle,
            wallets = data.wallets,
            cycleTransactions = txs,
            filteredTransactions = filtered,
            filters = cfg.filters,
            savingsGoals = data.goals,
            savingsAllocations = data.allocations,
            categories = data.categories,
            totalWalletBalance = data.wallets.sumOf { it.currentBalance },
            totalAllocatedSavings = data.goals.sumOf { it.currentAmount },
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            netCashflow = periodIncome - periodExpense,
            expenseByCategory = expenseBreakdown,
            incomeByCategory = incomeBreakdown,
            dailyExpenses = dailyExpenses,
            dailyIncomes = dailyIncomes,
            spendingTrends = trends,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DompetKuUiState(),
    )

    // --- Cycle Navigation ---
    fun previousCycle() {
        _cycleOffset.value -= 1
    }

    fun nextCycle() {
        _cycleOffset.value += 1
    }

    fun resetToActiveCycle() {
        _cycleOffset.value = 0
    }

    fun setCycleStartDay(day: Int) {
        viewModelScope.launch {
            preferencesManager.setCycleStartDay(day)
            _cycleOffset.value = 0
            _snackbarEvents.emit("Siklus bulanan diatur mulai tanggal ${day.coerceIn(1, 31)}")
        }
    }

    // --- Theme ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun cycleThemeMode() {
        val next = when (uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    // --- Filters ---
    fun updateSearchQuery(query: String) {
        _filters.value = _filters.value.copy(searchQuery = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filters.value = _filters.value.copy(typeFilter = type)
    }

    fun updateWalletFilter(walletId: Long?) {
        _filters.value = _filters.value.copy(walletIdFilter = walletId)
    }

    fun updateCategoryFilter(category: String?) {
        _filters.value = _filters.value.copy(categoryFilter = category)
    }

    fun clearFilters() {
        _filters.value = TransactionFilters()
    }

    // --- Transaction CRUD ---
    fun saveTransaction(
        id: Long = 0L,
        type: TransactionType,
        amount: Long,
        walletId: Long,
        targetWalletId: Long?,
        category: String,
        note: String,
        timestamp: Long,
    ) {
        viewModelScope.launch {
            val result = repository.saveTransaction(
                id = id,
                type = type,
                amount = amount,
                walletId = walletId,
                targetWalletId = targetWalletId,
                category = category,
                note = note,
                timestamp = timestamp,
            )
            result.onSuccess {
                _snackbarEvents.emit(if (id == 0L) "Transaksi tersimpan" else "Transaksi diperbarui")
            }.onFailure { err ->
                _snackbarEvents.emit(err.message ?: "Gagal menyimpan transaksi")
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _snackbarEvents.emit("Transaksi dihapus")
        }
    }

    // --- Wallet CRUD ---
    fun saveWallet(
        id: Long = 0L,
        name: String,
        type: WalletType,
        initialBalance: Long,
        colorIndex: Int,
    ) {
        viewModelScope.launch {
            repository.saveWallet(id, name, type, initialBalance, colorIndex)
            _snackbarEvents.emit(if (id == 0L) "Dompet ditambahkan" else "Dompet diperbarui")
        }
    }

    fun deleteWallet(walletId: Long) {
        viewModelScope.launch {
            val result = repository.deleteWallet(walletId)
            result.onSuccess {
                _snackbarEvents.emit("Dompet dihapus")
            }.onFailure { err ->
                _snackbarEvents.emit(err.message ?: "Dompet tidak dapat dihapus")
            }
        }
    }

    // --- Savings Goals ---
    fun saveSavingsGoal(
        id: Long = 0L,
        name: String,
        targetAmount: Long,
        deadlineMillis: Long?,
        linkedWalletId: Long?,
        colorIndex: Int,
    ) {
        viewModelScope.launch {
            repository.saveSavingsGoal(id, name, targetAmount, deadlineMillis, linkedWalletId, colorIndex)
            _snackbarEvents.emit(if (id == 0L) "Target tabungan dibuat" else "Target tabungan diperbarui")
        }
    }

    fun allocateToSavingsGoal(goalId: Long, walletId: Long, amount: Long, note: String) {
        viewModelScope.launch {
            repository.allocateToSavingsGoal(goalId, walletId, amount, note)
                .onSuccess { _snackbarEvents.emit("Dana dialokasikan ke target") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Gagal mengalokasikan dana") }
        }
    }

    fun withdrawFromSavingsGoal(goalId: Long, walletId: Long, amount: Long, note: String) {
        viewModelScope.launch {
            repository.withdrawFromSavingsGoal(goalId, walletId, amount, note)
                .onSuccess { _snackbarEvents.emit("Dana ditarik kembali ke dompet") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Gagal menarik dana") }
        }
    }

    fun deleteSavingsGoal(goalId: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goalId)
            _snackbarEvents.emit("Target dihapus & dana dikembalikan ke dompet asal")
        }
    }

    // --- Categories ---
    fun addCategory(name: String, type: TransactionType) {
        viewModelScope.launch {
            repository.addCategory(name, type)
            _snackbarEvents.emit("Kategori \"$name\" ditambahkan")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _snackbarEvents.emit("Kategori dihapus")
        }
    }

    // --- CSV Backup & Restore ---
    suspend fun buildExportCsv(): String {
        return repository.exportAllTransactionsToCsv()
    }

    fun parseCsvPreview(rawCsv: String): CsvParseResult {
        return CsvManager.parseCsv(rawCsv)
    }

    fun importCsvTransactions(items: List<ParsedCsvTransaction>, replaceExisting: Boolean) {
        viewModelScope.launch {
            val count = repository.importParsedCsvTransactions(items, replaceExisting)
            _snackbarEvents.emit("Berhasil mengimpor $count transaksi")
        }
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch { _snackbarEvents.emit(msg) }
    }

    private fun applyFilters(
        transactions: List<TransactionItem>,
        filters: TransactionFilters,
    ): List<TransactionItem> {
        val q = filters.searchQuery.trim().lowercase()
        return transactions.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.category.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.walletName.lowercase().contains(q) ||
                (tx.targetWalletName?.lowercase()?.contains(q) == true)

            val matchesType = filters.typeFilter == null || tx.type == filters.typeFilter
            val matchesWallet = filters.walletIdFilter == null ||
                tx.walletId == filters.walletIdFilter ||
                tx.targetWalletId == filters.walletIdFilter
            val matchesCategory = filters.categoryFilter == null ||
                tx.category.equals(filters.categoryFilter, ignoreCase = true)

            matchesQuery && matchesType && matchesWallet && matchesCategory
        }
    }

    private fun buildCategoryBreakdown(
        transactions: List<TransactionItem>,
        type: TransactionType,
    ): List<CategoryBreakdown> {
        val matching = transactions.filter { it.type == type }
        val total = matching.sumOf { it.amount }
        if (total <= 0L) return emptyList()

        return matching.groupBy { it.category }
            .entries
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                sum to (cat to list.size)
            }
            .sortedByDescending { it.first }
            .mapIndexed { idx, (sum, meta) ->
                CategoryBreakdown(
                    category = meta.first,
                    amount = sum,
                    percentage = sum.toFloat() / total.toFloat(),
                    count = meta.second,
                    colorIndex = idx,
                )
            }
    }

    private fun buildDailyPoints(
        transactions: List<TransactionItem>,
        cycle: CyclePeriod,
        type: TransactionType,
    ): List<DailyPoint> {
        val days = CycleDateHelper.enumerateDaysInCycle(cycle.startMillis, cycle.endMillis)
        val sumsByDay = transactions
            .filter { it.type == type }
            .groupBy { CycleDateHelper.startOfDayMillis(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return days.map { dayStart ->
            DailyPoint(
                dayLabel = CycleDateHelper.formatDayNumber(dayStart),
                fullDateLabel = CycleDateHelper.formatDayMonth(dayStart),
                dayStartMillis = dayStart,
                amount = sumsByDay[dayStart] ?: 0L,
            )
        }
    }

    private data class BaseConfig(
        val seeded: Boolean,
        val themeMode: ThemeMode,
        val cycleStartDay: Int,
        val cycle: CyclePeriod,
        val filters: TransactionFilters,
    )

    private data class LoadedData(
        val wallets: List<WalletWithBalance>,
        val transactions: List<TransactionItem>,
        val goals: List<SavingsGoalItem>,
        val allocations: List<SavingsAllocationEntity>,
        val categories: List<CategoryEntity>,
    )

    class Factory(
        private val repository: FinanceRepository,
        private val preferencesManager: PreferencesManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, preferencesManager) as T
        }
    }

    companion object {
        fun buildMonthlyTrends(
            cycles: List<CyclePeriod>,
            transactions: List<TransactionItem>,
            cycleStartDay: Int,
        ): List<MonthlyTrendPoint> {
            return cycles.map { cycle ->
                val inCycle = transactions.filter { it.timestamp in cycle.startMillis..cycle.endMillis }
                MonthlyTrendPoint(
                    cycleLabel = cycle.label,
                    shortMonthLabel = CycleDateHelper.formatShortCycleMonth(
                        cycleStartDay = cycleStartDay,
                        startMillis = cycle.startMillis,
                        endMillis = cycle.endMillis,
                    ),
                    startMillis = cycle.startMillis,
                    endMillis = cycle.endMillis,
                    totalExpense = inCycle.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    totalIncome = inCycle.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                )
            }
        }
    }
}
