# Форматы даты и времени

## Instant

Используется для абсолютных UTC-событий:

```text
createdAtUtc
updatedAtUtc
availableFromUtc
availableUntilUtc
startedAtUtc
completedAtUtc
removedAtUtc
```

Wire-формат ISO-8601, например:

```text
2026-10-05T15:30:00Z
```

## LocalDate

Например `Person.dateOfBirth`:

```text
2026-10-05
```

Формат: `YYYY-MM-DD`.

## LocalTime

В частности `Test.duration` технически хранится как `LocalTime`, например:

```text
00:30:00
```

Это не `java.time.Duration`, поэтому при развитии контракта нельзя автоматически трактовать поле как произвольную длительность больше 24 часов.

## Frontend datetime-local

Формы дат доступности используют локальный `datetime-local`, после чего клиент преобразует значение для API. При сравнении дат всегда учитывайте различие локального времени браузера и UTC `Instant` backend.
