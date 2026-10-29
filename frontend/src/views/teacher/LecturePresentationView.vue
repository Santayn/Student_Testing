<script setup>
import {
  computed,
  onMounted,
} from 'vue'

import { useRoute } from 'vue-router'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiLoadingState,
  UiSelect,
  UiTextarea,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import { useAiStatus } from '@/composables/ai/useAiStatus'
import { useUnsavedNavigationGuard } from '@/composables/shared/useUnsavedNavigationGuard'
import {
  SLIDE_IMAGE_OPTIONS,
  SLIDE_LAYOUT_OPTIONS,
  useLecturePresentation,
} from '@/composables/lectures/useLecturePresentation'

const route = useRoute()
const lectureId = computed(() => Number(route.params.lectureId))

const { aiStatus } = useAiStatus()

const {
  lecture,
  imageMaterials,
  loading,
  loadError,
  settings,
  outline,
  outlineJob,
  buildJob,
  builtResult,
  hasOutline,
  unsavedOutline,
  load,
  generateOutline,
  addSlide,
  removeSlide,
  moveSlide,
  searchImages,
  selectFound,
  generateImage,
  clearImage,
  build,
  downloadBuilt,
} = useLecturePresentation(lectureId)

const {
  confirmVisible: leaveConfirmVisible,
  continueEditing,
  discardAndNavigate,
} = useUnsavedNavigationGuard(unsavedOutline)

const imageModeOptions = computed(() => SLIDE_IMAGE_OPTIONS.filter((option) => {
  if (option.value === 'search') return aiStatus.value?.imageSearch !== false
  if (option.value === 'generate') return Boolean(aiStatus.value?.imageGeneration)
  if (option.value === 'material') return imageMaterials.value.length > 0
  return true
}))

const materialOptions = computed(() => imageMaterials.value.map((material) => ({
  value: material.id,
  label: material.fileName,
})))

const pageSubtitle = computed(() => (
  lecture.value?.title ? `Лекция «${lecture.value.title}»` : ''
))

function slidesText(count) {
  const value = Number(count) || 0
  const mod10 = value % 10
  const mod100 = value % 100
  if (mod10 === 1 && mod100 !== 11) return `${value} слайд`
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return `${value} слайда`
  return `${value} слайдов`
}

onMounted(load)
</script>

<template>
  <TeacherPageShell
    title="Презентация по лекции"
    :subtitle="pageSubtitle"
  >
    <UiLoadingState
      v-if="loading"
      label="Загрузка лекции..."
    />

    <UiAlert
      v-else-if="loadError"
      variant="danger"
      :message="loadError"
    />

    <template v-else>
      <UiAlert
        v-if="aiStatus && !aiStatus.presentation"
        variant="warning"
        message="ИИ не настроен на сервере — составить план не получится. Обратитесь к администратору."
      />

      <UiCard title="Шаг 1 · План слайдов">
        <div class="teacher-stack">
          <div class="teacher-grid teacher-grid--3">
            <UiInput
              v-model="settings.slideCount"
              label="Слайдов"
              type="number"
              min="5"
              max="20"
              step="1"
            />
          </div>
          <UiTextarea
            v-model="settings.instructions"
            label="Пожелания"
            :rows="2"
            auto-resize
            maxlength="1000"
            placeholder="Например: для первокурсников, больше примеров из практики"
          />
          <div class="presentation-actions">
            <UiButton
              variant="primary"
              icon="pi pi-sparkles"
              :label="hasOutline ? 'Составить план заново' : 'Составить план'"
              :loading="outlineJob.busy.value"
              :loading-text="outlineJob.progress.value || 'ИИ составляет план…'"
              :disabled="aiStatus && !aiStatus.presentation"
              @click="generateOutline"
            />
          </div>
          <UiAlert
            v-if="outlineJob.error.value"
            variant="danger"
            :message="outlineJob.error.value"
          />
        </div>
      </UiCard>

      <UiCard
        v-if="hasOutline"
        title="Шаг 2 · Слайды"
      >
        <div class="teacher-stack">
          <div class="teacher-grid">
            <UiInput
              v-model="outline.title"
              label="Название презентации"
              maxlength="120"
            />
            <UiInput
              v-model="outline.subtitle"
              label="Подзаголовок"
              maxlength="160"
            />
          </div>

          <article
            v-for="(slide, index) in outline.slides"
            :key="slide.key"
            class="presentation-slide"
          >
            <header class="presentation-slide__header">
              <strong>Слайд {{ index + 2 }}</strong>
              <div class="presentation-slide__controls">
                <UiButton
                  variant="ghost"
                  size="sm"
                  icon="pi pi-arrow-up"
                  aria-label="Переместить выше"
                  :disabled="index === 0"
                  @click="moveSlide(index, -1)"
                />
                <UiButton
                  variant="ghost"
                  size="sm"
                  icon="pi pi-arrow-down"
                  aria-label="Переместить ниже"
                  :disabled="index === outline.slides.length - 1"
                  @click="moveSlide(index, 1)"
                />
                <UiButton
                  variant="ghost"
                  size="sm"
                  icon="pi pi-trash"
                  aria-label="Удалить слайд"
                  :disabled="outline.slides.length <= 1"
                  @click="removeSlide(index)"
                />
              </div>
            </header>

            <div class="teacher-grid">
              <UiInput
                v-model="slide.title"
                label="Заголовок"
                maxlength="120"
              />
              <UiSelect
                v-model="slide.layout"
                label="Вид слайда"
                :options="SLIDE_LAYOUT_OPTIONS"
              />
            </div>

            <UiTextarea
              v-if="slide.layout === 'bullets' || slide.layout === 'quote'"
              v-model="slide.bulletsText"
              :label="slide.layout === 'quote' ? 'Текст' : 'Тезисы — по одному на строку'"
              :rows="4"
              auto-resize
            />

            <div
              v-if="slide.layout === 'two_columns'"
              class="teacher-grid"
            >
              <div class="teacher-stack">
                <UiInput v-model="slide.leftTitle" label="Левая колонка" />
                <UiTextarea v-model="slide.leftText" label="Пункты" :rows="3" auto-resize />
              </div>
              <div class="teacher-stack">
                <UiInput v-model="slide.rightTitle" label="Правая колонка" />
                <UiTextarea v-model="slide.rightText" label="Пункты" :rows="3" auto-resize />
              </div>
            </div>

            <UiTextarea
              v-if="slide.layout === 'process'"
              v-model="slide.stepsText"
              label="Шаги — по одному на строку"
              :rows="4"
              auto-resize
            />

            <UiTextarea
              v-model="slide.notes"
              label="Заметки докладчика"
              :rows="2"
              auto-resize
            />

            <div
              v-if="slide.layout === 'bullets'"
              class="presentation-image"
            >
              <UiSelect
                v-model="slide.image.mode"
                label="Картинка"
                :options="imageModeOptions"
                @update:model-value="clearImage(slide)"
              />

              <template v-if="slide.image.mode === 'search'">
                <div class="presentation-image__row">
                  <UiInput
                    v-model="slide.image.query"
                    label="Что искать"
                    @keydown.enter.prevent="searchImages(slide)"
                  />
                  <UiButton
                    icon="pi pi-search"
                    label="Найти"
                    :loading="slide.image.searching"
                    loading-text="Ищу…"
                    :disabled="!slide.image.query.trim()"
                    @click="searchImages(slide)"
                  />
                </div>
                <div
                  v-if="slide.image.results.length"
                  class="presentation-image__results"
                >
                  <button
                    v-for="result in slide.image.results.filter((item) => !item.failed)"
                    :key="result.imageUrl"
                    type="button"
                    class="presentation-image__option"
                    :class="{ 'presentation-image__option--selected': slide.image.selected?.url === result.imageUrl }"
                    :aria-pressed="slide.image.selected?.url === result.imageUrl ? 'true' : 'false'"
                    :disabled="!result.previewUrl"
                    :title="[result.title, result.author, result.license].filter(Boolean).join(' · ')"
                    @click="selectFound(slide, result)"
                  >
                    <img
                      v-if="result.previewUrl"
                      :src="result.previewUrl"
                      :alt="result.title || 'Найденная картинка'"
                    >
                    <span v-else class="presentation-image__placeholder">Загрузка…</span>
                    <small>{{ result.license || result.source }}</small>
                  </button>
                </div>
              </template>

              <template v-if="slide.image.mode === 'generate'">
                <UiTextarea
                  v-model="slide.image.prompt"
                  label="Описание картинки"
                  :rows="2"
                  auto-resize
                  maxlength="500"
                />
                <div class="presentation-actions">
                  <UiButton
                    icon="pi pi-image"
                    :label="slide.image.selected ? 'Сгенерировать заново' : 'Сгенерировать'"
                    :loading="slide.image.generating"
                    loading-text="Рисую…"
                    :disabled="!slide.image.prompt.trim()"
                    @click="generateImage(slide)"
                  />
                </div>
              </template>

              <UiSelect
                v-if="slide.image.mode === 'material'"
                v-model="slide.image.materialId"
                label="Файл"
                :options="materialOptions"
                placeholder="Выберите картинку"
              />

              <img
                v-if="slide.image.previewUrl && slide.image.mode !== 'search'"
                class="presentation-image__preview"
                :src="slide.image.previewUrl"
                alt="Картинка слайда"
              >

              <UiAlert
                v-if="slide.image.error"
                variant="warning"
                :message="slide.image.error"
              />
            </div>
          </article>

          <div class="presentation-actions">
            <UiButton
              variant="secondary"
              icon="pi pi-plus"
              label="Добавить слайд"
              @click="addSlide()"
            />
          </div>
        </div>
      </UiCard>

      <UiCard
        v-if="hasOutline"
        title="Шаг 3 · Сборка"
      >
        <div class="teacher-stack">
          <UiCheckbox
            v-model="settings.withPdf"
            label="Сделать также PDF"
          />
          <div class="presentation-actions">
            <UiButton
              variant="primary"
              icon="pi pi-file-export"
              label="Собрать презентацию"
              :loading="buildJob.busy.value"
              :loading-text="buildJob.progress.value || 'Собираю…'"
              @click="build"
            />
          </div>
          <UiAlert
            v-if="buildJob.error.value"
            variant="danger"
            :message="buildJob.error.value"
          />
          <template v-if="builtResult">
            <UiAlert
              variant="success"
              :message="`Готово: ${slidesText(builtResult.slides)}, файлы добавлены в материалы лекции.`"
            />
            <div class="presentation-actions">
              <UiButton
                icon="pi pi-download"
                :label="builtResult.fileName"
                @click="downloadBuilt(builtResult.materialId, builtResult.fileName)"
              />
              <UiButton
                v-if="builtResult.pdfMaterialId"
                icon="pi pi-download"
                :label="builtResult.pdfFileName"
                @click="downloadBuilt(builtResult.pdfMaterialId, builtResult.pdfFileName)"
              />
            </div>
          </template>
        </div>
      </UiCard>

      <UiEmptyState
        v-if="!hasOutline && !outlineJob.busy.value"
        description="Составьте план — слайды появятся здесь для правки."
        compact
      />
    </template>
    <UiUnsavedChangesConfirm
      v-model="leaveConfirmVisible"
      title="Презентация не собрана"
      message="План слайдов и правки будут потеряны."
      discard-label="Уйти без сборки"
      @continue="continueEditing"
      @discard="discardAndNavigate"
    />
  </TeacherPageShell>
</template>

<style scoped>
.presentation-actions,
.presentation-slide__controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.presentation-slide {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.presentation-slide__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.presentation-image {
  display: grid;
  gap: 10px;
}

.presentation-image__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 8px;
}

.presentation-image__results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}

.presentation-image__option {
  padding: 4px;
  display: grid;
  gap: 4px;
  color: var(--st-text-secondary);
  background: var(--st-surface);
  border: 2px solid var(--st-border);
  border-radius: 10px;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.presentation-image__option:focus-visible {
  outline: 2px solid var(--st-primary);
  outline-offset: 2px;
}

.presentation-image__option--selected {
  border-color: var(--st-primary);
}

.presentation-image__option img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  border-radius: 6px;
}

.presentation-image__option small {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.presentation-image__placeholder {
  height: 96px;
  display: grid;
  place-items: center;
  font-size: 12px;
}

.presentation-image__preview {
  max-width: min(100%, 360px);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

@media (max-width: 640px) {
  .presentation-image__row {
    grid-template-columns: 1fr;
  }
}
</style>
