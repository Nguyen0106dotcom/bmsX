package com.example.engine

import com.example.data.DailyKqxsStats
import com.example.data.DeepAnalysisResult
import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.MonthBreakdown
import com.example.data.YearBreakdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalyticsEngine {

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun computeDailyStats(
        targetDraw: LotteryDraw,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile
    ): DailyKqxsStats {
        val counts = HashMap<Int, Int>()
        for (n in targetDraw.allNumbers) {
            counts[n] = (counts[n] ?: 0) + 1
        }
        val multiHits = counts.entries
            .filter { it.value >= 2 }
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

        val targetIdx = allDraws.indexOfFirst { it.date == targetDraw.date && it.drawId == targetDraw.drawId }
        val prevDraw = if (targetIdx > 0) allDraws[targetIdx - 1] else null
        val loRoi = if (prevDraw != null) {
            targetDraw.uniqueNumbers.filter { it in prevDraw.uniqueNumbers }.sorted()
        } else {
            emptyList()
        }

        // Compute days absent strictly before this draw for each valid number
        val historyBefore = if (targetIdx > 0) allDraws.subList(0, targetIdx) else emptyList()
        val gapBeforeToday = HashMap<Int, Int>()
        for (n in profile.validRange) {
            var gap = historyBefore.size
            for (i in historyBefore.indices.reversed()) {
                if (n in historyBefore[i].uniqueNumbers) {
                    gap = historyBefore.size - 1 - i
                    break
                }
            }
            gapBeforeToday[n] = gap
        }

        val ganDaVe = targetDraw.uniqueNumbers
            .mapNotNull { n ->
                val g = gapBeforeToday[n] ?: 0
                if (g >= 10) n to g else null
            }
            .sortedByDescending { it.second }

        // Current gan lì (after this draw finishes)
        val historyUpToToday = if (targetIdx >= 0) allDraws.subList(0, targetIdx + 1) else allDraws
        val topGanLi = profile.validRange.map { n ->
            var gap = historyUpToToday.size
            for (i in historyUpToToday.indices.reversed()) {
                if (n in historyUpToToday[i].uniqueNumbers) {
                    gap = historyUpToToday.size - 1 - i
                    break
                }
            }
            n to gap
        }.sortedByDescending { it.second }.take(5)

        val heads = (0..9).associateWith { mutableListOf<Int>() }
        val tails = (0..9).associateWith { mutableListOf<Int>() }
        for (n in targetDraw.allNumbers.sorted()) {
            if (n in 0..99) {
                heads[n / 10]?.add(n % 10)
                tails[n % 10]?.add(n / 10)
            }
        }

        return DailyKqxsStats(
            multiHits = multiHits,
            loRoi = loRoi,
            topGanLi = topGanLi,
            ganDaVe = ganDaVe,
            headCounts = heads,
            tailCounts = tails
        )
    }

    fun analyzeNumbers(
        queriedNumbers: List<Int>,
        drawsInRange: List<LotteryDraw>
    ): DeepAnalysisResult {
        val targetSet = queriedNumbers.toSet()
        val totalDraws = drawsInRange.size
        if (totalDraws == 0 || targetSet.isEmpty()) {
            return DeepAnalysisResult(
                queriedNumbers = queriedNumbers,
                totalDrawsAnalyzed = 0,
                drawsWithHits = 0,
                totalHitCount = 0,
                hitPercentage = 0.0,
                avgDrawsPerHit = 0.0,
                specialHits = 0,
                maxGap = 0,
                maxGapRange = "-",
                currentGap = 0,
                maxStreak = 0,
                maxStreakRange = "-",
                maxHitsInOneDraw = 0,
                maxHitsDates = emptyList(),
                topCoOccurring = emptyList(),
                yearBreakdown = emptyList()
            )
        }

        var drawsWithHits = 0
        var totalHitCount = 0
        var specialHits = 0

        var maxGap = 0
        var maxGapRange = "-"
        var gapCounter = 0
        var gapStartDate: LocalDate? = null

        var maxStreak = 0
        var maxStreakRange = "-"
        var streakCounter = 0
        var streakStartDate: LocalDate? = null

        var maxHitsInOneDraw = 0
        val maxHitsDates = mutableListOf<LocalDate>()
        val coOccurringCounts = IntArray(100)

        // Year -> Month -> list of (date, hitCount)
        val tree = sortedMapOf<Int, MutableMap<Int, MutableList<Pair<LocalDate, Int>>>>(compareByDescending { it })

        for (draw in drawsInRange) {
            val hitsInDraw = draw.allNumbers.count { it in targetSet }
            if (draw.specialNumber != null && draw.specialNumber in targetSet) {
                specialHits++
            }

            if (hitsInDraw > 0) {
                drawsWithHits++
                totalHitCount += hitsInDraw

                if (hitsInDraw > maxHitsInOneDraw) {
                    maxHitsInOneDraw = hitsInDraw
                    maxHitsDates.clear()
                    maxHitsDates.add(draw.date)
                } else if (hitsInDraw == maxHitsInOneDraw) {
                    maxHitsDates.add(draw.date)
                }

                // End gap
                if (gapCounter > maxGap) {
                    maxGap = gapCounter
                    maxGapRange = "${gapStartDate?.format(dateFmt) ?: "?"} → ${draw.date.format(dateFmt)}"
                }
                gapCounter = 0
                gapStartDate = null

                // Continue streak
                if (streakCounter == 0) streakStartDate = draw.date
                streakCounter++
                if (streakCounter > maxStreak) {
                    maxStreak = streakCounter
                    maxStreakRange = "${streakStartDate?.format(dateFmt) ?: "?"} → ${draw.date.format(dateFmt)}"
                }

                // Co-occurring numbers
                for (num in draw.uniqueNumbers) {
                    if (num in 0..99 && num !in targetSet) {
                        coOccurringCounts[num]++
                    }
                }

                val mMap = tree.getOrPut(draw.date.year) { sortedMapOf(compareByDescending { it }) }
                val mList = mMap.getOrPut(draw.date.monthValue) { mutableListOf() }
                mList.add(draw.date to hitsInDraw)
            } else {
                // End streak
                streakCounter = 0
                streakStartDate = null

                // Continue gap
                if (gapCounter == 0) gapStartDate = draw.date
                gapCounter++
                if (gapCounter > maxGap) {
                    maxGap = gapCounter
                    maxGapRange = "${gapStartDate?.format(dateFmt) ?: "?"} → ${draw.date.format(dateFmt)}"
                }
            }
        }

        val currentGap = gapCounter
        val hitPct = drawsWithHits * 100.0 / totalDraws
        val avgDrawsPerHit = if (drawsWithHits > 0) totalDraws.toDouble() / drawsWithHits else 0.0

        val topCoOccurring = (0..99)
            .map { it to coOccurringCounts[it] }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)

        val yearBreakdown = tree.entries.map { (year, monthsMap) ->
            val monthList = monthsMap.entries.map { (month, items) ->
                MonthBreakdown(
                    month = month,
                    totalHits = items.sumOf { it.second },
                    drawDatesWithHit = items.sortedByDescending { it.first }
                )
            }
            YearBreakdown(
                year = year,
                totalHits = monthList.sumOf { it.totalHits },
                months = monthList
            )
        }

        return DeepAnalysisResult(
            queriedNumbers = queriedNumbers,
            totalDrawsAnalyzed = totalDraws,
            drawsWithHits = drawsWithHits,
            totalHitCount = totalHitCount,
            hitPercentage = hitPct,
            avgDrawsPerHit = avgDrawsPerHit,
            specialHits = specialHits,
            maxGap = maxGap,
            maxGapRange = maxGapRange,
            currentGap = currentGap,
            maxStreak = maxStreak,
            maxStreakRange = maxStreakRange,
            maxHitsInOneDraw = maxHitsInOneDraw,
            maxHitsDates = maxHitsDates.takeLast(10).reversed(),
            topCoOccurring = topCoOccurring,
            yearBreakdown = yearBreakdown
        )
    }
}
