import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    new URL(
      relativePath,
      import.meta.url
    ),
    'utf8'
  )
}

describe('teacher workload membership context', () => {
  it('keeps duplicate subject memberships separate for admin workload', () => {
    const workload = source(
      '../views/teacher/TeacherWorkloadView.vue'
    )

    expect(workload)
      .toContain('availableMembershipIds')

    expect(workload)
      .toContain('row.subjectMembershipId')

    expect(workload)
      .toContain('activeAssignmentsForMembership')

    expect(workload)
      .toContain('membershipOptionsForRow')

    expect(workload)
      .toContain('membershipLabel(row.subjectMembershipId)')

    expect(workload)
      .toContain('subjectMembershipId:\n        membership.id')

    expect(workload)
      .not.toContain('membershipBySubjectId')
  })
})
