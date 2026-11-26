package com.example.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val message: String,
    val totalRecords: Int,
    val addedRecords: Int,
    val latestDate: LocalDate?
)

class LotteryRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val dataDir: File
        get() = File(context.filesDir, "data").apply { if (!exists()) mkdirs() }

    private val drawCache = mutableMapOf<String, List<LotteryDraw>>()

    suspend fun ensureBundledDataSeeded() = withContext(Dispatchers.IO) {
        val assetFiles = try {
            context.assets.list("data")?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        for (fileName in assetFiles) {
            val outFile = File(dataDir, fileName)
            if (!outFile.exists() || outFile.length() == 0L) {
                context.assets.open("data/$fileName").use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    suspend fun loadDraws(profile: LotteryProfile, forceReload: Boolean = false): List<LotteryDraw> =
        withContext(Dispatchers.IO) {
            if (!forceReload) {
                drawCache[profile.id]?.let { return@withContext it }
            }
            ensureBundledDataSeeded()
            val file = File(dataDir, profile.fileName)
            if (!file.exists()) return@withContext emptyList()

            val parsed = if (profile.isJsonl) {
                parseJsonlFile(file, profile)
            } else {
                parseXsmbJsonFile(file)
            }
            val sorted = parsed.sortedWith(compareBy<LotteryDraw> { it.date }.thenBy { it.drawId })
            drawCache[profile.id] = sorted
            sorted
        }

    fun getLocalLatestDate(profile: LotteryProfile): LocalDate? =
        drawCache[profile.id]?.lastOrNull()?.date

    suspend fun syncFromRemote(
        profile: LotteryProfile,
        customUrl: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        ensureBundledDataSeeded()
        val url = customUrl?.trim()?.takeIf { it.isNotBlank() } ?: profile.defaultSyncUrl
        val targetFile = File(dataDir, profile.fileName)
        val beforeDraws = loadDraws(profile, forceReload = false)
        val beforeCount = beforeDraws.size
        var githubError: String? = null

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ThienCoSo-Android/6.2.0")
                .build()

            val bodyText = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    githubError = "Máy chủ trả về HTTP ${response.code}"
                    ""
                } else {
                    response.body?.string().orEmpty()
                }
            }

            if (bodyText.isNotBlank()) {
                val tempFile = File(dataDir, "${profile.fileName}.tmp")
                tempFile.writeText(bodyText, StandardCharsets.UTF_8)
                val remoteDraws = if (profile.isJsonl) {
                    parseJsonlFile(tempFile, profile)
                } else {
                    parseXsmbJsonFile(tempFile)
                }
                if (remoteDraws.isNotEmpty()) {
                    if (profile.isJsonl) {
                        mergeJsonlFiles(existingFile = targetFile, incomingFile = tempFile)
                    } else {
                        mergeXsmbJsonFiles(existingFile = targetFile, incomingFile = tempFile)
                    }
                } else if (githubError == null) {
                    githubError = "Nguồn từ xa không có bản ghi hợp lệ."
                }
                tempFile.delete()
            } else if (githubError == null) {
                githubError = "Dữ liệu tải về trống."
            }
        } catch (e: Exception) {
            githubError = e.localizedMessage ?: "Không thể kết nối máy chủ đồng bộ"
        }

        // For XSMB, crawl missing recent draws up to current Vietnam real-time date
        var crawledCount = 0
        if (profile.id == "xsmb") {
            val afterGithubDraws = loadDraws(profile, forceReload = true)
            val latestAfterGithub = afterGithubDraws.lastOrNull()?.date
            crawledCount = crawlMissingRecentXsmbDraws(targetFile, latestAfterGithub)
        }

        val reloaded = loadDraws(profile, forceReload = true)
        val added = (reloaded.size - beforeCount).coerceAtLeast(0)
        val latestDate = reloaded.lastOrNull()?.date

        if (added == 0 && crawledCount == 0 && githubError != null) {
            SyncResult(
                success = false,
                message = "Lỗi đồng bộ ${profile.shortName}: $githubError",
                totalRecords = reloaded.size,
                addedRecords = 0,
                latestDate = latestDate
            )
        } else {
            SyncResult(
                success = true,
                message = if (added > 0) {
                    "Đã đồng bộ thêm $added kỳ quay mới (${profile.shortName}, mới nhất: ${latestDate ?: "--"})."
                } else {
                    "Dữ liệu ${profile.shortName} đã ở trạng thái mới nhất (${reloaded.size} kỳ)."
                },
                totalRecords = reloaded.size,
                addedRecords = added,
                latestDate = latestDate
            )
        }
    }

    private fun crawlMissingRecentXsmbDraws(targetFile: File, latestLocalDate: LocalDate?): Int {
        val nowVn = java.time.ZonedDateTime.now(VN_ZONE_ID)
        val todayVn = nowVn.toLocalDate()
        val endDate = if (nowVn.toLocalTime().isBefore(java.time.LocalTime.of(18, 30))) {
            todayVn.minusDays(1)
        } else {
            todayVn
        }
        val startDate = if (latestLocalDate != null) {
            maxOf(latestLocalDate.plusDays(1), endDate.minusDays(45))
        } else {
            endDate.minusDays(14)
        }
        if (startDate.isAfter(endDate)) return 0

        val fetchedObjects = mutableListOf<JSONObject>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            fetchXsmbDayFromXoso(d)?.let { fetchedObjects.add(it) }
            d = d.plusDays(1)
        }
        if (fetchedObjects.isEmpty()) return 0

        val arr = JSONArray()
        fetchedObjects.forEach { arr.put(it) }
        val tempFile = File(dataDir, "xsmb-live.tmp")
        tempFile.writeText(arr.toString(), StandardCharsets.UTF_8)
        mergeXsmbJsonFiles(existingFile = targetFile, incomingFile = tempFile)
        tempFile.delete()
        return fetchedObjects.size
    }

    private fun fetchXsmbDayFromXoso(date: LocalDate): JSONObject? {
        val dmyPadded = "%02d-%02d-%04d".format(date.dayOfMonth, date.monthValue, date.year)
        val url = "https://xoso.com.vn/xsmb-$dmyPadded.html"
        val html = try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
        } catch (_: Exception) {
            ""
        }
        if (html.isBlank() || !html.contains("special-prize", ignoreCase = true)) return null

        val groups = listOf(
            "special-prize" to listOf("special"),
            "prize1" to listOf("prize1"),
            "prize2" to listOf("prize2_1", "prize2_2"),
            "prize3" to listOf("prize3_1", "prize3_2", "prize3_3", "prize3_4", "prize3_5", "prize3_6"),
            "prize4" to listOf("prize4_1", "prize4_2", "prize4_3", "prize4_4"),
            "prize5" to listOf("prize5_1", "prize5_2", "prize5_3", "prize5_4", "prize5_5", "prize5_6"),
            "prize6" to listOf("prize6_1", "prize6_2", "prize6_3"),
            "prize7" to listOf("prize7_1", "prize7_2", "prize7_3", "prize7_4")
        )

        val out = JSONObject()
        out.put("date", "${date}T00:00:00.000")
        var totalParsed = 0

        for ((cssClass, jsonKeys) in groups) {
            val regex = Regex("""class\s*=\s*["']?${Regex.escape(cssClass)}["']?[^>]*>([^<]+)""", RegexOption.IGNORE_CASE)
            val nums = regex.findAll(html)
                .mapNotNull { m ->
                    val digits = m.groupValues[1].filter { it.isDigit() }
                    extractLastTwoDigits(digits)
                }
                .toList()
            jsonKeys.forEachIndexed { idx, key ->
                val v = nums.getOrNull(idx)
                if (v != null) {
                    out.put(key, v)
                    totalParsed++
                }
            }
        }
        return if (totalParsed >= 20 && out.has("special")) out else null
    }

    suspend fun mergeXsmbJsonArray(incomingArray: JSONArray): List<LotteryDraw> = withContext(Dispatchers.IO) {
        ensureBundledDataSeeded()
        val targetFile = File(dataDir, LotteryProfiles.XSMB.fileName)
        if (incomingArray.length() > 0) {
            val tempFile = File(dataDir, "xsmb-7days.tmp")
            tempFile.writeText(incomingArray.toString(), StandardCharsets.UTF_8)
            mergeXsmbJsonFiles(existingFile = targetFile, incomingFile = tempFile)
            tempFile.delete()
        }
        loadDraws(LotteryProfiles.XSMB, forceReload = true)
    }

    private fun parseXsmbJsonFile(file: File): List<LotteryDraw> {
        val list = mutableListOf<LotteryDraw>()
        val seenDates = HashSet<LocalDate>()
        FileInputStream(file).use { fis ->
            JsonReader(InputStreamReader(fis, StandardCharsets.UTF_8)).use { reader ->
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
                            val rawDate = reader.nextString()
                            date = parseLocalDate(rawDate)
                        } else if (key in IGNORED_XSMB_KEYS) {
                            reader.skipValue()
                        } else {
                            val valueStr = when (reader.peek()) {
                                JsonToken.NUMBER -> "%02d".format(reader.nextInt())
                                JsonToken.STRING -> reader.nextString().trim()
                                else -> {
                                    reader.skipValue()
                                    ""
                                }
                            }
                            val twoDigit = extractLastTwoDigits(valueStr)
                            if (twoDigit != null) {
                                allNumbers.add(twoDigit)
                                if (key == "special" || key == "prize_special" || key == "dac_biet") {
                                    special = twoDigit
                                }
                                prizeMap.getOrPut(normalizeXsmbGroup(key)) { mutableListOf() }
                                    .add("%02d".format(twoDigit))
                            }
                        }
                    }
                    reader.endObject()

                    if (date != null && allNumbers.isNotEmpty() && seenDates.add(date)) {
                        list.add(
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
        }
        return list
    }

    private fun parseJsonlFile(file: File, profile: LotteryProfile): List<LotteryDraw> {
        val list = mutableListOf<LotteryDraw>()
        val seenKeys = HashSet<Pair<LocalDate, String>>()
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        val obj = JSONObject(trimmed)
                        val date = parseLocalDate(obj.optString("date"))
                        if (date != null) {
                            val drawId = obj.optString("id", "").trim()
                            val key = date to (drawId.ifEmpty { trimmed.hashCode().toString() })
                            if (seenKeys.add(key)) {
                                val draw = parseJsonlEntry(date, drawId, obj.opt("result"), profile)
                                if (draw != null) list.add(draw)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                line = reader.readLine()
            }
        }
        return list
    }

    private fun parseJsonlEntry(
        date: LocalDate,
        drawId: String,
        resultAny: Any?,
        profile: LotteryProfile
    ): LotteryDraw? {
        val allNumbers = mutableListOf<Int>()
        val prizeMap = LinkedHashMap<String, MutableList<String>>()
        var special: Int? = null

        when (resultAny) {
            is JSONArray -> {
                val rawInts = mutableListOf<Int>()
                for (i in 0 until resultAny.length()) {
                    val v = resultAny.optInt(i, -1)
                    if (v >= 0) rawInts.add(v)
                }
                when (profile.id) {
                    "power_655" -> {
                        val main = rawInts.take(6)
                        allNumbers.addAll(main)
                        special = rawInts.getOrNull(6)
                        prizeMap["6 số chính"] = main.map { "%02d".format(it) }.toMutableList()
                        special?.let { prizeMap["Số đặc biệt"] = mutableListOf("%02d".format(it)) }
                    }
                    "power_645" -> {
                        val main = rawInts.take(6)
                        allNumbers.addAll(main)
                        prizeMap["Bộ số trúng thưởng"] = main.map { "%02d".format(it) }.toMutableList()
                    }
                    "power_535" -> {
                        val main = rawInts.take(5)
                        allNumbers.addAll(main)
                        special = rawInts.getOrNull(5)
                        prizeMap["5 số chính"] = main.map { "%02d".format(it) }.toMutableList()
                        special?.let { prizeMap["Số đặc biệt"] = mutableListOf("%02d".format(it)) }
                    }
                    else -> {
                        allNumbers.addAll(rawInts)
                        prizeMap["Kết quả"] = rawInts.map { "%02d".format(it) }.toMutableList()
                    }
                }
            }
            is JSONObject -> {
                val keys = resultAny.keys()
                while (keys.hasNext()) {
                    val prizeKey = keys.next()
                    val arr = resultAny.optJSONArray(prizeKey)
                    val displayList = mutableListOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val rawStr = arr.optString(i).trim()
                            if (rawStr.isNotEmpty()) {
                                displayList.add(rawStr)
                                extractLastTwoDigits(rawStr)?.let { two ->
                                    allNumbers.add(two)
                                    if (special == null && prizeKey.contains("Đặc biệt", ignoreCase = true)) {
                                        special = two
                                    }
                                }
                            }
                        }
                    }
                    if (displayList.isNotEmpty()) {
                        prizeMap[prizeKey] = displayList
                    }
                }
            }
        }

        if (allNumbers.isEmpty()) return null
        return LotteryDraw(
            date = date,
            drawId = drawId,
            allNumbers = allNumbers,
            uniqueNumbers = allNumbers.toSet(),
            specialNumber = special,
            prizeMap = prizeMap
        )
    }

    private fun mergeXsmbJsonFiles(existingFile: File, incomingFile: File): Int {
        val mapByDate = LinkedHashMap<String, JSONObject>()
        if (existingFile.exists()) {
            try {
                val arr = JSONArray(existingFile.readText(StandardCharsets.UTF_8))
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val d = parseLocalDate(obj.optString("date"))?.toString() ?: continue
                    mapByDate[d] = obj
                }
            } catch (_: Exception) {
            }
        }
        val incArr = JSONArray(incomingFile.readText(StandardCharsets.UTF_8))
        for (i in 0 until incArr.length()) {
            val obj = incArr.optJSONObject(i) ?: continue
            val d = parseLocalDate(obj.optString("date"))?.toString() ?: continue
            mapByDate[d] = obj
        }
        val sortedKeys = mapByDate.keys.sorted()
        val outArr = JSONArray()
        for (k in sortedKeys) {
            outArr.put(mapByDate[k])
        }
        existingFile.writeText(outArr.toString(), StandardCharsets.UTF_8)
        return outArr.length()
    }

    private fun mergeJsonlFiles(existingFile: File, incomingFile: File): Int {
        val linesByKey = LinkedHashMap<String, String>()
        fun ingest(file: File) {
            if (!file.exists()) return
            file.forEachLine(StandardCharsets.UTF_8) { line ->
                val t = line.trim()
                if (t.isNotEmpty()) {
                    try {
                        val obj = JSONObject(t)
                        val d = parseLocalDate(obj.optString("date"))?.toString() ?: return@forEachLine
                        val id = obj.optString("id", "")
                        val key = if (id.isNotBlank()) "${d}_$id" else "${d}_${t.hashCode()}"
                        linesByKey[key] = t
                    } catch (_: Exception) {
                    }
                }
            }
        }
        ingest(existingFile)
        ingest(incomingFile)
        val sortedEntries = linesByKey.entries.sortedBy { it.key }
        existingFile.writeText(sortedEntries.joinToString("\n") { it.value } + "\n", StandardCharsets.UTF_8)
        return sortedEntries.size
    }

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

    private fun parseLocalDate(raw: String): LocalDate? {
        if (raw.isBlank()) return null
        val cleaned = raw.substringBefore('T').substringBefore(' ').trim()
        return try {
            LocalDate.parse(cleaned)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val IGNORED_XSMB_KEYS = setOf(
            "date", "_id", "source", "day_of_week", "sign",
            "created_at", "updated_at", "province_name", "province_id", "id"
        )
    }
}
