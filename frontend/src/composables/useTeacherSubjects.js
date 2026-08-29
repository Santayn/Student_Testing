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

const TEACHER_ROLE = 1

export function useTeacherSubjects() {
  const authStore = useAuthStore()

  const loadingSubjects = ref(false)
  const subjectMemberships = ref([])
  const subjects = ref([])
  const selectedSubjectId = ref('')

  const selectedSubject = computed(() => {
    return subjects.value.find(
      (subject) =>
        Number(subject.id) ===
        Number(selectedSubjectId.value)
    ) ?? null
  })

  const selectedMembership = computed(() => {
    return subjectMemberships.value.find(
      (membership) =>
        Number(membership.subjectId) ===
        Number(selectedSubjectId.value)
    ) ?? null
  })

  const subjectOptions = computed(() => {
    return subjects.value.map(
      (subject) => ({
        value: subject.id,
        label: subject.name ??
          `Предмет #${subject.id}`,
      })
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

      if (!personId) {
        throw new Error(
          'Не удалось определить преподавателя по текущему профилю.'
        )
      }

      const membershipsResponse =
        await membershipsApi
          .getSubjectMemberships({
            personId,
            activeOnly: true,
          })

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
                  'ru'
                )
          )

      const preferredMembership =
        preferredMembershipId
          ? subjectMemberships.value
              .find(
                (item) =>
                  String(item.id) ===
                  String(
                    preferredMembershipId
                  )
              )
          : null

      const preferredId =
        preferredMembership?.subjectId ??
        preferredSubjectId

      if (
        preferredId &&
        subjects.value.some(
          (item) =>
            String(item.id) ===
            String(preferredId)
        )
      ) {
        selectedSubjectId.value =
          String(preferredId)
      } else if (
        subjects.value.length === 1
      ) {
        selectedSubjectId.value =
          String(subjects.value[0].id)
      } else {
        selectedSubjectId.value = ''
      }

      return subjects.value
    } finally {
      loadingSubjects.value = false
    }
  }

  return {
    loadingSubjects,
    subjectMemberships,
    subjects,
    selectedSubjectId,
    selectedSubject,
    selectedMembership,
    subjectOptions,
    loadTeacherSubjects,
  }
}
