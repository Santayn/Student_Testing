# Хранилище и Docker volumes

## Persistent volumes

Compose создаёт два named volume:

```text
postgres-data
lecture-uploads
```

## PostgreSQL

```text
postgres-data
→ /var/lib/postgresql/data
```

Там находится физическое состояние PostgreSQL cluster.

## Материалы лекций

```text
lecture-uploads
→ /app/uploads/lecture-materials
```

В БД хранятся metadata файлов, а бинарное содержимое находится в этом volume.

## Обычная остановка

```bash
docker compose down
```

удаляет containers/network, но named volumes сохраняются. Следующий `docker compose up` использует прежние данные.

## Полное удаление данных

```bash
docker compose down -v
```

удаляет named volumes вместе с PostgreSQL и файлами лекций.

> Это разрушительная операция. Используйте её только если действительно требуется полностью чистая локальная среда.

## Пересборка образов

```bash
docker compose up -d --build
```

не удаляет persistent volumes. Обновление image само по себе не должно уничтожать пользовательские данные.

## Production

В production storage policy должна включать:

- регулярный backup PostgreSQL;
- отдельный backup lecture files;
- мониторинг свободного места;
- права файловой системы для backend service user;
- проверку восстановления обоих наборов данных.
