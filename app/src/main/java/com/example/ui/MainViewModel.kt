package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiGenerationResult
import com.example.ai.AiRepository
import com.example.ai.PlaceholderAiRepository
import com.example.data.AlgorithmPreference
import com.example.data.AppSettings
import com.example.data.BetStrategy
import com.example.data.CombinedPerformanceStats
import com.example.data.DailyKqxsStats
import com.example.data.DeepAnalysisResult
import com.example.data.Last7DaysApiResult
import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.LotteryProfiles
import com.example.data.LotteryRepository
import com.example.data.NumberDetailInfo
import com.example.data.PredictionResult
import com.example.data.ScheduledDrawInfo
import com.example.data.SettingsRepository
import com.example.data.SevenDayComparisonSummary
import com.example.data.SimulationSummary
import com.example.data.SortOrder
import com.example.data.StationDailyPrediction
import com.example.data.StoredBundleRecord
import com.example.data.SyncResult
import com.example.data.ThirtyDayAccuracyChartData
import com.example.data.TrialBetType
import com.example.data.VN_ZONE_ID
import com.example.data.XsmbPublicApiService
import com.example.data.XsmbStation
import com.example.engine.AnalyticsEngine
import com.example.engine.LotteryAlgorithm
import com.example.engine.OptimizationReport
import com.example.engine.OptimizerAndBacktestEngine
import com.example.engine.PeriodBacktestRow
import com.example.engine.PredictionEngine
import com.example.engine.TrialPlaySimulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class MainUiState(
    val isInitializing: Boolean = true,
    val darkTheme: Boolean = true,
    val selectedProfile: LotteryProfile = LotteryProfiles.XSMB,
    val draws: List<LotteryDraw> = emptyList(),
    val latestDrawDate: LocalDate? = null,
    val currentVnDateTime: ZonedDateTime = ZonedDateTime.now(VN_ZONE_ID),
    val nextScheduledDraw: ScheduledDrawInfo = LotteryProfiles.XSMB.nextScheduledDraw(),
    val countdownText: String = "--:--:--",
    val dataGapDays: Int = 0,
    val predictTargetDate: LocalDate = ZonedDateTime.now(VN_ZONE_ID).toLocalDate(),
    val sortOrder: SortOrder = SortOrder.HIGH_TO_LOW,
    val isPredicting: Boolean = false,
    val predictionResult: PredictionResult? = null,
    val selectedNumberDetail: NumberDetailInfo? = null,
    val isEvaluatingCombined: Boolean = false,
    val combinedPerformance: CombinedPerformanceStats? = null,
    val combinedFromDate: LocalDate = ZonedDateTime.now(VN_ZONE_ID).toLocalDate().minusDays(30),
    val combinedToDate: LocalDate = ZonedDateTime.now(VN_ZONE_ID).toLocalDate(),

    // XSMB Station Classification & Schedule & 7-Day Public API Comparison
    val selectedXsmbStation: XsmbStation = XsmbStation.ALL,
    val useStationOnlyHistory: Boolean = true,
    val stationSchedulePredictions: List<StationDailyPrediction> = emptyList(),
    val isLoadingSchedulePredictions: Boolean = false,
    val isLoadingSevenDays: Boolean = false,
    val sevenDayComparison: SevenDayComparisonSummary? = null,
    val isLoadingAccuracyChart: Boolean = false,
    val thirtyDayAccuracyChart: ThirtyDayAccuracyChartData? = null,

    // Algorithms & Optimizer & AI
    val algorithms: List<LotteryAlgorithm> = emptyList(),
    val algorithmPrefs: Map<String, AlgorithmPreference> = emptyMap(),
    val customBundles: List<StoredBundleRecord> = emptyList(),
    val isOptimizing: Boolean = false,
    val optimizeProgress: Float = 0f,
    val optimizationReport: OptimizationReport? = null,
    val isRunningPeriodBacktest: Boolean = false,
    val periodBacktestRows: List<PeriodBacktestRow> = emptyList(),
    val periodBacktestAlgoName: String = "",
    val isGeneratingAi: Boolean = false,
    val aiDraftResult: AiGenerationResult? = null,
    val isFirebaseAiBacked: Boolean = false,

    // KQXS & Deep Analysis
    val kqxsSelectedDate: LocalDate = LocalDate.now(),
    val kqxsStationFilter: XsmbStation = XsmbStation.ALL,
    val currentKqxsDraw: LotteryDraw? = null,
    val dailyKqxsStats: DailyKqxsStats? = null,
    val isAnalyzingDeep: Boolean = false,
    val deepAnalysisResult: DeepAnalysisResult? = null,

    // Trial Play
    val trialStrategies: List<BetStrategy> = listOf(
        BetStrategy(
            id = "default_lo_top3",
            type = TrialBetType.LO,
            positions = listOf(1, 2, 3),
            points = 5,
            costPerPoint = TrialBetType.LO.defaultCostPerPoint,
            winPerPoint = TrialBetType.LO.defaultWinPerPoint
        )
    ),
    val trialSelectedAlgoIds: Set<String> = emptySet(),
    val isSimulatingTrial: Boolean = false,
    val trialProgress: Pair<Int, Int> = 0 to 0,
    val simulationSummary: SimulationSummary? = null,

    // Sync & Settings
    val isSyncing: Boolean = false,
    val lastSyncResult: SyncResult? = null,
    val syncUrls: Map<String, String> = emptyMap(),
    val profileRecordCounts: Map<String, Int> = emptyMap(),
    val profileLatestDates: Map<String, LocalDate?> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val lotteryRepo = LotteryRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val predictionEngine = PredictionEngine()
    private val analyticsEngine = AnalyticsEngine()
    private val optimizerEngine = OptimizerAndBacktestEngine()
    private val trialSimulator = TrialPlaySimulator()
    private val aiRepository: AiRepository = PlaceholderAiRepository()
    private val xsmbPublicApi = XsmbPublicApiService()
    private var last7DaysApiResult: Last7DaysApiResult? = null

    private val _uiState = MutableStateFlow(MainUiState(isFirebaseAiBacked = aiRepository.isFirebaseBacked))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        startRealTimeClockTicker()
        viewModelScope.launch {
            val saved = settingsRepo.settingsFlow.first()
            val profile = LotteryProfiles.byId(saved.selectedLotteryId)
            val algos = predictionEngine.buildRegistry(saved.customBundles)
            val mergedPrefs = buildDefaultMergedPrefs(algos, saved.algorithmPrefs)

            _uiState.update {
                it.copy(
                    darkTheme = saved.darkTheme,
                    selectedProfile = profile,
                    algorithms = algos,
                    algorithmPrefs = mergedPrefs,
                    customBundles = saved.customBundles,
                    syncUrls = saved.syncUrls,
                    trialSelectedAlgoIds = algos.filter { a -> mergedPrefs[a.id]?.enabled == true }.map { a -> a.id }.toSet()
                )
            }

            loadProfileData(profile, runInitialPrediction = true)
            refreshAllProfileMetadata()
            refreshStationSchedulePredictions()
            fetchAndCompareLast7Days(userInitiated = false)
            // Silent background sync on startup to fetch up-to-date results for next-draw prediction
            syncProfile(profile, silent = true)
        }
    }

    private fun startRealTimeClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                val nowVn = ZonedDateTime.now(VN_ZONE_ID)
                val state = _uiState.value
                val nextInfo = computeActiveNextScheduledDraw(state, nowVn)
                val countdown = formatCountdown(nowVn, nextInfo)
                val refLatestDate = if (state.selectedProfile.id == "xsmb" && state.selectedXsmbStation != XsmbStation.ALL) {
                    state.draws.lastOrNull { state.selectedXsmbStation.matchesDate(it.date) }?.date
                } else {
                    state.latestDrawDate
                }
                val gapDays = refLatestDate?.let {
                    ChronoUnit.DAYS.between(it, nextInfo.targetDate).toInt().coerceAtLeast(0)
                } ?: 0

                _uiState.update {
                    it.copy(
                        currentVnDateTime = nowVn,
                        nextScheduledDraw = nextInfo,
                        countdownText = countdown,
                        dataGapDays = gapDays
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun computeActiveNextScheduledDraw(state: MainUiState, nowVn: ZonedDateTime): ScheduledDrawInfo {
        return if (state.selectedProfile.id == "xsmb" && state.selectedXsmbStation != XsmbStation.ALL) {
            val latestForStation = state.draws.lastOrNull { state.selectedXsmbStation.matchesDate(it.date) }?.date
            state.selectedXsmbStation.nextScheduledDraw(nowVn, latestForStation)
        } else {
            state.selectedProfile.nextScheduledDraw(nowVn, state.latestDrawDate)
        }
    }

    private fun formatCountdown(nowVn: ZonedDateTime, nextInfo: ScheduledDrawInfo): String {
        if (nextInfo.isLiveWindow) return "Đang mở thưởng"
        val secs = Duration.between(nowVn, nextInfo.drawDateTime).seconds.coerceAtLeast(0L)
        val days = secs / 86400
        val hours = (secs % 86400) / 3600
        val mins = (secs % 3600) / 60
        val remSecs = secs % 60
        return if (days > 0) {
            "%d ngày %02d:%02d:%02d".format(days, hours, mins, remSecs)
        } else {
            "%02d:%02d:%02d".format(hours, mins, remSecs)
        }
    }

    private fun buildDefaultMergedPrefs(
        algos: List<LotteryAlgorithm>,
        savedPrefs: Map<String, AlgorithmPreference>
    ): Map<String, AlgorithmPreference> {
        val out = LinkedHashMap<String, AlgorithmPreference>()
        for (algo in algos) {
            val existing = savedPrefs[algo.id]
            out[algo.id] = existing ?: AlgorithmPreference(
                id = algo.id,
                enabled = true,
                weightEnabled = false,
                weightValue = 1.0,
                params = emptyMap()
            )
        }
        return out
    }

    fun selectProfile(profile: LotteryProfile) {
        if (profile.id == _uiState.value.selectedProfile.id) return
        viewModelScope.launch {
            settingsRepo.setSelectedLottery(profile.id)
            _uiState.update {
                it.copy(
                    selectedProfile = profile,
                    isInitializing = true,
                    predictionResult = null,
                    combinedPerformance = null,
                    thirtyDayAccuracyChart = null,
                    deepAnalysisResult = null,
                    simulationSummary = null
                )
            }
            loadProfileData(profile, runInitialPrediction = true)
        }
    }

    private suspend fun loadProfileData(profile: LotteryProfile, runInitialPrediction: Boolean) {
        val draws = lotteryRepo.loadDraws(profile)
        val latestDate = draws.lastOrNull()?.date
        val nowVn = ZonedDateTime.now(VN_ZONE_ID)
        val curStation = _uiState.value.selectedXsmbStation
        val nextInfo = if (profile.id == "xsmb" && curStation != XsmbStation.ALL) {
            val latestStation = draws.lastOrNull { curStation.matchesDate(it.date) }?.date
            curStation.nextScheduledDraw(nowVn, latestStation)
        } else {
            profile.nextScheduledDraw(nowVn, latestDate)
        }
        val defaultPredictDate = nextInfo.targetDate
        val defaultKqxsDate = latestDate ?: nowVn.toLocalDate()
        val fromDate = (latestDate ?: nowVn.toLocalDate()).minusDays(30)
        val toDate = latestDate ?: nowVn.toLocalDate()
        val gapDays = latestDate?.let {
            ChronoUnit.DAYS.between(it, nextInfo.targetDate).toInt().coerceAtLeast(0)
        } ?: 0

        val kqxsDraw = draws.lastOrNull { it.date == defaultKqxsDate } ?: draws.lastOrNull()
        val kqxsStats = kqxsDraw?.let {
            withContext(Dispatchers.Default) {
                analyticsEngine.computeDailyStats(it, draws, profile)
            }
        }

        _uiState.update {
            it.copy(
                isInitializing = false,
                draws = draws,
                latestDrawDate = latestDate,
                currentVnDateTime = nowVn,
                nextScheduledDraw = nextInfo,
                countdownText = formatCountdown(nowVn, nextInfo),
                dataGapDays = gapDays,
                predictTargetDate = defaultPredictDate,
                kqxsSelectedDate = kqxsDraw?.date ?: defaultKqxsDate,
                currentKqxsDraw = kqxsDraw,
                dailyKqxsStats = kqxsStats,
                combinedFromDate = fromDate,
                combinedToDate = toDate,
                profileRecordCounts = it.profileRecordCounts + (profile.id to draws.size),
                profileLatestDates = it.profileLatestDates + (profile.id to latestDate)
            )
        }

        if (runInitialPrediction && draws.isNotEmpty()) {
            runPrediction()
        }
        if (profile.id == "xsmb") {
            refreshStationSchedulePredictions()
        }
        refreshThirtyDayAccuracyChart()
    }

    private fun refreshAllProfileMetadata() {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            val dates = mutableMapOf<String, LocalDate?>()
            for (p in LotteryProfiles.ALL) {
                val list = lotteryRepo.loadDraws(p)
                counts[p.id] = list.size
                dates[p.id] = list.lastOrNull()?.date
            }
            _uiState.update {
                it.copy(profileRecordCounts = counts, profileLatestDates = dates)
            }
        }
    }

    fun setPredictTargetDate(date: LocalDate) {
        _uiState.update { it.copy(predictTargetDate = date) }
        runPrediction()
    }

    fun predictNextRealTimeDraw(syncFirst: Boolean = false) {
        val state = _uiState.value
        val nowVn = ZonedDateTime.now(VN_ZONE_ID)
        val nextInfo = computeActiveNextScheduledDraw(state, nowVn)
        _uiState.update {
            it.copy(
                currentVnDateTime = nowVn,
                nextScheduledDraw = nextInfo,
                predictTargetDate = nextInfo.targetDate
            )
        }
        if (syncFirst) {
            syncProfile(state.selectedProfile, silent = false)
        } else {
            runPrediction()
        }
    }

    fun stepPredictDate(forward: Boolean) {
        val state = _uiState.value
        val profile = state.selectedProfile
        val current = state.predictTargetDate
        val next = if (profile.id == "xsmb" && state.selectedXsmbStation != XsmbStation.ALL) {
            if (forward) state.selectedXsmbStation.nextDrawDate(current) else state.selectedXsmbStation.prevDrawDate(current)
        } else {
            if (forward) profile.nextDrawDate(current) else profile.prevDrawDate(current)
        }
        setPredictTargetDate(next)
    }

    fun selectXsmbStation(station: XsmbStation) {
        val state = _uiState.value
        val nowVn = ZonedDateTime.now(VN_ZONE_ID)
        val nextInfo = if (station != XsmbStation.ALL) {
            val latestStation = state.draws.lastOrNull { station.matchesDate(it.date) }?.date
            station.nextScheduledDraw(nowVn, latestStation)
        } else {
            state.selectedProfile.nextScheduledDraw(nowVn, state.latestDrawDate)
        }
        _uiState.update {
            it.copy(
                selectedXsmbStation = station,
                currentVnDateTime = nowVn,
                nextScheduledDraw = nextInfo,
                countdownText = formatCountdown(nowVn, nextInfo),
                predictTargetDate = nextInfo.targetDate
            )
        }
        runPrediction()
        recomputeSevenDayComparison()
        refreshThirtyDayAccuracyChart()
    }

    fun toggleStationOnlyHistory(enabled: Boolean) {
        _uiState.update { it.copy(useStationOnlyHistory = enabled) }
        runPrediction()
        refreshStationSchedulePredictions()
        recomputeSevenDayComparison()
        refreshThirtyDayAccuracyChart()
    }

    fun refreshThirtyDayAccuracyChart() {
        val state = _uiState.value
        if (state.draws.size < 2) return
        _uiState.update { it.copy(isLoadingAccuracyChart = true) }
        viewModelScope.launch {
            val activeStation = if (state.selectedProfile.id == "xsmb") state.selectedXsmbStation else XsmbStation.ALL
            val chart = withContext(Dispatchers.Default) {
                predictionEngine.buildThirtyDayAccuracyTrend(
                    allDrawsSortedAsc = state.draws,
                    profile = state.selectedProfile,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    stationFilter = activeStation,
                    stationOnlyHistory = state.useStationOnlyHistory,
                    maxDraws = 30
                )
            }
            _uiState.update {
                it.copy(
                    isLoadingAccuracyChart = false,
                    thirtyDayAccuracyChart = chart
                )
            }
        }
    }

    fun selectScheduleDay(item: StationDailyPrediction) {
        val state = _uiState.value
        val nowVn = ZonedDateTime.now(VN_ZONE_ID)
        val latestForStation = state.draws.lastOrNull { item.station.matchesDate(it.date) }?.date
        val nextInfo = item.station.nextScheduledDraw(nowVn, latestForStation)
        _uiState.update {
            it.copy(
                selectedXsmbStation = item.station,
                predictTargetDate = item.date,
                currentVnDateTime = nowVn,
                nextScheduledDraw = nextInfo,
                countdownText = formatCountdown(nowVn, nextInfo)
            )
        }
        runPrediction()
    }

    fun refreshStationSchedulePredictions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSchedulePredictions = true) }
            val state = _uiState.value
            val xsmbDraws = if (state.selectedProfile.id == "xsmb" && state.draws.isNotEmpty()) {
                state.draws
            } else {
                lotteryRepo.loadDraws(LotteryProfiles.XSMB)
            }
            val nowVn = ZonedDateTime.now(VN_ZONE_ID)
            val items = withContext(Dispatchers.Default) {
                predictionEngine.buildWeeklyStationSchedulePredictions(
                    nowVn = nowVn,
                    latestLocalDate = xsmbDraws.lastOrNull()?.date,
                    allDrawsSortedAsc = xsmbDraws,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    stationOnlyHistory = state.useStationOnlyHistory
                )
            }
            _uiState.update {
                it.copy(
                    isLoadingSchedulePredictions = false,
                    stationSchedulePredictions = items
                )
            }
        }
    }

    fun fetchAndCompareLast7Days(userInitiated: Boolean = true) {
        if (_uiState.value.isLoadingSevenDays) return
        _uiState.update { it.copy(isLoadingSevenDays = true) }
        viewModelScope.launch {
            val localXsmb = if (_uiState.value.selectedProfile.id == "xsmb" && _uiState.value.draws.isNotEmpty()) {
                _uiState.value.draws
            } else {
                lotteryRepo.loadDraws(LotteryProfiles.XSMB)
            }
            val apiResult = xsmbPublicApi.fetchLast7DaysXsmb(localXsmb)
            last7DaysApiResult = apiResult

            val mergedXsmb = if (apiResult.fetchedFromNetwork && apiResult.draws.isNotEmpty()) {
                val arr = xsmbPublicApi.toXsmbJsonArray(apiResult.draws)
                if (arr.length() > 0) {
                    lotteryRepo.mergeXsmbJsonArray(arr)
                } else {
                    localXsmb
                }
            } else {
                localXsmb
            }

            if (mergedXsmb.size > localXsmb.size && _uiState.value.selectedProfile.id == "xsmb") {
                val latestDate = mergedXsmb.lastOrNull()?.date
                _uiState.update {
                    it.copy(
                        draws = mergedXsmb,
                        latestDrawDate = latestDate,
                        profileRecordCounts = it.profileRecordCounts + ("xsmb" to mergedXsmb.size),
                        profileLatestDates = it.profileLatestDates + ("xsmb" to latestDate)
                    )
                }
            }

            val state = _uiState.value
            val useStationHist = state.selectedXsmbStation != XsmbStation.ALL && state.useStationOnlyHistory
            val summary = withContext(Dispatchers.Default) {
                predictionEngine.buildSevenDayComparison(
                    targetDrawsDesc = apiResult.draws.sortedByDescending { it.date },
                    allDrawsSortedAsc = mergedXsmb,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    sourceLabel = apiResult.sourceLabel,
                    fetchedFromNetwork = apiResult.fetchedFromNetwork,
                    fetchedAt = apiResult.fetchedAt,
                    errorMessage = apiResult.errorMessage,
                    useStationHistory = useStationHist
                )
            }

            _uiState.update {
                it.copy(
                    isLoadingSevenDays = false,
                    sevenDayComparison = summary
                )
            }

            if (mergedXsmb.size > localXsmb.size) {
                runPrediction()
                refreshStationSchedulePredictions()
                refreshThirtyDayAccuracyChart()
            }

            if (userInitiated) {
                if (apiResult.fetchedFromNetwork) {
                    _snackbarEvents.tryEmit("Đã tải ${apiResult.draws.size} kỳ XSMB từ ${apiResult.sourceLabel}.")
                } else if (apiResult.errorMessage != null) {
                    _snackbarEvents.tryEmit(apiResult.errorMessage)
                }
            }
        }
    }

    private fun recomputeSevenDayComparison() {
        val apiResult = last7DaysApiResult ?: return
        val state = _uiState.value
        viewModelScope.launch {
            val xsmbDraws = if (state.selectedProfile.id == "xsmb" && state.draws.isNotEmpty()) {
                state.draws
            } else {
                lotteryRepo.loadDraws(LotteryProfiles.XSMB)
            }
            val useStationHist = state.selectedXsmbStation != XsmbStation.ALL && state.useStationOnlyHistory
            val summary = withContext(Dispatchers.Default) {
                predictionEngine.buildSevenDayComparison(
                    targetDrawsDesc = apiResult.draws.sortedByDescending { it.date },
                    allDrawsSortedAsc = xsmbDraws,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    sourceLabel = apiResult.sourceLabel,
                    fetchedFromNetwork = apiResult.fetchedFromNetwork,
                    fetchedAt = apiResult.fetchedAt,
                    errorMessage = apiResult.errorMessage,
                    useStationHistory = useStationHist
                )
            }
            _uiState.update { it.copy(sevenDayComparison = summary) }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun runPrediction() {
        val state = _uiState.value
        if (state.draws.isEmpty()) return
        _uiState.update { it.copy(isPredicting = true) }
        viewModelScope.launch {
            val activeStation = if (state.selectedProfile.id == "xsmb") state.selectedXsmbStation else XsmbStation.ALL
            val result = withContext(Dispatchers.Default) {
                predictionEngine.predictForDate(
                    targetDate = state.predictTargetDate,
                    allDraws = state.draws,
                    profile = state.selectedProfile,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    stationFilter = activeStation,
                    stationOnlyHistory = state.useStationOnlyHistory
                )
            }
            _uiState.update { it.copy(isPredicting = false, predictionResult = result) }
        }
    }

    fun inspectNumber(number: Int) {
        val pred = _uiState.value.predictionResult ?: return
        val draws = _uiState.value.draws
        viewModelScope.launch {
            val detail = withContext(Dispatchers.Default) {
                predictionEngine.buildNumberDetail(number, pred, draws)
            }
            _uiState.update { it.copy(selectedNumberDetail = detail) }
        }
    }

    fun dismissNumberDetail() {
        _uiState.update { it.copy(selectedNumberDetail = null) }
    }

    fun jumpToPastDrawOffset(drawsBackFromLatest: Int) {
        val state = _uiState.value
        if (state.draws.isEmpty()) return
        val pool = if (state.selectedProfile.id == "xsmb" && state.selectedXsmbStation != XsmbStation.ALL) {
            state.draws.filter { state.selectedXsmbStation.matchesDate(it.date) }
        } else {
            state.draws
        }
        if (pool.isEmpty()) return
        val targetIdx = (pool.lastIndex - drawsBackFromLatest).coerceIn(0, pool.lastIndex)
        setPredictTargetDate(pool[targetIdx].date)
    }

    fun setCombinedDateRange(from: LocalDate, to: LocalDate) {
        _uiState.update { it.copy(combinedFromDate = from, combinedToDate = to) }
    }

    fun runHistoricalVerificationForPreset(drawCount: Int) {
        val state = _uiState.value
        if (state.draws.isEmpty()) return
        val pool = if (state.selectedProfile.id == "xsmb" && state.selectedXsmbStation != XsmbStation.ALL) {
            state.draws.filter { state.selectedXsmbStation.matchesDate(it.date) }
        } else {
            state.draws
        }
        if (pool.isEmpty()) return
        val slice = pool.takeLast(drawCount.coerceAtLeast(1))
        val from = slice.first().date
        val to = slice.last().date
        _uiState.update { it.copy(combinedFromDate = from, combinedToDate = to) }
        runCombinedBacktest()
    }

    fun runCombinedBacktest() {
        val state = _uiState.value
        if (state.draws.isEmpty()) return
        _uiState.update { it.copy(isEvaluatingCombined = true) }
        viewModelScope.launch {
            val activeStation = if (state.selectedProfile.id == "xsmb") state.selectedXsmbStation else XsmbStation.ALL
            val stats = withContext(Dispatchers.Default) {
                predictionEngine.evaluateCombinedPerformance(
                    fromDate = state.combinedFromDate,
                    toDate = state.combinedToDate,
                    allDraws = state.draws,
                    profile = state.selectedProfile,
                    algorithms = state.algorithms,
                    prefsMap = state.algorithmPrefs,
                    stationFilter = activeStation,
                    stationOnlyHistory = state.useStationOnlyHistory
                )
            }
            _uiState.update { it.copy(isEvaluatingCombined = false, combinedPerformance = stats) }
        }
    }

    fun toggleAlgorithmEnabled(algoId: String, enabled: Boolean) {
        updateAlgoPref(algoId) { it.copy(enabled = enabled) }
    }

    fun toggleAlgorithmWeight(algoId: String, weightEnabled: Boolean) {
        updateAlgoPref(algoId) { it.copy(weightEnabled = weightEnabled) }
    }

    fun setAlgorithmWeightValue(algoId: String, weight: Double) {
        updateAlgoPref(algoId) { it.copy(weightValue = weight) }
    }

    fun updateAlgorithmParam(algoId: String, paramKey: String, value: Double) {
        updateAlgoPref(algoId) { pref ->
            pref.copy(params = pref.params + (paramKey to value))
        }
    }

    fun resetAlgorithmParams(algoId: String) {
        updateAlgoPref(algoId) { it.copy(params = emptyMap(), weightValue = 1.0, weightEnabled = false) }
        _snackbarEvents.tryEmit("Đã khôi phục tham số mặc định.")
    }

    private fun updateAlgoPref(algoId: String, transform: (AlgorithmPreference) -> AlgorithmPreference) {
        val currentMap = _uiState.value.algorithmPrefs.toMutableMap()
        val old = currentMap[algoId] ?: AlgorithmPreference(algoId)
        currentMap[algoId] = transform(old)
        _uiState.update { it.copy(algorithmPrefs = currentMap) }
        viewModelScope.launch {
            settingsRepo.saveAlgorithmPreferences(currentMap)
        }
        refreshStationSchedulePredictions()
        recomputeSevenDayComparison()
        refreshThirtyDayAccuracyChart()
    }

    fun optimizeAlgorithm(algo: LotteryAlgorithm) {
        val state = _uiState.value
        if (state.draws.size < 40) {
            _snackbarEvents.tryEmit("Cần tối thiểu 40 kỳ dữ liệu để tối ưu tham số.")
            return
        }
        _uiState.update { it.copy(isOptimizing = true, optimizeProgress = 0f, optimizationReport = null) }
        viewModelScope.launch {
            val initialParams = state.algorithmPrefs[algo.id]?.params ?: emptyMap()
            val report = withContext(Dispatchers.Default) {
                optimizerEngine.optimizeAlgorithmParameters(
                    algorithm = algo,
                    initialParams = initialParams,
                    allDraws = state.draws,
                    profile = state.selectedProfile,
                    validationCount = 45,
                    onProgress = { p -> _uiState.update { s -> s.copy(optimizeProgress = p) } }
                )
            }
            _uiState.update { it.copy(isOptimizing = false, optimizationReport = report) }
        }
    }

    fun applyOptimizationReport() {
        val report = _uiState.value.optimizationReport ?: return
        updateAlgoPref(report.algorithmId) { it.copy(params = report.optimizedParams, enabled = true) }
        _uiState.update { it.copy(optimizationReport = null) }
        _snackbarEvents.tryEmit("Đã áp dụng bộ tham số tối ưu cho ${report.algorithmName}.")
        runPrediction()
    }

    fun dismissOptimizationReport() {
        _uiState.update { it.copy(optimizationReport = null) }
    }

    fun runMultiPeriodBacktest(algo: LotteryAlgorithm, daysBack: Int = 180) {
        val state = _uiState.value
        if (state.draws.isEmpty()) return
        val endDate = state.draws.last().date
        val startDate = endDate.minusDays(daysBack.toLong())
        _uiState.update {
            it.copy(
                isRunningPeriodBacktest = true,
                periodBacktestAlgoName = algo.name,
                periodBacktestRows = emptyList()
            )
        }
        viewModelScope.launch {
            val rows = withContext(Dispatchers.Default) {
                optimizerEngine.runMultiPeriodBacktest(
                    algorithm = algo,
                    params = state.algorithmPrefs[algo.id]?.params ?: emptyMap(),
                    allDraws = state.draws,
                    profile = state.selectedProfile,
                    startDate = startDate,
                    endDate = endDate,
                    periodDays = 30
                )
            }
            _uiState.update { it.copy(isRunningPeriodBacktest = false, periodBacktestRows = rows) }
        }
    }

    fun generateAiFormula(prompt: String, name: String, description: String) {
        if (prompt.isBlank()) {
            _snackbarEvents.tryEmit("Vui lòng nhập mô tả ý tưởng thuật toán.")
            return
        }
        _uiState.update { it.copy(isGeneratingAi = true) }
        viewModelScope.launch {
            val result = aiRepository.generateAlgorithmBundle(prompt, name, description)
            _uiState.update { it.copy(isGeneratingAi = false, aiDraftResult = result) }
        }
    }

    fun saveDraftBundleAsAlgorithm() {
        val draft = _uiState.value.aiDraftResult ?: return
        viewModelScope.launch {
            settingsRepo.saveCustomBundle(draft.bundle)
            val saved = settingsRepo.settingsFlow.first()
            val algos = predictionEngine.buildRegistry(saved.customBundles)
            val newId = "bundle_${draft.bundle.id}"
            val updatedPrefs = _uiState.value.algorithmPrefs.toMutableMap()
            updatedPrefs[newId] = AlgorithmPreference(id = newId, enabled = true)
            settingsRepo.saveAlgorithmPreferences(updatedPrefs)
            _uiState.update {
                it.copy(
                    algorithms = algos,
                    customBundles = saved.customBundles,
                    algorithmPrefs = updatedPrefs,
                    aiDraftResult = null
                )
            }
            _snackbarEvents.tryEmit("Đã lưu thuật toán '${draft.bundle.name}' vào danh sách.")
            runPrediction()
            refreshThirtyDayAccuracyChart()
        }
    }

    fun deleteCustomBundle(bundleId: String) {
        viewModelScope.launch {
            settingsRepo.deleteCustomBundle(bundleId)
            val saved = settingsRepo.settingsFlow.first()
            val algos = predictionEngine.buildRegistry(saved.customBundles)
            _uiState.update {
                it.copy(algorithms = algos, customBundles = saved.customBundles)
            }
            _snackbarEvents.tryEmit("Đã xóa thuật toán tùy chỉnh.")
            runPrediction()
            refreshThirtyDayAccuracyChart()
        }
    }

    // --- KQXS & Deep Analysis ---
    fun selectKqxsStationFilter(station: XsmbStation) {
        _uiState.update { it.copy(kqxsStationFilter = station) }
        val draws = _uiState.value.draws
        if (draws.isEmpty()) return
        val filtered = if (station == XsmbStation.ALL) draws else draws.filter { station.matchesDate(it.date) }
        val latest = filtered.lastOrNull() ?: return
        selectKqxsDate(latest.date)
    }

    fun selectKqxsDate(date: LocalDate) {
        val draws = _uiState.value.draws
        val profile = _uiState.value.selectedProfile
        val stationFilter = if (profile.id == "xsmb") _uiState.value.kqxsStationFilter else XsmbStation.ALL
        val pool = if (stationFilter == XsmbStation.ALL) draws else draws.filter { stationFilter.matchesDate(it.date) }
        val exact = pool.lastOrNull { it.date == date }
            ?: pool.lastOrNull { !it.date.isAfter(date) }
            ?: pool.firstOrNull()
        viewModelScope.launch {
            val stats = exact?.let {
                withContext(Dispatchers.Default) {
                    analyticsEngine.computeDailyStats(it, draws, profile)
                }
            }
            _uiState.update {
                it.copy(
                    kqxsSelectedDate = exact?.date ?: date,
                    currentKqxsDraw = exact,
                    dailyKqxsStats = stats
                )
            }
        }
    }

    fun stepKqxsDate(forward: Boolean) {
        val draws = _uiState.value.draws
        val profile = _uiState.value.selectedProfile
        val stationFilter = if (profile.id == "xsmb") _uiState.value.kqxsStationFilter else XsmbStation.ALL
        val pool = if (stationFilter == XsmbStation.ALL) draws else draws.filter { stationFilter.matchesDate(it.date) }
        if (pool.isEmpty()) return
        val current = _uiState.value.currentKqxsDraw
        val idx = if (current != null) pool.indexOf(current).let { if (it >= 0) it else pool.lastIndex } else pool.lastIndex
        val nextIdx = if (forward) (idx + 1).coerceAtMost(pool.lastIndex) else (idx - 1).coerceAtLeast(0)
        selectKqxsDate(pool[nextIdx].date)
    }

    fun runDeepAnalysis(rawNumbers: String, daysLookback: Int?) {
        val parsedNums = rawNumbers.split(",", " ", ";", "-")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in _uiState.value.selectedProfile.validRange }
            .distinct()

        if (parsedNums.isEmpty()) {
            _snackbarEvents.tryEmit("Nhập ít nhất 1 số hợp lệ (VD: 68 hoặc 68, 86).")
            return
        }

        val draws = _uiState.value.draws
        val filteredDraws = if (daysLookback != null && draws.isNotEmpty()) {
            val cutoff = draws.last().date.minusDays(daysLookback.toLong())
            draws.filter { !it.date.isBefore(cutoff) }
        } else {
            draws
        }

        _uiState.update { it.copy(isAnalyzingDeep = true) }
        viewModelScope.launch {
            val res = withContext(Dispatchers.Default) {
                analyticsEngine.analyzeNumbers(parsedNums, filteredDraws)
            }
            _uiState.update { it.copy(isAnalyzingDeep = false, deepAnalysisResult = res) }
        }
    }

    // --- Trial Play ---
    fun toggleTrialAlgorithm(algoId: String) {
        val current = _uiState.value.trialSelectedAlgoIds.toMutableSet()
        if (algoId in current) current.remove(algoId) else current.add(algoId)
        _uiState.update { it.copy(trialSelectedAlgoIds = current) }
    }

    fun addTrialStrategy(strategy: BetStrategy) {
        _uiState.update { it.copy(trialStrategies = it.trialStrategies + strategy) }
    }

    fun removeTrialStrategy(strategyId: String) {
        _uiState.update { it.copy(trialStrategies = it.trialStrategies.filterNot { s -> s.id == strategyId }) }
    }

    fun addDefaultBetPreset(type: TrialBetType, positions: List<Int>, points: Int) {
        addTrialStrategy(
            BetStrategy(
                id = UUID.randomUUID().toString(),
                type = type,
                positions = positions,
                points = points,
                costPerPoint = type.defaultCostPerPoint,
                winPerPoint = type.defaultWinPerPoint
            )
        )
    }

    fun runTrialSimulation(
        initialCapital: Double,
        drawsCount: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ) {
        val state = _uiState.value
        val selectedAlgos = state.algorithms.filter { it.id in state.trialSelectedAlgoIds }
        if (selectedAlgos.isEmpty()) {
            _snackbarEvents.tryEmit("Chọn ít nhất 1 thuật toán để giả lập.")
            return
        }
        if (state.trialStrategies.isEmpty()) {
            _snackbarEvents.tryEmit("Thêm ít nhất 1 chiến lược đặt cược.")
            return
        }
        _uiState.update { it.copy(isSimulatingTrial = true, trialProgress = 0 to (drawsCount ?: 30)) }
        viewModelScope.launch {
            val summary = withContext(Dispatchers.Default) {
                trialSimulator.runSimulation(
                    initialCapital = initialCapital,
                    drawsToSimulateCount = drawsCount,
                    startDate = startDate,
                    endDate = endDate,
                    allDraws = state.draws,
                    profile = state.selectedProfile,
                    selectedAlgorithms = selectedAlgos,
                    prefsMap = state.algorithmPrefs,
                    strategies = state.trialStrategies,
                    onProgress = { done, total ->
                        _uiState.update { s -> s.copy(trialProgress = done to total) }
                    }
                )
            }
            _uiState.update { it.copy(isSimulatingTrial = false, simulationSummary = summary) }
        }
    }

    // --- Settings & Sync ---
    fun setDarkTheme(enabled: Boolean) {
        _uiState.update { it.copy(darkTheme = enabled) }
        viewModelScope.launch { settingsRepo.setDarkTheme(enabled) }
    }

    fun updateSyncUrl(lotteryId: String, url: String) {
        viewModelScope.launch {
            settingsRepo.setSyncUrl(lotteryId, url)
            val saved = settingsRepo.settingsFlow.first()
            _uiState.update { it.copy(syncUrls = saved.syncUrls) }
            _snackbarEvents.tryEmit("Đã lưu URL đồng bộ cho ${LotteryProfiles.byId(lotteryId).shortName}.")
        }
    }

    fun syncProfile(profile: LotteryProfile, silent: Boolean = false) {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val customUrl = _uiState.value.syncUrls[profile.id]
            val result = lotteryRepo.syncFromRemote(profile, customUrl)
            _uiState.update { it.copy(isSyncing = false, lastSyncResult = result) }
            if (!silent || result.addedRecords > 0) {
                _snackbarEvents.tryEmit(result.message)
            }
            if (result.success && (!silent || result.addedRecords > 0)) {
                if (profile.id == _uiState.value.selectedProfile.id) {
                    loadProfileData(profile, runInitialPrediction = true)
                }
                refreshAllProfileMetadata()
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsRepo.resetAllDefaults()
            val algos = predictionEngine.buildRegistry(emptyList())
            val merged = buildDefaultMergedPrefs(algos, emptyMap())
            _uiState.update {
                it.copy(
                    darkTheme = true,
                    selectedProfile = LotteryProfiles.XSMB,
                    algorithms = algos,
                    algorithmPrefs = merged,
                    customBundles = emptyList(),
                    syncUrls = emptyMap(),
                    trialSelectedAlgoIds = algos.map { a -> a.id }.toSet()
                )
            }
            loadProfileData(LotteryProfiles.XSMB, runInitialPrediction = true)
            _snackbarEvents.tryEmit("Đã khôi phục toàn bộ cài đặt gốc.")
        }
    }
}
