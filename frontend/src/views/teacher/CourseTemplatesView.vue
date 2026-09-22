<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  useRoute,
} from 'vue-router'

import {
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  buildCourseTemplateListParams,
  canCreateCourseTemplate,
} from '@/utils/courseTemplateContext'

const route = useRoute()
const authStore = useAuthStore()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedSubject,
  selectedMembership,
  membershipOptions,
  ensureSelectedMembershipActive,
  loadTeacherSubjects,
} = useTeacherSubjects()

const templates = ref([])
const versions = ref([])
const selectedTemplateId = ref(null)

const loading = ref(false)
const loadingVersions = ref(false)
const initialized = ref(false)
const publishingVersionId = ref(null)

const templateSearchQuery = ref('')
const templateVisibilityFilter = ref('all')
const templateSortMode = ref('name-asc')
const versionSearchQuery = ref('')
const versionPublicationFilter = ref('all')
const versionSortMode = ref('version-desc')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingTemplateId = ref(null)
const deleteError = ref('')
const templateFormError = ref('')
const versionFormError = ref('')
const handledRouteVersionKey = ref('')

const templatesRequest = createLatestRequestGuard()
const versionsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
})

const selectedTemplate = computed(() => {
  return templates.value.find(
    (item) =>
      Number(item.id) === Number(selectedTemplateId.value)
  ) ?? null
})

const templateCreationAllowed = computed(() => {
  return canCreateCourseTemplate({
    isAdmin: authStore.isAdminMode,
  })
})

function templateToForm(template = null) {
  return {
    id: template?.id ?? null,
    name: template?.name ?? '',
    publicVisible: template ? Boolean(template.publicVisible) : true,
  }
}

function nextVersionNumber() {
  return versions.value.reduce(
    (max, version) =>
      Math.max(max, Number(version.versionNumber ?? 0)),
    0
  ) + 1
}

function versionToForm(version = null) {
  return {
    id: version?.id ?? null,
    versionNumber: version
      ? Number(version.versionNumber ?? 1)
      : nextVersionNumber(),
    title: version?.title ?? selectedTemplate.value?.name ?? '',
    description: version?.description ?? '',
    changeNotes: version?.changeNotes ?? '',
    published: version ? Boolean(version.published) : false,
  }
}

const templateOverlay = useOverlayForm({
  createDefault: () => templateToForm(),
  mapEntity: templateToForm,
})

const versionOverlay = useOverlayForm({
  createDefault: () => versionToForm(),
  mapEntity: versionToForm,
})

const templateVisibilityOptions = [
  { value: 'all', label: 'Все шаблоны' },
  { value: 'visible', label: 'Опубликованные' },
  { value: 'hidden', label: 'Черновики' },
]

const templateSortOptions = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'id-desc', label: 'Сначала новые' },
]

const versionPublicationOptions = [
  { value: 'all', label: 'Все версии' },
  { value: 'published', label: 'Опубликованные' },
  { value: 'draft', label: 'Черновики' },
]

const versionSortOptions = [
  { value: 'version-desc', label: 'Сначала новые версии' },
  { value: 'version-asc', label: 'Сначала старые версии' },
  { value: 'title-asc', label: 'Название А–Я' },
]

const canWorkWithTemplates = computed(() => {
  return Boolean(selectedMembership.value && selectedSubjectId.value)
})

const contextHint = computed(() => {
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Шаблоны задают структуру курса, а версии используются при работе с лекциями.'
  }

  if (!selectedSubject.value) {
    return `Выбрано назначение #${selectedMembership.value.id}.`
  }

  return `Предмет «${selectedSubject.value.name}». Шаблонов курса: ${templates.value.length}.`
})

const hasActiveTemplateFilters = computed(() => {
  return Boolean(templateSearchQuery.value.trim()) ||
    templateVisibilityFilter.value !== 'all' ||
    templateSortMode.value !== 'name-asc'
})

const filteredTemplates = computed(() => {
  const query = templateSearchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = templates.value.filter((template) => {
    if (
      templateVisibilityFilter.value === 'visible' &&
      !template.publicVisible
    ) {
      return false
    }

    if (
      templateVisibilityFilter.value === 'hidden' &&
      template.publicVisible
    ) {
      return false
    }

    if (!query) {
      return true
    }

    return [template.id, template.name]
      .filter((value) => value !== null && value !== undefined)
      .join(' ')
      .toLocaleLowerCase('ru-RU')
      .includes(query)
  })

  return [...result].sort((left, right) => {
    if (templateSortMode.value === 'name-desc') {
      return String(right.name ?? '').localeCompare(
        String(left.name ?? ''),
        'ru'
      )
    }

    if (templateSortMode.value === 'id-desc') {
      return Number(right.id ?? 0) - Number(left.id ?? 0)
    }

    return String(left.name ?? '').localeCompare(
      String(right.name ?? ''),
      'ru'
    )
  })
})

const hasActiveVersionFilters = computed(() => {
  return Boolean(versionSearchQuery.value.trim()) ||
    versionPublicationFilter.value !== 'all' ||
    versionSortMode.value !== 'version-desc'
})

const filteredVersions = computed(() => {
  const query = versionSearchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = versions.value.filter((version) => {
    if (
      versionPublicationFilter.value === 'published' &&
      !version.published
    ) {
      return false
    }

    if (
      versionPublicationFilter.value === 'draft' &&
      version.published
    ) {
      return false
    }

    if (!query) {
      return true
    }

    return [
      version.versionNumber,
      version.title,
      version.description,
      version.changeNotes,
    ]
      .filter((value) => value !== null && value !== undefined)
      .join(' ')
      .toLocaleLowerCase('ru-RU')
      .includes(query)
  })

  return [...result].sort((left, right) => {
    if (versionSortMode.value === 'version-asc') {
      return Number(left.versionNumber ?? 0) - Number(right.versionNumber ?? 0)
    }

    if (versionSortMode.value === 'title-asc') {
      return String(left.title ?? '').localeCompare(
        String(right.title ?? ''),
        'ru'
      )
    }

    return Number(right.versionNumber ?? 0) - Number(left.versionNumber ?? 0)
  })
})

const templateFilterResultText = computed(() => {
  if (!canWorkWithTemplates.value) {
    return 'Сначала выберите предмет преподавателя.'
  }

  return `Показано: ${filteredTemplates.value.length} из ${templates.value.length}`
})

const versionFilterResultText = computed(() => {
  if (!selectedTemplate.value) {
    return 'Сначала выберите шаблон курса.'
  }

  return `Показано: ${filteredVersions.value.length} из ${versions.value.length}`
})

const templateDrawerTitle = computed(() => {
  return templateOverlay.isCreate.value
    ? 'Новый шаблон курса'
    : 'Редактирование шаблона'
})

const versionDrawerTitle = computed(() => {
  return versionOverlay.isCreate.value
    ? 'Новая версия курса'
    : 'Редактирование версии'
})

function routeQuery(versionId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId = selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId = selectedMembership.value.id
  }

  if (selectedTemplateId.value) {
    query.templateId = selectedTemplateId.value
  }

  if (versionId) {
    query.versionId = versionId
  }

  return query
}

function resetTemplateFilters() {
  templateSearchQuery.value = ''
  templateVisibilityFilter.value = 'all'
  templateSortMode.value = 'name-asc'
}

function resetVersionFilters() {
  versionSearchQuery.value = ''
  versionPublicationFilter.value = 'all'
  versionSortMode.value = 'version-desc'
}

function closeTemplateDrawerImmediately() {
  templateFormError.value = ''
  templateOverlay.closeImmediately()
}

function closeVersionDrawerImmediately() {
  versionFormError.value = ''
  versionOverlay.closeImmediately()
}

function openCreateTemplate() {
  if (!canWorkWithTemplates.value) {
    notice.value = {
      type: 'danger',
      message: 'Выберите предмет преподавателя.',
    }
    return
  }

  if (!templateCreationAllowed.value) {
    notice.value = {
      type: 'info',
      message: 'Новый шаблон должен создать сам преподаватель.',
    }
    return
  }

  closeVersionDrawerImmediately()
  templateFormError.value = ''
  templateOverlay.openCreate()
}

function openEditTemplate(template) {
  closeVersionDrawerImmediately()
  templateFormError.value = ''
  templateOverlay.openEdit(template)
}

function requestTemplateDrawerClose() {
  return templateOverlay.requestClose()
}

function handleTemplateDrawerVisibility(nextValue) {
  if (!nextValue) {
    requestTemplateDrawerClose()
  }
}

function discardTemplateDrawer() {
  templateOverlay.discardAndClose()
  templateFormError.value = ''
}

function openCreateVersion() {
  if (!selectedTemplate.value) {
    notice.value = {
      type: 'danger',
      message: 'Выберите шаблон курса.',
    }
    return
  }

  closeTemplateDrawerImmediately()
  versionFormError.value = ''
  versionOverlay.openCreate({
    versionNumber: nextVersionNumber(),
    title: selectedTemplate.value.name ?? '',
  })
}

function openEditVersion(version) {
  closeTemplateDrawerImmediately()
  versionFormError.value = ''
  versionOverlay.openEdit(version)
}

function requestVersionDrawerClose() {
  return versionOverlay.requestClose()
}

function handleVersionDrawerVisibility(nextValue) {
  if (!nextValue) {
    requestVersionDrawerClose()
  }
}

function discardVersionDrawer() {
  versionOverlay.discardAndClose()
  versionFormError.value = ''
}

function requestDeleteTemplate(template) {
  deleteTarget.value = template
  deleteError.value = ''
  deleteConfirmVisible.value = true
}

function closeDeleteDialog() {
  if (deletingTemplateId.value !== null) {
    return
  }

  deleteConfirmVisible.value = false
  deleteTarget.value = null
  deleteError.value = ''
}

function templateValidationMessage() {
  if (!canWorkWithTemplates.value) {
    return 'Выберите предмет преподавателя.'
  }

  const name = String(templateOverlay.form.name ?? '').trim()

  if (!name) {
    return 'Введите название шаблона.'
  }

  if (name.length > 200) {
    return 'Название шаблона не может быть длиннее 200 символов.'
  }

  return ''
}

function versionValidationMessage() {
  if (!selectedTemplate.value) {
    return 'Выберите шаблон курса.'
  }

  const versionNumber = Number(versionOverlay.form.versionNumber)
  const title = String(versionOverlay.form.title ?? '').trim()
  const description = String(versionOverlay.form.description ?? '').trim()
  const changeNotes = String(versionOverlay.form.changeNotes ?? '').trim()

  if (!Number.isInteger(versionNumber) || versionNumber <= 0) {
    return 'Номер версии должен быть целым числом больше нуля.'
  }

  if (!title) {
    return 'Введите название версии.'
  }

  if (title.length > 200) {
    return 'Название версии не может быть длиннее 200 символов.'
  }

  if (description.length > 2000) {
    return 'Описание версии не может быть длиннее 2000 символов.'
  }

  if (changeNotes.length > 2000) {
    return 'Примечания к изменениям не могут быть длиннее 2000 символов.'
  }

  const duplicate = versions.value.find(
    (version) =>
      Number(version.versionNumber) === versionNumber &&
      String(version.id) !== String(versionOverlay.form.id ?? '')
  )

  if (duplicate) {
    return 'Версия с таким номером уже существует в выбранном шаблоне.'
  }

  return ''
}

async function loadTemplates({
  preferredTemplateId = null,
  openRouteVersion = false,
} = {}) {
  const requestId = templatesRequest.begin()

  versionsRequest.invalidate()
  loadingVersions.value = false

  const previousTemplateId = preferredTemplateId ?? selectedTemplateId.value

  templates.value = []
  versions.value = []
  selectedTemplateId.value = null

  const subjectId = Number(selectedSubjectId.value || 0)

  if (!subjectId) {
    loading.value = false
    return
  }

  loading.value = true

  try {
    const params = buildCourseTemplateListParams({
      subjectId,
      isAdmin: authStore.isAdminMode,
      currentPersonId: authStore.personId,
      selectedMembership: selectedMembership.value,
    })

    if (!params) {
      if (templatesRequest.isCurrent(requestId)) {
        notice.value = {
          type: 'info',
          message: 'Не удалось определить преподавателя для выбранного предмета.',
        }
      }
      return
    }

    const response = await coursesApi.getTemplates(params)

    if (!templatesRequest.isCurrent(requestId)) {
      return
    }

    templates.value = listFromResponse(response)

    const routeTemplateId = route.query.templateId
    const desiredTemplateId =
      preferredTemplateId ?? routeTemplateId ?? previousTemplateId

    if (
      desiredTemplateId &&
      templates.value.some(
        (item) => String(item.id) === String(desiredTemplateId)
      )
    ) {
      await selectTemplate(Number(desiredTemplateId), {
        openRouteVersion,
      })
    } else if (templates.value.length === 1) {
      await selectTemplate(templates.value[0].id, {
        openRouteVersion,
      })
    }
  } catch (error) {
    if (!templatesRequest.isCurrent(requestId)) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить шаблоны курса'
      ),
    }
  } finally {
    if (templatesRequest.isCurrent(requestId)) {
      loading.value = false
    }
  }
}

async function selectTemplate(templateId, { openRouteVersion = false } = {}) {
  selectedTemplateId.value = Number(templateId)
  closeVersionDrawerImmediately()
  resetVersionFilters()
  await loadVersions({ openRouteVersion })
}

async function loadVersions({ openRouteVersion = false } = {}) {
  const requestId = versionsRequest.begin()

  versions.value = []

  const templateId = Number(selectedTemplateId.value || 0)

  if (!templateId) {
    loadingVersions.value = false
    return
  }

  loadingVersions.value = true

  try {
    const response = await coursesApi.getVersions(templateId)

    if (!versionsRequest.isCurrent(requestId)) {
      return
    }

    versions.value = listFromResponse(response)
      .sort(
        (left, right) =>
          Number(right.versionNumber ?? 0) -
          Number(left.versionNumber ?? 0)
      )

    if (openRouteVersion && route.query.versionId) {
      const routeVersionKey = `${templateId}:${route.query.versionId}`

      if (handledRouteVersionKey.value !== routeVersionKey) {
        handledRouteVersionKey.value = routeVersionKey

        const version = versions.value.find(
          (item) => String(item.id) === String(route.query.versionId)
        )

        if (version) {
          openEditVersion(version)
        }
      }
    }
  } catch (error) {
    if (!versionsRequest.isCurrent(requestId)) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить версии курса'
      ),
    }
  } finally {
    if (versionsRequest.isCurrent(requestId)) {
      loadingVersions.value = false
    }
  }
}

async function saveTemplate() {
  templateFormError.value = templateValidationMessage()

  if (templateFormError.value) {
    return
  }

  if (
    !templateOverlay.form.id &&
    !templateCreationAllowed.value
  ) {
    templateFormError.value = 'Новый шаблон должен создать сам преподаватель.'
    return
  }

  const payload = {
    subjectId: Number(selectedSubjectId.value),
    name: String(templateOverlay.form.name).trim(),
    publicVisible: Boolean(templateOverlay.form.publicVisible),
  }

  const editingId = templateOverlay.form.id

  templateOverlay.beginSaving()

  try {
    await ensureSelectedMembershipActive()

    const response = editingId
      ? await coursesApi.updateTemplate(editingId, payload)
      : await coursesApi.createTemplate(payload)

    const savedId = response?.data?.id ?? editingId ?? null

    notice.value = {
      type: 'success',
      message: editingId ? 'Шаблон обновлён.' : 'Шаблон создан.',
    }

    templateOverlay.finishSaving({ close: true })
    templateFormError.value = ''
    await loadTemplates({ preferredTemplateId: savedId })
  } catch (error) {
    templateFormError.value = getApiErrorMessage(
      error,
      editingId
        ? 'Не удалось обновить шаблон'
        : 'Не удалось создать шаблон'
    )
    templateOverlay.failSaving()
  }
}

async function deleteTemplate() {
  const template = deleteTarget.value

  if (!template || deletingTemplateId.value !== null) {
    return
  }

  deletingTemplateId.value = template.id
  deleteError.value = ''

  try {
    await ensureSelectedMembershipActive()
    await coursesApi.removeTemplate(template.id)

    if (Number(templateOverlay.form.id) === Number(template.id)) {
      closeTemplateDrawerImmediately()
    }

    if (Number(selectedTemplateId.value) === Number(template.id)) {
      selectedTemplateId.value = null
      versions.value = []
      closeVersionDrawerImmediately()
    }

    notice.value = {
      type: 'success',
      message: 'Шаблон удалён.',
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    await loadTemplates()
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить шаблон'
    )
  } finally {
    deletingTemplateId.value = null
  }
}

async function saveVersion() {
  versionFormError.value = versionValidationMessage()

  if (versionFormError.value) {
    return
  }

  const editingId = versionOverlay.form.id

  const basePayload = {
    versionNumber: Number(versionOverlay.form.versionNumber),
    title: String(versionOverlay.form.title).trim(),
    description: String(versionOverlay.form.description ?? '').trim() || null,
    changeNotes: String(versionOverlay.form.changeNotes ?? '').trim() || null,
  }

  versionOverlay.beginSaving()

  try {
    await ensureSelectedMembershipActive()

    if (editingId) {
      await coursesApi.updateVersion(editingId, basePayload)
    } else {
      await coursesApi.createVersion(
        selectedTemplateId.value,
        {
          ...basePayload,
          published: Boolean(versionOverlay.form.published),
        }
      )
    }

    notice.value = {
      type: 'success',
      message: editingId ? 'Версия обновлена.' : 'Версия создана.',
    }

    versionOverlay.finishSaving({ close: true })
    versionFormError.value = ''
    await loadVersions()
  } catch (error) {
    versionFormError.value = getApiErrorMessage(
      error,
      editingId
        ? 'Не удалось обновить версию'
        : 'Не удалось создать версию'
    )
    versionOverlay.failSaving()
  }
}

async function publishVersion(version) {
  publishingVersionId.value = version.id

  try {
    await ensureSelectedMembershipActive()

    if (version.published) {
      await coursesApi.unpublishVersion(version.id)

      notice.value = {
        type: 'success',
        message: 'Публикация версии снята.',
      }
    } else {
      await coursesApi.publishVersion(version.id)

      notice.value = {
        type: 'success',
        message: 'Версия опубликована.',
      }
    }

    await loadVersions()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось изменить публикацию версии'
      ),
    }
  } finally {
    publishingVersionId.value = null
  }
}

watch(
  selectedMembershipId,
  () => {
    if (!initialized.value) {
      return
    }

    closeTemplateDrawerImmediately()
    closeVersionDrawerImmediately()
    closeDeleteDialog()
    resetTemplateFilters()
    resetVersionFilters()
    handledRouteVersionKey.value = ''
    loadTemplates()
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId: route.query.subjectId,
      preferredMembershipId: route.query.subjectMembershipId,
    })

    initialized.value = true

    if (selectedSubjectId.value) {
      await loadTemplates({ openRouteVersion: true })
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message: 'Нет предметов преподавателя для работы с шаблонами курса.',
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(error, error.message),
    }
  }
})
</script>

<template>
  <TeacherPageShell
    title="Шаблоны курса"
    subtitle="Просматривайте структуру курса на рабочей странице, а шаблоны и версии редактируйте в боковой панели."
  >
    <template #actions>
      <UiButton
        v-if="templateCreationAllowed"
        variant="primary"
        :disabled="!canWorkWithTemplates"
        @click="openCreateTemplate"
      >
        Добавить шаблон
      </UiButton>
    </template>

    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiCard
      title="Контекст предмета"
      :description="contextHint"
    >
      <div class="teacher-grid teacher-template-context-grid">
        <UiSelect
          v-model="selectedMembershipId"
          label="Предмет преподавателя"
          :options="membershipOptions"
          placeholder="Выберите предмет"
          :disabled="loadingSubjects || !membershipOptions.length"
        />

        <div class="teacher-template-context-actions">
          <UiButton
            variant="secondary"
            :disabled="!selectedMembership"
            :to="selectedMembership ? {
              name: 'teacher-topics',
              query: routeQuery(),
            } : undefined"
          >
            Темы предмета
          </UiButton>
        </div>
      </div>

      <UiAlert
        v-if="authStore.isAdminMode"
        class="teacher-template-admin-hint"
        variant="info"
        message="Администратор может просматривать и редактировать существующие шаблоны выбранного преподавателя. Создание шаблона от имени преподавателя backend не поддерживает."
      />
    </UiCard>

    <UiCard
      title="Шаблоны курса"
      description="Выберите шаблон, чтобы открыть его версии. Создание и редактирование выполняются в боковой панели."
    >
      <UiFilterBar
        v-model="templateSearchQuery"
        search-placeholder="Название или ID шаблона"
        :result-text="templateFilterResultText"
        :reset-disabled="!hasActiveTemplateFilters"
        @reset="resetTemplateFilters"
      >
        <template #filters>
          <UiSelect
            v-model="templateVisibilityFilter"
            :options="templateVisibilityOptions"
            aria-label="Видимость шаблона"
          />

          <UiSelect
            v-model="templateSortMode"
            :options="templateSortOptions"
            aria-label="Сортировка шаблонов"
          />
        </template>

        <template #actions>
          <UiButton
            v-if="templateCreationAllowed"
            size="sm"
            variant="primary"
            :disabled="!canWorkWithTemplates"
            @click="openCreateTemplate"
          >
            Добавить шаблон
          </UiButton>
        </template>
      </UiFilterBar>

      <UiEmptyState
        v-if="loading"
        description="Загрузка шаблонов..."
        compact
      />

      <UiEmptyState
        v-else-if="!selectedSubjectId"
        description="Выберите предмет преподавателя, чтобы открыть его шаблоны курса."
        compact
      />

      <UiEmptyState
        v-else-if="!templates.length"
        description="У выбранного предмета пока нет шаблонов курса."
        compact
      />

      <UiEmptyState
        v-else-if="!filteredTemplates.length"
        description="По выбранным фильтрам шаблоны не найдены."
        compact
      />

      <div
        v-else
        class="teacher-entity-list teacher-template-list"
      >
        <article
          v-for="courseTemplate in filteredTemplates"
          :key="courseTemplate.id"
          class="teacher-entity-card"
          :class="{
            'teacher-entity-card--selected':
              Number(selectedTemplateId) === Number(courseTemplate.id),
          }"
        >
          <div class="teacher-entity-card__header">
            <div class="teacher-entity-card__heading">
              <span class="teacher-entity-card__eyebrow">
                Шаблон #{{ courseTemplate.id }}
              </span>

              <h3 class="teacher-entity-card__title">
                {{ courseTemplate.name }}
              </h3>
            </div>

            <span
              class="teacher-status"
              :class="{
                'teacher-status--success': courseTemplate.publicVisible,
              }"
            >
              {{ courseTemplate.publicVisible ? 'Виден' : 'Черновик' }}
            </span>
          </div>

          <div class="teacher-entity-card__meta">
            <span>
              {{ Number(selectedTemplateId) === Number(courseTemplate.id)
                ? `${versions.length} версий загружено`
                : 'Откройте шаблон, чтобы посмотреть версии' }}
            </span>
          </div>

          <div class="teacher-entity-card__actions">
            <UiButton
              :variant="
                Number(selectedTemplateId) === Number(courseTemplate.id)
                  ? 'primary'
                  : 'secondary'
              "
              size="sm"
              @click="selectTemplate(courseTemplate.id)"
            >
              {{ Number(selectedTemplateId) === Number(courseTemplate.id) ? 'Версии открыты' : 'Открыть версии' }}
            </UiButton>

            <UiButton
              size="sm"
              @click="openEditTemplate(courseTemplate)"
            >
              Изменить
            </UiButton>

            <UiButton
              variant="danger"
              size="sm"
              :loading="deletingTemplateId === courseTemplate.id"
              loading-text="Удаление..."
              @click="requestDeleteTemplate(courseTemplate)"
            >
              Удалить
            </UiButton>
          </div>
        </article>
      </div>
    </UiCard>

    <UiCard
      title="Версии шаблона"
      :description="
        selectedTemplate
          ? `Шаблон «${selectedTemplate.name}». Версии редактируются в той же боковой панели без вложенных окон.`
          : 'Выберите шаблон курса выше.'
      "
    >
      <UiFilterBar
        v-model="versionSearchQuery"
        search-placeholder="Название, номер или описание версии"
        :result-text="versionFilterResultText"
        :reset-disabled="!hasActiveVersionFilters"
        :show-search="Boolean(selectedTemplate)"
        :show-reset="Boolean(selectedTemplate)"
        @reset="resetVersionFilters"
      >
        <template v-if="selectedTemplate" #filters>
          <UiSelect
            v-model="versionPublicationFilter"
            :options="versionPublicationOptions"
            aria-label="Публикация версии"
          />

          <UiSelect
            v-model="versionSortMode"
            :options="versionSortOptions"
            aria-label="Сортировка версий"
          />
        </template>

        <template #actions>
          <UiButton
            size="sm"
            variant="primary"
            :disabled="!selectedTemplate"
            @click="openCreateVersion"
          >
            Добавить версию
          </UiButton>
        </template>
      </UiFilterBar>

      <UiEmptyState
        v-if="loadingVersions"
        description="Загрузка версий..."
        compact
      />

      <UiEmptyState
        v-else-if="!selectedTemplateId"
        description="Выберите шаблон курса выше."
        compact
      />

      <UiEmptyState
        v-else-if="!versions.length"
        description="У выбранного шаблона пока нет версий."
        compact
      />

      <UiEmptyState
        v-else-if="!filteredVersions.length"
        description="По выбранным фильтрам версии не найдены."
        compact
      />

      <div
        v-else
        class="teacher-entity-list teacher-version-list"
      >
        <article
          v-for="version in filteredVersions"
          :key="version.id"
          class="teacher-entity-card"
        >
          <div class="teacher-entity-card__header">
            <div class="teacher-entity-card__heading">
              <span class="teacher-entity-card__eyebrow">
                Версия {{ version.versionNumber }}
              </span>

              <h3 class="teacher-entity-card__title">
                {{ version.title }}
              </h3>
            </div>

            <span
              class="teacher-status"
              :class="{
                'teacher-status--success': version.published,
              }"
            >
              {{ version.published ? 'Опубликована' : 'Черновик' }}
            </span>
          </div>

          <p class="teacher-entity-card__description">
            {{ version.description || 'Описание пока не добавлено.' }}
          </p>

          <div
            v-if="version.changeNotes"
            class="teacher-version-notes"
          >
            <span class="teacher-muted">Что изменилось</span>
            <p>{{ version.changeNotes }}</p>
          </div>

          <div class="teacher-entity-card__actions">
            <UiButton
              size="sm"
              @click="openEditVersion(version)"
            >
              Изменить
            </UiButton>

            <UiButton
              :variant="version.published ? 'secondary' : 'primary'"
              size="sm"
              :loading="publishingVersionId === version.id"
              loading-text="Обновление..."
              @click="publishVersion(version)"
            >
              {{ version.published ? 'Снять публикацию' : 'Опубликовать' }}
            </UiButton>

            <UiButton
              size="sm"
              variant="secondary"
              :to="{
                name: 'teacher-lectures',
                query: routeQuery(version.id),
              }"
            >
              Лекции
            </UiButton>
          </div>
        </article>
      </div>
    </UiCard>

    <UiDrawer
      :model-value="templateOverlay.isOpen.value"
      :title="templateDrawerTitle"
      width="34rem"
      @update:model-value="handleTemplateDrawerVisibility"
    >
      <div class="teacher-stack">
        <div class="teacher-overlay-context">
          <span class="teacher-muted">Предмет</span>
          <strong>{{ selectedSubject?.name || `Предмет #${selectedSubjectId}` }}</strong>
        </div>

        <UiAlert
          v-if="templateFormError"
          variant="danger"
          :message="templateFormError"
        />

        <UiInput
          v-model="templateOverlay.form.name"
          label="Название шаблона"
          placeholder="Например: Базовый поток"
          maxlength="200"
          required
        />

        <UiCheckbox
          v-model="templateOverlay.form.publicVisible"
          label="Публиковать шаблон"
        />

        <p class="teacher-muted">
          Шаблон остаётся привязан к выбранному предмету. Версии создаются отдельно после сохранения шаблона.
        </p>
      </div>

      <template #footer>
        <div class="teacher-drawer-footer">
          <UiButton
            variant="secondary"
            :disabled="templateOverlay.saving.value"
            @click="requestTemplateDrawerClose"
          >
            Отмена
          </UiButton>

          <UiButton
            variant="primary"
            :loading="templateOverlay.saving.value"
            loading-text="Сохранение..."
            @click="saveTemplate"
          >
            Сохранить
          </UiButton>
        </div>
      </template>
    </UiDrawer>

    <UiDrawer
      :model-value="versionOverlay.isOpen.value"
      :title="versionDrawerTitle"
      width="42rem"
      @update:model-value="handleVersionDrawerVisibility"
    >
      <div class="teacher-stack">
        <div class="teacher-overlay-context">
          <span class="teacher-muted">Шаблон</span>
          <strong>{{ selectedTemplate?.name || 'Не выбран' }}</strong>
        </div>

        <UiAlert
          v-if="versionFormError"
          variant="danger"
          :message="versionFormError"
        />

        <div class="teacher-grid">
          <UiInput
            v-model="versionOverlay.form.versionNumber"
            label="Номер версии"
            type="number"
            min="1"
            step="1"
            required
          />

          <UiInput
            v-model="versionOverlay.form.title"
            label="Название версии"
            maxlength="200"
            required
          />
        </div>

        <UiTextarea
          v-model="versionOverlay.form.description"
          label="Описание"
          maxlength="2000"
          placeholder="Что входит в эту версию курса"
        />

        <UiTextarea
          v-model="versionOverlay.form.changeNotes"
          label="Что изменилось"
          maxlength="2000"
          placeholder="Кратко опишите изменения относительно предыдущей версии"
        />

        <UiCheckbox
          v-if="versionOverlay.isCreate.value"
          v-model="versionOverlay.form.published"
          label="Опубликовать сразу после создания"
        />

        <UiAlert
          v-else
          variant="info"
          message="Статус публикации существующей версии изменяется отдельной кнопкой в карточке версии."
        />
      </div>

      <template #footer>
        <div class="teacher-drawer-footer">
          <UiButton
            variant="secondary"
            :disabled="versionOverlay.saving.value"
            @click="requestVersionDrawerClose"
          >
            Отмена
          </UiButton>

          <UiButton
            variant="primary"
            :loading="versionOverlay.saving.value"
            loading-text="Сохранение..."
            @click="saveVersion"
          >
            Сохранить версию
          </UiButton>
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="templateOverlay.confirmCloseVisible.value"
      :busy="templateOverlay.saving.value"
      @continue="templateOverlay.continueEditing"
      @discard="discardTemplateDrawer"
    />

    <UiUnsavedChangesConfirm
      v-model="versionOverlay.confirmCloseVisible.value"
      :busy="versionOverlay.saving.value"
      @continue="versionOverlay.continueEditing"
      @discard="discardVersionDrawer"
    />

    <UiDialog
      :model-value="deleteConfirmVisible"
      title="Удалить шаблон курса?"
      width="32rem"
      :closable="deletingTemplateId === null"
      :close-on-escape="deletingTemplateId === null"
      :dismissable-mask="false"
      @update:model-value="($event) => !$event && closeDeleteDialog()"
    >
      <div class="teacher-stack">
        <p class="teacher-confirm-text">
          Шаблон «{{ deleteTarget?.name }}» будет удалён. Доступность связанных версий зависит от правил backend.
        </p>

        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-drawer-footer">
          <UiButton
            variant="secondary"
            :disabled="deletingTemplateId !== null"
            @click="closeDeleteDialog"
          >
            Отмена
          </UiButton>

          <UiButton
            variant="danger"
            :loading="deletingTemplateId !== null"
            loading-text="Удаление..."
            @click="deleteTemplate"
          >
            Удалить
          </UiButton>
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-template-context-grid {
  align-items: end;
}

.teacher-template-context-actions {
  display: flex;
  align-items: flex-end;
}

.teacher-template-admin-hint {
  margin-top: 12px;
}

.teacher-template-list,
.teacher-version-list {
  margin-top: 14px;
}

.teacher-version-notes {
  padding: 10px 12px;

  display: grid;
  gap: 4px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-version-notes p,
.teacher-confirm-text {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.teacher-overlay-context {
  padding: 10px 12px;

  display: grid;
  gap: 4px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.teacher-overlay-context strong {
  overflow-wrap: anywhere;
}

.teacher-drawer-footer {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .teacher-template-context-actions,
  .teacher-template-context-actions > * {
    width: 100%;
  }

  .teacher-drawer-footer,
  .teacher-drawer-footer > * {
    width: 100%;
  }
}
</style>
