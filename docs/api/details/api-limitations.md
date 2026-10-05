# Текущие ограничения API

## 1. Числовые enum-поля

Статусы, типы, roles и scopes передаются как `int`. Они плохо самодокументируемы и допускают одинаковые числа с разной семантикой в разных доменах.

## 2. Нет пагинации

Списковые endpoint'ы возвращают полные массивы. При росте пользователей, вопросов, memberships и результатов потребуется серверная pagination/filter contract.

## 3. Два current-user endpoint

`GET /auth/me` и `GET /users/me` частично дублируют назначение.

## 4. `public/learning` не anonymous

Namespace называется `public`, но требует authentication. Название может вводить интегратора в заблуждение.

## 5. Student DTO содержит устаревшие поля

`PublicQuestionResponse.text` и `question` дублируют значение. `PublicSubmitDetailResponse.correctAnswer` существует в модели, хотя сервер возвращает `null`.

## 6. HTTP status создания не унифицированы

Часть create endpoints возвращает `201`, часть — `200`.

## 7. Error codes frontend/backend расходятся

Frontend ожидает ряд domain-specific codes и 429 contract, которые backend сейчас не реализует системно.

## 8. Swagger неполон

Не вся security matrix, error response model и часть endpoint descriptions представлены в OpenAPI достаточно точно.

## 9. Management Test API шире UI

Backend содержит низкоуровневые attempt/response routes, которых текущий frontend не использует для студента. Их необходимо сохранять закрытыми administrative authority и не смешивать с Student Learning API.

## 10. Legacy lecture-test contract

`Lecture.linkedTestId` и множественная связь `/lectures/{id}/tests` сосуществуют. Это переходный API-контракт.

## 11. Test duration model

Management DTO использует `LocalTime`, а student DTO публикует duration как `String`. JSON выглядит совместимо, но OpenAPI type/schema различается.

## Рекомендация по версии API

Префикс `/api/v1` уже существует. Любое breaking изменение значения status/type, request shape или student security boundary следует проводить осознанно как изменение versioned contract, а не скрытую замену поведения в рамках того же API.
