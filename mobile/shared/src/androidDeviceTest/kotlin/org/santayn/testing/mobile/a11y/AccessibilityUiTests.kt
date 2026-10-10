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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.santayn.testing.mobile.core.network.ConnectivityObserver
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.platform.SilentSpeaker
import org.santayn.testing.mobile.core.platform.TextSpeaker
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.core.storage.ThemeMode
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
@RunWith(AndroidJUnit4::class)
class AccessibilityUiTests {

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
        StudentTestingTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, highContrast, textScale) {
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

    @Test
    fun matchingFieldNamesItsPrompt() {
        themed { TestQuestions() }
        rule.onNodeWithContentDescription("Соответствие для «Лекция»")
            .assert(roleIs(Role.DropdownList))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Выберите вариант"))
            .assertHasClickAction()
        // Видимая подсказка не дублируется отдельным элементом.
        rule.onAllNodesWithText("Лекция").assertCountEquals(0)
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

    // region заголовки экранов и сообщения о связи

    @Test
    fun screenTitleIsHeadingAndBackButtonIsLabelled() {
        themed { AppScreen(title = "Профиль", onBack = {}) { } }
        rule.onNodeWithText("Профиль").assert(isHeading())
        rule.onNodeWithContentDescription("Назад").assertHasClickAction()
        rule.onRoot().tryPerformAccessibilityChecks()
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
