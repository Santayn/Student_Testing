// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const groupsView = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'admin',
    'GroupsView.vue'
  ),
  'utf8'
)

describe('admin group members drawer', () => {
  it('opens membership management from the group workspace without creating a new route', () => {
    expect(groupsView).toContain('label="Состав группы"')
    expect(groupsView).toContain('openGroupMembers(group)')
    expect(groupsView).toContain('<UiDrawer')
    expect(groupsView).toContain('membersDrawerVisible')
    expect(groupsView).toContain('Текущие участники')
    expect(groupsView).toContain('Доступные студенты')
  })

  it('loads only student candidates while keeping all people for current-member labels', () => {
    expect(groupsView).toContain('usersApi.getPeople()')
    expect(groupsView).toContain("role: STUDENT_APP_ROLE")
    expect(groupsView).toContain("const STUDENT_APP_ROLE = 'STUDENT'")
    expect(groupsView).toContain('personName(personById(membership.personId))')
  })

  it('uses existing membership endpoints and keeps the backend contract unchanged', () => {
    expect(groupsView).toContain('membershipsApi.getGroupMemberships({')
    expect(groupsView).toContain('membershipsApi.addPersonToGroup(')
    expect(groupsView).toContain('membershipsApi.updateGroupMembershipStatus(')
    expect(groupsView).toContain('role: STUDENT_GROUP_ROLE')
    expect(groupsView).toContain('{ status: REMOVED_MEMBERSHIP_STATUS }')
  })

  it('reactivates paused membership instead of creating a conflicting duplicate', () => {
    expect(groupsView).toContain('pausedStudentMembership(personId)')
    expect(groupsView).toContain('PAUSED_MEMBERSHIP_STATUS')
    expect(groupsView).toContain('{ status: ACTIVE_MEMBERSHIP_STATUS }')
    expect(groupsView).toContain('Вернуть в группу')
  })

  it('requires confirmation before removing a student and preserves historical membership', () => {
    expect(groupsView).toContain('memberRemoveConfirmVisible')
    expect(groupsView).toContain('title="Убрать студента из группы?"')
    expect(groupsView).toContain('Историческая запись назначения сохранится в системе.')
    expect(groupsView).not.toContain('deleteGroupMembership')
  })

  it('keeps search inside the drawer and gives every atomic mutation its own loading state', () => {
    expect(groupsView).toContain('memberSearch')
    expect(groupsView).toContain('filteredCurrentStudentMemberships')
    expect(groupsView).toContain('filteredAvailableStudents')
    expect(groupsView).toContain('addingPersonId')
    expect(groupsView).toContain('removingMembershipId')
  })
})
