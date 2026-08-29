# Auth session + refresh token

Обновление frontend-авторизации под фактический backend контракт.

## Backend endpoints

```text
POST /api/v1/auth/login
POST /api/v1/auth/register
POST /api/v1/auth/refresh
POST /api/v1/auth/revoke
POST /api/v1/auth/change-password
GET  /api/v1/auth/me
```

## Что хранит AuthStore

Persisted:

```text
tokenType

accessToken
accessTokenExpiresAtUtc

refreshToken
refreshTokenExpiresAtUtc

lifetimeKind

user
```

Не persisted:

```text
loading
refreshing
initialized
error
```

## Login

```text
POST /auth/login
  ↓
access + refresh
  ↓
сохранить ОБА токена
  ↓
GET /auth/me
  ↓
user + roles + permissions
```

## Register

Контракт backend:

```json
{
  "login": "student1",
  "password": "password123",
  "personId": 15
}
```

`lifetimeKind` можно добавить опционально.

После регистрации backend сразу возвращает токены.

Поэтому RegisterView:

```text
register
  ↓
tokens
  ↓
/auth/me
  ↓
home
```

Переход на Login после успешной регистрации больше не нужен.

## Refresh

Access token проверяется перед защищённым запросом.

За 30 секунд до истечения он считается требующим обновления:

```text
TOKEN_EXPIRY_MARGIN_MS = 30000
```

Если token истёк/скоро истечёт:

```text
AuthStore.ensureAccessToken()
  ↓
AuthStore.refreshSession()
  ↓
POST /auth/refresh
```

Backend возвращает новую пару:

```text
NEW accessToken
NEW refreshToken
```

Store заменяет ОБА значения.

## Refresh lock

`refreshSession()` использует один module-level Promise:

```text
refreshPromise
```

Поэтому:

```text
Request A -> 401 ┐
Request B -> 401 ├─> ОДИН /auth/refresh
Request C -> 401 ┘
```

Все запросы затем используют новую пару.

## Axios

Есть два клиента.

### authHttp

Без auth interceptors:

```text
login
register
refresh
```

Это предотвращает рекурсивный refresh.

### http

Для остальных `/api/v1/**`:

```text
request
  ↓
ensureAccessToken()
  ↓
Authorization: Bearer ...

response 401
  ↓
refreshSession()
  ↓
retry ОДИН раз
```

## /auth/me

AuthStore больше не использует:

```text
/users/me
```

для восстановления авторизации.

Используется фактический endpoint:

```text
GET /api/v1/auth/me
```

Именно он возвращает:

```text
user
roles
permissions
person
```

## Logout

Теперь logout является async:

```js
await authStore.logout()
```

Алгоритм:

```text
access истёк?
  ↓ да
refresh, если возможно
  ↓
POST /auth/revoke
  body: current refreshToken
  ↓
clearSession()
```

Даже если revoke завершится сетевой ошибкой,
локальная сессия всё равно удаляется.

## Permissions

AuthStore получил:

```js
permissions
hasPermission(permission)
hasAnyPermission(...permissions)
```

Например:

```js
authStore.hasPermission(
  'teaching.manage'
)
```

## Change password

Добавлено:

```js
await authStore.changePassword(
  currentPassword,
  newPassword
)
```

которое вызывает:

```text
POST /api/v1/auth/change-password
```

## main.js

Старый:

```js
setAccessTokenProvider(...)
```

заменён на:

```js
configureHttpAuth({
  getAccessToken,
  ensureAccessToken,
  refreshSession,
  onSessionInvalid,
})
```

При окончательной потере сессии защищённая страница
перенаправляется на login с `redirect`.

## Что заменить

```text
src/stores/auth.js

src/api/http.js
src/api/auth.api.js
src/api/index.js

src/main.js

src/views/auth/LoginView.vue
src/views/auth/RegisterView.vue

src/components/layout/AppHeader.vue
```

Остальные API modules менять не требуется.


## Важный нюанс revoke

`POST /auth/revoke` выполняется с:

```js
skipAuthRefresh: true
```

Перед ним `AuthStore.logout()` сам обеспечивает действующий access token.

Это предотвращает сценарий:

```text
revoke(old refreshToken)
  ↓ 401
automatic refresh
  ↓
refresh token rotation
  ↓
retry revoke со СТАРЫМ body
```

То есть refresh rotation и revoke не конфликтуют.
