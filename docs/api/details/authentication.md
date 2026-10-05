# API аутентификации

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `POST` | `/api/v1/auth/login` | Выполнить вход и получить пару access/refresh token | Anonymous | `200` | `auth.api.js` |
| `POST` | `/api/v1/auth/register` | Зарегистрировать учётную запись | Anonymous | `201` | `auth.api.js` |
| `POST` | `/api/v1/auth/refresh` | Обновить пару токенов | Anonymous | `200` | `auth.api.js` |
| `POST` | `/api/v1/auth/revoke` | Отозвать refresh token | Authenticated | `204` | `auth.api.js` |
| `POST` | `/api/v1/auth/change-password` | Изменить пароль текущего пользователя | Authenticated | `204` | `auth.api.js` |
| `GET` | `/api/v1/auth/me` | Получить сведения о текущем пользователе | Authenticated | `200` | `auth.api.js` |

## Login

```http
POST /api/v1/auth/login
```

```json
{
  "login": "teacher",
  "password": "********",
  "lifetimeKind": 1
}
```

`lifetimeKind` опционален. Основные режимы сервиса: `1` — стандартная сессия, `2` — расширенная. Невалидное/отсутствующее значение нормализуется сервисом к стандартному режиму.

Типичный успешный ответ:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "accessTokenExpiresAtUtc": "...",
  "refreshToken": "...",
  "refreshTokenExpiresAtUtc": "...",
  "lifetimeKind": 1
}
```

## Register

`POST /auth/register` доступен на уровне Spring Security без access token, но сама возможность регистрации может быть отключена конфигурацией `APP_PUBLIC_REGISTRATION_ENABLED`. Новый аккаунт не получает автоматически рабочую роль и `Person` profile.

## Bearer access token

Все защищённые endpoint'ы используют:

```http
Authorization: Bearer <accessToken>
```

Backend stateless: серверная HTTP session не используется.

## Refresh

```http
POST /api/v1/auth/refresh
```

```json
{ "refreshToken": "..." }
```

Refresh token ротируется: использованный токен отзывается, а клиент получает новую пару access/refresh. В БД хранится hash refresh token, а не его исходный текст.

## Revoke

`POST /auth/revoke` требует access token и принимает refresh token в body. Успех — `204 No Content`.

## Change password

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

Успех — `204 No Content`. Frontend намеренно не делает автоматический refresh/retry на `401` этого запроса, потому что `401` может означать неверный текущий пароль.

## Current user

Канонический endpoint текущего auth store:

```http
GET /api/v1/auth/me
```

Существует также `GET /api/v1/users/me`, возвращающий близкую модель текущего пользователя. Это контрактное дублирование отмечено в ограничениях.
