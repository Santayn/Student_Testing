import { NAV_KEYS } from './navigation.config'
import { getWorkspaceNavigation } from './navigation'

// Destinations are resolved from the canonical, role-scoped navigation.
// A tab cannot accidentally grant access to an unlisted workspace route.
const PRIMARY_DESTINATIONS = Object.freeze({
  STUDENT: [NAV_KEYS.SUBJECTS, NAV_KEYS.RESULTS, NAV_KEYS.HOME, NAV_KEYS.PROFILE],
  TEACHER: [NAV_KEYS.SUBJECTS, NAV_KEYS.TEACHER_QUESTIONS, NAV_KEYS.TEACHER_LECTURES, NAV_KEYS.TEACHER_REVIEW],
  ADMIN: [NAV_KEYS.ADMIN_USERS, NAV_KEYS.ADMIN_GROUPS, NAV_KEYS.ADMIN_SUBJECTS, NAV_KEYS.ADMIN_TEACHING],
})

// Short labels apply to the mobile/rail tabs only, not Sidebar or breadcrumbs.
const PRIMARY_TAB_LABELS = Object.freeze({
  TEACHER: {
    [NAV_KEYS.SUBJECTS]: 'Предметы',
    [NAV_KEYS.TEACHER_REVIEW]: 'Проверка',
  },
  ADMIN: {
    [NAV_KEYS.ADMIN_SUBJECTS]: 'Предметы',
    [NAV_KEYS.ADMIN_TEACHING]: 'Нагрузка',
  },
})

export const MORE_DESTINATION_KEY = 'navigation-more'

export const NAVIGATION_GROUP_ICONS = Object.freeze({
  overview: 'pi pi-home',
  learning: 'pi pi-graduation-cap',
  academic: 'pi pi-building',
  assignments: 'pi pi-sitemap',
  content: 'pi pi-book',
  workload: 'pi pi-calendar',
  access: 'pi pi-shield',
  system: 'pi pi-cog',
  account: 'pi pi-user',
})

export function getPrimaryNavigation(workspaceRole) {
  const sections = getWorkspaceNavigation(workspaceRole)
  const items = sections.flatMap((section) => section.items)
  const available = new Map(items.map((item) => [item.key, item]))
  const primary = (PRIMARY_DESTINATIONS[workspaceRole] ?? [])
    .map((key) => available.get(key))
    .filter(Boolean)
    .map((item) => ({
      ...item,
      label: PRIMARY_TAB_LABELS[workspaceRole]?.[item.key] ?? item.label,
    }))

  if (items.some((item) => !primary.some((tab) => tab.key === item.key))) {
    primary.push({
      key: MORE_DESTINATION_KEY,
      label: 'Разделы',
      icon: 'pi pi-bars',
      route: null,
    })
  }

  return primary
}

export function activePrimaryKey(activeKey, tabs) {
  return tabs.some((item) => item.key === activeKey)
    ? activeKey
    : tabs.some((item) => item.key === MORE_DESTINATION_KEY)
      ? MORE_DESTINATION_KEY
      : null
}
