# Каталог backend-тестов

| Test class | `@Test` | Уровень | Основное назначение |
|---|---:|---|---|
| `DemoApplicationTests` | 2 | SpringBootTest | Запуск полного Spring context и базовые application-level проверки. |
| `DataLoaderIntegrationTests` | 4 | SpringBootTest | Создание/повторный запуск demo seed, связи учебной модели и retirement membership. |
| `DotNetPasswordHasherTests` | 4 | Unit/Mockito | Совместимый PBKDF2 password hashing, неверные пароли и некорректные параметры hash. |
| `PublicLearningSecurityIntegrationTests` | 3 | SpringBootTest | Student Learning security contract: metadata без расходования попытки, ownership и отсутствие correctAnswer. |
| `SecurityAccessRegressionTests` | 9 | SpringBootTest | Регрессия endpoint-доступа по ролям, raw attempt/admin API и database backup permissions. |
| `CurrentUserAccessServiceTests` | 2 | Unit/Mockito | Object-level authorization для чужих test/group contexts. |
| `LocalLlmTextAnswerEvaluationServiceTests` | 4 | Unit/Mockito | Local-only LLM endpoint, parsing результата и deterministic fallback. |
| `QuestionDocxImportParserTests` | 2 | Unit/Mockito | Разбор DOCX questions/options/matching/points. |
| `TestServiceAttemptLimitTests` | 2 | Unit/Mockito | Лимит попыток по Test + Person и наличие pessimistic locking в repository contract. |
| `TestServiceConcurrencyIntegrationTests` | 1 | SpringBootTest | Два конкурентных startAttempt при лимите 1: в БД должна остаться одна попытка. |
| `TextAnswerEvaluationServiceTests` | 3 | Unit/Mockito | Оркестрация text-answer grading и partial/exact behavior. |
| `TextAnswerEvaluatorTests` | 8 | Unit/Mockito | Нормализация и эвристики текста: опечатки, аббревиатуры, отрицания и semantic similarity. |

## Итог

- test classes: **12**;
- `@Test` methods: **44**;
- classes с `@SpringBootTest`: **5**;
- отдельного Testcontainers/PostgreSQL suite нет.

Количество методов не является процентом покрытия и не характеризует автоматически качество покрытия каждого сервиса.
