import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  buildCourseTemplateListParams,
  canCreateCourseTemplate,
} from '@/utils/courseTemplateContext'

import componentSource from '@/views/teacher/CourseTemplatesView.vue?raw'

describe('course template admin context', () => {
  it('filters admin templates by the teacher from the selected membership', () => {
    expect(
      buildCourseTemplateListParams({
        subjectId: 7,
        isAdmin: true,
        currentPersonId: 999,
        selectedMembership: {
          id: 31,
          subjectId: 7,
          personId: 42,
        },
      })
    ).toEqual({
      subjectId: 7,
      authorPersonId: 42,
    })
  })

  it('keeps a normal teacher scoped to the current person', () => {
    expect(
      buildCourseTemplateListParams({
        subjectId: '7',
        isAdmin: false,
        currentPersonId: '15',
        selectedMembership: {
          id: 22,
          subjectId: 7,
          personId: 15,
        },
      })
    ).toEqual({
      subjectId: 7,
      authorPersonId: 15,
    })
  })

  it('does not allow frontend creation of a teacher-owned template by admin', () => {
    expect(
      canCreateCourseTemplate({
        isAdmin: true,
      })
    ).toBe(false)

    expect(
      canCreateCourseTemplate({
        isAdmin: false,
      })
    ).toBe(true)
  })
  it('does not send client-controlled actor fields that backend ignores', () => {
    expect(componentSource)
      .toContain('buildCourseTemplateListParams')

    expect(componentSource)
      .not.toContain('createdByPersonId')

    expect(componentSource)
      .not.toContain('publishedByPersonId')

    expect(componentSource)
      .not.toContain('authorPersonId:')
  })

})
