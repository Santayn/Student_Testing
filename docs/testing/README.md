# Тестирование Student Testing

Раздел описывает фактическую стратегию проверки проекта: frontend-тесты, backend-тесты, статические quality gates, security/regression проверки, системный API smoke и текущие пробелы тестовой инфраструктуры.

Документация намеренно разделяет **существующие гарантии** и **целевые проверки**, которые ещё не реализованы. Наличие теста на H2, source-level UI contract или API smoke не трактуется как эквивалент PostgreSQL integration, browser E2E или visual regression.

## [Обзор стратегии тестирования](./details/testing-overview.md)

Какие уровни тестирования существуют, что именно они защищают и где проходят границы ответственности каждого уровня.

## [Уровни тестирования](./details/test-levels.md)

Карта от статической проверки и unit tests до integration/security tests и системного API smoke.

## Frontend

- [Frontend testing](./details/frontend-testing.md) — Vitest, Vue Test Utils, jsdom, contract/race/UI tests.
- [Каталог frontend-тестов](./details/frontend-test-catalog.md) — все 166 spec-файлов по доменам.
- [Frontend quality gate](./details/frontend-quality-gate.md) — `npm run quality` и последовательность проверок.
- [Static quality](./details/frontend-static-quality.md) — отдельный архитектурный quality script.

## Backend

- [Backend testing](./details/backend-testing.md) — JUnit 5, Spring Boot Test, Mockito, H2.
- [Каталог backend-тестов](./details/backend-test-catalog.md) — все 12 test classes и их назначение.
- [Backend test profile](./details/backend-test-profile.md) — профиль `test`, H2 и отличие от PostgreSQL runtime.

## Критические домены

- [Security testing](./details/security-testing.md) — авторизация, разграничение ролей и student contract.
- [Попытки и конкурентность](./details/attempt-and-concurrency-testing.md) — attempt limits, locking и concurrency regression.
- [Тестовые данные и fixtures](./details/test-data-and-fixtures.md) — DataLoader, H2 fixtures и ограничения demo data.

## Системные проверки и регрессия

- [API/system smoke](./details/api-system-smoke.md) — `full-system-smoke.ps1`, охват и текущая несинхронность с API.
- [Regression strategy](./details/regression-strategy.md) — что считается регрессией и как строится защитная сетка проекта.
- [Regression checklist](./details/regression-checklist.md) — практический pre-release checklist.

## Запуск и автоматизация

- [Запуск тестов](./details/running-tests.md) — актуальные команды frontend/backend и prerequisites.
- [CI и quality gates](./details/ci-and-quality-gates.md) — что должно выполняться автоматически и чего пока нет.
- [Coverage и метрики](./details/coverage-and-metrics.md) — какие метрики доступны и почему процент покрытия сейчас неизвестен.
- [E2E strategy](./details/e2e-strategy.md) — целевая browser + PostgreSQL E2E-схема.

## [Ограничения тестирования](./details/testing-limitations.md)

Непокрытые области, ложные эквиваленты тестовых уровней и приоритеты дальнейшего развития test suite.
