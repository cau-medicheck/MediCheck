package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

/**
 * RequirementCsv 단위 테스트: 107개 기능 자산 읽기.
 * 앱에 들어가는 진짜 자산 파일을 읽어서 확인한다 (Gradle 단위 테스트는 app/ 폴더에서 돈다).
 */
class RequirementCsvTest {

    private val asset = File("src/main/assets/requirements_107.csv").readText(Charsets.UTF_8)
    private val all = RequirementCsv.parse(asset)

    private fun byId(id: String) = all.single { it.id == id }

    // ---------- 진짜 자산 ----------

    @Test
    fun `자산은 107개 기능이다`() {
        assertEquals(107, all.size)
    }

    @Test
    fun `우선순위별 개수는 60 26 14 7이다`() {
        val counts = Requirements.countByMoscow(all)
        assertEquals(
            mapOf(Moscow.MUST to 60, Moscow.SHOULD to 26, Moscow.COULD to 14, Moscow.WONT to 7),
            counts
        )
    }

    @Test
    fun `첫 줄은 맨 앞의 BOM 없이 F001로 읽힌다`() {
        assertEquals(Requirement("F001", "계정", "회원가입", Moscow.MUST, listOf("S02")), all.first())
    }

    @Test
    fun `따옴표로 감싼 화면 목록을 순서대로 나눈다`() {
        assertEquals(listOf("S07", "S10", "S12", "S32"), byId("F070").screens)
        assertEquals(listOf("S08", "S07", "S42"), byId("F093").screens)
    }

    @Test
    fun `F107은 안전 고지 화면 S46을 가리킨다`() {
        assertEquals(listOf("S46"), byId("F107").screens)
        assertEquals(Moscow.WONT, byId("F107").moscow)
    }

    @Test
    fun `이번에 하지 않는 기능은 7개이고 파일 순서대로다`() {
        assertEquals(
            listOf("F098", "F099", "F100", "F101", "F102", "F106", "F107"),
            all.filter { it.moscow == Moscow.WONT }.map { it.id }
        )
    }

    // ---------- 한 줄 나누기 ----------

    @Test
    fun `칸 안의 쉼표와 겹친 따옴표를 바르게 나눈다`() {
        assertEquals(listOf("a", "b,c", "d\"e"), RequirementCsv.splitLine("a,\"b,c\",\"d\"\"e\""))
    }

    @Test
    fun `따옴표가 닫히지 않은 줄은 실패한다`() {
        assertThrows(IllegalArgumentException::class.java) { RequirementCsv.splitLine("F001,계정,\"S02") }
    }

    @Test
    fun `빈 칸도 칸 하나로 센다`() {
        assertEquals(listOf("a", "", "c", ""), RequirementCsv.splitLine("a,,c,"))
    }

    // ---------- 파일 모양 ----------

    private val header = "ID,분류,기능,MoSCoW,v3화면ID"

    @Test
    fun `줄 끝이 CRLF여도 LF여도 읽는다`() {
        val crlf = RequirementCsv.parse("$header\r\nF001,계정,회원가입,Must,S02\r\n")
        val lf = RequirementCsv.parse("$header\nF001,계정,회원가입,Must,S02\n")

        assertEquals(crlf, lf)
        assertEquals("F001", lf.single().id)
    }

    @Test
    fun `칸 순서가 달라도 머리줄의 이름으로 찾아 읽는다`() {
        val shuffled = RequirementCsv.parse("MoSCoW,v3화면ID,기능,ID,분류\nCould,\"S08,S40\",중복 확인,F023,약 관리\n")

        assertEquals(Requirement("F023", "약 관리", "중복 확인", Moscow.COULD, listOf("S08", "S40")), shuffled.single())
    }

    @Test
    fun `곧은 따옴표로 적은 Won't도 받아 준다`() {
        assertEquals(Moscow.WONT, RequirementCsv.parse("$header\nF107,범위,자동 추천,Won't,S46\n").single().moscow)
    }

    @Test
    fun `화면 목록 칸이 없으면 실패한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            RequirementCsv.parse("ID,분류,기능,MoSCoW\nF001,계정,회원가입,Must\n")
        }
    }

    @Test
    fun `모르는 우선순위 글자면 실패한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            RequirementCsv.parse("$header\nF001,계정,회원가입,Maybe,S02\n")
        }
    }

    @Test
    fun `빈 파일이나 칸이 모자란 줄이 있으면 실패한다`() {
        assertThrows(IllegalArgumentException::class.java) { RequirementCsv.parse("") }
        assertThrows(IllegalArgumentException::class.java) { RequirementCsv.parse("$header\nF001,계정\n") }
    }
}
