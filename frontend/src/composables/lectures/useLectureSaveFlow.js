import { computed, ref } from 'vue'
import { listFromResponse } from '@/utils/apiData'
import {
  FORM_FIELD_ERROR_SUMMARY,
  setFormFieldError,
} from '@/utils/formErrorLifecycle'

/**
 * Saves a lecture core first, then its test links and pending materials.
 * The server id becomes part of the editor immediately after create succeeds:
 * a dependent failure must never turn the next Save into a second POST.
 */
export function useLectureSaveFlow({
  form,
  formError,
  formFieldErrors,
  focusFormErrors = null,
  lectureFormMode,
  selectedSubject,
  selectedMembership,
  ensureSelectedMembershipActive,
  lectures,
  lectureTestsById,
  pendingFiles,
  fileInputKey,
  notice,
  beginSaving,
  saving,
  finishSaving,
  failSaving,
  loadLectures,
  clearLectureDrawerState,
  lecturesApi,
  getApiErrorMessage,
}) {
  const partialCreateState = ref(null)

  function structuredFieldErrors(error) {
    const details =
      error?.response?.data?.details

    if (!details) {
      return {}
    }

    if (
      details &&
      typeof details === 'object' &&
      !Array.isArray(details)
    ) {
      const result = {}

      for (const [field, value] of Object.entries(details)) {
        const messages = Array.isArray(value)
          ? value
          : value == null
            ? []
            : [value]

        const normalized = messages
          .map((item) => String(item ?? '').trim())
          .filter(Boolean)

        if (normalized.length) {
          result[field] = normalized
        }
      }

      return result
    }

    return {}
  }

  const partialCreatePending = computed(() => Boolean(
    partialCreateState.value &&
    Number(partialCreateState.value.id) === Number(form.id)
  ))

  function resetPartialCreate() {
    partialCreateState.value = null
  }

  function lectureValidation() {
    if (!selectedMembership.value || !selectedSubject.value) {
      return { field: null, message: 'Выберите предмет преподавателя.' }
    }

    const title = String(form.title ?? '').trim()
    const description = String(form.description ?? '').trim()

    if (!title) {
      return { field: 'title', message: 'Введите название лекции.' }
    }

    if (title.length > 200) {
      return { field: 'title', message: 'Название лекции не может быть длиннее 200 символов.' }
    }

    if (description.length > 2000) {
      return { field: 'description', message: 'Описание лекции не может быть длиннее 2000 символов.' }
    }

    return null
  }

  function nextOrdinal() {
    return lectures.value.reduce(
      (max, lecture) => Math.max(max, Number(lecture.ordinal ?? 0)),
      0
    ) + 1
  }

  function slugifyLectureTitle(value) {
    return String(value || '')
      .trim()
      .toLowerCase()
      .replace(/[^a-zа-яё0-9]+/gi, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 80) || 'lecture'
  }

  function buildLectureContentKey(title, lectureId = null) {
    const suffix = lectureId || Date.now()
    return `lecture-${suffix}-${slugifyLectureTitle(title)}`
  }

  function lectureCoreSignature(payload) {
    return JSON.stringify(payload)
  }

  function upsertLecture(lecture) {
    const lectureId = Number(lecture?.id ?? 0)

    if (!lectureId) {
      return
    }

    const next = lectures.value.filter(
      (item) => Number(item.id) !== lectureId
    )

    next.push(lecture)
    next.sort(
      (left, right) =>
        Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0) ||
        String(left.title ?? '').localeCompare(
          String(right.title ?? ''),
          'ru'
        )
    )

    lectures.value = next
  }

  async function syncLectureTests(lectureId, selectedTestIds) {
    const response = await lecturesApi.setTests(
      lectureId,
      {
        testIds: [
          ...new Set(
            selectedTestIds
              .map(Number)
              .filter(Boolean)
          ),
        ],
      }
    )

    const next = new Map(lectureTestsById.value)
    next.set(Number(lectureId), listFromResponse(response))
    lectureTestsById.value = next
  }

  async function uploadPendingFiles(lectureId, files) {
    const filesToUpload = Array.isArray(files)
      ? [...files]
      : []

    if (!filesToUpload.length) {
      return
    }

    await lecturesApi.uploadMaterials(
      lectureId,
      filesToUpload
    )

    pendingFiles.value = pendingFiles.value.filter(
      (file) => !filesToUpload.includes(file)
    )
    fileInputKey.value += 1
  }

  async function saveLecture() {
    if (saving?.value) {
      return false
    }

    formFieldErrors.value = {}
    formError.value = ''

    const validation =
      lectureValidation()

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
        formError.value = validation.message
      }

      return false
    }

    const editingId = form.id ? Number(form.id) : null
    const selectedTestIds = Array.isArray(form.testIds)
      ? [...form.testIds]
      : []
    const filesToUpload = Array.isArray(pendingFiles.value)
      ? [...pendingFiles.value]
      : []

    const editingLecture = lectures.value.find(
      (item) => Number(item.id) === editingId
    ) ?? null

    const payload = {
      subjectId: Number(selectedSubject.value.id),
      subjectMembershipId: Number(selectedMembership.value.id),
      courseVersionId: null,
      ordinal: editingLecture?.ordinal || nextOrdinal(),
      title: String(form.title).trim(),
      description: String(form.description ?? '').trim() || null,
      contentFolderKey:
        editingLecture?.contentFolderKey ||
        buildLectureContentKey(
          form.title,
          editingLecture?.id || null
        ),
      linkedTestId: null,
      publicVisible: Boolean(form.publicVisible),
    }

    const payloadSignature = lectureCoreSignature(payload)
    const retryingPartialCreate = partialCreatePending.value

    beginSaving()

    try {
      // This check must stay on the mutation path; read-only membership
      // catalogs cannot stand in for the current authorization state.
      await ensureSelectedMembershipActive()

      let lecture

      if (
        retryingPartialCreate &&
        partialCreateState.value.coreSignature === payloadSignature
      ) {
        lecture = editingLecture ?? {
          ...payload,
          id: partialCreateState.value.id,
        }
      } else if (editingId) {
        const response = await lecturesApi.update(editingId, payload)
        lecture = response.data
        upsertLecture(lecture)

        if (retryingPartialCreate) {
          partialCreateState.value = {
            ...partialCreateState.value,
            coreSignature: payloadSignature,
          }
        }
      } else {
        const response = await lecturesApi.create(payload)
        lecture = response.data

        // Persist the new id BEFORE any dependent request. On failure,
        // retry will continue in edit mode, not create another lecture.
        form.id = lecture.id
        lectureFormMode.value = 'edit'
        upsertLecture(lecture)
        partialCreateState.value = {
          id: Number(lecture.id),
          coreSignature: payloadSignature,
        }
      }

      await syncLectureTests(lecture.id, selectedTestIds)
      await uploadPendingFiles(lecture.id, filesToUpload)

      const completedCreateFlow = partialCreatePending.value
      resetPartialCreate()

      notice.value = {
        type: 'success',
        message: completedCreateFlow
          ? 'Лекция создана.'
          : 'Лекция обновлена.',
      }

      finishSaving({ close: true })
      clearLectureDrawerState()
      await loadLectures()
    } catch (error) {
      const createdLectureNeedsFollowUp =
        partialCreatePending.value

      const fallback =
        createdLectureNeedsFollowUp
          ? 'Лекция уже создана, но не удалось сохранить связанные тесты или материалы. Повторите сохранение — новая лекция создана повторно не будет.'
          : editingId
            ? 'Не удалось обновить лекцию'
            : 'Не удалось создать лекцию'

      const fieldErrors =
        structuredFieldErrors(error)

      if (
        Object.keys(fieldErrors).length
      ) {
        formFieldErrors.value =
          fieldErrors

        formError.value =
          FORM_FIELD_ERROR_SUMMARY

        failSaving()
        await focusFormErrors?.()
        return
      }

      formFieldErrors.value = {}

      /*
       * Partial-create and authorization failures intentionally keep the
       * workflow-specific fallback as the primary message. Raw transport /
       * service messages such as "links unavailable" or "revoked" must not
       * hide the recovery instruction or mutation context.
       */
      formError.value =
        createdLectureNeedsFollowUp
          ? fallback
          : getApiErrorMessage(
              {
                ...error,
                message: '',
              },
              fallback
            )

      failSaving()
    }
  }

  return {
    partialCreatePending,
    resetPartialCreate,
    saveLecture,
  }
}
