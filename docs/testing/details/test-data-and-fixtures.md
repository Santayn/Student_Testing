# Тестовые данные и fixtures

## Backend unit tests

Unit tests создают минимальные entities/DTO/mocks непосредственно внутри test class. Это обеспечивает изоляцию и быстрый запуск.

## H2 integration data

Spring Boot integration tests используют transient H2 database. В зависимости от test class данные создаются через repositories/services или включённый специально для теста DataLoader.

## `DataLoaderIntegrationTests`

Этот suite проверяет сам demo bootstrap:

```text
Faculty
→ Group
→ Subject
→ Teacher membership
→ Teaching assignment
→ Course template/version
→ Lecture
→ Topic
→ Test/questions
→ Test assignment
```

Также проверяется повторный запуск DataLoader и корректное состояние membership после изменений seed.

## Demo DataLoader не должен стать E2E fixture системой

Runtime DataLoader создаёт демонстрационное окружение и известные demo accounts. Он удобен для local/demo запуска, но плохо подходит как долгосрочная база E2E:

- зависит от production startup component;
- создаёт больше данных, чем нужно отдельному тесту;
- затрудняет cleanup;
- делает system smoke зависимым от известных credentials.

## Целевая fixture strategy

Для E2E желательно иметь отдельный test bootstrap:

```text
create isolated DB
   ↓
minimal ADMIN
minimal TEACHER
minimal STUDENT
   ↓
faculty/group/subject
   ↓
assignment/lecture/test
   ↓
run tests
   ↓
destroy DB/containers
```

Fixtures должны быть минимальными, детерминированными и disposable.
