# API/system smoke

`backend/scripts/full-system-smoke.ps1` — системный PowerShell smoke-тест, который выполняет реальные HTTP-запросы к уже запущенному backend.

## Что он проверяет

Скрипт выполняет крупные этапы:

- OpenAPI discovery;
- unauthenticated/auth scenarios;
- login/current user;
- roles/users lifecycle;
- academic CRUD;
- memberships;
- teaching;
- tests/questions/raw attempts;
- Student Learning;
- teacher results.

Базовый URL по умолчанию:

```text
http://localhost:8080
```

## Это не browser E2E

Smoke path:

```text
PowerShell HTTP client
      ↓
Spring Boot
      ↓
database
```

Он не тестирует Vue, browser, Nginx, client-side routing или frontend API adapters.

## Зависимость от demo data

Сценарий логинится заранее известными demo accounts и поэтому предполагает DataLoader/demo environment. На production-like environment с отключённым demo seed тест не является самодостаточным.

## Cleanup

Smoke создаёт сущности с уникальными suffix, но не удаляет всю созданную цепочку. Повторные запуски постепенно загрязняют используемую БД.

## Синхронизация с текущим API

Текущий REST index содержит **159** method/path operations. Статический аудит smoke source обнаружил прямые вызовы **134** уникальных method/path operations и **25** опубликованных операций, не представленных прямым `Invoke-Api` вызовом.

| Метод | Endpoint, отсутствующий в текущем smoke |
|---|---|
| `DELETE` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` |
| `DELETE` | `/api/v1/lectures/{lectureId}/materials/{materialId}` |
| `DELETE` | `/api/v1/tests/{id}` |
| `DELETE` | `/api/v1/topics/{id}` |
| `GET` | `/api/v1/faculties/{id}/subjects` |
| `GET` | `/api/v1/lectures/{id}/tests` |
| `GET` | `/api/v1/lectures/{lectureId}/materials` |
| `GET` | `/api/v1/lectures/{lectureId}/materials/{materialId}/download` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials/{materialId}/download` |
| `GET` | `/api/v1/results/student/data` |
| `GET` | `/api/v1/results/student/subjects` |
| `GET` | `/api/v1/subjects/{id}/faculties` |
| `GET` | `/api/v1/topics` |
| `GET` | `/api/v1/topics/{id}` |
| `POST` | `/api/v1/admin/database-backups` |
| `POST` | `/api/v1/admin/database-backups/restore` |
| `POST` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` |
| `POST` | `/api/v1/lectures/{lectureId}/materials` |
| `POST` | `/api/v1/questions/import` |
| `POST` | `/api/v1/topics` |
| `PUT` | `/api/v1/lectures/{id}/linked-test` |
| `PUT` | `/api/v1/lectures/{id}/tests` |
| `PUT` | `/api/v1/topics/{id}` |
| `PUT` | `/api/v1/users/{id}/person` |

При этом сам smoke в финале требует `missingOperations == 0`. Следовательно, перед использованием как обязательного release gate скрипт необходимо синхронизировать с актуальным OpenAPI/API index.

## Рекомендуемое развитие

1. запускать smoke только на disposable environment;
2. создавать test users/fixtures самим smoke bootstrap;
3. удалять environment целиком после теста вместо частичного cleanup;
4. синхронизировать operations с актуальным OpenAPI;
5. разделить обязательные critical-path checks и exhaustive API coverage, если полный охват становится слишком дорогим.
