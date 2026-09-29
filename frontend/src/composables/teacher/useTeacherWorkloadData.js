import { ref, watch } from 'vue'
import { getApiErrorMessage, groupsApi, teachingApi } from '@/api'
import { listFromResponse } from '@/utils/apiData'
import { createLatestRequestGuard } from '@/utils/latestRequest'

/** Loads only the teacher's existing assignments for the selected period. */
export function useTeacherWorkloadData({
  subjectMemberships,
  loadTeacherSubjects,
}) {
  const studyCourse = ref(1)
  const semester = ref(1)
  const academicYear = ref(new Date().getFullYear())
  const assignments = ref([])
  const loadTypes = ref([])
  const loading = ref(false)
  const initialized = ref(false)
  const notice = ref({ type: 'info', message: '' })
  const assignmentsRequest = createLatestRequestGuard()
  let disposed = false

  async function loadLoadTypes() {
    const response =
      await teachingApi.getLoadTypes()

    if (!disposed) {
      loadTypes.value = listFromResponse(response)
    }
  }

  async function refreshAssignments() {
    if (!initialized.value || disposed) {
      return
    }

    const requestId =
      assignmentsRequest.begin()

    const periodContext = {
      studyCourse: Number(
        studyCourse.value
      ),
      semester: Number(
        semester.value
      ),
      academicYear: Number(
        academicYear.value
      ),
    }

    const membershipSnapshot =
      subjectMemberships.value.map(
        (membership) => ({
          ...membership,
        })
      )

    loading.value = true
    notice.value.message = ''

    try {
      if (!membershipSnapshot.length) {
        if (
          assignmentsRequest.isCurrent(
            requestId
          )
        ) {
          assignments.value = []
        }

        return
      }

      const responses = await Promise.all(
        membershipSnapshot.map(
          (membership) =>
            teachingApi.getAssignments({
              subjectMembershipId:
                membership.id,
              ...periodContext,
            })
        )
      )

      if (
        !assignmentsRequest.isCurrent(
          requestId
        )
      ) {
        return
      }

      const rawAssignments = responses
        .flatMap(listFromResponse)
        .filter(
          (item, index, items) =>
            items.findIndex(
              (other) =>
                Number(other.id) ===
                Number(item.id)
            ) === index
        )

      const groupIds = [
        ...new Set(
          rawAssignments
            .map((item) =>
              Number(item.groupId)
            )
            .filter(Boolean)
        ),
      ]

      const groupResponses =
        await Promise.all(
          groupIds.map(
            (groupId) =>
              groupsApi.getById(groupId)
          )
        )

      if (
        !assignmentsRequest.isCurrent(
          requestId
        )
      ) {
        return
      }

      const groupsById = new Map(
        groupResponses
          .map((response) => response.data)
          .filter(Boolean)
          .map((group) => [
            Number(group.id),
            group,
          ])
      )

      assignments.value =
        rawAssignments.map(
          (item) => ({
            ...item,
            groupName:
              groupsById.get(
                Number(item.groupId)
              )?.name ?? null,
            groupCode:
              groupsById.get(
                Number(item.groupId)
              )?.code ?? null,
          })
        )
    } catch (error) {
      if (
        !assignmentsRequest.isCurrent(
          requestId
        )
      ) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить назначенную учебную нагрузку.'
        ),
      }
    } finally {
      if (
        assignmentsRequest.isCurrent(
          requestId
        )
      ) {
        loading.value = false
      }
    }
  }
  const stopPeriodWatch = watch(
    [studyCourse, semester, academicYear],
    () => refreshAssignments()
  )

  async function initialize() {
    try {
      await Promise.all([
        loadTeacherSubjects(),
        loadLoadTypes(),
      ])

      if (disposed) return
      initialized.value = true
      await refreshAssignments()
    } catch (error) {
      if (disposed) return
      initialized.value = true
      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить данные преподавателя.'
        ),
      }
    }
  }

  function dispose() {
    disposed = true
    stopPeriodWatch()
    assignmentsRequest.invalidate()
  }

  return {
    studyCourse,
    semester,
    academicYear,
    assignments,
    loadTypes,
    loading,
    notice,
    refreshAssignments,
    initialize,
    dispose,
  }
}
