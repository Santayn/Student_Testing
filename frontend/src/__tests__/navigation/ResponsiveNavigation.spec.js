import { describe, expect, it } from 'vitest'
import { getWorkspaceNavigation } from '@/navigation'
import { NAV_KEYS } from '@/navigation/navigation.config'
import {
  getPrimaryNavigation,
  activePrimaryKey,
  MORE_DESTINATION_KEY,
} from '@/navigation/presentationNavigation'

for (const role of ['STUDENT', 'TEACHER', 'ADMIN']) {
  describe(`${role} responsive navigation`, () => {
    it('only promotes existing role routes and never exceeds five entries', () => {
      const tabs = getPrimaryNavigation(role)
      const canonical = getWorkspaceNavigation(role).flatMap((group) => group.items)
      expect(tabs).toHaveLength(role === 'STUDENT' ? 4 : 5)
      expect(tabs.length).toBeLessThanOrEqual(5)
      for (const tab of tabs.filter((item) => item.key !== MORE_DESTINATION_KEY)) {
        expect(canonical.some((item) => item.key === tab.key)).toBe(true)
        expect(tab.route?.name).toBeTruthy()
      }
    })
  })
}

it('uses task-first mobile navigation for each role', () => {
  expect(getPrimaryNavigation('STUDENT').map((item) => item.key)).toEqual([
    NAV_KEYS.SUBJECTS, NAV_KEYS.RESULTS, NAV_KEYS.HOME, NAV_KEYS.PROFILE,
  ])
  expect(getPrimaryNavigation('TEACHER').map((item) => item.key)).toEqual([
    NAV_KEYS.SUBJECTS, NAV_KEYS.TEACHER_QUESTIONS, NAV_KEYS.TEACHER_LECTURES,
    NAV_KEYS.TEACHER_REVIEW, MORE_DESTINATION_KEY,
  ])
  expect(getPrimaryNavigation('ADMIN').map((item) => item.key)).toEqual([
    NAV_KEYS.ADMIN_USERS, NAV_KEYS.ADMIN_GROUPS, NAV_KEYS.ADMIN_SUBJECTS,
    NAV_KEYS.ADMIN_TEACHING, MORE_DESTINATION_KEY,
  ])
  expect(getPrimaryNavigation('TEACHER')[3].label).toBe('Проверка')
  expect(getPrimaryNavigation('ADMIN')[2].label).toBe('Предметы')
  expect(getPrimaryNavigation('ADMIN')[3].label).toBe('Нагрузка')
})

it('maps non-primary routes to More instead of selecting a wrong page', () => {
  const tabs = getPrimaryNavigation('ADMIN')
  expect(activePrimaryKey(NAV_KEYS.ADMIN_DATABASE_BACKUPS, tabs)).toBe(MORE_DESTINATION_KEY)
  expect(activePrimaryKey(NAV_KEYS.ADMIN_USERS, tabs)).toBe(NAV_KEYS.ADMIN_USERS)
})

it('does not show More when all destinations already fit', () => {
  expect(getPrimaryNavigation('STUDENT').some((item) => item.key === MORE_DESTINATION_KEY)).toBe(false)
})
