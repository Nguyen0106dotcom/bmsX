package com.example.ai

import com.example.data.StoredBundleRecord
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiGenerationResult(
    val bundle: StoredBundleRecord,
    val summaryNote: String,
    val isPlaceholder: Boolean
)

/**
 * Abstraction for generating custom JsonBundleAlgorithm formulas from natural language ideas.
 */
interface AiRepository {
    val isFirebaseBacked: Boolean
    suspend fun generateAlgorithmBundle(
        prompt: String,
        bundleName: String,
        bundleDescription: String
    ): AiGenerationResult
}

/**
 * Fallback implementation used when Firebase setup was declined by the user.
 * Clearly labels every generated formula as a local sample template requiring Firebase AI Logic for live Gemini synthesis.
 */
class PlaceholderAiRepository : AiRepository {
    override val isFirebaseBacked: Boolean = false

    override suspend fun generateAlgorithmBundle(
        prompt: String,
        bundleName: String,
        bundleDescription: String
    ): AiGenerationResult {
        delay(350)
        val lower = prompt.lowercase()
        val cleanName = bundleName.trim().ifBlank { "Mẫu AI: Phân Tích Đa Tầng" }
        val taggedName = if (cleanName.startsWith("[MẪU]")) cleanName else "[MẪU] $cleanName"
        val bundleId = "sample_ai_${UUID.randomUUID().toString().take(8)}"

        val recencyWeight = if ("ngắn hạn" in lower || "rơi" in lower || "nóng" in lower) 1.45 else 1.0
        val absenceWeight = if ("gan" in lower || "vắng" in lower || "trễ" in lower) 1.55 else 0.85
        val neighborGain = if ("láng giềng" in lower || "kề" in lower || "đặc biệt" in lower || "đề" in lower) 1.35 else 0.75
        val poissonGain = if ("poisson" in lower || "xác suất" in lower || "chuẩn" in lower) 1.15 else 0.6
        val moduloGain = if ("chu kỳ" in lower || "modulo" in lower || "tuần" in lower) 0.9 else 0.45

        val formulasArray = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "sub_recency_decay")
                put("name", "Mẫu Tần Suất Suy Giảm Ngắn Hạn")
                put("weight", recencyWeight)
                put("parameters", JSONObject().apply {
                    put("window_days", if ("ngắn hạn" in lower) 21.0 else 45.0)
                    put("decay", 0.92)
                    put("special_boost", 2.2)
                    put("all_prize_boost", 1.0)
                    put("hot_boost", if ("nóng" in lower || "rơi" in lower) 0.85 else 0.25)
                    put("neighbor_gain", neighborGain)
                    put("digit_gain", 0.55)
                })
            })
            put(JSONObject().apply {
                put("id", "sub_absence_poisson")
                put("name", "Mẫu Lô Gan & Độ Lệch Poisson")
                put("weight", absenceWeight)
                put("parameters", JSONObject().apply {
                    put("window_days", 90.0)
                    put("decay", 0.96)
                    put("absence_gain", if ("gan" in lower) 0.45 else 0.22)
                    put("absence_power", 1.15)
                    put("gap_floor", 3.0)
                    put("gap_cap", 35.0)
                    put("poisson_gain", poissonGain)
                    put("modulo", 7.0)
                    put("modulo_gain", moduloGain)
                })
            })
        }

        val bundleJson = JSONObject().apply {
            put("bundle_id", bundleId)
            put("bundle_name", taggedName)
            put(
                "ai_advisor_summary",
                "[DỮ LIỆU MẪU - Cần Firebase AI Logic cho Gemini trực tiếp] " +
                    (bundleDescription.trim().ifBlank { "Công thức mẫu ghép từ gợi ý: \"$prompt\"" })
            )
            put("config", JSONObject().apply {
                put("normalization_mode", "rank")
                put("top_k", 3)
            })
            put("formulas", formulasArray)
        }.toString(2)

        val record = StoredBundleRecord(
            id = bundleId,
            name = taggedName,
            description = "[MẪU] Cần bật Firebase AI Logic để gọi Gemini thực tế. Công thức mẫu đã tùy biến theo từ khóa phân tích.",
            rawJson = bundleJson,
            isPlaceholderSample = true
        )

        return AiGenerationResult(
            bundle = record,
            summaryNote = "Chế độ xem trước cục bộ (chưa kết nối Firebase Gemini). Công thức mẫu đã được tạo để bạn thử nghiệm chấm điểm & backtest.",
            isPlaceholder = true
        )
    }
}
