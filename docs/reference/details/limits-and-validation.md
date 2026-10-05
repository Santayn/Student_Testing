# Основные лимиты и валидация

Ниже — быстрый справочник наиболее употребимых ограничений. Конкретный endpoint остаётся источником истины.

## Строки

| Поле/группа | Максимум |
|---|---:|
| login | 100 |
| password | 200 |
| Person first/last name | 100 |
| email | 255 |
| phone | 50 |
| faculty/group name | 200 |
| faculty/group code | 50 |
| subject name | 200 |
| role/permission name | 100 |
| role/permission description | 500 |
| topic name | 200 |
| topic description | 2000 |
| lecture title | 200 |
| lecture description | 2000 |
| `contentFolderKey` | 255 |
| course/version title | 200 |
| version description/change notes | 2000 |
| test title | 200 |
| test description | 4000 |
| question/option/matching text | 2000 |
| correct textual answer | 4000 |
| большинство notes | 1000 |

## Пароль

API validation использует ориентировочный диапазон:

```text
6..200 символов
```

## Учебный период

- `semester`: `1` или `2`;
- `studyCourse`: backend требует `>= 1`; admin frontend предлагает `1..6`;
- `academicYear`: `>= 2000`.

## Прогресс

```text
minProgressPercent: 0..100
progressPercent:    0..100
```

## Hours per week

Backend требует неотрицательное значение. Колонка рассчитана на decimal precision/scale, а frontend дополнительно ограничивает ввод своим UI-диапазоном.

## Multipart

Значения backend по умолчанию:

```text
max-file-size:    50MB
max-request-size: 200MB
```

При превышении сервер возвращает `413 payload_too_large`.
