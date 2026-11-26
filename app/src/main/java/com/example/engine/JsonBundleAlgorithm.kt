package com.example.engine

import com.example.data.LotteryDraw
import com.example.data.LotteryProfile
import com.example.data.StoredBundleRecord
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Port chuẩn xác modules/json_bundle.py:
 * hỗ trợ evaluate_appearance_formula, compute_ranks, và nạp JSON Bundle từ AI Studio / tùy chỉnh.
 */
class JsonBundleAlgorithm(
    private val bundleRecord: StoredBundleRecord
) : LotteryAlgorithm {

    override val id: String = "bundle_${bundleRecord.id}"
    override val name: String = bundleRecord.name
    override val description: String = bundleRecord.description
    override val paramSpecs: List<AlgorithmParamSpec> = emptyList()

    private data class SubFormula(
        val id: String,
        val weight: Double,
        val params: Map<String, Double>
    )

    private val normalizationMode: String
    private val formulas: List<SubFormula>

    init {
        var mode = "rank"
        val parsed = mutableListOf<SubFormula>()
        try {
            val root = JSONObject(bundleRecord.rawJson)
            val cfg = root.optJSONObject("config")
            if (cfg != null) {
                mode = cfg.optString("normalization_mode", "rank")
            }
            val bestInd = root.optJSONObject("best_individual")
            val bestWeights = bestInd?.optJSONObject("weights")
            val bestParams = bestInd?.optJSONObject("params")

            val rawArr = root.optJSONArray("active_algorithms") ?: root.optJSONArray("formulas")
            if (rawArr != null) {
                for (i in 0 until rawArr.length()) {
                    val fObj = rawArr.optJSONObject(i) ?: continue
                    val fId = fObj.optString("id", "f_$i")
                    val baseWeight = fObj.optDouble("weight", 1.0)
                    val weight = if (bestWeights != null && bestWeights.has(fId)) {
                        bestWeights.optDouble(fId, baseWeight)
                    } else {
                        baseWeight
                    }
                    val pMap = mutableMapOf<String, Double>()
                    val pObj = fObj.optJSONObject("parameters")
                    if (pObj != null) {
                        val keys = pObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = pObj.optDouble(k, Double.NaN)
                            if (!v.isNaN()) pMap[k] = v
                        }
                    }
                    val overrideObj = bestParams?.optJSONObject(fId)
                    if (overrideObj != null) {
                        val keys = overrideObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = overrideObj.optDouble(k, Double.NaN)
                            if (!v.isNaN()) pMap[k] = v
                        }
                    }
                    parsed.add(SubFormula(id = fId, weight = weight, params = pMap))
                }
            }
        } catch (_: Exception) {
        }
        normalizationMode = mode
        formulas = parsed
    }

    override fun predict(
        dateToPredict: LocalDate,
        historyBeforeDate: List<LotteryDraw>,
        profile: LotteryProfile,
        params: Map<String, Double>
    ): Map<Int, Double> {
        val totalScores = LinkedHashMap<Int, Double>()
        for (n in profile.validRange) totalScores[n] = 0.0
        if (historyBeforeDate.isEmpty() || formulas.isEmpty()) return totalScores

        val lastDrawDate = historyBeforeDate.last().date
        val effectiveRefDate = if (ChronoUnit.DAYS.between(lastDrawDate, dateToPredict) > 3L) {
            lastDrawDate.plusDays(1)
        } else {
            dateToPredict
        }

        val drawEntries = ArrayList<Triple<Int, Int?, Set<Int>>>(historyBeforeDate.size)
        val lastSeen = HashMap<Int, Int>()
        for (i in historyBeforeDate.indices.reversed()) {
            val draw = historyBeforeDate[i]
            val daysAgo = ChronoUnit.DAYS.between(draw.date, effectiveRefDate).toInt()
            if (daysAgo < 1) continue
            for (nVal in draw.uniqueNumbers) {
                if (nVal !in lastSeen) lastSeen[nVal] = daysAgo
            }
            drawEntries.add(Triple(daysAgo, draw.specialNumber, draw.uniqueNumbers))
        }
        if (drawEntries.isEmpty()) return totalScores

        for (formula in formulas) {
            val subScores = evaluateAppearanceFormula(
                params = formula.params,
                profile = profile,
                drawEntries = drawEntries,
                lastSeen = lastSeen
            )
            val normalized = if (normalizationMode == "rank") {
                computeRanks(subScores)
            } else {
                val minVal = subScores.values.minOrNull() ?: 0.0
                val maxVal = subScores.values.maxOrNull() ?: 0.0
                val diff = if (maxVal - minVal > 1e-9) maxVal - minVal else 1.0
                subScores.mapValues { (_, v) -> ((v - minVal) / diff) * 100.0 }
            }
            for ((num, normVal) in normalized) {
                totalScores[num] = (totalScores[num] ?: 0.0) + formula.weight * normVal
            }
        }
        // Scale down around 0 delta so it blends cleanly with native algorithms on base score 100
        return totalScores.mapValues { (_, v) -> (v / formulas.size.coerceAtLeast(1)) * 0.1 }
    }

    private fun computeRanks(scores: Map<Int, Double>): Map<Int, Double> {
        val sorted = scores.entries.sortedBy { it.value }
        val n = sorted.size
        if (n == 0) return emptyMap()
        val ranks = LinkedHashMap<Int, Double>(n)
        var i = 0
        while (i < n) {
            var j = i
            while (j < n - 1 && abs(sorted[j + 1].value - sorted[i].value) < 1e-9) {
                j++
            }
            val avgRank = (i + j + 2) / 2.0
            val normRank = (avgRank / n.toDouble()) * 100.0
            for (k in i..j) {
                ranks[sorted[k].key] = normRank
            }
            i = j + 1
        }
        return ranks
    }

    private fun evaluateAppearanceFormula(
        params: Map<String, Double>,
        profile: LotteryProfile,
        drawEntries: List<Triple<Int, Int?, Set<Int>>>,
        lastSeen: Map<Int, Int>
    ): Map<Int, Double> {
        fun d(key: String, def: Double) = params[key] ?: def
        val windowDays = d("window_days", 60.0)
        val decay = params["decay"] ?: params["recency_decay_rate"] ?: 0.95
        val halfLife = (params["lag_half_life"] ?: params["recency_halflife"])?.takeIf { it > 0.0 }
        val expRecency = d("exp_recency", 1.0)
        val recencyMix = params["recency_mix"]
        val specialBoost = d("special_boost", 1.0)
        val allPrizeBoost = d("all_prize_boost", 1.0)

        val gapFloor = d("gap_floor", 0.0)
        val gapCap = d("gap_cap", 100.0)
        val absenceGain = d("absence_gain", 0.0)
        val absencePower = d("absence_power", 1.0)
        val absenceFactor = d("absence_factor", 1.0)
        val coldPenalty = d("cold_penalty", 0.0)
        val hotBoost = d("hot_boost", 0.0)

        val modulo = (params["modulo"] ?: params["modulo_divisor"])?.takeIf { it > 0.0 }
        val moduloGain = params["modulo_gain"] ?: params["modulo_weight"] ?: 0.0
        val phasePeriod = params["phase_period"]?.takeIf { it > 0.0 }
        val phaseGain = d("phase_gain", 0.0)

        val neighborGain = d("neighbor_gain", 0.0)
        val digitGain = d("digit_gain", 0.0)
        val pairCooccurW = d("pair_cooccur_w", 0.0)
        val poissonGain = d("poisson_gain", 0.0)
        val poissonSmoothing = d("poisson_smoothing_factor", 0.1)

        val scale = d("scale", 1.0)
        val globalBias = d("global_bias", 0.0)
        val regLambda = d("regularization_lambda", 0.0)

        val lastDraw = drawEntries.firstOrNull()
        val lastSpecial = lastDraw?.second
        val lastNumbersSet = lastDraw?.third ?: emptySet()

        val recencyScores = DoubleArray(100)
        val windowCounts = IntArray(100)
        var totalWindowAppearances = 0

        for ((daysAgo, spNum, numsSet) in drawEntries) {
            if (daysAgo > windowDays) continue
            val decayFactor = if (halfLife != null) {
                exp(-daysAgo * ln(2.0) / halfLife)
            } else {
                decay.pow(minOf(daysAgo.toDouble(), 100.0))
            }
            for (nVal in numsSet) {
                if (nVal !in 0..99) continue
                val isSpecial = spNum != null && nVal == spNum
                val w = if (isSpecial) specialBoost * decayFactor else allPrizeBoost * decayFactor
                recencyScores[nVal] += w
                windowCounts[nVal]++
                totalWindowAppearances++
            }
        }

        val lambdaPoisson = if (totalWindowAppearances > 0) {
            totalWindowAppearances / 100.0
        } else {
            0.27 * windowDays
        }

        val out = LinkedHashMap<Int, Double>()
        for (num in profile.validRange) {
            var rScore = recencyScores[num]
            if (expRecency != 1.0 && rScore > 0.0) {
                rScore = rScore.pow(expRecency)
            }

            val gap = (lastSeen[num] ?: (windowDays + 10.0).toInt()).toDouble()
            val effGap = (minOf(gap, gapCap) - gapFloor).coerceAtLeast(0.0)
            var aScore = effGap.pow(absencePower) * absenceGain * absenceFactor
            if (coldPenalty > 0.0 && gap > 15.0) aScore -= coldPenalty * (gap - 15.0)
            if (hotBoost > 0.0 && gap <= 3.0) aScore += hotBoost * (4.0 - gap)

            var combined = if (recencyMix != null) {
                recencyMix * rScore + (1.0 - recencyMix) * aScore
            } else {
                rScore + aScore
            }

            if (modulo != null && moduloGain > 0.0) {
                val modVal = (num % modulo) / modulo
                combined += cos(2.0 * PI * modVal) * moduloGain
                if (lastSpecial != null) {
                    val modDiff = (((num - lastSpecial) % modulo + modulo) % modulo) / modulo
                    combined += cos(2.0 * PI * modDiff) * (moduloGain * 0.5)
                }
            }

            if (phasePeriod != null && phaseGain > 0.0) {
                combined += cos(2.0 * PI * (num / phasePeriod)) * phaseGain
            }

            if (lastSpecial != null) {
                if (neighborGain > 0.0) {
                    val diff = abs(num - lastSpecial)
                    val dist = minOf(diff, 100 - diff)
                    if (dist <= 3) combined += neighborGain * (1.0 - 0.25 * dist)
                }
                if (digitGain > 0.0) {
                    val headMatch = if (num / 10 == lastSpecial / 10) 1.0 else 0.0
                    val tailMatch = if (num % 10 == lastSpecial % 10) 1.0 else 0.0
                    combined += digitGain * (headMatch + tailMatch) * 0.5
                }
            }

            if (pairCooccurW > 0.0 && num in lastNumbersSet) {
                combined += pairCooccurW * 0.5
            }

            if (poissonGain > 0.0) {
                val kVal = windowCounts[num].toDouble()
                val denom = sqrt(lambdaPoisson + poissonSmoothing + 1e-6)
                combined += poissonGain * ((kVal - lambdaPoisson) / denom)
            }

            combined += globalBias
            if (regLambda > 0.0) combined *= (1.0 - regLambda)
            combined *= scale
            out[num] = combined
        }
        return out
    }
}
