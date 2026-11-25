package com.example.domain.model

enum class TransactionType(val labelId: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan"),
    TRANSFER("Transfer"),
}

enum class WalletType(val labelId: String) {
    CASH("Tunai"),
    BANK("Rekening Bank"),
    EWALLET("E-Wallet"),
    SAVINGS("Tabungan / Trading"),
}

enum class ThemeMode(val labelId: String) {
    SYSTEM("Sistem"),
    LIGHT("Terang"),
    DARK("Gelap"),
}

data class CyclePeriod(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val offsetFromCurrent: Int,
)

data class WalletWithBalance(
    val id: Long,
    val name: String,
    val type: WalletType,
    val initialBalance: Long,
    val currentBalance: Long,
    val colorIndex: Int,
    val periodIncome: Long = 0L,
    val periodExpense: Long = 0L,
)

data class TransactionItem(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val walletId: Long,
    val walletName: String,
    val targetWalletId: Long?,
    val targetWalletName: String?,
    val category: String,
    val note: String,
    val timestamp: Long,
)

data class CategoryBreakdown(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val count: Int,
    val colorIndex: Int,
)

data class DailyPoint(
    val dayLabel: String,
    val fullDateLabel: String,
    val dayStartMillis: Long,
    val amount: Long,
)

data class MonthlyTrendPoint(
    val cycleLabel: String,
    val shortMonthLabel: String,
    val startMillis: Long,
    val endMillis: Long,
    val totalExpense: Long,
    val totalIncome: Long,
)

data class SavingsGoalItem(
    val id: Long,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val deadlineMillis: Long?,
    val linkedWalletId: Long?,
    val colorIndex: Int,
) {
    val progressFraction: Float
        get() = if (targetAmount <= 0L) 0f else (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = ((progressFraction * 100f).toInt()).coerceIn(0, 100)
}

object DefaultCategories {
    val expenseCategories = listOf(
        "Makan & Minum",
        "Transportasi",
        "Belanja",
        "Tagihan & Utilitas",
        "Hiburan",
        "Kesehatan",
        "Pendidikan",
        "Keluarga",
        "Lainnya",
    )

    val incomeCategories = listOf(
        "Gaji",
        "Bonus",
        "Usaha & Freelance",
        "Investasi",
        "Hadiah",
        "Lainnya",
    )

    const val TRANSFER_CATEGORY = "Transfer Dompet"
}
