import { ref } from 'vue'

import {
  getApiErrorMessage,
  questionsApi,
} from '@/api'

import {
  questionToForm,
} from '@/composables/questions/useQuestionEditorState'

import {
  normalizeMatchingPairs,
} from '@/utils/matchingPairs'

export function useQuestionMutations({
  form,
  formError,
  selectedTopicId,
  questions,
  notice,
  questionValidationMessage,
  beginSaving,
  saving,
  finishSaving,
  failSaving,
  ensureSelectedMembershipActive,
  loadQuestions,
  openEdit,
  loadOptions,
  clearOptions,
  api = questionsApi,
} = {}) {
  const togglingQuestionId = ref(null)

  async function saveQuestion() {
    if (saving?.value) {
      return false
    }

    const wasCreate = !form.id

    formError.value = questionValidationMessage()

    if (formError.value) {
      return
    }

    const type = Number(form.type)
    const basePayload = {
      courseLectureId: null,
      topicId: Number(selectedTopicId.value),
      type,
      question: String(form.question).trim(),
      points: Number(form.points) || 0,
      ordinal: Number(form.ordinal) || 1,
      correctAnswer:
        type === 4
          ? String(form.correctAnswer ?? '').trim() || null
          : null,
      matchingPairs:
        type === 3
          ? normalizeMatchingPairs(form.matchingPairs)
          : [],
    }

    beginSaving()

    try {
      await ensureSelectedMembershipActive()

      let response

      if (form.id) {
        response = await api.update(
          form.id,
          {
            ...basePayload,
            active: Boolean(form.active),
          }
        )

        notice.value = {
          type: 'success',
          message: 'Вопрос обновлён.',
        }
      } else {
        response = await api.create({
          testId: null,
          ...basePayload,
        })

        notice.value = {
          type: 'success',
          message:
            'Вопрос создан. Теперь можно добавить варианты ответа, если они нужны.',
        }
      }

      const savedId = Number(
        response.data?.id ?? form.id ?? 0
      )

      await loadQuestions()

      const saved =
        questions.value.find(
          (item) => Number(item.id) === savedId
        ) ?? response.data

      if (saved) {
        if (wasCreate) {
          openEdit(saved)
        } else {
          finishSaving({
            close: false,
            values: questionToForm(saved),
          })
        }

        if (
          Number(saved.type) === 1 ||
          Number(saved.type) === 2
        ) {
          await loadOptions(saved.id)
        } else {
          clearOptions()
        }
      } else {
        finishSaving({ close: true })
        clearOptions()
      }
    } catch (error) {
      formError.value = getApiErrorMessage(
        error,
        form.id
          ? 'Не удалось обновить вопрос'
          : 'Не удалось создать вопрос'
      )
      failSaving()
    }
  }

  async function toggleQuestionActive(question) {
    if (togglingQuestionId.value !== null) {
      return
    }

    togglingQuestionId.value = question.id

    try {
      await ensureSelectedMembershipActive()

      await api.updateActive(
        question.id,
        {
          active: !question.active,
        }
      )

      await loadQuestions()

      notice.value = {
        type: 'success',
        message:
          question.active
            ? 'Вопрос скрыт.'
            : 'Вопрос активирован.',
      }
    } catch (error) {
      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось изменить статус вопроса'
        ),
      }
    } finally {
      togglingQuestionId.value = null
    }
  }

  return {
    togglingQuestionId,
    saveQuestion,
    toggleQuestionActive,
  }
}
