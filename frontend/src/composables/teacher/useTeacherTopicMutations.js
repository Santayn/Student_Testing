import { ref } from 'vue'

import {
  getApiErrorMessage,
  topicsApi,
} from '@/api'

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
      formError.value = getApiErrorMessage(
        error,
        form.id
          ? 'Не удалось обновить тему.'
          : 'Не удалось создать тему.'
      )
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
      deleteError.value = getApiErrorMessage(
        error,
        'Не удалось удалить тему.'
      )
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
    requestDeleteTopic,
    closeDeleteDialog,
    topicFormValidationMessage,
    saveTopic,
    deleteTopic,
  }
}
