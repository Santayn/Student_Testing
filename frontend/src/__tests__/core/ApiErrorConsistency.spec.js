import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  apiErrorStatus,
  isApiConflict,
  isApiForbidden,
  isApiNotFound,
} from '@/api'

function responseError(status) {
  return {
    response: { status },
  }
}

describe('API error consistency helpers', () => {
  it('extracts only a numeric HTTP response status', () => {
    expect(apiErrorStatus(responseError(409))).toBe(409)
    expect(apiErrorStatus({ response: { status: '404' } })).toBe(404)
    expect(apiErrorStatus(new Error('boom'))).toBeNull()
  })

  it('classifies conflict, forbidden and not-found errors without parsing backend text', () => {
    expect(isApiConflict(responseError(409))).toBe(true)
    expect(isApiConflict(responseError(400))).toBe(false)
    expect(isApiForbidden(responseError(403))).toBe(true)
    expect(isApiForbidden(responseError(401))).toBe(false)
    expect(isApiNotFound(responseError(404))).toBe(true)
    expect(isApiNotFound(responseError(409))).toBe(false)
  })
})
