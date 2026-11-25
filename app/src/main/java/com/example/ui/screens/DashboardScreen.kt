package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletWithBalance
import com.example.ui.DompetKuUiState
import com.example.ui.components.CategoryIcons
import com.example.ui.components.CycleHeaderBar
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter

@Composable
fun DashboardScreen(
    state: DompetKuUiState,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onResetCycle: () -> Unit,
    onOpenTransactionForm: (TransactionType) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenWalletDetail: (WalletWithBalance) -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

            // Hero Summary Card
            item {
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F766E),
                        Color(0xFF0E5E6F),
                        Color(0xFF134E4A),
                    ),
                )
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .background(gradient)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column {
                                Text(
                                    text = "Total Saldo Dompet",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(state.totalWalletBalance),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.16f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    Text(
                                        text = "Arus Kas Bersih",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(state.netCashflow, showSign = true),
                                        style = TabularAmountStyle.copy(
                                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = if (state.netCashflow >= 0L) Color(0xFF99F6E4) else Color(0xFFFECACA),
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HeroMetricPill(
                                label = "Pemasukan",
                                amount = state.periodIncome,
                                icon = Icons.Rounded.ArrowDownward,
                                iconTint = Color(0xFF86EFAC),
                                modifier = Modifier.weight(1f),
                            )
                            HeroMetricPill(
                                label = "Pengeluaran",
                                amount = state.periodExpense,
                                icon = Icons.Rounded.ArrowUpward,
                                iconTint = Color(0xFFFCA5A5),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Quick Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { onOpenTransactionForm(TransactionType.EXPENSE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_add_expense"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Catat", maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { onOpenTransactionForm(TransactionType.TRANSFER) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_transfer_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Wallets Section
            item {
                SectionHeader(
                    title = "Dompet Saya",
                    actionLabel = "Kelola",
                    onAction = onOpenWallets,
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(state.wallets, key = { it.id }) { wallet ->
                        val accent = CategoryIcons.walletColor(wallet.colorIndex)
                        Surface(
                            modifier = Modifier
                                .width(168.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenWalletDetail(wallet) }
                                .testTag("dashboard_wallet_card_${wallet.id}"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = CategoryIcons.iconForWalletType(wallet.type),
                                            contentDescription = wallet.name,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = wallet.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = wallet.type.labelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyFormatter.formatRupiah(wallet.currentBalance),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (wallet.currentBalance < 0L) financeColors.expense else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Top Categories Snapshot
            if (state.expenseByCategory.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Pengeluaran Terbesar",
                        actionLabel = "Lihat Grafik",
                        onAction = onOpenAnalytics,
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenAnalytics),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.expenseByCategory.take(3).forEach { cat ->
                                val barColor = ChartPalette[cat.colorIndex % ChartPalette.size]
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatRupiah(cat.amount)} (${(cat.percentage * 100).toInt()}%)",
                                            style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { cat.percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                SectionHeader(
                    title = "Transaksi Terbaru",
                    actionLabel = "Semua (${state.cycleTransactions.size})",
                    onAction = onOpenAllTransactions,
                )
            }

            if (state.cycleTransactions.isEmpty()) {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp),
                            )
                            Text(
                                text = "Belum ada transaksi di siklus ini",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Ketuk tombol + untuk mencatat pemasukan, pengeluaran, atau transfer antar dompet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(state.cycleTransactions.take(5), key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onClick = { onEditTransaction(tx) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    amount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.22f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    text = CurrencyFormatter.formatCompactRupiah(amount),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
