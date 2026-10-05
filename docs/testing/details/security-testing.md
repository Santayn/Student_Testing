# Security testing

Security suite защищает как request-level authorization, так и отдельные object-level правила.

## `SecurityAccessRegressionTests`

Проверяются запреты для STUDENT на административные области, в том числе:

- questions management;
- topic CRUD;
- test management/raw attempt API;
- database backup/restore.

Также проверяется, что ADMIN имеет доступ к backup operations и что readiness endpoint остаётся доступным согласно runtime contract.

## `PublicLearningSecurityIntegrationTests`

Защищает student contract:

- получение metadata теста не должно создавать попытку;
- студент не может отправить чужую attempt;
- student result не должен раскрывать `correctAnswer`.

## `CurrentUserAccessServiceTests`

Targeted unit tests проверяют object-level ownership/access decisions для чужих test/group contexts.

## Важное ограничение MockMvc security tests

Часть tests использует Spring Security Test конструкции вида:

```java
.with(user("student").roles("STUDENT"))
```

Такой тест создаёт Authentication напрямую и не проходит весь production JWT path:

```text
Authorization: Bearer
      ↓
JWT parser
      ↓
UserDetails/authorities
      ↓
filter chain
```

Поэтому отдельный full JWT lifecycle integration test остаётся желательным.

## Что стоит добавить

Минимальный JWT/security integration набор:

1. login → access/refresh;
2. authenticated Bearer request;
3. expired/invalid access token;
4. refresh rotation;
5. revoked refresh token;
6. password change → session invalidation/refresh behavior;
7. direct permission + role permission combinations.
