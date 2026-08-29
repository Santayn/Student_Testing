# Auth group

Готовая группа страниц авторизации.

## Структура

```text
src/
├── layouts/
│   └── AuthLayout.vue
│
├── views/
│   └── auth/
│       ├── LoginView.vue
│       ├── RegisterView.vue
│       └── RequireAuthView.vue
│
└── router/
    └── routes/
        └── public.js
```

## Маршруты

Основные URL:

```text
/auth/login
/auth/register
/auth/required
```

Старые URL сохраняются через redirect:

```text
/login
/register
/auth-required
```

поэтому существующий код Router Guard менять не нужно.

## Login

Использует:

```js
authStore.login(login, password)
```

После авторизации:

```text
/auth/login?redirect=/subjects
```

пользователь возвращается на:

```text
/subjects
```

## Register

Использует:

```js
authStore.register(data)
```

После успешной регистрации выполняется переход на login.

DTO сейчас формируется так:

```js
{
  login,
  email,
  firstName,
  lastName,
  middleName,
  password
}
```

Если backend DTO немного отличается,
достаточно изменить объект в `RegisterView.vue`.

## RequireAuth

Использует query:

```text
/auth/required?redirect=/admin/users
```

и передаёт этот redirect в login.

## AuthLayout

Все auth-страницы используют один компактный layout:

```text
Student Testing
---------------------
RouterView auth page
```

Поэтому Login/Register/RequireAuth имеют единый размер и стиль.

## Theme

Используются CSS variables:

```text
--surface
--surface-secondary
--text
--text-secondary
--border
--brand
--danger
--warning
--shadow
```

Страницы автоматически работают с текущим ThemeStore.

## Mobile

На телефоне формы перестраиваются в одну колонку.
