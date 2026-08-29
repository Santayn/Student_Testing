# Unified API v1

Все backend controller'ы унифицированы под:

```text
/api/v1/...
```

Поэтому API-слой упрощён.

## Удалено

```text
.env
.env.example
VITE_API_BASE_URL
VITE_API_ROOT_URL
rootHttp
```

## Единственный Axios instance

```js
const http = axios.create({
  baseURL: '/api/v1',
})
```

## Примеры

```js
subjectsApi.getAll()
```

отправит:

```text
GET /api/v1/subjects
```

```js
questionsApi.importFile(file)
```

отправит:

```text
POST /api/v1/questions/import
```

```js
lecturesApi.uploadMaterial(id, file)
```

отправит:

```text
POST /api/v1/lectures/{id}/materials
```

## Vite proxy

Vite проксирует весь `/api` в Spring Boot:

```text
localhost:5173/api/v1/...
        ↓
localhost:8080/api/v1/...
```

## JWT

Связь с AuthStore не меняется:

```js
setAccessTokenProvider(
  () => authStore.accessToken
)
```

## Что удалить из старой версии

Удалите `.env` / `.env.example`, если они использовались только для API.

Также удалите любые импорты:

```js
rootHttp
```

Файлы из этого архива уже используют только единый `http`.
