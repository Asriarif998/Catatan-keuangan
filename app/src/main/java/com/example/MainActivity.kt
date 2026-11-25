package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletWithBalance
import com.example.ui.MainViewModel
import com.example.ui.components.SavingsAllocationSheet
import com.example.ui.components.SavingsGoalFormSheet
import com.example.ui.components.TransactionFormSheet
import com.example.ui.components.WalletDetailSheet
import com.example.ui.components.WalletFormSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.theme.DompetKuTheme
import com.example.util.CsvParseResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val WALLETS = "wallets"
    const val ANALYTICS = "analytics"
    const val SAVINGS = "savings"
    const val SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopLevelTabs = listOf(
    NavTab(Routes.DASHBOARD, "Ringkasan", Icons.Rounded.Home, Icons.Outlined.Home),
    NavTab(Routes.TRANSACTIONS, "Transaksi", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
    NavTab(Routes.WALLETS, "Dompet", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavTab(Routes.ANALYTICS, "Grafik", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    NavTab(Routes.SAVINGS, "Target", Icons.Rounded.Savings, Icons.Outlined.Savings),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DompetKuApplication
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.repository, app.preferencesManager),
            )
            DompetKuApp(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DompetKuApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Modal sheet states
    var showTxSheet by remember { mutableStateOf(false) }
    var txInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    var showWalletForm by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletWithBalance?>(null) }
    var inspectingWalletId by remember { mutableStateOf<Long?>(null) }

    var showSavingsForm by remember { mutableStateOf(false) }
    var editingSavingsGoal by remember { mutableStateOf<SavingsGoalItem?>(null) }
    var allocationTarget by remember { mutableStateOf<Pair<SavingsGoalItem, Boolean>?>(null) }

    var pendingCsvImport by remember { mutableStateOf<CsvParseResult?>(null) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = withContext(Dispatchers.IO) { viewModel.buildExportCsv() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("Tidak dapat membuka file tujuan")
                    }
                }.onSuccess {
                    viewModel.emitMessage("Backup CSV berhasil diekspor")
                }.onFailure { err ->
                    viewModel.emitMessage("Gagal ekspor CSV: ${err.message}")
                }
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: error("Tidak dapat membaca file CSV")
                    }
                    viewModel.parseCsvPreview(raw)
                }.onSuccess { result ->
                    if (result.items.isEmpty()) {
                        viewModel.emitMessage("File CSV tidak berisi transaksi yang valid")
                    } else {
                        pendingCsvImport = result
                    }
                }.onFailure { err ->
                    viewModel.emitMessage("Gagal membaca CSV: ${err.message}")
                }
            }
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    DompetKuTheme(themeMode = state.themeMode) {
        val isSettingsScreen = currentRoute == Routes.SETTINGS

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!isSettingsScreen) {
                        val title = TopLevelTabs.firstOrNull { it.route == currentRoute }?.let {
                            if (it.route == Routes.DASHBOARD) "DompetKu" else it.label
                        } ?: "DompetKu"

                        TopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            actions = {
                                Box {
                                    val themeIcon = when (state.themeMode) {
                                        ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                    }
                                    IconButton(
                                        onClick = { showThemeMenu = true },
                                        modifier = Modifier.testTag("top_theme_toggle"),
                                    ) {
                                        Icon(
                                            imageVector = themeIcon,
                                            contentDescription = "Ubah Tema (${state.themeMode.labelId})",
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showThemeMenu,
                                        onDismissRequest = { showThemeMenu = false },
                                    ) {
                                        ThemeMode.entries.forEach { mode ->
                                            val icon = when (mode) {
                                                ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                            }
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (state.themeMode == mode) "✓ Mode ${mode.labelId}" else "Mode ${mode.labelId}",
                                                        fontWeight = if (state.themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                },
                                                leadingIcon = { Icon(icon, contentDescription = null) },
                                                onClick = {
                                                    viewModel.setThemeMode(mode)
                                                    showThemeMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { navController.navigate(Routes.SETTINGS) },
                                    modifier = Modifier.testTag("open_settings_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Pengaturan",
                                    )
                                }
                            },
                        )
                    }
                },
                bottomBar = {
                    if (!isSettingsScreen && !useNavRail) {
                        NavigationBar {
                            TopLevelTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.route}"),
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isSettingsScreen && (currentRoute == Routes.DASHBOARD || currentRoute == Routes.TRANSACTIONS)) {
                        FloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                txInitialType = TransactionType.EXPENSE
                                showTxSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_transaction"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Tambah Transaksi")
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (!isSettingsScreen && useNavRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            TopLevelTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}"),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                state = state,
                                onPreviousCycle = viewModel::previousCycle,
                                onNextCycle = viewModel::nextCycle,
                                onResetCycle = viewModel::resetToActiveCycle,
                                onOpenTransactionForm = { type ->
                                    editingTransaction = null
                                    txInitialType = type
                                    showTxSheet = true
                                },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    txInitialType = tx.type
                                    showTxSheet = true
                                },
                                onOpenAllTransactions = { navigateToTab(Routes.TRANSACTIONS) },
                                onOpenWallets = { navigateToTab(Routes.WALLETS) },
                                onOpenWalletDetail = { wallet -> inspectingWalletId = wallet.id },
                                onOpenAnalytics = { navigateToTab(Routes.ANALYTICS) },
                            )
                        }

                        composable(Routes.TRANSACTIONS) {
                            TransactionsScreen(
                                state = state,
                                onPreviousCycle = viewModel::previousCycle,
                                onNextCycle = viewModel::nextCycle,
                                onResetCycle = viewModel::resetToActiveCycle,
                                onSearchChange = viewModel::updateSearchQuery,
                                onTypeFilterChange = viewModel::updateTypeFilter,
                                onWalletFilterChange = viewModel::updateWalletFilter,
                                onCategoryFilterChange = viewModel::updateCategoryFilter,
                                onClearFilters = viewModel::clearFilters,
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    txInitialType = tx.type
                                    showTxSheet = true
                                },
                            )
                        }

                        composable(Routes.WALLETS) {
                            WalletsScreen(
                                state = state,
                                onAddWallet = {
                                    editingWallet = null
                                    showWalletForm = true
                                },
                                onTransferBetweenWallets = {
                                    editingTransaction = null
                                    txInitialType = TransactionType.TRANSFER
                                    showTxSheet = true
                                },
                                onSelectWallet = { wallet -> inspectingWalletId = wallet.id },
                            )
                        }

                        composable(Routes.ANALYTICS) {
                            AnalyticsScreen(
                                state = state,
                                onPreviousCycle = viewModel::previousCycle,
                                onNextCycle = viewModel::nextCycle,
                                onResetCycle = viewModel::resetToActiveCycle,
                            )
                        }

                        composable(Routes.SAVINGS) {
                            SavingsScreen(
                                state = state,
                                onAddGoal = {
                                    editingSavingsGoal = null
                                    showSavingsForm = true
                                },
                                onEditGoal = { goal ->
                                    editingSavingsGoal = goal
                                    showSavingsForm = true
                                },
                                onDeleteGoal = viewModel::deleteSavingsGoal,
                                onAllocateToGoal = { goal -> allocationTarget = goal to true },
                                onWithdrawFromGoal = { goal -> allocationTarget = goal to false },
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSelectTheme = viewModel::setThemeMode,
                                onSelectCycleStartDay = viewModel::setCycleStartDay,
                                onExportCsv = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    exportCsvLauncher.launch("dompetku_backup_$stamp.csv")
                                },
                                onImportCsv = {
                                    importCsvLauncher.launch(
                                        arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain"),
                                    )
                                },
                                onAddCategory = viewModel::addCategory,
                                onDeleteCategory = viewModel::deleteCategory,
                            )
                        }
                    }
                }
            }
        }

        // --- Bottom Sheets & Dialogs ---
        if (showTxSheet) {
            TransactionFormSheet(
                editingTransaction = editingTransaction,
                initialType = txInitialType,
                wallets = state.wallets,
                categories = state.categories,
                onDismiss = {
                    showTxSheet = false
                    editingTransaction = null
                },
                onSave = { id, type, amount, walletId, targetWalletId, category, note, timestamp ->
                    viewModel.saveTransaction(id, type, amount, walletId, targetWalletId, category, note, timestamp)
                },
                onDelete = { id ->
                    viewModel.deleteTransaction(id)
                },
            )
        }

        if (showWalletForm) {
            WalletFormSheet(
                editingWallet = editingWallet,
                onDismiss = {
                    showWalletForm = false
                    editingWallet = null
                },
                onSave = { id, name, type, initialBalance, colorIndex ->
                    viewModel.saveWallet(id, name, type, initialBalance, colorIndex)
                },
            )
        }

        val inspectedWallet = inspectingWalletId?.let { id -> state.wallets.firstOrNull { it.id == id } }
        if (inspectedWallet != null) {
            val walletTxs = state.cycleTransactions.filter {
                it.walletId == inspectedWallet.id || it.targetWalletId == inspectedWallet.id
            }
            WalletDetailSheet(
                wallet = inspectedWallet,
                walletTransactions = walletTxs,
                onDismiss = { inspectingWalletId = null },
                onEditWallet = {
                    editingWallet = inspectedWallet
                    inspectingWalletId = null
                    showWalletForm = true
                },
                onDeleteWallet = {
                    viewModel.deleteWallet(inspectedWallet.id)
                },
                onEditTransaction = { tx ->
                    inspectingWalletId = null
                    editingTransaction = tx
                    txInitialType = tx.type
                    showTxSheet = true
                },
            )
        }

        if (showSavingsForm) {
            SavingsGoalFormSheet(
                editingGoal = editingSavingsGoal,
                wallets = state.wallets,
                onDismiss = {
                    showSavingsForm = false
                    editingSavingsGoal = null
                },
                onSave = { id, name, targetAmount, deadlineMillis, linkedWalletId, colorIndex ->
                    viewModel.saveSavingsGoal(id, name, targetAmount, deadlineMillis, linkedWalletId, colorIndex)
                },
            )
        }

        allocationTarget?.let { (goal, isDeposit) ->
            SavingsAllocationSheet(
                goal = goal,
                isDeposit = isDeposit,
                wallets = state.wallets,
                onDismiss = { allocationTarget = null },
                onConfirm = { walletId, amount, note ->
                    if (isDeposit) {
                        viewModel.allocateToSavingsGoal(goal.id, walletId, amount, note)
                    } else {
                        viewModel.withdrawFromSavingsGoal(goal.id, walletId, amount, note)
                    }
                },
            )
        }

        pendingCsvImport?.let { result ->
            AlertDialog(
                onDismissRequest = { pendingCsvImport = null },
                title = { Text("Konfirmasi Impor CSV") },
                text = {
                    val skippedMsg = if (result.skippedRows > 0) {
                        " (${result.skippedRows} baris dilewati)"
                    } else {
                        ""
                    }
                    Text(
                        "Ditemukan ${result.items.size} transaksi valid$skippedMsg. " +
                            "Pilih apakah ingin menggabungkan dengan transaksi saat ini atau mengganti seluruh riwayat transaksi.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.importCsvTransactions(result.items, replaceExisting = false)
                            pendingCsvImport = null
                        },
                    ) {
                        Text("Gabungkan")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.importCsvTransactions(result.items, replaceExisting = true)
                            pendingCsvImport = null
                        },
                    ) {
                        Text("Ganti Semua")
                    }
                },
            )
        }
    }
}
