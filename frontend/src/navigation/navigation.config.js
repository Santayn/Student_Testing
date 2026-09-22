export const NAV_KEYS = Object.freeze({
  HOME: 'home',
  PROFILE: 'profile',
  SUBJECTS: 'subjects',
  RESULTS: 'results',

  TEACHER_QUESTIONS: 'teacher-questions',
  TEACHER_TEST_CREATE: 'teacher-test-create',
  TEACHER_LECTURES: 'teacher-lectures',
  TEACHER_TOPICS: 'teacher-topics',
  TEACHER_COURSES: 'teacher-courses',
  TEACHER_WORKLOAD: 'teacher-workload',

  ADMIN_USERS: 'admin-users',
  ADMIN_ROLES: 'admin-roles',
  ADMIN_FACULTIES: 'admin-faculties',
  ADMIN_GROUPS: 'admin-groups',
  ADMIN_SUBJECTS: 'admin-subjects',
  ADMIN_FACULTY_SUBJECTS: 'admin-faculty-subjects',
  ADMIN_TEACHER_SUBJECTS: 'admin-teacher-subjects',
  ADMIN_TEACHING: 'admin-teaching',
})

export const NAVIGATION_DESTINATIONS = Object.freeze({
  [NAV_KEYS.HOME]: {
    key: NAV_KEYS.HOME,
    label: 'Главная',
    icon: 'pi pi-home',
    routeName: 'home',
  },

  [NAV_KEYS.PROFILE]: {
    key: NAV_KEYS.PROFILE,
    label: 'Профиль',
    icon: 'pi pi-user',
    routeName: 'profile',
  },

  [NAV_KEYS.SUBJECTS]: {
    key: NAV_KEYS.SUBJECTS,
    label: 'Предметы',
    icon: 'pi pi-book',
    routeName: 'subjects',
  },

  [NAV_KEYS.RESULTS]: {
    key: NAV_KEYS.RESULTS,
    label: 'Результаты',
    icon: 'pi pi-chart-bar',
    routeName: 'results',
  },

  [NAV_KEYS.TEACHER_QUESTIONS]: {
    key: NAV_KEYS.TEACHER_QUESTIONS,
    label: 'Вопросы',
    icon: 'pi pi-question-circle',
    routeName: 'teacher-questions',
  },

  [NAV_KEYS.TEACHER_TEST_CREATE]: {
    key: NAV_KEYS.TEACHER_TEST_CREATE,
    label: 'Создать тест',
    icon: 'pi pi-plus-circle',
    routeName: 'teacher-test-create',
  },

  [NAV_KEYS.TEACHER_LECTURES]: {
    key: NAV_KEYS.TEACHER_LECTURES,
    label: 'Лекции',
    icon: 'pi pi-file',
    routeName: 'teacher-lectures',
  },

  [NAV_KEYS.TEACHER_TOPICS]: {
    key: NAV_KEYS.TEACHER_TOPICS,
    label: 'Темы предмета',
    icon: 'pi pi-tags',
    routeName: 'teacher-topics',
  },

  [NAV_KEYS.TEACHER_COURSES]: {
    key: NAV_KEYS.TEACHER_COURSES,
    label: 'Шаблоны курса',
    icon: 'pi pi-clone',
    routeName: 'teacher-courses',
  },

  [NAV_KEYS.TEACHER_WORKLOAD]: {
    key: NAV_KEYS.TEACHER_WORKLOAD,
    label: 'Моя нагрузка',
    icon: 'pi pi-calendar',
    routeName: 'teacher-workload',
  },

  [NAV_KEYS.ADMIN_USERS]: {
    key: NAV_KEYS.ADMIN_USERS,
    label: 'Пользователи',
    icon: 'pi pi-users',
    routeName: 'admin-users',
  },

  [NAV_KEYS.ADMIN_ROLES]: {
    key: NAV_KEYS.ADMIN_ROLES,
    label: 'Роли и права',
    icon: 'pi pi-shield',
    routeName: 'admin-roles',
  },

  [NAV_KEYS.ADMIN_FACULTIES]: {
    key: NAV_KEYS.ADMIN_FACULTIES,
    label: 'Факультеты',
    icon: 'pi pi-building',
    routeName: 'admin-faculties',
  },

  [NAV_KEYS.ADMIN_GROUPS]: {
    key: NAV_KEYS.ADMIN_GROUPS,
    label: 'Группы',
    icon: 'pi pi-users',
    routeName: 'admin-groups',
  },

  [NAV_KEYS.ADMIN_SUBJECTS]: {
    key: NAV_KEYS.ADMIN_SUBJECTS,
    label: 'Справочник предметов',
    icon: 'pi pi-book',
    routeName: 'admin-subjects',
  },

  [NAV_KEYS.ADMIN_FACULTY_SUBJECTS]: {
    key: NAV_KEYS.ADMIN_FACULTY_SUBJECTS,
    label: 'Предметы факультетов',
    icon: 'pi pi-sitemap',
    routeName: 'admin-faculty-subjects',
  },

  [NAV_KEYS.ADMIN_TEACHER_SUBJECTS]: {
    key: NAV_KEYS.ADMIN_TEACHER_SUBJECTS,
    label: 'Преподаватели и предметы',
    icon: 'pi pi-id-card',
    routeName: 'admin-teacher-subjects',
  },

  [NAV_KEYS.ADMIN_TEACHING]: {
    key: NAV_KEYS.ADMIN_TEACHING,
    label: 'Учебная нагрузка',
    icon: 'pi pi-calendar',
    routeName: 'admin-teaching',
  },
})

/*
 * Sidebar описывается только через устойчивые destination keys.
 * Команды создания/изменения остаются контекстными действиями страниц.
 * label можно переопределить для конкретного workspace,
 * не меняя каноническое название destination/breadcrumb.
 */
export const WORKSPACE_NAVIGATION = Object.freeze({
  STUDENT: [
    {
      key: 'learning',
      label: 'Обучение',
      items: [
        NAV_KEYS.HOME,
        NAV_KEYS.SUBJECTS,
        NAV_KEYS.RESULTS,
      ],
    },
    {
      key: 'account',
      label: 'Аккаунт',
      items: [
        NAV_KEYS.PROFILE,
      ],
    },
  ],

  TEACHER: [
    {
      key: 'learning',
      label: 'Обучение',
      items: [
        NAV_KEYS.HOME,
        {
          key: NAV_KEYS.SUBJECTS,
          label: 'Мои предметы',
        },
        NAV_KEYS.RESULTS,
      ],
    },
    {
      key: 'content',
      label: 'Учебный контент',
      items: [
        NAV_KEYS.TEACHER_TOPICS,
        NAV_KEYS.TEACHER_QUESTIONS,
        NAV_KEYS.TEACHER_LECTURES,
        NAV_KEYS.TEACHER_COURSES,
      ],
    },
    {
      key: 'workload',
      label: 'Нагрузка',
      items: [
        NAV_KEYS.TEACHER_WORKLOAD,
      ],
    },
    {
      key: 'account',
      label: 'Аккаунт',
      items: [
        NAV_KEYS.PROFILE,
      ],
    },
  ],

  ADMIN: [
    {
      key: 'overview',
      label: 'Обзор',
      items: [
        NAV_KEYS.HOME,
        NAV_KEYS.RESULTS,
      ],
    },
    {
      key: 'academic',
      label: 'Академическая структура',
      items: [
        NAV_KEYS.ADMIN_FACULTIES,
        NAV_KEYS.ADMIN_GROUPS,
        NAV_KEYS.ADMIN_SUBJECTS,
      ],
    },
    {
      key: 'assignments',
      label: 'Назначения и нагрузка',
      items: [
        NAV_KEYS.ADMIN_FACULTY_SUBJECTS,
        NAV_KEYS.ADMIN_TEACHER_SUBJECTS,
        NAV_KEYS.ADMIN_TEACHING,
      ],
    },
    {
      key: 'content',
      label: 'Учебный контент',
      items: [
        {
          key: NAV_KEYS.SUBJECTS,
          label: 'Доступные предметы',
        },
        NAV_KEYS.TEACHER_TOPICS,
        NAV_KEYS.TEACHER_QUESTIONS,
        NAV_KEYS.TEACHER_LECTURES,
        NAV_KEYS.TEACHER_COURSES,
      ],
    },
    {
      key: 'access',
      label: 'Управление доступом',
      items: [
        NAV_KEYS.ADMIN_USERS,
        NAV_KEYS.ADMIN_ROLES,
      ],
    },
    {
      key: 'account',
      label: 'Аккаунт',
      items: [
        NAV_KEYS.PROFILE,
      ],
    },
  ],
})

/*
 * Entity descriptors нужны для deep breadcrumbs.
 * contextLabel/contextId позволяют странице подставить уже загруженное
 * человекочитаемое имя, но resolver умеет работать и без него.
 */
export const BREADCRUMB_ENTITIES = Object.freeze({
  subject: {
    key: 'subject',
    contextLabel: 'subjectName',
    contextId: 'subjectId',
    routeParam: 'subjectId',
    routeQuery: 'subjectId',
    fallbackLabel: 'Предмет',
    routeName: 'subject-details',
    routeParamName: 'subjectId',
  },

  lecture: {
    key: 'lecture',
    contextLabel: 'lectureTitle',
    contextId: 'lectureId',
    routeParam: 'lectureId',
    routeQuery: 'lectureId',
    fallbackLabel: 'Лекция',
    routeName: 'lecture-details',
    routeParamName: 'lectureId',
  },

  test: {
    key: 'test',
    contextLabel: 'testTitle',
    contextId: 'testId',
    routeParam: 'testId',
    routeQuery: 'testId',
    fallbackLabel: 'Тест',
    routeName: 'test',
    routeParamName: 'testId',
  },
})

/*
 * Breadcrumb config использует те же destination keys, что и Sidebar.
 * Динамический контекст можно передавать в resolveBreadcrumbs(route, context).
 */
export const BREADCRUMB_CONFIG = Object.freeze({
  home: [
    { destination: NAV_KEYS.HOME },
  ],

  profile: [
    { destination: NAV_KEYS.PROFILE },
  ],

  subjects: [
    { destination: NAV_KEYS.SUBJECTS },
  ],

  'subject-details': [
    {
      destination: NAV_KEYS.SUBJECTS,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'subject',
      preserveQuery: ['facultyId'],
    },
  ],

  'subject-lectures': [
    {
      destination: NAV_KEYS.SUBJECTS,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'subject',
      preserveQuery: ['facultyId'],
    },
    {
      key: 'lectures',
      label: 'Лекции',
      routeName: 'subject-lectures',
      paramsFrom: ['subjectId'],
      requires: ['subjectId'],
      preserveQuery: ['facultyId'],
    },
  ],

  'lecture-details': [
    {
      destination: NAV_KEYS.SUBJECTS,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'subject',
      optional: true,
      preserveQuery: ['facultyId'],
    },
    {
      key: 'lectures',
      label: 'Лекции',
      routeName: 'subject-lectures',
      paramsFrom: ['subjectId'],
      requires: ['subjectId'],
      optional: true,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'lecture',
    },
  ],

  test: [
    {
      destination: NAV_KEYS.SUBJECTS,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'subject',
      optional: true,
      preserveQuery: ['facultyId'],
    },
    {
      key: 'lectures',
      label: 'Лекции',
      routeName: 'subject-lectures',
      paramsFrom: ['subjectId'],
      requires: ['subjectId'],
      optional: true,
      preserveQuery: ['facultyId'],
    },
    {
      entity: 'lecture',
      optional: true,
    },
    {
      entity: 'test',
    },
  ],

  results: [
    { destination: NAV_KEYS.RESULTS },
  ],

  'teacher-questions': [
    { destination: NAV_KEYS.TEACHER_QUESTIONS },
  ],

  'teacher-test-create': [
    { destination: NAV_KEYS.TEACHER_QUESTIONS },
    { destination: NAV_KEYS.TEACHER_TEST_CREATE },
  ],

  'teacher-lectures': [
    { destination: NAV_KEYS.TEACHER_LECTURES },
  ],

  'teacher-topics': [
    { destination: NAV_KEYS.TEACHER_TOPICS },
  ],

  'teacher-courses': [
    { destination: NAV_KEYS.TEACHER_COURSES },
  ],

  'teacher-workload': [
    { destination: NAV_KEYS.TEACHER_WORKLOAD },
  ],

  'admin-users': [
    { destination: NAV_KEYS.ADMIN_USERS },
  ],

  'admin-roles': [
    { destination: NAV_KEYS.ADMIN_ROLES },
  ],

  'admin-faculties': [
    { destination: NAV_KEYS.ADMIN_FACULTIES },
  ],

  'admin-groups': [
    { destination: NAV_KEYS.ADMIN_GROUPS },
  ],

  'admin-subjects': [
    { destination: NAV_KEYS.ADMIN_SUBJECTS },
  ],

  'admin-faculty-subjects': [
    { destination: NAV_KEYS.ADMIN_FACULTY_SUBJECTS },
  ],

  'admin-teacher-subjects': [
    { destination: NAV_KEYS.ADMIN_TEACHER_SUBJECTS },
  ],

  'admin-teaching': [
    { destination: NAV_KEYS.ADMIN_TEACHING },
  ],
})
