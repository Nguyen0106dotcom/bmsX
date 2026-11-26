package com.example.data

import android.util.JsonReader
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

data class Last7DaysApiResult(
    val draws: List<LotteryDraw>,
    val sourceLabel: String,
    val fetchedFromNetwork: Boolean,
    val fetchedAt: ZonedDateTime,
    val errorMessage: String? = null
)

/**
 * Fetches the last 7 days of XSMB results from public APIs (zero API key required):
 * 1. xskt.com.vn official XSMB RSS feed (lightweight, real-time last ~7-10 draws)
 * 2. Public GitHub XSMB 2-digits JSON datasets (khiemdoan / junlangzi)
 * 3. Direct daily HTML prize table on xoso.com.vn for any remaining gap days
 */
class XsmbPublicApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchLast7DaysXsmb(localFallbackDraws: List<LotteryDraw>): Last7DaysApiResult =
        withContext(Dispatchers.IO) {
            val nowVn = ZonedDateTime.now(VN_ZONE_ID)
            val collectedByDate = LinkedHashMap<LocalDate, LotteryDraw>()
            val sourceTags = mutableListOf<String>()
            val errors = mutableListOf<String>()

            // 1. Primary lightweight public RSS feed (xskt.com.vn)
            val rssDraws = fetchFromXsktRss(nowVn.toLocalDate())
            if (rssDraws.isNotEmpty()) {
                rssDraws.forEach { collectedByDate[it.date] = it }
                sourceTags.add("xskt.com.vn RSS")
            } else {
                errors.add("RSS")
            }

            // 2. Public GitHub XSMB JSON APIs if we still have < 7 recent draws
            if (collectedByDate.size < 7) {
                val jsonDraws = fetchRecentFromPublicJsonApis()
                if (jsonDraws.isNotEmpty()) {
                    jsonDraws.forEach { draw ->
                        if (draw.date !in collectedByDate) {
                            collectedByDate[draw.date] = draw
                        }
                    }
                    sourceTags.add("GitHub Public JSON")
                } else {
                    errors.add("GitHub JSON")
                }
            }

            // 3. Direct daily HTML fallback for any missing date in the last 7 scheduled days
            val latestTargetDay = if (nowVn.toLocalTime().isBefore(LocalTime.of(18, 30))) {
                nowVn.toLocalDate().minusDays(1)
            } else {
                nowVn.toLocalDate()
            }
            var d = latestTargetDay.minusDays(6)
            var htmlFilled = 0
            while (!d.isAfter(latestTargetDay)) {
                if (d !in collectedByDate) {
                    fetchXsmbFromXosoHtml(d)?.let { draw ->
                        collectedByDate[draw.date] = draw
                        htmlFilled++
                    }
                }
                d = d.plusDays(1)
            }
            if (htmlFilled > 0) {
                sourceTags.add("xoso.com.vn")
            }

            if (collectedByDate.isNotEmpty()) {
                val sortedLatest7 = collectedByDate.values
                    .sortedBy { it.date }
                    .takeLast(7)
                return@withContext Last7DaysApiResult(
                    draws = sortedLatest7,
                    sourceLabel = sourceTags.joinToString(" + "),
                    fetchedFromNetwork = true,
                    fetchedAt = nowVn,
                    errorMessage = null
                )
            }

            // Offline / unreachable fallback: clearly label as local cache
            val localLatest7 = localFallbackDraws.sortedBy { it.date }.takeLast(7)
            Last7DaysApiResult(
                draws = localLatest7,
                sourceLabel = "Bộ nhớ cục bộ (Ngoại tuyến)",
                fetchedFromNetwork = false,
                fetchedAt = nowVn,
                errorMessage = "Không thể kết nối Public API (${errors.joinToString(", ")}). Đang hiển thị 7 kỳ gần nhất đã lưu."
            )
        }

    private fun fetchFromXsktRss(referenceDate: LocalDate): List<LotteryDraw> {
        val xml = httpGet("https://xskt.com.vn/rss-feed/mien-bac-xsmb.rss") ?: return emptyList()
        val itemRegex = Regex("""<item>(.*?)</item>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val titleRegex = Regex("""<title>(.*?)</title>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val descRegex = Regex("""<description>(.*?)</description>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val dateRegex = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{4}))?""")

        val out = mutableListOf<LotteryDraw>()
        for (match in itemRegex.findAll(xml)) {
            val block = match.groupValues[1]
            val rawTitle = decodeXmlEntities(titleRegex.find(block)?.groupValues?.get(1).orEmpty())
            val rawDesc = decodeXmlEntities(descRegex.find(block)?.groupValues?.get(1).orEmpty())

            val dateMatch = dateRegex.find(rawTitle) ?: continue
            val day = dateMatch.groupValues[1].toIntOrNull() ?: continue
            val month = dateMatch.groupValues[2].toIntOrNull() ?: continue
            val year = dateMatch.groupValues[3].toIntOrNull() ?: inferYear(referenceDate, month, day)
            val date = try {
                LocalDate.of(year, month, day)
            } catch (_: Exception) {
                continue
            }

            val parsedDraw = parseRssDescriptionToDraw(date, rawDesc)
            if (parsedDraw != null) out.add(parsedDraw)
        }
        return out.sortedBy { it.date }.takeLast(7)
    }

    private fun inferYear(ref: LocalDate, month: Int, day: Int): Int {
        val candidate = try {
            LocalDate.of(ref.year, month, day)
        } catch (_: Exception) {
            return ref.year
        }
        return if (candidate.isAfter(ref.plusDays(7))) ref.year - 1 else ref.year
    }

    private fun parseRssDescriptionToDraw(date: LocalDate, rawDesc: String): LotteryDraw? {
        val cleaned = rawDesc
            .replace(Regex("<[^>]+>"), " ")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Typical xskt RSS description:
        // "ĐB: 12345 1: 23456 2: 34567 - 45678 3: ... 7: 12 - 34 - 56 - 78"
        val markers = listOf(
            "ĐB:" to "special",
            "1:" to "prize1",
            "2:" to "prize2",
            "3:" to "prize3",
            "4:" to "prize4",
            "5:" to "prize5",
            "6:" to "prize6",
            "7:" to "prize7"
        )

        val positions = markers.mapNotNull { (token, key) ->
            val idx = cleaned.indexOf(token)
            if (idx >= 0) Triple(idx, token.length, key) else null
        }.sortedBy { it.first }

        if (positions.isEmpty()) return null

        val prizeMap = LinkedHashMap<String, MutableList<String>>()
        val allNumbers = mutableListOf<Int>()
        var special: Int? = null

        for (i in positions.indices) {
            val (startIdx, tokLen, groupKey) = positions[i]
            val contentStart = startIdx + tokLen
            val contentEnd = if (i + 1 < positions.size) positions[i + 1].first else cleaned.length
            val segment = cleaned.substring(contentStart, contentEnd)
            val tokens = Regex("""\d{2,5}""").findAll(segment).map { it.value }.toList()
            for (tok in tokens) {
                val two = extractLastTwoDigits(tok) ?: continue
                allNumbers.add(two)
                if (groupKey == "special" && special == null) special = two
                prizeMap.getOrPut(groupKey) { mutableListOf() }.add("%02d".format(two))
            }
        }

        if (allNumbers.size < 20 || special == null) return null
        return LotteryDraw(
            date = date,
            drawId = "",
            allNumbers = allNumbers,
            uniqueNumbers = allNumbers.toSet(),
            specialNumber = special,
            prizeMap = prizeMap
        )
    }

    private fun fetchRecentFromPublicJsonApis(): List<LotteryDraw> {
        val urls = listOf(
            "https://raw.githubusercontent.com/khiemdoan/vietnam-lottery-xsmb-analysis/refs/heads/main/data/xsmb-2-digits.json",
            "https://raw.githubusercontent.com/junlangzi/data-kqxs/refs/heads/main/data/xsmb-2-digits.json"
        )
        for (url in urls) {
            val body = httpGet(url) ?: continue
            val parsed = parseXsmbJsonStringTail(body, 7)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun parseXsmbJsonStringTail(jsonText: String, limit: Int): List<LotteryDraw> {
        val ring = ArrayDeque<LotteryDraw>(limit + 1)
        try {
            JsonReader(StringReader(jsonText)).use { reader ->
                reader.isLenient = true
                if (reader.peek() != JsonToken.BEGIN_ARRAY) return emptyList()
                reader.beginArray()
                while (reader.hasNext()) {
                    var date: LocalDate? = null
                    var special: Int? = null
                    val prizeMap = LinkedHashMap<String, MutableList<String>>()
                    val allNumbers = ArrayList<Int>(27)

                    reader.beginObject()
                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull()
                            continue
                        }
                        if (key == "date") {
                            val raw = reader.nextString().substringBefore('T').substringBefore(' ').trim()
                            date = try {
                                LocalDate.parse(raw)
                            } catch (_: Exception) {
                                null
                            }
                        } else if (key in IGNORED_KEYS) {
                            reader.skipValue()
                        } else {
                            val valStr = when (reader.peek()) {
                                JsonToken.NUMBER -> "%02d".format(reader.nextInt())
                                JsonToken.STRING -> reader.nextString().trim()
                                else -> {
                                    reader.skipValue()
                                    ""
                                }
                            }
                            val two = extractLastTwoDigits(valStr)
                            if (two != null) {
                                allNumbers.add(two)
                                if (key == "special" || key == "prize_special" || key == "dac_biet") {
                                    special = two
                                }
                                prizeMap.getOrPut(normalizeXsmbGroup(key)) { mutableListOf() }
                                    .add("%02d".format(two))
                            }
                        }
                    }
                    reader.endObject()

                    if (date != null && allNumbers.isNotEmpty()) {
                        if (ring.size >= limit) ring.removeFirst()
                        ring.addLast(
                            LotteryDraw(
                                date = date,
                                drawId = "",
                                allNumbers = allNumbers,
                                uniqueNumbers = allNumbers.toSet(),
                                specialNumber = special,
                                prizeMap = prizeMap
                            )
                        )
                    }
                }
                reader.endArray()
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return ring.toList()
    }

    private fun fetchXsmbFromXosoHtml(date: LocalDate): LotteryDraw? {
        val dmy = "%02d-%02d-%04d".format(date.dayOfMonth, date.monthValue, date.year)
        val html = httpGet("https://xoso.com.vn/xsmb-$dmy.html") ?: return null
        if (!html.contains("special-prize", ignoreCase = true)) return null

        val groups = listOf(
            "special-prize" to "special",
            "prize1" to "prize1",
            "prize2" to "prize2",
            "prize3" to "prize3",
            "prize4" to "prize4",
            "prize5" to "prize5",
            "prize6" to "prize6",
            "prize7" to "prize7"
        )

        val prizeMap = LinkedHashMap<String, MutableList<String>>()
        val allNumbers = mutableListOf<Int>()
        var special: Int? = null

        for ((cssClass, groupKey) in groups) {
            val regex = Regex("""class\s*=\s*["']?${Regex.escape(cssClass)}["']?[^>]*>([^<]+)""", RegexOption.IGNORE_CASE)
            val nums = regex.findAll(html).mapNotNull { m ->
                extractLastTwoDigits(m.groupValues[1].filter { it.isDigit() })
            }.toList()
            for (n in nums) {
                allNumbers.add(n)
                if (groupKey == "special" && special == null) special = n
                prizeMap.getOrPut(groupKey) { mutableListOf() }.add("%02d".format(n))
            }
        }
        return if (allNumbers.size >= 20 && special != null) {
            LotteryDraw(
                date = date,
                drawId = "",
                allNumbers = allNumbers,
                uniqueNumbers = allNumbers.toSet(),
                specialNumber = special,
                prizeMap = prizeMap
            )
        } else {
            null
        }
    }

    private fun httpGet(url: String): String? = try {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (_: Exception) {
        null
    }

    private fun decodeXmlEntities(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("<![CDATA[", "")
        .replace("]]>", "")

    private fun normalizeXsmbGroup(key: String): String = when {
        key == "special" || key == "prize_special" || key == "dac_biet" -> "special"
        key == "prize1" -> "prize1"
        key.startsWith("prize2") -> "prize2"
        key.startsWith("prize3") -> "prize3"
        key.startsWith("prize4") -> "prize4"
        key.startsWith("prize5") -> "prize5"
        key.startsWith("prize6") -> "prize6"
        key.startsWith("prize7") -> "prize7"
        else -> key
    }

    private fun extractLastTwoDigits(raw: String): Int? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val tail = if (digits.length >= 2) digits.takeLast(2) else digits
        return tail.toIntOrNull()?.takeIf { it in 0..99 }
    }

    fun toXsmbJsonArray(draws: List<LotteryDraw>): JSONArray {
        val arr = JSONArray()
        val keyOrderByGroup = mapOf(
            "special" to listOf("special"),
            "prize1" to listOf("prize1"),
            "prize2" to listOf("prize2_1", "prize2_2"),
            "prize3" to listOf("prize3_1", "prize3_2", "prize3_3", "prize3_4", "prize3_5", "prize3_6"),
            "prize4" to listOf("prize4_1", "prize4_2", "prize4_3", "prize4_4"),
            "prize5" to listOf("prize5_1", "prize5_2", "prize5_3", "prize5_4", "prize5_5", "prize5_6"),
            "prize6" to listOf("prize6_1", "prize6_2", "prize6_3"),
            "prize7" to listOf("prize7_1", "prize7_2", "prize7_3", "prize7_4")
        )
        for (draw in draws) {
            val obj = JSONObject()
            obj.put("date", "${draw.date}T00:00:00.000")
            for ((group, keys) in keyOrderByGroup) {
                val vals = draw.prizeMap[group].orEmpty()
                keys.forEachIndexed { idx, jsonKey ->
                    vals.getOrNull(idx)?.toIntOrNull()?.let { obj.put(jsonKey, it) }
                }
            }
            if (obj.has("special")) arr.put(obj)
        }
        return arr
    }

    companion object {
        private val IGNORED_KEYS = setOf(
            "date", "_id", "source", "day_of_week", "sign",
            "created_at", "updated_at", "province_name", "province_id", "id"
        )
    }
}
