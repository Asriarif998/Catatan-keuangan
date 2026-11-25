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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.WalletWithBalance
import com.example.ui.DompetKuUiState
import com.example.ui.components.CategoryIcons
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter

@Composable
fun WalletsScreen(
    state: DompetKuUiState,
    onAddWallet: () -> Unit,
    onTransferBetweenWallets: () -> Unit,
    onSelectWallet: (WalletWithBalance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val totalWealth = state.totalWalletBalance + state.totalAllocatedSavings

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Total Saldo Tersedia (${state.wallets.size} Dompet)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(state.totalWalletBalance),
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (state.totalAllocatedSavings > 0L) {
                            Text(
                                text = "Teralokasi di Target: ${CurrencyFormatter.formatRupiah(state.totalAllocatedSavings)} • Total Kekayaan: ${CurrencyFormatter.formatRupiah(totalWealth)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onAddWallet,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_wallet_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Dompet")
                    }
                    OutlinedButton(
                        onClick = onTransferBetweenWallets,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("transfer_wallet_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer")
                    }
                }
            }

            items(state.wallets, key = { it.id }) { wallet ->
                val accent = CategoryIcons.walletColor(wallet.colorIndex)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectWallet(wallet) }
                        .testTag("wallet_card_${wallet.id}"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.iconForWalletType(wallet.type),
                                    contentDescription = wallet.name,
                                    tint = accent,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = wallet.type.labelId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatRupiah(wallet.currentBalance),
                                style = TabularAmountStyle.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (wallet.currentBalance < 0L) financeColors.expense else MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = financeColors.incomeContainer,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "+${CurrencyFormatter.formatCompactRupiah(wallet.periodIncome)}",
                                    style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                                    color = financeColors.income,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = financeColors.expenseContainer,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "-${CurrencyFormatter.formatCompactRupiah(wallet.periodExpense)}",
                                    style = TabularAmountStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                                    color = financeColors.expense,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
