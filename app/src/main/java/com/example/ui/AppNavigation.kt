package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.signOutFromGoogle
import com.example.data.BudgetRepository
import com.example.data.ai.FinancialAdvisorRepository
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.remote.CloudSyncRepository
import com.example.domain.BudgetTransaction
import com.example.domain.TransactionType
import com.example.ui.screens.AuthSplashScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseEditorDialog
import com.example.ui.screens.IncomeEditorDialog
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MoreSubScreen
import com.example.ui.screens.PlanningHubScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsAndBudgetScreen
import com.example.ui.state.BudgetViewModel
import com.example.ui.theme.MasroufAlBaitTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class MainTab(
    val labelAr: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("الرئيسية", Icons.Rounded.Home, "nav_dashboard"),
    TRANSACTIONS("المصروفات", Icons.Rounded.ReceiptLong, "nav_transactions"),
    PLANNING("التخطيط", Icons.Rounded.Savings, "nav_planning"),
    REPORTS("التقارير", Icons.Rounded.Analytics, "nav_reports"),
    MORE("المزيد", Icons.Rounded.GridView, "nav_more")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val preferencesRepository = remember(context) { PreferencesRepository(context) }
    val prefs by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    MasroufAlBaitTheme(
        themeMode = prefs.themeMode,
        primaryArgb = prefs.primaryColorArgb,
        secondaryArgb = prefs.secondaryColorArgb,
        accentArgb = prefs.accentColorArgb
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (currentUser == null && !prefs.localOnlyMode) {
                AuthSplashScreen(
                    onGoogleAuthSuccess = { /* authStateFlow switches screen automatically */ },
                    onContinueLocal = {
                        scope.launch { preferencesRepository.setLocalOnlyMode(true) }
                    }
                )
            } else {
                val authKey = currentUser?.uid ?: "local"
                key(authKey) {
                    MasroufMainShell(
                        authKey = authKey,
                        currentUserId = currentUser?.uid,
                        currentUserName = currentUser?.displayName,
                        currentUserEmail = currentUser?.email,
                        preferencesRepository = preferencesRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun MasroufMainShell(
    authKey: String,
    currentUserId: String?,
    currentUserName: String?,
    currentUserEmail: String?,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(appContext) { CredentialManager.create(appContext) }

    val viewModel: BudgetViewModel = viewModel(
        key = "budget_vm_$authKey",
        factory = viewModelFactory {
            initializer {
                val db = AppDatabase.getInstance(appContext)
                val cloudSync = CloudSyncRepository(appContext)
                val advisor = FinancialAdvisorRepository()
                val repo = BudgetRepository(
                    dao = db.appDao(),
                    cloudSync = cloudSync,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail
                )
                BudgetViewModel(
                    repository = repo,
                    preferencesRepository = preferencesRepository,
                    advisorRepository = advisor,
                    isCloudMode = currentUserId != null,
                    appContext = appContext
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val banner = feedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(banner.message)
        viewModel.clearFeedback()
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var planningSubTab by rememberSaveable { mutableIntStateOf(0) }
    var moreSubScreen by rememberSaveable { mutableStateOf(MoreSubScreen.MENU) }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<BudgetTransaction?>(null) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<BudgetTransaction?>(null) }

    BackHandler(
        enabled = currentTab != MainTab.DASHBOARD &&
            !(currentTab == MainTab.MORE && moreSubScreen != MoreSubScreen.MENU)
    ) {
        currentTab = MainTab.DASHBOARD
    }

    if (showExpenseDialog) {
        ExpenseEditorDialog(
            initial = editingExpense,
            categories = uiState.categories,
            currencyCode = uiState.preferences.currencyCode,
            activeMemberName = uiState.preferences.activeMemberName,
            onDismiss = {
                showExpenseDialog = false
                editingExpense = null
            },
            onSave = { existingId, amountText, category, merchant, notes, dateMillis, receiptUri ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    type = TransactionType.EXPENSE,
                    amountText = amountText,
                    categoryId = category.id,
                    categoryName = category.name,
                    incomeSource = null,
                    merchant = merchant,
                    notes = notes,
                    dateEpochMillis = dateMillis,
                    receiptUri = receiptUri
                )
                showExpenseDialog = false
                editingExpense = null
            }
        )
    }

    if (showIncomeDialog) {
        IncomeEditorDialog(
            initial = editingIncome,
            currencyCode = uiState.preferences.currencyCode,
            onDismiss = {
                showIncomeDialog = false
                editingIncome = null
            },
            onSave = { existingId, amountText, source, notes, dateMillis ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    type = TransactionType.INCOME,
                    amountText = amountText,
                    categoryId = "income_${source.name.lowercase()}",
                    categoryName = source.labelAr,
                    incomeSource = source,
                    merchant = null,
                    notes = notes,
                    dateEpochMillis = dateMillis,
                    receiptUri = null
                )
                showIncomeDialog = false
                editingIncome = null
            }
        )
    }

    val openTransactionEditor: (BudgetTransaction) -> Unit = { tx ->
        if (tx.type == TransactionType.EXPENSE) {
            editingExpense = tx
            showExpenseDialog = true
        } else {
            editingIncome = tx
            showIncomeDialog = true
        }
    }

    val showFab = !(currentTab == MainTab.MORE &&
        (moreSubScreen == MoreSubScreen.AI_ADVISOR || moreSubScreen == MoreSubScreen.THEME_WHEEL))

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
            when (currentTab) {
                MainTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    isOnline = isOnline,
                    isCloudMode = currentUserId != null,
                    onAddExpenseClick = {
                        editingExpense = null
                        showExpenseDialog = true
                    },
                    onAddIncomeClick = {
                        editingIncome = null
                        showIncomeDialog = true
                    },
                    onEditTransaction = openTransactionEditor,
                    onDeleteTransaction = viewModel::deleteTransaction,
                    onNavigateToFunds = {
                        planningSubTab = 0
                        currentTab = MainTab.PLANNING
                    },
                    onNavigateToWeeklyReview = {
                        planningSubTab = 3
                        currentTab = MainTab.PLANNING
                    },
                    onNavigateToAiAdvisor = {
                        moreSubScreen = MoreSubScreen.AI_ADVISOR
                        currentTab = MainTab.MORE
                    },
                    onNavigateToThemeWheel = {
                        moreSubScreen = MoreSubScreen.THEME_WHEEL
                        currentTab = MainTab.MORE
                    },
                    onNavigateToReports = {
                        currentTab = MainTab.REPORTS
                    },
                    modifier = contentModifier
                )

                MainTab.TRANSACTIONS -> TransactionsAndBudgetScreen(
                    uiState = uiState,
                    onAddExpenseClick = {
                        editingExpense = null
                        showExpenseDialog = true
                    },
                    onAddIncomeClick = {
                        editingIncome = null
                        showIncomeDialog = true
                    },
                    onEditTransaction = openTransactionEditor,
                    onDeleteTransaction = viewModel::deleteTransaction,
                    onSaveCategory = { id, name, color, budget ->
                        viewModel.saveCategory(id, name, color, budget)
                    },
                    onDeleteCategory = viewModel::deleteCategory,
                    onSaveBudgetSettings = viewModel::updateBudgetSettings,
                    onCloseMonth = viewModel::closeCurrentMonth,
                    modifier = contentModifier
                )

                MainTab.PLANNING -> PlanningHubScreen(
                    uiState = uiState,
                    initialSubTab = planningSubTab,
                    onAddFund = viewModel::addSinkingFund,
                    onFundMovement = viewModel::recordFundMovement,
                    onSaveShoppingItem = { id, name, freq, qty, expected, catId ->
                        viewModel.saveShoppingItem(id, name, freq, qty, expected, catId)
                    },
                    onToggleShoppingPurchased = viewModel::toggleShoppingPurchased,
                    onConvertShoppingToExpense = viewModel::convertShoppingToExpense,
                    onDeleteShoppingItem = viewModel::deleteShoppingItem,
                    onSaveInventoryItem = { id, name, qty, min, unit, exp ->
                        viewModel.saveInventoryItem(id, name, qty, min, unit, exp)
                    },
                    onAdjustInventory = viewModel::adjustInventoryQuantity,
                    onAddInventoryToShopping = viewModel::addInventoryToShoppingList,
                    onDeleteInventoryItem = viewModel::deleteInventoryItem,
                    onCompleteWeeklyReview = viewModel::markWeeklyReviewCompleted,
                    modifier = contentModifier
                )

                MainTab.REPORTS -> ReportsScreen(
                    uiState = uiState,
                    onExportPdf = viewModel::exportPdfReport,
                    onExportCsv = viewModel::exportCsvReport,
                    modifier = contentModifier
                )

                MainTab.MORE -> MoreHubScreen(
                    subScreen = moreSubScreen,
                    onSelectSubScreen = { moreSubScreen = it },
                    uiState = uiState,
                    aiMessages = aiMessages,
                    aiState = aiState,
                    isOnline = isOnline,
                    isCloudMode = currentUserId != null,
                    userEmail = currentUserEmail,
                    onAskAi = viewModel::askAiAdvisor,
                    onSaveTheme = viewModel::saveThemeCustomization,
                    onResetTheme = viewModel::resetThemeColors,
                    onUpdateHouseholdName = viewModel::updateHouseholdName,
                    onSaveMember = viewModel::saveFamilyMember,
                    onDeleteMember = viewModel::deleteFamilyMember,
                    onSetActiveMember = viewModel::setActiveMemberName,
                    onJoinByCode = viewModel::joinHouseholdByCode,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetCurrency = viewModel::setCurrencyCode,
                    onUpdateNotifications = viewModel::updateNotificationSettings,
                    onGenerateBackupJson = viewModel::generateBackupJson,
                    onRestoreBackupJson = viewModel::restoreBackupJson,
                    onSignOut = {
                        signOutFromGoogle(credentialManager, scope) {
                            scope.launch { preferencesRepository.setLocalOnlyMode(false) }
                        }
                    },
                    onSignedInFromLocal = {
                        scope.launch { preferencesRepository.setLocalOnlyMode(false) }
                    },
                    modifier = contentModifier
                )
            }
        }

        if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(16.dp))
                    MainTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = {
                                if (tab != MainTab.MORE) moreSubScreen = MoreSubScreen.MENU
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.labelAr) },
                            label = {
                                Text(
                                    tab.labelAr,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        if (showFab) {
                            FloatingActionButton(
                                onClick = {
                                    editingExpense = null
                                    showExpenseDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_add_expense")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "إضافة مصروف")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        mainContent(Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (showFab) {
                        FloatingActionButton(
                            onClick = {
                                editingExpense = null
                                showExpenseDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_expense")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "إضافة مصروف")
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = {
                                    if (tab != MainTab.MORE) moreSubScreen = MoreSubScreen.MENU
                                    currentTab = tab
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.labelAr) },
                                label = {
                                    Text(
                                        tab.labelAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    mainContent(Modifier.fillMaxSize())
                }
            }
        }
    }
}
