import { computed, getCurrentScope, onScopeDispose, ref } from 'vue'

import {
  getApiErrorMessage,
  questionsApi,
} from '@/api'

import { listFromResponse } from '@/utils/apiData'
import { createAbortableRequestGuard } from '@/utils/latestRequest'

function normalizedOptionDraft(value) {
  return JSON.stringify({
    id: value?.id ?? null,
    text: String(value?.text ?? ''),
    ordinal: Number(value?.ordinal ?? 1),
    correct: Boolean(value?.correct),
  })
}

export function useQuestionOptions({
  form,
  isSelectableType,
  ensureSelectedMembershipActive,
  notice,
  formError,
  api = questionsApi,
} = {}) {
  const options = ref([])
  const loadingOptions = ref(false)
  const savingOption = ref(false)
  const optionForm = ref({
    id: null,
    text: '',
    ordinal: 1,
    correct: false,
  })
  const optionBaseline = ref('')
  const request = createAbortableRequestGuard()

  const optionDraftDirty = computed(() => {
    return normalizedOptionDraft(optionForm.value) !== optionBaseline.value
  })

  function resetOptionForm() {
    const maxOrdinal = options.value.reduce(
      (max, option) => Math.max(max, Number(option.ordinal ?? 0)),
      0
    )

    optionForm.value = {
      id: null,
      text: '',
      ordinal: maxOrdinal + 1,
      correct: false,
    }
    optionBaseline.value = normalizedOptionDraft(optionForm.value)
  }

  function editOption(option) {
    optionForm.value = {
      id: option.id,
      text: option.text || '',
      ordinal: Number(option.ordinal ?? 1),
      correct: Boolean(option.correct),
    }
    optionBaseline.value = normalizedOptionDraft(optionForm.value)
  }

  function clearOptions() {
    request.invalidate()
    options.value = []
    loadingOptions.value = false
    resetOptionForm()
  }

  async function loadOptions(questionId) {
    const { requestId, signal } = request.begin()

    options.value = []
    resetOptionForm()

    if (!questionId || !isSelectableType?.value) {
      loadingOptions.value = false
      return
    }

    loadingOptions.value = true

    try {
      const response = await api.getOptions(questionId, { signal })

      if (!request.isCurrent(requestId)) {
        return
      }

      options.value = listFromResponse(response).sort(
        (left, right) =>
          Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
      )

      resetOptionForm()
    } catch (error) {
      if (!request.isCurrent(requestId)) {
        return
      }

      if (formError) {
        formError.value = getApiErrorMessage(
          error,
          'Не удалось загрузить варианты ответа'
        )
      }
    } finally {
      if (request.isCurrent(requestId)) {
        loadingOptions.value = false
      }
    }
  }

  async function saveOption() {
    const text = String(optionForm.value.text ?? '').trim()

    if (!form?.id || !isSelectableType?.value || !text) {
      if (formError) {
        formError.value =
          'Сохраните вопрос и заполните текст варианта ответа.'
      }
      return
    }

    const ordinal = Number(optionForm.value.ordinal)

    if (!Number.isInteger(ordinal) || ordinal <= 0) {
      if (formError) {
        formError.value =
          'Порядок варианта должен быть целым числом больше нуля.'
      }
      return
    }

    const payload = {
      text,
      ordinal,
      correct: Boolean(optionForm.value.correct),
    }

    savingOption.value = true
    if (formError) formError.value = ''

    try {
      await ensureSelectedMembershipActive()

      if (optionForm.value.id) {
        await api.updateOption(optionForm.value.id, payload)
        if (notice) {
          notice.value = {
            type: 'success',
            message: 'Вариант ответа обновлён.',
          }
        }
      } else {
        await api.createOption(form.id, payload)
        if (notice) {
          notice.value = {
            type: 'success',
            message: 'Вариант ответа создан.',
          }
        }
      }

      await loadOptions(form.id)
    } catch (error) {
      if (formError) {
        formError.value = getApiErrorMessage(
          error,
          'Не удалось сохранить вариант ответа'
        )
      }
    } finally {
      savingOption.value = false
    }
  }

  if (getCurrentScope()) {
    onScopeDispose(() => {
      request.invalidate()
    })
  }

  return {
    options,
    loadingOptions,
    savingOption,
    optionForm,
    optionDraftDirty,
    resetOptionForm,
    editOption,
    clearOptions,
    loadOptions,
    saveOption,
  }
}
