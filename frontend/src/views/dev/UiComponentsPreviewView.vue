<script setup>
import { computed, ref } from 'vue'

import {
  UiActionMenu,
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiEmptyState,
  UiFileInput,
  UiInput,
  UiRadio,
  UiSearchInput,
  UiSelect,
  UiTable,
  UiTag,
  UiTextarea,
  UiToastHost,
  UiToolbar,
  useUiToast,
} from '@/components/ui'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const uiToast = useUiToast()

const name = ref('Александр Тиликин')
const email = ref('student@example.com')
const readonlyValue = ref('Только для чтения')
const search = ref('')
const note = ref('Короткий пример многострочного текста.')
const subject = ref('db')
const accepted = ref(true)
const disabledAccepted = ref(false)
const selectedModules = ref(['tests'])
const role = ref('student')
const dialogVisible = ref(false)

const subjectOptions = [
  { label: 'Базы данных', value: 'db' },
  { label: 'Алгоритмы и структуры данных', value: 'algo' },
  { label: 'Компьютерные сети', value: 'net' },
]

const tableColumns = [
  { key: 'name', label: 'Предмет' },
  { key: 'status', label: 'Статус' },
  { key: 'students', label: 'Студентов' },
]

const tableRows = [
  { id: 1, name: 'Алгоритмы и структуры данных', status: 'active', students: 28 },
  { id: 2, name: 'Базы данных', status: 'draft', students: 21 },
  { id: 3, name: 'Компьютерные сети', status: 'active', students: 31 },
  { id: 4, name: 'Операционные системы', status: 'archive', students: 19 },
  { id: 5, name: 'Архитектура ЭВМ', status: 'active', students: 24 },
  { id: 6, name: 'Теория информации', status: 'draft', students: 18 },
  { id: 7, name: 'Программная инженерия', status: 'active', students: 27 },
  { id: 8, name: 'Компьютерная графика', status: 'archive', students: 16 },
]

const actionItems = [
  {
    label: 'Открыть',
    icon: 'pi pi-external-link',
    command: () => uiToast.info('Выбрано действие «Открыть».', 'Меню'),
  },
  {
    label: 'Дублировать',
    icon: 'pi pi-copy',
    command: () => uiToast.success('Элемент продублирован.', 'Меню'),
  },
  { separator: true },
  {
    label: 'Удалить',
    icon: 'pi pi-trash',
    danger: true,
    command: () => uiToast.error('Пример danger-действия.', 'Меню'),
  },
]

function statusVariant(status) {
  if (status === 'active') return 'success'
  if (status === 'draft') return 'warning'
  return 'secondary'
}

function statusLabel(status) {
  if (status === 'active') return 'Активен'
  if (status === 'draft') return 'Черновик'
  return 'Архив'
}

const activeThemeLabel = computed(() =>
  `${themeStore.theme} → ${themeStore.resolvedTheme}`
)
</script>

<template>
  <main class="ui-preview">
    <UiToastHost />
    <header class="ui-preview__header">
      <div>
        <div class="ui-preview__eyebrow">CUSTOM UI PREVIEW</div>
        <h1>Ui-компоненты</h1>
        <p>
          Проверка наших PrimeVue-обёрток на общей --st-* палитре без изменения
          геометрии и поведения компонентов.
        </p>
      </div>

      <div class="ui-preview__theme-panel">
        <span class="ui-preview__theme-state">{{ activeThemeLabel }}</span>
        <div class="ui-preview__theme-actions">
          <button type="button" @click="themeStore.setTheme('light')">Light</button>
          <button type="button" @click="themeStore.setTheme('dark')">Dark</button>
          <button type="button" @click="themeStore.setTheme('system')">System</button>
        </div>
      </div>
    </header>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">ACTIONS</div>
          <h2>UiButton</h2>
        </div>
        <p>Primary, secondary, semantic и ghost-состояния.</p>
      </div>

      <div class="ui-preview__row">
        <UiButton variant="primary" label="Основная" icon="pi pi-check" />
        <UiButton variant="secondary" label="Вторичная" />
        <UiButton variant="success" label="Сохранено" icon="pi pi-check-circle" />
        <UiButton variant="danger" label="Удалить" icon="pi pi-trash" />
        <UiButton variant="ghost" label="Без рамки" />
        <UiButton variant="primary" label="Недоступно" disabled />
      </div>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">FORMS</div>
          <h2>Поля ввода</h2>
        </div>
        <p>Normal, readonly, disabled, error, search, textarea и select.</p>
      </div>

      <div class="ui-preview__form-grid">
        <UiInput v-model="name" label="Имя" hint="Обычное поле" />
        <UiInput v-model="email" label="Email" error="Пример ошибки валидации" />
        <UiInput v-model="readonlyValue" label="Readonly" readonly />
        <UiInput model-value="Недоступное поле" label="Disabled" disabled />
        <UiSearchInput v-model="search" label="Поиск" placeholder="Введите запрос" />
        <UiSelect
          v-model="subject"
          label="Предмет"
          :options="subjectOptions"
          filter
          clearable
        />
        <UiTextarea
          v-model="note"
          class="ui-preview__wide"
          label="Комментарий"
          :rows="4"
        />
      </div>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">CHOICE</div>
          <h2>Checkbox / Radio</h2>
        </div>
        <p>Проверка surface, selected, hover и disabled состояний.</p>
      </div>

      <div class="ui-preview__choice-stack">
        <div>
          <div class="ui-preview__choice-state">
            Binary checkbox: {{ accepted ? 'true' : 'false' }}
          </div>
          <div class="ui-preview__choice-grid">
            <UiCheckbox
              v-model="accepted"
              label="Уведомления включены"
              description="Кликабельна вся карточка, а не только квадрат"
            />
            <UiCheckbox
              v-model="disabledAccepted"
              label="Недоступный вариант"
              description="Disabled состояние не переключается"
              disabled
            />
          </div>
        </div>

        <div>
          <div class="ui-preview__choice-state">
            Multiple checkbox: {{ selectedModules.join(', ') || 'ничего' }}
          </div>
          <div class="ui-preview__choice-grid">
            <UiCheckbox
              v-model="selectedModules"
              mode="multiple"
              value="tests"
              label="Тесты"
              description="Первое значение массива"
            />
            <UiCheckbox
              v-model="selectedModules"
              mode="multiple"
              value="results"
              label="Результаты"
              description="Второе независимое значение массива"
            />
          </div>
        </div>

        <div>
          <div class="ui-preview__choice-state">
            Radio group: {{ role }}
          </div>
          <div class="ui-preview__choice-grid">
            <UiRadio
              v-model="role"
              value="student"
              name="preview-role"
              label="Студент"
              description="Нажатие по всей карточке выбирает этот radio"
            />
            <UiRadio
              v-model="role"
              value="teacher"
              name="preview-role"
              label="Преподаватель"
              description="Та же группа name, поэтому выбор взаимоисключающий"
            />
          </div>
        </div>
      </div>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">FEEDBACK</div>
          <h2>UiAlert / UiTag</h2>
        </div>
        <p>Semantic soft-поверхности без яркого белого фона.</p>
      </div>

      <div class="ui-preview__alert-grid">
        <UiAlert variant="info" title="Информация" message="Информационное сообщение." />
        <UiAlert variant="success" title="Готово" message="Операция завершена успешно." />
        <UiAlert variant="warning" title="Внимание" message="Проверьте данные перед продолжением." />
        <UiAlert variant="danger" title="Ошибка" message="Операцию выполнить не удалось." />
      </div>

      <div class="ui-preview__row ui-preview__tags">
        <UiTag variant="secondary" value="Черновик" />
        <UiTag variant="info" value="Информация" />
        <UiTag variant="success" value="Активен" />
        <UiTag variant="warning" value="Ожидает" />
        <UiTag variant="danger" value="Ошибка" />
      </div>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">SURFACE</div>
          <h2>UiCard</h2>
        </div>
        <p>Основная surface-карточка и вложенные Ui-компоненты.</p>
      </div>

      <div class="ui-preview__cards">
        <UiCard
          title="Карточка предмета"
          description="Базы данных · 21 студент"
        >
          <p class="ui-preview__muted">
            Карточка использует те же surface, border и text tokens, что и
            остальные кастомные компоненты.
          </p>
          <template #footer>
            <div class="ui-preview__row">
              <UiButton variant="primary" size="sm" label="Открыть" />
              <UiButton variant="secondary" size="sm" label="Подробнее" />
            </div>
          </template>
        </UiCard>

        <UiCard
          title="Состояния"
          description="Небольшая сводка"
          compact
        >
          <div class="ui-preview__metric">78%</div>
          <p class="ui-preview__muted">материалов опубликовано</p>
        </UiCard>
      </div>
    </section>


    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">UTILITY STATES</div>
          <h2>UiFileInput / UiEmptyState</h2>
        </div>
        <p>Нативный file control и пустые состояния на общей surface/text палитре.</p>
      </div>

      <div class="ui-preview__form-grid">
        <UiFileInput
          label="Материалы"
          hint="PDF, DOCX или изображения"
          accept=".pdf,.doc,.docx,image/*"
          multiple
        />
        <UiFileInput
          label="Файл с ошибкой"
          error="Выберите поддерживаемый файл"
          accept=".pdf"
        />
        <UiFileInput
          class="ui-preview__wide"
          label="Недоступная загрузка"
          hint="Disabled состояние"
          disabled
        />
      </div>

      <div class="ui-preview__empty-grid">
        <UiEmptyState
          title="Пока нет материалов"
          description="Здесь появятся прикреплённые к занятию файлы."
        >
          <template #actions>
            <UiButton variant="secondary" size="sm" label="Добавить материал" />
          </template>
        </UiEmptyState>

        <UiEmptyState
          title="Нет результатов"
          description="Измените параметры поиска или фильтра."
          compact
        />
      </div>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">COMPOSITE ACTIONS</div>
          <h2>UiToolbar / UiActionMenu</h2>
        </div>
        <p>Surface, border, hover, focus, popup и danger-состояния.</p>
      </div>

      <UiToolbar>
        <template #start>
          <UiButton variant="primary" label="Создать" icon="pi pi-plus" />
          <UiButton variant="secondary" label="Обновить" icon="pi pi-refresh" />
        </template>
        <template #end>
          <UiActionMenu :items="actionItems" aria-label="Действия preview" />
        </template>
      </UiToolbar>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">DATA</div>
          <h2>UiTable / Paginator</h2>
        </div>
        <p>Header, строки, striped/hover, pagination, active page и page-size select.</p>
      </div>

      <UiTable
        :columns="tableColumns"
        :rows="tableRows"
        :page-size="4"
        :page-size-options="[4, 8]"
        paginator
        striped
      >
        <template #cell-status="{ value }">
          <UiTag :variant="statusVariant(value)" :value="statusLabel(value)" />
        </template>
      </UiTable>
    </section>

    <section class="ui-preview__section">
      <div class="ui-preview__section-heading">
        <div>
          <div class="ui-preview__eyebrow">OVERLAYS</div>
          <h2>UiDialog / UiToastHost</h2>
        </div>
        <p>Overlay, elevated surface и semantic feedback поверх текущей темы.</p>
      </div>

      <div class="ui-preview__row">
        <UiButton
          variant="secondary"
          label="Открыть modal"
          icon="pi pi-window-maximize"
          @click="dialogVisible = true"
        />
        <UiButton
          variant="success"
          label="Success toast"
          @click="uiToast.success('Настройки успешно сохранены.')"
        />
        <UiButton
          variant="danger"
          label="Error toast"
          @click="uiToast.error('Не удалось выполнить операцию.')"
        />
      </div>

      <UiDialog v-model="dialogVisible" title="Проверка модального окна">
        <p class="ui-preview__muted">
          Dialog должен использовать ту же surface/text/border палитру, что и
          остальная рабочая область, без белой вспышки в dark mode.
        </p>
        <template #footer>
          <div class="ui-preview__row ui-preview__dialog-actions">
            <UiButton variant="secondary" label="Отмена" @click="dialogVisible = false" />
            <UiButton variant="primary" label="Подтвердить" @click="dialogVisible = false" />
          </div>
        </template>
      </UiDialog>
    </section>
  </main>
</template>

<style scoped>
.ui-preview {
  min-height: 100%;
  padding: 28px;
  display: grid;
  gap: 24px;
  color: var(--st-text);
  background: var(--st-page-bg);
}

.ui-preview__header,
.ui-preview__section {
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  background: var(--st-surface);
}

.ui-preview__header {
  padding: 22px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.ui-preview h1,
.ui-preview h2,
.ui-preview p {
  margin: 0;
}

.ui-preview h1 {
  margin-top: 5px;
  font-size: 28px;
}

.ui-preview h2 {
  margin-top: 4px;
  font-size: 20px;
}

.ui-preview__header p,
.ui-preview__section-heading > p,
.ui-preview__muted {
  color: var(--st-text-secondary);
}

.ui-preview__header p {
  margin-top: 8px;
  max-width: 760px;
  line-height: 1.55;
}

.ui-preview__eyebrow {
  color: var(--st-primary);
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.08em;
}

.ui-preview__theme-panel {
  display: grid;
  justify-items: end;
  gap: 8px;
}

.ui-preview__theme-state {
  color: var(--st-text-secondary);
  font-size: 12px;
}

.ui-preview__theme-actions,
.ui-preview__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ui-preview__theme-actions button {
  min-height: 32px;
  padding: 0 10px;
  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 7px;
  cursor: pointer;
}

.ui-preview__section {
  padding: 22px;
  display: grid;
  gap: 18px;
}

.ui-preview__section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.ui-preview__section-heading > p {
  max-width: 520px;
  text-align: right;
  font-size: 13px;
}

.ui-preview__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ui-preview__wide {
  grid-column: 1 / -1;
}

.ui-preview__choice-grid,
.ui-preview__alert-grid,
.ui-preview__cards,
.ui-preview__empty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}


.ui-preview__choice-stack {
  display: grid;
  gap: 16px;
}

.ui-preview__choice-state {
  margin-bottom: 8px;
  color: var(--st-text-secondary);
  font-size: 12px;
  font-weight: 650;
}

.ui-preview__tags {
  margin-top: 4px;
}

.ui-preview__dialog-actions {
  justify-content: flex-end;
}

.ui-preview__metric {
  margin-top: 2px;
  color: var(--st-text);
  font-size: 38px;
  font-weight: 750;
  line-height: 1;
}

.ui-preview__muted {
  line-height: 1.5;
}

@media (max-width: 760px) {
  .ui-preview {
    padding: 16px;
  }

  .ui-preview__header,
  .ui-preview__section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .ui-preview__theme-panel {
    justify-items: start;
  }

  .ui-preview__section-heading > p {
    text-align: left;
  }

  .ui-preview__form-grid,
  .ui-preview__choice-grid,
  .ui-preview__alert-grid,
  .ui-preview__cards,
  .ui-preview__empty-grid {
    grid-template-columns: 1fr;
  }
}
</style>
