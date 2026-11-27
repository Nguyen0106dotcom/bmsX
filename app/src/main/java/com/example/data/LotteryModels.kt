package com.example.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

val VN_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

enum class SortOrder(val label: String) {
    HIGH_TO_LOW("Điểm cao → thấp"),
    LOW_TO_HIGH("Điểm thấp → cao"),
    CUSTOM("Thứ tự số (00→99)")
}

data class ScheduledDrawInfo(
    val targetDate: LocalDate,
    val drawDateTime: ZonedDateTime,
    val isTodayDraw: Boolean,
    val isLiveWindow: Boolean
)

enum class XsmbStation(
    val id: String,
    val displayName: String,
    val shortName: String,
    val code: String,
    val drawDays: Set<DayOfWeek>,
    val scheduleLabel: String
) {
    ALL(
        id = "all",
        displayName = "Tất cả đài XSMB",
        shortName = "Tất cả đài",
        code = "XSMB",
        drawDays = DayOfWeek.entries.toSet(),
        scheduleLabel = "Hàng ngày · 18h15"
    ),
    HANOI(
        id = "hanoi",
        displayName = "Đài Hà Nội (Thủ Đô)",
        shortName = "Hà Nội",
        code = "XSHN",
        drawDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        scheduleLabel = "Thứ 2 & Thứ 5 · 18h15"
    ),
    QUANG_NINH(
        id = "quang_ninh",
        displayName = "Đài Quảng Ninh",
        shortName = "Quảng Ninh",
        code = "XSQN",
        drawDays = setOf(DayOfWeek.TUESDAY),
        scheduleLabel = "Thứ 3 · 18h15"
    ),
    BAC_NINH(
        id = "bac_ninh",
        displayName = "Đài Bắc Ninh",
        shortName = "Bắc Ninh",
        code = "XSBN",
        drawDays = setOf(DayOfWeek.WEDNESDAY),
        scheduleLabel = "Thứ 4 · 18h15"
    ),
    HAI_PHONG(
        id = "hai_phong",
        displayName = "Đài Hải Phòng",
        shortName = "Hải Phòng",
        code = "XSHP",
        drawDays = setOf(DayOfWeek.FRIDAY),
        scheduleLabel = "Thứ 6 · 18h15"
    ),
    NAM_DINH(
        id = "nam_dinh",
        displayName = "Đài Nam Định",
        shortName = "Nam Định",
        code = "XSNĐ",
        drawDays = setOf(DayOfWeek.SATURDAY),
        scheduleLabel = "Thứ 7 · 18h15"
    ),
    THAI_BINH(
        id = "thai_binh",
        displayName = "Đài Thái Bình",
        shortName = "Thái Bình",
        code = "XSTB",
        drawDays = setOf(DayOfWeek.SUNDAY),
        scheduleLabel = "Chủ Nhật · 18h15"
    );

    fun matchesDate(date: LocalDate): Boolean = this == ALL || date.dayOfWeek in drawDays

    fun nextDrawDate(date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        repeat(14) {
            if (d.dayOfWeek in drawDays) return d
            d = d.plusDays(1)
        }
        return date.plusDays(1)
    }

    fun prevDrawDate(date: LocalDate): LocalDate {
        var d = date.minusDays(1)
        repeat(14) {
            if (d.dayOfWeek in drawDays) return d
            d = d.minusDays(1)
        }
        return date.minusDays(1)
    }

    fun nextScheduledDraw(
        nowVn: ZonedDateTime = ZonedDateTime.now(VN_ZONE_ID),
        latestStationLocalDate: LocalDate? = null
    ): ScheduledDrawInfo {
        val today = nowVn.toLocalDate()
        val nowTime = nowVn.toLocalTime()
        val drawTime = LocalTime.of(18, 15)
        val liveGraceEnd = LocalTime.of(18, 35)

        if (today.dayOfWeek in drawDays) {
            if (nowTime.isBefore(drawTime)) {
                if (latestStationLocalDate != null && !latestStationLocalDate.isBefore(today)) {
                    val nextDay = nextDrawDate(today)
                    return ScheduledDrawInfo(
                        targetDate = nextDay,
                        drawDateTime = ZonedDateTime.of(nextDay, drawTime, VN_ZONE_ID),
                        isTodayDraw = false,
                        isLiveWindow = false
                    )
                }
                return ScheduledDrawInfo(
                    targetDate = today,
                    drawDateTime = ZonedDateTime.of(today, drawTime, VN_ZONE_ID),
                    isTodayDraw = true,
                    isLiveWindow = false
                )
            }
            if (!nowTime.isBefore(drawTime) && nowTime.isBefore(liveGraceEnd) &&
                (latestStationLocalDate == null || latestStationLocalDate.isBefore(today))
            ) {
                return ScheduledDrawInfo(
                    targetDate = today,
                    drawDateTime = ZonedDateTime.of(today, drawTime, VN_ZONE_ID),
                    isTodayDraw = true,
                    isLiveWindow = true
                )
            }
        }

        val nextDay = nextDrawDate(today)
        return ScheduledDrawInfo(
            targetDate = nextDay,
            drawDateTime = ZonedDateTime.of(nextDay, drawTime, VN_ZONE_ID),
            isTodayDraw = false,
            isLiveWindow = false
        )
    }

    companion object {
        val PROVINCES: List<XsmbStation> = listOf(
            HANOI, QUANG_NINH, BAC_NINH, HAI_PHONG, NAM_DINH, THAI_BINH
        )

        fun forDate(date: LocalDate): XsmbStation = when (date.dayOfWeek) {
            DayOfWeek.MONDAY, DayOfWeek.THURSDAY -> HANOI
            DayOfWeek.TUESDAY -> QUANG_NINH
            DayOfWeek.WEDNESDAY -> BAC_NINH
            DayOfWeek.FRIDAY -> HAI_PHONG
            DayOfWeek.SATURDAY -> NAM_DINH
            DayOfWeek.SUNDAY -> THAI_BINH
        }
    }
}

data class LotteryProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val fileName: String,
    val isJsonl: Boolean,
    val minVal: Int,
    val maxVal: Int,
    val numCols: Int,
    val drawDays: Set<DayOfWeek>,
    val drawTimes: List<LocalTime>,
    val evalPicks: Int,
    val minMatches: Int,
    val prizeName: String,
    val description: String,
    val defaultSyncUrl: String
) {
    val validRange: IntRange get() = minVal..maxVal

    fun formatNumber(num: Int): String = "%02d".format(num)

    fun isValidDrawDate(date: LocalDate): Boolean = date.dayOfWeek in drawDays

    fun nextDrawDate(date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        repeat(14) {
            if (d.dayOfWeek in drawDays) return d
            d = d.plusDays(1)
        }
        return date.plusDays(1)
    }

    fun prevDrawDate(date: LocalDate): LocalDate {
        var d = date.minusDays(1)
        repeat(14) {
            if (d.dayOfWeek in drawDays) return d
            d = d.minusDays(1)
        }
        return date.minusDays(1)
    }

    fun nextScheduledDraw(
        nowVn: ZonedDateTime = ZonedDateTime.now(VN_ZONE_ID),
        latestLocalDate: LocalDate? = null
    ): ScheduledDrawInfo {
        val today = nowVn.toLocalDate()
        val nowTime = nowVn.toLocalTime()
        val sortedTimes = drawTimes.sorted()
        val primaryTime = sortedTimes.firstOrNull() ?: LocalTime.of(18, 15)
        val liveGraceEnd = sortedTimes.lastOrNull()?.plusMinutes(20) ?: LocalTime.of(18, 35)

        if (today.dayOfWeek in drawDays) {
            val upcomingTimeToday = sortedTimes.firstOrNull { nowTime.isBefore(it) }
            if (upcomingTimeToday != null) {
                // If single daily draw and today's draw is already recorded locally, move to next draw day
                if (sortedTimes.size == 1 && latestLocalDate != null && !latestLocalDate.isBefore(today)) {
                    val nextDay = nextDrawDate(today)
                    return ScheduledDrawInfo(
                        targetDate = nextDay,
                        drawDateTime = ZonedDateTime.of(nextDay, primaryTime, VN_ZONE_ID),
                        isTodayDraw = false,
                        isLiveWindow = false
                    )
                }
                return ScheduledDrawInfo(
                    targetDate = today,
                    drawDateTime = ZonedDateTime.of(today, upcomingTimeToday, VN_ZONE_ID),
                    isTodayDraw = true,
                    isLiveWindow = false
                )
            }

            // Between draw start and result finalization (+20 min) when result not yet in local DB
            val lastTimeToday = sortedTimes.lastOrNull() ?: primaryTime
            if (!nowTime.isBefore(lastTimeToday) && nowTime.isBefore(liveGraceEnd) &&
                (latestLocalDate == null || latestLocalDate.isBefore(today))
            ) {
                return ScheduledDrawInfo(
                    targetDate = today,
                    drawDateTime = ZonedDateTime.of(today, lastTimeToday, VN_ZONE_ID),
                    isTodayDraw = true,
                    isLiveWindow = true
                )
            }
        }

        val nextDay = nextDrawDate(today)
        return ScheduledDrawInfo(
            targetDate = nextDay,
            drawDateTime = ZonedDateTime.of(nextDay, primaryTime, VN_ZONE_ID),
            isTodayDraw = false,
            isLiveWindow = false
        )
    }
}

object LotteryProfiles {
    private val ALL_DAYS = DayOfWeek.entries.toSet()

    val XSMB = LotteryProfile(
        id = "xsmb",
        name = "Xổ số Miền Bắc (XSMB)",
        shortName = "XSMB",
        fileName = "xsmb-2-digits.json",
        isJsonl = false,
        minVal = 0,
        maxVal = 99,
        numCols = 10,
        drawDays = ALL_DAYS,
        drawTimes = listOf(LocalTime.of(18, 15)),
        evalPicks = 3,
        minMatches = 1,
        prizeName = "Lô / Đề (Trùng ≥1 số)",
        description = "Quay 18h15 hàng ngày · 100 số (00–99), 27 giải thưởng",
        defaultSyncUrl = "https://raw.githubusercontent.com/junlangzi/data-kqxs/refs/heads/main/data/xsmb-2-digits.json"
    )

    val POWER_645 = LotteryProfile(
        id = "power_645",
        name = "Vietlott Mega 6/45",
        shortName = "Mega 6/45",
        fileName = "power645.jsonl",
        isJsonl = true,
        minVal = 1,
        maxVal = 45,
        numCols = 9,
        drawDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
        drawTimes = listOf(LocalTime.of(18, 0)),
        evalPicks = 6,
        minMatches = 3,
        prizeName = "Giải Ba (Trùng ≥3/6 số, 30.000đ)",
        description = "Quay 18h00 Thứ 4, 6, CN · Chọn 6 số từ 01–45",
        defaultSyncUrl = "https://raw.githubusercontent.com/vietvudanh/vietlott-data/main/data/power645.jsonl"
    )

    val POWER_655 = LotteryProfile(
        id = "power_655",
        name = "Vietlott Power 6/55",
        shortName = "Power 6/55",
        fileName = "power655.jsonl",
        isJsonl = true,
        minVal = 1,
        maxVal = 55,
        numCols = 10,
        drawDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
        drawTimes = listOf(LocalTime.of(18, 0)),
        evalPicks = 6,
        minMatches = 3,
        prizeName = "Giải Ba (Trùng ≥3/6 số, 50.000đ)",
        description = "Quay 18h00 Thứ 3, 5, 7 · Chọn 6 số từ 01–55",
        defaultSyncUrl = "https://raw.githubusercontent.com/vietvudanh/vietlott-data/main/data/power655.jsonl"
    )

    val POWER_535 = LotteryProfile(
        id = "power_535",
        name = "Vietlott Lotto 5/35",
        shortName = "Lotto 5/35",
        fileName = "power535.jsonl",
        isJsonl = true,
        minVal = 1,
        maxVal = 35,
        numCols = 7,
        drawDays = ALL_DAYS,
        drawTimes = listOf(LocalTime.of(13, 0), LocalTime.of(21, 0)),
        evalPicks = 5,
        minMatches = 2,
        prizeName = "Giải Tư / KK (Trùng ≥2/5 số, 20.000đ)",
        description = "Quay 13h00 & 21h00 hàng ngày · Chọn 5 số từ 01–35",
        defaultSyncUrl = "https://raw.githubusercontent.com/vietvudanh/vietlott-data/main/data/power535.jsonl"
    )

    val MAX_3D = LotteryProfile(
        id = "3d",
        name = "Vietlott Max 3D / 3D+",
        shortName = "Max 3D",
        fileName = "3d.jsonl",
        isJsonl = true,
        minVal = 0,
        maxVal = 99,
        numCols = 10,
        drawDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        drawTimes = listOf(LocalTime.of(18, 0)),
        evalPicks = 2,
        minMatches = 1,
        prizeName = "Giải Bảy / Ba (Trùng ≥1 bộ)",
        description = "Quay 18h00 Thứ 2, 4, 6 · 100 số 2 chữ số cuối (00–99)",
        defaultSyncUrl = "https://raw.githubusercontent.com/vietvudanh/vietlott-data/main/data/3d.jsonl"
    )

    val MAX_3D_PRO = LotteryProfile(
        id = "3d_pro",
        name = "Vietlott Max 3D Pro",
        shortName = "Max 3D Pro",
        fileName = "3d_pro.jsonl",
        isJsonl = true,
        minVal = 0,
        maxVal = 99,
        numCols = 10,
        drawDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
        drawTimes = listOf(LocalTime.of(18, 0)),
        evalPicks = 2,
        minMatches = 1,
        prizeName = "Giải Sáu (Trùng ≥1 bộ, 40.000đ)",
        description = "Quay 18h00 Thứ 3, 5, 7 · 100 số 2 chữ số cuối (00–99)",
        defaultSyncUrl = "https://raw.githubusercontent.com/vietvudanh/vietlott-data/main/data/3d_pro.jsonl"
    )

    val ALL: List<LotteryProfile> = listOf(
        XSMB, POWER_645, POWER_655, POWER_535, MAX_3D, MAX_3D_PRO
    )

    fun byId(id: String): LotteryProfile = ALL.find { it.id == id } ?: XSMB
}

/**
 * Một kỳ quay thưởng chuẩn hóa dùng chung cho Dự đoán, KQXS, Backtest, Tối ưu và Chơi thử.
 */
data class LotteryDraw(
    val date: LocalDate,
    val drawId: String = "",
    val allNumbers: List<Int>,              // Có lặp lại để đếm nháy / vị trí giải
    val uniqueNumbers: Set<Int>,          // Tập hợp số 2 chữ số đã ra
    val specialNumber: Int? = null,       // Số ĐB (hoặc số bổ sung power 6/55, 5/35)
    val prizeMap: Map<String, List<String>> = emptyMap() // Giữ nguyên từng giải để hiển thị bảng KQXS
) {
    val xsmbStation: XsmbStation get() = XsmbStation.forDate(date)
}

data class AlgorithmContribution(
    val algorithmId: String,
    val algorithmName: String,
    val rawScore: Double,
    val weight: Double,
    val weightedDelta: Double
)

data class NumberDetailInfo(
    val number: Int,
    val totalScore: Double,
    val rank: Int,
    val contributions: List<AlgorithmContribution>,
    val freq30Days: Int,
    val freq100Days: Int,
    val freq365Days: Int,
    val daysSinceLast: Int?,
    val avgIntervalDays: Double?
)

data class WinEvaluation(
    val isWin: Boolean,
    val matchCount: Int,
    val evalPicks: Int,
    val picks: List<Int>,
    val matchedNumbers: Set<Int>,
    val actualNumbers: Set<Int>,
    val specialNumber: Int?,
    val prizeDetail: String
)

data class PredictionResult(
    val lotteryId: String,
    val targetDate: LocalDate,
    val scores: Map<Int, Double>,
    val rankedNumbers: List<Pair<Int, Double>>,
    val top3: List<Pair<Int, Double>>,
    val contributionsByNumber: Map<Int, List<AlgorithmContribution>>,
    val actualDraw: LotteryDraw?,
    val winEvaluation: WinEvaluation?
)

data class HistoricalVerificationDay(
    val date: LocalDate,
    val stationName: String,
    val predictedTop10: List<Pair<Int, Double>>,
    val actualSpecial: Int?,
    val actualUniqueNumbers: Set<Int>,
    val actualHitCounts: Map<Int, Int>,
    val matchedTop3: Set<Int>,
    val matchedTop5: Set<Int>,
    val matchedTop10: Set<Int>,
    val totalNhayTop3: Int,
    val specialHitTop3: Boolean,
    val specialHitTop10: Boolean,
    val isProfileWin: Boolean,
    val prizeDetail: String,
    val actualPrizeMap: Map<String, List<String>> = emptyMap()
) {
    val predictedTop3: List<Pair<Int, Double>> get() = predictedTop10.take(3)
    val predictedTop5: List<Pair<Int, Double>> get() = predictedTop10.take(5)
    val isWinTop3: Boolean get() = matchedTop3.isNotEmpty()
    val isWinTop5: Boolean get() = matchedTop5.isNotEmpty()
    val isWinTop10: Boolean get() = matchedTop10.isNotEmpty()
}

data class CombinedPerformanceStats(
    val totalDays: Int,
    val top1Hits: Int,
    val top3Hits: Int,
    val top5Hits: Int,
    val top10Hits: Int,
    val specialTop3Hits: Int,
    val profileWinHits: Int,
    val totalNhayTop3: Int = 0,
    val specialTop10Hits: Int = 0,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val stationLabel: String = "",
    val dayLogs: List<HistoricalVerificationDay> = emptyList()
) {
    val top1Rate: Double get() = if (totalDays > 0) top1Hits * 100.0 / totalDays else 0.0
    val top3Rate: Double get() = if (totalDays > 0) top3Hits * 100.0 / totalDays else 0.0
    val top5Rate: Double get() = if (totalDays > 0) top5Hits * 100.0 / totalDays else 0.0
    val top10Rate: Double get() = if (totalDays > 0) top10Hits * 100.0 / totalDays else 0.0
    val profileWinRate: Double get() = if (totalDays > 0) profileWinHits * 100.0 / totalDays else 0.0
}

data class DailyKqxsStats(
    val multiHits: List<Pair<Int, Int>>,       // Lô về >= 2 nháy (số, số nháy)
    val loRoi: List<Int>,                      // Lô rơi từ kỳ trước
    val topGanLi: List<Pair<Int, Int>>,        // Top 3 số gan lâu nhất tính tới kỳ này
    val ganDaVe: List<Pair<Int, Int>>,         // Số gan >= 10 ngày đã nổ hôm nay
    val headCounts: Map<Int, List<Int>>,       // Đầu 0..9 -> danh sách đuôi
    val tailCounts: Map<Int, List<Int>>        // Đuôi 0..9 -> danh sách đầu
)

data class MonthBreakdown(
    val month: Int,
    val totalHits: Int,
    val drawDatesWithHit: List<Pair<LocalDate, Int>> // Ngày -> số nháy
)

data class YearBreakdown(
    val year: Int,
    val totalHits: Int,
    val months: List<MonthBreakdown>
)

data class DeepAnalysisResult(
    val queriedNumbers: List<Int>,
    val totalDrawsAnalyzed: Int,
    val drawsWithHits: Int,
    val totalHitCount: Int,
    val hitPercentage: Double,
    val avgDrawsPerHit: Double,
    val specialHits: Int,
    val maxGap: Int,
    val maxGapRange: String,
    val currentGap: Int,
    val maxStreak: Int,
    val maxStreakRange: String,
    val maxHitsInOneDraw: Int,
    val maxHitsDates: List<LocalDate>,
    val topCoOccurring: List<Pair<Int, Int>>,
    val yearBreakdown: List<YearBreakdown>
)

enum class TrialBetType(
    val label: String,
    val defaultCostPerPoint: Double,
    val defaultWinPerPoint: Double,
    val requiredPositions: Int
) {
    DE("Đề (ĐB)", 1_000.0, 70_000.0, 1),
    LO("Lô (27 giải)", 22_000.0, 80_000.0, 1),
    XIEN_2("Xiên 2", 22_000.0, 220_000.0, 2),
    XIEN_3("Xiên 3", 22_000.0, 880_000.0, 3),
    XIEN_4("Xiên 4", 22_000.0, 8_800_000.0, 4)
}

data class BetStrategy(
    val id: String,
    val type: TrialBetType,
    val positions: List<Int>, // 1-based rank positions (e.g. [1, 2])
    val points: Int,
    val costPerPoint: Double,
    val winPerPoint: Double
)

data class SimulationDayLog(
    val predictDate: LocalDate,
    val actualDate: LocalDate,
    val details: List<String>,
    val cost: Double,
    val win: Double,
    val balanceAfter: Double
)

data class SimulationSummary(
    val initialCapital: Double,
    val finalBalance: Double,
    val totalDays: Int,
    val winDays: Int,
    val lossDays: Int,
    val totalCost: Double,
    val totalWin: Double,
    val logs: List<SimulationDayLog>
) {
    val netProfit: Double get() = finalBalance - initialCapital
    val roiPercent: Double get() = if (initialCapital > 0) (netProfit / initialCapital) * 100.0 else 0.0
}

data class SevenDayComparisonRow(
    val date: LocalDate,
    val actualSpecial: Int?,
    val actualUniqueNumbers: Set<Int>,
    val actualHitCounts: Map<Int, Int>,
    val predictedTop10: List<Pair<Int, Double>>,
    val hitNumbersInTop3: Set<Int>,
    val hitNumbersInTop5: Set<Int>,
    val hitNumbersInTop10: Set<Int>,
    val specialHitInTop3: Boolean,
    val specialHitInTop10: Boolean,
    val totalNhayInTop3: Int,
    val prizeMap: Map<String, List<String>>
) {
    val predictedTop3: List<Pair<Int, Double>> get() = predictedTop10.take(3)
    val predictedTop5: List<Pair<Int, Double>> get() = predictedTop10.take(5)
    val isWinTop3: Boolean get() = hitNumbersInTop3.isNotEmpty()
    val isWinTop5: Boolean get() = hitNumbersInTop5.isNotEmpty()
    val isWinTop10: Boolean get() = hitNumbersInTop10.isNotEmpty()
    val station: XsmbStation get() = XsmbStation.forDate(date)
}

data class SevenDayComparisonSummary(
    val rows: List<SevenDayComparisonRow>,
    val sourceLabel: String,
    val fetchedFromNetwork: Boolean,
    val fetchedAtLabel: String,
    val errorMessage: String? = null
) {
    val daysCount: Int get() = rows.size
    val top3WinDays: Int get() = rows.count { it.isWinTop3 }
    val top5WinDays: Int get() = rows.count { it.isWinTop5 }
    val top10WinDays: Int get() = rows.count { it.isWinTop10 }
    val specialHitsTop10: Int get() = rows.count { it.specialHitInTop10 }
    val totalNhayTop3: Int get() = rows.sumOf { it.totalNhayInTop3 }
    val top3WinRate: Double get() = if (daysCount > 0) top3WinDays * 100.0 / daysCount else 0.0
    val top10WinRate: Double get() = if (daysCount > 0) top10WinDays * 100.0 / daysCount else 0.0
}

data class StationDailyPrediction(
    val date: LocalDate,
    val station: XsmbStation,
    val isTodayDraw: Boolean,
    val isLiveWindow: Boolean,
    val stationHistoryCount: Int,
    val top3: List<Pair<Int, Double>>,
    val top5: List<Pair<Int, Double>>,
    val actualDraw: LotteryDraw? = null
)

data class AccuracyTrendPoint(
    val dayIndex: Int,
    val date: LocalDate,
    val stationName: String,
    val hitTop3: Boolean,
    val hitTop5: Boolean,
    val hitTop10: Boolean,
    val totalNhayTop3: Int,
    val specialHitTop10: Boolean,
    val cumulativeTop3Pct: Double,
    val cumulativeTop5Pct: Double,
    val cumulativeTop10Pct: Double,
    val rolling7Top3Pct: Double,
    val rolling7Top5Pct: Double,
    val rolling7Top10Pct: Double
)

data class ThirtyDayAccuracyChartData(
    val points: List<AccuracyTrendPoint>,
    val stationLabel: String,
    val overallTop3Pct: Double,
    val overallTop5Pct: Double,
    val overallTop10Pct: Double,
    val top3Hits: Int,
    val top5Hits: Int,
    val top10Hits: Int
)

data class XsmbStationAccuracyRow(
    val station: XsmbStation,
    val totalDraws: Int,
    val top3Hits: Int,
    val top5Hits: Int,
    val top10Hits: Int,
    val specialTop10Hits: Int
) {
    val top3AccuracyPct: Double get() = if (totalDraws > 0) top3Hits * 100.0 / totalDraws else 0.0
    val top5AccuracyPct: Double get() = if (totalDraws > 0) top5Hits * 100.0 / totalDraws else 0.0
    val top10AccuracyPct: Double get() = if (totalDraws > 0) top10Hits * 100.0 / totalDraws else 0.0
}

data class XsmbDashboardSummary(
    val stats: CombinedPerformanceStats,
    val windowDrawsRequested: Int,
    val top1AccuracyPct: Double,
    val top3AccuracyPct: Double,
    val top5AccuracyPct: Double,
    val top10AccuracyPct: Double,
    val specialTop10AccuracyPct: Double,
    val avgNhayTop3: Double,
    val recent7Top3AccuracyPct: Double,
    val recent7Top5AccuracyPct: Double,
    val recent7Top10AccuracyPct: Double,
    val longestTop3WinStreak: Int,
    val currentTop3WinStreak: Int,
    val stationBreakdowns: List<XsmbStationAccuracyRow>
)
