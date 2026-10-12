# Тема C «Зачёт»

**Статус:** DONE (реализация — раздел 10)  
**Перед началом:** прочитайте [README.md](README.md) целиком (архитектура тем, правила, проверка).  
**Макет:** `mockups/varianty-dizaina.html`, ряд **C** (Главная, Лекция, Вопрос теста, Результат).  
**Образец реализации:** тема A — `core/ui/design/PolytechTheme.kt`, `feature/home/PolytechHome.kt`,
`feature/learning/LectureHero.kt`, `feature/tests/ResultHero.kt`.

## Идея

Тёмная, «игровая» тема для студентов, которые учатся с телефона вечером: глубокий тёмно-синий фон, ярко-зелёный
и небесный акценты, **«объёмные» кнопки и карточки** (нижний край темнее, как у физической кнопки), кольца
прогресса, **серия дней подряд**. Крупный жирный шрифт Rubik.

Ключевые отличия от A:

| | A «Политех» | C «Зачёт» |
|---|---|---|
| Шрифт | Unbounded + Onest | **Rubik** (один на всё, веса до 800) |
| Основной режим | светлый | **тёмный** (светлая версия тоже обязательна) |
| Кнопки | «пилюля», плоские | скругление **18dp**, высота **58dp**, **«объёмные» (5dp)**, подпись ПРОПИСНЫМИ |
| Карточки | 20dp, плоские | 18dp, **«объёмные» (4dp)** |
| Нижняя навигация | «пилюля» с подписью активной | **только иконки**, активная — небесный скруглённый квадрат |
| Заголовки разделов | акцидентный шрифт | жирный 18sp (`SectionTitleStyle.BOLD`, уже есть) |
| Варианты ответа | карточки | карточки **с «объёмом»** (`AnswerStyle.CARDS` + `cardDepth` — уже работает) |

«Объём» уже реализован: `Modifier.depth(depth, shape, base)` в `Components.kt` применяется к `AppCard`,
`BusyButton` и `AnswerOptionCard`, если `buttonDepth`/`cardDepth` > 0. Тема A задаёт 0, поэтому его не видно.

---

## 1. Шрифт

```
curl -L -o mobile/shared/src/commonMain/composeResources/font/rubik.ttf "https://github.com/google/fonts/raw/main/ofl/rubik/Rubik%5Bwght%5D.ttf"
```

Веса: 400, 500, 600, 700, 800. `displayFont = bodyFont`. Строка в `mobile/FONTS.md`:
`rubik.ttf | Rubik (вариативный, wght) | «Зачёт» — весь текст | ofl/rubik/Rubik[wght].ttf`.

| Где | Размер | Вес |
|---|---|---|
| Заголовок лекции, вопрос теста | 26–28sp | 800 |
| Имя на главной, заголовок итога | 22–30sp | 700–800 |
| Подписи кнопок | 17–18sp | 800, ПРОПИСНЫЕ (см. 4.2) |
| Карточки: заголовок / подпись | 16sp 600 / 13sp 400 | |

---

## 2. Палитры (проверены: все пары `ContrastTests` ≥ 4.5:1)

`core/ui/design/ZachetTheme.kt`. Тёмная — основная (как в макете); светлая — для тех, у кого выбран светлый
режим (`ThemeMode.LIGHT` или светлая система). Обе обязательны.

```kotlin
internal val ZachetDark = darkColorScheme(
    primary = Color(0xFF9BD5FF), onPrimary = Color(0xFF0A1A3F),
    primaryContainer = Color(0xFF1B3573), onPrimaryContainer = Color(0xFFC9E9FF),
    secondary = Color(0xFF11D56B), onSecondary = Color(0xFF04261A),
    secondaryContainer = Color(0xFF0E3B2A), onSecondaryContainer = Color(0xFF7CF0B0),
    tertiary = Color(0xFFFFB86B), onTertiary = Color(0xFF2A1A06),
    tertiaryContainer = Color(0xFF2A1A06), onTertiaryContainer = Color(0xFFFFD4A3),
    error = Color(0xFFFFA8A0), onError = Color(0xFF3D0A06),
    errorContainer = Color(0xFF3D1A1A), onErrorContainer = Color(0xFFFFECEA),
    background = Color(0xFF0A1A3F), onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF13285C), onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1B3573), onSurfaceVariant = Color(0xFFB4C2E3),
    surfaceContainerLowest = Color(0xFF0A1A3F), surfaceContainerLow = Color(0xFF0F2150),
    surfaceContainer = Color(0xFF13285C), surfaceContainerHigh = Color(0xFF1B3573),
    surfaceContainerHighest = Color(0xFF22407F),
    outline = Color(0xFF6D86C7), outlineVariant = Color(0xFF26407F),
    inverseSurface = Color(0xFFFFFFFF), inverseOnSurface = Color(0xFF13285C),
    inversePrimary = Color(0xFF1F48C9), scrim = Color(0xFF000000),
)

internal val ZachetLight = lightColorScheme(
    primary = Color(0xFF1F48C9), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF), onPrimaryContainer = Color(0xFF142F86),
    secondary = Color(0xFF0B6B37), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF8E8), onSecondaryContainer = Color(0xFF0B6B37),
    tertiary = Color(0xFF8A4B00), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1D6), onTertiaryContainer = Color(0xFF8A4B00),
    error = Color(0xFFB42318), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DC), onErrorContainer = Color(0xFF9B1C0C),
    background = Color(0xFFEEF3FF), onBackground = Color(0xFF0A1A3F),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0A1A3F),
    surfaceVariant = Color(0xFFE1E9FA), onSurfaceVariant = Color(0xFF3D4F7A),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F8FF), surfaceContainerHigh = Color(0xFFE1E9FA),
    surfaceContainerHighest = Color(0xFFD3DEF5),
    outline = Color(0xFF6F80AD), outlineVariant = Color(0xFFC9D5EE),
    inverseSurface = Color(0xFF0A1A3F), inverseOnSurface = Color(0xFFEEF3FF),
    inversePrimary = Color(0xFF9BD5FF), scrim = Color(0xFF000000),
)

internal val ZachetDarkStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFF0E3B2A), successText = Color(0xFF7CF0B0),
    warning = Color(0xFFFFB86B), warningSoft = Color(0xFF2A1A06), warningText = Color(0xFFFFD4A3),
    danger = Color(0xFFFF8A80), dangerSoft = Color(0xFF3D1A1A), dangerText = Color(0xFFFFA8A0),
    info = Color(0xFF9BD5FF), infoSoft = Color(0xFF102A55), infoText = Color(0xFFC9E9FF),
    primarySoft = Color(0xFF1B3573), primarySoftText = Color(0xFFC9E9FF),
    shell = Color(0xFF13285C), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFFB4C2E3),
)

internal val ZachetLightStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFFDDF8E8), successText = Color(0xFF0B6B37),
    warning = Color(0xFFF59E0B), warningSoft = Color(0xFFFFF1D6), warningText = Color(0xFF8A4B00),
    danger = Color(0xFFE5484D), dangerSoft = Color(0xFFFFE1DC), dangerText = Color(0xFF9B1C0C),
    info = Color(0xFF1179D5), infoSoft = Color(0xFFE0F0FF), infoText = Color(0xFF045BA7),
    primarySoft = Color(0xFFDCE6FF), primarySoftText = Color(0xFF142F86),
    shell = Color(0xFF0A1A3F), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF3D4F7A),
)

internal val ZachetDarkBrand = BrandColors(
    hero = Color(0xFF13285C), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFB4C2E3),
    heroChip = Color(0xFF1B3573),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF04261A),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0A1A3F),
)

// В светлом режиме фирменные панели остаются тёмно-синими — это узнаваемая часть темы.
internal val ZachetLightBrand = BrandColors(
    hero = Color(0xFF0A1A3F), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFB4C2E3),
    heroChip = Color(0xFF1B3573),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF04261A),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0A1A3F),
)
```

Дополнительный цвет «серии дней» — оранжевый огонь: значок `#FF9F43`, текст на тёмной поверхности
`#FFB86B` (≥ 4.5:1 к `#13285C`). В светлом режиме текст серии — `#8A4B00`. Положите в файл темы.

---

## 3. Токены

```kotlin
@Composable
internal fun zachetTokens(): DesignTokens {
    val rubik = variableFont(Res.font.rubik, listOf(400, 500, 600, 700, 800))
    return DesignTokens(
        theme = DesignTheme.ZACHET,
        displayFont = rubik,
        bodyFont = rubik,
        buttonShape = Shapes.rounded(18.dp),
        buttonHeight = 58.dp,
        buttonDepth = 5.dp,
        cardShape = Shapes.rounded(18.dp),
        cardBorder = false,
        cardDepth = 4.dp,
        chipShape = Shapes.rounded(10.dp),
        fieldShape = Shapes.rounded(14.dp),
        tileShape = Shapes.rounded(14.dp),
        iconButtonsOnSurface = true,
        topBarOnBackground = true,
        sectionTitle = SectionTitleStyle.BOLD,
        navBar = NavBarStyle.FLOATING_ICONS,   // НОВОЕ значение, см. 4.1
        answerStyle = AnswerStyle.CARDS,
    )
}
```

`enum class DesignTheme`: `ZACHET(title = "Зачёт", description = "Тёмная игровая тема: крупные кнопки и прогресс")`.

Формы стандартных Material-компонентов выводятся из этих токенов сами (README, раздел 1, таблица `appShapes()`):
чипы-фильтры — 10dp, поля и меню — 14dp, плавающая кнопка `AppFab` — 18dp, как кнопки. «Объём» (`buttonDepth`)
уже рисуют `BusyButton` и `AppButton`. Контурные кнопки (`outlined`, `AppOutlinedButton`) и `AppFab` остаются
плоскими — так и задумано (у FAB своя тень).

Проверьте «объём» в высоком контрасте: в HC `cardBorder` включается сам, `depth` рисуется темнее `outlineVariant`
HC-палитры — должно читаться. Если некрасиво — в `StudentTestingTheme` при `highContrast` обнуляйте
`buttonDepth`/`cardDepth` (для всех тем это безопасно: у A они и так 0).

---

## 4. Новые виды компонентов

### 4.1. `NavBarStyle.FLOATING_ICONS` — плавающая панель из иконок

Значение в `NavBarStyle` + ветка в `AppNavigationBar` (`navigation/MainShell.kt`). Образец — `FloatingPillBar`
там же (скопируйте структуру и семантику).

- `Surface(shape = RoundedCornerShape(24.dp), color = surface)`, отступы как у `FloatingPillBar`
  (`navigationBarsPadding()`, 16dp по бокам, 12dp снизу), высота ≥ 68dp, без тени (тёмная тема).
- Внутри `Row(Modifier.selectableGroup())`, вкладки равной ширины (`weight(1f)`), каждая:
  `selectable(selected, role = Role.Tab, onClick)` + `semantics(mergeDescendants = true) { contentDescription = tab.label }`
  — **подписей на экране нет, поэтому `contentDescription` обязателен**.
- Активная: прямоугольник 56×48dp, скругление 16dp, фон `Design.brand.highlight`, иконка `onHighlight`.
  Неактивные: иконка `onSurfaceVariant`, фон прозрачный, та же площадь касания 56×48dp.

### 4.2. Подписи кнопок ПРОПИСНЫМИ

Добавьте в `DesignTokens` `buttonLabelUppercase: Boolean` (у C `true`, **в `polytechTokens()` — `false`**).
В `BusyButton` (`Components.kt`): если `true` — на экране `text.uppercase()` весом 800, а для TalkBack — исходный
текст: `Text(text.uppercase(), modifier = Modifier.clearAndSetSemantics { contentDescription = text })`.
Иначе TalkBack может читать короткие слова в прописных по буквам («Д-А-Л-Е-Е»).

То же сделайте в `AppFab` (там подпись уже передаётся строкой и `contentDescription` задан — меняется только
видимый текст). `AppButton` и `AppOutlinedButton` принимают произвольное содержимое (`content: RowScope.() -> Unit`),
поэтому прописными сами не станут. Переведите их на `text: String` + необязательный `icon: ImageVector?`, как у
`BusyButton`, и поправьте вызовы (их немного: `grep -rn "AppButton(\|AppOutlinedButton(" mobile/shared/src`).
Иначе в теме C кнопки будут вперемешку: часть прописными, часть нет.

### 4.3. (По желанию) квадратные кнопки-иконки

В макете C кнопки «назад», «прочитать вслух» — скруглённые квадраты 44dp (14dp), а не круги.
Если делаете — токен `iconButtonShape: Shape` (`CircleShape` в A) и `IconButton(shape = …)` в `AppIconButton`.

---

## 5. Серия дней — общая логика (делается в HomeViewModel, не в теме)

В макете на главной «5 дней подряд». Это **честно считается из данных**: попытки студента с датами завершения
уже грузятся в `HomeViewModel` (`results.getStudentData()`).

1. В `feature/home/HomeData.kt` добавьте чистую функцию и поле:
   ```kotlin
   /** Сколько дней подряд (по местному времени) студент завершал хотя бы одну попытку.
    *  Серия жива, если последняя попытка сегодня или вчера; иначе 0. */
   fun streakDays(completedAt: List<Instant>, today: LocalDate, zone: TimeZone): Int
   ```
   и `val streakDays: Int` в `StudentHome` (заполнить из `attempts.mapNotNull { parseInstantOrNull(it.completedAt) }`,
   `today = nowInstant().toLocalDateTime(zone).date`, `zone = TimeZone.currentSystemDefault()`).
2. Unit-тесты в `commonTest` (`domain/DomainTests.kt` или новый файл): пусто → 0; только сегодня → 1;
   сегодня+вчера+позавчера → 3; разрыв в день → считается только хвост; последняя позавчера → 0;
   две попытки в один день → 1; попытка в 23:30 и 00:30 по местному времени — два разных дня.
3. Показывать серию только если `streakDays >= 1`. Склонение: «1 день / 2 дня / 5 дней подряд»
   (функция рядом со `subjectsWord` в `PolytechHome.kt` — вынесите обе в `HomeData.kt`).

Это поле общее — тема A его просто не показывает.

---

## 6. Фирменные экраны

Общие правила: фон `background`; боковые отступы 20dp; между блоками 16–18dp; «объёмные» карточки — через
`AppCard` (депт уже в токенах), без своих теней.

### 6.1. Главная — `feature/home/ZachetHome.kt`

Сигнатура как у `PolytechHome`. `LazyColumn`, `statusBarsPadding()` у первого элемента.
Тема тёмная → значки статус-бара светлые сами; в светлом режиме C верх экрана на светлом фоне — ничего не делать.

1. **Шапка** (Row): квадрат 48dp, скругление 16dp, фон `brand.highlight`, инициалы 18sp/700 `onHighlight`
   (декоративный — `clearAndSetSemantics {}`); колонка: группа 14sp `onSurfaceVariant` (из `contextLine`,
   до « · ») и имя 22sp/700 (`heading()`); справа — **чип серии** (если `streakDays >= 1`): высота 40dp,
   скругление 20dp, фон `surface`, значок огня 20dp `#FF9F43` (`Icons.Default.LocalFireDepartment`,
   `contentDescription = null`) + текст «5 дней подряд» 15sp/600 цвета серии из раздела 2.
2. `NetworkBanner()`.
3. **Студент:**
   - **Карточка «Продолжить»** (`AppCard(onClick = { openTab(ResultsRoute) })`): слева кольцо 76dp (как `ScoreRing` в `ResultHero.kt` — вынесите
     его в общий `core/ui/ScoreRing.kt` с параметрами размера/толщины): трек `outlineVariant`, заполнение
     `brand.accent`, в центре «50%» 17sp/700; справа: «ПОСЛЕДНИЙ ТЕСТ» 13sp/600 цвета `statusText` для успеха
     (`AppTheme.status.successText`), название теста 19sp/700, строка «50 % верно · 10.10.2026» 14sp
     `onSurfaceVariant`. Для TalkBack карточка — один элемент: «Последний тест: <название>, 50 процентов верно».
     Нет попыток — карточка «Начни с первого предмета» + кнопка `BusyButton(accent = true)` «Мои предметы».
     **«Пройдено 50% предмета» из макета НЕ делать** — прогресса по предмету в API нет; кольцо — результат
     последней попытки.
   - **«Мои предметы»** (`SectionTitle` — BOLD сделает сам) и под ним предметы `AppCard(onClick)`: плитка 44dp
     скругление 14dp (`tileShape`), цвет по индексу — чётные `brand.accent`/`onAccent`, нечётные
     `brand.highlight`/`onHighlight`, внутри инициалы предмета; название 16sp/600, описание 13sp
     `onSurfaceVariant` в одну строку.
     **«Заблокированный» предмет с замком из макета НЕ делать** — такого понятия в системе нет.
     **«Задания на неделю» со сроками НЕ делать** — сроков нет.
4. **Преподаватель / администратор:** шапка без серии + разделы `homeSections(role)` через `NavCard`
   (он перекрасится в «объёмную» карточку сам).

### 6.2. Шапка лекции — ветка `ZACHET` в `LectureHero.kt`

- Строка справа — `SpeakButton(...)` (или квадратная кнопка из 4.3).
- Надзаголовок «Лекция 101» (+ « · версия N») 14sp/600 цвета `brand.highlight` на тёмном / `primary` на светлом
  — проще всего `MaterialTheme.colorScheme.primary` (в тёмной C это и есть небесный).
- Заголовок 28sp/800, lineHeight 32sp, `heading()`.
- Описание 16sp, lineHeight 24sp, `onSurfaceVariant`.

Под шапкой — общие материалы и тесты. **По желанию, после всего остального:** «путь по шагам» из макета
(вертикальная линия с кружками: материалы → тест). Если делаете — это отдельная ветка компоновки тела
`LectureScreen` по теме; состояние шага — только реальное: материал «сохранён на телефоне»
(`savedMaterialIds`), тест «есть незавершённая попытка» (`canResume`) / «попыток не осталось»
(`attemptsRemaining == 0`). «Просмотрено» — нет в данных, не выдумывать.

### 6.3. Шапка итога — ветка `ZACHET` в `ResultHero.kt`

`statusBarsPadding()`, затем:
- Строка: кнопка «назад» (`AppIconButton`, «К лекции»).
- По центру: название теста 15sp/600 `primary`; заголовок 30sp/800 (`heading()` + `liveRegion`) по проценту:
  ≥ 90 % — «Отличный результат!», ≥ 60 % — «Хороший результат», > 0 % — «Есть над чем поработать»,
  0 % — «Попробуй ещё раз». **Не писать «зачёт/незачёт»** — правил оценивания на клиенте нет.
  Под ним «2 из 4 верно · 2 балла из 4» 16sp `onSurfaceVariant` (баллы — только если `score != null`).
- Ряд из 2 «объёмных» плиток (`Row` + `weight(1f)`, `AppCard`): «50%» 26sp/800 цвета `brand.accent` +
  «результат»; «2/4» 26sp/800 цвета `primary` + «верно». Цвет текста на `surface` в светлом режиме —
  проверьте контраст (`brand.accent` на белом не годится → в светлом режиме `status.successText`).
  **Плитки «время 6:18» и «дней серия» из макета НЕ делать** — времени нет в ответе, а серия живёт на главной.

Разбор ответов и кнопки ниже — общие (`TestResultScreen`).

### 6.4. Вопрос теста

Своей компоновки нет: `AnswerStyle.CARDS` + `cardDepth = 4.dp` уже дают «объёмные» карточки-варианты,
сегменты прогресса и нижняя панель шагов — общие.

Одно отличие от A: в макете C рамка **выбранного** варианта зелёная (`brand.accent`), а в A — синяя (`primary`).
Сделайте токеном: `selectedAnswerUsesAccent: Boolean` в `DesignTokens` (C — `true`, **A — `false`**) и в
`AnswerOptionCard` (`Components.kt`): `borderColor = if (selected) (if (tokens.selectedAnswerUsesAccent) Design.brand.accent
else MaterialTheme.colorScheme.primary) else outlineVariant`. Радио/флажок внутри карточки — в тот же цвет через
`RadioButtonDefaults.colors(selectedColor = …)` / `CheckboxDefaults.colors(checkedColor = …, checkmarkColor = brand.onAccent)`.
Контраст рамки к `surface` ≥ 3:1 — проверьте отдельной парой в `ContrastTests`.

---

## 7. Тесты (в дополнение к README, раздел 4)

- `ContrastTests` подхватит тему. Добавьте пары: текст серии / `surface`, «50%» в плитке итога / `surface`
  (оба режима).
- Unit-тесты `streakDays` (раздел 5) — обязательно.
- `AccessibilityUiTests`: тема прогонится сама; добавьте тест панели `FLOATING_ICONS` (отрисовать
  `AppNavigationBar` с 4 вкладками → у каждой `contentDescription`, роль `Tab`, выбранная `assertIsSelected`,
  ATF без ошибок — особенно размер касания 48dp). `AppNavigationBar` сейчас `private` — сделайте `internal`.
- Снимки главной и итога в превью-обёртке (см. README).

## 8. Требует backend (не делать на клиенте)

- Прогресс по предмету («пройдено 50%»), сроки заданий, «время прохождения» попытки,
  блокировка предметов/лекций по порядку прохождения.

## 9. Чек-лист приёмки темы C

- [x] «Зачёт» выбирается в профиле и в диалоге на экране входа; сохраняется.
- [x] Тёмная и светлая палитры; высокий контраст; «Очень крупный» + системный 1.3 — без поломок
      (кнопки 58dp с прописными подписями на 1.5× не обрезают текст — проверить «НАЧАТЬ ТЕСТ», «СЛЕДУЮЩИЙ ВОПРОС»).
- [x] «Объёмные» кнопки и карточки; нижняя навигация из иконок с подписями для TalkBack.
- [x] Серия дней считается из реальных попыток, покрыта unit-тестами.
- [x] Главная, шапка лекции, итог — по макету C (с исключениями из раздела 8).
- [x] Тема A не изменилась (снимки до/после).
- [x] Все тесты зелёные; iOS-компиляция проходит; README/FONTS обновлены.

## 10. Реализация (11.10.2026)

Сделано поверх ветки темы B (`feature/mobile-theme-kafedra`). Проверено на эмуляторе Android 14 (API 34):
юнит-тесты 47/47, `AccessibilityUiTests` 63/63 (три темы), `compileKotlinIosSimulatorArm64` проходит.
Тема A: снимки экранов студента до и после совпадают побайтно, кроме профиля (в «Стиле оформления» теперь
три пункта). Тема B: 16 экранов преподавателя и администратора до и после совпадают побайтно — переделка
общих кнопок их не изменила.

Что сделано не дословно по этому документу и почему:

- **`BrandColors.accentOnSurface`** (новое поле): акцент для линий, значков и чисел на surface. Яркий зелёный
  на белом — 1.9:1, поэтому в светлом «Зачёте» обводка выбранного ответа (6.6:1), кольцо на главной и «50%»
  в плитке итога берут тёмно-зелёный `#0B6B37`; в тёмном — тот же `#11D56B`. Пара добавлена в `ContrastTests`
  для всех тем. Галочка выбранного флажка — цвета surface: `onAccent` на тёмно-зелёном не читается.
- **Подписи прописными** (4.2): программа чтения с экрана получает исходный текст через свойство `text`,
  а не `contentDescription` — поиск кнопок по тексту в UI-тестах работает одинаково во всех темах.
  Подпись в «Зачёте» — 17sp / 800.
- **`AppButton` / `AppOutlinedButton`** принимают `text` и `icon`. Отступ между значком и подписью остался
  пробелом перед текстом, как было в вызовах, — иначе в темах A и B кнопки сдвинулись бы на пару пикселей.
- **Шапка итога** (6.3): под заголовком «2 из 4 верно · баллы: 2», а не «2 балла из 4» — максимального балла
  в ответе нет.
- **Кнопки-иконки** (4.3, по желанию) сделаны: токен `iconButtonShape`, в «Зачёте» — скруглённый квадрат.
- **«Высокий контраст»** (раздел 3): «объём» оставлен — нижний край читается и в светлой, и в тёмной палитре.
- **`AccessibilityUiTests`**: добавлены тест нижней навигации (для каждой темы) и тест кнопок при «Очень крупном»
  тексте со снимком; ветки `ZACHET` в тестах главной, шапки лекции и итога.

Не сделано:

- «Путь по шагам» на экране лекции (6.2, по желанию).
- Экран теста в живом приложении не открывался: вход в тест создаёт попытку на сервере (`startAttempt`).
  Вопросы, варианты и кнопки шагов проверены UI-тестами со снимками.

Осталось нерешённым (то же, что в теме B): экран «О системе» открывается только из шапки главной темы A;
инициалы на главной берутся из имени для приветствия, а не из имени и фамилии.
