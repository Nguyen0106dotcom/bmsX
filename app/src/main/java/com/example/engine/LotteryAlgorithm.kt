package com.example.engine

import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AlgorithmParamSpec(
    val key: String,
    val label: String,
    val defaultValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val step: Double,
    val isInt: Boolean = false
)

interface LotteryAlgorithm {
    val id: String
    val name: String
    val description: String
    val paramSpecs: List<AlgorithmParamSpec>

    fun resetSessionState() {}

    fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double>
}

/**
 * Port chuẩn xác thuật toán thuat_toan_01.py (appearance_based_optimized_v13)
 * và biến thể tối ưu optimized_optimized_thuat_toan_01.
 */
class AppearancePointAlgorithm(
    override val id: String,
    override val name: String,
    override val description: String,
    private val defaultParams: Map<String, Double>
) : LotteryAlgorithm {

    private val previousTops = ArrayDeque<List<Int>>()

    override val paramSpecs: List<AlgorithmParamSpec> = listOf(
        AlgorithmParamSpec("short_term_days", "Số ngày ngắn hạn", defaultParams["short_term_days"] ?: 14.0, 5.0, 35.0, 1.0, true),
        AlgorithmParamSpec("frequency_window_short", "Cửa sổ tần suất ngắn", defaultParams["frequency_window_short"] ?: 45.0, 15.0, 90.0, 1.0, true),
        AlgorithmParamSpec("frequency_window_long", "Cửa sổ tần suất dài", defaultParams["frequency_window_long"] ?: 180.0, 60.0, 365.0, 5.0, true),
        AlgorithmParamSpec("base_point_short", "Điểm cơ bản ngắn hạn", defaultParams["base_point_short"] ?: 3.5, 0.5, 8.0, 0.1),
        AlgorithmParamSpec("frequency_weight_short", "Trọng số tần suất ngắn", defaultParams["frequency_weight_short"] ?: 0.5, 0.0, 2.0, 0.05),
        AlgorithmParamSpec("frequency_weight_long", "Trọng số tần suất dài", defaultParams["frequency_weight_long"] ?: 0.25, 0.0, 2.0, 0.05),
        AlgorithmParamSpec("neighbor_bonus", "Thưởng láng giềng ĐB", defaultParams["neighbor_bonus"] ?: 1.2, 0.0, 4.0, 0.1),
        AlgorithmParamSpec("neighbor_range", "Biên độ láng giềng", defaultParams["neighbor_range"] ?: 5.0, 1.0, 10.0, 1.0, true),
        AlgorithmParamSpec("special_multiplier", "Hệ số giải Đặc biệt", defaultParams["special_multiplier"] ?: 2.0, 1.0, 4.0, 0.1),
        AlgorithmParamSpec("repeat_penalty_top", "Phạt lặp lại Top 3", defaultParams["repeat_penalty_top"] ?: -0.5, -3.0, 0.0, 0.1),
        AlgorithmParamSpec("bonus_long_absence", "Thưởng số gan lâu", defaultParams["bonus_long_absence"] ?: 0.8, 0.0, 3.0, 0.1)
    )

    override fun resetSessionState() {
        previousTops.clear()
    }

    override fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double> {
        val scores = LinkedHashMap<Int, Double>()
        for (n in profile.validRange) scores[n] = 0.0
        if (historyBeforeDate.isEmpty()) return scores

        fun p(key: String) = params[key] ?: defaultParams[key] ?: 0.0

        val shortTermDays = p("short_term_days").toInt().coerceAtLeast(1)
        val freqWindowShort = p("frequency_window_short").toInt().coerceAtLeast(1)
        val freqWindowLong = p("frequency_window_long").toInt().coerceAtLeast(1)
        val basePointShort = p("base_point_short")
        val freqWeightShort = p("frequency_weight_short")
        val freqWeightLong = p("frequency_weight_long")
        val neighborBonus = p("neighbor_bonus")
        val neighborRange = p("neighbor_range").toInt().coerceAtLeast(0)
        val increment = p("increment")
        val bonusAfter3Days = p("bonus_after_3_days")
        val bonusLongAbsence = p("bonus_long_absence")
        val deductionLastDay = p("deduction_if_appeared_last_day")
        val repeatPenaltyTop = p("repeat_penalty_top")
        val repeatWindow = p("repeat_window").toInt().coerceIn(1, 14)
        val repeatThresholdPenalty = p("repeat_threshold_penalty")
        val repeatThreshold = p("repeat_threshold").toInt().coerceAtLeast(1)
        val repeatStreakPenalty = p("repeat_streak_penalty")
        val bonusFreq5 = p("bonus_freq_5")
        val cycle7Bonus = p("cycle_7_bonus")
        val cycle30Bonus = p("cycle_30_bonus")
        val specialMultiplier = p("special_multiplier")
        val specialFreqMultiplier = p("special_freq_multiplier")

        val lastDraw = historyBeforeDate.last()
        val gapToTarget = ChronoUnit.DAYS.between(lastDraw.date, dateToPredict)
        val effectiveRefDate = if (gapToTarget > 3L) lastDraw.date.plusDays(1) else dateToPredict

        val shortTermStart = effectiveRefDate.minusDays(shortTermDays.toLong())
        val freqShortStart = effectiveRefDate.minusDays(freqWindowShort.toLong())
        val freqLongStart = effectiveRefDate.minusDays(freqWindowLong.toLong())

        val freqShort = IntArray(100)
        val freqLong = IntArray(100)
        val specialFreqShort = IntArray(100)

        for (i in historyBeforeDate.indices.reversed()) {
            val draw = historyBeforeDate[i]
            val daysAgo = ChronoUnit.DAYS.between(draw.date, effectiveRefDate).toInt()
            if (daysAgo < 1) continue

            if (!draw.date.isBefore(shortTermStart)) {
                val decay = 1.0 - 0.08 * daysAgo
                for (num in draw.uniqueNumbers) {
                    if (num in scores) {
                        var pt = basePointShort * decay
                        if (draw.specialNumber == num) pt *= specialMultiplier
                        scores[num] = (scores[num] ?: 0.0) + pt
                    }
                }
            }

            if (!draw.date.isBefore(freqShortStart)) {
                for (num in draw.uniqueNumbers) {
                    if (num in 0..99) freqShort[num]++
                }
                val sp = draw.specialNumber
                if (sp != null && sp in 0..99) specialFreqShort[sp]++
            }

            if (!draw.date.isBefore(freqLongStart)) {
                for (num in draw.uniqueNumbers) {
                    if (num in 0..99) freqLong[num]++
                }
            }

            if (daysAgo <= 28 && daysAgo % 7 == 0) {
                for (num in draw.uniqueNumbers) {
                    if (num in scores) scores[num] = (scores[num] ?: 0.0) + cycle7Bonus
                }
            }
            if (daysAgo <= 180 && daysAgo % 30 == 0) {
                for (num in draw.uniqueNumbers) {
                    if (num in scores) scores[num] = (scores[num] ?: 0.0) + cycle30Bonus
                }
            }
        }

        for (num in profile.validRange) {
            var add = freqWeightShort * freqShort[num] + freqWeightLong * freqLong[num]
            if (freqShort[num] > 5) add += bonusFreq5
            if (specialFreqShort[num] > 5) add += specialFreqMultiplier
            scores[num] = (scores[num] ?: 0.0) + add
        }

        val specialLast = lastDraw.specialNumber
        if (specialLast != null && neighborRange > 0) {
            val span = (profile.maxVal - profile.minVal + 1)
            for (offset in -neighborRange..neighborRange) {
                if (offset == 0) continue
                val neighbor = if (profile.minVal == 0) {
                    ((specialLast + offset) % 100 + 100) % 100
                } else {
                    profile.minVal + (((specialLast - profile.minVal + offset) % span + span) % span)
                }
                if (neighbor in scores) {
                    scores[neighbor] = (scores[neighbor] ?: 0.0) +
                        neighborBonus * (1.0 - 0.1 * kotlin.math.abs(offset))
                }
            }
        }

        // Absence calculation (ordered newest -> oldest)
        val daysSinceMap = IntArray(100) { historyBeforeDate.size }
        val seen = BooleanArray(100)
        var remaining = 0
        for (n in profile.validRange) if (n in 0..99) remaining++
        var idxBack = 0
        for (i in historyBeforeDate.indices.reversed()) {
            if (remaining <= 0) break
            for (num in historyBeforeDate[i].uniqueNumbers) {
                if (num in 0..99 && !seen[num]) {
                    seen[num] = true
                    daysSinceMap[num] = idxBack
                    remaining--
                }
            }
            idxBack++
        }

        for (num in profile.validRange) {
            val daysSince = daysSinceMap[num]
            var add = daysSince * increment
            if (daysSince >= 3) add += bonusAfter3Days
            if (daysSince >= 15 && freqLong[num] > 10) add += bonusLongAbsence
            scores[num] = (scores[num] ?: 0.0) + add
        }

        if (ChronoUnit.DAYS.between(lastDraw.date, dateToPredict) == 1L) {
            for (num in lastDraw.uniqueNumbers) {
                if (num in scores) scores[num] = (scores[num] ?: 0.0) + deductionLastDay
            }
        }

        // Repeat Top-3 penalty across recent prediction calls
        if (previousTops.isNotEmpty()) {
            val recent = previousTops.takeLast(repeatWindow)
            val repeatCounts = IntArray(100)
            val streakCounts = IntArray(100)
            val revRecent = recent.reversed()
            for (i in revRecent.indices) {
                val prevTop = revRecent[i]
                for (num in prevTop) {
                    if (num in 0..99) {
                        repeatCounts[num]++
                        if (i == 0 || num !in revRecent[i - 1]) {
                            if (streakCounts[num] == 0) streakCounts[num] = 1
                        } else {
                            streakCounts[num]++
                        }
                    }
                }
            }
            for (num in profile.validRange) {
                val rc = repeatCounts[num]
                val sc = streakCounts[num]
                if (rc > 0) {
                    var pen = repeatPenaltyTop * rc
                    if (rc >= repeatThreshold) pen += repeatThresholdPenalty
                    if (sc > 1) pen += repeatStreakPenalty * (sc - 1)
                    scores[num] = (scores[num] ?: 0.0) + pen
                }
            }
        }

        val currentTop3 = scores.entries.sortedByDescending { it.value }.take(3).map { it.key }
        previousTops.addLast(currentTop3)
        while (previousTops.size > repeatWindow) previousTops.removeFirst()

        return scores
    }

    companion object {
        val STANDARD_DEFAULTS = mapOf(
            "short_term_days" to 14.0,
            "frequency_window_short" to 45.0,
            "frequency_window_long" to 180.0,
            "base_point_short" to 3.5,
            "base_point_long" to 0.3,
            "frequency_weight_short" to 0.5,
            "frequency_weight_long" to 0.25,
            "neighbor_bonus" to 1.2,
            "neighbor_range" to 5.0,
            "increment" to 0.005,
            "bonus_after_3_days" to 0.07,
            "bonus_long_absence" to 0.8,
            "deduction_if_appeared_last_day" to -0.02,
            "repeat_penalty_top" to -0.5,
            "repeat_window" to 7.0,
            "repeat_threshold_penalty" to -2.0,
            "repeat_threshold" to 2.0,
            "repeat_streak_penalty" to -0.8,
            "bonus_freq_5" to 0.25,
            "cycle_7_bonus" to 0.2,
            "cycle_30_bonus" to 0.3,
            "special_multiplier" to 2.0,
            "special_freq_multiplier" to 3.0
        )

        val OPTIMIZED_DEFAULTS = STANDARD_DEFAULTS + mapOf(
            "short_term_days" to 20.0,
            "frequency_window_short" to 37.0,
            "frequency_window_long" to 225.0,
            "base_point_short" to 3.675,
            "frequency_weight_short" to 0.4,
            "special_multiplier" to 2.1
        )
    }
}

/**
 * Port chuẩn xác thuat_toan_test_01.py (DaysSinceLastAppearanceAlgorithm)
 */
class DaysSinceLastAlgorithm : LotteryAlgorithm {
    override val id: String = "days_since_last"
    override val name: String = "Điểm Lô Gan & Mốc Thời Gian"
    override val description: String =
        "Cộng điểm theo số ngày chưa xuất hiện, mốc thưởng (3/7/10/15), cộng dồn cấp số cộng và thưởng lặp ngày cuối."

    private val defaults = mapOf(
        "base_increment_per_day" to 0.1,
        "milestone_bonus" to 0.1,
        "last_day_multiplier" to 0.05,
        "progressive_start_day" to 15.0,
        "progressive_increment" to 0.1
    )

    override val paramSpecs: List<AlgorithmParamSpec> = listOf(
        AlgorithmParamSpec("base_increment_per_day", "Điểm cộng mỗi ngày vắng", 0.1, 0.01, 1.0, 0.02),
        AlgorithmParamSpec("milestone_bonus", "Thưởng tại mỗi mốc", 0.1, 0.0, 2.0, 0.05),
        AlgorithmParamSpec("last_day_multiplier", "Hệ số xuất hiện ngày cuối", 0.05, -0.5, 1.0, 0.05),
        AlgorithmParamSpec("progressive_start_day", "Mốc bắt đầu cộng dồn", 15.0, 5.0, 30.0, 1.0, true),
        AlgorithmParamSpec("progressive_increment", "Gia tăng cộng dồn/ngày", 0.1, 0.0, 0.5, 0.02)
    )

    override fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double> {
        val scores = LinkedHashMap<Int, Double>()
        for (n in profile.validRange) scores[n] = 0.0
        if (historyBeforeDate.isEmpty()) return scores

        fun p(key: String) = params[key] ?: defaults[key] ?: 0.0
        val baseInc = p("base_increment_per_day")
        val milestoneBonus = p("milestone_bonus")
        val lastDayMult = p("last_day_multiplier")
        val progStartDay = p("progressive_start_day").toInt().coerceAtLeast(1)
        val progInc = p("progressive_increment")
        val milestones = listOf(3, 7, 10, 15)

        val daysAbsent = IntArray(100) { historyBeforeDate.size }
        val seen = BooleanArray(100)
        var offset = 0
        for (i in historyBeforeDate.indices.reversed()) {
            for (num in historyBeforeDate[i].uniqueNumbers) {
                if (num in 0..99 && !seen[num]) {
                    seen[num] = true
                    daysAbsent[num] = offset
                }
            }
            offset++
        }

        for (num in profile.validRange) {
            val absent = daysAbsent[num]
            var sc = absent * baseInc
            for (m in milestones) {
                if (absent >= m) sc += milestoneBonus
            }
            if (absent >= progStartDay) {
                val n = absent - progStartDay
                sc += progInc * n * (n + 1) / 2.0
            }
            scores[num] = sc
        }

        val lastDraw = historyBeforeDate.last()
        for (num in lastDraw.allNumbers) {
            if (num in scores) {
                scores[num] = (scores[num] ?: 0.0) + lastDayMult
            }
        }
        return scores
    }
}

/**
 * Port chuẩn xác thuat_toan_test_02.py (PrizePositionPenaltyAlgorithm)
 */
class PrizePositionPenaltyAlgorithm : LotteryAlgorithm {
    override val id: String = "prize_position_penalty"
    override val name: String = "Phạt Vị Trí Giải Kỳ Trước"
    override val description: String =
        "Trừ điểm theo vị trí giải thưởng đảo ngược (Giải 7 = 1 → Đặc biệt = 27) của kỳ quay gần nhất."

    override val paramSpecs: List<AlgorithmParamSpec> = listOf(
        AlgorithmParamSpec("penalty_multiplier", "Hệ số phạt vị trí", 0.1, 0.0, 1.0, 0.02)
    )

    override fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double> {
        val scores = LinkedHashMap<Int, Double>()
        for (n in profile.validRange) scores[n] = 0.0
        val lastDraw = historyBeforeDate.lastOrNull() ?: return scores

        val penaltyMult = params["penalty_multiplier"] ?: 0.1
        if (lastDraw.prizeMap.isNotEmpty() && profile.id == "xsmb") {
            for ((group, values) in lastDraw.prizeMap) {
                val basePosList = when (group) {
                    "special" -> listOf(27)
                    "prize1" -> listOf(26)
                    "prize2" -> listOf(25, 24)
                    "prize3" -> listOf(23, 22, 21, 20, 19, 18)
                    "prize4" -> listOf(17, 16, 15, 14)
                    "prize5" -> listOf(13, 12, 11, 10, 9, 8)
                    "prize6" -> listOf(7, 6, 5)
                    "prize7" -> listOf(4, 3, 2, 1)
                    else -> emptyList()
                }
                values.forEachIndexed { idx, raw ->
                    val revPos = basePosList.getOrNull(idx) ?: return@forEachIndexed
                    val num = raw.toIntOrNull() ?: return@forEachIndexed
                    if (num in scores) {
                        scores[num] = (scores[num] ?: 0.0) - penaltyMult * revPos
                    }
                }
            }
        } else {
            val total = lastDraw.allNumbers.size
            lastDraw.allNumbers.forEachIndexed { idx, num ->
                if (num in scores) {
                    val revPos = (total - idx).coerceAtLeast(1)
                    scores[num] = (scores[num] ?: 0.0) - penaltyMult * revPos
                }
            }
        }
        return scores
    }
}

/**
 * Port chuẩn xác thuat_toan_test_03.py (ThirtyDayFrequencyPenaltyAlgorithm)
 */
class ThirtyDayFreqPenaltyAlgorithm : LotteryAlgorithm {
    override val id: String = "thirty_day_freq_penalty"
    override val name: String = "Phạt Tần Suất 30 Ngày"
    override val description: String =
        "Thưởng cho các số chưa xuất hiện trong 30 ngày gần nhất và phạt theo nhóm tần suất tăng dần."

    override val paramSpecs: List<AlgorithmParamSpec> = listOf(
        AlgorithmParamSpec("history_days", "Số ngày thống kê", 30.0, 10.0, 90.0, 1.0, true),
        AlgorithmParamSpec("min_appearance_penalty", "Phạt nhóm xuất hiện ít nhất", -0.1, -2.0, 0.0, 0.05),
        AlgorithmParamSpec("penalty_increment", "Bước tăng mức phạt", -0.1, -1.0, 0.0, 0.05),
        AlgorithmParamSpec("never_appeared_bonus", "Thưởng số vắng mặt hoàn toàn", 0.5, 0.0, 3.0, 0.1)
    )

    override fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double> {
        val scores = LinkedHashMap<Int, Double>()
        for (n in profile.validRange) scores[n] = 0.0

        val histDays = (params["history_days"] ?: 30.0).toInt().coerceAtLeast(1)
        val minPenalty = params["min_appearance_penalty"] ?: -0.1
        val penaltyInc = params["penalty_increment"] ?: -0.1
        val neverBonus = params["never_appeared_bonus"] ?: 0.5

        val lastDrawDate = historyBeforeDate.lastOrNull()?.date
        val effectiveRefDate = if (lastDrawDate != null && ChronoUnit.DAYS.between(lastDrawDate, dateToPredict) > 3L) {
            lastDrawDate.plusDays(1)
        } else {
            dateToPredict
        }
        val startLimit = effectiveRefDate.minusDays(histDays.toLong())
        val windowDraws = historyBeforeDate.filter { !it.date.isBefore(startLimit) }
        if (windowDraws.isEmpty()) {
            for (n in profile.validRange) scores[n] = neverBonus
            return scores
        }

        val counts = IntArray(100)
        for (draw in windowDraws) {
            for (num in draw.uniqueNumbers) {
                if (num in 0..99) counts[num]++
            }
        }

        val groups = sortedMapOf<Int, MutableList<Int>>()
        for (num in profile.validRange) {
            val c = counts[num]
            if (c == 0) {
                scores[num] = neverBonus
            } else {
                groups.getOrPut(c) { mutableListOf() }.add(num)
            }
        }

        var currentPenalty = minPenalty
        for ((_, nums) in groups) {
            for (num in nums) {
                scores[num] = (scores[num] ?: 0.0) + currentPenalty
            }
            currentPenalty += penaltyInc
        }
        return scores
    }
}
