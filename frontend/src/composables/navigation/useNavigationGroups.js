import { ref, watch } from 'vue'

const STORAGE_PREFIX = 'st:navigation:expanded:'

function readExpanded(role, surface, sections, activeKey) {
  const valid = new Set(sections.map((section) => section.key))
  let saved = null

  try {
    const raw = localStorage.getItem(`${STORAGE_PREFIX}${surface}:${role}`)
    if (raw !== null) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        saved = parsed.filter((key) => valid.has(key))
      }
    }
  } catch {
    // Storage may be blocked. Navigation remains fully functional.
  }

  const defaults = sections
    .filter((section) => section.items.some((item) => item.key === activeKey))
    .map((section) => section.key)

  return [...new Set(saved === null ? defaults : [...saved, ...defaults])]
}

/** Several sections may stay open. Expansion is scoped to role and surface. */
export function useNavigationGroups({ role, sections, activeKey, surface }) {
  const expanded = ref([])
  let restoring = false

  watch([role, sections], () => {
    restoring = true
    expanded.value = readExpanded(role.value, surface, sections.value, activeKey.value)
    restoring = false
  }, { immediate: true })

  watch(activeKey, (key) => {
    if (!key) return
    const section = sections.value.find((entry) =>
      entry.items.some((item) => item.key === key)
    )
    if (section && !expanded.value.includes(section.key)) {
      expanded.value = [...expanded.value, section.key]
    }
  })

  watch(expanded, (keys) => {
    if (restoring) return
    try {
      localStorage.setItem(
        `${STORAGE_PREFIX}${surface}:${role.value}`,
        JSON.stringify(keys)
      )
    } catch {
      // Private browsing / quota limits should not affect menu controls.
    }
  })

  function toggleGroup(key) {
    expanded.value = expanded.value.includes(key)
      ? expanded.value.filter((value) => value !== key)
      : [...expanded.value, key]
  }

  return { expanded, toggleGroup }
}
