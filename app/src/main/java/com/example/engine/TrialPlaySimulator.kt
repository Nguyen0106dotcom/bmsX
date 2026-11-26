package com.example.engine

import com.example.data.AlgorithmPreference
import com.example.data.BetStrategy
import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.SimulationDayLog
import com.example.data.SimulationSummary
import com.example.data.TrialBetType
import java.time.LocalDate

class TrialPlaySimulator {

    fun runSimulation(
        initialCapital: Double,
        drawsToSimulateCount: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile,
        selectedAlgorithms: List<LotteryAlgorithm>,
        prefsMap: Map<String, AlgorithmPreference>,
        strategies: List<BetStrategy>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): SimulationSummary {
        if (allDraws.size < 2 || selectedAlgorithms.isEmpty() || strategies.isEmpty()) {
            return SimulationSummary(initialCapital, initialCapital, 0, 0, 0, 0.0, 0.0, emptyList())
        }

        // Pair each prediction base draw i with actual next draw i+1
        val candidatePairs = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until allDraws.size - 1) {
            val d = allDraws[i].date
            val inRange = if (startDate != null && endDate != null) {
                !d.isBefore(startDate) && !d.isAfter(endDate)
            } else true
            if (inRange) candidatePairs.add(i to (i + 1))
        }

        val pairsToRun = if (drawsToSimulateCount != null && startDate == null) {
            candidatePairs.takeLast(drawsToSimulateCount.coerceAtLeast(1))
        } else {
            candidatePairs
        }

        if (pairsToRun.isEmpty()) {
            return SimulationSummary(initialCapital, initialCapital, 0, 0, 0, 0.0, 0.0, emptyList())
        }

        selectedAlgorithms.forEach { it.resetSessionState() }

        var balance = initialCapital
        var totalCost = 0.0
        var totalWin = 0.0
        var winDays = 0
        var lossDays = 0
        val logs = mutableListOf<SimulationDayLog>()
        var stopped = false

        pairsToRun.forEachIndexed { stepIdx, (baseIdx, actualIdx) ->
            if (stopped) return@forEachIndexed
            val baseDraw = allDraws[baseIdx]
            val actualDraw = allDraws[actualIdx]

            if (balance < 1_000.0) {
                logs.add(
                    SimulationDayLog(
                        predictDate = baseDraw.date,
                        actualDate = actualDraw.date,
                        details = listOf("⛔ Dừng giả lập: Đã hết vốn khả dụng!"),
                        cost = 0.0,
                        win = 0.0,
                        balanceAfter = balance
                    )
                )
                stopped = true
                return@forEachIndexed
            }

            val historyForPredict = allDraws.subList(0, baseIdx + 1)
            val combined = LinkedHashMap<Int, Double>()
            for (n in profile.validRange) combined[n] = PredictionEngine.BASE_SCORE
            for (algo in selectedAlgorithms) {
                val pref = prefsMap[algo.id]
                val w = if (pref?.weightEnabled == true) pref.weightValue else 1.0
                val raw = algo.predict(actualDraw.date, historyForPredict, profile, pref?.params ?: emptyMap())
                for ((n, v) in raw) {
                    combined[n] = (combined[n] ?: PredictionEngine.BASE_SCORE) + v * w
                }
            }
            val ranked = combined.entries.sortedByDescending { it.value }.map { it.key }

            val hitCounts = HashMap<Int, Int>()
            for (n in actualDraw.allNumbers) hitCounts[n] = (hitCounts[n] ?: 0) + 1
            val specialNum = actualDraw.specialNumber

            var dayCost = 0.0
            var dayWin = 0.0
            val dayDetails = mutableListOf<String>()

            for (strategy in strategies) {
                if (stopped) break
                val targetNumbers = strategy.positions.mapNotNull { pos -> ranked.getOrNull(pos - 1) }
                if (targetNumbers.size != strategy.positions.size) continue

                val unitCost = strategy.points * strategy.costPerPoint
                val unitWinBase = strategy.points * strategy.winPerPoint

                when (strategy.type) {
                    TrialBetType.DE, TrialBetType.LO -> {
                        for (num in targetNumbers) {
                            if (balance < unitCost) {
                                dayDetails.add("⛔ Không đủ vốn cho số %02d".format(num))
                                stopped = true
                                break
                            }
                            balance -= unitCost
                            dayCost += unitCost

                            var winVal = 0.0
                            val statusText = if (strategy.type == TrialBetType.DE) {
                                if (num == specialNum) {
                                    winVal = unitWinBase
                                    "🎯 TRÚNG ĐỀ"
                                } else "Trượt"
                            } else {
                                val hits = hitCounts[num] ?: 0
                                if (hits > 0) {
                                    winVal = unitWinBase * hits
                                    if (hits > 1) "🎯 TRÚNG ($hits nháy)" else "🎯 TRÚNG"
                                } else "Trượt"
                            }

                            balance += winVal
                            dayWin += winVal
                            dayDetails.add(
                                "[${strategy.type.label}] %02d: %s (Chi %,.0fđ · Thu %,.0fđ)".format(
                                    num, statusText, unitCost, winVal
                                )
                            )
                        }
                    }

                    TrialBetType.XIEN_2, TrialBetType.XIEN_3, TrialBetType.XIEN_4 -> {
                        if (balance < unitCost) {
                            dayDetails.add("⛔ Không đủ vốn cho ${strategy.type.label}")
                            stopped = true
                            break
                        }
                        balance -= unitCost
                        dayCost += unitCost

                        val allHit = targetNumbers.all { (hitCounts[it] ?: 0) > 0 }
                        var winVal = 0.0
                        val numsLabel = targetNumbers.joinToString("-") { "%02d".format(it) }
                        val statusText = if (allHit) {
                            val mult = targetNumbers.fold(1) { acc, n -> acc * (hitCounts[n] ?: 1) }
                            winVal = unitWinBase * mult
                            "🎯 TRÚNG XIÊN (x$mult)"
                        } else {
                            "Trượt"
                        }

                        balance += winVal
                        dayWin += winVal
                        dayDetails.add(
                            "[${strategy.type.label}] $numsLabel: $statusText (Chi %,.0fđ · Thu %,.0fđ)".format(
                                unitCost, winVal
                            )
                        )
                    }
                }
            }

            totalCost += dayCost
            totalWin += dayWin
            val dayProfit = dayWin - dayCost
            if (dayProfit > 0) winDays++ else if (dayProfit < 0) lossDays++

            logs.add(
                SimulationDayLog(
                    predictDate = baseDraw.date,
                    actualDate = actualDraw.date,
                    details = dayDetails,
                    cost = dayCost,
                    win = dayWin,
                    balanceAfter = balance
                )
            )
            onProgress(stepIdx + 1, pairsToRun.size)
        }

        return SimulationSummary(
            initialCapital = initialCapital,
            finalBalance = balance,
            totalDays = logs.size,
            winDays = winDays,
            lossDays = lossDays,
            totalCost = totalCost,
            totalWin = totalWin,
            logs = logs.reversed()
        )
    }
}
