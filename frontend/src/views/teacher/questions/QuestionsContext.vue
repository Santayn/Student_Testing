<script setup>
import { UiCard, UiSelect, UiButton } from '@/components/ui'
import { toRefs } from 'vue'
import { useQuestionsWorkspaceContext } from '@/composables/questions/questionsWorkspaceContext'
const {
  contextHint,
  selectedMembershipId,
  membershipOptions,
  loadingSubjects,
  selectedTopicId,
  topicOptions,
  routeQuery,
} = toRefs(useQuestionsWorkspaceContext())
</script>
<template>
<UiCard
      title="Контекст вопросов"
      :description="contextHint"
    >
      <div class="teacher-question-context">
        <div class="teacher-grid">
          <UiSelect
            v-model="selectedMembershipId"
            label="Предмет преподавателя"
            :options="membershipOptions"
            placeholder="Выберите предмет"
            :disabled="loadingSubjects || !membershipOptions.length"
          />

          <UiSelect
            v-model="selectedTopicId"
            label="Тема"
            :options="topicOptions"
            placeholder="Выберите тему"
            :disabled="!topicOptions.length"
          />
        </div>

        <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
          <UiButton
            :to="{
              name: 'teacher-topics',
              query: routeQuery(),
            }"
          >
            Темы предмета
          </UiButton>

          <UiButton
            v-if="selectedTopicId"
            :to="{
              name: 'teacher-test-create',
              query: routeQuery(),
            }"
          >
            Создать тест по теме
          </UiButton>
        </div>
      </div>
    </UiCard>
</template>
<style scoped>

.teacher-question-context { min-width: 0; display: grid; gap: 14px; }

</style>
