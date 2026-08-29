# ProfileView.vue

Миграция старого `profile.html` на Vue 3.

## Сохранённая структура

Старый профиль имел три вкладки:

```text
Пользователь
Студент
Преподаватель
```

Новая Vue-страница сохраняет эту модель.

### Пользователь

Показывает:

```text
Имя
Фамилия
Отчество
Логин
Email
Телефон
Роли
Person ID
```

### Студент

Показывает:

```text
Группа
Факультет
GroupMembership
Предметы
```

Вкладка появляется только при роли `STUDENT`.

### Преподаватель

Показывает:

```text
Предметы
Группы из учебной нагрузки
```

Вкладка появляется для:

```text
TEACHER
ADMIN
```

## Используемые API

Страница не обращается к Axios напрямую.

Она использует существующие модули:

```js
membershipsApi
teachingApi
groupsApi
facultiesApi
subjectsApi
```

Все они работают через единый:

```text
/api/v1/...
```

## AuthStore

Основные данные пользователя берутся из:

```js
useAuthStore()
```

При открытии страницы выполняется:

```js
await authStore.loadCurrentUser()
```

Поэтому профиль синхронизируется с backend.

## Поддержка DTO

Учтены оба варианта данных пользователя:

```js
user.firstName
```

и старый:

```js
user.person.firstName
```

А Person ID может быть:

```js
user.personId
```

или:

```js
user.person.id
```

## Старый DOM-код удалён

Больше нет:

```text
getElementById
querySelectorAll
addEventListener
innerHTML
hidden
classList
```

Вкладки и списки работают через обычное Vue-состояние.

## Mobile

- header профиля становится вертикальным;
- сетки данных переходят в одну колонку;
- списки корректно помещаются на узких экранах.
