package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.SavingsGoalItem
import com.example.ui.DompetKuUiState
import com.example.ui.components.CategoryIcons
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import com.example.util.CycleDateHelper

@Composable
fun SavingsScreen(
    state: DompetKuUiState,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoalItem) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAllocateToGoal: (SavingsGoalItem) -> Unit,
    onWithdrawFromGoal: (SavingsGoalItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var goalToDelete by remember { mutableStateOf<SavingsGoalItem?>(null) }

    val totalTarget = state.savingsGoals.sumOf { it.targetAmount }
    val totalCollected = state.totalAllocatedSavings
    val overallProgress = if (totalTarget > 0L) (totalCollected.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f

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
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Total Dana Target Terkumpul",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalCollected),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            ) {
                                Text(
                                    text = "${(overallProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }

                        if (totalTarget > 0L) {
                            LinearProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                strokeCap = StrokeCap.Round,
                            )
                            Text(
                                text = "Dari total target ${CurrencyFormatter.formatRupiah(totalTarget)} (${state.savingsGoals.size} target)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onAddGoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_savings_goal_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Target Tabungan Baru")
                }
            }

            if (state.savingsGoals.isEmpty()) {
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
                                imageVector = Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(42.dp),
                            )
                            Text(
                                text = "Belum ada target tabungan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Buat target untuk dana darurat, gadget, atau liburan dan alokasikan dana langsung dari dompet Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(state.savingsGoals, key = { it.id }) { goal ->
                    val accent = CategoryIcons.walletColor(goal.colorIndex)
                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0L)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_card_${goal.id}"),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Flag,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val subtitle = buildString {
                                        if (goal.deadlineMillis != null) {
                                            append("Tenggat: ${CycleDateHelper.formatShortDate(goal.deadlineMillis)}")
                                        } else {
                                            append("Tanpa tenggat waktu")
                                        }
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accent.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = "${goal.progressPercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                IconButton(onClick = { onEditGoal(goal) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Target", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { goalToDelete = goal }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Hapus Target",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { goal.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatRupiah(goal.currentAmount),
                                    style = TabularAmountStyle.copy(
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "Target: ${CurrencyFormatter.formatRupiah(goal.targetAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (remaining > 0L) {
                                Text(
                                    text = "Kurang ${CurrencyFormatter.formatRupiah(remaining)} lagi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "Target tercapai!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = { onAllocateToGoal(goal) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("allocate_goal_${goal.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alokasi")
                                }
                                OutlinedButton(
                                    onClick = { onWithdrawFromGoal(goal) },
                                    enabled = goal.currentAmount > 0L,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("withdraw_goal_${goal.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tarik")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (goalToDelete != null) {
        val target = goalToDelete!!
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Hapus Target ${target.name}?") },
            text = {
                Text(
                    if (target.currentAmount > 0L) {
                        "Dana teralokasi sebesar ${CurrencyFormatter.formatRupiah(target.currentAmount)} akan dikembalikan ke dompet asal."
                    } else {
                        "Target tabungan ini akan dihapus permanen."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGoal(target.id)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Batal")
                }
            },
        )
    }
}
