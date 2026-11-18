package com.cau.medicheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Requirements 단위 테스트: 요구사항 맵(S45)의 검색과 우선순위 고르기.
 * 기대값은 진짜 자산 파일로 따로 계산해 확인한 수다.
 */
class RequirementsTest {

    private val all = RequirementCsv.parse(File("src/main/assets/requirements_107.csv").readText(Charsets.UTF_8))

    private fun ids(query: String, tier: Moscow? = null) = Requirements.filter(all, query, tier).map { it.id }

    @Test
    fun `빈 검색어는 107개 전부다`() {
        assertEquals(107, ids("").size)
    }

    @Test
    fun `대소문자를 가리지 않는다`() {
        val expected = listOf("F100", "F101", "F102", "F103", "F104", "F105", "F106", "F107")
        assertEquals(expected, ids("f10"))
        assertEquals(expected, ids("F10"))
    }

    @Test
    fun `분류로도 찾는다`() {
        assertEquals(listOf("F098", "F099", "F100", "F101", "F102"), ids("가족"))
    }

    @Test
    fun `이름으로 찾는다`() {
        assertEquals(17, ids("알림").size)
        assertEquals(listOf("F106"), ids("ocr"))
        assertEquals(listOf("F062", "F069"), ids("room"))
    }

    @Test
    fun `화면 ID로는 찾지 않는다`() {
        assertTrue(ids("s45").isEmpty())
    }

    @Test
    fun `우선순위와 검색을 함께 적용한다`() {
        assertEquals(11, ids("알림", Moscow.MUST).size)
    }

    @Test
    fun `우선순위만 고르면 그 우선순위의 기능만 남는다`() {
        assertEquals(listOf("F098", "F099", "F100", "F101", "F102", "F106", "F107"), ids("", Moscow.WONT))
    }

    @Test
    fun `결과는 파일 순서 그대로다`() {
        val result = ids("")
        assertEquals(result.sorted(), result)
    }

    @Test
    fun `검색어의 앞뒤 공백은 지우지 않는다`() {
        // "F001 회원가입 계정"에는 "회원가입 "(뒤에 공백)은 있고 " F001"(앞에 공백)은 없다
        assertEquals(listOf("F001"), ids("회원가입 "))
        assertTrue(ids(" F001").isEmpty())
    }
}
