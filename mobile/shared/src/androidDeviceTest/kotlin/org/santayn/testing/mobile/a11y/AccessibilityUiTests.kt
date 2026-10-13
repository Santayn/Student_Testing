package org.santayn.testing.mobile.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.santayn.testing.mobile.core.network.ConnectivityObserver
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.platform.SilentSpeaker
import org.santayn.testing.mobile.core.platform.SpeechProblem
import org.santayn.testing.mobile.core.ui.SpeechProblemHost
import org.santayn.testing.mobile.core.platform.TextSpeaker
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.core.storage.ThemeMode
import org.santayn.testing.mobile.core.ui.AppFab
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.ErrorState
import org.santayn.testing.mobile.core.ui.LocalNetworkStatus
import org.santayn.testing.mobile.core.ui.NetworkBanner
import org.santayn.testing.mobile.core.ui.StudentTestingTheme
import org.santayn.testing.mobile.data.model.MatchingPromptDto
import org.santayn.testing.mobile.data.model.PublicOptionDto
import org.santayn.testing.mobile.data.model.PublicQuestionDto
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.domain.AttemptAnswers
import org.santayn.testing.mobile.feature.profile.DisplaySettings
import org.santayn.testing.mobile.feature.tests.QuestionCard
import org.santayn.testing.mobile.feature.tests.ResultHero
import org.santayn.testing.mobile.data.model.SubmitAttemptResponse
import androidx.navigation.compose.rememberNavController
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.data.model.PublicLectureDto
import org.santayn.testing.mobile.feature.home.KafedraHome
import org.santayn.testing.mobile.feature.home.LastAttempt
import org.santayn.testing.mobile.feature.home.PolytechHome
import org.santayn.testing.mobile.feature.home.StudentHome
import org.santayn.testing.mobile.feature.learning.LectureHero
import org.santayn.testing.mobile.feature.learning.SubjectItem
import org.santayn.testing.mobile.navigation.AppNavigator
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap

/**
 * UI-тесты доступности на устройстве.
 *
 * - Семантика для TalkBack: роли, состояния, подписи, заголовки, live region.
 * - Автоматические проверки Google Accessibility Test Framework (как в Accessibility Scanner):
 *   размер сенсорных целей, контраст текста, наличие подписей у элементов, дубли подписей.
 *   Включены для каждого теста ([enableAccessibilityChecks]): выполняются при каждом действии
 *   (performClick и т. п.) и явно через [tryPerformAccessibilityChecks]; ошибка ATF валит тест.
 *
 * Запуск (эмулятор или телефон по USB): `./gradlew :shared:connectedAndroidDeviceTest`
 */
/*
 * Параметризовано темой оформления: каждая тема из DesignTheme проходит ВСЕ проверки ниже
 * (новая тема подключается автоматически).
 */
@RunWith(Parameterized::class)
class AccessibilityUiTests(private val design: DesignTheme) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun themes(): List<DesignTheme> = DesignTheme.entries
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private class MemoryStore : KeyValueStore {
        val map = mutableMapOf<String, String>()
        override fun getString(key: String) = map[key]
        override fun putString(key: String, value: String?) {
            if (value == null) map.remove(key) else map[key] = value
        }
        override fun keys() = map.keys
    }

    private class FixedConnectivity(connected: Boolean) : ConnectivityObserver {
        override val isConnected = MutableStateFlow(connected)
    }

    @Before
    fun setUp() {
        rule.enableAccessibilityChecks()
        startKoin {
            modules(
                module {
                    single<TextSpeaker> { SilentSpeaker() }
                    single { AppSettings(MemoryStore()) }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // region данные

    private val single = PublicQuestionDto(
        id = 1, type = QuestionType.SINGLE, text = "Какое устройство хранит данные постоянно?", points = 1.0,
        options = listOf(PublicOptionDto(10, "RAM", 1), PublicOptionDto(11, "SSD", 2), PublicOptionDto(12, "CPU", 3)),
    )
    private val multiple = PublicQuestionDto(
        id = 2, type = QuestionType.MULTIPLE, text = "Выберите устройства ввода", points = 1.0,
        options = listOf(PublicOptionDto(20, "Клавиатура", 1), PublicOptionDto(21, "Мышь", 2), PublicOptionDto(22, "Монитор", 3)),
    )
    private val matching = PublicQuestionDto(
        id = 3, type = QuestionType.MATCHING, text = "Сопоставьте понятия", points = 1.0,
        matchingPrompts = listOf(MatchingPromptDto(1, "Лекция"), MatchingPromptDto(2, "Тема")),
        matchingOptions = listOf("Раздел", "Материал"),
    )
    private val text = PublicQuestionDto(id = 4, type = QuestionType.TEXT, text = "Расшифруйте CPU", points = 1.0)

    // endregion

    private fun themed(
        dark: Boolean = false,
        highContrast: Boolean = false,
        textScale: Float = 1f,
        network: NetworkStatus? = null,
        content: @Composable () -> Unit,
    ) = rule.setContent {
        StudentTestingTheme(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            design = design,
            highContrast = highContrast,
            textScale = textScale,
        ) {
            CompositionLocalProvider(LocalNetworkStatus provides network) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    /** Все четыре типа вопросов с рабочим состоянием ответов. */
    @Composable
    private fun TestQuestions() {
        var answers by remember { mutableStateOf(AttemptAnswers()) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            listOf(single, multiple, matching, text).forEachIndexed { index, q ->
                QuestionCard(index, q, answers, enabled = true, onChange = { transform -> answers = transform(answers) })
            }
        }
    }

    private val roleIs = { role: Role -> SemanticsMatcher.expectValue(SemanticsProperties.Role, role) }
    private val isLiveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    // region семантика вопросов теста

    @Test
    fun singleChoiceOptionIsOneSelectableRadioButton() {
        themed { TestQuestions() }
        // Строка варианта — один элемент (текст и кружок объединены), роль «переключатель».
        rule.onAllNodesWithText("SSD").assertCountEquals(1)
        rule.onNodeWithText("SSD").assert(roleIs(Role.RadioButton)).assertIsNotSelected()
        rule.onNodeWithText("SSD").performClick()
        rule.onNodeWithText("SSD").assertIsSelected()
        rule.onNodeWithText("RAM").assertIsNotSelected()
    }

    @Test
    fun multipleChoiceOptionIsOneCheckbox() {
        themed { TestQuestions() }
        rule.onAllNodesWithText("Мышь").assertCountEquals(1)
        rule.onNodeWithText("Мышь").assert(roleIs(Role.Checkbox)).assertIsOff()
        rule.onNodeWithText("Мышь").performScrollTo().performClick()
        rule.onNodeWithText("Мышь").assertIsOn()
    }

    private fun stateIs(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun matchingSlotNamesItsPromptAndOffersActions() {
        themed { TestQuestions() }
        val slot = rule.onNodeWithContentDescription("Соответствие для «Лекция»").performScrollTo()
        slot.assert(roleIs(Role.Button)).assert(stateIs("не выбрано"))
        // Видимая подсказка не дублируется отдельным элементом — она в описании слота.
        rule.onAllNodesWithText("Лекция").assertCountEquals(0)
        // Действие для TalkBack/VoiceOver вместо перетаскивания.
        val action = slot.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "Поставить «Раздел»" }
        rule.runOnIdle { action.action() }
        slot.assert(stateIs("Раздел"))
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun matchingWorksByTapsWithoutDragging() {
        themed { TestQuestions() }
        rule.onNodeWithContentDescription("Соответствие для «Тема»").performScrollTo()
        rule.onNodeWithText("Материал").performClick()
        rule.onNodeWithText("Материал").assert(stateIs("выбран, нажмите на подсказку"))
        rule.onNodeWithContentDescription("Соответствие для «Тема»").performClick()
        rule.onNodeWithContentDescription("Соответствие для «Тема»").assert(stateIs("Материал"))
    }

    @Test
    fun matchingWorksByDragAndDrop() {
        themed { TestQuestions() }
        rule.onNodeWithContentDescription("Соответствие для «Тема»").performScrollTo()
        val chip = rule.onNodeWithText("Раздел")
        val chipBounds = chip.fetchSemanticsNode().boundsInRoot
        val row = rule.onNodeWithContentDescription("Соответствие для «Тема»").fetchSemanticsNode().boundsInRoot
        // Слот — правая половина строки.
        val target = Offset(row.right - row.width / 4f, row.center.y) - chipBounds.topLeft
        chip.performTouchInput {
            down(center)
            repeat(12) { step -> moveTo(lerp(center, target, (step + 1) / 12f)) }
            up()
        }
        rule.onNodeWithContentDescription("Соответствие для «Тема»").assert(stateIs("Раздел"))
        saveScreenshot("matching-dragged")
    }

    @Test
    fun questionTextIsHeadingAndHasReadAloudButton() {
        themed { TestQuestions() }
        rule.onNodeWithText("Какое устройство хранит данные постоянно?").assert(isHeading())
        rule.onAllNodesWithContentDescriptionCompat("Прочитать вслух").onFirst().assertHasClickAction()
    }

    // endregion

    // region автоматические проверки ATF в разных режимах отображения

    @Test fun testScreenPassesAtfLight() = atfOnQuestions(dark = false, highContrast = false)
    @Test fun testScreenPassesAtfDark() = atfOnQuestions(dark = true, highContrast = false)
    @Test fun testScreenPassesAtfHighContrastLight() = atfOnQuestions(dark = false, highContrast = true)
    @Test fun testScreenPassesAtfHighContrastDark() = atfOnQuestions(dark = true, highContrast = true)
    @Test fun testScreenPassesAtfWithLargestText() = atfOnQuestions(dark = false, highContrast = false, textScale = 1.5f)

    private fun atfOnQuestions(dark: Boolean, highContrast: Boolean, textScale: Float = 1f) {
        themed(dark, highContrast, textScale) { TestQuestions() }
        rule.onRoot().tryPerformAccessibilityChecks()
        rule.onNodeWithText("SSD").performScrollTo().performClick() // ещё раз — в состоянии «выбрано»
        rule.onNodeWithText("Клавиатура").performScrollTo().performClick()
    }

    @Test
    fun displaySettingsPassAtfAndUseProperRoles() {
        themed { Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { DisplaySettings() } }
        rule.onRoot().tryPerformAccessibilityChecks()
        rule.onNodeWithText("Тема").assert(isHeading())
        rule.onNodeWithText("Крупный").assert(roleIs(Role.RadioButton)).performClick().assertIsSelected()
        rule.onNodeWithText("Высокий контраст").assert(roleIs(Role.Switch)).assertIsOff()
        rule.onNodeWithText("Высокий контраст").performScrollTo().performClick()
        rule.onNodeWithText("Высокий контраст").assertIsOn()
    }

    // endregion

    /**
     * Контроль: проверки ATF действительно включены — заведомо недоступный элемент
     * (кнопка 20 dp без подписи) должен валить проверку.
     */
    @Test
    fun atfChecksAreActiveAndCatchBadControls() {
        themed {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(32.dp).size(20.dp).clickable { }
            )
        }
        val error = runCatching { rule.onRoot().tryPerformAccessibilityChecks() }.exceptionOrNull()
        assertNotNull("ATF должен найти проблему у кнопки 20 dp без подписи", error)
        assertTrue(error!!::class.java.name, error::class.java.name.contains("Accessibility"))
    }

    // region фирменные экраны темы: итог теста, главная, шапка лекции

    @Test
    fun resultHeroAnnouncesScoreAndPassesAtf() {
        themed {
            Column {
                ResultHero(
                    title = "Тест по теме лекции",
                    result = SubmitAttemptResponse(attemptId = 1, score = 2.0, correctCount = 2, totalCount = 4),
                    onBack = {},
                )
            }
        }
        // Итог объявляется сам (заголовок + live region), число верных — одним сообщением; вид — свой у темы.
        when (design) {
            DesignTheme.POLYTECH -> {
                rule.onNodeWithContentDescription("Результат 50 процентов").assertIsDisplayed()
                rule.onNodeWithText("2 из 4 верно").assert(isHeading()).assert(isLiveRegionSelf)
            }
            DesignTheme.KAFEDRA -> {
                rule.onNodeWithText("Тест завершён").assert(isHeading()).assert(isLiveRegionSelf)
                rule.onNodeWithContentDescription("2 из 4 правильных ответов").assertIsDisplayed()
                rule.onNodeWithContentDescription("2 верно, 2 неверно").assertIsDisplayed()
            }
        }
        rule.onNodeWithContentDescription("К лекции").assertHasClickAction()
        rule.onRoot().tryPerformAccessibilityChecks()
        saveScreenshot("result-hero")
    }

    /** Главная темы в превью-обёртке: данные передаются готовыми, без ViewModel. */
    @Test
    fun studentHomePassesAtf() {
        val home = StudentHome(
            contextLine = "ПИ-201 · Информационные системы",
            subjects = listOf(
                SubjectItem(1, "Информатика", "Устройство компьютера и основы алгоритмов", null),
                SubjectItem(2, "Базы данных", null, null),
            ),
            lastAttempt = LastAttempt("Тест по теме лекции", 50.0, "2026-10-10T09:00:00Z"),
            emptyReason = null,
        )
        themed {
            val navigator = AppNavigator(rememberNavController())
            val state = LoadState.Ready(home)
            when (design) {
                DesignTheme.POLYTECH -> PolytechHome(WorkspaceRole.STUDENT, "Анна", state, onRetry = {}, navigator = navigator)
                DesignTheme.KAFEDRA -> KafedraHome(WorkspaceRole.STUDENT, "Анна", state, onRetry = {}, navigator = navigator)
            }
        }
        rule.onNodeWithText("Информатика").assertIsDisplayed()
        when (design) {
            DesignTheme.POLYTECH -> rule.onNodeWithText("Привет, Анна").assert(isHeading())
            DesignTheme.KAFEDRA -> {
                rule.onNodeWithText("Сегодня").assert(isHeading())
                rule.onNodeWithContentDescription("Профиль").assertHasClickAction()
                rule.onNodeWithContentDescription("50 процентов верно", useUnmergedTree = true).assertIsDisplayed()
            }
        }
        rule.onRoot().tryPerformAccessibilityChecks()
        saveScreenshot("home")
    }

    @Test
    fun lectureHeroTitleIsHeadingAndPassesAtf() {
        themed {
            Column(Modifier.padding(16.dp)) {
                LectureHero(
                    PublicLectureDto(
                        id = 1, ordinal = 101, versionNumber = 1,
                        title = "Устройства персонального компьютера",
                        description = "Память, процессор и устройства ввода-вывода: из чего состоит компьютер.",
                    )
                )
            }
        }
        rule.onNodeWithText("Устройства персонального компьютера").assert(isHeading())
        // «Прочитать вслух»: в «Политехе» — кнопка с подписью, в «Кафедре» — кнопка-иконка.
        when (design) {
            DesignTheme.POLYTECH -> rule.onNodeWithText("Прочитать вслух").assertHasClickAction()
            DesignTheme.KAFEDRA -> rule.onNodeWithContentDescription("Прочитать вслух").assertHasClickAction()
        }
        rule.onRoot().tryPerformAccessibilityChecks()
        saveScreenshot("lecture-hero")
    }

    @Test
    fun answerCardsKeepRolesInCardsStyle() {
        themed { TestQuestions() }
        saveScreenshot("test-question-cards")
        rule.onNodeWithText("Клавиатура").performScrollTo().assert(roleIs(Role.Checkbox)).performClick().assertIsOn()
    }

    /** Снимок для визуальной проверки: /sdcard/Android/data/<пакет тестов>/files/screens/<name>.png. */
    private fun saveScreenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screens")
        dir.mkdirs()
        java.io.File(dir, "${design.name.lowercase()}-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val isLiveRegionSelf = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    // endregion

    // region заголовки экранов и сообщения о связи

    @Test
    fun screenTitleIsHeadingAndBackButtonIsLabelled() {
        themed { AppScreen(title = "Профиль", onBack = {}) { } }
        rule.onNodeWithText("Профиль").assert(isHeading())
        rule.onNodeWithContentDescription("Назад").assertHasClickAction()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun floatingActionButtonIsLabelledButton() {
        themed {
            Column {
                AppFab("Тема", onClick = {}, accessibilityLabel = "Новая тема")
                AppFab("Добавить", onClick = {})
            }
        }
        rule.onNodeWithContentDescription("Новая тема")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithText("Добавить")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        saveScreenshot("fab")
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun speechProblemIsExplainedWithAction() {
        val speaker = org.koin.core.context.GlobalContext.get().get<TextSpeaker>() as SilentSpeaker
        speaker.problem.value = SpeechProblem.NO_RUSSIAN_VOICE
        themed { SpeechProblemHost() }
        rule.onNodeWithText("Нет русского голоса").assertIsDisplayed()
        rule.onNodeWithText("Открыть настройки").assertHasClickAction()
        rule.onNodeWithText("Нет русского голоса").tryPerformAccessibilityChecks()
        rule.onNodeWithText("Закрыть").performClick()
        rule.waitForIdle()
        assertEquals(null, speaker.problem.value)
        rule.onNodeWithText("Нет русского голоса").assertDoesNotExist()
    }

    @Test
    fun offlineErrorIsAnnouncedWithClearText() {
        val network = NetworkStatus(FixedConnectivity(false), CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)) { false }
        themed(network = network) { ErrorState("Нет подключения к интернету", onRetry = {}) }
        rule.onNodeWithText("Нет подключения к интернету")
            .assertIsDisplayed()
            .assert(isHeading())
            .assert(hasAnyAncestor(isLiveRegion))
        rule.onNodeWithText("Повторить").assertHasClickAction()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun networkBannerIsLiveRegion() {
        val network = NetworkStatus(FixedConnectivity(false), CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)) { false }
        themed(network = network) { NetworkBanner() }
        rule.onNode(hasText("Нет подключения к интернету") and hasAnyAncestor(isLiveRegion)).assertIsDisplayed()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    // endregion

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithContentDescriptionCompat(label: String) =
        onAllNodes(androidx.compose.ui.test.hasContentDescription(label))
}
