import { computed } from 'vue'

import { useOverlayForm } from '@/composables/shared/useOverlayForm'

import {
  ensureMatchingPairRows,
  matchingPairsValidationMessage,
} from '@/utils/matchingPairs'

export function questionToForm(question) {
  return {
    id: question?.id ?? null,
    question: question?.question ?? '',
    type: Number(question?.type ?? 1),
    points: Number(question?.points ?? 1),
    ordinal: Number(question?.ordinal ?? 1),
    correctAnswer: question?.correctAnswer ?? '',
    matchingPairs: ensureMatchingPairRows(
      question?.matchingPairs,
      Number(question?.type ?? 1) === 3 ? 2 : 0
    ),
    active: question?.active !== false,
  }
}

export function useQuestionEditorState({
  selectedMembership,
  selectedTopicId,
} = {}) {
  const overlay = useOverlayForm({
    createDefault: () => questionToForm(),
    mapEntity: questionToForm,
  })

  const currentQuestionType = computed(() => {
    return Number(overlay.form.type ?? 1)
  })

  const isSelectableType = computed(() => {
    return currentQuestionType.value === 1 || currentQuestionType.value === 2
  })

  const isMatchingType = computed(() => currentQuestionType.value === 3)
  const isTextType = computed(() => currentQuestionType.value === 4)

  const drawerTitle = computed(() => {
    return overlay.isCreate.value
      ? 'Новый вопрос'
      : 'Редактирование вопроса'
  })

  function validation() {
    if (!selectedMembership?.value || !selectedTopicId?.value) {
      return { field: null, message: 'Выберите предмет и тему.' }
    }

    const question = String(overlay.form.question ?? '').trim()
    const points = Number(overlay.form.points)
    const ordinal = Number(overlay.form.ordinal)

    if (!question) {
      return { field: 'question', message: 'Введите текст вопроса.' }
    }

    if (question.length > 2000) {
      return { field: 'question', message: 'Текст вопроса не может быть длиннее 2000 символов.' }
    }

    if (!Number.isFinite(points) || points < 0) {
      return { field: 'points', message: 'Количество баллов должно быть числом не меньше нуля.' }
    }

    if (!Number.isInteger(ordinal) || ordinal <= 0) {
      return { field: 'ordinal', message: 'Порядковый номер должен быть целым числом больше нуля.' }
    }

    if (isMatchingType.value) {
      const error = matchingPairsValidationMessage(
        overlay.form.matchingPairs
      )

      if (error) {
        return { field: 'matchingPairs', message: error }
      }
    }

    return null
  }

  function validationMessage() {
    return validation()?.message ?? ''
  }

  return {
    ...overlay,
    currentQuestionType,
    isSelectableType,
    isMatchingType,
    isTextType,
    drawerTitle,
    validation,
    validationMessage,
  }
}
