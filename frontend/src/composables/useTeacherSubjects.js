import {
  computed,
  ref,
} from 'vue'

import {
  membershipsApi,
  subjectsApi,
} from '@/api'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  isAssignableTeacherMembership,
} from '@/utils/teacherMembershipEligibility'

const TEACHER_ROLE = 1

export function useTeacherSubjects() {
  const authStore = useAuthStore()

  const loadingSubjects = ref(false)
  const subjectMemberships = ref([])
  const subjects = ref([])

  /*
   * SubjectMembership — основной контекст преподавателя.
   * Именно membership нужен Topics API и другим teacher-сценариям,
   * поэтому храним выбранным прежде всего его ID.
   */
  const selectedMembershipId = ref('')

  /*
   * Исторические membership оставляем в subjectMemberships,
   * чтобы экраны вроде нагрузки могли корректно показать уже
   * существующие связи. Но для новых teacher-действий и выбора
   * в UI используем только реально активные назначения.
   */
  const activeSubjectMemberships = computed(() => {
    return subjectMemberships.value.filter(
      isAssignableTeacherMembership
    )
  })

  const selectedMembership = computed(() => {
    return activeSubjectMemberships.value.find(
      (membership) =>
        String(membership.id) ===
        String(selectedMembershipId.value)
    ) ?? null
  })

  const selectedSubject = computed(() => {
    const membership =
      selectedMembership.value

    if (!membership) {
      return null
    }

    return subjects.value.find(
      (subject) =>
        Number(subject.id) ===
        Number(membership.subjectId)
    ) ?? null
  })

  /*
   * Совместимость с уже существующими teacher views.
   * Они по-прежнему могут работать через selectedSubjectId,
   * но setter всегда переводит выбор в конкретный membership.
   */
  const selectedSubjectId = computed({
    get() {
      return selectedMembership.value
        ? String(
            selectedMembership.value
              .subjectId
          )
        : ''
    },

    set(value) {
      if (
        value === null ||
        value === undefined ||
        value === ''
      ) {
        selectedMembershipId.value = ''
        return
      }

      const current =
        selectedMembership.value

      if (
        current &&
        String(current.subjectId) ===
          String(value)
      ) {
        return
      }

      const membership =
        activeSubjectMemberships.value.find(
          (item) =>
            String(item.subjectId) ===
            String(value)
        )

      selectedMembershipId.value =
        membership
          ? String(membership.id)
          : ''
    },
  })

  const subjectOptions = computed(() => {
    const activeSubjectIds = new Set(
      activeSubjectMemberships.value
        .map((membership) =>
          Number(membership.subjectId)
        )
        .filter(Boolean)
    )

    return subjects.value
      .filter((subject) =>
        activeSubjectIds.has(
          Number(subject.id)
        )
      )
      .map((subject) => ({
        value: subject.id,
        label:
          subject.name ??
          `Предмет #${subject.id}`,
      }))
  })

  const membershipOptions = computed(() => {
    const membershipCountBySubject =
      activeSubjectMemberships.value.reduce(
        (map, membership) => {
          const key =
            String(
              membership.subjectId
            )

          map.set(
            key,
            (map.get(key) ?? 0) + 1
          )

          return map
        },
        new Map()
      )

    return activeSubjectMemberships.value.map(
      (membership) => {
        const subject =
          subjects.value.find(
            (item) =>
              Number(item.id) ===
              Number(
                membership.subjectId
              )
          )

        const baseLabel =
          subject?.name ??
          `Предмет #${membership.subjectId}`

        const duplicates =
          membershipCountBySubject.get(
            String(
              membership.subjectId
            )
          ) ?? 0

        const adminSuffix =
          authStore.isAdmin
            ? ` — преподаватель #${membership.personId ?? '?'} · назначение #${membership.id}`
            : ''

        return {
          value: membership.id,
          subjectId:
            membership.subjectId,
          label: authStore.isAdmin
            ? `${baseLabel}${adminSuffix}`
            : duplicates > 1
              ? `${baseLabel} — назначение #${membership.id}`
              : baseLabel,
        }
      }
    )
  })

  async function loadTeacherSubjects({
    preferredSubjectId = null,
    preferredMembershipId = null,
  } = {}) {
    loadingSubjects.value = true

    try {
      const personId =
        authStore.personId

      if (
        !authStore.isAdmin &&
        !personId
      ) {
        throw new Error(
          'Не удалось определить преподавателя по текущему профилю.'
        )
      }

      const membershipParams =
        authStore.isAdmin
          ? {
              activeOnly: true,
            }
          : {
              personId,
              activeOnly: true,
            }

      const membershipsResponse =
        await membershipsApi
          .getSubjectMemberships(
            membershipParams
          )

      subjectMemberships.value =
        listFromResponse(
          membershipsResponse
        )
          .filter(
            (item) =>
              Number(item.role) ===
              TEACHER_ROLE
          )
          .sort(
            (left, right) =>
              Number(left.id) -
              Number(right.id)
          )

      const subjectIds = [
        ...new Set(
          subjectMemberships.value
            .map(
              (item) =>
                Number(item.subjectId)
            )
            .filter(Boolean)
        ),
      ]

      const subjectResponses =
        await Promise.all(
          subjectIds.map(
            (subjectId) =>
              subjectsApi.getById(
                subjectId
              )
          )
        )

      subjects.value =
        subjectResponses
          .map(
            (response) =>
              response.data
          )
          .filter(Boolean)
          .sort(
            (left, right) =>
              String(left.name ?? '')
                .localeCompare(
                  String(
                    right.name ?? ''
                  ),
                  'ru',
                  {
                    sensitivity: 'base',
                  }
                )
          )

      const preferredMembership =
        preferredMembershipId
          ? activeSubjectMemberships.value
              .find(
                (item) =>
                  String(item.id) ===
                  String(
                    preferredMembershipId
                  )
              )
          : null

      if (preferredMembership) {
        selectedMembershipId.value =
          String(
            preferredMembership.id
          )

        return subjects.value
      }

      const preferredBySubject =
        preferredSubjectId
          ? activeSubjectMemberships.value
              .find(
                (item) =>
                  String(
                    item.subjectId
                  ) ===
                  String(
                    preferredSubjectId
                  )
              )
          : null

      if (preferredBySubject) {
        selectedMembershipId.value =
          String(
            preferredBySubject.id
          )
      } else if (
        activeSubjectMemberships.value
          .length === 1
      ) {
        selectedMembershipId.value =
          String(
            activeSubjectMemberships.value[0]
              .id
          )
      } else {
        selectedMembershipId.value = ''
      }

      return subjects.value
    } finally {
      loadingSubjects.value = false
    }
  }

  return {
    loadingSubjects,
    subjectMemberships,
    activeSubjectMemberships,
    subjects,
    selectedMembershipId,
    selectedMembership,
    selectedSubjectId,
    selectedSubject,
    subjectOptions,
    membershipOptions,
    loadTeacherSubjects,
  }
}
