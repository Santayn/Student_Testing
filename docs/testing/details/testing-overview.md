# Обзор стратегии тестирования

Текущая стратегия Student Testing состоит из нескольких независимых уровней. Наиболее развит frontend regression layer; backend сфокусирован на security, попытках тестирования, DataLoader, текстовом оценивании и сложных алгоритмах.

## Текущий инвентарь

| Область | Фактическое состояние |
|---|---:|
| Frontend spec-файлы | **166** |
| Frontend обычные `it/test(...)` declarations | **732** |
| Frontend `it/test.each(...)` declarations | **8** |
| Всего test declarations в исходниках frontend | **740** |
| Backend test classes | **12** |
| Backend `@Test` methods | **44** |
| Полный Spring context (`@SpringBootTest`) | **5 классов** |
| Browser E2E | отсутствует |
| Testcontainers/PostgreSQL integration | отсутствует |
| Coverage thresholds | отсутствуют |
| CI workflow | отсутствует |
| API/system smoke | присутствует, требует синхронизации |

`it.each(...)` может разворачивать одно объявление в несколько runtime cases, поэтому число деклараций не следует трактовать как точное число выполненных test cases.

## Основная идея

Тестовая система ориентирована прежде всего на предотвращение регрессий в критичных пользовательских и security-сценариях:

```text
route/security contracts
        ↓
student attempt lifecycle
        ↓
race/stale response protection
        ↓
teacher/admin workflows
        ↓
UI/theme/mobile contracts
        ↓
backend authorization/concurrency
```

## Что уже хорошо защищено

- маршрутизация и ролевые guards frontend;
- session/auth lifecycle frontend;
- student attempt draft/reload/submit;
- stale response и race-condition сценарии;
- teacher/admin context transitions;
- responsive/theme/UI contracts;
- backend security regression;
- student public-learning contract;
- password hashing;
- DOCX parser;
- text answer grading, включая LLM fallback;
- attempt limit и конкурентный запуск попыток;
- DataLoader consistency.

## Что нельзя считать подтверждённым

Текущие тесты не дают следующих гарантий:

- поведение схемы и блокировок на реальном PostgreSQL;
- корректность `schema.sql`;
- полный JWT lifecycle через настоящий Bearer filter;
- реальный browser frontend → Nginx → backend → PostgreSQL;
- визуальное совпадение UI на уровне screenshots;
- автоматический accessibility audit через axe/Pa11y;
- заданный процент code coverage;
- production migration/upgrade path.

Эти ограничения подробно перечислены в [testing-limitations.md](./testing-limitations.md).
