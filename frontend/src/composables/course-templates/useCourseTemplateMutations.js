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
    versionFormError.value = versionValidationMessage()

    if (versionFormError.value) {
      return
    }

    const editingId = versionOverlay.form.id

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
          selectedTemplateId.value,
          {
            ...basePayload,
            published: Boolean(versionOverlay.form.published),
          }
        )
      }

      notice.value = {
        type: 'success',
        message: editingId ? 'Версия обновлена.' : 'Версия создана.',
      }

      versionOverlay.finishSaving({ close: true })
      versionFormError.value = ''
      await loadVersions()
    } catch (error) {
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
    publishingVersionId.value = version.id

    try {
      await ensureSelectedMembershipActive()

      if (version.published) {
        await coursesApi.unpublishVersion(version.id)

        notice.value = {
          type: 'success',
          message: 'Публикация версии снята.',
        }
      } else {
        await coursesApi.publishVersion(version.id)

        notice.value = {
          type: 'success',
          message: 'Версия опубликована.',
        }
      }

      await loadVersions()
    } catch (error) {
      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось изменить публикацию версии'
        ),
      }
    } finally {
      publishingVersionId.value = null
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
