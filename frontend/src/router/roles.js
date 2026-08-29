/*
 * Реальные роли backend:
 *
 * USER    — базовый авторизованный пользователь
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
 * Страницы учебного контента:
 *
 * STUDENT:
 *   lectures.read
 *   tests.take
 *
 * TEACHER:
 *   lectures.read
 *   + teacher management permissions
 *
 * ADMIN:
 *   полный набор permissions
 *
 * USER не имеет учебных permissions.
 */
export const LEARNING_ROLES = Object.freeze([
  'STUDENT',
  'TEACHER',
  'ADMIN',
])

/*
 * Непосредственное прохождение теста.
 *
 * tests.take есть у:
 *   STUDENT
 *   ADMIN
 *
 * У TEACHER этого permission нет.
 */
export const TEST_TAKER_ROLES = Object.freeze([
  'STUDENT',
  'ADMIN',
])

export const TEACHER_ROLES = Object.freeze([
  'TEACHER',
  'ADMIN',
])

export const ADMIN_ROLES = Object.freeze([
  'ADMIN',
])
