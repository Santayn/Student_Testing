// Server contracts: ApiExceptionHandler + ErrorResponse (Spring Boot).
// HTTP 422/429 are handled by status, but are not error codes of this backend.
export const API_ERROR_CODES = Object.freeze({
  VALIDATION_FAILED: 'validation_failed',
  BAD_REQUEST: 'bad_request',
  UNAUTHORIZED: 'unauthorized',
  FORBIDDEN: 'forbidden',
  NOT_FOUND: 'not_found',
  CONFLICT: 'conflict',
  PAYLOAD_TOO_LARGE: 'payload_too_large',
  INTERNAL_ERROR: 'internal_error',
  AI_UNAVAILABLE: 'ai_unavailable',
  DATABASE_BACKUP_FAILED: 'database_backup_failed',
})

export function apiErrorCodeMessage(code, messages = {}) {
  if (!code) {
    return ''
  }

  const message = messages?.[code]

  return typeof message === 'string' ? message : ''
}
