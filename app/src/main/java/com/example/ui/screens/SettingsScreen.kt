package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionType
import com.example.ui.DompetKuUiState
import com.example.util.CycleDateHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: DompetKuUiState,
    onBack: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectCycleStartDay: (Int) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onAddCategory: (String, TransactionType) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
) {
    BackHandler(onBack = onBack)

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Theme Mode
                item {
                    SettingsSectionCard(title = "Tampilan & Mode Gelap") {
                        Text(
                            text = "Pilih tampilan terang, gelap, atau ikuti pengaturan sistem perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val modes = ThemeMode.entries
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = state.themeMode == mode,
                                    onClick = { onSelectTheme(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                                    icon = {
                                        val icon = when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                                        }
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}"),
                                ) {
                                    Text(mode.labelId, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 2. Custom Monthly Cycle
                item {
                    val preview = CycleDateHelper.getCyclePeriod(state.cycleStartDay, 0)
                    SettingsSectionCard(title = "Siklus Periode Bulanan") {
                        Text(
                            text = "Tentukan tanggal awal gajian / siklus bulanan Anda. Periode aktif saat ini: ${preview.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Tanggal Mulai Siklus",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        if (state.cycleStartDay > 1) onSelectCycleStartDay(state.cycleStartDay - 1)
                                    },
                                    modifier = Modifier.testTag("cycle_day_minus"),
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Kurangi tanggal")
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = "Tgl ${state.cycleStartDay}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (state.cycleStartDay < 31) onSelectCycleStartDay(state.cycleStartDay + 1)
                                    },
                                    modifier = Modifier.testTag("cycle_day_plus"),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Tambah tanggal")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { onSelectCycleStartDay(26) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Default (26 – 25)", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onSelectCycleStartDay(1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Tgl 1 – Akhir", maxLines = 1)
                            }
                        }
                    }
                }

                // 3. CSV Backup & Restore
                item {
                    SettingsSectionCard(title = "Backup & Restore CSV") {
                        Text(
                            text = "Ekspor seluruh riwayat transaksi ke file .csv atau impor kembali dari penyimpanan perangkat melalui Storage Access Framework (tanpa perlu izin penyimpanan tambahan).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onExportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_csv_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor CSV")
                            }
                            OutlinedButton(
                                onClick = onImportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("import_csv_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Impor CSV")
                            }
                        }
                    }
                }

                // 4. Custom Categories
                item {
                    SettingsSectionCard(title = "Kelola Kategori") {
                        val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            types.forEachIndexed { idx, t ->
                                SegmentedButton(
                                    selected = newCategoryType == t,
                                    onClick = { newCategoryType = t },
                                    shape = SegmentedButtonDefaults.itemShape(idx, types.size),
                                ) {
                                    Text(t.labelId)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("Kategori Baru") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        onAddCategory(newCategoryName, newCategoryType)
                                        newCategoryName = ""
                                    }
                                },
                                modifier = Modifier.height(52.dp),
                            ) {
                                Text("Tambah")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            state.categories
                                .filter { it.type == newCategoryType }
                                .forEach { cat ->
                                    InputChip(
                                        selected = false,
                                        onClick = { if (!cat.isDefault) onDeleteCategory(cat) },
                                        label = { Text(cat.name) },
                                        trailingIcon = if (!cat.isDefault) {
                                            {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "Hapus kategori ${cat.name}",
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
