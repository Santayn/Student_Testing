# Production database migrations

Production runs with Hibernate schema validation and SQL initialization disabled. Therefore schema changes must be applied explicitly before deploying a backend version that depends on them.

## Required order for the current backend

Apply these scripts exactly once, in this order:

1. `2026-09-30-user-security-version.sql`
2. `2026-09-30-lecture-content-versioning.sql`
3. `2026-10-01-async-text-answer-grading.sql`

The scripts use idempotent PostgreSQL operations (`IF NOT EXISTS` where applicable), so re-running them is safe for the changes they own. Do not substitute the development `schema.sql` for production migrations.

## Example with psql

From the repository root:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f backend/docs/sql/2026-09-30-user-security-version.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f backend/docs/sql/2026-09-30-lecture-content-versioning.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f backend/docs/sql/2026-10-01-async-text-answer-grading.sql
```

For installations that use separate PostgreSQL connection parameters instead of `DATABASE_URL`, pass the normal `psql` `-h`, `-p`, `-U`, and `-d` options.

## Deployment rule

1. Back up the production database.
2. Stop or drain backend instances that could write incompatible data while the migration is being applied.
3. Apply all pending scripts with `ON_ERROR_STOP=1`.
4. Start the new backend with the production profile.
5. Confirm that Hibernate schema validation succeeds before enabling traffic.

Local/demo startup may continue to use `schema.sql`; production must use the migration scripts above.
