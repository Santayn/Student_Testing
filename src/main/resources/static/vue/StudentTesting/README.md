# UserStore для Pinia

Архив предназначен для Vue 3 + JavaScript + Pinia.

## Файл

```text
src/stores/user.js
```

## Зависимости

Store использует уже созданный API-слой:

```text
src/api/index.js
src/api/users.api.js
src/api/error.js
```

Также должен быть подключён:

```bash
npm install pinia pinia-plugin-persistedstate
```

## Важно

В `main.js` Pinia должна быть настроена с persistence:

```js
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()

pinia.use(piniaPluginPersistedstate)

app.use(pinia)
```

## Использование

```js
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

await userStore.loadCurrentUser()

console.log(userStore.fullName)
console.log(userStore.roles)
console.log(userStore.isAdmin)
```

Проверка роли:

```js
if (userStore.hasRole('ADMIN')) {
  // ...
}
```

Несколько ролей:

```js
if (userStore.hasAnyRole('ADMIN', 'TEACHER')) {
  // ...
}
```

Очистка:

```js
userStore.clearUser()
```

JWT здесь намеренно не хранится. Позже токен и login/logout лучше вынести в отдельный AuthStore.
