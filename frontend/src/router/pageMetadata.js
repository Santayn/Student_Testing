const APP_NAME = 'Student Testing'

const PAGE_TITLES = Object.freeze({
  home: 'Главная',
  profile: 'Профиль',
  subjects: 'Предметы',
  'subject-details': 'Предмет',
  'subject-lectures': 'Лекции предмета',
  'lecture-details': 'Лекция',
  test: 'Прохождение теста',
  results: 'Результаты',

  'teacher-questions': 'Вопросы',
  'teacher-test-create': 'Создание теста',
  'teacher-lectures': 'Лекции',
  'teacher-topics': 'Темы предмета',
  'teacher-courses': 'Шаблоны курса',
  'teacher-workload': 'Моя нагрузка',

  'admin-users': 'Пользователи',
  'admin-roles': 'Роли и права',
  'admin-faculties': 'Факультеты',
  'admin-groups': 'Группы',
  'admin-subjects': 'Справочник предметов',
  'admin-faculty-subjects': 'Предметы факультетов',
  'admin-teacher-subjects': 'Преподаватели и предметы',
  'admin-teaching': 'Учебная нагрузка',

  about: 'О приложении',
  login: 'Вход',
  register: 'Регистрация',
  'account-pending': 'Доступ к аккаунту',
  'auth-required': 'Требуется вход',
  forbidden: 'Доступ запрещён',
  'not-found': 'Страница не найдена',

  'overlay-components-preview': 'Overlay Components Preview',
  'ui-components-preview': 'UI Components Preview',
  'primevue-preview': 'PrimeVue Preview',
  'ui-showcase': 'UI Showcase',
})

export function getDocumentTitle(routeName) {
  const pageTitle = PAGE_TITLES[String(routeName ?? '')]

  return pageTitle
    ? `${pageTitle} — ${APP_NAME}`
    : APP_NAME
}

export { APP_NAME }
