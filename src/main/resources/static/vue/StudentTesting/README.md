# AppSidebar + mobile adaptation

Ролевой Sidebar для текущей архитектуры Vue.

## Файлы

```text
src/
├── components/
│   └── layout/
│       └── AppSidebar.vue
│
└── layouts/
    └── WorkspaceLayout.vue
```

## Роли

### TEACHER

Видит:

```text
Преподаватель
├── Вопросы
├── Создать тест
├── Лекции
├── Темы
├── Шаблоны курса
└── Нагрузка
```

### ADMIN

Видит преподавательский блок и:

```text
Администрирование
├── Пользователи
├── Факультеты
├── Группы
├── Предметы
├── Предметы факультета
├── Предметы преподавателей
└── Преподавательская нагрузка
```

Для обычного STUDENT Sidebar вообще не отображается.

## Desktop

При ширине больше 960px:

```text
┌──────────── Sidebar ────────────┐  ┌──────── Page ────────┐
│ Преподаватель                   │  │                      │
│ Вопросы                         │  │    RouterView        │
│ Создать тест                    │  │                      │
│ Лекции                          │  │                      │
└─────────────────────────────────┘  └──────────────────────┘
```

Sidebar использует:

```css
position: sticky;
```

и остаётся видимым при прокрутке.

## Mobile

При ширине <= 960px показывается компактная строка:

```text
☰ Разделы
```

По нажатию:

```text
┌──────────────────────────────┐
│ Разделы                   ×  │
├──────────────────────────────┤
│ ПРЕПОДАВАТЕЛЬ                │
│ Вопросы                      │
│ Создать тест                 │
│ Лекции                       │
│ ...                          │
└──────────────────────────────┘
```

Это drawer поверх страницы.

Добавлено:

- затемнение background;
- закрытие по клику на overlay;
- закрытие по `Escape`;
- закрытие после любого Router-перехода;
- блокировка прокрутки body при открытом drawer;
- active state через `router-link-active`.

## Использование

Для рабочих страниц можно использовать:

```vue
<WorkspaceLayout />
```

или просто:

```vue
<AppSidebar />
```

в собственном layout.

## Требуемые route names

Sidebar использует уже созданные маршруты:

```text
teacher-questions
teacher-test-create
teacher-lectures
teacher-topics
teacher-courses
teacher-workload

admin-users
admin-faculties
admin-groups
admin-subjects
admin-faculty-subjects
admin-teacher-subjects
admin-teaching
```

## Почему Sidebar скрыт для Student

Студенту достаточно Header:

```text
Главная
Профиль
Предметы
Результаты
```

Поэтому sidebar не занимает экран там, где он не нужен.
