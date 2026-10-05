# Backend testing

Backend использует JUnit 5 через `spring-boot-starter-test`, Spring Security Test, Mockito/AssertJ и H2 в test profile.

Текущий каталог содержит **12 test classes** и **44 `@Test` methods**.

## Основные группы

### Алгоритмические unit tests

Без полного Spring context проверяются:

- `DotNetPasswordHasher`;
- text-answer evaluators;
- Local LLM fallback/validation;
- DOCX parser;
- `CurrentUserAccessService`;
- attempt-limit service logic.

### Spring Boot integration

`@SpringBootTest` используется для:

- basic context startup;
- DataLoader integration;
- student-learning/security contracts;
- security access regression;
- concurrent attempt start.

## Тестовая БД

Spring integration tests используют **H2 in PostgreSQL compatibility mode**, а не PostgreSQL. Подробнее: [backend-test-profile.md](./backend-test-profile.md).

## Сильные backend области

- security regression;
- student public-learning contract;
- text-answer grading;
- password compatibility;
- DataLoader consistency;
- attempt limit/locking;
- concurrent start attempt;
- DOCX parser.

## Слабее покрыты

Большинство обычных CRUD/service domains не имеют собственных targeted unit/integration suites: faculties, groups, subjects, course service, lectures/materials, role/user CRUD и большая часть teaching service.

Полный каталог: [backend-test-catalog.md](./backend-test-catalog.md).
