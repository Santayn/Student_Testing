/*
 * Реальные роли backend:
 *
 * USER    — базовый авторизованный пользователь без доступа к рабочим разделам
 * STUDENT — студент
 * TEACHER — преподаватель
 * ADMIN   — администратор
 */
export const APP_ROLES = Object.freeze([
  'USER',
  'STUDENT',
  'TEACHER',
  'ADMIN',
])

/*
 * Роли, которые дают доступ к рабочей части приложения.
 * Одной роли недостаточно: frontend также требует привязанный personId.
 */
export const WORKSPACE_ROLES = Object.freeze([
  'STUDENT',
  'TEACHER',
  'ADMIN',
])

/*
 * Общие учебные страницы с ролевой логикой внутри view.
 *
 * /subjects и /results умеют работать отдельно для
 * STUDENT / TEACHER / ADMIN и не являются прямым
 * student-only public-learning flow.
 */
export const LEARNING_ROLES = Object.freeze([
  'STUDENT',
  'TEACHER',
  'ADMIN',
])

/*
 * Маршруты, построенные исключительно на
 * /api/v1/public/learning/**.
 *
 * Backend формирует для них studentLearningContext,
 * поэтому одного lectures.read/tests.take недостаточно:
 * пользователь должен иметь студенческий контекст.
 */
export const STUDENT_LEARNING_ROLES = Object.freeze([
  'STUDENT',
])

/*
 * Непосредственное прохождение теста также является
 * частью student-only /public/learning/** flow.
 */
export const TEST_TAKER_ROLES = Object.freeze([
  'STUDENT',
])

export const TEACHER_ROLES = Object.freeze([
  'TEACHER',
  'ADMIN',
])

export const ADMIN_ROLES = Object.freeze([
  'ADMIN',
])
