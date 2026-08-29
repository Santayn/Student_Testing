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

  /*
   * SubjectMembership — основной контекст преподавателя.
   * Именно membership нужен Topics API и другим teacher-сценариям,
   * поэтому храним выбранным прежде всего его ID.
   */
  const selectedMembershipId = ref('')

  const selectedMembership = computed(() => {
    return subjectMemberships.value.find(
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
        subjectMemberships.value.find(
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
    return subjects.value.map(
      (subject) => ({
        value: subject.id,
        label:
          subject.name ??
          `Предмет #${subject.id}`,
      })
    )
  })

  const membershipOptions = computed(() => {
    const membershipCountBySubject =
      subjectMemberships.value.reduce(
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

    return subjectMemberships.value.map(
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

        return {
          value: membership.id,
          subjectId:
            membership.subjectId,
          label:
            duplicates > 1
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
                  'ru',
                  {
                    sensitivity: 'base',
                  }
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

      if (preferredMembership) {
        selectedMembershipId.value =
          String(
            preferredMembership.id
          )

        return subjects.value
      }

      const preferredBySubject =
        preferredSubjectId
          ? subjectMemberships.value
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
        subjectMemberships.value
          .length === 1
      ) {
        selectedMembershipId.value =
          String(
            subjectMemberships.value[0]
              .id
          )
      } else if (
        subjects.value.length === 1 &&
        subjectMemberships.value.length
      ) {
        /*
         * Сохраняем старое удобство для экранов,
         * где выбор идёт по предмету. TopicLibrary при наличии
         * дубликатов всё равно показывает membershipOptions.
         */
        selectedMembershipId.value =
          String(
            subjectMemberships.value[0]
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
