# Contribution workflow

В репозитории пока нет формального `CONTRIBUTING.md`, PR templates или CI. Этот документ задаёт рекомендуемый процесс.

## Перед началом

1. синхронизировать рабочую ветку;
2. проверить базовые gates, если среда позволяет;
3. прочитать соответствующие technical/API docs;
4. определить scope задачи.

## Во время работы

- держать diff тематически цельным;
- не смешивать unrelated refactor с bugfix;
- сохранять architecture boundaries;
- добавлять regression tests;
- не commit'ить секреты и локальные `.env`.

## Перед review

Выполнить frontend quality/backend tests по затронутым слоям, security/API review, Docker check для инфраструктуры и docs update.

## Описание изменения

Полезный формат:

```text
Что изменено?
Почему?
Какие риски?
Чем проверено?
Какие docs обновлены?
```

Breaking API/database/config changes должны быть отмечены явно.

Пока CI отсутствует, responsibility за gates остаётся на разработчике.
