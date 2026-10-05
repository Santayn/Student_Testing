# Рабочий процесс изменения

## 1. Определить домен

Выберите предметную область: auth/users, academic structure, teaching, courses/lectures, questions, tests, results, UI или infrastructure.

## 2. Изучить существующий путь

Перед изменением прочитайте соответствующие:

- Entity/Service/Controller;
- frontend API module/composable;
- ownership checks;
- tests;
- `docs/api/` и `docs/technical/`.

## 3. Спроектировать вертикальный diff

Типовой путь:

```text
DB/entity
→ repository
→ service
→ access check
→ endpoint
→ DTO
→ frontend API
→ composable
→ view
→ tests
→ docs
```

## 4. Реализовать слоями

Рекомендуемый порядок:

1. модель и бизнес-правило;
2. repository/service;
3. security;
4. REST contract;
5. backend tests;
6. frontend API;
7. composable/state;
8. UI/navigation;
9. frontend tests;
10. docs.

## 5. Security audit

Для каждой операции проверить authentication, permission, ownership, privacy response и прямой HTTP-доступ вне UI.

## 6. Regression test

Bugfix должен оставить тест, воспроизводящий исходную проблему.

## 7. Quality gates

Frontend:

```bash
npm ci
npm run quality
```

Backend:

```powershell
.\mvnw.cmd test
```

## 8. Документация

Изменение поведения обновляет соответствующий раздел `docs/`.

## 9. Review

Перед handoff пройти [review checklist](./review-checklist.md).
