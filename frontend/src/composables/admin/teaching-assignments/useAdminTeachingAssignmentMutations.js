import {
  getApiErrorMessage,
  membershipsApi,
  teachingApi,
} from '@/api'

import {
  uniqueNumbers,
} from '@/utils/apiData'

import {
  revalidateAssignableTeacherMembershipIds,
  TeacherMembershipEligibilityError,
} from '@/utils/teacherMembershipEligibility'

export function useAdminTeachingAssignmentMutations({
  assignmentForm,
  assignmentIsCreate,
  assignmentSaving,
  assignmentFormError,
  assignmentValidationMessage,
  assignmentPayload,
  beginAssignmentSaving,
  finishAssignmentSaving,
  failAssignmentSaving,
  assignments,
  groups,
  groupName,
  context,
  refreshAssignments,
  showNotice,
}) {
  async function currentAssignableMembershipIds(
    membershipIds
  ) {
    return revalidateAssignableTeacherMembershipIds({
      api: membershipsApi,
      membershipIds,
    })
  }

  function captureAssignmentForm() {
    return {
      ...assignmentForm,
      groupIds: Array.isArray(assignmentForm.groupIds)
        ? [...assignmentForm.groupIds]
        : [],
    }
  }

  async function ensureSelectedSubjectLoadType(formSnapshot) {
    await teachingApi
      .addLoadTypeToSubjectMembership(
        Number(formSnapshot.subjectMembershipId),
        {
          teachingLoadTypeId: Number(
            formSnapshot.loadTypeId
          ),
          notes:
            'Добавлено администратором при настройке учебной нагрузки',
        }
      )
  }

  async function validateSelectedTeacherMembership({
    formSnapshot,
    isCreate,
    editingAssignment,
  }) {
    const selectedId = Number(
      formSnapshot.subjectMembershipId
    )

    const teacherChanged =
      !editingAssignment ||
      Number(
        editingAssignment.subjectMembershipId
      ) !== selectedId

    const statusRequiresActiveTeacher =
      [1, 2].includes(
        Number(formSnapshot.status)
      )

    if (
      isCreate ||
      teacherChanged ||
      statusRequiresActiveTeacher
    ) {
      await currentAssignableMembershipIds([
        selectedId,
      ])
    }
  }

  async function saveAssignment() {
    if (assignmentSaving.value) {
      return
    }

    assignmentFormError.value = ''

    const validation =
      assignmentValidationMessage()

    if (validation) {
      assignmentFormError.value = validation
      return
    }

    const formSnapshot = captureAssignmentForm()
    const isCreate = Boolean(assignmentIsCreate.value)
    const contextSnapshot = {
      studyCourse: Number(context.studyCourse),
      semester: Number(context.semester),
      academicYear: Number(context.academicYear),
    }
    const availableGroupIds = new Set(
      groups.value.map((group) => Number(group.id))
    )
    const editingAssignment = isCreate
      ? null
      : assignments.value.find(
          (item) =>
            Number(item.id) ===
            Number(formSnapshot.id)
        ) ?? null

    beginAssignmentSaving()

    try {
      await validateSelectedTeacherMembership({
        formSnapshot,
        isCreate,
        editingAssignment,
      })
      await ensureSelectedSubjectLoadType(
        formSnapshot
      )

      if (isCreate) {
        const groupIds = uniqueNumbers(
          formSnapshot.groupIds
        )

        const results =
          await Promise.allSettled(
            groupIds.map((groupId) =>
              teachingApi.createAssignment(
                assignmentPayload(
                  groupId,
                  formSnapshot
                )
              )
            )
          )

        const failedGroupIds = []
        let successCount = 0
        let firstError = null

        results.forEach((result, index) => {
          if (result.status === 'fulfilled') {
            successCount += 1
            return
          }

          failedGroupIds.push(
            groupIds[index]
          )
          firstError ??= result.reason
        })

        await refreshAssignments()

        if (failedGroupIds.length) {
          const failedNames = failedGroupIds
            .map(groupName)
            .join(', ')

          finishAssignmentSaving({
            close: false,
            values: {
              ...formSnapshot,
              groupIds: failedGroupIds,
            },
          })

          assignmentFormError.value =
            successCount
              ? `Создано назначений: ${successCount}. Не удалось создать для групп: ${failedNames}. ${getApiErrorMessage(firstError, '')}`.trim()
              : getApiErrorMessage(
                  firstError,
                  'Не удалось создать назначения.'
                )
          return
        }

        showNotice(
          'success',
          groupIds.length === 1
            ? 'Учебная нагрузка создана.'
            : `Создано назначений: ${groupIds.length}.`
        )

        finishAssignmentSaving({ close: true })
        return
      }

      await teachingApi.updateAssignment(
        formSnapshot.id,
        assignmentPayload(
          formSnapshot.groupId,
          formSnapshot
        )
      )

      const movedOutOfContext =
        Number(formSnapshot.studyCourse) !==
          contextSnapshot.studyCourse ||
        Number(formSnapshot.semester) !==
          contextSnapshot.semester ||
        Number(formSnapshot.academicYear) !==
          contextSnapshot.academicYear ||
        (
          editingAssignment &&
          !availableGroupIds.has(
            Number(formSnapshot.groupId)
          )
        )

      await refreshAssignments()

      showNotice(
        'success',
        movedOutOfContext
          ? 'Назначение обновлено и перенесено в другой учебный период.'
          : 'Учебная нагрузка обновлена.'
      )

      finishAssignmentSaving({ close: true })
    } catch (error) {
      if (
        error instanceof
        TeacherMembershipEligibilityError
      ) {
        assignmentFormError.value =
          'Выбранное назначение преподавателя больше не активно. Выберите преподавателя заново.'
      } else {
        assignmentFormError.value =
          getApiErrorMessage(
            error,
            isCreate
              ? 'Не удалось создать учебную нагрузку.'
              : 'Не удалось обновить учебную нагрузку.'
          )
      }

      failAssignmentSaving()
    }
  }

  return {
    saveAssignment,
  }
}
