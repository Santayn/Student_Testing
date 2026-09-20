<script setup>
import {
  ref,
} from 'vue'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Menu from 'primevue/menu'
import Password from 'primevue/password'
import ProgressBar from 'primevue/progressbar'
import RadioButton from 'primevue/radiobutton'
import Select from 'primevue/select'
import Skeleton from 'primevue/skeleton'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useToast } from 'primevue/usetoast'

import {
  UiActionMenu,
  UiButton,
  UiCard,
  UiDialog,
  UiInput,
  UiSearchInput,
  UiSelect,
  UiTable,
  UiTag,
  UiToastHost,
  UiToolbar,
} from '@/components/ui'

import {
  useThemeStore,
} from '@/stores/theme'

const toast = useToast()
const themeStore = useThemeStore()

const dialogVisible = ref(false)
const actionMenu = ref(null)
const activeActionTarget = ref(null)
const name = ref('Контрольная работа № 2')
const password = ref('student-testing')
const description = ref(
  'Проверка знаний по темам нормализации и SQL-запросов.'
)
const duration = ref(45)
const faculty = ref(null)
const accepted = ref(true)
const role = ref('student')
const selectedTeacher = ref('Тиликин А. Ю.')
const foundationSearch = ref('')
const foundationTitle = ref('Лабораторная работа')
const foundationFaculty = ref('IT')
const foundationDialogVisible = ref(false)

const faculties = [
  {
    name: 'Информационные технологии',
    code: 'IT',
  },
  {
    name: 'Кибербезопасность',
    code: 'KB',
  },
  {
    name: 'Экономика',
    code: 'EC',
  },
]

const students = [
  {
    id: 1,
    name: 'Анна Смирнова',
    group: '23-КБ',
    result: 92,
    status: 'Активен',
  },
  {
    id: 2,
    name: 'Иван Петров',
    group: '23-КБ',
    result: 78,
    status: 'Черновик',
  },
  {
    id: 3,
    name: 'Мария Волкова',
    group: '24-ИТ',
    result: 0,
    status: 'Приостановлен',
  },
]

const foundationColumns = [
  { key: 'name', label: 'Студент' },
  { key: 'group', label: 'Группа' },
  { key: 'status', label: 'Статус', sortable: false },
  { key: 'actions', label: '', sortable: false, style: { width: '56px' } },
]

function foundationRowActions(row) {
  return [
    {
      label: 'Открыть',
      icon: 'pi pi-external-link',
      command: () => toast.add({ severity: 'info', summary: 'Открыть', detail: row.name, life: 2200 }),
    },
    {
      label: 'Изменить',
      icon: 'pi pi-pencil',
      command: () => toast.add({ severity: 'info', summary: 'Изменить', detail: row.name, life: 2200 }),
    },
    { separator: true },
    {
      label: 'Удалить',
      icon: 'pi pi-trash',
      danger: true,
      command: () => toast.add({ severity: 'warn', summary: 'Удалить', detail: row.name, life: 2200 }),
    },
  ]
}

const navItems = [
  {
    icon: 'pi pi-home',
    label: 'Главная',
  },
  {
    icon: 'pi pi-book',
    label: 'Предметы',
    active: true,
  },
  {
    icon: 'pi pi-chart-bar',
    label: 'Результаты',
  },
  {
    icon: 'pi pi-file-edit',
    label: 'Тесты',
  },
]

const palette = [
  {
    name: 'Primary',
    value: '#2563EB',
    style: 'background:#2563EB;color:white',
  },
  {
    name: 'Navy',
    value: '#0F172A',
    style: 'background:#0F172A;color:white',
  },
  {
    name: 'Page',
    value: '#F8FAFC',
    style: 'background:#F8FAFC;color:#0F172A',
  },
  {
    name: 'Surface',
    value: '#FFFFFF',
    style: 'background:#FFFFFF;color:#0F172A',
  },
  {
    name: 'Border',
    value: '#E2E8F0',
    style: 'background:#E2E8F0;color:#0F172A',
  },
  {
    name: 'Success',
    value: '#16A34A',
    style: 'background:#16A34A;color:white',
  },
  {
    name: 'Warning',
    value: '#D97706',
    style: 'background:#D97706;color:white',
  },
  {
    name: 'Danger',
    value: '#DC2626',
    style: 'background:#DC2626;color:white',
  },
]


const actionMenuItems = [
  {
    label: 'Открыть',
    icon: 'pi pi-external-link',
    command: () => runShowcaseAction('Открыть'),
  },
  {
    label: 'Изменить',
    icon: 'pi pi-pencil',
    command: () => runShowcaseAction('Изменить'),
  },
  {
    separator: true,
  },
  {
    label: 'Приостановить',
    icon: 'pi pi-pause',
    command: () => runShowcaseAction('Приостановить'),
  },
  {
    label: 'Удалить',
    icon: 'pi pi-trash',
    class: 'showcase-menu-danger',
    command: () => runShowcaseAction('Удалить'),
  },
]

function actionTargetName(target) {
  if (typeof target === 'string') {
    return target
  }

  return target?.name ?? target?.label ?? 'запись'
}

function openActionMenu(event, target) {
  activeActionTarget.value = target
  actionMenu.value?.toggle(event)
}

function runShowcaseAction(action) {
  const targetName = actionTargetName(activeActionTarget.value)

  toast.add({
    severity: action === 'Удалить' ? 'warn' : 'info',
    summary: action,
    detail: `${targetName}: пример действия из контекстного меню.`,
    life: 2600,
  })
}

function statusSeverity(status) {
  if (status === 'Активен') {
    return 'success'
  }

  if (status === 'Приостановлен') {
    return 'warn'
  }

  return 'secondary'
}

function showToast() {
  toast.add({
    severity: 'success',
    summary: 'Готово',
    detail:
      'Настройки сохранены. Это пример системного уведомления.',
    life: 3000,
  })
}
</script>

<template>
  <main
    class="mx-auto grid w-full max-w-[var(--st-content-width)] gap-6 p-4 md:p-6 xl:p-8"
  >
    <UiToastHost />
    <Menu
      id="showcase-actions-menu"
      ref="actionMenu"
      :model="actionMenuItems"
      popup
    />

    <section
      class="rounded-[var(--st-radius-card)] border border-surface-200 bg-surface-0 p-5 shadow-[var(--st-shadow-card)] dark:border-surface-700 dark:bg-surface-900 md:p-6"
    >
      <div
        class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between"
      >
        <div class="max-w-3xl">
          <p
            class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-primary"
          >
            Academic Navy · визуальная проверка
          </p>

          <h1
            class="m-0 text-3xl font-bold tracking-tight text-surface-950 dark:text-surface-0 md:text-4xl"
          >
            Student Testing Design System
          </h1>

          <p
            class="mt-3 text-sm leading-6 text-surface-600 dark:text-surface-300"
          >
            Тестовый полигон PrimeVue + Tailwind. Здесь проверяем палитру,
            геометрию, плотность административного интерфейса и более
            свободную student-подачу до миграции рабочих страниц.
          </p>
        </div>

        <div class="flex flex-wrap gap-2">
          <Button
            icon="pi pi-sun"
            label="Светлая"
            severity="secondary"
            outlined
            @click="themeStore.setTheme('light')"
          />

          <Button
            icon="pi pi-moon"
            label="Тёмная"
            severity="secondary"
            outlined
            @click="themeStore.setTheme('dark')"
          />

          <Button
            icon="pi pi-desktop"
            label="Системная"
            severity="secondary"
            text
            @click="themeStore.setTheme('system')"
          />
        </div>
      </div>
    </section>

    <UiCard
      title="Этап 24 · UI foundation"
      description="Общий проектный слой поверх PrimeVue. Будущие business views должны использовать эти Ui-компоненты, а не primevue/* напрямую."
    >
      <div class="grid gap-4">
        <UiToolbar>
          <template #start>
            <div class="w-full sm:w-80">
              <UiSearchInput
                v-model="foundationSearch"
                placeholder="Поиск по студентам"
              />
            </div>
          </template>

          <template #end>
            <UiButton
              variant="secondary"
              icon="pi pi-filter-slash"
              label="Сбросить"
              @click="foundationSearch = ''"
            />
            <UiButton
              variant="primary"
              icon="pi pi-plus"
              label="Создать"
              @click="foundationDialogVisible = true"
            />
          </template>
        </UiToolbar>

        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <UiInput
            v-model="foundationTitle"
            label="Название"
            hint="Единый field shell и focus-state."
          />
          <UiSelect
            v-model="foundationFaculty"
            label="Факультет"
            :options="[
              { label: 'Информационные технологии', value: 'IT' },
              { label: 'Кибербезопасность', value: 'KB' },
            ]"
          />
          <div class="grid content-start gap-2">
            <span class="text-xs font-semibold text-muted-color">Статусы</span>
            <div class="flex flex-wrap gap-2">
              <UiTag value="Активен" variant="success" />
              <UiTag value="Черновик" />
              <UiTag value="Приостановлен" variant="warning" />
            </div>
          </div>
          <div class="grid content-start gap-2">
            <span class="text-xs font-semibold text-muted-color">Действия</span>
            <div class="flex gap-2">
              <UiButton label="Primary" variant="primary" />
              <UiActionMenu :items="foundationRowActions(students[0])" />
            </div>
          </div>
        </div>

        <UiTable
          :columns="foundationColumns"
          :rows="students"
          empty-message="Студенты не найдены."
        >
          <template #cell-status="{ row }">
            <UiTag
              :value="row.status"
              :variant="
                row.status === 'Активен'
                  ? 'success'
                  : row.status === 'Приостановлен'
                    ? 'warning'
                    : 'secondary'
              "
            />
          </template>

          <template #cell-actions="{ row }">
            <div class="flex justify-end">
              <UiActionMenu
                :items="foundationRowActions(row)"
                :aria-label="`Действия: ${row.name}`"
              />
            </div>
          </template>
        </UiTable>
      </div>
    </UiCard>

    <UiDialog
      v-model="foundationDialogVisible"
      title="Новая запись"
    >
      <div class="grid gap-4">
        <UiInput v-model="foundationTitle" label="Название" />
        <UiSelect
          v-model="foundationFaculty"
          label="Факультет"
          :options="[
            { label: 'Информационные технологии', value: 'IT' },
            { label: 'Кибербезопасность', value: 'KB' },
          ]"
        />
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <UiButton
            variant="secondary"
            label="Отмена"
            @click="foundationDialogVisible = false"
          />
          <UiButton
            variant="primary"
            label="Сохранить"
            icon="pi pi-check"
            @click="foundationDialogVisible = false"
          />
        </div>
      </template>
    </UiDialog>

    <Card>
      <template #title>
        Палитра Academic Navy
      </template>

      <template #subtitle>
        Один холодный синий accent, navy-каркас и спокойные slate-поверхности.
      </template>

      <template #content>
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div
            v-for="color in palette"
            :key="color.name"
            class="overflow-hidden rounded-[var(--st-radius-card)] border border-surface-200 dark:border-surface-700"
          >
            <div
              class="flex h-20 items-end p-3 text-xs font-semibold"
              :style="color.style"
            >
              {{ color.name }}
            </div>
            <div class="bg-surface-0 px-3 py-2 text-xs dark:bg-surface-900">
              <strong>{{ color.value }}</strong>
            </div>
          </div>
        </div>
      </template>
    </Card>

    <Card>
      <template #title>
        Каркас приложения
      </template>

      <template #subtitle>
        Header 64 px · sidebar 256 px · светлая рабочая область · плотный admin/teacher content.
      </template>

      <template #content>
        <div
          class="overflow-hidden rounded-[var(--st-radius-card)] border border-surface-200 bg-[var(--st-page-bg)] shadow-[var(--st-shadow-card)] dark:border-surface-700"
        >
          <div
            class="flex h-[var(--st-header-height)] items-center justify-between border-b border-[var(--st-shell-border)] bg-[var(--st-shell-bg)] px-4 text-[var(--st-shell-text)] md:px-5"
          >
            <div class="flex items-center gap-3 font-semibold">
              <div
                class="grid size-9 place-items-center rounded-lg bg-primary text-primary-contrast"
              >
                <i class="pi pi-graduation-cap" />
              </div>
              <span>Student Testing</span>
            </div>

            <div class="flex items-center gap-2">
              <span
                class="hidden rounded-lg border border-[var(--st-shell-border)] bg-[var(--st-shell-elevated)] px-3 py-2 text-xs font-medium sm:inline"
              >
                Преподаватель
              </span>
              <button
                type="button"
                class="grid size-9 place-items-center rounded-full border border-[var(--st-shell-border)] bg-[var(--st-shell-elevated)] text-sm font-bold"
                aria-label="Профиль"
              >
                АТ
              </button>
            </div>
          </div>

          <div class="grid min-h-[500px] md:grid-cols-[256px_minmax(0,1fr)]">
            <aside
              class="hidden border-r border-[var(--st-shell-border)] bg-[var(--st-shell-bg)] p-3 text-[var(--st-shell-text)] md:block"
            >
              <p
                class="mb-2 mt-3 px-3 text-[11px] font-bold uppercase tracking-[0.12em] text-[var(--st-shell-muted)]"
              >
                Обучение
              </p>

              <nav class="grid gap-1">
                <button
                  v-for="item in navItems"
                  :key="item.label"
                  type="button"
                  class="relative flex min-h-10 w-full appearance-none items-center gap-3 rounded-lg border-0 bg-transparent px-3 text-left text-sm outline-none transition-colors focus-visible:bg-[var(--st-shell-hover)] focus-visible:text-white"
                  :class="item.active
                    ? 'bg-[var(--st-shell-active)] font-semibold text-white before:absolute before:inset-y-2 before:left-0 before:w-[3px] before:rounded-full before:bg-primary'
                    : 'text-[var(--st-shell-muted)] hover:bg-[var(--st-shell-hover)] hover:text-white'"
                >
                  <i :class="item.icon" />
                  <span>{{ item.label }}</span>
                </button>
              </nav>

              <p
                class="mb-2 mt-6 px-3 text-[11px] font-bold uppercase tracking-[0.12em] text-[var(--st-shell-muted)]"
              >
                Преподаватель
              </p>

              <div class="grid gap-1">
                <button
                  type="button"
                  class="flex min-h-10 items-center gap-3 rounded-lg border-0 bg-transparent px-3 text-sm text-[var(--st-shell-muted)] outline-none transition-colors hover:bg-[var(--st-shell-hover)] hover:text-white focus-visible:bg-[var(--st-shell-hover)] focus-visible:text-white"
                >
                  <i class="pi pi-file-edit" />
                  Банк вопросов
                </button>
                <button
                  type="button"
                  class="flex min-h-10 items-center gap-3 rounded-lg border-0 bg-transparent px-3 text-sm text-[var(--st-shell-muted)] outline-none transition-colors hover:bg-[var(--st-shell-hover)] hover:text-white focus-visible:bg-[var(--st-shell-hover)] focus-visible:text-white"
                >
                  <i class="pi pi-bookmark" />
                  Шаблоны
                </button>
              </div>
            </aside>

            <section class="min-w-0 bg-[var(--st-page-bg)] p-4 md:p-6">
              <div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <p class="m-0 text-sm text-[var(--st-text-secondary)]">
                    Кибербезопасность · 23-КБ
                  </p>
                  <h2 class="mb-0 mt-1 text-2xl font-bold text-[var(--st-text)]">
                    Предметы преподавателя
                  </h2>
                  <p class="mb-0 mt-2 text-sm text-[var(--st-text-secondary)]">
                    Назначения и текущая учебная нагрузка.
                  </p>
                </div>

                <Button
                  icon="pi pi-plus"
                  label="Назначить предмет"
                />
              </div>

              <div
                class="mb-4 grid gap-3 rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface)] p-4 shadow-[var(--st-shadow-card)] md:grid-cols-[1fr_220px_auto]"
              >
                <div class="showcase-search-control w-full">
                  <span class="showcase-search-icon" aria-hidden="true">
                    <i class="pi pi-search" />
                  </span>
                  <InputText
                    class="showcase-search-input w-full"
                    placeholder="Поиск по предметам"
                    aria-label="Поиск по предметам"
                  />
                </div>

                <Select
                  v-model="selectedTeacher"
                  :options="['Тиликин А. Ю.', 'Петров И. В.']"
                  fluid
                />

                <Button
                  icon="pi pi-filter-slash"
                  label="Сбросить"
                  severity="secondary"
                  outlined
                />
              </div>

              <div
                class="overflow-hidden rounded-[var(--st-radius-card)] border border-[var(--st-border)] bg-[var(--st-surface)] shadow-[var(--st-shadow-card)]"
              >
                <div class="overflow-x-auto">
                  <table class="w-full min-w-[640px] border-collapse text-left text-sm">
                    <thead class="bg-[var(--st-surface-muted)] text-[var(--st-text-secondary)]">
                      <tr>
                        <th class="px-4 py-3 font-semibold">Предмет</th>
                        <th class="px-4 py-3 font-semibold">Факультет</th>
                        <th class="px-4 py-3 font-semibold">Статус</th>
                        <th class="w-14 px-4 py-3 text-right font-semibold">Действия</th>
                      </tr>
                    </thead>
                    <tbody class="text-[var(--st-text)]">
                      <tr class="border-t border-[var(--st-border)]">
                        <td class="px-4 py-3 font-medium">Базы данных</td>
                        <td class="px-4 py-3 text-[var(--st-text-secondary)]">ИТ</td>
                        <td class="px-4 py-3">
                          <Tag value="Активен" severity="success" />
                        </td>
                        <td class="px-4 py-2 text-right">
                          <Button
                            icon="pi pi-ellipsis-v"
                            severity="secondary"
                            text
                            rounded
                            aria-label="Действия с предметом Базы данных"
                            aria-haspopup="true"
                            aria-controls="showcase-actions-menu"
                            @click="openActionMenu($event, 'Базы данных')"
                          />
                        </td>
                      </tr>
                      <tr class="border-t border-[var(--st-border)]">
                        <td class="px-4 py-3 font-medium">Java</td>
                        <td class="px-4 py-3 text-[var(--st-text-secondary)]">ИТ</td>
                        <td class="px-4 py-3">
                          <Tag value="Активен" severity="success" />
                        </td>
                        <td class="px-4 py-2 text-right">
                          <Button
                            icon="pi pi-ellipsis-v"
                            severity="secondary"
                            text
                            rounded
                            aria-label="Действия с предметом Java"
                            aria-haspopup="true"
                            aria-controls="showcase-actions-menu"
                            @click="openActionMenu($event, 'Java')"
                          />
                        </td>
                      </tr>
                      <tr class="border-t border-[var(--st-border)]">
                        <td class="px-4 py-3 font-medium">Компьютерные сети</td>
                        <td class="px-4 py-3 text-[var(--st-text-secondary)]">КБ</td>
                        <td class="px-4 py-3">
                          <Tag value="Приостановлен" severity="warn" />
                        </td>
                        <td class="px-4 py-2 text-right">
                          <Button
                            icon="pi pi-ellipsis-v"
                            severity="secondary"
                            text
                            rounded
                            aria-label="Действия с предметом Компьютерные сети"
                            aria-haspopup="true"
                            aria-controls="showcase-actions-menu"
                            @click="openActionMenu($event, 'Компьютерные сети')"
                          />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          </div>
        </div>
      </template>
    </Card>

    <section class="grid gap-4 lg:grid-cols-3">
      <Card>
        <template #title>
          Геометрия
        </template>

        <template #content>
          <dl class="grid gap-3 text-sm">
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Header</dt>
              <dd class="m-0 font-semibold">64 px</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Sidebar</dt>
              <dd class="m-0 font-semibold">256 / 72 px</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Control radius</dt>
              <dd class="m-0 font-semibold">8 px</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Card radius</dt>
              <dd class="m-0 font-semibold">12 px</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Dialog radius</dt>
              <dd class="m-0 font-semibold">16 px</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-muted-color">Control height</dt>
              <dd class="m-0 font-semibold">40 px</dd>
            </div>
          </dl>
        </template>
      </Card>

      <Card>
        <template #title>
          Статусы
        </template>

        <template #content>
          <div class="grid gap-3">
            <div class="flex items-center justify-between gap-3">
              <span class="text-sm text-muted-color">Текущая сущность</span>
              <Tag value="Активен" severity="success" />
            </div>
            <div class="flex items-center justify-between gap-3">
              <span class="text-sm text-muted-color">Работа не опубликована</span>
              <Tag value="Черновик" severity="secondary" />
            </div>
            <div class="flex items-center justify-between gap-3">
              <span class="text-sm text-muted-color">Временно недоступно</span>
              <Tag value="Приостановлен" severity="warn" />
            </div>
            <div class="flex items-center justify-between gap-3">
              <span class="text-sm text-muted-color">Требует внимания</span>
              <Tag value="Ошибка" severity="danger" />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          Кнопки
        </template>

        <template #content>
          <div class="flex flex-wrap gap-2">
            <Button icon="pi pi-plus" label="Создать" />
            <Button icon="pi pi-save" label="Сохранить" severity="secondary" />
            <Button icon="pi pi-trash" label="Удалить" severity="danger" outlined />
            <Button icon="pi pi-ellipsis-h" severity="secondary" text rounded aria-label="Ещё" />
          </div>
        </template>
      </Card>
    </section>

    <section class="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
      <Card>
        <template #title>
          Форма
        </template>

        <template #subtitle>
          Спокойная вертикальная структура; две колонки только там, где поля связаны.
        </template>

        <template #content>
          <div class="grid gap-5 md:grid-cols-2">
            <label class="grid gap-2 text-sm font-semibold">
              Название теста
              <InputText v-model="name" fluid />
            </label>

            <label class="grid gap-2 text-sm font-semibold">
              Пароль
              <Password
                v-model="password"
                :feedback="false"
                toggle-mask
                fluid
              />
            </label>

            <label class="grid gap-2 text-sm font-semibold">
              Факультет
              <Select
                v-model="faculty"
                :options="faculties"
                option-label="name"
                placeholder="Выберите факультет"
                fluid
              />
            </label>

            <label class="grid gap-2 text-sm font-semibold">
              Длительность, минут
              <InputNumber
                v-model="duration"
                :min="1"
                :max="300"
                show-buttons
                fluid
              />
            </label>

            <label class="grid gap-2 text-sm font-semibold md:col-span-2">
              Описание
              <Textarea
                v-model="description"
                rows="4"
                auto-resize
                fluid
              />
            </label>

            <div class="flex flex-wrap items-center gap-5 md:col-span-2">
              <label class="flex items-center gap-2 text-sm font-medium">
                <Checkbox v-model="accepted" binary />
                Опубликован
              </label>

              <label class="flex items-center gap-2 text-sm font-medium">
                <RadioButton
                  v-model="role"
                  name="showcase-role"
                  value="student"
                />
                Студент
              </label>

              <label class="flex items-center gap-2 text-sm font-medium">
                <RadioButton
                  v-model="role"
                  name="showcase-role"
                  value="teacher"
                />
                Преподаватель
              </label>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          Student-подача
        </template>

        <template #subtitle>
          Та же система, но больше воздуха и меньше административной плотности.
        </template>

        <template #content>
          <div class="grid gap-4 sm:grid-cols-2">
            <article
              class="rounded-[var(--st-radius-card)] border border-surface-200 bg-surface-0 p-5 shadow-[var(--st-shadow-card)] dark:border-surface-700 dark:bg-surface-900"
            >
              <div class="mb-6 flex items-start justify-between gap-3">
                <div class="grid size-10 place-items-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-200">
                  <i class="pi pi-database" />
                </div>
                <Tag value="3 теста" severity="info" />
              </div>
              <h3 class="m-0 text-lg font-semibold">Базы данных</h3>
              <p class="mb-5 mt-2 text-sm leading-6 text-muted-color">
                8 лекций · следующий тест доступен до пятницы.
              </p>
              <Button label="Продолжить" icon="pi pi-arrow-right" icon-pos="right" text />
            </article>

            <article
              class="rounded-[var(--st-radius-card)] border border-surface-200 bg-surface-0 p-5 shadow-[var(--st-shadow-card)] dark:border-surface-700 dark:bg-surface-900"
            >
              <div class="mb-6 flex items-start justify-between gap-3">
                <div class="grid size-10 place-items-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-200">
                  <i class="pi pi-code" />
                </div>
                <Tag value="5 тестов" severity="info" />
              </div>
              <h3 class="m-0 text-lg font-semibold">Java</h3>
              <p class="mb-5 mt-2 text-sm leading-6 text-muted-color">
                12 лекций · последнее занятие завершено.
              </p>
              <Button label="Открыть" icon="pi pi-arrow-right" icon-pos="right" text />
            </article>
          </div>
        </template>
      </Card>
    </section>

    <Card>
      <template #title>
        PrimeVue DataTable
      </template>

      <template #subtitle>
        Ориентир для административных списков: компактно, без разноцветной россыпи действий.
      </template>

      <template #content>
        <DataTable
          :value="students"
          data-key="id"
          size="small"
          table-style="min-width: 42rem"
        >
          <Column field="name" header="Студент" />
          <Column field="group" header="Группа" />
          <Column field="result" header="Результат">
            <template #body="slotProps">
              {{ slotProps.data.result }}%
            </template>
          </Column>
          <Column field="status" header="Статус">
            <template #body="slotProps">
              <Tag
                :value="slotProps.data.status"
                :severity="statusSeverity(slotProps.data.status)"
              />
            </template>
          </Column>
          <Column header="" style="width: 4rem">
            <template #body="slotProps">
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                text
                rounded
                :aria-label="`Действия: ${slotProps.data.name}`"
                aria-haspopup="true"
                aria-controls="showcase-actions-menu"
                @click="openActionMenu($event, slotProps.data)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <section class="grid gap-4 md:grid-cols-2">
      <Card>
        <template #title>
          Обратная связь
        </template>

        <template #content>
          <div class="grid gap-4">
            <Message severity="info">
              Служебные сообщения остаются спокойными и не конкурируют с основным содержанием.
            </Message>

            <div class="flex flex-wrap gap-2">
              <Button
                icon="pi pi-bell"
                label="Показать Toast"
                @click="showToast"
              />
              <Button
                icon="pi pi-window-maximize"
                label="Открыть Dialog"
                severity="secondary"
                outlined
                @click="dialogVisible = true"
              />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          Loading states
        </template>

        <template #content>
          <div class="grid gap-5">
            <ProgressBar :value="68" />
            <div class="grid gap-3">
              <Skeleton width="70%" height="1rem" />
              <Skeleton width="100%" height="1rem" />
              <Skeleton width="88%" height="1rem" />
            </div>
          </div>
        </template>
      </Card>
    </section>

    <Dialog
      v-model:visible="dialogVisible"
      modal
      header="Подтверждение действия"
      :style="{ width: 'min(92vw, 32rem)' }"
    >
      <p class="m-0 leading-6 text-muted-color">
        Диалог использует тот же Academic Navy preset и должен оставаться
        визуально спокойнее основной рабочей области.
      </p>

      <template #footer>
        <Button
          label="Отмена"
          severity="secondary"
          text
          @click="dialogVisible = false"
        />
        <Button
          label="Подтвердить"
          @click="dialogVisible = false"
        />
      </template>
    </Dialog>
  </main>
</template>


<style scoped>
.showcase-search-control {
  display: flex;
  min-height: var(--st-control-height);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
  background: var(--st-surface);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.showcase-search-control:hover {
  border-color: color-mix(in srgb, var(--st-border) 55%, var(--st-text-secondary));
}

.showcase-search-control:focus-within {
  border-color: var(--st-primary);
  box-shadow: 0 0 0 3px var(--st-primary-soft);
}

.showcase-search-icon {
  position: relative;
  display: grid;
  width: 2.75rem;
  flex: 0 0 2.75rem;
  place-items: center;
  color: var(--st-text-muted);
}

.showcase-search-icon::after {
  position: absolute;
  top: 22%;
  right: 0;
  bottom: 22%;
  width: 1px;
  background: var(--st-border);
  content: '';
}

.showcase-search-control:focus-within .showcase-search-icon {
  color: var(--st-primary);
}

.showcase-search-control:focus-within .showcase-search-icon::after {
  background: color-mix(in srgb, var(--st-primary) 45%, var(--st-border));
}

:deep(.showcase-search-input) {
  min-width: 0;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  outline: none !important;
}

:deep(.showcase-search-input:focus),
:deep(.showcase-search-input:focus-visible) {
  border: 0 !important;
  box-shadow: none !important;
  outline: none !important;
}

:deep(.showcase-menu-danger .p-menuitem-content),
:deep(.showcase-menu-danger .p-menuitem-link) {
  color: var(--st-danger);
}
</style>
