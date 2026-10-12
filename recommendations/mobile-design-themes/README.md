# Темы оформления мобильного приложения

Дата создания: 11.10.2026

В мобильном приложении (`mobile/`, Kotlin Multiplatform + Compose Multiplatform) три темы оформления на выбор пользователя.
Макеты всех трёх — `mockups/varianty-dizaina.html` (откройте в браузере; ряды A, B, C, по 4 экрана).

| Тема | Идея | Статус | Документ |
|---|---|---|---|
| **A «Политех»** | фирменный стиль kubstu.ru: электрический синий, Unbounded + Onest, «пилюли», плитки-бенто | **DONE** — реализована, по умолчанию | этот файл (как образец) |
| **B «Кафедра»** | спокойно, по-академически: Golos Text, много воздуха, сгруппированные списки, тонкие линии | **DONE** — реализована | [THEME_B_KAFEDRA.md](THEME_B_KAFEDRA.md) |
| **C «Зачёт»** | тёмная и игровая: Rubik, «объёмные» кнопки, кольца прогресса, серия дней | **DONE** — реализована | [THEME_C_ZACHET.md](THEME_C_ZACHET.md) |

Каждая тема B и C — отдельная задача для одной сессии Claude Code. Их можно делать параллельно в разных ветках:
пересекаются они только в нескольких строках `when` (см. «Точки расширения»), конфликты тривиальные.

---

## 1. Как устроены темы (прочитать до начала работы)

Тема — это **не только цвета**. Она задаётся четырьмя слоями:

1. **Палитра** (`ThemePalette`): Material `ColorScheme` + `StatusColors` (успех/ошибка/предупреждение/инфо) +
   `BrandColors` (фирменная панель, акцент, подсветка) — каждое в светлом и тёмном варианте.
2. **Токены** (`DesignTokens`): шрифты, форма и высота кнопок, «объёмность», форма карточек/чипов/полей/плиток,
   рамка у карточек, вид верхней панели, вид заголовков разделов, вид нижней навигации, вид вариантов ответа.
3. **Общие компоненты** (`core/ui/Components.kt`, `navigation/MainShell.kt`) читают токены — поэтому ~40 экранов
   преподавателя и администратора перекрашиваются сами, их трогать не нужно.
   Стандартные Material-компоненты тоже следуют теме: `MaterialTheme.shapes` собирается из токенов
   (`appShapes()` в `Theme.kt`):

   | Material-форма | Токен | Где видно |
   |---|---|---|
   | `extraSmall` | `fieldShape` | поля ввода, выпадающие меню, снекбар |
   | `small` | `chipShape` | чипы-фильтры (`FilterChip`) |
   | `medium` | `cardShape` | `Card` |
   | `large` | `buttonShape` | плавающая кнопка (`AppFab`) |
   | `extraLarge` | 28dp, общее | диалоги, окно выбора, нижние листы |

   Поэтому токены форм — `CornerBasedShape` (`Shapes.rounded(…)`, `Shapes.pill`), а не произвольный `Shape`.
   В экранах **не** берите `MaterialTheme.shapes.small/large` для своих элементов: при `chipShape = pill` строка
   или окно станут «пилюлей»/кругом. Берите нужный токен напрямую (`Design.tokens.tileShape`, `fieldShape`, …).
4. **«Фирменные» экраны** — три места, где у темы своя компоновка, а не только стиль:
   главная студента, шапка лекции, шапка итога теста. Для них у каждой темы свой Composable.

Поверх темы всегда работают пользовательские настройки (их **не** трогать):
- «Тема»: как в системе / светлая / тёмная — тема обязана иметь и светлую, и тёмную палитру;
- «Высокий контраст» — общая палитра для всех тем (`Theme.kt`), формы и шрифты остаются от темы, у карточек включается рамка;
- «Размер текста» ×1 / ×1.25 / ×1.5 поверх системного шрифта — вёрстка темы обязана выдерживать ×1.5 при системном ×1.3.

### Файлы

```
mobile/shared/src/commonMain/kotlin/org/santayn/testing/mobile/
  core/ui/design/DesignTheme.kt   — enum DesignTheme, DesignTokens, BrandColors, ThemePalette,
                                    SectionTitleStyle, NavBarStyle, AnswerStyle, LocalDesign, Design.tokens/brand
  core/ui/design/PolytechTheme.kt — ОБРАЗЕЦ: палитры, шрифты и токены темы A
  core/ui/Theme.kt                — сборка темы, общий «Высокий контраст», реестры palette()/tokens()
  core/ui/Components.kt           — общие компоненты на токенах: AppScreen, AppIconButton, AppCard, NavCard,
                                    SectionTitle, StatusChip, Banner, StatTile, поля, BusyButton(accent), Modifier.depth,
                                    AnswerOptionCard (вариант ответа карточкой), RadioRow/CheckRow (строкой),
                                    AppButton / AppOutlinedButton (кнопки в форме темы), AppFab (плавающая кнопка с подписью)
  navigation/MainShell.kt         — AppNavigationBar: CLASSIC и FLOATING_PILL
  feature/home/HomeData.kt        — HomeViewModel: данные главной (общие для всех тем)
  feature/home/PolytechHome.kt    — главная темы A
  feature/home/HomeScreens.kt     — HomeScreen: выбор компоновки главной по теме
  feature/learning/LectureHero.kt — шапка лекции по теме
  feature/tests/ResultHero.kt     — шапка итога теста по теме
  feature/tests/TestScreen.kt     — тест по одному вопросу (общий), QuestionCard по AnswerStyle
  feature/profile/DisplaySettings.kt — «Стиль оформления»: появляется сам, когда тем > 1
  core/platform/SystemBars.kt     — LightStatusBarIcons(): белые значки статус-бара над цветной панелью
mobile/shared/src/commonMain/composeResources/font/ — шрифты (лицензии: mobile/FONTS.md)
mobile/shared/src/commonTest/.../core/ContrastTests.kt — контраст КАЖДОЙ темы (новая тема проверяется сама)
mobile/shared/src/androidDeviceTest/.../a11y/AccessibilityUiTests.kt — UI-тесты доступности, параметризованы темой
```

### Точки расширения (компилятор сам покажет каждую)

Все выборы по теме сделаны **исчерпывающими `when` без `else`**. Добавили значение в `enum class DesignTheme` —
сборка упадёт ровно в тех местах, где тему нужно подключить:

| Где | Что добавить |
|---|---|
| `Theme.kt` → `DesignTheme.palette()` | `DesignTheme.KAFEDRA -> KafedraPalette` |
| `Theme.kt` → `DesignTheme.tokens()` | `DesignTheme.KAFEDRA -> kafedraTokens()` |
| `HomeScreens.kt` → `HomeScreen` | `DesignTheme.KAFEDRA -> KafedraHome(...)` |
| `LectureHero.kt` → `LectureHero` | `DesignTheme.KAFEDRA -> KafedraLectureHero(...)` |
| `ResultHero.kt` → `ResultHero` | `DesignTheme.KAFEDRA -> KafedraResultHero(...)` |

Если теме нужен новый **вид** компонента (например, C нужна панель навигации `FLOATING_ICONS`, а B — заголовок
экрана «крупным заголовком»), добавляется значение в соответствующий enum токенов (`NavBarStyle`, …) и ветка в
компоненте. Новые **поля** `DesignTokens` добавлять можно, но тогда задайте их и в `polytechTokens()` так, чтобы
тема A выглядела как раньше.

---

## 2. Порядок добавления темы (по шагам)

1. **Ветка.** От `feature/mobile-app` (или `master`, если PR уже влит): `feature/mobile-theme-kafedra` /
   `feature/mobile-theme-zachet`.
2. **Шрифт.** Скачайте вариативный TTF из Google Fonts (OFL) в `composeResources/font/` строчными латинскими
   буквами, без скобок, и допишите строку в `mobile/FONTS.md`:
   ```
   curl -L -o mobile/shared/src/commonMain/composeResources/font/golos_text.ttf "https://github.com/google/fonts/raw/main/ofl/golostext/GolosText%5Bwght%5D.ttf"
   ```
   Проверьте, что это TTF (первые байты `00 01 00 00`), а не HTML-страница. Ресурс станет `Res.font.golos_text`.
3. **Enum.** В `DesignTheme` добавьте значение с `title` и `description` (они показываются в настройках).
4. **Тема.** Создайте `core/ui/design/<Name>Theme.kt` по образцу `PolytechTheme.kt`: `lightColorScheme`,
   `darkColorScheme`, два `StatusColors`, два `BrandColors`, `<Name>Palette`, `<name>Tokens()`.
   Шрифт — через `variableFont(Res.font.xxx, listOf(...весов))` из `PolytechTheme.kt`.
5. **Реестры.** Ветки в `palette()` и `tokens()` в `Theme.kt`.
6. **Новые виды компонентов**, если нужны (см. документ темы): значения enum + ветки в компонентах.
7. **Фирменные экраны.** `feature/home/<Name>Home.kt`, ветки в `LectureHero.kt` и `ResultHero.kt`.
   Данные — только из `HomeViewModel`, `LectureViewModel`, `SubmitAttemptResponse`; если для элемента макета данных
   нет — **не выдумывайте** его (см. «Правила»).
8. **Сборка и тесты** (раздел 4). `ContrastTests` и `AccessibilityUiTests` проверят новую тему автоматически.
9. **Ручная проверка** на эмуляторе: светлая, тёмная, высокий контраст, «Очень крупный» текст (раздел 4).
10. **README**: в `mobile/README.md` (раздел «Темы оформления») и в таблице вверху этого файла смените статус на DONE.
11. **Коммит** в свою ветку; PR — по договорённости с тимлидом.

---

## 3. Правила (нарушение = задача не принята)

- **Бэкенд не меняется.** Нужны данные, которых нет в API, — элемент не делается или делается из имеющихся
  данных честно; запишите это в раздел «Требует backend» документа темы.
- **Не выдумывать данные.** В макетах есть условные элементы (срок «до 15 октября», «осталась 1 попытка»,
  «время 6:18»). Срока теста, времени прохождения и прогресса по предмету в API нет: такие элементы опускаются
  или заменяются реальными (что именно — расписано в документах тем).
- **Тема A не должна измениться.** После работы сравните скриншоты A до/после (раздел 4) — они должны совпадать.
- **Доступность — не хуже A** (ориентир WCAG 2.1 AA / ГОСТ Р 52872-2019):
  - текст ≥ 4.5:1 к фону (крупный ≥ 24sp — ≥ 3:1), высокий контраст ≥ 7:1 — проверит `ContrastTests`;
    каждую новую пару «текст/фон» темы (например, текст на цветной плитке) **добавьте в `textPairs`**;
  - значки и границы, по которым понимают состояние, ≥ 3:1 к фону; яркий зелёный `#11D56B` на белом — 1.9:1,
    поэтому для значков «верно» на светлом фоне берётся `status.successText`, а не `status.success`;
  - сенсорные цели ≥ 48dp; варианты ответа и переключатели — один элемент с ролью и состоянием
    (используйте готовые `AnswerOptionCard`/`RadioRow`/`CheckRow`, не собирайте свои из `clickable`);
  - кнопки-иконки — с подписью (`AppIconButton(icon, label)`), декоративные иконки — `contentDescription = null`;
  - заголовки экранов, вопросов, разделов — `semantics { heading() }`;
  - статус не только цветом: значок + текст («верно»/«неверно»);
  - вёрстка выдерживает «Очень крупный» текст (×1.5) при системном шрифте 1.3 — ничего не обрезается так,
    что теряется смысл.
- **Компоненты, а не копии.** Экраны собираются из `core/ui/Components.kt`. Своё — только в «фирменных» местах.
  В экранах не используйте напрямую `Button`, `OutlinedButton`, `ExtendedFloatingActionButton`: их формы
  и высоты не из темы. Замены: `BusyButton`/`AppButton`, `BusyButton(outlined = true)`/`AppOutlinedButton`, `AppFab`.
  (Исключение — `TextButton` в диалогах и ссылки.) Проверка: `grep -rn "OutlinedButton(\|ExtendedFloatingActionButton(" feature/`
  должна находить только `AppOutlinedButton(`.
- **Плавающие кнопки — только `AppFab`.** У стандартного `ExtendedFloatingActionButton` текст не попадает в дерево
  доступности (в `uiautomator dump` — `NAF="true"`, TalkBack читает «кнопка» без названия). `AppFab` задаёт подпись сам;
  для коротких надписей передавайте `accessibilityLabel` («Тема» → «Новая тема»).
- **Статус-бар.** Если экран рисует цветную панель под статус-баром — вызовите `LightStatusBarIcons()`.
- **Никаких хардкод-цветов** в общих компонентах и фирменных экранах вне файла темы: только
  `MaterialTheme.colorScheme`, `AppTheme.status`, `Design.brand`.

---

## 4. Сборка, тесты, проверка

### Окружение (Windows, путь проекта с кириллицей)

- JDK 21: `JAVA_HOME="C:\Program Files\Java\jdk-21"`; Android SDK с Platform 37.
- Каталоги сборки вынесены в `%USERPROFILE%\.student-testing-build` (иначе тесты не находят классы из-за кириллицы).
- Инкрементальная компиляция Kotlin выключена (`kotlin.incremental=false`) — полная пересборка `shared` ~30–40 с, это нормально.
- Если сборка падает с «Storage … is already registered» — это и есть баг IC; не включайте инкрементальную компиляцию.
- Бэкенд: из корня репозитория `docker compose up -d`; эмулятор ходит на `http://10.0.2.2:8080/api/v1`.
- Тестовые пользователи: `student/student1`, `teacher/teacher1`, `admin/admin123`.
- **Не отправляйте попытки теста** под `student` без нужды: у тестового теста 2 попытки, они кончатся.
  Итог теста проверяйте UI-тестом со снимком (ниже).

### Команды (из `mobile/`)

```
./gradlew :androidApp:compileDebugKotlin             # быстрая проверка компиляции
./gradlew :shared:testAndroidHostTest                # юнит-тесты, в т.ч. ContrastTests по всем темам
./gradlew :shared:compileKotlinIosSimulatorArm64     # общий код компилируется под iOS
./gradlew :androidApp:installDebug                   # поставить на эмулятор/телефон
./gradlew :shared:connectedAndroidDeviceTest         # UI-тесты доступности (каждая тема × каждый тест)
```

Отчёты: `%USERPROFILE%\.student-testing-build\shared\test-results\testAndroidHostTest\*.xml`,
`%USERPROFILE%\.student-testing-build\shared\outputs\androidTest-results\connected\androidMain\*.xml`.

### Снимки экранов из UI-тестов

`AccessibilityUiTests` сохраняет снимки `<тема>-result-hero.png`, `<тема>-test-question-cards.png`.
`connectedAndroidDeviceTest` удаляет тестовый APK вместе со снимками, поэтому для снимков запускайте так:

```
./gradlew :shared:installAndroidDeviceTest
adb shell am instrument -w -e class org.santayn.testing.mobile.a11y.AccessibilityUiTests \
  org.santayn.testing.mobile.shared.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/org.santayn.testing.mobile.shared.test/files/screens ./screens
```

Добавьте в тест снимки своей главной/шапки лекции, если делаете их в превью-обёртке (без ViewModel).

### Ручная проверка на эмуляторе (обязательно)

- Профиль → «Стиль оформления» → своя тема. Пройти: главная → предмет → лекции → лекция → «Продолжить тест»
  (переключать вопросы сегментами, не отправлять) → «Результаты» → «Профиль».
- То же под `teacher`: главная, «Предметы», «Контент» → «Темы», «Вопросы», «Создать тест», «Лекции», «Шаблоны курса»,
  «Моя нагрузка», «Результаты», «Профиль».
- То же под `admin`: **каждый** пункт «Структуры» (Факультеты, Группы → «Состав группы», Справочник предметов,
  Предметы факультетов, Преподаватели и предметы, Учебная нагрузка) и «Доступа» (Пользователи → карточка
  пользователя, Роли и права, Резервные копии), «Главная» → «Результаты».
  Откройте хотя бы по одному: редактор («Добавить»), окно выбора (поле «Преподаватель»), подтверждение удаления
  (нажмите «Удалить», затем «Отмена») — так видны формы диалогов, полей и чипов.
- Сценарием через `adb` переходите осторожно: «Назад» на корневой вкладке закрывает приложение, и следующий
  скриншот снимет рабочий стол. Перед снимком проверяйте, что приложение впереди:
  `adb shell dumpsys activity activities | grep topResumedActivity`. Если приложение пропало — смотрите
  `adb logcat -d -b crash`: это может быть падение, а не промах.
- Режимы: светлая, тёмная (`adb shell cmd uimode night yes|no`), «Высокий контраст»,
  «Очень крупный» + `adb shell settings put system font_scale 1.3` (после — вернуть `1.0`).
- Без сети: `adb shell cmd connectivity airplane-mode enable|disable` — плашка «Нет подключения» видна и читаема в теме.
- Снимок: `adb exec-out screencap -p > shot.png`. Дерево доступности: `adb shell uiautomator dump /sdcard/ui.xml`
  (в Git Bash перед командами adb с путями `/sdcard/...` — `export MSYS_NO_PATHCONV=1`).

### Definition of Done

- [ ] Тема выбирается в профиле и на экране входа (кнопка ♿), выбор сохраняется после перезапуска.
- [ ] Палитры светлая и тёмная; `ContrastTests` зелёные (включая добавленные пары).
- [ ] `AccessibilityUiTests` зелёные на эмуляторе для **всех** тем.
- [ ] Главная, шапка лекции, итог теста — по макету темы; остальные экраны выглядят цельно (общие компоненты).
- [ ] Пройдены все экраны преподавателя и администратора (список в «Ручной проверке»), включая диалоги:
      ничего не «выпадает» из темы (чужие формы кнопок, круглые окна, поля-«пилюли»), падений нет.
- [ ] «Очень крупный» текст, высокий контраст, тёмная — без поломок вёрстки.
- [ ] Тема A не изменилась (сравнение снимков).
- [ ] `compileKotlinIosSimulatorArm64` проходит.
- [ ] Обновлены `mobile/README.md`, `mobile/FONTS.md`, таблица статусов вверху этого файла.

---

## 5. Промпт для запуска сессии Claude Code

Откройте Claude Code в корне репозитория и вставьте (замените `THEME_B_KAFEDRA.md` на `THEME_C_ZACHET.md` для темы C):

```
Задача: добавить в мобильное приложение (папка mobile/, Kotlin Multiplatform + Compose) новую тему оформления.
Прочитай целиком recommendations/mobile-design-themes/README.md и recommendations/mobile-design-themes/THEME_B_KAFEDRA.md
и сделай всё, что там описано, строго по шагам раздела 2 README.
Перед кодом изучи образец темы A: mobile/shared/src/commonMain/kotlin/org/santayn/testing/mobile/core/ui/design/PolytechTheme.kt,
feature/home/PolytechHome.kt, feature/learning/LectureHero.kt, feature/tests/ResultHero.kt.
Макеты — recommendations/mobile-design-themes/mockups/varianty-dizaina.html (нужный ряд).
Бэкенд не меняй, данные не выдумывай, тему A не ломай. Работай в отдельной ветке.
В конце: все тесты из раздела 4, ручная проверка на эмуляторе, снимки экранов, отчёт что сделано и что нет.
```
