package com.example

import com.example.data.local.entity.SavingsAllocationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.WalletEntity
import com.example.data.repository.FinanceRepository
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletType
import com.example.util.CsvManager
import com.example.util.CurrencyFormatter
import com.example.util.CycleDateHelper
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceLogicTest {

    private fun dateMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `cycle 26 to 25 includes Oct 26 through Nov 25 when reference is Nov 24`() {
        val ref = dateMillis(2026, Calendar.NOVEMBER, 24)
        val cycle = CycleDateHelper.getCyclePeriod(cycleStartDay = 26, offsetFromCurrent = 0, referenceMillis = ref)

        val oct26Start = dateMillis(2026, Calendar.OCTOBER, 26, 0, 0)
        val nov25End = dateMillis(2026, Calendar.NOVEMBER, 25, 23, 59)
        val oct25 = dateMillis(2026, Calendar.OCTOBER, 25, 23, 59)
        val nov26 = dateMillis(2026, Calendar.NOVEMBER, 26, 0, 0)

        assertTrue(oct26Start in cycle.startMillis..cycle.endMillis)
        assertTrue(nov25End in cycle.startMillis..cycle.endMillis)
        assertTrue(oct25 < cycle.startMillis)
        assertTrue(nov26 > cycle.endMillis)
    }

    @Test
    fun `cycle 1 to end of month handles leap year February correctly`() {
        val ref = dateMillis(2024, Calendar.FEBRUARY, 15)
        val cycle = CycleDateHelper.getCyclePeriod(cycleStartDay = 1, offsetFromCurrent = 0, referenceMillis = ref)

        val feb1 = dateMillis(2024, Calendar.FEBRUARY, 1, 0, 0)
        val feb29 = dateMillis(2024, Calendar.FEBRUARY, 29, 23, 59)
        val mar1 = dateMillis(2024, Calendar.MARCH, 1, 0, 0)

        assertTrue(feb1 in cycle.startMillis..cycle.endMillis)
        assertTrue(feb29 in cycle.startMillis..cycle.endMillis)
        assertTrue(mar1 > cycle.endMillis)
    }

    @Test
    fun `transfer moves balance between wallets without inflating income or expense`() {
        val wallets = listOf(
            WalletEntity(id = 1L, name = "Bank BCA", type = WalletType.BANK, initialBalance = 1_000_000L),
            WalletEntity(id = 2L, name = "GoPay", type = WalletType.EWALLET, initialBalance = 100_000L),
        )
        val txs = listOf(
            TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 500_000L, walletId = 1L, category = "Gaji", timestamp = 1_000L),
            TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amount = 150_000L, walletId = 1L, category = "Makan", timestamp = 1_100L),
            TransactionEntity(id = 3L, type = TransactionType.TRANSFER, amount = 300_000L, walletId = 1L, targetWalletId = 2L, category = "Transfer Dompet", timestamp = 1_200L),
        )
        val allocations = listOf(
            SavingsAllocationEntity(id = 1L, goalId = 10L, walletId = 2L, amountDelta = 50_000L, timestamp = 1_300L),
        )

        val computed = FinanceRepository.computeWalletBalances(
            wallets = wallets,
            transactions = txs,
            allocations = allocations,
            periodStartMillis = 0L,
            periodEndMillis = 5_000L,
        )

        val bca = computed.first { it.id == 1L }
        val gopay = computed.first { it.id == 2L }

        // BCA: 1,000,000 + 500,000 - 150,000 - 300,000 = 1,050,000
        assertEquals(1_050_000L, bca.currentBalance)
        // GoPay: 100,000 + 300,000 - 50,000 (allocated to savings) = 350,000
        assertEquals(350_000L, gopay.currentBalance)

        // Period income & expense MUST exclude the 300,000 transfer!
        assertEquals(500_000L, bca.periodIncome)
        assertEquals(150_000L, bca.periodExpense)
        assertEquals(0L, gopay.periodIncome)
        assertEquals(0L, gopay.periodExpense)
    }

    @Test
    fun `csv export and import round-trips commas quotes and transfers`() {
        val original = listOf(
            TransactionItem(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 75_000L,
                walletId = 1L,
                walletName = "Tunai",
                targetWalletId = null,
                targetWalletName = null,
                category = "Makan & Minum",
                note = "Kopi, roti \"spesial\"",
                timestamp = 1_760_000_000_000L,
            ),
            TransactionItem(
                id = 2L,
                type = TransactionType.TRANSFER,
                amount = 250_000L,
                walletId = 2L,
                walletName = "Bank BCA",
                targetWalletId = 3L,
                targetWalletName = "GoPay",
                category = "Transfer Dompet",
                note = "Top up e-wallet",
                timestamp = 1_760_000_500_000L,
            ),
        )

        val csv = CsvManager.exportToCsv(original)
        val parsed = CsvManager.parseCsv(csv)

        assertEquals(0, parsed.skippedRows)
        assertEquals(2, parsed.items.size)

        val transfer = parsed.items[0] // sorted descending by timestamp
        assertEquals(TransactionType.TRANSFER, transfer.type)
        assertEquals(250_000L, transfer.amount)
        assertEquals("Bank BCA", transfer.walletName)
        assertEquals("GoPay", transfer.targetWalletName)

        val expense = parsed.items[1]
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals(75_000L, expense.amount)
        assertEquals("Kopi, roti \"spesial\"", expense.note)
    }

    @Test
    fun `rupiah formatter formats Indonesian grouping`() {
        assertEquals("Rp 1.500.000", CurrencyFormatter.formatRupiah(1_500_000L))
        assertEquals("+Rp 250.000", CurrencyFormatter.formatRupiah(250_000L, showSign = true))
        assertEquals("-Rp 50.000", CurrencyFormatter.formatRupiah(-50_000L))
    }

    @Test
    fun `six month spending trends aggregates expenses per cycle and excludes transfers`() {
        val ref = dateMillis(2026, Calendar.NOVEMBER, 24)
        val cycles = CycleDateHelper.getLastNCyclePeriods(
            cycleStartDay = 26,
            count = 6,
            endOffset = 0,
            referenceMillis = ref,
        )
        assertEquals(6, cycles.size)

        val txs = listOf(
            // Cycle index 0 (May 26 - Jun 25)
            TransactionItem(1L, TransactionType.EXPENSE, 1_200_000L, 1L, "BCA", null, null, "Belanja", "", dateMillis(2026, Calendar.JUNE, 10)),
            // Cycle index 4 (Sep 26 - Oct 25)
            TransactionItem(2L, TransactionType.EXPENSE, 850_000L, 1L, "BCA", null, null, "Makan", "", dateMillis(2026, Calendar.OCTOBER, 5)),
            // Cycle index 5 (Oct 26 - Nov 25): 500k expense + 2M transfer (must be excluded!)
            TransactionItem(3L, TransactionType.EXPENSE, 500_000L, 1L, "BCA", null, null, "Tagihan", "", dateMillis(2026, Calendar.NOVEMBER, 12)),
            TransactionItem(4L, TransactionType.TRANSFER, 2_000_000L, 1L, "BCA", 2L, "GoPay", "Transfer Dompet", "", dateMillis(2026, Calendar.NOVEMBER, 15)),
            TransactionItem(5L, TransactionType.INCOME, 7_000_000L, 1L, "BCA", null, null, "Gaji", "", dateMillis(2026, Calendar.NOVEMBER, 1)),
        )

        val trends = com.example.ui.MainViewModel.buildMonthlyTrends(cycles, txs, cycleStartDay = 26)
        assertEquals(6, trends.size)
        assertEquals(1_200_000L, trends[0].totalExpense)
        assertEquals(0L, trends[1].totalExpense)
        assertEquals(850_000L, trends[4].totalExpense)
        assertEquals(500_000L, trends[5].totalExpense)
        assertEquals(7_000_000L, trends[5].totalIncome)
    }
}
