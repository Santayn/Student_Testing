<script setup>
import { UiLoadingState, UiEmptyState, UiButton } from '@/components/ui'
import { toRefs } from 'vue'
import { useQuestionsWorkspaceContext } from '@/composables/questions/questionsWorkspaceContext'
const {
  loading,
  selectedMembership,
  selectedTopicId,
  questions,
  filteredQuestions,
  resetFilters,
  questionTypeLabel,
  questionMatchingPairs,
  questionDisplayAnswer,
  editQuestion,
  toggleQuestionActive,
  togglingQuestionId,
} = toRefs(useQuestionsWorkspaceContext())
</script>
<template>
        <UiLoadingState
          v-if="loading"
          compact
          label="Загрузка вопросов..."
        />

        <UiEmptyState
          v-else-if="!selectedMembership"
          description="Выберите предмет преподавателя."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedTopicId"
          description="Выберите тему, чтобы открыть банк вопросов."
          compact
        />

        <UiEmptyState
          v-else-if="!questions.length"
          description="В выбранной теме пока нет вопросов. Добавьте первый вопрос кнопкой выше или импортируйте .docx."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredQuestions.length"
          description="По текущему поиску и фильтрам вопросы не найдены."
          compact
        >
          <template #actions>
            <UiButton
              variant="secondary"
              size="sm"
              label="Сбросить фильтры"
              @click="resetFilters"
            />
          </template>
        </UiEmptyState>

        <div
          v-else
          class="teacher-entity-list"
        >
          <article
            v-for="question in filteredQuestions"
            :key="question.id"
            class="teacher-entity-card"
          >
            <div class="teacher-entity-card__header">
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  {{ question.ordinal }} · {{ questionTypeLabel(question.type) }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ question.question }}
                </h3>
              </div>

              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success': question.active,
                }"
              >
                {{ question.active ? 'Активен' : 'Скрыт' }}
              </span>
            </div>

            <div
              v-if="Number(question.type) === 3 && questionMatchingPairs(question).length"
              class="teacher-matching-preview"
            >
              <div
                v-for="pair in questionMatchingPairs(question)"
                :key="`${question.id}-${pair.ordinal}`"
                class="teacher-matching-preview__pair"
              >
                <span class="teacher-matching-preview__number">{{ pair.ordinal }}</span>
                <span>{{ pair.left }}</span>
                <i class="pi pi-arrow-right" aria-hidden="true" />
                <strong>{{ pair.right }}</strong>
              </div>
            </div>

            <p
              v-else-if="questionDisplayAnswer(question)"
              class="teacher-entity-card__description teacher-question-answer-preview"
            >
              {{ questionDisplayAnswer(question) }}
            </p>

            <div class="teacher-entity-card__meta">
              <span>Баллы: {{ question.points }}</span>
            </div>

            <div class="teacher-entity-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="editQuestion(question)"
              />

              <UiButton
                size="sm"
                :loading="Number(togglingQuestionId) === Number(question.id)"
                :loading-text="question.active ? 'Скрытие...' : 'Активация...'"
                @click="toggleQuestionActive(question)"
              >
                {{ question.active ? 'Скрыть' : 'Активировать' }}
              </UiButton>
            </div>
          </article>
        </div>
</template>
<style scoped>
.teacher-question-answer-preview {
  padding: 10px 12px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
  white-space: pre-wrap;
}

.teacher-matching-preview {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.teacher-matching-preview__pair {
  min-width: 0;
  padding: 8px 10px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-matching-preview__pair > span,
.teacher-matching-preview__pair > strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.teacher-matching-preview__pair > i {
  color: var(--st-text-secondary);
}

.teacher-matching-preview__number {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 7px;
  font-size: 11px;
  font-weight: var(--st-font-weight-bold);
}


@media (max-width: 640px) {
  .teacher-matching-preview__pair { grid-template-columns: auto minmax(0, 1fr); }
  .teacher-matching-preview__pair > i { grid-column: 2; transform: rotate(90deg); }
  .teacher-matching-preview__pair > strong { grid-column: 2; }
}

</style>
