package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * CSV 만들기 테스트. (S43 복약 기록 내보내기)
 * "지금"은 2026-11-18 12:00.
 */
class CsvExporterTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 18)
    private val now = LocalDateTime.of(today, LocalTime.of(12, 0))

    private fun dose(
        date: LocalDate = today,
        hour: Int = 8,
        status: DoseStatus = DoseStatus.PENDING,
        name: String = "비타민 D",
        actedAt: Long? = null
    ) = DoseItem(
        recordId = 1, scheduleId = 1, medicationId = 1, medicationName = name, dosage = "1정", memo = "",
        date = date, time = LocalTime.of(hour, 0), status = status, actedAt = actedAt
    )

    private fun millis(date: LocalDate, hour: Int, minute: Int) =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).toInstant(zone).toEpochMilli()

    @Test
    fun `파일은 BOM으로 시작하고 첫 줄은 머리줄이다`() {
        val csv = CsvExporter.build(emptyList())

        assertEquals('\uFEFF', csv[0])
        assertEquals("\"날짜\",\"약 이름\",\"복용량\",\"예정시각\",\"상태\",\"실제복용시각\"", csv.substring(1))
    }

    @Test
    fun `줄은 CRLF로 나누고 모든 칸을 큰따옴표로 감싼다`() {
        val csv = CsvExporter.build(listOf(listOf("2026-11-18", "비타민 D", "1정", "08:00", "복용 완료", "07:50")))

        val lines = csv.substring(1).split("\r\n")
        assertEquals(2, lines.size)
        assertEquals("\"2026-11-18\",\"비타민 D\",\"1정\",\"08:00\",\"복용 완료\",\"07:50\"", lines[1])
    }

    @Test
    fun `칸 안의 큰따옴표는 두 개로 적고 쉼표와 줄바꿈은 그대로 감싼다`() {
        val csv = CsvExporter.build(listOf(listOf("비타민 \"D\", 1000IU", "두 줄\n메모")))

        assertTrue(csv.contains("\"비타민 \"\"D\"\", 1000IU\",\"두 줄\n메모\""))
    }

    @Test
    fun `상태는 내보내는 순간 기준의 한국어 네 가지다`() {
        fun status(item: DoseItem) = CsvExporter.toRow(item, now, zone)[4]

        assertEquals("복용 완료", status(dose(status = DoseStatus.TAKEN)))
        assertEquals("건너뜀", status(dose(status = DoseStatus.SKIPPED)))
        assertEquals("미복용", status(dose(hour = 8)))       // 08:00, 지금 12:00
        assertEquals("복용 예정", status(dose(hour = 12)))   // 정각이지만 30분 여유 안
        assertEquals("복용 예정", status(dose(hour = 20)))
        assertEquals("복용 예정", status(dose(date = today.plusDays(1))))
    }

    @Test
    fun `한 줄은 날짜 약 이름 복용량 예정시각 상태 실제복용시각 순서다`() {
        val item = dose(status = DoseStatus.TAKEN, actedAt = millis(today, 7, 50))

        assertEquals(
            listOf("2026-11-18", "비타민 D", "1정", "08:00", "복용 완료", "07:50"),
            CsvExporter.toRow(item, now, zone)
        )
    }

    @Test
    fun `실제복용시각은 복용 완료이고 같은 날 누른 시각이 있을 때만 적는다`() {
        fun actual(item: DoseItem) = CsvExporter.toRow(item, now, zone)[5]

        // 복용 완료인데 누른 시각이 없으면(샘플 데이터 등) 빈칸
        assertEquals("", actual(dose(status = DoseStatus.TAKEN, actedAt = null)))
        // 다른 날 누른 복용 완료는 빈칸
        assertEquals("", actual(dose(date = today.minusDays(2), status = DoseStatus.TAKEN, actedAt = millis(today, 9, 0))))
        // 건너뜀은 누른 시각이 있어도 빈칸
        assertEquals("", actual(dose(status = DoseStatus.SKIPPED, actedAt = millis(today, 9, 0))))
        assertEquals("", actual(dose()))
    }

    @Test
    fun `내보낼 순서는 날짜 예정 시각 약 이름이다`() {
        val items = listOf(
            dose(date = today, hour = 8, name = "오메가3"),
            dose(date = today.minusDays(1), hour = 20, name = "마그네슘"),
            dose(date = today, hour = 8, name = "비타민 D"),
            dose(date = today, hour = 7, name = "철분"),
            dose(date = today, hour = 8, name = "vitamin"),
            dose(date = today, hour = 8, name = "Zinc")
        )

        assertEquals(
            listOf("마그네슘", "철분", "vitamin", "Zinc", "비타민 D", "오메가3"),
            CsvExporter.sorted(items).map { it.medicationName }
        )
    }

    @Test
    fun `수식으로 읽힐 수 있는 약 이름과 복용량은 앞에 작은따옴표를 붙인다`() {
        val item = dose(name = "=HYPERLINK(1)").copy(dosage = "-1정")

        val row = CsvExporter.toRow(item, now, zone)

        assertEquals("'=HYPERLINK(1)", row[1])
        assertEquals("'-1정", row[2])
        // 보통 글자는 그대로다
        assertEquals("비타민 D", CsvExporter.toRow(dose(), now, zone)[1])
    }
}
