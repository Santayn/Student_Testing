# Regression checklist

Checklist предназначен для ручной/полуавтоматической финальной проверки версии после прохождения unit/integration quality gates.

## Базовые автоматические проверки

- [ ] Frontend dependencies установлены через `npm ci`.
- [ ] `npm run quality` завершился успешно.
- [ ] Backend `mvn test` / `mvnw.cmd test` завершился успешно.
- [ ] Docker production/local images собираются.
- [ ] Backend readiness возвращает `200` при доступной БД.

## Authentication

- [ ] Вход корректными credentials работает.
- [ ] Неверный пароль отклоняется.
- [ ] Refresh восстанавливает истёкший access token.
- [ ] Logout/revoke завершает сессию.
- [ ] Смена пароля требует повторной авторизации согласно contract.
- [ ] Смена аккаунта не показывает данные предыдущего пользователя.

## STUDENT

- [ ] Видны только назначенные предметы.
- [ ] Открывается лекция и материалы.
- [ ] Metadata теста не расходует попытку.
- [ ] Новая попытка стартует через assignment.
- [ ] Незавершённая попытка продолжается, а не дублируется.
- [ ] Черновик ответов восстанавливается после возврата.
- [ ] Все четыре типа вопросов можно заполнить.
- [ ] Submit с пропусками требует подтверждения.
- [ ] После submit появляется результат.
- [ ] В Student UI/API не раскрываются правильные ответы сверх разрешённого контракта.
- [ ] Student видит только собственные результаты.

## TEACHER

- [ ] Видны только доступные преподавателю предметы.
- [ ] CRUD тем работает в доступном subject context.
- [ ] Создание/редактирование всех типов вопросов работает.
- [ ] DOCX import обрабатывает валидный template.
- [ ] Создание теста проходит все шаги wizard.
- [ ] Недостаточный банк вопросов блокирует некорректную конфигурацию.
- [ ] Назначения теста создаются для выбранных групп.
- [ ] Лекция создаётся/изменяется и связывается с тестами.
- [ ] Материалы загружаются и скачиваются.
- [ ] Course template/version workflow работает.
- [ ] Teacher results cascade возвращает только допустимые данные.

## ADMIN

- [ ] Пользователи фильтруются и открываются.
- [ ] User ↔ Person binding работает.
- [ ] Несколько ролей назначаются корректно.
- [ ] Faculties/groups/subjects CRUD работает.
- [ ] Group membership add/suspend/restore работает.
- [ ] Faculty ↔ Subject link/unlink работает.
- [ ] Teacher ↔ Subject assignment работает.
- [ ] Teaching assignment создаётся и редактируется.
- [ ] Load types управляются корректно.
- [ ] Database backup скачивается.
- [ ] Restore проверяется только на disposable/test environment.

## Security

- [ ] STUDENT получает `403` на management/raw attempt/admin operations.
- [ ] TEACHER не получает доступ к объекту другого преподавателя без соответствующих прав.
- [ ] ADMIN-only backup/restore закрыт для остальных ролей.
- [ ] Object-level ownership проверяется backend, а не только frontend.

## UI / accessibility

- [ ] Light theme читаема.
- [ ] Dark theme читаема.
- [ ] Sidebar mobile drawer открывается/закрывается и восстанавливает focus.
- [ ] Основные controls доступны с клавиатуры.
- [ ] На мобильной ширине нет критического horizontal overflow.
- [ ] Touch targets основных controls остаются удобными.

## Deployment/runtime

- [ ] `docker compose up -d --build` стартует чистое local environment.
- [ ] Persistent volumes не теряются после обычного restart/down.
- [ ] `down -v` используется только при намеренном reset.
- [ ] Nginx `/health` доступен.
- [ ] Backend `/api/v1/status/readiness` доступен.
- [ ] Lecture files сохраняются после restart container.
