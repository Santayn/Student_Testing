import {
  computed,
  onScopeDispose,
  ref,
} from 'vue'

import {
  facultiesApi,
  groupsApi,
  membershipsApi,
  subjectsApi,
  teachingApi,
} from '@/api'

import {
  hasWorkspaceAccess,
} from '@/utils/accountAccess'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  getSharedLearningContextCache,
} from '@/utils/learningContextCache'

import {
  loadTeacherSubjectContext,
} from '@/utils/teacherSubjectContext'

import {
  loadStudentLearningContext,
} from '@/utils/studentLearningContext'

function emptyStudentInfo() {
  return {
    memberships: [],
    groups: [],
    faculties: [],
    subjects: [],
  }
}

function emptyTeacherInfo() {
  return {
    subjects: [],
    groups: [],
  }
}

function entityLabel(entity, fallback) {
  return entity?.name ?? entity?.title ?? entity?.code ?? fallback
}

function sortByLabel(items, fallback) {
  return [...items].sort((left, right) => {
    return entityLabel(left, fallback).localeCompare(
      entityLabel(right, fallback),
      'ru',
      { sensitivity: 'base' }
    )
  })
}

export function useProfileContext({
  authStore,
  router,
  redirectPath = '/profile',
}) {
  const profileRequest = createLatestRequestGuard()

  const identityLoading = ref(false)
  const studentLoading = ref(false)
  const teacherLoading = ref(false)
  const profileError = ref('')
  const studentError = ref('')
  const teacherError = ref('')

  const studentInfo = ref(emptyStudentInfo())
  const teacherInfo = ref(emptyTeacherInfo())

  const profileLoading = computed(() => {
    return (
      identityLoading.value ||
      studentLoading.value ||
      teacherLoading.value
    )
  })

  function resetProfileContext() {
    studentInfo.value = emptyStudentInfo()
    teacherInfo.value = emptyTeacherInfo()
    studentError.value = ''
    teacherError.value = ''
    studentLoading.value = false
    teacherLoading.value = false
  }

  async function loadStudentContext() {
    if (!authStore.personId) {
      return emptyStudentInfo()
    }

    const context = await loadStudentLearningContext({
      personId: authStore.personId,
      membershipsApi,
      groupsApi,
      facultiesApi,
      teachingApi,
      subjectsApi,
      cache: getSharedLearningContextCache(authStore),
    })

    return {
      memberships: context.memberships,
      groups: sortByLabel(context.groups, 'Группа без названия'),
      faculties: sortByLabel(context.faculties, 'Факультет без названия'),
      subjects: sortByLabel(context.subjects, 'Предмет без названия'),
    }
  }

  async function loadTeacherContext() {
    if (!authStore.personId) {
      return emptyTeacherInfo()
    }

    const cache = getSharedLearningContextCache(authStore)
    const { memberships: teacherMemberships, subjects } =
      await loadTeacherSubjectContext({
        authStore,
        membershipsApi,
        subjectsApi,
        cache,
      })

    if (!teacherMemberships.length) {
      return emptyTeacherInfo()
    }

    /*
     * Assignment queries remain scoped by membership as required by the
     * backend. Subject references come from the shared teacher catalog.
     */
    const assignmentResponses = await Promise.all(
      teacherMemberships.map((membership) =>
        teachingApi.getAssignments({
          subjectMembershipId: membership.id,
        })
      )
    )

    const assignments = assignmentResponses.flatMap(listFromResponse)
    const groupIds = new Set(
      uniqueNumbers(assignments.map((item) => item.groupId))
    )

    let groups = []

    if (groupIds.size) {
      const fetchGroups = async () =>
        listFromResponse(await groupsApi.getAll())
      const allGroups = cache
        ? await cache.load('groups:catalog', fetchGroups, {
            ttlMs: 60_000,
          })
        : await fetchGroups()

      groups = allGroups
        .filter((group) => groupIds.has(Number(group.id)))
    }

    return {
      subjects: sortByLabel(subjects, 'Предмет без названия'),
      groups: sortByLabel(groups, 'Группа без названия'),
    }
  }

  async function redirectAfterIdentityLoss() {
    if (!authStore.isAuthenticated) {
      await router.replace({
        name: 'login',
        query: {
          redirect: redirectPath,
        },
      })

      return true
    }

    if (!hasWorkspaceAccess(authStore)) {
      await router.replace({
        name: 'account-pending',
      })

      return true
    }

    return false
  }

  async function loadProfile({ synchronizeIdentity = false } = {}) {
    const requestId = profileRequest.begin()

    identityLoading.value =
      Boolean(synchronizeIdentity)
    profileError.value = ''
    resetProfileContext()

    try {
      /*
       * Auth bootstrap already loaded the authoritative identity before a
       * protected route can render. Reuse that store snapshot on ordinary
       * Profile mount instead of paying another /auth/me round-trip.
       *
       * Explicit "Обновить данные" remains a forced server synchronization.
       */
      if (synchronizeIdentity) {
        await authStore.refreshIdentity()
      }

      if (!profileRequest.isCurrent(requestId)) {
        return
      }

      if (await redirectAfterIdentityLoss()) {
        return
      }
    } catch {
      if (!profileRequest.isCurrent(requestId)) {
        return
      }

      if (await redirectAfterIdentityLoss()) {
        return
      }

      profileError.value =
        'Не удалось обновить данные профиля. Повторите попытку.'

      return
    } finally {
      if (profileRequest.isCurrent(requestId)) {
        identityLoading.value = false
      }
    }

    const jobs = []

    if (authStore.isStudent) {
      studentLoading.value = true

      jobs.push(
        loadStudentContext()
          .then((context) => {
            if (profileRequest.isCurrent(requestId)) {
              studentInfo.value = context
            }
          })
          .catch(() => {
            if (profileRequest.isCurrent(requestId)) {
              studentInfo.value = emptyStudentInfo()
              studentError.value =
                'Не удалось загрузить учебный контекст студента.'
            }
          })
          .finally(() => {
            if (profileRequest.isCurrent(requestId)) {
              studentLoading.value = false
            }
          })
      )
    }

    if (authStore.isTeacher) {
      teacherLoading.value = true

      jobs.push(
        loadTeacherContext()
          .then((context) => {
            if (profileRequest.isCurrent(requestId)) {
              teacherInfo.value = context
            }
          })
          .catch(() => {
            if (profileRequest.isCurrent(requestId)) {
              teacherInfo.value = emptyTeacherInfo()
              teacherError.value =
                'Не удалось загрузить учебный контекст преподавателя.'
            }
          })
          .finally(() => {
            if (profileRequest.isCurrent(requestId)) {
              teacherLoading.value = false
            }
          })
      )
    }

    await Promise.all(jobs)
  }

  onScopeDispose(() => {
    profileRequest.invalidate()
  })

  return {
    profileLoading,
    studentLoading,
    teacherLoading,
    profileError,
    studentError,
    teacherError,
    studentInfo,
    teacherInfo,
    loadProfile,
  }
}
