import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

import {
  LEARNING_CONTEXT_TTL_MS,
  REFERENCE_TTL_MS,
} from '@/utils/learningContextCache'

const TEACHER_ROLE = 1

export async function loadSubjectCatalog(subjectsApi, cache = null) {
  const load = async () => listFromResponse(await subjectsApi.getAll())

  return cache
    ? cache.load('subjects:catalog', load, {
        ttlMs: REFERENCE_TTL_MS,
      })
    : load()
}

/**
 * Shared read-only teacher context. Active eligibility is ALWAYS rechecked
 * against the backend by ensureSelectedMembershipActive() before mutations.
 */
export async function loadTeacherSubjectContext({
  authStore,
  membershipsApi,
  subjectsApi,
  cache = null,
}) {
  const personId = authStore.personId
  const isAdmin = authStore.isAdminMode

  if (!isAdmin && !personId) {
    throw new Error(
      'Не удалось определить преподавателя по текущему профилю.'
    )
  }

  const load = async () => {
    const response = await membershipsApi.getSubjectMemberships(
      isAdmin
        ? { activeOnly: true }
        : { personId, activeOnly: true }
    )

    const memberships = listFromResponse(response)
      .filter((item) => Number(item.role) === TEACHER_ROLE)
      .sort((left, right) => Number(left.id) - Number(right.id))

    const subjectIds = uniqueNumbers(
      memberships.map((item) => item.subjectId)
    )

    if (!subjectIds.length) {
      return { memberships, subjects: [] }
    }

    const allowed = new Set(subjectIds)

    // The real API supports a single catalog request. Keep a getById
    // fallback for isolated component fixtures exposing only that endpoint.
    let subjects
    if (typeof subjectsApi.getAll === 'function') {
      subjects = (await loadSubjectCatalog(subjectsApi, cache))
        .filter((item) => allowed.has(Number(item.id)))
    } else {
      subjects = await Promise.all(
        subjectIds.map(async (id) => {
          const fetchSubject = async () =>
            (await subjectsApi.getById(id)).data

          return cache
            ? cache.load(`subject:${id}`, fetchSubject, {
                ttlMs: REFERENCE_TTL_MS,
              })
            : fetchSubject()
        })
      )
    }

    return {
      memberships,
      subjects: subjects
        .filter(Boolean)
        .sort((left, right) =>
          String(left.name ?? '').localeCompare(
            String(right.name ?? ''),
            'ru',
            { sensitivity: 'base' }
          )
        ),
    }
  }

  return cache
    ? cache.load(`teacher:context:${isAdmin ? 'admin' : personId}`, load, {
        ttlMs: LEARNING_CONTEXT_TTL_MS,
      })
    : load()
}
