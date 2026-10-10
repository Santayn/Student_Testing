# Тема B «Кафедра»

**Статус:** OPEN  
**Перед началом:** прочитайте [README.md](README.md) целиком (архитектура тем, правила, проверка).  
**Макет:** `mockups/varianty-dizaina.html`, ряд **B** (Главная, Лекция, Вопрос теста, Результат).  
**Образец реализации:** тема A — `core/ui/design/PolytechTheme.kt`, `feature/home/PolytechHome.kt`,
`feature/learning/LectureHero.kt`, `feature/tests/ResultHero.kt`.

## Идея

Спокойный «академический» интерфейс в духе системных настроек iOS и хороших университетских сайтов.
Минимум цвета, много воздуха, сгруппированные списки на сером фоне, тонкие разделители, крупные заголовки.
Цвет — только у действий и состояний. Тема для тех, кому «Политех» слишком яркий, и для долгого чтения лекций.

Ключевые отличия от A:

| | A «Политех» | B «Кафедра» |
|---|---|---|
| Шрифт | Unbounded + Onest | **Golos Text** (один на всё) |
| Фон / карточки | голубой фон, белые карточки без рамки | серый `#F4F6F9`, белые карточки **с тонкой рамкой** |
| Кнопки | «пилюля» 52dp | **скругление 12dp**, 52dp |
| Заголовки разделов | крупный акцидентный | **МЕЛКИЕ ПРОПИСНЫЕ** с разрядкой, приглушённые |
| Нижняя навигация | плавающая «пилюля» | **плоская панель вкладок** с подписями и линией сверху |
| Шапки экранов | цветные панели | **без цветных панелей**: крупный заголовок на фоне |
| Варианты ответа | отдельные карточки | **строки списка** в одной карточке (`AnswerStyle.LIST`, уже есть) |

---

## 1. Шрифт

Golos Text — российский шрифт (создан для госсервисов), отличная кириллица, OFL.

```
curl -L -o mobile/shared/src/commonMain/composeResources/font/golos_text.ttf "https://github.com/google/fonts/raw/main/ofl/golostext/GolosText%5Bwght%5D.ttf"
```

Веса: 400, 500, 600, 700. `displayFont` и `bodyFont` — один и тот же `FontFamily`.
Строка в `mobile/FONTS.md`: `golos_text.ttf | Golos Text (вариативный, wght) | «Кафедра» — весь текст | ofl/golostext/GolosText[wght].ttf`.

Типографика (шкала собирается в `Theme.kt` → `appTypography`; для B нужны **явные стили** на фирменных экранах):

| Где | Размер | Вес | Прочее |
|---|---|---|---|
| Крупный заголовок экрана («Сегодня», «Тест завершён») | 32sp | 700 | letterSpacing −0.02em |
| Заголовок лекции | 28sp | 700 | lineHeight 34sp, letterSpacing −0.015em |
| Вопрос теста | 24sp | 600 | lineHeight 31sp |
| Текст лекции | 17sp | 400 | lineHeight 27sp (1.6) |
| Надзаголовки и заголовки разделов | 13sp | 600 | ПРОПИСНЫЕ, letterSpacing 0.06em, цвет `onSurfaceVariant` |
| Строка списка: заголовок / подпись | 16sp 500 / 13sp 400 | | |

---

## 2. Палитры (проверены: все пары `ContrastTests` ≥ 4.5:1)

Создайте `core/ui/design/KafedraTheme.kt` по образцу `PolytechTheme.kt`. Значения:

```kotlin
internal val KafedraLight = lightColorScheme(
    primary = Color(0xFF045BA7), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F0FA), onPrimaryContainer = Color(0xFF03457F),
    secondary = Color(0xFF0E8A6A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7F5F0), onSecondaryContainer = Color(0xFF0B6B4F),
    tertiary = Color(0xFFB45309), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDF3E7), onTertiaryContainer = Color(0xFF8A4106),
    error = Color(0xFFB42318), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDEDE7), onErrorContainer = Color(0xFFA3330A),
    background = Color(0xFFF4F6F9), onBackground = Color(0xFF0E1F3D),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0E1F3D),
    surfaceVariant = Color(0xFFEEF1F6), onSurfaceVariant = Color(0xFF5B6678),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8F9FB), surfaceContainerHigh = Color(0xFFEEF1F6),
    surfaceContainerHighest = Color(0xFFE3E8EF),
    outline = Color(0xFF7A8496),          // границы полей ввода: ≥ 3:1 к белому
    outlineVariant = Color(0xFFE3E8EF),   // тонкие разделители и рамки карточек (декор)
    inverseSurface = Color(0xFF0E1F3D), inverseOnSurface = Color(0xFFF4F6F9),
    inversePrimary = Color(0xFF7DB3EE), scrim = Color(0xFF000000),
)

internal val KafedraDark = darkColorScheme(
    primary = Color(0xFF7DB3EE), onPrimary = Color(0xFF0B1420),
    primaryContainer = Color(0xFF12304F), onPrimaryContainer = Color(0xFFCFE4FA),
    secondary = Color(0xFF3FBF97), onSecondary = Color(0xFF07201A),
    secondaryContainer = Color(0xFF10291F), onSecondaryContainer = Color(0xFF8EE0C3),
    tertiary = Color(0xFFE8A25C), onTertiary = Color(0xFF2E1F0E),
    tertiaryContainer = Color(0xFF2E1F0E), onTertiaryContainer = Color(0xFFF5C792),
    error = Color(0xFFF2A08F), onError = Color(0xFF2A0E08),
    errorContainer = Color(0xFF33170E), onErrorContainer = Color(0xFFF7B9A3),
    background = Color(0xFF0E1420), onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF161D2B), onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1E2636), onSurfaceVariant = Color(0xFFA3ADBF),
    surfaceContainerLowest = Color(0xFF0E1420), surfaceContainerLow = Color(0xFF121826),
    surfaceContainer = Color(0xFF161D2B), surfaceContainerHigh = Color(0xFF1E2636),
    surfaceContainerHighest = Color(0xFF262F42),
    outline = Color(0xFF6B7689), outlineVariant = Color(0xFF2A3344),
    inverseSurface = Color(0xFFE6EAF2), inverseOnSurface = Color(0xFF161D2B),
    inversePrimary = Color(0xFF045BA7), scrim = Color(0xFF000000),
)

internal val KafedraLightStatus = StatusColors(
    success = Color(0xFF0E8A6A), successSoft = Color(0xFFE7F5F0), successText = Color(0xFF0B6B4F),
    warning = Color(0xFFB45309), warningSoft = Color(0xFFFDF3E7), warningText = Color(0xFF8A4106),
    danger = Color(0xFFC2410C), dangerSoft = Color(0xFFFDEDE7), dangerText = Color(0xFFA3330A),
    info = Color(0xFF045BA7), infoSoft = Color(0xFFE6F0FA), infoText = Color(0xFF03457F),
    primarySoft = Color(0xFFE6F0FA), primarySoftText = Color(0xFF03457F),
    shell = Color(0xFF045BA7), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF5B6678),
)

internal val KafedraDarkStatus = StatusColors(
    success = Color(0xFF3FBF97), successSoft = Color(0xFF10291F), successText = Color(0xFF8EE0C3),
    warning = Color(0xFFE8A25C), warningSoft = Color(0xFF2E1F0E), warningText = Color(0xFFF5C792),
    danger = Color(0xFFF08A67), dangerSoft = Color(0xFF33170E), dangerText = Color(0xFFF7B9A3),
    info = Color(0xFF7DB3EE), infoSoft = Color(0xFF10243A), infoText = Color(0xFFA9D0F5),
    primarySoft = Color(0xFF12304F), primarySoftText = Color(0xFFCFE4FA),
    shell = Color(0xFF161D2B), onShell = Color(0xFFE6EAF2),
    textMuted = Color(0xFFA3ADBF),
)

// В «Кафедре» нет цветных панелей: «hero» — обычная белая (тёмная) поверхность,
// акцент совпадает с primary. Значения нужны, т.к. BrandColors читают общие компоненты
// (BusyButton(accent = true), чипы итога и т.п.).
internal val KafedraLightBrand = BrandColors(
    hero = Color(0xFFFFFFFF), onHero = Color(0xFF0E1F3D), onHeroMuted = Color(0xFF5B6678),
    heroChip = Color(0xFFEEF1F6),
    accent = Color(0xFF045BA7), onAccent = Color(0xFFFFFFFF),
    highlight = Color(0xFFE6F0FA), onHighlight = Color(0xFF03457F),
)

internal val KafedraDarkBrand = BrandColors(
    hero = Color(0xFF161D2B), onHero = Color(0xFFE6EAF2), onHeroMuted = Color(0xFFA3ADBF),
    heroChip = Color(0xFF1E2636),
    accent = Color(0xFF7DB3EE), onAccent = Color(0xFF0B1420),
    highlight = Color(0xFF12304F), onHighlight = Color(0xFFCFE4FA),
)
```

Цвета полосок предметов на главной (по `subject.id % 5`, декоративные, ≥ 3:1 к белому/тёмной поверхности):
светлая — `#045BA7, #0E8A6A, #B45309, #6D28D9, #BE185D`; тёмная — `#7DB3EE, #3FBF97, #E8A25C, #B79BF5, #F08FBF`.
Положите их в файл темы (`internal val KafedraSubjectAccentsLight/Dark`), не в экран.

---

## 3. Токены

```kotlin
@Composable
internal fun kafedraTokens(): DesignTokens {
    val golos = variableFont(Res.font.golos_text, listOf(400, 500, 600, 700))
    return DesignTokens(
        theme = DesignTheme.KAFEDRA,
        displayFont = golos,
        bodyFont = golos,
        buttonShape = Shapes.rounded(12.dp),
        buttonHeight = 52.dp,
        buttonDepth = 0.dp,
        cardShape = Shapes.rounded(14.dp),
        cardBorder = true,                 // белая карточка на #F4F6F9 + рамка outlineVariant
        cardDepth = 0.dp,
        chipShape = Shapes.rounded(8.dp),
        fieldShape = Shapes.rounded(12.dp),
        tileShape = Shapes.rounded(10.dp),
        iconButtonsOnSurface = false,      // кнопки-иконки без подложки
        topBarOnBackground = true,
        sectionTitle = SectionTitleStyle.CAPS,
        navBar = NavBarStyle.TAB_BAR,      // НОВОЕ значение, см. 4.1
        answerStyle = AnswerStyle.LIST,
    )
}
```

`enum class DesignTheme`: `KAFEDRA(title = "Кафедра", description = "Спокойный академический стиль: списки и много воздуха")`.

Эти токены сами задают формы стандартных Material-компонентов (README, раздел 1, таблица `appShapes()`):
чипы-фильтры получат 8dp, поля и меню — 12dp, плавающая кнопка (`AppFab`) — 12dp, как кнопки. Отдельно их
настраивать не нужно. Проверьте на «Вопросах» преподавателя и «Учебной нагрузке» администратора: чипы-фильтры
должны выглядеть как в макете B (прямоугольники 8dp, не «пилюли»).

Плавающая кнопка в B стоит над плоской панелью вкладок: убедитесь, что `AppFab` не перекрывает
последнюю карточку списка (у `ScreenList` нижний отступ берётся из `Scaffold`, проверьте на «Факультетах»).

---

## 4. Новые виды компонентов

### 4.1. `NavBarStyle.TAB_BAR` — плоская панель вкладок (как в iOS)

Добавьте значение в `NavBarStyle` (`DesignTheme.kt`) и ветку в `AppNavigationBar` (`navigation/MainShell.kt`).
`CLASSIC` не трогайте.

- `Column { HorizontalDivider(color = outlineVariant); NavigationBar(containerColor = surface, tonalElevation = 0.dp) { … } }`.
- `NavigationBarItem` с `colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent,
  selectedIconColor = primary, selectedTextColor = primary, unselectedIconColor = onSurfaceVariant,
  unselectedTextColor = onSurfaceVariant)` — активная вкладка только цветом **и** жирностью подписи
  (`FontWeight.SemiBold` у выбранной): одного цвета для состояния мало.
- Подписи 11–12sp. Логика «при крупном шрифте подпись только у активной + `contentDescription` у иконок
  остальных» — как в ветке `CLASSIC` (скопируйте её).

### 4.2. Крупный заголовок на главных вкладках

На вкладках (экраны без `onBack`) в макете B — крупный заголовок 32sp слева, как Large Title в iOS.
Добавьте токен `largeTopLevelTitle: Boolean` в `DesignTokens` (`true` у B, **`false` в `polytechTokens()`**) и в
`AppScreen` (`core/ui/Components.kt`): если `onBack == null && Design.tokens.largeTopLevelTitle` — вместо
`TopAppBar` используйте `LargeTopAppBar` (с `scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`
и `Modifier.nestedScroll(...)` на `Scaffold`), стиль заголовка — 32sp/700/−0.02em. Заголовок остаётся
`heading()` (это уже сделано в `AppScreen`, не потеряйте при переделке).

---

## 5. Фирменные экраны

Общие правила: экраны рисуются на `MaterialTheme.colorScheme.background`; отступы по бокам 16dp (группы) /
20dp (текст); между группами 20–24dp; ни одного хардкод-цвета вне файла темы.

### 5.1. Главная — `feature/home/KafedraHome.kt`

Сигнатура как у `PolytechHome(role, name, student, onRetry, navigator)`. Ветка в `HomeScreen`.
`LazyColumn` на весь экран, `statusBarsPadding()` у первого элемента, `contentPadding(bottom = 24.dp)`.

1. **Шапка** (padding 20dp по бокам, 16dp сверху):
   слева колонка — дата 14sp `onSurfaceVariant` в формате «Пятница, 10 октября» (напишите `formatTodayRu()` в
   `core/util/Time.kt`: дни недели и месяцы в родительном падеже — своими массивами, kotlinx-datetime названий
   на русском не даёт) и под ней заголовок «Сегодня» 32sp/700 (`heading()`); справа — круг 44dp цвета `primary`
   с инициалами пользователя (`initials()` из `PolytechHome.kt` — вынесите её в общий файл `HomeData.kt`),
   кликабельный: открывает «Профиль» (`navigator.openTab(ProfileRoute)`), `contentDescription = "Профиль"`.
2. `NetworkBanner()` (как в A).
3. **Студент:**
   - **Карточка «последний тест»** (`AppCard(onClick = { openTab(ResultsRoute) })`): надзаголовок
     «ПОСЛЕДНИЙ ТЕСТ · 10.10.2026» 12sp/600/caps `primary`; название теста 18sp/600; строка
     «50 % верно» 14sp `onSurfaceVariant`; полоса прогресса 6dp, скругление 3dp, трек `outlineVariant`,
     заполнение `primary` на долю процента (`clearAndSetSemantics { contentDescription = "50 процентов верно" }`).
     Нет попыток — карточка «С чего начать»: «Откройте предмет и пройдите первый тест» + кнопка «Мои предметы».
     **Срока «до 15 октября» из макета НЕ делать** — в API нет сроков (см. «Требует backend»).
   - **«ПРЕДМЕТЫ»** — `SectionTitle` (CAPS сделает сам) + **одна сгруппированная карточка** со строками:
     строка min 64dp, padding 16dp: полоска 6×36dp скругление 3dp цвета предмета → 14dp → колонка
     (название 16sp/500, под ним `description` 13sp `onSurfaceVariant` в одну строку) → шеврон `onSurfaceVariant`.
     Между строками `HorizontalDivider(color = outlineVariant)` с отступом слева 36dp. Вся строка — один
     кликабельный элемент (`Modifier.clickable` + `semantics(mergeDescendants = true)`), открывает
     `SubjectDetailsRoute(id, facultyId)`. Показывайте все предметы (их мало), не 4.
     **ФИО преподавателя и число лекций из макета НЕ делать** — их нет в данных главной.
   - Пустые состояния/ошибка — как в A (`emptyReason` → `Banner`, ошибка → `RetryBanner`).
4. **Преподаватель / администратор:** под шапкой — разделы `homeSections(role)`: `SectionTitle(title)` +
   сгруппированная карточка со строками (иконка `entry.icon` 22dp `primary` → название 16sp/500 + описание 13sp →
   шеврон). Это же сгруппированное представление вынесите в приватный `GroupedList` и используйте для предметов.

### 5.2. Шапка лекции — ветка `KAFEDRA` в `feature/learning/LectureHero.kt`

Без цветной карточки — как начало статьи:
- строка: надзаголовок «ЛЕКЦИЯ 101 · ВЕРСИЯ 1» (13sp/600/caps/`onSurfaceVariant`, «ВЕРСИЯ» — только если
  `versionNumber > 0`) и справа `SpeakButton(key = "lecture-${id}", text = …)` (иконка, уже готов);
- заголовок лекции 28sp/700/lineHeight 34sp, `heading()`;
- описание 17sp/lineHeight 27sp `onSurface`; нет описания — «Описание лекции не указано.» `onSurfaceVariant`.

Материалы и тесты под шапкой — общие (`MaterialRow`, `TestCard`), они перекрасятся токенами сами.

### 5.3. Шапка итога теста — ветка `KAFEDRA` в `feature/tests/ResultHero.kt`

`ResultHero` стоит первым элементом `LazyColumn` без верхней панели — шапка сама отвечает за
`statusBarsPadding()` и кнопку «назад».
- Строка: `AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, "К лекции", onBack)`.
- Надзаголовок: название теста 14sp `onSurfaceVariant`; заголовок «Тест завершён» 32sp/700, `heading()` +
  `liveRegion = Polite`.
- `AppCard` (padding 20dp):
  - «**2**/4» — `2` 56sp/700, `/4` того же размера цветом `onSurfaceVariant`, рядом «правильных ответов» 16sp;
    всё одним элементом для TalkBack: `contentDescription = "2 из 4 правильных ответов"`;
  - полоса по вопросам: `Row` из `result.details`, сегменты `weight(1f)` × 8dp, скругление 2dp, зазор 4dp,
    `successText`-цвет для верных и `dangerText` для неверных (ярких `success/danger` не берите — на белом
    они < 3:1); `clearAndSetSemantics { contentDescription = "2 верно, 2 неверно" }`;
  - сетка 2 колонки (`HorizontalDivider` сверху, отступ 14dp): «Баллы» → `formatNumber(score)`,
    «Верно» → «50 %». **«Время» и «Осталось попыток» из макета НЕ делать** — этих данных нет в ответе.

Разбор ответов и кнопки под шапкой — общие (`TestResultScreen`), не трогайте.

### 5.4. Вопрос теста

Ничего своего: `AnswerStyle.LIST` уже рисует вопрос и варианты одной карточкой строками.
Проверьте только, что вопрос с сопоставлением (выпадающие поля) читается на 1.5× шрифте.

---

## 6. Тесты (в дополнение к README, раздел 4)

- `ContrastTests` подхватит тему сам. Добавьте пары, которых там нет: полоски предметов к `surface`
  (≥ 3:1 — графика) — отдельным тестом `kafedraSubjectAccentsAreVisible`.
- `AccessibilityUiTests` прогонит тему сам. Добавьте снимки «главной» и «шапки лекции» в превью-обёртке
  (без ViewModel: передайте `LoadState.Ready(StudentHome(...))` прямо в `KafedraHome`).
- Unit-тест на `formatTodayRu()` (понедельник/воскресенье, январь/май — падежи).

## 7. Требует backend (не делать на клиенте)

- Срок сдачи теста («до 15 октября») — в `PublicTestDto` нет даты.
- ФИО преподавателя и количество лекций в списке предметов на главной — нет в данных главной; с лекциями можно
  было бы по запросу на каждый предмет, но это N запросов на старте — не делаем.

## 8. Чек-лист приёмки темы B

- [ ] «Кафедра» выбирается в профиле и в диалоге на экране входа; после перезапуска сохраняется.
- [ ] Главная, шапка лекции, итог — по макету B (с исключениями из раздела 7).
- [ ] Панель вкладок плоская с линией сверху; активная вкладка — цвет + жирность.
- [ ] Крупные заголовки на вкладках сворачиваются при прокрутке.
- [ ] Тёмная, высокий контраст, «Очень крупный» + системный 1.3 — без поломок.
- [ ] Тема A не изменилась (снимки до/после совпадают).
- [ ] Все тесты зелёные; iOS-компиляция проходит; README/FONTS обновлены.
