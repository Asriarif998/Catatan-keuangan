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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionType
import com.example.ui.DompetKuUiState
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CycleHeaderBar
import com.example.ui.components.DailyBarChart
import com.example.ui.components.SpendingTrendsLineChart
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    state: DompetKuUiState,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onResetCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var chartTab by remember { mutableIntStateOf(0) } // 0 = Kategori, 1 = Harian, 2 = Tren 6 Bln
    var selectedSliceIndex by remember(selectedType, state.currentCycle) { mutableStateOf<Int?>(null) }
    var selectedTrendIndex by remember(state.spendingTrends) {
        mutableIntStateOf((state.spendingTrends.size - 1).coerceAtLeast(0))
    }

    val breakdown = if (selectedType == TransactionType.EXPENSE) {
        state.expenseByCategory
    } else {
        state.incomeByCategory
    }
    val totalAmount = if (selectedType == TransactionType.EXPENSE) {
        state.periodExpense
    } else {
        state.periodIncome
    }
    val dailyPoints = if (selectedType == TransactionType.EXPENSE) {
        state.dailyExpenses
    } else {
        state.dailyIncomes
    }
    val accentColor = if (selectedType == TransactionType.EXPENSE) {
        financeColors.expense
    } else {
        financeColors.income
    }

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
                CycleHeaderBar(
                    cycle = state.currentCycle,
                    cycleStartDay = state.cycleStartDay,
                    onPrevious = onPreviousCycle,
                    onNext = onNextCycle,
                    onResetToCurrent = onResetCycle,
                )
            }

            item {
                val metricOptions = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    metricOptions.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(idx, metricOptions.size),
                        ) {
                            Text(type.labelId)
                        }
                    }
                }
            }

            item {
                val chartModes = listOf(
                    "Kategori" to Icons.Rounded.PieChart,
                    "Harian" to Icons.Rounded.BarChart,
                    "Tren 6 Bln" to Icons.AutoMirrored.Rounded.ShowChart,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    chartModes.forEachIndexed { idx, (label, icon) ->
                        SegmentedButton(
                            selected = chartTab == idx,
                            onClick = { chartTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, chartModes.size),
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("analytics_tab_$idx"),
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (chartTab == 2) {
                val trendPoints = state.spendingTrends
                val showExpense = selectedType == TransactionType.EXPENSE
                val trendValues = trendPoints.map { if (showExpense) it.totalExpense else it.totalIncome }
                val totalSixMonths = trendValues.sum()
                val avgSixMonths = if (trendValues.isEmpty()) 0L else totalSixMonths / trendValues.size
                val currentMonthVal = trendValues.lastOrNull() ?: 0L
                val prevMonthVal = trendValues.getOrNull(trendValues.lastIndex - 1) ?: 0L
                val momDeltaPercent = if (prevMonthVal > 0L) {
                    (((currentMonthVal - prevMonthVal).toFloat() / prevMonthVal.toFloat()) * 100f).roundToInt()
                } else {
                    null
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Total 6 Siklus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatCompactRupiah(totalSixMonths),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "Rata-rata ${CurrencyFormatter.formatCompactRupiah(avgSixMonths)}/bln",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "vs Siklus Sebelumnya",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (momDeltaPercent == null) {
                                    Text(
                                        text = if (currentMonthVal > 0L) "Baru" else "0%",
                                        style = TabularAmountStyle.copy(
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Belum ada pembanding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    val isIncrease = momDeltaPercent > 0
                                    val deltaColor = when {
                                        momDeltaPercent == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                        showExpense && isIncrease -> financeColors.expense
                                        showExpense && !isIncrease -> financeColors.income
                                        !showExpense && isIncrease -> financeColors.income
                                        else -> financeColors.expense
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (momDeltaPercent != 0) {
                                            Icon(
                                                imageVector = if (isIncrease) {
                                                    Icons.AutoMirrored.Rounded.TrendingUp
                                                } else {
                                                    Icons.AutoMirrored.Rounded.TrendingDown
                                                },
                                                contentDescription = null,
                                                tint = deltaColor,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                        Text(
                                            text = if (isIncrease) "+$momDeltaPercent%" else "$momDeltaPercent%",
                                            style = TabularAmountStyle.copy(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = deltaColor,
                                        )
                                    }
                                    Text(
                                        text = if (isIncrease) "Naik dari bulan lalu" else if (momDeltaPercent < 0) "Turun dari bulan lalu" else "Sama dengan bulan lalu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = if (showExpense) "Spending Trends (6 Bulan)" else "Tren Pemasukan (6 Bulan)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Geser atau ketuk titik bulan untuk detail",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            SpendingTrendsLineChart(
                                points = trendPoints,
                                showExpense = showExpense,
                                lineColor = accentColor,
                                selectedIndex = selectedTrendIndex.coerceIn(0, (trendPoints.size - 1).coerceAtLeast(0)),
                                onSelectIndex = { selectedTrendIndex = it },
                            )
                        }
                    }
                }

                // Per-month comparison breakdown list (newest first)
                val maxTrendAmount = (trendValues.maxOrNull() ?: 0L).coerceAtLeast(1L)
                itemsIndexed(trendPoints.reversed(), key = { _, pt -> pt.startMillis }) { revIdx, pt ->
                    val origIdx = trendPoints.lastIndex - revIdx
                    val amount = if (showExpense) pt.totalExpense else pt.totalIncome
                    val prevAmount = trendPoints.getOrNull(origIdx - 1)?.let {
                        if (showExpense) it.totalExpense else it.totalIncome
                    }
                    val pctChange = if (prevAmount != null && prevAmount > 0L) {
                        (((amount - prevAmount).toFloat() / prevAmount.toFloat()) * 100f).roundToInt()
                    } else {
                        null
                    }
                    val isSelected = origIdx == selectedTrendIndex

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTrendIndex = origIdx },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pt.cycleLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val deltaText = when {
                                        pctChange == null -> "Periode awal / pembanding Rp 0"
                                        pctChange > 0 -> "+$pctChange% dibanding siklus sebelumnya"
                                        pctChange < 0 -> "-${abs(pctChange)}% dibanding siklus sebelumnya"
                                        else -> "Stabil dibanding siklus sebelumnya"
                                    }
                                    Text(
                                        text = deltaText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatRupiah(amount),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (amount > 0L) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (amount.toFloat() / maxTrendAmount.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }
                }
            } else if (totalAmount == 0L) {
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (chartTab == 0) Icons.Rounded.PieChart else Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp),
                            )
                            Text(
                                text = "Belum ada data ${selectedType.labelId.lowercase()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Catat transaksi pada siklus ${state.currentCycle.label} untuk melihat grafik visualisasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (chartTab == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CategoryDonutChart(
                                items = breakdown,
                                totalAmount = totalAmount,
                                selectedIndex = selectedSliceIndex,
                                onSelectIndex = { selectedSliceIndex = it },
                            )
                            Text(
                                text = "Ketuk bagian grafik atau daftar di bawah untuk detail",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                itemsIndexed(breakdown, key = { _, item -> item.category }) { idx, item ->
                    val sliceColor = ChartPalette[item.colorIndex % ChartPalette.size]
                    val isSelected = selectedSliceIndex == idx
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedSliceIndex = if (isSelected) null else idx
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) sliceColor else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(sliceColor),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${item.count} transaksi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.amount),
                                        style = TabularAmountStyle.copy(
                                            fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Text(
                                        text = "${(item.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = sliceColor,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { item.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = sliceColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }
                }
            } else {
                item {
                    val activeDaysCount = dailyPoints.count { it.amount > 0L }.coerceAtLeast(1)
                    val avgPerDay = totalAmount / dailyPoints.size.coerceAtLeast(1)
                    val peakDay = dailyPoints.maxByOrNull { it.amount }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = "Tren Harian (${dailyPoints.size} Hari)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            DailyBarChart(
                                points = dailyPoints,
                                accentColor = accentColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Rata-rata / Hari",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatCompactRupiah(avgPerDay),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "$activeDaysCount hari aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Hari Tertinggi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatCompactRupiah(peakDay?.amount ?: 0L),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = accentColor,
                                )
                                Text(
                                    text = peakDay?.fullDateLabel ?: "-",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
