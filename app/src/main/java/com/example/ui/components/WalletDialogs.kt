package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionItem
import com.example.domain.model.WalletType
import com.example.domain.model.WalletWithBalance
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.theme.WalletSwatches
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WalletFormSheet(
    editingWallet: WalletWithBalance?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: WalletType, initialBalance: Long, colorIndex: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(editingWallet?.name.orEmpty()) }
    var type by remember { mutableStateOf(editingWallet?.type ?: WalletType.BANK) }
    var initialDigits by remember {
        mutableStateOf(editingWallet?.initialBalance?.takeIf { it > 0L }?.toString().orEmpty())
    }
    var colorIndex by remember { mutableIntStateOf(editingWallet?.colorIndex ?: 0) }
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
                text = if (editingWallet == null) "Tambah Dompet Baru" else "Edit Dompet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMsg = null
                },
                label = { Text("Nama Dompet / Akun") },
                placeholder = { Text("Misal: BCA, Mandiri, GoPay, Tunai") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_name_input"),
            )

            Text(
                text = "Jenis Dompet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WalletType.entries.forEach { wType ->
                    FilterChip(
                        selected = type == wType,
                        onClick = { type = wType },
                        label = { Text(wType.labelId) },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIcons.iconForWalletType(wType),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }

            val parsedInitial = CurrencyFormatter.parseAmount(initialDigits)
            OutlinedTextField(
                value = initialDigits,
                onValueChange = { raw -> initialDigits = raw.filter { it.isDigit() }.take(13) },
                label = { Text("Saldo Awal (Rp)") },
                prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                supportingText = {
                    Text(
                        text = if (parsedInitial > 0L) CurrencyFormatter.formatRupiah(parsedInitial) else "Opsional, default Rp 0",
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_initial_balance_input"),
            )

            Text(
                text = "Warna Kartu",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WalletSwatches.forEachIndexed { idx, color ->
                    val selected = colorIndex == idx
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { colorIndex = idx },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Dipilih",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
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
                    if (name.trim().isEmpty()) {
                        errorMsg = "Nama dompet wajib diisi."
                    } else {
                        onSave(editingWallet?.id ?: 0L, name, type, parsedInitial, colorIndex)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_wallet_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (editingWallet == null) "Simpan Dompet" else "Perbarui Dompet",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailSheet(
    wallet: WalletWithBalance,
    walletTransactions: List<TransactionItem>,
    onDismiss: () -> Unit,
    onEditWallet: () -> Unit,
    onDeleteWallet: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CategoryIcons.walletColor(wallet.colorIndex).copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CategoryIcons.iconForWalletType(wallet.type),
                                contentDescription = null,
                                tint = CategoryIcons.walletColor(wallet.colorIndex),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = wallet.type.labelId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditWallet) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Dompet")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus Dompet",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Saldo Saat Ini",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(wallet.currentBalance),
                        style = TabularAmountStyle.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Masuk (Siklus Ini)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatRupiah(wallet.periodIncome),
                                style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                                color = financeColors.income,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Keluar (Siklus Ini)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatRupiah(wallet.periodExpense),
                                style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                                color = financeColors.expense,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Riwayat Dompet (${walletTransactions.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (walletTransactions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Belum ada transaksi pada siklus ini untuk dompet ${wallet.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(walletTransactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = { onEditTransaction(tx) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Dompet ${wallet.name}?") },
            text = {
                Text("Dompet hanya bisa dihapus jika belum memiliki riwayat transaksi.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWallet()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
