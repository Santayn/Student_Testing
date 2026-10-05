# Обзор разработки

## Основная модель

Student Testing — единый репозиторий с Vue frontend и Spring Boot backend. Почти любое существенное изменение проходит вертикально через несколько слоёв:

```text
Database / Entity
       ↓
Repository
       ↓
Service
       ↓
Authorization
       ↓
REST Controller + DTO
       ↓
Frontend API module
       ↓
Composable / View
       ↓
Navigation / UI
       ↓
Tests
       ↓
Documentation
```

Поэтому функция не считается завершённой только потому, что один слой компилируется.

## Стек

| Область | Технология |
|---|---|
| Backend | Java 17, Spring Boot 3.4.3 |
| Persistence | Spring Data JPA / Hibernate |
| Runtime DB | PostgreSQL 16 |
| Backend tests | JUnit 5, Spring Boot Test, Mockito, H2 |
| Frontend | Vue 3.5, JavaScript |
| Build | Vite 8 |
| State | Pinia |
| HTTP | Axios |
| UI | PrimeVue через `components/ui` |
| Frontend tests | Vitest, Vue Test Utils, jsdom |
| Containers | Docker / Docker Compose |

## Источник истины

Backend является источником истины для доступа, ролей, ownership, количества попыток, оценивания и постоянного состояния.

Frontend отвечает за UX, навигацию, локальные черновики, orchestration запросов и представление ошибок.

Client-side проверка не заменяет server-side бизнес-правило.

## Перед разработкой

Нужно определить:

1. какой домен затрагивается;
2. меняется ли модель данных;
3. меняется ли REST-контракт;
4. меняется ли security model;
5. нужен ли route/API/composable;
6. какие regression tests нужны;
7. какая документация перестанет быть актуальной.
