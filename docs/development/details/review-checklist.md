# Review checklist

## Архитектура

- [ ] Изменение находится в правильном слое.
- [ ] Business logic не переехала в Controller/View.
- [ ] Frontend HTTP идёт через `src/api`.
- [ ] Business UI использует `components/ui`.
- [ ] Новый крупный файл не скрывает отдельную ответственность.

## Backend

- [ ] Transaction boundary корректна.
- [ ] DTO не раскрывает лишнее.
- [ ] Exception/status семантичны.
- [ ] OpenAPI обновлён.

## Security

- [ ] Request-level access задан.
- [ ] Object ownership проверен.
- [ ] STUDENT не получил management route.
- [ ] Чужие results/attempts недоступны.
- [ ] Correct answers не раскрыты.

## Database

- [ ] Entity и PostgreSQL schema согласованы.
- [ ] Проверены FK/index/unique/nullability.
- [ ] Учтены существующие данные.
- [ ] Изменение не полагается только на `ddl-auto=update`.

## Frontend

- [ ] Route/meta/navigation согласованы.
- [ ] Async flow защищён от stale response.
- [ ] Есть loading/error/empty states.
- [ ] Mobile/dark/accessibility не сломаны.
- [ ] Новый module достижим для static-quality.

## Tests

- [ ] Есть regression test.
- [ ] Frontend quality выполнен при frontend diff.
- [ ] Backend tests выполнены при backend diff.
- [ ] DB-specific изменение дополнительно проверено.

## Documentation

- [ ] API docs обновлены при contract change.
- [ ] User guide обновлён при UX change.
- [ ] Deployment docs обновлены при config/runtime change.
- [ ] Technical/development docs обновлены при architecture/process change.

## Handoff

- [ ] Нет debug logs/temporary flags/secrets.
- [ ] Нет случайных generated files.
- [ ] Описано, чем изменение проверено.
