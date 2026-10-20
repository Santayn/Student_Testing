<script setup>
import {
  computed,
  ref,
} from 'vue'

import { aiApi } from '@/api/ai.api'

import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiFileInput,
  UiTextarea,
} from '@/components/ui'

import LectureMarkdown from '@/components/lectures/LectureMarkdown.vue'

import { useAiJob } from '@/composables/ai/useAiJob'
import { useAiStatus } from '@/composables/ai/useAiStatus'

const props = defineProps({
  modelValue: { type: String, default: '' },
  lectureId: { type: [Number, String], default: null },
  disabled: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const { aiStatus } = useAiStatus()
const scanJob = useAiJob()

const preview = ref(false)
const scanOpen = ref(false)
const scanFiles = ref([])
const scanInputKey = ref(0)
const attachOriginals = ref(true)
const recognized = ref('')

const scanAvailable = computed(() => Boolean(aiStatus.value?.lectureScan))
const hasBody = computed(() => Boolean(String(props.modelValue ?? '').trim()))

function toggleScan() {
  scanOpen.value = !scanOpen.value
  if (!scanOpen.value) {
    resetScan()
  }
}

function resetScan() {
  scanJob.reset()
  scanFiles.value = []
  scanInputKey.value++
  recognized.value = ''
}

async function startScan() {
  if (!scanFiles.value.length || scanJob.busy.value) {
    return
  }

  recognized.value = ''
  const done = await scanJob.track(
    aiApi.scanLecture(scanFiles.value, {
      lectureId: props.lectureId ? Number(props.lectureId) : null,
      attachOriginals: Boolean(props.lectureId) && attachOriginals.value,
    })
  )

  if (done) {
    recognized.value = String(done.result?.markdown ?? '').trim()
  }
}

function insertRecognized(mode) {
  const current = String(props.modelValue ?? '').trim()
  const next = mode === 'append' && current
    ? `${current}\n\n${recognized.value}`
    : recognized.value
  emit('update:modelValue', next)
  scanOpen.value = false
  resetScan()
}
</script>

<template>
  <section class="teacher-lecture-form-section lecture-body-editor">
    <div class="teacher-lecture-form-section__heading">
      <span class="teacher-muted">Текст лекции</span>
      <div class="lecture-body-editor__tools">
        <UiButton
          variant="ghost"
          size="sm"
          :icon="preview ? 'pi pi-pencil' : 'pi pi-eye'"
          :label="preview ? 'Редактировать' : 'Просмотр'"
          :disabled="!hasBody && !preview"
          @click="preview = !preview"
        />
        <UiButton
          v-if="scanAvailable"
          variant="ghost"
          size="sm"
          icon="pi pi-camera"
          :label="scanOpen ? 'Скрыть распознавание' : 'Распознать скан'"
          :disabled="disabled"
          @click="toggleScan"
        />
      </div>
    </div>

    <div
      v-if="scanOpen"
      class="lecture-body-editor__scan"
    >
      <UiFileInput
        :key="scanInputKey"
        label="Фото страниц или PDF"
        accept="image/jpeg,image/png,application/pdf"
        multiple
        :disabled="scanJob.busy.value"
        @files-change="scanFiles = $event"
      />

      <UiCheckbox
        v-if="lectureId"
        v-model="attachOriginals"
        label="Прикрепить сканы к материалам лекции"
        :disabled="scanJob.busy.value"
      />

      <div class="lecture-body-editor__scan-actions">
        <UiButton
          variant="primary"
          size="sm"
          icon="pi pi-sparkles"
          label="Распознать"
          :loading="scanJob.busy.value"
          :loading-text="scanJob.progress.value || 'Распознаю…'"
          :disabled="!scanFiles.length"
          @click="startScan"
        />
        <span
          v-if="scanFiles.length && !scanJob.busy.value"
          class="teacher-muted"
        >Файлов: {{ scanFiles.length }}</span>
      </div>

      <UiAlert
        v-if="scanJob.error.value"
        variant="danger"
        :message="scanJob.error.value"
      />

      <template v-if="recognized">
        <div class="lecture-body-editor__recognized">
          <LectureMarkdown :source="recognized" />
        </div>
        <div class="lecture-body-editor__scan-actions">
          <UiButton
            variant="primary"
            size="sm"
            :label="hasBody ? 'Заменить текст лекции' : 'Вставить в текст лекции'"
            @click="insertRecognized('replace')"
          />
          <UiButton
            v-if="hasBody"
            variant="secondary"
            size="sm"
            label="Добавить в конец"
            @click="insertRecognized('append')"
          />
        </div>
      </template>
    </div>

    <div
      v-if="preview"
      class="lecture-body-editor__preview"
    >
      <LectureMarkdown :source="modelValue" />
    </div>

    <UiTextarea
      v-else
      :model-value="modelValue"
      class="lecture-body-editor__input"
      label="Markdown"
      hint="## Заголовок, - пункт списка, **жирный**, *курсив*, таблицы через |"
      :rows="10"
      auto-resize
      :error="error"
      :disabled="disabled"
      @update:model-value="emit('update:modelValue', $event)"
    />
  </section>
</template>

<style scoped>
.teacher-lecture-form-section__heading {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.lecture-body-editor__tools,
.lecture-body-editor__scan-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.lecture-body-editor__scan {
  padding: 12px;
  display: grid;
  gap: 12px;
  background: var(--st-surface);
  border: 1px dashed var(--st-border);
  border-radius: 10px;
}

.lecture-body-editor__recognized,
.lecture-body-editor__preview {
  max-height: 420px;
  padding: 14px;
  overflow-y: auto;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.lecture-body-editor__input :deep(textarea) {
  font-family: var(--st-font-mono);
  font-size: 13px;
}
</style>
