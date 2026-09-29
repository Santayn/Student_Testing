// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(
      process.cwd(),
      'src',
      ...relativePath.split('/')
    ),
    'utf8'
  )
}

const groupsView = source('views/admin/GroupsView.vue')
const groupMembers = source('composables/admin/groups/useAdminGroupMembers.js')
const membersDrawer = source('components/admin/AdminGroupMembersDrawer.vue')

describe('admin group members drawer', () => {
  it('keeps GroupsView as the composition boundary and delegates the member workspace', () => {
    expect(groupsView).toContain('AdminGroupMembersDrawer')
    expect(groupsView).toContain('useAdminGroupMembers()')
    expect(groupsView).toContain('label="Состав группы"')
    expect(groupsView).toContain('openGroupMembers(group)')
    expect(groupsView).not.toContain('membershipsApi.')
    expect(groupsView).not.toContain('usersApi.getPeople')
  })

  it('keeps the drawer UI outside the page-level view', () => {
    expect(membersDrawer).toContain('<UiDrawer')
    expect(membersDrawer).toContain('Текущие участники')
    expect(membersDrawer).toContain('Доступные студенты')
    expect(membersDrawer).toContain('title="Убрать студента из группы?"')
    expect(membersDrawer).toContain('Историческая запись назначения сохранится в системе.')
    expect(membersDrawer).not.toContain("from '@/api'")
  })

  it('loads only student candidates while keeping all people for current-member labels', () => {
    expect(groupMembers).toContain('usersApi.getPeople()')
    expect(groupMembers).toContain("role: STUDENT_APP_ROLE")
    expect(groupMembers).toContain("const STUDENT_APP_ROLE = 'STUDENT'")
    expect(membersDrawer).toContain('personName(personById(membership.personId))')
  })

  it('uses existing membership endpoints and keeps the backend contract unchanged', () => {
    expect(groupMembers).toContain('membershipsApi.getGroupMemberships({')
    expect(groupMembers).toContain('membershipsApi.addPersonToGroup(')
    expect(groupMembers).toContain('membershipsApi.updateGroupMembershipStatus(')
    expect(groupMembers).toContain('role: STUDENT_GROUP_ROLE')
    expect(groupMembers).toContain('{ status: REMOVED_MEMBERSHIP_STATUS }')
  })

  it('reactivates paused membership instead of creating a conflicting duplicate', () => {
    expect(groupMembers).toContain('pausedStudentMembership(personId)')
    expect(groupMembers).toContain('PAUSED_MEMBERSHIP_STATUS')
    expect(groupMembers).toContain('{ status: ACTIVE_MEMBERSHIP_STATUS }')
    expect(groupMembers).toContain("? 'Вернуть в группу'")
  })

  it('keeps search and atomic mutation loading state inside the member boundary', () => {
    expect(groupMembers).toContain('memberSearch')
    expect(groupMembers).toContain('filteredCurrentStudentMemberships')
    expect(groupMembers).toContain('filteredAvailableStudents')
    expect(groupMembers).toContain('addingPersonId')
    expect(groupMembers).toContain('removingMembershipId')
    expect(membersDrawer).toContain(':loading="addingPersonId === person.id"')
    expect(membersDrawer).toContain(':loading="removingMembershipId === membership.id"')
  })
})
