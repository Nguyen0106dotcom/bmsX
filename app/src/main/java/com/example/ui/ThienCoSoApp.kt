package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.XsmbStation
import com.example.ui.screens.AlgorithmsScreen
import com.example.ui.screens.KqxsScreen
import com.example.ui.screens.PredictScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrialPlayScreen
import com.example.ui.theme.ThienCoSoTheme

sealed class AppRoute(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    data object Predict : AppRoute("predict", R.string.nav_predict, Icons.Default.Analytics, "nav_predict")
    data object Algorithms : AppRoute("algorithms", R.string.nav_algorithms, Icons.Default.Tune, "nav_algorithms")
    data object Kqxs : AppRoute("kqxs", R.string.nav_kqxs, Icons.AutoMirrored.Filled.ReceiptLong, "nav_kqxs")
    data object Trial : AppRoute("trial", R.string.nav_trial, Icons.Default.Casino, "nav_trial")
    data object Settings : AppRoute("settings", R.string.nav_settings, Icons.Default.Settings, "nav_settings")
}

private val TOP_DESTINATIONS = listOf(
    AppRoute.Predict,
    AppRoute.Algorithms,
    AppRoute.Kqxs,
    AppRoute.Trial,
    AppRoute.Settings
)

@Composable
fun ThienCoSoApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.Predict.route

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    ThienCoSoTheme(darkTheme = state.darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            TOP_DESTINATIONS.forEach { dest ->
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(AppRoute.Predict.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            TOP_DESTINATIONS.forEach { dest ->
                                val label = stringResource(dest.labelRes)
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(AppRoute.Predict.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.Predict.route,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(AppRoute.Predict.route) {
                            PredictScreen(
                                state = state,
                                onSelectProfile = viewModel::selectProfile,
                                onPrevDate = { viewModel.stepPredictDate(false) },
                                onNextDate = { viewModel.stepPredictDate(true) },
                                onDatePicked = viewModel::setPredictTargetDate,
                                onRunPrediction = viewModel::runPrediction,
                                onPredictNextRealTime = { viewModel.predictNextRealTimeDraw(syncFirst = false) },
                                onJumpToPastDraw = viewModel::jumpToPastDrawOffset,
                                onSortChanged = viewModel::setSortOrder,
                                onInspectNumber = viewModel::inspectNumber,
                                onDismissDetail = viewModel::dismissNumberDetail,
                                onSyncCurrent = { viewModel.syncProfile(state.selectedProfile) },
                                onSelectVerifyPresetDraws = viewModel::runHistoricalVerificationForPreset,
                                onRunCombinedBacktest = viewModel::runCombinedBacktest,
                                onSelectXsmbStation = viewModel::selectXsmbStation,
                                onToggleStationOnlyHistory = viewModel::toggleStationOnlyHistory,
                                onSelectScheduleDay = viewModel::selectScheduleDay,
                                onFetchSevenDays = { viewModel.fetchAndCompareLast7Days(userInitiated = true) },
                                onOpenKqxsForDate = { date ->
                                    viewModel.selectKqxsStationFilter(XsmbStation.ALL)
                                    viewModel.selectKqxsDate(date)
                                    navController.navigate(AppRoute.Kqxs.route) {
                                        popUpTo(AppRoute.Predict.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(AppRoute.Algorithms.route) {
                            AlgorithmsScreen(
                                state = state,
                                onToggleEnabled = viewModel::toggleAlgorithmEnabled,
                                onToggleWeight = viewModel::toggleAlgorithmWeight,
                                onWeightChange = viewModel::setAlgorithmWeightValue,
                                onParamChange = viewModel::updateAlgorithmParam,
                                onResetAlgo = viewModel::resetAlgorithmParams,
                                onOptimizeAlgo = viewModel::optimizeAlgorithm,
                                onApplyOptimization = viewModel::applyOptimizationReport,
                                onDismissOptimization = viewModel::dismissOptimizationReport,
                                onRunPeriodBacktest = { algo -> viewModel.runMultiPeriodBacktest(algo) },
                                onGenerateAiBundle = viewModel::generateAiFormula,
                                onSaveAiBundle = viewModel::saveDraftBundleAsAlgorithm,
                                onDeleteCustomBundle = viewModel::deleteCustomBundle
                            )
                        }

                        composable(AppRoute.Kqxs.route) {
                            KqxsScreen(
                                state = state,
                                onPrevKqxsDate = { viewModel.stepKqxsDate(false) },
                                onNextKqxsDate = { viewModel.stepKqxsDate(true) },
                                onSelectKqxsDate = viewModel::selectKqxsDate,
                                onSelectKqxsStationFilter = viewModel::selectKqxsStationFilter,
                                onRunDeepAnalysis = viewModel::runDeepAnalysis
                            )
                        }

                        composable(AppRoute.Trial.route) {
                            TrialPlayScreen(
                                state = state,
                                onToggleTrialAlgo = viewModel::toggleTrialAlgorithm,
                                onAddStrategy = viewModel::addTrialStrategy,
                                onRemoveStrategy = viewModel::removeTrialStrategy,
                                onRunSimulation = viewModel::runTrialSimulation
                            )
                        }

                        composable(AppRoute.Settings.route) {
                            SettingsScreen(
                                state = state,
                                onToggleDarkTheme = viewModel::setDarkTheme,
                                onUpdateSyncUrl = viewModel::updateSyncUrl,
                                onSyncProfile = viewModel::syncProfile,
                                onResetDefaults = viewModel::resetAllSettings
                            )
                        }
                    }
                }
            }
        }
    }
}
