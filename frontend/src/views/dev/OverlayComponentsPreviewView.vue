<script setup>
import { computed, reactive } from 'vue'

import {
  UiAlert,
  UiButton,
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const subjectOptions = [
  { label: 'Все предметы', value: 'all' },
  { label: 'Базы данных', value: 'db' },
  { label: 'Алгоритмы и структуры данных', value: 'algo' },
]

const topicOptions = [
  { label: 'Транзакции', value: 'transactions' },
  { label: 'Индексы', value: 'indexes' },
  { label: 'Нормализация', value: 'normalization' },
]

const questionTypeOptions = [
  { label: 'Все типы', value: 'all' },
  { label: 'Один ответ', value: 'single' },
  { label: 'Несколько ответов', value: 'multiple' },
  { label: 'Текстовый ответ', value: 'text' },
  { label: 'Сопоставление', value: 'matching' },
]

const questions = [
  {
    id: 1,
    subject: 'db',
    subjectLabel: 'Базы данных',
    topic: 'transactions',
    topicLabel: 'Транзакции',
    type: 'single',
    typeLabel: 'Один ответ',
    text: 'Какое свойство ACID гарантирует принцип «всё или ничего»?',
  },
  {
    id: 2,
    subject: 'db',
    subjectLabel: 'Базы данных',
    topic: 'indexes',
    topicLabel: 'Индексы',
    type: 'multiple',
    typeLabel: 'Несколько ответов',
    text: 'Какие структуры могут использоваться для организации индекса?',
  },
  {
    id: 3,
    subject: 'algo',
    subjectLabel: 'Алгоритмы и структуры данных',
    topic: 'normalization',
    topicLabel: 'Практика',
    type: 'text',
    typeLabel: 'Текстовый ответ',
    text: 'Опишите асимптотическую сложность бинарного поиска.',
  },
]

const filters = reactive({
  search: '',
  subject: 'all',
  type: 'all',
})

const filteredQuestions = computed(() => {
  const search = filters.search.trim().toLocaleLowerCase('ru-RU')

  return questions.filter((question) => {
    const matchesSearch = !search || [
      question.text,
      question.topicLabel,
      question.subjectLabel,
    ].some((value) => value.toLocaleLowerCase('ru-RU').includes(search))
    const matchesSubject = filters.subject === 'all' || question.subject === filters.subject
    const matchesType = filters.type === 'all' || question.type === filters.type
    return matchesSearch && matchesSubject && matchesType
  })
})

const filtersAreClean = computed(() =>
  !filters.search && filters.subject === 'all' && filters.type === 'all'
)

function resetFilters() {
  filters.search = ''
  filters.subject = 'all'
  filters.type = 'all'
}

const topicOverlay = useOverlayForm({
  createDefault: () => ({
    title: '',
    description: '',
  }),
})

const questionOverlay = useOverlayForm({
  createDefault: () => ({
    subject: 'db',
    topic: 'transactions',
    type: 'single',
    text: '',
    required: true,
    explanation: '',
  }),
})

function openTopicDemo() {
  topicOverlay.openEdit({
    title: 'Транзакции',
    description: 'ACID, уровни изоляции и типовые аномалии.',
  })
}

function openQuestionDemo() {
  questionOverlay.openEdit({
    subject: 'db',
    topic: 'transactions',
    type: 'single',
    text: 'Какое свойство ACID гарантирует, что транзакция выполняется целиком или не выполняется вовсе?',
    required: true,
    explanation: 'Atomicity описывает принцип «всё или ничего».',
  })
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function saveTopic() {
  topicOverlay.beginSaving()
  await wait(650)
  topicOverlay.finishSaving()
}

async function saveQuestion() {
  questionOverlay.beginSaving()
  await wait(650)
  questionOverlay.finishSaving()
}

const activeThemeLabel = computed(() =>
  `${themeStore.theme} → ${themeStore.resolvedTheme}`
)
</script>

<template>
  <main class="min-h-full bg-[var(--st-page-bg)] p-4 text-[var(--st-text)] md:p-7">
    <section
      class="mx-auto grid max-w-6xl gap-6 rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface)] p-5 shadow-[var(--st-shadow-card)] md:p-7"
    >
      <header class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <div class="text-xs font-bold tracking-[0.08em] text-[var(--st-primary)]">
            TEACHER CRUD FOUNDATION PREVIEW
          </div>
          <h1 class="mt-1 text-2xl font-bold md:text-3xl">Workspace + overlay foundation</h1>
          <p class="mt-3 leading-7 text-[var(--st-text-secondary)]">
            На этом этапе Teacher pages ещё не мигрируются. Проверяем общий toolbar фильтров,
            create/edit state, dirty-state, saving-state и защиту от случайного закрытия формы.
          </p>
        </div>

        <div class="grid gap-2 lg:justify-items-end">
          <span class="text-xs text-[var(--st-text-secondary)]">{{ activeThemeLabel }}</span>
          <div class="flex flex-wrap gap-2">
            <UiButton size="sm" variant="secondary" label="Light" @click="themeStore.setTheme('light')" />
            <UiButton size="sm" variant="secondary" label="Dark" @click="themeStore.setTheme('dark')" />
            <UiButton size="sm" variant="secondary" label="System" @click="themeStore.setTheme('system')" />
          </div>
        </div>
      </header>

      <UiAlert
        variant="info"
        title="Что нового"
        message="UiFilterBar стандартизирует рабочий список. useOverlayForm управляет create/edit, dirty и saving без знания конкретной сущности. UiUnsavedChangesConfirm защищает изменённую форму при ×, Esc и кнопке Отмена."
      />

      <section class="grid gap-4">
        <div>
          <h2 class="text-lg font-bold">1. UiFilterBar</h2>
          <p class="mt-1 text-sm leading-6 text-[var(--st-text-secondary)]">
            Такой блок будет стоять над темами, вопросами, лекциями и шаблонами. На телефоне
            поиск, фильтры и действия автоматически складываются в одну колонку.
          </p>
        </div>

        <UiFilterBar
          v-model="filters.search"
          search-placeholder="Поиск по вопросу, теме или предмету"
          :result-count="filteredQuestions.length"
          :reset-disabled="filtersAreClean"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="filters.subject"
              label="Предмет"
              :options="subjectOptions"
            />
            <UiSelect
              v-model="filters.type"
              label="Тип вопроса"
              :options="questionTypeOptions"
            />
          </template>

          <template #actions>
            <UiButton
              variant="primary"
              label="Добавить вопрос"
              icon="pi pi-plus"
              @click="questionOverlay.openCreate()"
            />
          </template>
        </UiFilterBar>

        <div class="grid gap-3">
          <article
            v-for="question in filteredQuestions"
            :key="question.id"
            class="rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface-muted)] p-4"
          >
            <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <div class="flex flex-wrap gap-2">
                  <UiTag variant="info" :value="question.subjectLabel" />
                  <UiTag variant="secondary" :value="question.topicLabel" />
                  <UiTag variant="secondary" :value="question.typeLabel" />
                </div>
                <p class="mt-3 font-semibold leading-7 [overflow-wrap:anywhere]">
                  {{ question.text }}
                </p>
              </div>
              <UiButton
                variant="secondary"
                size="sm"
                label="Редактировать"
                icon="pi pi-pencil"
                @click="openQuestionDemo"
              />
            </div>
          </article>

          <div
            v-if="filteredQuestions.length === 0"
            class="rounded-[var(--st-radius-card)] border border-dashed border-[var(--st-border)] p-6 text-center text-[var(--st-text-secondary)]"
          >
            По заданным фильтрам ничего не найдено.
          </div>
        </div>
      </section>

      <section class="grid gap-4 lg:grid-cols-2">
        <article
          class="grid content-between gap-5 rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface-muted)] p-5"
        >
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <UiTag variant="info" value="Dialog" />
              <UiTag variant="secondary" value="Короткая форма" />
            </div>
            <h2 class="mt-3 text-lg font-bold">2. Редактирование темы</h2>
            <p class="mt-2 leading-6 text-[var(--st-text-secondary)]">
              Измени любое поле и нажми ×, Esc или «Отмена» — форма не должна закрыться
              без подтверждения.
            </p>
          </div>

          <UiButton
            variant="primary"
            label="Проверить Dialog + dirty-state"
            icon="pi pi-window-maximize"
            @click="openTopicDemo"
          />
        </article>

        <article
          class="grid content-between gap-5 rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface-muted)] p-5"
        >
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <UiTag variant="success" value="Drawer" />
              <UiTag variant="secondary" value="Средняя форма" />
            </div>
            <h2 class="mt-3 text-lg font-bold">3. Редактирование вопроса</h2>
            <p class="mt-2 leading-6 text-[var(--st-text-secondary)]">
              Здесь тот же lifecycle, но для большой формы. «Сохранить» показывает
              loading-state и после успешного сохранения закрывает Drawer.
            </p>
          </div>

          <UiButton
            variant="primary"
            label="Проверить Drawer + dirty-state"
            icon="pi pi-arrow-left"
            @click="openQuestionDemo"
          />
        </article>
      </section>

      <section class="rounded-[var(--st-radius-card)] border border-dashed border-[var(--st-border)] p-5">
        <h2 class="text-lg font-bold">Контракт перед эталонной страницей</h2>
        <div class="mt-4 grid gap-3 md:grid-cols-3">
          <div class="rounded-[var(--st-radius-control)] bg-[var(--st-surface-muted)] p-4">
            <strong>Workspace</strong>
            <p class="mt-1 text-sm leading-6 text-[var(--st-text-secondary)]">
              Поиск + фильтры + результат + основное действие остаются на активной странице.
            </p>
          </div>
          <div class="rounded-[var(--st-radius-control)] bg-[var(--st-surface-muted)] p-4">
            <strong>Overlay lifecycle</strong>
            <p class="mt-1 text-sm leading-6 text-[var(--st-text-secondary)]">
              create/edit, dirty, saving и закрытие работают одинаково для Dialog и Drawer.
            </p>
          </div>
          <div class="rounded-[var(--st-radius-control)] bg-[var(--st-surface-muted)] p-4">
            <strong>Предметная форма</strong>
            <p class="mt-1 text-sm leading-6 text-[var(--st-text-secondary)]">
              Общий слой не знает API и структуру сущности — это останется внутри конкретной страницы.
            </p>
          </div>
        </div>
      </section>
    </section>

    <UiDialog
      v-model="topicOverlay.model.value"
      :title="topicOverlay.isCreate.value ? 'Создание темы' : 'Редактирование темы'"
      width="34rem"
    >
      <div class="grid gap-4">
        <UiInput v-model="topicOverlay.form.title" label="Название темы" autofocus />
        <UiTextarea
          v-model="topicOverlay.form.description"
          label="Описание"
          :rows="5"
          hint="Короткое описание для преподавателя и навигации."
        />
      </div>

      <template #footer>
        <div class="flex w-full flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="topicOverlay.saving.value"
            @click="topicOverlay.requestClose"
          />
          <UiButton
            variant="primary"
            label="Сохранить"
            loading-text="Сохраняем..."
            icon="pi pi-check"
            :loading="topicOverlay.saving.value"
            @click="saveTopic"
          />
        </div>
      </template>
    </UiDialog>

    <UiDrawer
      v-model="questionOverlay.model.value"
      :title="questionOverlay.isCreate.value ? 'Новый вопрос' : 'Редактирование вопроса'"
      position="right"
      width="42rem"
    >
      <div class="grid gap-5">
        <div class="grid gap-4 sm:grid-cols-2">
          <UiSelect
            v-model="questionOverlay.form.subject"
            label="Предмет"
            :options="subjectOptions.filter((item) => item.value !== 'all')"
            class="sm:col-span-2"
          />
          <UiSelect
            v-model="questionOverlay.form.topic"
            label="Тема"
            :options="topicOptions"
            filter
          />
          <UiSelect
            v-model="questionOverlay.form.type"
            label="Тип вопроса"
            :options="questionTypeOptions.filter((item) => item.value !== 'all')"
          />
        </div>

        <UiTextarea
          v-model="questionOverlay.form.text"
          label="Текст вопроса"
          :rows="6"
        />

        <UiCheckbox
          v-model="questionOverlay.form.required"
          label="Обязательный вопрос"
          description="Используется как пример состояния редактора."
        />

        <UiTextarea
          v-model="questionOverlay.form.explanation"
          label="Пояснение к ответу"
          :rows="4"
        />

        <div class="rounded-[var(--st-radius-control)] border border-[var(--st-border)] bg-[var(--st-surface-muted)] p-4">
          <strong>Редактор вариантов ответа</strong>
          <p class="mt-1 text-sm leading-6 text-[var(--st-text-secondary)]">
            На эталонной странице этот блок будет собран из существующих UiInput,
            UiCheckbox и UiRadio. Foundation не навязывает его структуру.
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex w-full flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="questionOverlay.saving.value"
            @click="questionOverlay.requestClose"
          />
          <UiButton
            variant="primary"
            label="Сохранить изменения"
            loading-text="Сохраняем..."
            icon="pi pi-check"
            :loading="questionOverlay.saving.value"
            @click="saveQuestion"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="topicOverlay.confirmCloseVisible.value"
      :busy="topicOverlay.saving.value"
      @continue="topicOverlay.continueEditing"
      @discard="topicOverlay.discardAndClose"
    />

    <UiUnsavedChangesConfirm
      v-model="questionOverlay.confirmCloseVisible.value"
      :busy="questionOverlay.saving.value"
      @continue="questionOverlay.continueEditing"
      @discard="questionOverlay.discardAndClose"
    />
  </main>
</template>
