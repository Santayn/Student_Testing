# Обновление документации при разработке

Документация входит в Definition of Done.

| Изменение | Раздел |
|---|---|
| новая возможность | `project-description/` |
| пользовательский сценарий | `user-guide/` |
| архитектура | `technical/` |
| env/Docker/runtime | `deployment/` |
| REST contract/error | `api/` |
| test layer/gate | `testing/` |
| developer workflow | `development/` |

Не дублировать большие фрагменты: разделы должны ссылаться друг на друга.

README каждого раздела остаётся кратким оглавлением, подробности — в `details/`.

Любой REST endpoint change должен обновлять API index.

В review задавать отдельный вопрос: **изменилось ли поведение, уже описанное в docs?**
