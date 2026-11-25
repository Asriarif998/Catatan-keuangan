package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.WalletWithBalance
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import com.example.util.CycleDateHelper
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalFormSheet(
    editingGoal: SavingsGoalItem?,
    wallets: List<WalletWithBalance>,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, targetAmount: Long, deadlineMillis: Long?, linkedWalletId: Long?, colorIndex: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(editingGoal?.name.orEmpty()) }
    var targetDigits by remember {
        mutableStateOf(editingGoal?.targetAmount?.toString().orEmpty())
    }
    var deadlineMillis by remember { mutableStateOf(editingGoal?.deadlineMillis) }
    var linkedWalletId by remember {
        mutableStateOf(editingGoal?.linkedWalletId ?: wallets.firstOrNull()?.id)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
            Text(
                text = if (editingGoal == null) "Buat Target Tabungan" else "Edit Target Tabungan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMsg = null
                },
                label = { Text("Nama Target") },
                placeholder = { Text("Misal: Dana Darurat, Laptop Baru, Liburan") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("savings_name_input"),
            )

            val parsedTarget = CurrencyFormatter.parseAmount(targetDigits)
            OutlinedTextField(
                value = targetDigits,
                onValueChange = { raw ->
                    targetDigits = raw.filter { it.isDigit() }.take(13)
                    errorMsg = null
                },
                label = { Text("Nominal Target (Rp)") },
                prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                supportingText = {
                    if (parsedTarget > 0L) {
                        Text(
                            text = CurrencyFormatter.formatRupiah(parsedTarget),
                            style = TabularAmountStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("savings_target_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = deadlineMillis?.let { "Tenggat: ${CycleDateHelper.formatShortDate(it)}" }
                            ?: "Tenggat Waktu (Opsional)",
                    )
                }
                if (deadlineMillis != null) {
                    IconButton(onClick = { deadlineMillis = null }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Hapus Tenggat")
                    }
                }
            }

            if (wallets.isNotEmpty()) {
                Text(
                    text = "Dompet Utama Terkait",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = linkedWalletId == w.id,
                            onClick = { linkedWalletId = w.id },
                            label = { Text(w.name) },
                        )
                    }
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    when {
                        name.trim().isEmpty() -> errorMsg = "Nama target wajib diisi."
                        parsedTarget <= 0L -> errorMsg = "Nominal target harus lebih dari Rp 0."
                        else -> {
                            onSave(
                                editingGoal?.id ?: 0L,
                                name,
                                parsedTarget,
                                deadlineMillis,
                                linkedWalletId,
                                editingGoal?.colorIndex ?: (name.length % 6),
                            )
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_savings_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (editingGoal == null) "Simpan Target" else "Perbarui Target",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val baseMillis = deadlineMillis ?: System.currentTimeMillis()
        val localCal = Calendar.getInstance().apply { timeInMillis = baseMillis }
        val initUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initUtc)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utc ->
                            val uCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                            val resultCal = Calendar.getInstance().apply {
                                clear()
                                set(uCal.get(Calendar.YEAR), uCal.get(Calendar.MONTH), uCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                            }
                            deadlineMillis = resultCal.timeInMillis
                        }
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAllocationSheet(
    goal: SavingsGoalItem,
    isDeposit: Boolean,
    wallets: List<WalletWithBalance>,
    onDismiss: () -> Unit,
    onConfirm: (walletId: Long, amount: Long, note: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedWalletId by remember {
        mutableLongStateOf(goal.linkedWalletId ?: wallets.firstOrNull()?.id ?: 0L)
    }
    var amountDigits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
            Text(
                text = if (isDeposit) "Alokasi Dana ke ${goal.name}" else "Tarik Dana dari ${goal.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Dana terkumpul saat ini: ${CurrencyFormatter.formatRupiah(goal.currentAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val parsedAmount = CurrencyFormatter.parseAmount(amountDigits)
            OutlinedTextField(
                value = amountDigits,
                onValueChange = { raw ->
                    amountDigits = raw.filter { it.isDigit() }.take(13)
                    errorMsg = null
                },
                label = { Text("Nominal (Rp)") },
                prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                supportingText = {
                    if (parsedAmount > 0L) {
                        Text(CurrencyFormatter.formatRupiah(parsedAmount), style = TabularAmountStyle)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("allocation_amount_input"),
            )

            Text(
                text = if (isDeposit) "Ambil dari Dompet" else "Kembalikan ke Dompet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                wallets.forEach { w ->
                    val selected = selectedWalletId == w.id
                    FilterChip(
                        selected = selected,
                        onClick = { selectedWalletId = w.id },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(w.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = CurrencyFormatter.formatCompactRupiah(w.currentBalance),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    when {
                        wallets.isEmpty() -> errorMsg = "Tidak ada dompet tersedia."
                        parsedAmount <= 0L -> errorMsg = "Nominal harus lebih dari Rp 0."
                        !isDeposit && parsedAmount > goal.currentAmount ->
                            errorMsg = "Nominal penarikan melebihi dana terkumpul."
                        else -> {
                            onConfirm(selectedWalletId, parsedAmount, note)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_allocation_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (isDeposit) "Alokasikan Dana" else "Tarik Dana",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
