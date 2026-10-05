import { ref } from 'vue'

import {
  coursesApi,
  getApiErrorMessage,
} from '@/api'

export function useCourseTemplateMutations({
  selectedSubjectId,
  selectedTemplateId,
  versions,
  templateOverlay,
  versionOverlay,
  templateFormError,
  versionFormError,
  templateCreationAllowed,
  templateValidationMessage,
  versionValidationMessage,
  ensureSelectedMembershipActive,
  loadTemplates,
  loadVersions,
  closeTemplateDrawerImmediately,
  closeVersionDrawerImmediately,
  notice,
}) {
  const publishingVersionId = ref(null)

  const deleteTarget = ref(null)
  const deleteConfirmVisible = ref(false)
  const deletingTemplateId = ref(null)
  const deleteError = ref('')

  function requestDeleteTemplate(template) {
    if (!template || deletingTemplateId.value !== null) {
      return
    }

    deleteTarget.value = template
    deleteError.value = ''
    deleteConfirmVisible.value = true
  }

  function closeDeleteDialog() {
    if (deletingTemplateId.value !== null) {
      return
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    deleteError.value = ''
  }

  async function saveTemplate() {
    if (templateOverlay.saving.value) {
      return false
    }

    templateFormError.value = templateValidationMessage()

    if (templateFormError.value) {
      return
    }

    if (
      !templateOverlay.form.id &&
      !templateCreationAllowed.value
    ) {
      templateFormError.value = 'Новый шаблон должен создать сам преподаватель.'
      return
    }

    const payload = {
      subjectId: Number(selectedSubjectId.value),
      name: String(templateOverlay.form.name).trim(),
      publicVisible: Boolean(templateOverlay.form.publicVisible),
    }

    const editingId = templateOverlay.form.id

    templateOverlay.beginSaving()

    try {
      await ensureSelectedMembershipActive()

      const response = editingId
        ? await coursesApi.updateTemplate(editingId, payload)
        : await coursesApi.createTemplate(payload)

      const savedId = response?.data?.id ?? editingId ?? null

      notice.value = {
        type: 'success',
        message: editingId ? 'Шаблон обновлён.' : 'Шаблон создан.',
      }

      templateOverlay.finishSaving({ close: true })
      templateFormError.value = ''
      await loadTemplates({ preferredTemplateId: savedId })
    } catch (error) {
      templateFormError.value = getApiErrorMessage(
        error,
        editingId
          ? 'Не удалось обновить шаблон'
          : 'Не удалось создать шаблон'
      )
      templateOverlay.failSaving()
    }
  }

  async function deleteTemplate() {
    const template = deleteTarget.value

    if (!template || deletingTemplateId.value !== null) {
      return
    }

    deletingTemplateId.value = template.id
    deleteError.value = ''

    try {
      await ensureSelectedMembershipActive()
      await coursesApi.removeTemplate(template.id)

      if (Number(templateOverlay.form.id) === Number(template.id)) {
        closeTemplateDrawerImmediately()
      }

      if (Number(selectedTemplateId.value) === Number(template.id)) {
        selectedTemplateId.value = null
        versions.value = []
        closeVersionDrawerImmediately()
      }

      notice.value = {
        type: 'success',
        message: 'Шаблон удалён.',
      }

      deleteConfirmVisible.value = false
      deleteTarget.value = null
      await loadTemplates()
    } catch (error) {
      deleteError.value = getApiErrorMessage(
        error,
        'Не удалось удалить шаблон'
      )
    } finally {
      deletingTemplateId.value = null
    }
  }

  async function saveVersion() {
    if (versionOverlay.saving.value) {
      return false
    }

    versionFormError.value = versionValidationMessage()

    if (versionFormError.value) {
      return
    }

    const editingId = versionOverlay.form.id
    const targetTemplateId = Number(selectedTemplateId.value)
    const requestedPublished = Boolean(versionOverlay.form.published)

    const basePayload = {
      versionNumber: Number(versionOverlay.form.versionNumber),
      title: String(versionOverlay.form.title).trim(),
      description: String(versionOverlay.form.description ?? '').trim() || null,
      changeNotes: String(versionOverlay.form.changeNotes ?? '').trim() || null,
    }

    versionOverlay.beginSaving()

    try {
      await ensureSelectedMembershipActive()

      if (editingId) {
        await coursesApi.updateVersion(editingId, basePayload)
      } else {
        await coursesApi.createVersion(
          targetTemplateId,
          {
            ...basePayload,
            published: requestedPublished,
          }
        )
      }

      if (Number(selectedTemplateId.value) !== targetTemplateId) {
        return
      }

      notice.value = {
        type: 'success',
        message: editingId ? 'Версия обновлена.' : 'Версия создана.',
      }

      versionOverlay.finishSaving({ close: true })
      versionFormError.value = ''
      await loadVersions()
    } catch (error) {
      if (Number(selectedTemplateId.value) !== targetTemplateId) {
        return
      }

      versionFormError.value = getApiErrorMessage(
        error,
        editingId
          ? 'Не удалось обновить версию'
          : 'Не удалось создать версию'
      )
      versionOverlay.failSaving()
    }
  }

  async function publishVersion(version) {
    if (publishingVersionId.value !== null) {
      return false
    }

    const targetTemplateId = Number(selectedTemplateId.value)
    const targetVersionId = Number(version.id)
    const wasPublished = Boolean(version.published)

    publishingVersionId.value = targetVersionId

    try {
      await ensureSelectedMembershipActive()

      if (wasPublished) {
        await coursesApi.unpublishVersion(targetVersionId)
      } else {
        await coursesApi.publishVersion(targetVersionId)
      }

      if (Number(selectedTemplateId.value) !== targetTemplateId) {
        return
      }

      notice.value = {
        type: 'success',
        message: wasPublished
          ? 'Публикация версии снята.'
          : 'Версия опубликована.',
      }

      await loadVersions()
    } catch (error) {
      if (Number(selectedTemplateId.value) === targetTemplateId) {
        notice.value = {
          type: 'danger',
          message: getApiErrorMessage(
            error,
            'Не удалось изменить публикацию версии'
          ),
        }
      }
    } finally {
      if (publishingVersionId.value === targetVersionId) {
        publishingVersionId.value = null
      }
    }
  }

  return {
    publishingVersionId,
    deleteTarget,
    deleteConfirmVisible,
    deletingTemplateId,
    deleteError,
    requestDeleteTemplate,
    closeDeleteDialog,
    saveTemplate,
    deleteTemplate,
    saveVersion,
    publishVersion,
  }
}
