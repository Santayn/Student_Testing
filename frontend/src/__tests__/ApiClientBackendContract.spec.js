import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const here = dirname(fileURLToPath(import.meta.url))

function source(relativePath) {
  return readFileSync(resolve(here, '..', relativePath), 'utf8')
}

describe('frontend API clients match backend contracts', () => {
  it('uses the supported subject load-type endpoints', () => {
    const teachingApi = source('api/teaching.api.js')

    expect(teachingApi).not.toContain('createSubjectLoadType(')
    expect(teachingApi).toContain('getSubjectLoadTypes(params = {})')
    expect(teachingApi).toContain(
      "return http.get('/teaching/subject-load-types', { params })"
    )
    expect(teachingApi).toContain(
      '`/teaching/subject-memberships/${subjectMembershipId}/load-types`'
    )
  })

  it('uploads lecture materials using the backend multipart field name', () => {
    const lecturesApi = source('api/lectures.api.js')

    expect(lecturesApi).not.toContain('uploadMaterial(')
    expect(lecturesApi).not.toContain("formData.append('file', file)")
    expect(lecturesApi).toContain('uploadMaterials(lectureId, files)')
    expect(lecturesApi).toContain("formData.append('files', file)")
  })
})
