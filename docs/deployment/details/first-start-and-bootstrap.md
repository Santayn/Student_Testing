# Первый запуск и bootstrap

## Что происходит в local profile

При стандартном Docker Compose включён `DataLoader`. Он создаёт недостающие системные и демонстрационные данные.

В одном механизме сейчас объединены две ответственности:

```text
System bootstrap
├── roles
├── permissions
└── teaching load types

Demo seed
├── demo users
├── people
├── academic data
└── учебный контент
```

## Демонстрационные аккаунты

Текущий DataLoader содержит известные demo credentials, включая:

```text
student / student1
teacher / teacher1
admin / admin123
```

и дополнительные demo users.

Они допустимы только для локальной/демонстрационной среды.

> Не запускайте production с `APP_DATA_LOADER_ENABLED=true`.

## Почему отключить DataLoader на пустой БД недостаточно

Правильная production-конфигурация должна стремиться к:

```text
APP_DATA_LOADER_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_SQL_INIT_MODE=never
```

Но в текущем проекте отсутствуют:

- versioned database migrations;
- отдельный system bootstrap ролей/permissions;
- команда создания первого администратора;
- production seed policy.

Поэтому чистая пустая production database не имеет законченного автоматического bootstrap path.

## Публичная регистрация не создаёт администратора

Регистрация создаёт обычную учётную запись без полного рабочего академического контекста. Она не является механизмом initial administrator provisioning.

## Рекомендуемое направление

Перед production deployment необходимо разделить:

```text
SystemBootstrap
→ обязательные системные роли/permissions/reference data

DemoDataLoader
→ только демонстрационные пользователи и данные
```

и добавить отдельный безопасный способ provisioning первого `ADMIN`.
