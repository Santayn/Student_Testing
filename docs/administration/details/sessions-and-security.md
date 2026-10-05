# Сессии и эксплуатационная безопасность

## Access и refresh tokens

Система использует:

```text
access JWT
+
refresh token с hash в БД
```

Refresh token можно отозвать, а при смене пароля активные refresh tokens пользователя отзываются.

## Деактивация пользователя

После `active=false`:

- новый login запрещён;
- refresh запрещён;
- но уже выданный access JWT может оставаться рабочим до собственного expiration.

Причина: authentication filter загружает текущие authorities из БД, однако `JwtService.isTokenValid()` не проверяет `UserDetails.isEnabled()` перед созданием Authentication.

Обычные access token lifetimes:

```text
15 минут
или
120 минут для длинной сессии
```

Поэтому деактивацию нельзя считать мгновенным принудительным завершением сессии.

## Смена ролей и permissions

Authorities при запросе загружаются заново из БД. Поэтому изменение роли/permission применяется к последующим запросам даже при старом JWT быстрее, чем полная token rotation.

## Смена пароля

Смена пароля отзывает refresh tokens, но не имеет server-side blacklist уже выданных access tokens.

## Восстановление старой БД

`RefreshTokens` хранятся в PostgreSQL. Restore старого snapshot может вернуть более старое состояние refresh sessions. После серьёзного recovery рекомендуется считать все прежние сессии потенциально недостоверными.

## Принудительный global logout

Ротация `APP_JWT_SECRET` инвалидирует существующие access JWT, но сама по себе не очищает refresh tokens в БД.

Для полноценного global logout нужен отдельный механизм массовой ревокации/очистки refresh tokens, которого сейчас нет в административном API.

## Операционная рекомендация

При компрометации security-секретов или после сомнительного restore:

1. ротировать `APP_JWT_SECRET`;
2. отозвать/очистить активные refresh sessions безопасным способом;
3. перезапустить backend;
4. проверить login/refresh;
5. задокументировать инцидент.
