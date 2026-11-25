package com.cau.medicheck

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cau.medicheck.domain.MedicationDraft
import com.cau.medicheck.domain.ScheduleDraft
import com.cau.medicheck.data.repository.AuthResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 앱을 에뮬레이터에서 실제로 띄워 사람처럼 눌러 보는 흐름 테스트 (2026-11-25).
 * 단위 테스트는 계산만 확인하고, 이 테스트는 "화면 → 저장 → 다른 화면에 보이기"까지 이어지는지 확인한다.
 *
 * 테스트마다 새 계정을 만들어 시작한다(이메일에 시각을 붙여 겹치지 않게). 그래서 순서와 상관없이 돈다.
 * 주의: ./gradlew connectedDebugAndroidTest 는 끝나면 에뮬레이터의 앱과 데이터를 지운다.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val container get() = (context.applicationContext as MediCheckApplication).container
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        // 알림 권한 창이 화면을 가리지 않게 미리 허락해 둔다
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { container.authRepository.logout() }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { container.authRepository.logout() }
    }

    // ---------- 도우미 ----------

    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** 새 계정을 만들고 로그인한 상태로 둔다. 돌려주는 값은 사용자 번호 */
    private fun signUpNewUser(): Long = runBlocking {
        val email = "flow${System.nanoTime()}@test.com"
        val result = container.authRepository.signUp("테스터", email, "test1234", "test1234")
        (result as AuthResult.Success).user.id
    }

    /** 글자가 화면에 나올 때까지 기다린다(DB 읽기·화면 이동이 끝나기를) */
    private fun waitFor(matcher: SemanticsMatcher, timeoutMs: Long = 10_000) =
        compose.waitUntilAtLeastOneExists(matcher, timeoutMs)

    private fun waitForText(text: String, substring: Boolean = false) = waitFor(hasText(text, substring = substring))

    /** 누를 수 있는 것 중 이 글자를 가진 첫 번째를 누른다 (하단 탭·단추) */
    private fun tap(text: String, substring: Boolean = false) {
        val matcher = hasText(text, substring = substring) and hasClickAction()
        waitFor(matcher)
        compose.onAllNodes(matcher).onFirst().performScrollToIfPossible().performClick()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performScrollToIfPossible() =
        apply { runCatching { performScrollTo() } }

    // ---------- 흐름 ----------

    /** 시작 화면 → 로그인 화면 → 샘플 계정 → 홈 */
    @Test
    fun 시작화면에서_샘플계정으로_들어가면_홈이_보인다() {
        launch()
        // 시작 화면을 본 적이 없으면 시작 화면, 있으면 바로 로그인 화면이 열린다
        waitFor(hasText(str(R.string.s01_sample)) or hasText(str(R.string.s00_have_account)))
        if (compose.onAllNodes(hasText(str(R.string.s00_have_account))).fetchSemanticsNodes().isNotEmpty()) {
            tap(str(R.string.s00_have_account))
        }
        tap(str(R.string.s01_sample))
        waitForText(str(R.string.s04_section_schedule))
    }

    /** 약 등록 화면에서 이름·복용량만 넣고 등록 → 내 약 목록과 일정에 보인다 */
    @Test
    fun 약을_등록하면_내약과_일정에_보인다() {
        signUpNewUser()
        launch()
        tap(str(R.string.tab_meds))
        tap(str(R.string.s06_btn_add))

        val name = "흐름테스트약"
        waitFor(hasContentDescription(str(R.string.s08_label_name)))
        compose.onNode(hasContentDescription(str(R.string.s08_label_name))).performTextInput(name)
        compose.onNode(hasContentDescription(str(R.string.s08_label_dosage))).performTextInput("1정")
        tap(str(R.string.s08_btn_register))

        // 저장하면 그 약의 상세 화면이 열린다
        waitForText(name)
        // 내 약 목록에도 1개로 보인다
        tap(str(R.string.tab_meds))
        waitForText(str(R.string.s06_filter_all, 1))
        waitForText(name)

        // 일정 탭에도 같은 약이 있다 (기본 시간 08:00, 매일, 오늘 시작)
        tap(str(R.string.tab_schedule))
        waitForText(name)
    }

    /** 일정에서 회차를 눌러 복용 완료 → 홈 진행률이 1 / 1회가 된다 */
    @Test
    fun 복용완료를_누르면_홈_진행률이_올라간다() {
        val userId = signUpNewUser()
        val name = "자정약"
        runBlocking {
            container.medicationRepository.addMedication(
                userId,
                MedicationDraft(
                    name = name, dosage = "1정", startDate = LocalDate.now(), endDate = null, memo = "",
                    schedules = listOf(ScheduleDraft(time = LocalTime.of(0, 0), days = DayOfWeek.entries.toSet()))
                )
            )
        }
        launch()
        tap(str(R.string.tab_schedule))
        tap(name)
        tap(str(R.string.s10_btn_taken))

        tap(str(R.string.tab_home))
        waitForText(str(R.string.s04_progress, 1, 1))
    }

    /** 하단 탭과 통계 안의 탭을 모두 열어 본다 (화면이 죽지 않고 그려지는지) */
    @Test
    fun 모든_탭이_열린다() {
        runBlocking { container.authRepository.loginAsSample() }
        launch()
        tap(str(R.string.tab_schedule))
        waitForText(str(R.string.s05_btn_today))
        tap(str(R.string.tab_meds))
        waitForText(str(R.string.s06_btn_add))
        tap(str(R.string.tab_stats))
        for (tab in listOf("주간", "월간", "기타")) tap(tab)
        tap(str(R.string.tab_home))
    }

    /** 오른쪽 위 메뉴 → 설정 → 로그아웃 → 로그인 전 화면 */
    @Test
    fun 설정에서_로그아웃하면_로그인_전_화면으로_간다() {
        signUpNewUser()
        launch()
        val settings = hasContentDescription(str(R.string.header_cd_settings)) and hasClickAction()
        waitFor(settings)
        compose.onAllNodes(settings).onFirst().performClick()
        tap(str(R.string.action_logout))
        // 시작 화면(처음) 또는 로그인 화면(시작 화면을 본 적 있음) 중 하나
        waitFor(hasText(str(R.string.s01_sample)) or hasText(str(R.string.s00_have_account)))
    }
}
