# Backend test profile

Spring integration tests используют профиль `test`, определённый в:

```text
backend/src/test/resources/application-test.yml
```

## Datasource

```text
jdbc:h2:mem:student_test
MODE=PostgreSQL
DATABASE_TO_LOWER=TRUE
DB_CLOSE_DELAY=-1
```

Дополнительно через H2 `INIT` создаётся compatibility domain:

```sql
CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR
```

## JPA

```text
ddl-auto=create-drop
open-in-view=false
```

Test schema строится Hibernate при запуске suite и удаляется после завершения context.

## SQL init

```text
spring.sql.init.mode=never
```

Это принципиально: production/local `schema.sql` **не выполняется backend JUnit suite**.

Следовательно, JUnit не способен обнаружить ошибки, существующие только в `schema.sql`. Именно поэтому отдельная проверка реального PostgreSQL/migrations остаётся необходимой.

## Остальные test settings

- DataLoader по умолчанию выключен;
- public registration выключена;
- используется отдельный test JWT secret;
- lecture files направляются в `target/test-lecture-materials`.

## H2 ≠ PostgreSQL

Режим совместимости полезен для быстрых integration tests, но не полностью воспроизводит:

- PostgreSQL extensions;
- partial indexes;
- точную lock semantics;
- DDL behavior;
- native SQL differences;
- `pg_dump`/`psql`;
- production schema initialization.
