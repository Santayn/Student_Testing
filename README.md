# Student Test Platform

Spring Boot приложение для платформы тестирования студентов. Проект использует новую модель данных из папки `TestPlatformBackend`, но основной запускаемый код находится в `src`.

Приложение включает REST API, статический HTML/JS фронтенд, JWT-аутентификацию, управление учебной структурой, курсами, лекциями, тестами, вопросами, попытками и результатами.

## Текущий статус

Основной код Java адаптирован под новую схему БД `TestPlatformBackend`:

- пользователи работают через `Users`, `People`, `Roles`, `Permissions`;
- студенты и преподаватели представлены через `Person` и membership-таблицы;
- курсы построены через `CourseTemplates`, `CourseVersions`, `CourseLectures`;
- тесты используют `Tests`, `TestQuestions`, `QuestionOptions`, `TestAssignments`, `TestAttempts`, `QuestionResponses`, `SelectedOptions`;
- вопросы можно импортировать из Word `.docx`;
- вопросы в попытку подставляются случайно;
- для теста можно настроить несколько тематик вопросов по лекциям и лимиты по типам вопросов;
- боковая навигация подключается централизованно через `src/main/resources/static/js/api.js`.

## Стек

- Java 17+
- Spring Boot 3.4.3
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven Wrapper
- Apache POI для импорта `.docx`
- Статический frontend: HTML, CSS, JavaScript

## Структура проекта

```text
.
├── src/
│   ├── main/
│   │   ├── java/org/santayn/testing/
│   │   │   ├── config/               # конфигурация, seed-данные, naming strategy
│   │   │   ├── controller/           # редиректы страниц
│   │   │   ├── models/               # JPA модели новой схемы
│   │   │   ├── repository/           # Spring Data репозитории
│   │   │   ├── security/             # JWT, SecurityConfig, password hashers
│   │   │   ├── service/              # бизнес-логика
│   │   │   └── web/controller/rest/  # REST API
│   │   └── resources/
│   │       ├── static/               # HTML/CSS/JS страницы
│   │       ├── application.yml       # локальная конфигурация Spring Boot
│   │       └── schema.sql            # совместимые SQL-добавления поверх новой БД
│   └── test/                         # JUnit тесты
├── TestPlatformBackend/              # .NET backend, источник новой модели БД
├── docs/                             # заметки по миграции
├── mvnw / mvnw.cmd                   # Maven Wrapper
└── pom.xml
```

## Требования

Перед запуском должны быть установлены:

- JDK 17 или новее;
- PostgreSQL;
- доступ к базе данных `Test`;
- Maven ставить отдельно не нужно, используется `mvnw.cmd`;
- опционально .NET SDK, если нужно поднять схему БД через миграции `TestPlatformBackend`;
- опционально Docker Desktop / Docker Engine + Docker Compose, если приложение нужно запускать в контейнерах.

Проверка Java:

```powershell
java -version
```

Проверка PostgreSQL:

```powershell
psql --version
```

## База данных

Java-приложение работает с новой схемой БД и запускается с:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Это значит:

- Hibernate не создает основную схему автоматически;
- таблицы новой платформы должны уже существовать;
- если таблиц нет, приложение упадет на старте с ошибкой validation;
- `schema.sql` добавляет только совместимые расширения, которые нужны текущей Java-версии.

Текущие настройки подключения находятся в `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/Test
    username: postgres
    password: 20040725
server:
  port: 8081
```

Если у вас другой пароль, порт или имя базы, измените `application.yml`.

### Создание базы

Если базы еще нет:

```sql
CREATE DATABASE "Test";
```

Через `psql`:

```powershell
psql -U postgres -c "CREATE DATABASE ""Test"";"
```

### Создание основной схемы

Основная схема берется из `TestPlatformBackend`. Варианты:

1. Использовать уже готовую базу, созданную .NET-проектом.
2. Применить EF Core миграции из `TestPlatformBackend`.

Пример через .NET:

```powershell
cd TestPlatformBackend
dotnet restore
dotnet ef database update
cd ..
```

В `TestPlatformBackend/appsettings.Development.json` подключение по умолчанию:

```json
"DefaultConnection": "Host=localhost;Port=5432;Database=Test;Username=postgres;Password=1234"
```

Если пароль отличается, обновите connection string перед применением миграций.

### Что добавляет Java `schema.sql`

При запуске Spring Boot выполняется `src/main/resources/schema.sql`. Сейчас он добавляет:

- расширение `citext`;
- колонку `"CourseLectureId"` в `"TestQuestions"`;
- таблицу `"TestQuestionSelectionRules"` для правил выбора вопросов по тематикам;
- индексы для новых связей.

Скрипт идемпотентный: повторный запуск не должен ломать базу.

## Сборка и запуск

Все команды ниже рассчитаны на Windows PowerShell из корня проекта.

Установить зависимости и прогнать тесты:

```powershell
.\mvnw.cmd test
```

Запустить приложение:

```powershell
.\mvnw.cmd spring-boot:run
```

По умолчанию приложение поднимается на:

```text
http://localhost:8081
```

Открыть страницу входа:

```text
http://localhost:8081/login.html
```

Запуск на другом порту:

```powershell
.\mvnw.cmd "-Dspring-boot.run.arguments=--server.port=8082" spring-boot:run
```

Быстрый smoke-start без тестов:

```powershell
.\mvnw.cmd -DskipTests "-Dspring-boot.run.arguments=--server.port=8082" spring-boot:start spring-boot:stop
```

### Docker

В репозитории есть готовый `docker-compose.yml`, который поднимает:

- `postgres` с базой `Test`;
- `db-migrator`, который применяет EF Core миграции из `TestPlatformBackend`;
- `app`, который собирает и запускает Spring Boot backend.

Запуск всего стека:

```powershell
docker compose up --build
```

После запуска будут доступны:

```text
http://localhost:8081
http://localhost:8081/login.html
http://localhost:8081/swagger-ui.html
```

Остановить стек:

```powershell
docker compose down
```

Остановить стек и удалить volume базы данных:

```powershell
docker compose down -v
```

## Seed-данные

В проекте есть `DataLoader`, который создаёт базовые роли, permissions, тестовых пользователей и демо-академическую структуру под текущую модель. По умолчанию он включён:

```yaml
app:
  data-loader:
    enabled: true
```

При необходимости его можно отключить:

```yaml
app:
  data-loader:
    enabled: false
```

Или через переменную окружения:

```powershell
$env:APP_DATA_LOADER_ENABLED='false'
```

Loader создаёт пользователей, если их ещё нет:

| Логин | Пароль | Роль |
| --- | --- | --- |
| `student` | `student1` | `STUDENT` |
| `student2` | `student2` | `STUDENT` |
| `teacher` | `teacher1` | `TEACHER` |
| `teacher2` | `teacher2` | `TEACHER` |
| `admin` | `admin123` | `ADMIN` |

Также он поднимает минимальные справочники и связи для новой схемы:

- факультет `Программная инженерия`;
- группы `ПИ-201` и `ПИ-202`;
- предметы `Информатика` и `Базы данных`;
- привязку предметов к факультету;
- привязку преподавателей к предметам;
- привязку студентов к группам;
- базовый тип нагрузки `Основная нагрузка`.

При этом сам шаблон нагрузки и назначения групп преподавателям loader не создаёт. Их предполагается заводить вручную через админскую страницу.

## Основные страницы

| Страница | Назначение |
| --- | --- |
| `/login.html` | вход |
| `/register.html` | регистрация |
| `/main.html` | главная страница |
| `/profile.html` | личный кабинет |
| `/subjects.html` | список предметов |
| `/lectures.html` | лекции |
| `/lecture-details.html` | детали лекции и доступные тесты |
| `/test.html` | прохождение теста |
| `/create-test.html` | создание теста и назначения |
| `/questions-page.html` | вопросы теста, варианты, импорт Word |
| `/result-list.html` | результаты тестов |
| `/users.html` | управление ролями пользователей |
| `/groups.html` | группы |
| `/manage_students.html` | управление студентами |
| `/manage-teachers.html` | учебная нагрузка |
| `/manage-lectures-subject.html` | управление лекциями |
| `/manage-topics-subject.html` | управление курсами |

Также есть редиректы старых URL в `PageRedirectController`, например `/kubstuTest`, `/kubstuTest/subjects`, `/kubstuTest/results`.

## Аутентификация

Frontend хранит access token в `localStorage`:

```text
student-testing.access-token
```

Общий JS-клиент находится в:

```text
src/main/resources/static/js/api.js
```

Он:

- добавляет `Authorization: Bearer ...` к API-запросам;
- управляет текущим пользователем;
- скрывает/показывает teacher/admin пункты меню;
- автоматически добавляет боковую панель на страницы без статического sidebar;
- привязывает кнопку выхода.

## Основные API-группы

Все основные endpoints доступны с двумя префиксами:

```text
/api/...
/api/v1/...
```

Ключевые группы:

| API | Назначение |
| --- | --- |
| `/api/v1/auth/*` | login, register, refresh, текущий пользователь |
| `/api/v1/users/*` | пользователи, роли, permissions, people |
| `/api/v1/roles/*` | роли и permissions |
| `/api/v1/faculties/*` | факультеты |
| `/api/v1/groups/*` | группы |
| `/api/v1/subjects/*` | предметы |
| `/api/v1/memberships/*` | memberships факультетов, групп, предметов |
| `/api/v1/courses/*` | шаблоны курсов и версии |
| `/api/v1/lectures/*` | лекции курса |
| `/api/v1/teaching/*` | учебная нагрузка и enrollments |
| `/api/v1/tests/*` | тесты, назначения, попытки, правила выборки |
| `/api/v1/questions/*` | вопросы, варианты, импорт Word |
| `/api/v1/public/learning/*` | публичное чтение предметов, лекций и прохождение тестов |
| `/api/v1/results/*` | результаты |

## Swagger / OpenAPI

В проект добавлен `springdoc-openapi-starter-webmvc-ui`.

После запуска приложения Swagger UI доступен по адресу:

```text
http://localhost:8081/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8081/v3/api-docs
```

OpenAPI YAML:

```text
http://localhost:8081/v3/api-docs.yaml
```

Документация ограничена REST-контроллерами из пакета:

```text
org.santayn.testing.web.controller.rest
```

В Swagger добавлена схема авторизации `bearerAuth`. Для защищенных API:

1. Выполните вход через `/api/v1/auth/login`.
2. Скопируйте `accessToken`.
3. Нажмите `Authorize` в Swagger UI.
4. Вставьте токен в формате:

```text
Bearer <accessToken>
```

## Создание теста с несколькими тематиками

Страница:

```text
/create-test.html
```

Общий процесс:

1. Выберите предмет.
2. Выберите курс.
3. Выберите версию курса.
4. Выберите лекцию, к которой будет назначен тест.
5. Заполните название, описание, попытки и период доступности.
6. В блоке `Тематики вопросов` добавьте одну или несколько лекций.
7. Для каждой тематики задайте:
   - `Всего вопросов`;
   - `Текстовых`;
   - `С одним вариантом`;
   - `С несколькими вариантами`.
8. Создайте тест.
9. Перейдите к вопросам и добавьте или импортируйте вопросы по выбранным тематикам.

Внутри API правила хранятся в таблице:

```text
TestQuestionSelectionRules
```

Поля правила:

| Поле | Значение |
| --- | --- |
| `TestId` | тест |
| `CourseLectureId` | лекция-тематика |
| `QuestionCount` | сколько всего вопросов взять из этой тематики |
| `TextQuestionCount` | сколько обязательно текстовых вопросов |
| `SingleAnswerQuestionCount` | сколько обязательно вопросов с одним вариантом |
| `MultipleAnswerQuestionCount` | сколько обязательно вопросов с несколькими вариантами |
| `Ordinal` | порядок правила |

Важно:

- сумма трех счетчиков по типам не может быть больше `QuestionCount`;
- если по тематике не хватает активных вопросов нужного типа, старт попытки вернет ошибку;
- если обязательных типов меньше, чем `QuestionCount`, оставшиеся вопросы добираются случайно из этой же тематики;
- если у теста нет правил, работает старый режим: случайный выбор из всех активных вопросов теста по `Tests.QuestionCount`.

## Добавление вопросов

Страница:

```text
/questions-page.html
```

Для тестов с правилами тематик нужно выбирать тематику вопроса. Она соответствует лекции курса и сохраняется в:

```text
TestQuestions.CourseLectureId
```

Типы вопросов:

| Значение | Тип |
| --- | --- |
| `1` | текстовый ответ |
| `2` | один вариант |
| `3` | несколько вариантов |

Для вариантов ответа используется таблица:

```text
QuestionOptions
```

## Импорт вопросов из Word

Импорт находится на странице:

```text
/questions-page.html
```

API:

```http
POST /api/v1/questions/import?testId={id}&courseLectureId={lectureId}
```

Файл должен быть `.docx`.

Поддерживаемый формат:

```text
Что такое JVM?
A) Java Virtual Machine *
B) Java Visual Machine
C) Java Version Manager

Какой оператор используется для наследования в Java?
A) extends *
B) implements
C) inherits
```

Также поддерживаются:

- варианты `A)`, `Б)`, `1)`;
- отметка правильного варианта через `*` или `+`;
- строка вида `Ответ: A, C`;
- вопросы, разделенные пустой строкой.

При импорте для теста с правилами обязательно выберите тематику. Иначе вопросы не попадут в нужный тематический пул.

## Прохождение теста

Обычно студент открывает тест из деталей лекции:

```text
/lecture-details.html
```

Ссылка ведет на:

```text
/test.html?testId={testId}&assignmentId={assignmentId}
```

При старте попытки:

1. Проверяется доступность назначения.
2. Проверяется лимит попыток.
3. Если есть незавершенная попытка, она возобновляется.
4. Если попытка новая, вопросы выбираются случайно.
5. Если у теста есть правила тематик, выбор идет по этим правилам.
6. Для выбранных вопросов создаются пустые `QuestionResponses`.

После отправки ответов попытка закрывается, баллы считаются автоматически для текстовых и вариантных вопросов.

## Навигация

Боковая панель управляется централизованно в:

```text
src/main/resources/static/js/api.js
```

Если страница не содержит `.sidebar`, общий JS добавляет его автоматически. Если страница содержит старый статический sidebar, общий JS приводит его к единому списку пунктов.

Страницы входа и регистрации исключены из автодобавления sidebar.

## Тесты

Запуск всех тестов:

```powershell
.\mvnw.cmd test
```

Сейчас тестовый набор проверяет:

- старт Spring context;
- совместимость password hasher;
- парсер импорта `.docx`;
- отсутствие старых frontend API-вызовов;
- наличие общего sidebar script;
- наличие workflow тематик вопросов в статических страницах и схеме.

Быстрая компиляция без тестов:

```powershell
.\mvnw.cmd -DskipTests test
```

Проверка синтаксиса общего JS:

```powershell
node --check src/main/resources/static/js/api.js
```

Проверка inline script в страницах:

```powershell
node -e "const fs=require('fs'); for (const f of ['src/main/resources/static/create-test.html','src/main/resources/static/questions-page.html']) { const html=fs.readFileSync(f,'utf8'); let i=0; for (const match of html.matchAll(/<script>([\s\S]*?)<\/script>/g)) { new Function(match[1]); i++; } console.log(f + ': ' + i + ' inline scripts ok'); }"
```

## Частые проблемы

### Приложение падает на Hibernate validation

Причина: база не содержит таблицы новой схемы.

Что сделать:

1. Проверьте, что подключение идет к правильной базе `Test`.
2. Примените миграции `TestPlatformBackend`.
3. Проверьте регистр имен таблиц. Новая схема использует quoted identifiers: `"Users"`, `"Tests"`, `"TestQuestions"`.

### Ошибка подключения к PostgreSQL

Проверьте:

- запущен ли PostgreSQL;
- совпадает ли порт;
- существует ли база `Test`;
- верны ли `username` и `password` в `application.yml`.

### 401 Unauthorized

Причина: нет JWT токена или токен устарел.

Что сделать:

1. Выйдите из системы.
2. Зайдите снова через `/login.html`.
3. Если пользователь создан вручную, проверьте `Users.IsActive`.

### 403 Forbidden

Причина: у пользователя нет роли или permission.

Для административных страниц обычно нужны `ADMIN` или соответствующие permissions. Для преподавательских страниц нужны `TEACHER`/`ADMIN` и membership-записи.

### Тест не стартует: не хватает вопросов

Если у теста настроены тематики, проверьте:

- у вопросов заполнен `CourseLectureId`;
- вопросы активны;
- тип вопроса соответствует лимитам правила;
- по каждой тематике достаточно вопросов.

Например, если правило требует `SingleAnswerQuestionCount = 3`, то в этой тематике должно быть минимум 3 активных вопроса типа `2`.

### Импорт Word прошел, но вопросы не попадают в тест

Для тестов с тематиками при импорте нужно выбрать тематику. Иначе вопрос будет без `CourseLectureId` и не попадет в тематическую выборку.

## Полезные команды

Статус Git:

```powershell
git status --short
```

Список HTML страниц:

```powershell
Get-ChildItem -Path 'src/main/resources/static' -Filter '*.html' | Select-Object Name
```

Поиск старых API-вызовов:

```powershell
Select-String -Path 'src/main/resources/static/*.html','src/main/resources/static/js/*.js' -Pattern '/public/tests|/teacher-results|/legacy'
```

Запуск на временном порту:

```powershell
.\mvnw.cmd "-Dspring-boot.run.arguments=--server.port=8082" spring-boot:run
```

## Разработка

Рекомендуемый порядок работы:

1. Перед изменениями проверить состояние:

```powershell
git status --short
```

2. Запустить тесты:

```powershell
.\mvnw.cmd test
```

3. Вносить изменения вертикальным срезом: модель, репозиторий, сервис, контроллер, frontend.

4. После изменений проверить:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests "-Dspring-boot.run.arguments=--server.port=8082" spring-boot:start spring-boot:stop
```

5. Для frontend-правок дополнительно проверить JS:

```powershell
node --check src/main/resources/static/js/api.js
```

## Дополнительная документация

Заметки по миграции на новую модель:

```text
docs/migration-to-test-platform-backend.md
```

Источник новой схемы и поведения:

```text
TestPlatformBackend
```
