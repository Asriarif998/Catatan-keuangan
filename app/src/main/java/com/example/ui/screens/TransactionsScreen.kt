package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.ui.DompetKuUiState
import com.example.ui.components.CycleHeaderBar
import com.example.ui.components.TransactionRow
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import com.example.util.CycleDateHelper

@Composable
fun TransactionsScreen(
    state: DompetKuUiState,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onResetCycle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onWalletFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var walletMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val availableCategoryNames = remember(state.cycleTransactions, state.categories) {
        (state.categories.map { it.name } + state.cycleTransactions.map { it.category })
            .distinct()
            .sorted()
    }

    val groupedByDay = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { CycleDateHelper.startOfDayMillis(it.timestamp) }
    }

    val hasActiveFilters = state.filters.searchQuery.isNotBlank() ||
        state.filters.typeFilter != null ||
        state.filters.walletIdFilter != null ||
        state.filters.categoryFilter != null

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CycleHeaderBar(
                    cycle = state.currentCycle,
                    cycleStartDay = state.cycleStartDay,
                    onPrevious = onPreviousCycle,
                    onNext = onNextCycle,
                    onResetToCurrent = onResetCycle,
                )
            }

            item {
                OutlinedTextField(
                    value = state.filters.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari kategori, catatan, atau dompet...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Cari") },
                    trailingIcon = {
                        if (state.filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_search_input"),
                )
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.filters.typeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text("Semua") },
                    )
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = state.filters.typeFilter == type,
                            onClick = {
                                onTypeFilterChange(if (state.filters.typeFilter == type) null else type)
                            },
                            label = { Text(type.labelId) },
                        )
                    }

                    // Wallet Dropdown Filter
                    Box {
                        val selectedWalletName = state.wallets.firstOrNull { it.id == state.filters.walletIdFilter }?.name
                        FilterChip(
                            selected = state.filters.walletIdFilter != null,
                            onClick = { walletMenuExpanded = true },
                            label = { Text(selectedWalletName ?: "Dompet") },
                            leadingIcon = {
                                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                        DropdownMenu(
                            expanded = walletMenuExpanded,
                            onDismissRequest = { walletMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Semua Dompet") },
                                onClick = {
                                    onWalletFilterChange(null)
                                    walletMenuExpanded = false
                                },
                            )
                            state.wallets.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = {
                                        onWalletFilterChange(w.id)
                                        walletMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Category Dropdown Filter
                    Box {
                        FilterChip(
                            selected = state.filters.categoryFilter != null,
                            onClick = { categoryMenuExpanded = true },
                            label = { Text(state.filters.categoryFilter ?: "Kategori") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Semua Kategori") },
                                onClick = {
                                    onCategoryFilterChange(null)
                                    categoryMenuExpanded = false
                                },
                            )
                            availableCategoryNames.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        onCategoryFilterChange(cat)
                                        categoryMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (hasActiveFilters) {
                        AssistChip(
                            onClick = onClearFilters,
                            label = { Text("Reset") },
                            leadingIcon = {
                                Icon(Icons.Rounded.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                }
            }

            if (groupedByDay.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = if (hasActiveFilters) "Tidak ada transaksi yang cocok" else "Belum ada transaksi di periode ini",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (hasActiveFilters) "Coba ubah kata kunci atau filter yang dipilih." else "Mulai catat pemasukan, pengeluaran, atau transfer antar dompet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                groupedByDay.forEach { (dayStart, dayTransactions) ->
                    val dayIncome = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val dayExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val dayNet = dayIncome - dayExpense

                    item(key = "header_$dayStart") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = CycleDateHelper.relativeDateHeader(dayStart),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (dayIncome > 0L || dayExpense > 0L) {
                                Text(
                                    text = CurrencyFormatter.formatRupiah(dayNet, showSign = true),
                                    style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    items(dayTransactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = { onEditTransaction(tx) },
                        )
                    }
                }
            }
        }
    }
}
