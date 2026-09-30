import { ref } from 'vue'

import {
  API_ERROR_CODES,
  topicsApi,
} from '@/api'

import {
  presentApiError,
} from '@/utils/apiErrorPresentation'

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

  function topicFormValidationMessage() {
    const membership = selectedMembership.value
    const ordinal = Number(form.ordinal)
    const name = String(form.name ?? '').trim()
    const description = String(form.description ?? '').trim()

    if (!membership) {
      return 'Выберите предмет преподавателя.'
    }

    if (!Number.isInteger(ordinal) || ordinal <= 0) {
      return 'Порядковый номер темы должен быть целым числом больше нуля.'
    }

    if (!name) {
      return 'Введите название темы.'
    }

    if (name.length > 200) {
      return 'Название темы не может быть длиннее 200 символов.'
    }

    if (description.length > 2000) {
      return 'Описание темы не может быть длиннее 2000 символов.'
    }

    const duplicateOrdinal = topics.value.find(
      (topic) =>
        Number(topic.ordinal) === ordinal &&
        String(topic.id) !== String(form.id ?? '')
    )

    if (duplicateOrdinal) {
      return 'Тема с таким порядковым номером уже существует в выбранном назначении преподавателя.'
    }

    return ''
  }

  async function saveTopic() {
    if (saving?.value) {
      return false
    }

    formFieldErrors.value = {}
    formError.value = topicFormValidationMessage()

    if (formError.value) {
      return
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
          ? 'Проверьте выделенные поля.'
          : presentation.message

      failSaving()
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
