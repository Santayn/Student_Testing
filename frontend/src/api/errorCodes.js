export const API_ERROR_CODES = Object.freeze({
  VALIDATION_ERROR:
    'VALIDATION_ERROR',

  ACCESS_DENIED:
    'ACCESS_DENIED',

  RESOURCE_NOT_FOUND:
    'RESOURCE_NOT_FOUND',

  RATE_LIMITED:
    'RATE_LIMITED',

  GROUP_HAS_DEPENDENCIES:
    'GROUP_HAS_DEPENDENCIES',

  FACULTY_HAS_DEPENDENCIES:
    'FACULTY_HAS_DEPENDENCIES',

  SUBJECT_HAS_DEPENDENCIES:
    'SUBJECT_HAS_DEPENDENCIES',

  TOPIC_HAS_DEPENDENCIES:
    'TOPIC_HAS_DEPENDENCIES',

  DATABASE_BACKUP_FAILED:
    'database_backup_failed',

  PAYLOAD_TOO_LARGE:
    'payload_too_large',
})

export function apiErrorCodeMessage(
  code,
  messages = {}
) {
  if (!code) {
    return ''
  }

  const message =
    messages?.[code]

  return typeof message === 'string'
    ? message
    : ''
}
