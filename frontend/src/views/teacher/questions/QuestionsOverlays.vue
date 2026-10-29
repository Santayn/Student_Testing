<script setup>
import { UiDrawer, UiDialog, UiButton, UiAlert, UiCheckbox, UiEmptyState,
  UiInput, UiSelect, UiTextarea, UiFileInput, UiLoadingState,
  UiUnsavedChangesConfirm } from '@/components/ui'
import GradingCriteriaField from '@/components/questions/GradingCriteriaField.vue'
import MatchingPairsEditor from '@/components/questions/MatchingPairsEditor.vue'
import { apiFieldError } from '@/utils/apiErrorPresentation'
import { clearFormFieldError } from '@/utils/formErrorLifecycle'
import { toRefs } from 'vue'
import { useQuestionsWorkspaceContext } from '@/composables/questions/questionsWorkspaceContext'
const {
  questionDrawerOpen,
  drawerTitle,
  handleQuestionDrawerVisibility,
  setQuestionFormElement,
  form,
  formError,
  formFieldErrors,
  isCreate,
  questionTypeOptions,
  handleQuestionTypeChange,
  isTextType,
  isMatchingType,
  isSelectableType,
  savingQuestion,
  savingOption,
  options,
  loadingOptions,
  optionForm,
  editOption,
  saveOption,
  resetOptionForm,
  requestQuestionDrawerClose,
  saveQuestion,
  confirmCloseVisible,
  continueEditing,
  discardQuestionDrawer,
  importDialogVisible,
  importing,
  importError,
  selectedImportTopicId,
  topicOptions,
  fileInputKey,
  onWordFiles,
  wordFiles,
  closeImportDialog,
  importWord,
} = toRefs(useQuestionsWorkspaceContext())
</script>
<template>
<UiDrawer
      :model-value="questionDrawerOpen"
      :title="drawerTitle"
      width="46rem"
      @update:model-value="handleQuestionDrawerVisibility"
    >
      <div
        :ref="setQuestionFormElement"
        class="teacher-stack teacher-question-drawer"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
          closable
          @close="formError = ''"
        />

        <section class="teacher-question-form-section">
          <div class="teacher-question-form-section__heading">
            <span class="teacher-muted">Вопрос</span>
            <strong>
              {{ isCreate ? 'Новая запись банка' : `ID ${form.id}` }}
            </strong>
          </div>

          <UiTextarea
            v-model="form.question"
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'question')"
          :error="apiFieldError({ fieldErrors: formFieldErrors }, 'question')"
            label="Текст вопроса"
            maxlength="2000"
            required
          />

          <div class="teacher-grid--3 teacher-grid">
            <UiSelect
              v-model="form.type"
              :error="apiFieldError({ fieldErrors: formFieldErrors }, 'type')"
              label="Тип"
              :options="questionTypeOptions"
              @update:model-value="(value) => {
                clearFormFieldError(formFieldErrors, formError, 'type')
                handleQuestionTypeChange(value)
              }"
            />

            <UiInput
              v-model="form.points"
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'points')"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'points')"
              label="Баллы"
              type="number"
              min="0"
              step="0.01"
            />

            <UiInput
              v-model="form.ordinal"
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'ordinal')"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'ordinal')"
              label="Порядок"
              type="number"
              min="1"
              step="1"
            />
          </div>

          <UiInput
            v-if="isTextType"
            v-model="form.correctAnswer"
          @update:model-value="clearFormFieldError(formFieldErrors, formError, 'correctAnswer')"
          :error="apiFieldError({ fieldErrors: formFieldErrors }, 'correctAnswer')"
            label="Правильный ответ"
            hint="Можно указать несколько допустимых вариантов через символ | или с новой строки."
            maxlength="2000"
          />

          <GradingCriteriaField
            v-if="isTextType"
            v-model="form.gradingCriteria"
            :question="form.question"
            :correct-answer="form.correctAnswer"
            :points="form.points"
            :disabled="savingQuestion"
            :error="apiFieldError({ fieldErrors: formFieldErrors }, 'gradingCriteria')"
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'gradingCriteria')"
          />

          <MatchingPairsEditor
            v-if="isMatchingType"
            v-model="form.matchingPairs"
            :disabled="savingQuestion"
            @update:model-value="clearFormFieldError(formFieldErrors, formError, 'matchingPairs')"
          />

          <UiCheckbox
            v-if="!isCreate"
            v-model="form.active"
            label="Вопрос активен"
          />
        </section>

        <section
          v-if="isSelectableType"
          class="teacher-question-form-section"
        >
          <div class="teacher-question-form-section__heading">
            <span class="teacher-muted">Варианты ответа</span>
            <strong>
              {{ form.id ? `${options.length} вариантов` : 'После сохранения вопроса' }}
            </strong>
          </div>

          <UiEmptyState
            v-if="!form.id"
            description="Сначала сохраните вопрос. После получения ID управление вариантами станет доступно в этой же панели."
            compact
          />

          <template v-else>
            <UiLoadingState
              v-if="loadingOptions"
              compact
              label="Загрузка вариантов..."
            />

            <UiEmptyState
              v-else-if="!options.length"
              description="У вопроса пока нет вариантов ответа. Добавьте первый вариант ниже."
              compact
            />

            <div
              v-else
              class="teacher-choice-list"
            >
              <div
                v-for="option in options"
                :key="option.id"
                class="teacher-choice-row"
              >
                <div class="teacher-choice-row__copy">
                  <strong class="teacher-choice-row__title">
                    {{ option.ordinal }}. {{ option.text }}
                  </strong>
                  <span class="teacher-choice-row__meta">
                    {{ option.correct ? 'Правильный вариант' : 'Обычный вариант' }}
                  </span>
                </div>

                <UiButton
                  size="sm"
                  icon="pi pi-pencil"
                  label="Изменить"
                  @click="editOption(option)"
                />
              </div>
            </div>

            <div class="teacher-divider" />

            <UiInput
              v-model="optionForm.text"
              label="Текст варианта"
              maxlength="2000"
            />

            <div class="teacher-grid">
              <UiInput
                v-model="optionForm.ordinal"
                label="Порядок"
                type="number"
                min="1"
                step="1"
              />

              <UiCheckbox
                v-model="optionForm.correct"
                label="Правильный вариант"
              />
            </div>

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="savingOption"
                loading-text="Сохранение..."
                @click="saveOption"
              >
                {{ optionForm.id ? 'Сохранить вариант' : 'Добавить вариант' }}
              </UiButton>

              <UiButton
                :disabled="savingOption"
                @click="resetOptionForm"
              >
                Очистить вариант
              </UiButton>
            </div>
          </template>
        </section>
      </div>

      <template #footer>
        <div class="teacher-question-drawer__footer">
          <UiButton
            variant="secondary"
            label="Закрыть"
            :disabled="savingQuestion || savingOption"
            @click="requestQuestionDrawerClose"
          />

          <UiButton
            variant="primary"
            :loading="savingQuestion"
            loading-text="Сохранение..."
            :label="form.id ? 'Сохранить вопрос' : 'Создать вопрос'"
            :disabled="savingOption"
            @click="saveQuestion"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="savingQuestion || savingOption"
      @continue="continueEditing"
      @discard="discardQuestionDrawer"
    />

    <UiDialog
      v-model="importDialogVisible"
      title="Импорт вопросов из Word"
      width="34rem"
      :close-on-escape="!importing"
      :closable="!importing"
    >
      <div class="teacher-stack">
        <p class="teacher-muted teacher-question-dialog-copy">
          Выберите тему и .docx. Импорт не меняет текущие фильтры банка вопросов.
        </p>

        <UiAlert
          v-if="importError"
          variant="danger"
          :message="importError"
        />

        <UiSelect
          v-model="selectedImportTopicId"
          label="Тема для импорта"
          :options="topicOptions"
          placeholder="Выберите тему"
          :disabled="importing || !topicOptions.length"
        />

        <UiFileInput
          :key="fileInputKey"
          label="Файл .docx"
          accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          :disabled="importing || !selectedImportTopicId"
          @files-change="onWordFiles"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="importing"
            @click="closeImportDialog"
          />

          <UiButton
            variant="primary"
            :loading="importing"
            loading-text="Импорт..."
            label="Импортировать"
            :disabled="!selectedImportTopicId || !wordFiles.length"
            @click="importWord"
          />
        </div>
      </template>
    </UiDialog>
</template>
<style scoped>
.teacher-question-drawer {
  min-width: 0;
  padding-bottom: 4px;
}

.teacher-question-form-section {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 14px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-question-form-section__heading {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.teacher-question-form-section__heading strong {
  min-width: 0;
  color: var(--st-text);
  overflow-wrap: anywhere;
}

.teacher-question-drawer__footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.teacher-question-dialog-copy {
  margin: 0;
  line-height: 1.55;
}


@media (max-width: 640px) {
  .teacher-question-form-section { padding: 12px; }
  .teacher-question-drawer__footer,
  .teacher-question-drawer__footer > * { width: 100%; }
  .teacher-question-drawer__footer { flex-direction: column-reverse; }
}

</style>
