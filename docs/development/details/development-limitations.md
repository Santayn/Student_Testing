# Ограничения процесса разработки

## Высокий приоритет

### Нет migration system

Database development зависит от JPA + Hibernate + `schema.sql`; versioned production upgrade path отсутствует.

**Цель:** Flyway/Liquibase + PostgreSQL integration tests.

### Нет CI

Quality gates существуют, но запускаются вручную.

**Цель:** frontend `npm run quality` и backend `mvn test` на push/PR.

## Средний приоритет

### Нет Java formatter/linter

Frontend имеет ESLint/Prettier/static-quality, backend — только compiler/tests.

### Unix Maven Wrapper

`backend/mvnw` имеет CRLF/permission проблему в текущем snapshot; `.gitattributes` rule `/mvnw` не покрывает вложенный wrapper.

### Dev port mismatch

Vite proxy — `8081`, backend default — `8080`.

### Нет PostgreSQL-specific test layer

H2 не проверяет `schema.sql`, `citext`, partial indexes и реальные lock semantics.

## Процессные пробелы

Отсутствуют `CONTRIBUTING.md`, CODEOWNERS, PR/issue templates, EditorConfig и release/changelog policy.

## Frontend без TypeScript

API shapes защищаются главным образом tests/runtime conventions.

## Hot-spot файлы

Есть крупные View/store/service/controller/DataLoader файлы. При расширении сначала оценивать вынос новой ответственности.

## DataLoader

Смешивает system bootstrap и demo fixtures.

## OpenAPI

Часть metadata поддерживается вручную в `OpenApiConfig`, поэтому новый route может получить generic description.

## Starter metadata

`pom.xml` и `DemoApplication` сохраняют starter naming.

## Root README

Исторически сфокусирован на Docker frontend integration и уже слаб для onboarding.

## Рекомендуемый порядок улучшений

```text
1. CI
2. migrations + PostgreSQL test layer
3. mvnw/port onboarding fixes
4. Java formatter/linter
5. bootstrap/demo separation
6. contribution templates
7. metadata/root README cleanup
```
