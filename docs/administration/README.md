# Эксплуатация и администрирование Student Testing

Раздел описывает эксплуатацию уже развёрнутой системы Student Testing: запуск и остановку сервисов, health/readiness, логи, административный доступ, сессии, резервное копирование, хранение файлов, обновление, rollback и восстановление после сбоев.

Это **не руководство по кнопкам административного интерфейса**. Пользовательские действия администратора описаны в [руководстве пользователя](../user-guide/details/admin/README.md). Здесь система рассматривается с точки зрения оператора, который отвечает за её доступность и сохранность данных.

## [Обзор эксплуатации](./details/administration-overview.md)

Границы administration-документации, текущая operational-модель и ответственность оператора.

## [Operational-модель](./details/operational-model.md)

Контейнеры, volumes, зависимости сервисов, persistence и жизненный цикл single-host Docker stack.

## Работа сервисов

- [Запуск и остановка](./details/startup-and-shutdown.md)
- [Health и readiness](./details/service-health.md)
- [Логи и диагностика](./details/logs-and-diagnostics.md)

## Доступ и безопасность

- [Администрирование учётных записей](./details/account-administration.md)
- [Доступ администратора и восстановление доступа](./details/administrator-access.md)
- [Сессии и эксплуатационная безопасность](./details/sessions-and-security.md)

## Резервное копирование

- [Резервная копия базы данных](./details/database-backup.md)
- [Восстановление базы данных](./details/database-restore.md)
- [Резервное копирование файлов лекций](./details/lecture-files-backup.md)
- [Полная резервная копия системы](./details/full-system-backup.md)

## Данные и обслуживание

- [Volumes и хранение данных](./details/storage-and-volumes.md)
- [Обслуживание базы данных](./details/database-maintenance.md)

## Обновление и аварийное восстановление

- [Обновление системы](./details/update-procedure.md)
- [Rollback](./details/rollback-procedure.md)
- [Disaster recovery](./details/disaster-recovery.md)
- [Диагностика инцидентов](./details/incident-diagnostics.md)

## [Operational checklist](./details/operational-checklist.md)

Краткие проверки перед обновлением, после запуска, после восстановления и при регулярной эксплуатации.

## [Ограничения эксплуатации](./details/administration-limitations.md)

Текущие ограничения: bootstrap аккаунтов, восстановление пароля, last-admin lockout, sessions, backup/restore, observability и отсутствие полноценного production operations layer.
