<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiLoadingState,
  UiSelect,
  UiStat,
  UiStatGrid,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

import ReviewAnswerCard from '@/components/results/ReviewAnswerCard.vue'
import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import { useAiStatus } from '@/composables/ai/useAiStatus'
import { useUnsavedNavigationGuard } from '@/composables/shared/useUnsavedNavigationGuard'
import {
  REVIEW_STATUS_OPTIONS,
  useReviewQueue,
} from '@/composables/results/useReviewQueue'

const {
  status,
  items,
  counts,
  loading,
  error,
  busyResponseId,
  load,
  grade,
  accept,
  regrade,
} = useReviewQueue()

const { aiStatus } = useAiStatus()

const notice = ref({ type: 'info', message: '' })

// Ответы, в которых введены, но не сохранены баллы или комментарий.
const dirtyResponses = reactive(new Set())
const hasUnsavedGrades = computed(() => dirtyResponses.size > 0)

function trackDirty(responseId, dirty) {
  if (dirty) {
    dirtyResponses.add(responseId)
  } else {
    dirtyResponses.delete(responseId)
  }
}

const {
  confirmVisible: leaveConfirmVisible,
  continueEditing: continueReviewing,
  discardAndNavigate,
} = useUnsavedNavigationGuard(hasUnsavedGrades)

const aiAvailable = computed(() => Boolean(aiStatus.value?.grading))

const emptyText = computed(() => (
  status.value === 'needs_teacher'
    ? 'Все ответы проверены.'
    : 'В этом разделе пока нет ответов.'
))

async function handle(result) {
  const outcome = await result
  if (outcome?.message) {
    notice.value = {
      type: outcome.ok ? 'success' : 'danger',
      message: outcome.message,
    }
  }
}

watch(status, () => load())

onMounted(load)
</script>

<template>
  <TeacherPageShell title="Проверка ответов">
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="load"
      />
    </template>

    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiAlert
      v-if="aiStatus && !aiStatus.grading"
      variant="info"
      message="Проверка ответов ИИ выключена. Здесь собираются ответы, засчитанные по похожести на эталон, — подтвердите или исправьте их баллы."
    />

    <UiStatGrid>
      <UiStat label="Нужна проверка" :value="counts.needs_teacher" />
      <UiStat label="Проверяет ИИ" :value="counts.ai_pending" />
      <UiStat label="Оценено ИИ" :value="counts.ai_graded" />
      <UiStat label="Проверено" :value="counts.teacher_confirmed" />
    </UiStatGrid>

    <UiCard title="Ответы">
      <div class="teacher-stack">
        <div class="review-queue__filters">
          <UiSelect
            v-model="status"
            label="Показать"
            :options="REVIEW_STATUS_OPTIONS"
            size="sm"
          />
        </div>

        <UiAlert
          v-if="error"
          variant="danger"
          :message="error"
        />

        <UiLoadingState
          v-if="loading && !items.length"
          compact
          label="Загрузка ответов..."
        />

        <UiEmptyState
          v-else-if="!items.length"
          :description="emptyText"
          compact
        />

        <div
          v-else
          class="review-queue__list"
        >
          <ReviewAnswerCard
            v-for="item in items"
            :key="item.responseId"
            :item="item"
            :busy="busyResponseId === item.responseId"
            @dirty-change="trackDirty(item.responseId, $event)"
            :ai-available="aiAvailable"
            @grade="handle(grade(item, $event.points, $event.comment))"
            @accept="handle(accept(item))"
            @regrade="handle(regrade(item))"
          />
        </div>
      </div>
    </UiCard>
    <UiUnsavedChangesConfirm
      v-model="leaveConfirmVisible"
      title="Есть несохранённые оценки"
      message="Введённые баллы и комментарии не сохранены и будут потеряны."
      discard-label="Уйти без сохранения"
      @continue="continueReviewing"
      @discard="discardAndNavigate"
    />
  </TeacherPageShell>
</template>

<style scoped>
.review-queue__filters {
  max-width: 320px;
}

.review-queue__list {
  display: grid;
  gap: 12px;
}
</style>
