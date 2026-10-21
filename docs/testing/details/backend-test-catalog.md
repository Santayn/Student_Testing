# Каталог backend-тестов

| Test class | `@Test` | Уровень | Основное назначение |
|---|---:|---|---|
| `DemoApplicationTests` | 2 | SpringBootTest | Запуск полного Spring context и базовые application-level проверки. |
| `DataLoaderIntegrationTests` | 4 | SpringBootTest | Создание/повторный запуск demo seed, связи учебной модели и retirement membership. |
| `DotNetPasswordHasherTests` | 4 | Unit/Mockito | Совместимый PBKDF2 password hashing, неверные пароли и некорректные параметры hash. |
| `PublicLearningSecurityIntegrationTests` | 3 | SpringBootTest | Student Learning security contract: metadata без расходования попытки, ownership и отсутствие correctAnswer. |
| `SecurityAccessRegressionTests` | 11 | SpringBootTest | Регрессия endpoint-доступа по ролям, raw attempt/admin API, database backup permissions и отчёт расхода ИИ. |
| `CurrentUserAccessServiceTests` | 2 | Unit/Mockito | Object-level authorization для чужих test/group contexts. |
| `AiJsonAndVerdictTests` | 9 | Unit | Разбор JSON от модели, вердикт проверки, части скана, белый список хостов картинок. |
| `AiPipelineIntegrationTests` | 7 | SpringBootTest | Проверка ответа ИИ (уверенно, неуверенно, обман, сбой провайдера), очередь преподавателя, скан PDF, план и сборка презентации, учёт токенов в отчёте расхода. |
| `OpenAiCompatibleChatClientTests` | 4 | Unit (loopback HTTP) | Формат запроса Yandex/OpenAI, заголовки, ошибки без утечки ключа. |
| `PptxPresentationBuilderTests` | 2 | Unit | Все виды слайдов, картинка, заметки, PDF-копия с фоном титула. |
| `LectureDeletionIntegrationTests` | 5 | SpringBootTest | Удаление лекции с материалами (файлы — после коммита), темами, связями с тестами и ИИ-задачами; запрет при вопросах, правилах отбора и попытках студентов. |
| `LectureTextExporterTests` | 3 | Unit | Выгрузка текста лекции в DOCX/PDF: заголовки, списки, таблицы, кириллица. |
| `QuestionDocxImportParserTests` | 2 | Unit/Mockito | Разбор DOCX questions/options/matching/points. |
| `TestServiceAttemptLimitTests` | 2 | Unit/Mockito | Лимит попыток по Test + Person и наличие pessimistic locking в repository contract. |
| `TestServiceConcurrencyIntegrationTests` | 2 | SpringBootTest | Два конкурентных startAttempt при лимите 1; удаление теста с начатыми попытками и ответами. |
| `TextAnswerEvaluationServiceTests` | 3 | Unit/Mockito | Оркестрация text-answer grading и partial/exact behavior. |
| `TextAnswerEvaluatorTests` | 8 | Unit/Mockito | Нормализация и эвристики текста: опечатки, аббревиатуры, отрицания и semantic similarity. |

## Итог

- test classes: **17**;
- `@Test` methods: **73**;
- classes с `@SpringBootTest`: **8**;
- отдельного Testcontainers/PostgreSQL suite нет.

Количество методов не является процентом покрытия и не характеризует автоматически качество покрытия каждого сервиса.
