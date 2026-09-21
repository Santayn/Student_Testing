<script setup>
import { ref } from 'vue'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Menu from 'primevue/menu'
import Message from 'primevue/message'
import Paginator from 'primevue/paginator'
import ProgressBar from 'primevue/progressbar'
import RadioButton from 'primevue/radiobutton'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import Toast from 'primevue/toast'
import ToggleSwitch from 'primevue/toggleswitch'
import { useToast } from 'primevue/usetoast'

import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const toast = useToast()

const textValue = ref('Пример значения')
const emptyValue = ref('')
const textareaValue = ref(
  'Многострочное поле для проверки текста, фона, границы и focus-состояния.'
)

const selectOptions = [
  { label: 'Алгоритмы', value: 'algorithms' },
  { label: 'Базы данных', value: 'databases' },
  { label: 'Компьютерные сети', value: 'networks' },
  { label: 'Операционные системы', value: 'os' },
]

const selectedSubject = ref('databases')
const checkboxValue = ref(true)
const radioValue = ref('teacher')
const switchValue = ref(true)
const dialogVisible = ref(false)
const menu = ref(null)
const paginatorFirst = ref(0)

const menuItems = [
  {
    label: 'Открыть',
    icon: 'pi pi-external-link',
  },
  {
    label: 'Дублировать',
    icon: 'pi pi-copy',
  },
  {
    separator: true,
  },
  {
    label: 'Удалить',
    icon: 'pi pi-trash',
  },
]

const rows = [
  {
    id: 1,
    name: 'Алгоритмы и структуры данных',
    status: 'Активен',
    students: 28,
  },
  {
    id: 2,
    name: 'Базы данных',
    status: 'Черновик',
    students: 21,
  },
  {
    id: 3,
    name: 'Компьютерные сети',
    status: 'Активен',
    students: 31,
  },
  {
    id: 4,
    name: 'Операционные системы',
    status: 'Архив',
    students: 19,
  },
]

function setTheme(theme) {
  themeStore.setTheme(theme)
}

function toggleMenu(event) {
  menu.value?.toggle(event)
}

function showToast(severity) {
  const labels = {
    success: 'Операция выполнена',
    info: 'Информационное сообщение',
    warn: 'Требуется внимание',
    error: 'Произошла ошибка',
  }

  toast.add({
    severity,
    summary: labels[severity],
    detail: 'Preview системного уведомления PrimeVue.',
    life: 2600,
  })
}
</script>

<template>
  <main class="prime-preview">
    <Toast />

    <header class="prime-preview__header">
      <div>
        <p class="prime-preview__eyebrow">
          Student Testing · PrimeVue laboratory
        </p>
        <h1>PrimeVue Preview</h1>
        <p class="prime-preview__lead">
          Чистые PrimeVue-компоненты без Ui*-обёрток. Страница показывает,
          как текущий PrimeVue preset сочетается с общей light/dark палитрой.
        </p>
      </div>

      <div class="theme-panel" aria-label="Выбор темы">
        <span class="theme-panel__status">
          {{ themeStore.theme }} → {{ themeStore.resolvedTheme }}
        </span>

        <div class="theme-panel__buttons">
          <button
            type="button"
            :class="{ 'is-active': themeStore.theme === 'light' }"
            @click="setTheme('light')"
          >
            Светлая
          </button>
          <button
            type="button"
            :class="{ 'is-active': themeStore.theme === 'dark' }"
            @click="setTheme('dark')"
          >
            Тёмная
          </button>
          <button
            type="button"
            :class="{ 'is-active': themeStore.theme === 'system' }"
            @click="setTheme('system')"
          >
            Системная
          </button>
        </div>
      </div>
    </header>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Actions</p>
          <h2>Button</h2>
        </div>
        <p>Основные severity, outlined, text и disabled состояния.</p>
      </div>

      <div class="preview-row">
        <Button label="Primary" icon="pi pi-check" />
        <Button label="Secondary" severity="secondary" />
        <Button label="Success" severity="success" />
        <Button label="Warning" severity="warn" />
        <Button label="Danger" severity="danger" />
        <Button label="Outlined" outlined />
        <Button label="Text" text />
        <Button label="Disabled" disabled />
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Inputs</p>
          <h2>Form controls</h2>
        </div>
        <p>Обычные, placeholder, disabled, invalid и overlay-состояния.</p>
      </div>

      <div class="control-grid">
        <label class="control-sample">
          <span>InputText</span>
          <InputText v-model="textValue" />
        </label>

        <label class="control-sample">
          <span>Placeholder</span>
          <InputText v-model="emptyValue" placeholder="Введите значение" />
        </label>

        <label class="control-sample">
          <span>Invalid</span>
          <InputText model-value="Некорректное значение" invalid />
        </label>

        <label class="control-sample">
          <span>Disabled</span>
          <InputText model-value="Недоступно" disabled />
        </label>

        <label class="control-sample control-sample--wide">
          <span>Select + popup</span>
          <Select
            v-model="selectedSubject"
            :options="selectOptions"
            option-label="label"
            option-value="value"
            placeholder="Выберите предмет"
            filter
            show-clear
          />
        </label>

        <label class="control-sample control-sample--wide">
          <span>Textarea</span>
          <Textarea v-model="textareaValue" rows="4" auto-resize />
        </label>
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Choice</p>
          <h2>Checkbox / Radio / ToggleSwitch</h2>
        </div>
        <p>Проверка активного, неактивного и disabled состояния.</p>
      </div>

      <div class="choice-grid">
        <div class="choice-sample">
          <Checkbox v-model="checkboxValue" input-id="preview-checkbox" binary />
          <label for="preview-checkbox">Активный checkbox</label>
        </div>

        <div class="choice-sample">
          <Checkbox :model-value="false" input-id="preview-checkbox-disabled" binary disabled />
          <label for="preview-checkbox-disabled">Disabled checkbox</label>
        </div>

        <div class="choice-sample">
          <RadioButton v-model="radioValue" input-id="preview-radio-student" value="student" />
          <label for="preview-radio-student">Студент</label>
        </div>

        <div class="choice-sample">
          <RadioButton v-model="radioValue" input-id="preview-radio-teacher" value="teacher" />
          <label for="preview-radio-teacher">Преподаватель</label>
        </div>

        <div class="choice-sample">
          <ToggleSwitch v-model="switchValue" input-id="preview-switch" />
          <label for="preview-switch">Включённый ToggleSwitch</label>
        </div>
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Semantic</p>
          <h2>Tag / Message / Progress</h2>
        </div>
        <p>Семантические цвета на поверхности текущей темы.</p>
      </div>

      <div class="preview-row">
        <Tag value="Secondary" severity="secondary" />
        <Tag value="Info" severity="info" />
        <Tag value="Success" severity="success" />
        <Tag value="Warning" severity="warn" />
        <Tag value="Danger" severity="danger" />
      </div>

      <div class="message-grid">
        <Message severity="info" :closable="false">Информационное сообщение</Message>
        <Message severity="success" :closable="false">Операция выполнена</Message>
        <Message severity="warn" :closable="false">Требуется внимание</Message>
        <Message severity="error" :closable="false">Ошибка операции</Message>
      </div>

      <div class="progress-sample">
        <span>ProgressBar · 62%</span>
        <ProgressBar :value="62" />
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Surface</p>
          <h2>Card</h2>
        </div>
        <p>Поверхность, заголовок, secondary text и action area.</p>
      </div>

      <div class="card-grid">
        <Card>
          <template #title>Карточка предмета</template>
          <template #subtitle>Базы данных · 21 студент</template>
          <template #content>
            <p class="card-copy">
              Карточка показывает стандартный PrimeVue Card без наших UI-обёрток.
            </p>
          </template>
          <template #footer>
            <div class="preview-row preview-row--compact">
              <Button label="Открыть" size="small" />
              <Button label="Подробнее" size="small" severity="secondary" outlined />
            </div>
          </template>
        </Card>

        <Card>
          <template #title>Состояние курса</template>
          <template #subtitle>Текущая версия</template>
          <template #content>
            <div class="card-stat">
              <strong>78%</strong>
              <span>материалов опубликовано</span>
            </div>
          </template>
        </Card>
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Data</p>
          <h2>DataTable / Paginator</h2>
        </div>
        <p>Границы, заголовок таблицы, строки, hover и pagination.</p>
      </div>

      <DataTable :value="rows" striped-rows show-gridlines>
        <Column field="name" header="Предмет" />
        <Column field="status" header="Статус" />
        <Column field="students" header="Студентов" />
      </DataTable>

      <div class="paginator-wrap">
        <Paginator
          :first="paginatorFirst"
          :rows="5"
          :total-records="42"
          @page="paginatorFirst = $event.first"
        />
      </div>
    </section>

    <section class="preview-section">
      <div class="preview-section__heading">
        <div>
          <p class="preview-section__kicker">Overlay</p>
          <h2>Dialog / Menu / Toast</h2>
        </div>
        <p>Overlay-поверхности особенно важны для проверки dark theme.</p>
      </div>

      <div class="preview-row">
        <Button label="Открыть Dialog" icon="pi pi-window-maximize" @click="dialogVisible = true" />
        <Button label="Открыть Menu" icon="pi pi-ellipsis-v" severity="secondary" @click="toggleMenu" />
        <Button label="Success Toast" severity="success" outlined @click="showToast('success')" />
        <Button label="Warning Toast" severity="warn" outlined @click="showToast('warn')" />
        <Button label="Error Toast" severity="danger" outlined @click="showToast('error')" />
      </div>

      <Menu ref="menu" :model="menuItems" popup />

      <Dialog
        v-model:visible="dialogVisible"
        modal
        header="PrimeVue Dialog"
        :style="{ width: 'min(520px, calc(100vw - 32px))' }"
      >
        <p class="dialog-copy">
          Проверяем фон окна, header, основной текст, границы, overlay и footer
          на текущей теме.
        </p>

        <template #footer>
          <Button label="Отмена" severity="secondary" text @click="dialogVisible = false" />
          <Button label="Подтвердить" @click="dialogVisible = false" />
        </template>
      </Dialog>
    </section>
  </main>
</template>

<style scoped>
.prime-preview {
  width: min(100%, var(--st-content-width));
  margin: 0 auto;
  padding: 32px var(--st-space-page) 64px;
  display: grid;
  gap: 24px;
  color: var(--st-text);
}

.prime-preview__header,
.preview-section {
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.prime-preview__header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.prime-preview__eyebrow,
.preview-section__kicker {
  margin: 0 0 6px;
  color: var(--st-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.prime-preview h1,
.preview-section h2 {
  margin: 0;
  color: var(--st-text);
}

.prime-preview h1 {
  font-size: clamp(28px, 4vw, 40px);
}

.prime-preview__lead {
  max-width: 760px;
  margin: 10px 0 0;
  color: var(--st-text-secondary);
  line-height: 1.65;
}

.theme-panel {
  min-width: 280px;
  padding: 12px;
  display: grid;
  gap: 10px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.theme-panel__status {
  color: var(--st-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.theme-panel__buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.theme-panel__buttons button {
  min-height: 34px;
  padding: 0 10px;
  color: var(--st-text-secondary);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 7px;
  cursor: pointer;
}

.theme-panel__buttons button:hover,
.theme-panel__buttons button.is-active {
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-color: color-mix(in srgb, var(--st-primary) 55%, var(--st-border));
}

.preview-section {
  padding: 22px;
  display: grid;
  gap: 20px;
}

.preview-section__heading {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
}

.preview-section__heading > p {
  max-width: 520px;
  margin: 0;
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  text-align: right;
}

.preview-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.preview-row--compact {
  gap: 8px;
}

.control-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.control-sample {
  min-width: 0;
  display: grid;
  gap: 7px;
  color: var(--st-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.control-sample--wide {
  grid-column: 1 / -1;
}

.control-sample > :deep(.p-inputtext),
.control-sample > :deep(.p-select),
.control-sample > :deep(.p-textarea) {
  width: 100%;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.choice-sample {
  min-height: 48px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.choice-sample label {
  cursor: pointer;
}

.message-grid,
.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.progress-sample {
  display: grid;
  gap: 8px;
  color: var(--st-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.card-copy,
.dialog-copy {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.6;
}

.card-stat {
  display: grid;
  gap: 4px;
}

.card-stat strong {
  color: var(--st-text);
  font-size: 32px;
}

.card-stat span {
  color: var(--st-text-secondary);
}

.paginator-wrap {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

@media (max-width: 900px) {
  .prime-preview__header,
  .preview-section__heading {
    align-items: stretch;
    flex-direction: column;
  }

  .preview-section__heading > p {
    text-align: left;
  }

  .theme-panel {
    min-width: 0;
  }

  .choice-grid,
  .message-grid,
  .card-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .prime-preview {
    padding: 18px 14px 40px;
  }

  .prime-preview__header,
  .preview-section {
    padding: 16px;
  }

  .control-grid,
  .choice-grid,
  .message-grid,
  .card-grid {
    grid-template-columns: 1fr;
  }

  .control-sample--wide {
    grid-column: auto;
  }
}
</style>
