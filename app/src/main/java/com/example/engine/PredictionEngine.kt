package com.example.engine

import com.example.data.AccuracyTrendPoint
import com.example.data.AlgorithmContribution
import com.example.data.AlgorithmPreference
import com.example.data.CombinedPerformanceStats
import com.example.data.HistoricalVerificationDay
import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.LotteryProfiles
import com.example.data.NumberDetailInfo
import com.example.data.ThirtyDayAccuracyChartData
import com.example.data.PredictionResult
import com.example.data.SevenDayComparisonRow
import com.example.data.SevenDayComparisonSummary
import com.example.data.StationDailyPrediction
import com.example.data.StoredBundleRecord
import com.example.data.VN_ZONE_ID
import com.example.data.WinEvaluation
import com.example.data.XsmbDashboardSummary
import com.example.data.XsmbStation
import com.example.data.XsmbStationAccuracyRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PredictionEngine {

    companion object {
        const val BASE_SCORE = 100.0
    }

    fun buildRegistry(customBundles: List<StoredBundleRecord>): List<LotteryAlgorithm> {
        val builtIn = listOf(
            AppearancePointAlgorithm(
                id = "appearance_v13",
                name = "Điểm Lịch Sử Tối Ưu V13",
                description = "Phân tích đa tầng ngắn/dài hạn, thưởng giải ĐB & láng giềng, phạt chuỗi lặp Top 3 (thuat_toan_01.py).",
                defaultParams = AppearancePointAlgorithm.STANDARD_DEFAULTS
            ),
            AppearancePointAlgorithm(
                id = "appearance_v13_optimized",
                name = "Điểm Lịch Sử V13 (Bản Tối Ưu)",
                description = "Biến thể tinh chỉnh tham số từ dữ liệu huấn luyện (short=20, freq=37/225, special=2.1).",
                defaultParams = AppearancePointAlgorithm.OPTIMIZED_DEFAULTS
            ),
            DaysSinceLastAlgorithm(),
            PrizePositionPenaltyAlgorithm(),
            ThirtyDayFreqPenaltyAlgorithm()
        )
        val custom = customBundles.map { JsonBundleAlgorithm(it) }
        return builtIn + custom
    }

    fun predictForDate(
        targetDate: LocalDate,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile,
        algorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        warmUpTop3Days: Int = 7,
        stationFilter: XsmbStation = XsmbStation.ALL,
        stationOnlyHistory: Boolean = false
    ): PredictionResult {
        val history = if (profile.id == "xsmb" && stationFilter != XsmbStation.ALL && stationOnlyHistory) {
            allDraws.filter { it.date.isBefore(targetDate) && stationFilter.matchesDate(it.date) }
        } else {
            allDraws.filter { it.date.isBefore(targetDate) }
        }
        val actualDraw = allDraws.firstOrNull { it.date == targetDate }

        val enabledAlgos = algorithms.filter { prefsMap[it.id]?.enabled ?: true }
        val totalScores = LinkedHashMap<Int, Double>()
        val contributions = LinkedHashMap<Int, MutableList<AlgorithmContribution>>()
        for (n in profile.validRange) {
            totalScores[n] = BASE_SCORE
            contributions[n] = mutableListOf()
        }

        if (history.isNotEmpty() && enabledAlgos.isNotEmpty()) {
            for (algo in enabledAlgos) {
                algo.resetSessionState()
                val pref = prefsMap[algo.id]
                val algoParams = pref?.params ?: emptyMap()
                val weight = if (pref?.weightEnabled == true) pref.weightValue else 1.0

                // Warm up stateful previousTops if needed
                if (algo is AppearancePointAlgorithm && warmUpTop3Days > 0 && history.size > 1) {
                    val startIdx = (history.size - warmUpTop3Days).coerceAtLeast(1)
                    for (idx in startIdx until history.size) {
                        val subTarget = history[idx].date
                        algo.predict(subTarget, history.subList(0, idx), profile, algoParams)
                    }
                }

                val rawMap = algo.predict(targetDate, history, profile, algoParams)
                for ((num, raw) in rawMap) {
                    val weighted = raw * weight
                    totalScores[num] = (totalScores[num] ?: BASE_SCORE) + weighted
                    contributions[num]?.add(
                        AlgorithmContribution(
                            algorithmId = algo.id,
                            algorithmName = algo.name,
                            rawScore = raw,
                            weight = weight,
                            weightedDelta = weighted
                        )
                    )
                }
            }
        }

        val ranked = totalScores.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

        val top3 = ranked.take(3)
        val winEval = actualDraw?.let { evaluateWin(profile, ranked.map { p -> p.first }, it) }

        return PredictionResult(
            lotteryId = profile.id,
            targetDate = targetDate,
            scores = totalScores,
            rankedNumbers = ranked,
            top3 = top3,
            contributionsByNumber = contributions,
            actualDraw = actualDraw,
            winEvaluation = winEval
        )
    }

    fun evaluateWin(
        profile: LotteryProfile,
        rankedNumbers: List<Int>,
        actualDraw: LotteryDraw
    ): WinEvaluation {
        val picks = rankedNumbers.take(profile.evalPicks)
        val matched = picks.filter { it in actualDraw.uniqueNumbers }.toSet()
        val matchCount = matched.size
        val isWin = matchCount >= profile.minMatches
        val detail = if (isWin) {
            when (profile.id) {
                "xsmb" -> {
                    val spHit = actualDraw.specialNumber != null && actualDraw.specialNumber in matched
                    if (spHit) "🎯 NỔ ĐẶC BIỆT + Lô (${matched.joinToString(", ") { "%02d".format(it) }})"
                    else "✅ Trúng $matchCount/${profile.evalPicks} số (${matched.joinToString(", ") { "%02d".format(it) }})"
                }
                "power_645" -> when (matchCount) {
                    6 -> "🎉 JACKPOT (6/6 số)"
                    5 -> "⭐ Giải Nhất (5/6 số)"
                    4 -> "✨ Giải Nhì (4/6 số)"
                    else -> "✅ Giải Ba (3/6 số)"
                }
                "power_655" -> when {
                    matchCount == 6 -> "🎉 JACKPOT 1 (6/6 số)"
                    matchCount == 5 && actualDraw.specialNumber in picks -> "🎉 JACKPOT 2 (5 + ĐB)"
                    matchCount == 5 -> "⭐ Giải Nhất (5/6 số)"
                    matchCount == 4 -> "✨ Giải Nhì (4/6 số)"
                    else -> "✅ Giải Ba (3/6 số)"
                }
                else -> "✅ Trúng $matchCount/${profile.evalPicks} số (${profile.prizeName})"
            }
        } else {
            "Chưa trúng ($matchCount/${profile.evalPicks} số, cần ≥${profile.minMatches})"
        }
        return WinEvaluation(
            isWin = isWin,
            matchCount = matchCount,
            evalPicks = profile.evalPicks,
            picks = picks,
            matchedNumbers = matched,
            actualNumbers = actualDraw.uniqueNumbers,
            specialNumber = actualDraw.specialNumber,
            prizeDetail = detail
        )
    }

    fun buildNumberDetail(
        number: Int,
        prediction: PredictionResult,
        allDraws: List<LotteryDraw>
    ): NumberDetailInfo {
        val history = allDraws.filter { it.date.isBefore(prediction.targetDate) }
        val d30 = prediction.targetDate.minusDays(30)
        val d100 = prediction.targetDate.minusDays(100)
        val d365 = prediction.targetDate.minusDays(365)

        var freq30 = 0
        var freq100 = 0
        var freq365 = 0
        val appearanceDates = mutableListOf<LocalDate>()

        for (draw in history) {
            if (number in draw.uniqueNumbers) {
                appearanceDates.add(draw.date)
                if (!draw.date.isBefore(d30)) freq30++
                if (!draw.date.isBefore(d100)) freq100++
                if (!draw.date.isBefore(d365)) freq365++
            }
        }

        val lastDate = appearanceDates.lastOrNull()
        val daysSinceLast = lastDate?.let {
            ChronoUnit.DAYS.between(it, prediction.targetDate).toInt()
        }
        val avgInterval = if (appearanceDates.size >= 2) {
            val totalSpan = ChronoUnit.DAYS.between(appearanceDates.first(), appearanceDates.last()).toDouble()
            totalSpan / (appearanceDates.size - 1)
        } else {
            null
        }

        val rankIdx = prediction.rankedNumbers.indexOfFirst { it.first == number }
        val rank = if (rankIdx >= 0) rankIdx + 1 else 0
        val totalScore = prediction.scores[number] ?: BASE_SCORE

        return NumberDetailInfo(
            number = number,
            totalScore = totalScore,
            rank = rank,
            contributions = prediction.contributionsByNumber[number].orEmpty(),
            freq30Days = freq30,
            freq100Days = freq100,
            freq365Days = freq365,
            daysSinceLast = daysSinceLast,
            avgIntervalDays = avgInterval
        )
    }

    fun evaluateCombinedPerformance(
        fromDate: LocalDate,
        toDate: LocalDate,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile,
        algorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        stationFilter: XsmbStation = XsmbStation.ALL,
        stationOnlyHistory: Boolean = false
    ): CombinedPerformanceStats {
        val useStationFilter = profile.id == "xsmb" && stationFilter != XsmbStation.ALL
        val targetDraws = allDraws.filter { draw ->
            !draw.date.isBefore(fromDate) &&
                !draw.date.isAfter(toDate) &&
                (!useStationFilter || stationFilter.matchesDate(draw.date))
        }
        val stationDesc = if (profile.id == "xsmb") stationFilter.displayName else profile.shortName
        if (targetDraws.isEmpty()) {
            return CombinedPerformanceStats(
                totalDays = 0,
                top1Hits = 0,
                top3Hits = 0,
                top5Hits = 0,
                top10Hits = 0,
                specialTop3Hits = 0,
                profileWinHits = 0,
                fromDate = fromDate,
                toDate = toDate,
                stationLabel = stationDesc
            )
        }

        val enabledAlgos = algorithms.filter { prefsMap[it.id]?.enabled ?: true }
        enabledAlgos.forEach { it.resetSessionState() }

        // Warm up stateful previousTops cleanly on draws right before the verification window
        val firstTargetDate = targetDraws.first().date
        val prePool = allDraws.filter {
            it.date.isBefore(firstTargetDate) &&
                (!useStationFilter || !stationOnlyHistory || stationFilter.matchesDate(it.date))
        }
        if (prePool.size > 1) {
            val warmStart = (prePool.size - 5).coerceAtLeast(1)
            for (algo in enabledAlgos) {
                if (algo is AppearancePointAlgorithm) {
                    val algoParams = prefsMap[algo.id]?.params ?: emptyMap()
                    for (idx in warmStart until prePool.size) {
                        algo.predict(prePool[idx].date, prePool.subList(0, idx), profile, algoParams)
                    }
                }
            }
        }

        var evaluatedDays = 0
        var top1 = 0
        var top3 = 0
        var top5 = 0
        var top10 = 0
        var specialTop3 = 0
        var specialTop10 = 0
        var profileWins = 0
        var totalNhayTop3 = 0
        val dayLogs = ArrayList<HistoricalVerificationDay>(targetDraws.size)

        for (draw in targetDraws) {
            val historyBefore = if (useStationFilter && stationOnlyHistory) {
                allDraws.filter { it.date.isBefore(draw.date) && stationFilter.matchesDate(it.date) }
            } else {
                allDraws.filter { it.date.isBefore(draw.date) }
            }
            if (historyBefore.isEmpty()) continue
            evaluatedDays++

            val combined = LinkedHashMap<Int, Double>()
            for (n in profile.validRange) combined[n] = BASE_SCORE
            for (algo in enabledAlgos) {
                val pref = prefsMap[algo.id]
                val weight = if (pref?.weightEnabled == true) pref.weightValue else 1.0
                val raw = algo.predict(draw.date, historyBefore, profile, pref?.params ?: emptyMap())
                for ((n, v) in raw) {
                    combined[n] = (combined[n] ?: BASE_SCORE) + v * weight
                }
            }
            val rankedPairs = combined.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
                .map { it.key to it.value }
            val ranked = rankedPairs.map { it.first }
            val top10Pairs = rankedPairs.take(10)
            val actual = draw.uniqueNumbers
            val hitCounts = HashMap<Int, Int>()
            for (n in draw.allNumbers) {
                hitCounts[n] = (hitCounts[n] ?: 0) + 1
            }

            val top3Nums = ranked.take(3)
            val top5Nums = ranked.take(5)
            val top10Nums = ranked.take(10)
            val m3 = top3Nums.filter { it in actual }.toSet()
            val m5 = top5Nums.filter { it in actual }.toSet()
            val m10 = top10Nums.filter { it in actual }.toSet()
            val nhay3 = top3Nums.sumOf { hitCounts[it] ?: 0 }
            val spHit3 = draw.specialNumber != null && draw.specialNumber in top3Nums
            val spHit10 = draw.specialNumber != null && draw.specialNumber in top10Nums
            val winEval = evaluateWin(profile, ranked, draw)

            if (ranked.take(1).any { it in actual }) top1++
            if (m3.isNotEmpty()) top3++
            if (m5.isNotEmpty()) top5++
            if (m10.isNotEmpty()) top10++
            if (spHit3) specialTop3++
            if (spHit10) specialTop10++
            if (winEval.isWin) profileWins++
            totalNhayTop3 += nhay3

            dayLogs.add(
                HistoricalVerificationDay(
                    date = draw.date,
                    stationName = if (profile.id == "xsmb") XsmbStation.forDate(draw.date).shortName else profile.shortName,
                    predictedTop10 = top10Pairs,
                    actualSpecial = draw.specialNumber,
                    actualUniqueNumbers = actual,
                    actualHitCounts = hitCounts,
                    matchedTop3 = m3,
                    matchedTop5 = m5,
                    matchedTop10 = m10,
                    totalNhayTop3 = nhay3,
                    specialHitTop3 = spHit3,
                    specialHitTop10 = spHit10,
                    isProfileWin = winEval.isWin,
                    prizeDetail = winEval.prizeDetail,
                    actualPrizeMap = draw.prizeMap
                )
            )
        }

        return CombinedPerformanceStats(
            totalDays = evaluatedDays,
            top1Hits = top1,
            top3Hits = top3,
            top5Hits = top5,
            top10Hits = top10,
            specialTop3Hits = specialTop3,
            profileWinHits = profileWins,
            totalNhayTop3 = totalNhayTop3,
            specialTop10Hits = specialTop10,
            fromDate = targetDraws.first().date,
            toDate = targetDraws.last().date,
            stationLabel = stationDesc,
            dayLogs = dayLogs.sortedByDescending { it.date }
        )
    }

    fun buildSevenDayComparison(
        targetDrawsDesc: List<LotteryDraw>,
        allDrawsSortedAsc: List<LotteryDraw>,
        algorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        sourceLabel: String,
        fetchedFromNetwork: Boolean,
        fetchedAt: ZonedDateTime,
        errorMessage: String? = null,
        useStationHistory: Boolean = false
    ): SevenDayComparisonSummary {
        val profile = LotteryProfiles.XSMB
        val enabledAlgos = algorithms.filter { prefsMap[it.id]?.enabled ?: true }
        val drawsAsc = targetDrawsDesc.sortedBy { it.date }
        val rowsAsc = mutableListOf<SevenDayComparisonRow>()

        // Reset session state once and step oldest -> newest when evaluating all draws sequentially
        enabledAlgos.forEach { it.resetSessionState() }

        for (draw in drawsAsc) {
            val station = XsmbStation.forDate(draw.date)
            val historyBefore = if (useStationHistory) {
                allDrawsSortedAsc.filter { it.date.isBefore(draw.date) && station.matchesDate(it.date) }
            } else {
                allDrawsSortedAsc.filter { it.date.isBefore(draw.date) }
            }

            val totalScores = LinkedHashMap<Int, Double>(100)
            for (n in profile.validRange) totalScores[n] = BASE_SCORE

            if (historyBefore.isNotEmpty() && enabledAlgos.isNotEmpty()) {
                for (algo in enabledAlgos) {
                    if (useStationHistory) {
                        algo.resetSessionState()
                    }
                    val pref = prefsMap[algo.id]
                    val algoParams = pref?.params ?: emptyMap()
                    val weight = if (pref?.weightEnabled == true) pref.weightValue else 1.0
                    val rawMap = algo.predict(draw.date, historyBefore, profile, algoParams)
                    for ((num, raw) in rawMap) {
                        totalScores[num] = (totalScores[num] ?: BASE_SCORE) + raw * weight
                    }
                }
            }

            val ranked = totalScores.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Double>> { it.value }.thenBy { it.key })
                .map { it.key to it.value }

            val top10 = ranked.take(10)
            val top3Nums = top10.take(3).map { it.first }
            val top5Nums = top10.take(5).map { it.first }
            val top10Nums = top10.map { it.first }

            val actualSet = draw.uniqueNumbers
            val hitCounts = HashMap<Int, Int>()
            for (n in draw.allNumbers) {
                hitCounts[n] = (hitCounts[n] ?: 0) + 1
            }

            val hits3 = top3Nums.filter { it in actualSet }.toSet()
            val hits5 = top5Nums.filter { it in actualSet }.toSet()
            val hits10 = top10Nums.filter { it in actualSet }.toSet()
            val sp = draw.specialNumber
            val totalNhay3 = top3Nums.sumOf { hitCounts[it] ?: 0 }

            rowsAsc.add(
                SevenDayComparisonRow(
                    date = draw.date,
                    actualSpecial = sp,
                    actualUniqueNumbers = actualSet,
                    actualHitCounts = hitCounts,
                    predictedTop10 = top10,
                    hitNumbersInTop3 = hits3,
                    hitNumbersInTop5 = hits5,
                    hitNumbersInTop10 = hits10,
                    specialHitInTop3 = sp != null && sp in top3Nums,
                    specialHitInTop10 = sp != null && sp in top10Nums,
                    totalNhayInTop3 = totalNhay3,
                    prizeMap = draw.prizeMap
                )
            )
        }

        val fetchedAtStr = fetchedAt.withZoneSameInstant(VN_ZONE_ID)
            .format(DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"))

        return SevenDayComparisonSummary(
            rows = rowsAsc.sortedByDescending { it.date },
            sourceLabel = sourceLabel,
            fetchedFromNetwork = fetchedFromNetwork,
            fetchedAtLabel = fetchedAtStr,
            errorMessage = errorMessage
        )
    }

    fun buildWeeklyStationSchedulePredictions(
        nowVn: ZonedDateTime,
        latestLocalDate: LocalDate?,
        allDrawsSortedAsc: List<LotteryDraw>,
        algorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        stationOnlyHistory: Boolean = true
    ): List<StationDailyPrediction> {
        if (allDrawsSortedAsc.isEmpty()) return emptyList()
        val profile = LotteryProfiles.XSMB
        val today = nowVn.toLocalDate()
        val nowTime = nowVn.toLocalTime()
        val drawTime = LocalTime.of(18, 15)
        val liveEnd = LocalTime.of(18, 35)
        val todayAlreadyRecorded = latestLocalDate != null && !latestLocalDate.isBefore(today)
        val startDate = if (todayAlreadyRecorded || !nowTime.isBefore(liveEnd)) {
            today.plusDays(1)
        } else {
            today
        }

        val drawByDate = allDrawsSortedAsc.associateBy { it.date }
        val result = ArrayList<StationDailyPrediction>(7)

        for (offset in 0..6) {
            val date = startDate.plusDays(offset.toLong())
            val station = XsmbStation.forDate(date)
            val pred = predictForDate(
                targetDate = date,
                allDraws = allDrawsSortedAsc,
                profile = profile,
                algorithms = algorithms,
                prefsMap = prefsMap,
                warmUpTop3Days = 4,
                stationFilter = station,
                stationOnlyHistory = stationOnlyHistory
            )
            val historyCount = if (stationOnlyHistory) {
                allDrawsSortedAsc.count { it.date.isBefore(date) && station.matchesDate(it.date) }
            } else {
                allDrawsSortedAsc.count { it.date.isBefore(date) }
            }
            val isToday = date == today
            val isLive = isToday && !todayAlreadyRecorded && !nowTime.isBefore(drawTime) && nowTime.isBefore(liveEnd)
            result.add(
                StationDailyPrediction(
                    date = date,
                    station = station,
                    isTodayDraw = isToday,
                    isLiveWindow = isLive,
                    stationHistoryCount = historyCount,
                    top3 = pred.top3,
                    top5 = pred.rankedNumbers.take(5),
                    actualDraw = drawByDate[date]
                )
            )
        }
        return result
    }

    fun buildThirtyDayAccuracyTrend(
        allDrawsSortedAsc: List<LotteryDraw>,
        profile: LotteryProfile,
        algorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        stationFilter: XsmbStation = XsmbStation.ALL,
        stationOnlyHistory: Boolean = false,
        maxDraws: Int = 30
    ): ThirtyDayAccuracyChartData? {
        val useStation = profile.id == "xsmb" && stationFilter != XsmbStation.ALL
        val matchingPool = if (useStation) {
            allDrawsSortedAsc.filter { stationFilter.matchesDate(it.date) }
        } else {
            allDrawsSortedAsc
        }
        if (matchingPool.size < 2) return null

        val targetSlice = matchingPool.takeLast(maxDraws.coerceAtLeast(1))
        val evalStats = evaluateCombinedPerformance(
            fromDate = targetSlice.first().date,
            toDate = targetSlice.last().date,
            allDraws = allDrawsSortedAsc,
            profile = profile,
            algorithms = algorithms,
            prefsMap = prefsMap,
            stationFilter = stationFilter,
            stationOnlyHistory = stationOnlyHistory
        )
        return buildThirtyDayAccuracyTrendFromLogs(
            dayLogs = evalStats.dayLogs,
            stationLabel = if (profile.id == "xsmb") stationFilter.displayName else profile.shortName
        )
    }

    fun buildThirtyDayAccuracyTrendFromLogs(
        dayLogs: List<HistoricalVerificationDay>,
        stationLabel: String,
        maxPoints: Int = 30
    ): ThirtyDayAccuracyChartData? {
        val chronoLogs = dayLogs.sortedBy { it.date }.takeLast(maxPoints.coerceAtLeast(1))
        if (chronoLogs.size < 2) return null

        var cum3 = 0
        var cum5 = 0
        var cum10 = 0
        val points = ArrayList<AccuracyTrendPoint>(chronoLogs.size)

        for (i in chronoLogs.indices) {
            val day = chronoLogs[i]
            if (day.isWinTop3) cum3++
            if (day.isWinTop5) cum5++
            if (day.isWinTop10) cum10++

            val countSoFar = (i + 1).toDouble()
            val wStart = (i - 6).coerceAtLeast(0)
            val window = chronoLogs.subList(wStart, i + 1)
            val wSize = window.size.toDouble()
            val roll3 = window.count { it.isWinTop3 } * 100.0 / wSize
            val roll5 = window.count { it.isWinTop5 } * 100.0 / wSize
            val roll10 = window.count { it.isWinTop10 } * 100.0 / wSize

            points.add(
                AccuracyTrendPoint(
                    dayIndex = i,
                    date = day.date,
                    stationName = day.stationName,
                    hitTop3 = day.isWinTop3,
                    hitTop5 = day.isWinTop5,
                    hitTop10 = day.isWinTop10,
                    totalNhayTop3 = day.totalNhayTop3,
                    specialHitTop10 = day.specialHitTop10,
                    cumulativeTop3Pct = cum3 * 100.0 / countSoFar,
                    cumulativeTop5Pct = cum5 * 100.0 / countSoFar,
                    cumulativeTop10Pct = cum10 * 100.0 / countSoFar,
                    rolling7Top3Pct = roll3,
                    rolling7Top5Pct = roll5,
                    rolling7Top10Pct = roll10
                )
            )
        }

        val total = points.size.toDouble()
        return ThirtyDayAccuracyChartData(
            points = points,
            stationLabel = stationLabel,
            overallTop3Pct = cum3 * 100.0 / total,
            overallTop5Pct = cum5 * 100.0 / total,
            overallTop10Pct = cum10 * 100.0 / total,
            top3Hits = cum3,
            top5Hits = cum5,
            top10Hits = cum10
        )
    }

    fun buildXsmbDashboardSummary(
        stats: CombinedPerformanceStats,
        windowDrawsRequested: Int
    ): XsmbDashboardSummary {
        val total = stats.totalDays.coerceAtLeast(0)
        val chrono = stats.dayLogs.sortedBy { it.date }
        val recent7 = chrono.takeLast(7)
        val rCount = recent7.size.toDouble().coerceAtLeast(1.0)

        var longestStreak = 0
        var running = 0
        for (day in chrono) {
            if (day.isWinTop3) {
                running++
                if (running > longestStreak) longestStreak = running
            } else {
                running = 0
            }
        }
        val currentStreak = running

        val breakdowns = XsmbStation.PROVINCES.mapNotNull { station ->
            val rows = chrono.filter { station.matchesDate(it.date) }
            if (rows.isEmpty()) {
                null
            } else {
                XsmbStationAccuracyRow(
                    station = station,
                    totalDraws = rows.size,
                    top3Hits = rows.count { it.isWinTop3 },
                    top5Hits = rows.count { it.isWinTop5 },
                    top10Hits = rows.count { it.isWinTop10 },
                    specialTop10Hits = rows.count { it.specialHitTop10 }
                )
            }
        }

        return XsmbDashboardSummary(
            stats = stats,
            windowDrawsRequested = windowDrawsRequested,
            top1AccuracyPct = stats.top1Rate,
            top3AccuracyPct = stats.top3Rate,
            top5AccuracyPct = stats.top5Rate,
            top10AccuracyPct = stats.top10Rate,
            specialTop10AccuracyPct = if (total > 0) stats.specialTop10Hits * 100.0 / total else 0.0,
            avgNhayTop3 = if (total > 0) stats.totalNhayTop3.toDouble() / total else 0.0,
            recent7Top3AccuracyPct = if (recent7.isNotEmpty()) recent7.count { it.isWinTop3 } * 100.0 / rCount else 0.0,
            recent7Top5AccuracyPct = if (recent7.isNotEmpty()) recent7.count { it.isWinTop5 } * 100.0 / rCount else 0.0,
            recent7Top10AccuracyPct = if (recent7.isNotEmpty()) recent7.count { it.isWinTop10 } * 100.0 / rCount else 0.0,
            longestTop3WinStreak = longestStreak,
            currentTop3WinStreak = currentStreak,
            stationBreakdowns = breakdowns
        )
    }
}
