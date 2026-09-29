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

  async function ensureSelectedSubjectLoadType() {
    await teachingApi
      .addLoadTypeToSubjectMembership(
        Number(
          assignmentForm.subjectMembershipId
        ),
        {
          teachingLoadTypeId: Number(
            assignmentForm.loadTypeId
          ),
          notes:
            'Добавлено администратором при настройке учебной нагрузки',
        }
      )
  }

  async function validateSelectedTeacherMembership() {
    const selectedId = Number(
      assignmentForm.subjectMembershipId
    )

    const editingAssignment =
      assignments.value.find(
        (item) =>
          Number(item.id) ===
          Number(assignmentForm.id)
      ) ?? null

    const teacherChanged =
      !editingAssignment ||
      Number(
        editingAssignment.subjectMembershipId
      ) !== selectedId

    const statusRequiresActiveTeacher =
      [1, 2].includes(
        Number(assignmentForm.status)
      )

    if (
      assignmentIsCreate.value ||
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

    beginAssignmentSaving()

    try {
      await validateSelectedTeacherMembership()
      await ensureSelectedSubjectLoadType()

      if (assignmentIsCreate.value) {
        const groupIds = uniqueNumbers(
          assignmentForm.groupIds
        )

        const results =
          await Promise.allSettled(
            groupIds.map((groupId) =>
              teachingApi.createAssignment(
                assignmentPayload(groupId)
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
              ...assignmentForm,
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

      const previous = assignments.value.find(
        (item) =>
          Number(item.id) ===
          Number(assignmentForm.id)
      )

      await teachingApi.updateAssignment(
        assignmentForm.id,
        assignmentPayload(
          assignmentForm.groupId
        )
      )

      const movedOutOfContext =
        Number(assignmentForm.studyCourse) !==
          Number(context.studyCourse) ||
        Number(assignmentForm.semester) !==
          Number(context.semester) ||
        Number(assignmentForm.academicYear) !==
          Number(context.academicYear) ||
        (
          previous &&
          !groups.value.some(
            (group) =>
              Number(group.id) ===
              Number(assignmentForm.groupId)
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
            assignmentIsCreate.value
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
