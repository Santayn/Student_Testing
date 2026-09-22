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
  getApiErrorMessage,
  lecturesApi,
  testsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFileInput,
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
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const route = useRoute()

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

const lectures = ref([])
const availableTests = ref([])
const lectureTestsById = ref(new Map())
const materials = ref([])
const pendingFiles = ref([])
const fileInputKey = ref(0)

const loading = ref(false)
const loadingMaterials = ref(false)
const initialized = ref(false)

const searchQuery = ref('')
const visibilityFilter = ref('all')
const testFilter = ref('all')
const sortMode = ref('ordinal')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')

const materialDeleteTarget = ref(null)
const materialDeleteConfirmVisible = ref(false)
const deletingMaterialId = ref(null)
const materialDeleteError = ref('')

const formError = ref('')
const handledRouteLectureKey = ref('')

const lecturesRequest = createLatestRequestGuard()
const materialsRequest = createLatestRequestGuard()

const notice = ref({
  type: 'info',
  message: '',
})

const visibilityOptions = [
  { value: 'all', label: 'Все лекции' },
  { value: 'visible', label: 'Опубликованные' },
  { value: 'hidden', label: 'Скрытые' },
]

const testFilterOptions = [
  { value: 'all', label: 'Все связи с тестами' },
  { value: 'with-tests', label: 'Есть связанные тесты' },
  { value: 'without-tests', label: 'Без связанных тестов' },
]

const sortOptions = [
  { value: 'ordinal', label: 'По порядку' },
  { value: 'title-asc', label: 'Название А–Я' },
  { value: 'title-desc', label: 'Название Я–А' },
]

function lectureTests(lectureId) {
  return lectureTestsById.value.get(Number(lectureId)) ?? []
}

function lectureToForm(lecture = null) {
  return {
    id: lecture?.id ?? null,
    title: lecture?.title ?? '',
    description: lecture?.description ?? '',
    publicVisible: lecture ? Boolean(lecture.publicVisible) : true,
    testIds: lecture
      ? lectureTests(lecture.id).map((test) => Number(test.id))
      : [],
  }
}

const {
  form,
  isOpen: lectureDrawerOpen,
  isCreate,
  dirty: lectureDirty,
  saving,
  confirmCloseVisible,
  openCreate,
  openEdit,
  closeImmediately,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
} = useOverlayForm({
  createDefault: () => lectureToForm(),
  mapEntity: lectureToForm,
})

const canEdit = computed(() => Boolean(selectedMembership.value))

const contextHint = computed(() => {
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Лекции останутся на рабочей странице, а создание и редактирование открываются в боковой панели.'
  }

  if (!selectedSubject.value) {
    return `Выбрано назначение #${selectedMembership.value.id}.`
  }

  return `Предмет «${selectedSubject.value.name}». Лекций в выбранном назначении: ${lectures.value.length}.`
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    visibilityFilter.value !== 'all' ||
    testFilter.value !== 'all' ||
    sortMode.value !== 'ordinal'
})

const filteredLectures = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  const result = lectures.value.filter((lecture) => {
    if (
      visibilityFilter.value === 'visible' &&
      !lecture.publicVisible
    ) {
      return false
    }

    if (
      visibilityFilter.value === 'hidden' &&
      lecture.publicVisible
    ) {
      return false
    }

    const linkedTests = lectureTests(lecture.id)

    if (
      testFilter.value === 'with-tests' &&
      !linkedTests.length
    ) {
      return false
    }

    if (
      testFilter.value === 'without-tests' &&
      linkedTests.length
    ) {
      return false
    }

    if (!query) {
      return true
    }

    const haystack = [
      lecture.id,
      lecture.ordinal,
      lecture.title,
      lecture.description,
      ...linkedTests.map((test) => test.title),
    ]
      .filter((value) => value !== null && value !== undefined)
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  })

  return [...result].sort((left, right) => {
    if (sortMode.value === 'title-asc') {
      return String(left.title ?? '').localeCompare(
        String(right.title ?? ''),
        'ru'
      )
    }

    if (sortMode.value === 'title-desc') {
      return String(right.title ?? '').localeCompare(
        String(left.title ?? ''),
        'ru'
      )
    }

    return (
      Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0) ||
      String(left.title ?? '').localeCompare(
        String(right.title ?? ''),
        'ru'
      )
    )
  })
})

const filterResultText = computed(() => {
  if (!selectedMembership.value) {
    return 'Сначала выберите предмет преподавателя.'
  }

  return `Показано: ${filteredLectures.value.length} из ${lectures.value.length}`
})

const drawerTitle = computed(() => {
  return isCreate.value ? 'Новая лекция' : 'Редактирование лекции'
})

const pendingFilesDirty = computed(() => pendingFiles.value.length > 0)

function routeQuery(lectureId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId = selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId = selectedMembership.value.id
  }

  if (lectureId) {
    query.lectureId = lectureId
  }

  return query
}

function nextOrdinal() {
  return lectures.value.reduce(
    (max, lecture) => Math.max(max, Number(lecture.ordinal ?? 0)),
    0
  ) + 1
}

function slugifyLectureTitle(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-zа-яё0-9]+/gi, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80) || 'lecture'
}

function buildLectureContentKey(title, lectureId = null) {
  const suffix = lectureId || Date.now()

  return `lecture-${suffix}-${slugifyLectureTitle(title)}`
}

function lectureTestSummary(lectureId) {
  const tests = lectureTests(lectureId)

  if (!tests.length) {
    return 'Нет связанных тестов'
  }

  return tests
    .map((test) => test.title || `Тест #${test.id}`)
    .join(', ')
}

function resetFilters() {
  searchQuery.value = ''
  visibilityFilter.value = 'all'
  testFilter.value = 'all'
  sortMode.value = 'ordinal'
}

function clearLectureDrawerState() {
  materialsRequest.invalidate()
  materials.value = []
  pendingFiles.value = []
  loadingMaterials.value = false
  fileInputKey.value += 1
  formError.value = ''
  closeMaterialDeleteDialog()
}

function openCreateLecture() {
  if (!canEdit.value) {
    notice.value = {
      type: 'danger',
      message: 'Выберите предмет преподавателя.',
    }
    return
  }

  clearLectureDrawerState()
  openCreate()
}

async function openEditLecture(lecture) {
  clearLectureDrawerState()
  openEdit(lecture)
  await loadMaterials(lecture.id)
}

function requestLectureDrawerClose() {
  if (saving.value) {
    return false
  }

  if (lectureDirty.value || pendingFilesDirty.value) {
    confirmCloseVisible.value = true
    return false
  }

  closeImmediately()
  clearLectureDrawerState()
  return true
}

function handleLectureDrawerVisibility(nextValue) {
  if (nextValue) {
    return
  }

  requestLectureDrawerClose()
}

function discardLectureDrawer() {
  pendingFiles.value = []
  discardAndClose()
  clearLectureDrawerState()
}

function closeLectureDrawerImmediately() {
  closeImmediately()
  clearLectureDrawerState()
}

function requestDeleteLecture(lecture) {
  deleteTarget.value = lecture
  deleteError.value = ''
  deleteConfirmVisible.value = true
}

function closeDeleteDialog() {
  if (deletingId.value !== null) {
    return
  }

  deleteConfirmVisible.value = false
  deleteTarget.value = null
  deleteError.value = ''
}

function requestDeleteMaterial(material) {
  materialDeleteTarget.value = material
  materialDeleteError.value = ''
  materialDeleteConfirmVisible.value = true
}

function closeMaterialDeleteDialog() {
  if (deletingMaterialId.value !== null) {
    return
  }

  materialDeleteConfirmVisible.value = false
  materialDeleteTarget.value = null
  materialDeleteError.value = ''
}

function lectureValidationMessage() {
  if (!selectedMembership.value || !selectedSubject.value) {
    return 'Выберите предмет преподавателя.'
  }

  const title = String(form.title ?? '').trim()
  const description = String(form.description ?? '').trim()

  if (!title) {
    return 'Введите название лекции.'
  }

  if (title.length > 200) {
    return 'Название лекции не может быть длиннее 200 символов.'
  }

  if (description.length > 2000) {
    return 'Описание лекции не может быть длиннее 2000 символов.'
  }

  return ''
}

async function loadLectures({ openRouteLecture = false } = {}) {
  const requestId = lecturesRequest.begin()

  const membershipId = Number(selectedMembership.value?.id ?? 0)
  const subjectId = Number(selectedSubjectId.value ?? 0)

  lectures.value = []
  availableTests.value = []
  lectureTestsById.value = new Map()

  if (!membershipId) {
    loading.value = false
    return
  }

  loading.value = true

  try {
    const lecturesResponse = await lecturesApi.getAll({
      subjectMembershipId: membershipId,
    })

    const nextLectures = listFromResponse(lecturesResponse)
      .sort(
        (left, right) =>
          Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0) ||
          String(left.title ?? '').localeCompare(
            String(right.title ?? ''),
            'ru'
          )
      )

    const [testsResponse, testLists] = await Promise.all([
      testsApi.getAll({ subjectId }),
      Promise.all(
        nextLectures.map((lecture) => lecturesApi.getTests(lecture.id))
      ),
    ])

    if (!lecturesRequest.isCurrent(requestId)) {
      return
    }

    lectures.value = nextLectures
    availableTests.value = listFromResponse(testsResponse)
      .sort((left, right) =>
        String(left.title ?? '').localeCompare(
          String(right.title ?? ''),
          'ru'
        )
      )

    lectureTestsById.value = new Map(
      nextLectures.map((lecture, index) => [
        Number(lecture.id),
        listFromResponse(testLists[index]),
      ])
    )

    if (openRouteLecture && route.query.lectureId) {
      const routeLectureKey = `${membershipId}:${route.query.lectureId}`

      if (handledRouteLectureKey.value !== routeLectureKey) {
        handledRouteLectureKey.value = routeLectureKey

        const lecture = nextLectures.find(
          (item) => String(item.id) === String(route.query.lectureId)
        )

        if (lecture) {
          await openEditLecture(lecture)
        } else {
          notice.value = {
            type: 'info',
            message: 'Лекция из ссылки не найдена в выбранном назначении преподавателя.',
          }
        }
      }
    }
  } catch (error) {
    if (!lecturesRequest.isCurrent(requestId)) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить лекции'
      ),
    }
  } finally {
    if (lecturesRequest.isCurrent(requestId)) {
      loading.value = false
    }
  }
}

async function loadMaterials(lectureId) {
  const requestId = materialsRequest.begin()

  materials.value = []

  if (!lectureId) {
    loadingMaterials.value = false
    return
  }

  loadingMaterials.value = true

  try {
    const response = await lecturesApi.getMaterials(lectureId)

    if (!materialsRequest.isCurrent(requestId)) {
      return
    }

    materials.value = listFromResponse(response)
  } catch (error) {
    if (!materialsRequest.isCurrent(requestId)) {
      return
    }

    formError.value = getApiErrorMessage(
      error,
      'Не удалось загрузить материалы лекции'
    )
  } finally {
    if (materialsRequest.isCurrent(requestId)) {
      loadingMaterials.value = false
    }
  }
}

async function syncLectureTests(lectureId, selectedTestIds) {
  const response = await lecturesApi.setTests(
    lectureId,
    {
      testIds: [
        ...new Set(
          selectedTestIds
            .map(Number)
            .filter(Boolean)
        ),
      ],
    }
  )

  const next = new Map(lectureTestsById.value)
  next.set(Number(lectureId), listFromResponse(response))
  lectureTestsById.value = next
}

async function uploadPendingFiles(lectureId) {
  if (!pendingFiles.value.length) {
    return
  }

  await lecturesApi.uploadMaterials(
    lectureId,
    pendingFiles.value
  )

  pendingFiles.value = []
  fileInputKey.value += 1
}

async function saveLecture() {
  formError.value = lectureValidationMessage()

  if (formError.value) {
    return
  }

  const editingLecture = lectures.value.find(
    (item) => Number(item.id) === Number(form.id)
  ) ?? null

  const payload = {
    subjectId: Number(selectedSubject.value.id),
    subjectMembershipId: Number(selectedMembership.value.id),
    courseVersionId: null,
    ordinal: editingLecture?.ordinal || nextOrdinal(),
    title: String(form.title).trim(),
    description: String(form.description ?? '').trim() || null,
    contentFolderKey:
      editingLecture?.contentFolderKey ||
      buildLectureContentKey(
        form.title,
        editingLecture?.id || null
      ),
    linkedTestId: null,
    publicVisible: Boolean(form.publicVisible),
  }

  beginSaving()

  try {
    await ensureSelectedMembershipActive()

    const response = form.id
      ? await lecturesApi.update(form.id, payload)
      : await lecturesApi.create(payload)

    const lecture = response.data

    await syncLectureTests(lecture.id, form.testIds)
    await uploadPendingFiles(lecture.id)

    notice.value = {
      type: 'success',
      message: form.id
        ? 'Лекция обновлена.'
        : 'Лекция создана.',
    }

    finishSaving({ close: true })
    clearLectureDrawerState()
    await loadLectures()
  } catch (error) {
    formError.value = getApiErrorMessage(
      error,
      form.id
        ? 'Не удалось обновить лекцию'
        : 'Не удалось создать лекцию'
    )
    failSaving()
  }
}

async function deleteLecture() {
  const lecture = deleteTarget.value

  if (!lecture || deletingId.value !== null) {
    return
  }

  deletingId.value = lecture.id
  deleteError.value = ''

  try {
    await ensureSelectedMembershipActive()
    await lecturesApi.remove(lecture.id)

    if (Number(form.id) === Number(lecture.id)) {
      closeLectureDrawerImmediately()
    }

    notice.value = {
      type: 'success',
      message: 'Лекция удалена.',
    }

    deleteConfirmVisible.value = false
    deleteTarget.value = null
    await loadLectures()
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить лекцию'
    )
  } finally {
    deletingId.value = null
  }
}

function onFiles(files) {
  pendingFiles.value = files
}

function removePendingFile(index) {
  pendingFiles.value = pendingFiles.value.filter(
    (_, itemIndex) => itemIndex !== index
  )
}

async function deleteMaterial() {
  const material = materialDeleteTarget.value

  if (!form.id || !material || deletingMaterialId.value !== null) {
    return
  }

  deletingMaterialId.value = material.id
  materialDeleteError.value = ''

  try {
    await ensureSelectedMembershipActive()
    await lecturesApi.removeMaterial(form.id, material.id)

    materials.value = materials.value.filter(
      (item) => Number(item.id) !== Number(material.id)
    )

    materialDeleteConfirmVisible.value = false
    materialDeleteTarget.value = null
  } catch (error) {
    materialDeleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить материал'
    )
  } finally {
    deletingMaterialId.value = null
  }
}

async function downloadMaterial(material) {
  if (!form.id) {
    return
  }

  try {
    const response = await lecturesApi.downloadMaterial(
      form.id,
      material.id
    )

    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')

    anchor.href = url
    anchor.download = material.fileName || `material-${material.id}`
    anchor.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    formError.value = getApiErrorMessage(
      error,
      'Не удалось скачать материал'
    )
  }
}

watch(
  selectedMembershipId,
  () => {
    if (!initialized.value) {
      return
    }

    closeLectureDrawerImmediately()
    closeDeleteDialog()
    resetFilters()
    handledRouteLectureKey.value = ''
    loadLectures({ openRouteLecture: true })
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
      await loadLectures({ openRouteLecture: true })
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message: 'Нет предметов преподавателя для управления лекциями.',
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
    title="Лекции предмета"
    subtitle="Просматривайте и фильтруйте лекции выбранного предмета. Создание, редактирование, материалы и связи с тестами открываются в боковой панели."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <UiCard
      title="Контекст лекций"
      :description="contextHint"
    >
      <div class="teacher-lecture-context">
        <UiSelect
          v-model="selectedMembershipId"
          label="Предмет преподавателя"
          :options="membershipOptions"
          placeholder="Выберите предмет"
          :disabled="loadingSubjects || !membershipOptions.length"
        />

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
            :to="{
              name: 'teacher-questions',
              query: routeQuery(),
            }"
          >
            Все вопросы
          </UiButton>

          <UiButton
            :to="{
              name: 'teacher-test-create',
              query: routeQuery(),
            }"
          >
            Создать тест
          </UiButton>
        </div>
      </div>
    </UiCard>

    <UiCard
      title="Лекции"
      :description="selectedSubject ? `Предмет: ${selectedSubject.name}.` : 'Предмет не выбран.'"
    >
      <div class="teacher-stack">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, описание, тест, ID или номер лекции"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="visibilityFilter"
              label="Публикация"
              :options="visibilityOptions"
              size="sm"
            />

            <UiSelect
              v-model="testFilter"
              label="Связанные тесты"
              :options="testFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              label="Сортировка"
              :options="sortOptions"
              size="sm"
            />
          </template>

          <template #actions>
            <UiButton
              variant="primary"
              size="sm"
              icon="pi pi-plus"
              label="Добавить лекцию"
              :disabled="!canEdit"
              @click="openCreateLecture"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loading"
          description="Загрузка лекций..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedMembership"
          description="Выберите предмет преподавателя, чтобы открыть его лекции."
          compact
        />

        <UiEmptyState
          v-else-if="!lectures.length"
          description="Для выбранного предмета пока нет лекций. Добавьте первую лекцию кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredLectures.length"
          description="По текущему поиску и фильтрам лекции не найдены."
          compact
        >
          <template #actions>
            <UiButton
              variant="secondary"
              size="sm"
              label="Сбросить фильтры"
              @click="resetFilters"
            />
          </template>
        </UiEmptyState>

        <div
          v-else
          class="teacher-entity-list"
        >
          <article
            v-for="lecture in filteredLectures"
            :key="lecture.id"
            class="teacher-entity-card"
            :class="{
              'teacher-entity-card--selected':
                lectureDrawerOpen && Number(form.id) === Number(lecture.id),
            }"
          >
            <div class="teacher-entity-card__header">
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  Лекция {{ lecture.ordinal }} · ID {{ lecture.id }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ lecture.title }}
                </h3>
              </div>

              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success': lecture.publicVisible,
                }"
              >
                {{ lecture.publicVisible ? 'Опубликована' : 'Скрыта' }}
              </span>
            </div>

            <p class="teacher-entity-card__description">
              {{ lecture.description || 'Описание пока не добавлено.' }}
            </p>

            <div class="teacher-lecture-tests-preview">
              <span class="teacher-muted">Связанные тесты</span>
              <strong>{{ lectureTestSummary(lecture.id) }}</strong>
            </div>

            <div class="teacher-entity-card__actions">
              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditLecture(lecture)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === lecture.id"
                loading-text="Удаление..."
                @click="requestDeleteLecture(lecture)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDrawer
      :model-value="lectureDrawerOpen"
      :title="drawerTitle"
      width="46rem"
      @update:model-value="handleLectureDrawerVisibility"
    >
      <div class="teacher-stack teacher-lecture-drawer">
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
          closable
          @close="formError = ''"
        />

        <section class="teacher-lecture-form-section">
          <div class="teacher-lecture-form-section__heading">
            <span class="teacher-muted">Основные данные</span>
            <strong>
              {{ isCreate ? 'Новая лекция' : `Лекция #${form.id}` }}
            </strong>
          </div>

          <UiInput
            v-model="form.title"
            label="Название"
            maxlength="200"
            required
          />

          <UiTextarea
            v-model="form.description"
            label="Описание"
            maxlength="2000"
          />

          <UiCheckbox
            v-model="form.publicVisible"
            label="Публиковать лекцию для студентов"
          />
        </section>

        <section class="teacher-lecture-form-section">
          <div class="teacher-lecture-form-section__heading">
            <span class="teacher-muted">Связанные тесты</span>
            <strong>{{ form.testIds.length }} выбрано</strong>
          </div>

          <UiEmptyState
            v-if="!availableTests.length"
            description="Для предмета пока нет доступных тестов."
            compact
          />

          <div
            v-else
            class="teacher-selection-grid"
          >
            <UiCheckbox
              v-for="test in availableTests"
              :key="test.id"
              v-model="form.testIds"
              mode="multiple"
              :value="test.id"
              :label="test.title || `Тест #${test.id}`"
              :description="test.description || ''"
              :disabled="saving"
            />
          </div>
        </section>

        <section class="teacher-lecture-form-section">
          <div class="teacher-lecture-form-section__heading">
            <span class="teacher-muted">Материалы</span>
            <strong>
              {{ form.id ? `${materials.length} загружено` : 'Будут загружены после создания' }}
            </strong>
          </div>

          <UiFileInput
            :key="fileInputKey"
            label="Добавить файлы"
            hint="Можно выбрать несколько файлов. Они загрузятся вместе с сохранением лекции."
            multiple
            :disabled="saving"
            @files-change="onFiles"
          />

          <div
            v-if="pendingFiles.length"
            class="teacher-file-list"
          >
            <div
              v-for="(file, index) in pendingFiles"
              :key="`${file.name}-${index}`"
              class="teacher-file-item"
            >
              <span>{{ file.name }}</span>

              <UiButton
                size="sm"
                variant="secondary"
                label="Убрать"
                :disabled="saving"
                @click="removePendingFile(index)"
              />
            </div>
          </div>

          <template v-if="form.id">
            <div class="teacher-divider" />

            <UiEmptyState
              v-if="loadingMaterials"
              description="Загрузка материалов..."
              compact
            />

            <UiEmptyState
              v-else-if="!materials.length"
              description="Загруженных материалов пока нет."
              compact
            />

            <div
              v-else
              class="teacher-file-list"
            >
              <div
                v-for="material in materials"
                :key="material.id"
                class="teacher-file-item"
              >
                <span>{{ material.fileName || `Материал #${material.id}` }}</span>

                <div class="teacher-inline-actions">
                  <UiButton
                    size="sm"
                    variant="secondary"
                    icon="pi pi-download"
                    label="Скачать"
                    @click="downloadMaterial(material)"
                  />

                  <UiButton
                    size="sm"
                    variant="danger"
                    icon="pi pi-trash"
                    label="Удалить"
                    :loading="deletingMaterialId === material.id"
                    loading-text="Удаление..."
                    @click="requestDeleteMaterial(material)"
                  />
                </div>
              </div>
            </div>
          </template>
        </section>
      </div>

      <template #footer>
        <div class="teacher-lecture-drawer__footer">
          <UiButton
            variant="secondary"
            label="Закрыть"
            :disabled="saving"
            @click="requestLectureDrawerClose"
          />

          <UiButton
            variant="primary"
            :loading="saving"
            loading-text="Сохранение..."
            :label="form.id ? 'Сохранить лекцию' : 'Создать лекцию'"
            @click="saveLecture"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="saving"
      @continue="continueEditing"
      @discard="discardLectureDrawer"
    />

    <UiDialog
      v-model="deleteConfirmVisible"
      title="Удалить лекцию?"
      width="31rem"
      :close-on-escape="deletingId === null"
      :closable="deletingId === null"
    >
      <div class="teacher-stack">
        <p class="teacher-lecture-dialog-copy">
          Лекция «{{ deleteTarget?.title }}» будет удалена. Это действие нельзя отменить.
        </p>

        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            label="Удалить лекцию"
            @click="deleteLecture"
          />
        </div>
      </template>
    </UiDialog>

    <UiDialog
      v-model="materialDeleteConfirmVisible"
      title="Удалить материал?"
      width="31rem"
      :close-on-escape="deletingMaterialId === null"
      :closable="deletingMaterialId === null"
    >
      <div class="teacher-stack">
        <p class="teacher-lecture-dialog-copy">
          Файл «{{ materialDeleteTarget?.fileName || `Материал #${materialDeleteTarget?.id ?? ''}` }}» будет удалён из лекции.
        </p>

        <UiAlert
          v-if="materialDeleteError"
          variant="danger"
          :message="materialDeleteError"
        />
      </div>

      <template #footer>
        <div class="teacher-actions teacher-actions--mobile-stack">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingMaterialId !== null"
            @click="closeMaterialDeleteDialog"
          />

          <UiButton
            variant="danger"
            :loading="deletingMaterialId !== null"
            loading-text="Удаление..."
            label="Удалить файл"
            @click="deleteMaterial"
          />
        </div>
      </template>
    </UiDialog>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-lecture-context {
  min-width: 0;
  display: grid;
  gap: 14px;
}

.teacher-lecture-tests-preview {
  min-width: 0;
  padding: 9px 10px;
  display: grid;
  gap: 3px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-lecture-tests-preview strong {
  min-width: 0;
  color: var(--st-text);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.teacher-lecture-drawer {
  min-width: 0;
  padding-bottom: 4px;
}

.teacher-lecture-form-section {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 14px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-lecture-form-section__heading {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.teacher-lecture-form-section__heading strong {
  min-width: 0;
  color: var(--st-text);
  overflow-wrap: anywhere;
}

.teacher-lecture-drawer__footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.teacher-lecture-dialog-copy {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: 1.55;
}

@media (max-width: 640px) {
  .teacher-lecture-form-section {
    padding: 12px;
  }

  .teacher-lecture-drawer__footer,
  .teacher-lecture-drawer__footer > * {
    width: 100%;
  }

  .teacher-lecture-drawer__footer {
    flex-direction: column-reverse;
  }
}
</style>
