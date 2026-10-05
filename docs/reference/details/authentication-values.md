# Значения authentication

## lifetimeKind

| `lifetimeKind` | Access token | Refresh token |
|---:|---:|---:|
| `1` | 15 минут | 7 дней |
| `2` | 120 минут | 30 дней |

`null`, `0` и неподдерживаемые значения нормализуются backend в режим `1`.

## Token type

Ответы login/register/refresh используют:

```text
tokenType = Bearer
```

API-запросы с access token передают:

```http
Authorization: Bearer <accessToken>
```

## Refresh token

Refresh token хранится у клиента и передаётся в auth API; в базе backend сохраняется его SHA-256 hash. При refresh используется ротация: использованный refresh token отзывается и заменяется новым.

Подробнее: [Technical: authentication](../../technical/details/authentication.md).
