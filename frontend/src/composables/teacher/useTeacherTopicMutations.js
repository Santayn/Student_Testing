import { ref } from 'vue'

import {
  API_ERROR_CODES,
  topicsApi,
} from '@/api'

import {
  presentApiError,
} from '@/utils/apiErrorPresentation'

import {
  FORM_FIELD_ERROR_SUMMARY,
  setFormFieldError,
} from '@/utils/formErrorLifecycle'

export function useTeacherTopicMutations({
  form,
  selectedMembership,
  topics,
  notice,
  ensureSelectedMembershipActive,
  beginSaving,
  saving,
  finishSaving,
  failSaving,
  loadTopics,
  focusFormErrors = null,
}) {
  const deleteTarget = ref(null)
  const deleteConfirmVisible = ref(false)
  const deletingId = ref(null)
  const deleteError = ref('')
  const formError = ref('')
  const formFieldErrors = ref({})

  function requestDeleteTopic(topic) {
    deleteTarget.value = topic
    deleteError.value = ''
    deleteConfirmVisible.value = true
  }

  function closeDeleteDialog() {
    if (deletingId.value !== null) {
      return
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    deleteError.value = ''
  }

  function topicFormValidation() {
    const membership = selectedMembership.value
    const ordinal = Number(form.ordinal)
    const name = String(form.name ?? '').trim()
    const description = String(form.description ?? '').trim()

    if (!membership) {
      return { field: null, message: 'Выберите предмет преподавателя.' }
    }

    if (!Number.isInteger(ordinal) || ordinal <= 0) {
      return { field: 'ordinal', message: 'Порядковый номер темы должен быть целым числом больше нуля.' }
    }

    if (!name) {
      return { field: 'name', message: 'Введите название темы.' }
    }

    if (name.length > 200) {
      return { field: 'name', message: 'Название темы не может быть длиннее 200 символов.' }
    }

    if (description.length > 2000) {
      return { field: 'description', message: 'Описание темы не может быть длиннее 2000 символов.' }
    }

    const duplicateOrdinal = topics.value.find(
      (topic) =>
        Number(topic.ordinal) === ordinal &&
        String(topic.id) !== String(form.id ?? '')
    )

    if (duplicateOrdinal) {
      return { field: 'ordinal', message: 'Тема с таким порядковым номером уже существует в выбранном назначении преподавателя.' }
    }

    return null
  }

  function topicFormValidationMessage() {
    return (
      topicFormValidation()?.message ??
      ''
    )
  }

  async function saveTopic() {
    if (saving?.value) {
      return false
    }

    formFieldErrors.value = {}
    formError.value = ''

    const validation =
      topicFormValidation()

    if (validation) {
      if (validation.field) {
        setFormFieldError(
          formFieldErrors,
          formError,
          validation.field,
          validation.message
        )

        await focusFormErrors?.()
      } else {
        formError.value =
          validation.message
      }

      return false
    }

    const membership = selectedMembership.value
    const payload = {
      subjectId: Number(membership.subjectId),
      courseLectureId: null,
      subjectMembershipId: Number(membership.id),
      ordinal: Number(form.ordinal),
      name: String(form.name ?? '').trim(),
      description: String(form.description ?? '').trim() || null,
    }

    beginSaving()

    try {
      await ensureSelectedMembershipActive()

      if (form.id) {
        await topicsApi.update(form.id, payload)
        notice.value = { type: 'success', message: 'Тема обновлена.' }
      } else {
        await topicsApi.create(payload)
        notice.value = { type: 'success', message: 'Тема создана.' }
      }

      await loadTopics()
      finishSaving({ close: true })
    } catch (error) {
      const presentation = presentApiError(
        error,
        {
          context: 'form',
          fallback: form.id
            ? 'Не удалось обновить тему.'
            : 'Не удалось создать тему.',
          forbiddenMessage:
            'Недостаточно прав для сохранения темы.',
        }
      )

      formFieldErrors.value =
        presentation.fieldErrors

      formError.value =
        presentation.channel === 'field'
          ? FORM_FIELD_ERROR_SUMMARY
          : presentation.message

      failSaving()

      if (
        presentation.channel === 'field'
      ) {
        await focusFormErrors?.()
      }
    }
  }

  async function deleteTopic() {
    const topic = deleteTarget.value

    if (!topic || deletingId.value !== null) {
      return
    }

    deletingId.value = topic.id
    deleteError.value = ''

    try {
      await ensureSelectedMembershipActive()
      await topicsApi.remove(topic.id)

      notice.value = { type: 'success', message: 'Тема удалена.' }
      deleteConfirmVisible.value = false
      deleteTarget.value = null
      await loadTopics()
    } catch (error) {
      deleteError.value = presentApiError(
        error,
        {
          context: 'delete',
          fallback:
            'Не удалось удалить тему.',
          codeMessages: {
            [API_ERROR_CODES.TOPIC_HAS_DEPENDENCIES]:
              'Тема используется вопросами, тестами или другими связанными данными и не может быть удалена.',
          },
          conflictMessage:
            'Тема используется вопросами, тестами или другими связанными данными и не может быть удалена.',
          forbiddenMessage:
            'Недостаточно прав для удаления темы.',
          notFoundMessage:
            'Тема уже удалена или больше недоступна.',
        }
      ).message
    } finally {
      deletingId.value = null
    }
  }

  return {
    deleteTarget,
    deleteConfirmVisible,
    deletingId,
    deleteError,
    formError,
    formFieldErrors,
    requestDeleteTopic,
    closeDeleteDialog,
    topicFormValidationMessage,
    saveTopic,
    deleteTopic,
  }
}
