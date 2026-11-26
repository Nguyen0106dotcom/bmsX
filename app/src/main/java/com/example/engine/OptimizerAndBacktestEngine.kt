package com.example.engine

import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import java.time.LocalDate
import kotlin.math.round

data class PeriodBacktestRow(
    val periodLabel: String,
    val totalDays: Int,
    val top1Rate: Double,
    val top3Rate: Double,
    val top5Rate: Double,
    val top10Rate: Double
)

data class OptimizationReport(
    val algorithmId: String,
    val algorithmName: String,
    val baselineTop3Rate: Double,
    val optimizedTop3Rate: Double,
    val validationDays: Int,
    val optimizedParams: Map<String, Double>
)

class OptimizerAndBacktestEngine {

    /**
     * Port EvaluationWorker (modules/workers.py):
     * Đánh giá một thuật toán theo nhiều chu kỳ (30 kỳ/giai đoạn) cho Top 1/3/5/10.
     */
    fun runMultiPeriodBacktest(
        algorithm: LotteryAlgorithm,
        params: Map<String, Double>,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile,
        startDate: LocalDate,
        endDate: LocalDate,
        periodDays: Int = 30
    ): List<PeriodBacktestRow> {
        val targetDraws = allDraws.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
        if (targetDraws.isEmpty()) return emptyList()

        val rows = mutableListOf<PeriodBacktestRow>()
        val chunks = targetDraws.chunked(periodDays.coerceAtLeast(7))
        for (chunk in chunks) {
            algorithm.resetSessionState()
            var top1 = 0
            var top3 = 0
            var top5 = 0
            var top10 = 0
            var validCount = 0

            for (draw in chunk) {
                val idx = allDraws.indexOf(draw)
                if (idx <= 0) continue
                val history = allDraws.subList(0, idx)
                val scores = algorithm.predict(draw.date, history, profile, params)
                val ranked = scores.entries.sortedByDescending { it.value }.map { it.key }
                val actual = draw.uniqueNumbers
                validCount++
                if (ranked.take(1).any { it in actual }) top1++
                if (ranked.take(3).any { it in actual }) top3++
                if (ranked.take(5).any { it in actual }) top5++
                if (ranked.take(10).any { it in actual }) top10++
            }

            if (validCount > 0) {
                val label = "${chunk.first().date} → ${chunk.last().date}"
                rows.add(
                    PeriodBacktestRow(
                        periodLabel = label,
                        totalDays = validCount,
                        top1Rate = top1 * 100.0 / validCount,
                        top3Rate = top3 * 100.0 / validCount,
                        top5Rate = top5 * 100.0 / validCount,
                        top10Rate = top10 * 100.0 / validCount
                    )
                )
            }
        }
        return rows
    }

    /**
     * Port logic tối ưu tham số thuật toán (modules/optimizer.py):
     * Coordinate ascent nhanh trên tập kỳ quay gần nhất để tối đa hóa tỷ lệ trúng Top 3.
     */
    fun optimizeAlgorithmParameters(
        algorithm: LotteryAlgorithm,
        initialParams: Map<String, Double>,
        allDraws: List<LotteryDraw>,
        profile: LotteryProfile,
        validationCount: Int = 45,
        onProgress: (Float) -> Unit = {}
    ): OptimizationReport {
        val specs = algorithm.paramSpecs
        val current = LinkedHashMap<String, Double>()
        for (spec in specs) {
            current[spec.key] = initialParams[spec.key] ?: spec.defaultValue
        }

        val totalSize = allDraws.size
        val evalDraws = if (totalSize > validationCount + 30) {
            allDraws.subList(totalSize - validationCount, totalSize)
        } else {
            allDraws.drop((totalSize / 3).coerceAtLeast(1))
        }

        fun evaluateHitRate(candidateParams: Map<String, Double>): Double {
            if (evalDraws.isEmpty()) return 0.0
            algorithm.resetSessionState()
            var hits = 0
            var count = 0
            for (draw in evalDraws) {
                val idx = allDraws.indexOf(draw)
                if (idx <= 0) continue
                val scores = algorithm.predict(draw.date, allDraws.subList(0, idx), profile, candidateParams)
                val topK = scores.entries.sortedByDescending { it.value }.take(profile.evalPicks).map { it.key }
                if (topK.any { it in draw.uniqueNumbers }) hits++
                count++
            }
            return if (count > 0) hits * 100.0 / count else 0.0
        }

        val baselineRate = evaluateHitRate(current)
        var bestRate = baselineRate

        if (specs.isNotEmpty() && evalDraws.isNotEmpty()) {
            specs.forEachIndexed { idx, spec ->
                val curVal = current[spec.key] ?: spec.defaultValue
                val delta = (spec.step * 2.0).coerceAtLeast(spec.step)
                val candidates = listOf(
                    (curVal - delta * 2).coerceIn(spec.minValue, spec.maxValue),
                    (curVal - delta).coerceIn(spec.minValue, spec.maxValue),
                    (curVal + delta).coerceIn(spec.minValue, spec.maxValue),
                    (curVal + delta * 2).coerceIn(spec.minValue, spec.maxValue)
                ).map { v ->
                    if (spec.isInt) round(v) else (round(v * 1000.0) / 1000.0)
                }.distinct()

                for (candVal in candidates) {
                    val trialMap = current.toMutableMap().apply { put(spec.key, candVal) }
                    val score = evaluateHitRate(trialMap)
                    if (score > bestRate) {
                        bestRate = score
                        current[spec.key] = candVal
                    }
                }
                onProgress((idx + 1).toFloat() / specs.size.toFloat())
            }
        } else {
            onProgress(1f)
        }

        return OptimizationReport(
            algorithmId = algorithm.id,
            algorithmName = algorithm.name,
            baselineTop3Rate = baselineRate,
            optimizedTop3Rate = bestRate,
            validationDays = evalDraws.size,
            optimizedParams = current
        )
    }
}
