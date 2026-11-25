package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.DefaultCategories
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletWithBalance
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import com.example.util.CycleDateHelper
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    editingTransaction: TransactionItem?,
    initialType: TransactionType,
    wallets: List<WalletWithBalance>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        type: TransactionType,
        amount: Long,
        walletId: Long,
        targetWalletId: Long?,
        category: String,
        note: String,
        timestamp: Long,
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current

    var selectedType by remember {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }
    var amountDigits by remember {
        mutableStateOf(editingTransaction?.amount?.toString().orEmpty())
    }
    var selectedWalletId by remember {
        mutableLongStateOf(
            editingTransaction?.walletId ?: wallets.firstOrNull()?.id ?: 0L,
        )
    }
    var selectedTargetWalletId by remember {
        mutableLongStateOf(
            editingTransaction?.targetWalletId
                ?: wallets.firstOrNull { it.id != selectedWalletId }?.id
                ?: wallets.firstOrNull()?.id
                ?: 0L,
        )
    }

    val availableCategories = remember(selectedType, categories) {
        val list = categories.filter { it.type == selectedType }.map { it.name }
        if (list.isNotEmpty()) list else when (selectedType) {
            TransactionType.EXPENSE -> DefaultCategories.expenseCategories
            TransactionType.INCOME -> DefaultCategories.incomeCategories
            TransactionType.TRANSFER -> listOf(DefaultCategories.TRANSFER_CATEGORY)
        }
    }

    var selectedCategory by remember(selectedType) {
        val initial = editingTransaction?.takeIf { it.type == selectedType }?.category
            ?: availableCategories.firstOrNull()
            ?: "Lainnya"
        mutableStateOf(initial)
    }
    var note by remember { mutableStateOf(editingTransaction?.note.orEmpty()) }
    var timestamp by remember {
        mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (editingTransaction == null) "Catat Transaksi" else "Edit Transaksi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (editingTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_transaction_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus Transaksi",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Transaction Type Segmented Switch
            val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            validationError = null
                            if (type == TransactionType.TRANSFER && selectedTargetWalletId == selectedWalletId) {
                                selectedTargetWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id ?: selectedWalletId
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = types.size),
                        modifier = Modifier.testTag("type_tab_${type.name.lowercase()}"),
                    ) {
                        Text(text = type.labelId, maxLines = 1)
                    }
                }
            }

            // Amount Input with Rupiah formatting preview
            val parsedAmount = CurrencyFormatter.parseAmount(amountDigits)
            OutlinedTextField(
                value = amountDigits,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(13)
                    amountDigits = clean
                    validationError = null
                },
                label = { Text("Nominal (Rp)") },
                prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                supportingText = {
                    if (parsedAmount > 0L) {
                        Text(
                            text = CurrencyFormatter.formatRupiah(parsedAmount),
                            style = TabularAmountStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input"),
            )

            // Quick amount add chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(10_000L to "+10rb", 25_000L to "+25rb", 50_000L to "+50rb", 100_000L to "+100rb", 500_000L to "+500rb")
                    .forEach { (delta, label) ->
                        AssistChip(
                            onClick = {
                                val current = CurrencyFormatter.parseAmount(amountDigits)
                                amountDigits = (current + delta).toString()
                                validationError = null
                            },
                            label = { Text(label) },
                        )
                    }
            }

            // Wallet Selection
            if (selectedType == TransactionType.TRANSFER) {
                Text(
                    text = "Dari Dompet",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WalletChipSelector(
                    wallets = wallets,
                    selectedWalletId = selectedWalletId,
                    onSelect = { selectedWalletId = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ke Dompet Tujuan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            val tmp = selectedWalletId
                            selectedWalletId = selectedTargetWalletId
                            selectedTargetWalletId = tmp
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Tukar Dompet",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tukar")
                    }
                }
                WalletChipSelector(
                    wallets = wallets,
                    selectedWalletId = selectedTargetWalletId,
                    onSelect = { selectedTargetWalletId = it },
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = financeColors.transferContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Transfer antar dompet tidak dihitung sebagai pemasukan atau pengeluaran bulanan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = financeColors.transfer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            } else {
                Text(
                    text = if (selectedType == TransactionType.INCOME) "Masuk ke Dompet" else "Bayar dari Dompet",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WalletChipSelector(
                    wallets = wallets,
                    selectedWalletId = selectedWalletId,
                    onSelect = { selectedWalletId = it },
                )

                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIcons.iconForCategory(cat, selectedType),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            // Date Picker Button
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_date_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tanggal: ${CycleDateHelper.formatFullDate(timestamp)}")
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Misal: Makan siang, bayar listrik, transfer gaji") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val amount = CurrencyFormatter.parseAmount(amountDigits)
                    when {
                        wallets.isEmpty() -> validationError = "Buat minimal satu dompet terlebih dahulu."
                        amount <= 0L -> validationError = "Masukkan nominal lebih dari Rp 0."
                        selectedType == TransactionType.TRANSFER && wallets.size < 2 ->
                            validationError = "Minimal perlu 2 dompet untuk melakukan transfer."
                        selectedType == TransactionType.TRANSFER && selectedWalletId == selectedTargetWalletId ->
                            validationError = "Dompet asal dan tujuan harus berbeda."
                        else -> {
                            onSave(
                                editingTransaction?.id ?: 0L,
                                selectedType,
                                amount,
                                selectedWalletId,
                                if (selectedType == TransactionType.TRANSFER) selectedTargetWalletId else null,
                                selectedCategory,
                                note,
                                timestamp,
                            )
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (editingTransaction == null) "Simpan Transaksi" else "Simpan Perubahan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val localCal = remember(timestamp) {
            Calendar.getInstance().apply { timeInMillis = timestamp }
        }
        val initialUtcMillis = remember(timestamp) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                )
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcSelected ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcSelected
                            }
                            val merged = Calendar.getInstance().apply {
                                timeInMillis = timestamp
                                set(
                                    utcCal.get(Calendar.YEAR),
                                    utcCal.get(Calendar.MONTH),
                                    utcCal.get(Calendar.DAY_OF_MONTH),
                                )
                            }
                            timestamp = merged.timeInMillis
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && editingTransaction != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Transaksi?") },
            text = {
                Text(
                    "Transaksi ${editingTransaction.category} sebesar ${
                        CurrencyFormatter.formatRupiah(editingTransaction.amount)
                    } akan dihapus dan saldo dompet akan disesuaikan otomatis.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(editingTransaction.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_transaction"),
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun WalletChipSelector(
    wallets: List<WalletWithBalance>,
    selectedWalletId: Long,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        wallets.forEach { wallet ->
            val selected = wallet.id == selectedWalletId
            val accent = CategoryIcons.walletColor(wallet.colorIndex)
            FilterChip(
                selected = selected,
                onClick = { onSelect(wallet.id) },
                label = {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(wallet.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = CurrencyFormatter.formatCompactRupiah(wallet.currentBalance),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = CategoryIcons.iconForWalletType(wallet.type),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else accent,
                        modifier = Modifier.size(18.dp),
                    )
                },
                border = if (selected) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                },
            )
        }
    }
}
