package com.cau.medicheck.ui.navigation

import com.cau.medicheck.ui.navigation.graph.REGISTERED_SCREENS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 47개 화면이 빠짐없이 화면 틀에 붙어 있는지 확인한다.
 * 주소(route)만 있고 화면을 붙이지 않으면, 그 주소로 이동하는 순간 앱이 멈춘다. 그 실수를 빌드 단계에서 잡는다.
 *
 * 진짜 내비게이션 그래프는 폰이 있어야 만들 수 있어서, 여기서는 그래프를 만드는 소스 파일을 글자로 읽어
 * `composable(Routes.Sxx` 줄을 센다. (Gradle 단위 테스트는 app/ 폴더에서 돈다)
 */
class RegisteredScreensTest {

    private val graphFolder = File("src/main/java/com/cau/medicheck/ui/navigation/graph")
    private val postLoginGraphs =
        listOf("MainGraph", "MedicationGraph", "RecordGraph", "NotificationGraph", "SettingsGraph", "ExtrasGraph")
    private val expected = Routes.ALL_SCREEN_IDS.toSet() - Routes.PRE_LOGIN

    /** 그래프 파일들에서 `composable(Routes.Sxx` 로 붙인 화면 ID를 모두 찾는다 (같은 ID가 두 번이면 두 번 들어 있다). */
    private fun composedScreenIds(): List<String> {
        val call = Regex("""composable\(\s*Routes\.(S\d\d)""")
        return postLoginGraphs.flatMap { name ->
            call.findAll(File(graphFolder, "$name.kt").readText()).map { it.groupValues[1] }.toList()
        }
    }

    @Test
    fun `로그인 뒤 화면 S04부터 S46까지 43개가 실제로 붙어 있다`() {
        assertEquals(43, expected.size)
        assertEquals(expected, composedScreenIds().toSet())
    }

    @Test
    fun `같은 화면을 두 번 붙이지 않았다`() {
        // 같은 주소를 두 번 등록하면 앱을 켤 때 멈춘다.
        val ids = composedScreenIds()
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `화면 묶음 다섯 개를 모두 로그인 뒤 틀에 이어 붙였다`() {
        // 그래프 파일에 화면을 붙여 두어도, mainGraph가 그 파일의 함수를 부르지 않으면 화면이 생기지 않는다.
        val mainGraph = File(graphFolder, "MainGraph.kt").readText()
        listOf("medicationGraph(", "recordGraph(", "notificationGraph(", "settingsGraph(", "extrasGraph(").forEach {
            assertTrue("mainGraph가 $it 를 부르지 않는다", mainGraph.contains(it))
        }
    }

    @Test
    fun `화면 ID 목록도 같은 43개를 가리킨다`() {
        assertEquals(expected, REGISTERED_SCREENS)
    }

    @Test
    fun `로그인 전 화면은 S00부터 S03까지 4개다`() {
        assertEquals(setOf("S00", "S01", "S02", "S03"), Routes.PRE_LOGIN)
    }
}
