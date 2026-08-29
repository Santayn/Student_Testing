# Vue Admin Group

Миграция admin-группы старого frontend на Vue 3.

## Готовые страницы

```text
src/views/admin/
├── UsersView.vue
├── FacultiesView.vue
├── GroupsView.vue
├── SubjectsAdminView.vue
├── FacultySubjectsView.vue
├── TeacherSubjectsView.vue
└── TeachingTemplatesView.vue
```

Они соответствуют уже созданным маршрутам:

```text
/admin/users
/admin/faculties
/admin/groups
/admin/subjects
/admin/faculty-subjects
/admin/teacher-subjects
/admin/teaching
```

## Общие компоненты

Добавлены:

```text
src/components/admin/
├── AdminPageShell.vue
└── AdminNotice.vue
```

`AdminPageShell` содержит единые structural styles всей admin-группы:

- header;
- cards;
- forms;
- buttons;
- tables;
- checklists;
- stats;
- mobile layout.

## ThemeStore / theme.css

В admin-компонентах нет собственного ThemeStore.

Они получают цвета только из уже созданного:

```text
src/assets/theme.css
```

Используются токены:

```text
--bg
--surface
--surface-secondary
--text
--text-secondary
--text-on-brand
--border
--brand
--brand-hover
--brand-soft
--focus-ring
--success
--success-soft
--warning
--warning-soft
--warning-border
--danger
--danger-soft
--danger-border
```

То есть переключение:

```text
light ↔ dark
```

происходит автоматически.

## Adaptive layout

На desktop таблицы остаются обычными.

При ширине <= 640px:

```text
table
  ↓
card list
```

Каждая строка превращается в карточку:

```text
Пользователь   admin
ФИО            Иванов Иван
Email          ...
Роль           [ADMIN]
```

Формы на tablet/mobile перестраиваются в одну колонку.

## UsersView

Перенесена логика старого:

```text
users.html
```

Функции:

- загрузка ролей;
- загрузка пользователей;
- фильтр по роли;
- изменение роли.

Backend:

```text
GET /api/v1/roles
GET /api/v1/users
PUT /api/v1/users/{id}/roles
```

Тело смены роли:

```js
{
  roleIds: [roleId]
}
```

## FacultiesView

Перенесён:

```text
create-faculty.html
```

CRUD факультетов.

## GroupsView

Перенесён:

```text
groups.html
```

CRUD групп + выбор факультета.

## SubjectsAdminView

Перенесён:

```text
create-subject.html
```

CRUD предметов.

## FacultySubjectsView

Перенесён:

```text
manage-subject-faculty.html
```

Можно массово:

- добавлять предметы факультету;
- удалять предметы факультета.

## TeacherSubjectsView

Перенесён:

```text
manage-teachers-subject.html
```

Можно:

- выбрать преподавателя;
- назначить несколько предметов;
- оставить примечание;
- снять предметы.

Использованы старые backend-константы:

```js
TEACHER_ROLE = 1
REMOVED_STATUS = 3
```

## TeachingTemplatesView

Перенесена основная логика:

```text
manage-teacher-groups.html
```

Поддерживается:

- курс;
- семестр;
- учебный год;
- факультет;
- статус;
- примечание;
- несколько строк предмет → преподаватель;
- отдельный набор групп для каждой строки;
- создание `TeachingAssignment`;
- предотвращение дублей на клиенте;
- автоматическое обеспечение load type `Основная нагрузка`;
- просмотр назначений периода;
- смена преподавателя;
- смена статуса;
- редактирование примечания.

Статусы сохранены:

```text
1 Активно
2 Черновик
3 Закрыто
4 В паузе
```

## API изменения

В архиве обновлены:

```text
src/api/users.api.js
src/api/teaching.api.js
```

В `usersApi.updateRoles` теперь явно передаётся объект:

```js
usersApi.updateRoles(id, {
  roleIds: [roleId],
})
```

В `teachingApi` добавлен:

```js
getSubjectLoadTypes(params)
```

для:

```text
GET /api/v1/teaching/subject-load-types
```

## Router

Если ваш текущий `admin.js` уже содержит созданные ранее маршруты,
его менять не требуется.

Ожидаемые имена компонентов полностью совпадают с ним.

## Что заменить

Скопируйте из архива:

```text
src/components/admin/
src/views/admin/
src/utils/apiData.js
src/api/users.api.js
src/api/teaching.api.js
```

`theme.css`, `AuthStore`, Router и Sidebar менять не нужно.
