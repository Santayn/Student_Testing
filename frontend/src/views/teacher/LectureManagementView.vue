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
  UiEmptyState,
  UiFileInput,
  UiInput,
  UiSelect,
  UiTable,
  UiTextarea,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  listFromResponse,
} from '@/utils/apiData'

const route = useRoute()

const {
  loadingSubjects,
  selectedSubjectId,
  selectedSubject,
  selectedMembership,
  subjectOptions,
  loadTeacherSubjects,
} = useTeacherSubjects()

const lectures = ref([])
const availableTests = ref([])
const lectureTestsById = ref(
  new Map()
)
const materials = ref([])
const pendingFiles = ref([])
const fileInputKey = ref(0)

const loading = ref(false)
const saving = ref(false)
const deletingId = ref(null)
const loadingMaterials = ref(false)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const form = ref({
  id: null,
  title: '',
  description: '',
  publicVisible: true,
  testIds: [],
})

const lectureColumns = [
  {
    key: 'ordinal',
    label: '#',
  },
  {
    key: 'title',
    label: 'Лекция',
  },
  {
    key: 'tests',
    label: 'Тесты',
    value: (row) =>
      lectureTestSummary(row.id),
    sortValue: (row) =>
      lectureTestSummary(row.id),
  },
  {
    key: 'publicVisible',
    label: 'Статус',
    value: (row) =>
      row.publicVisible
        ? 'Видима'
        : 'Скрыта',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

const canEdit = computed(() => {
  return Boolean(
    selectedMembership.value
  )
})

function routeQuery() {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId =
      selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId =
      selectedMembership.value.id
  }

  return query
}

function nextOrdinal() {
  return lectures.value.reduce(
    (max, lecture) =>
      Math.max(
        max,
        Number(lecture.ordinal ?? 0)
      ),
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

function buildLectureContentKey(
  title,
  lectureId = null
) {
  const suffix =
    lectureId || Date.now()

  return (
    `lecture-${suffix}-` +
    slugifyLectureTitle(title)
  )
}

function lectureTests(lectureId) {
  return (
    lectureTestsById.value.get(
      Number(lectureId)
    ) ?? []
  )
}

function lectureTestSummary(lectureId) {
  const tests = lectureTests(lectureId)

  if (!tests.length) {
    return 'Нет связанных тестов'
  }

  return tests
    .map(
      (test) =>
        test.title || `Тест #${test.id}`
    )
    .join(', ')
}

function resetForm() {
  form.value = {
    id: null,
    title: '',
    description: '',
    publicVisible: true,
    testIds: [],
  }

  materials.value = []
  pendingFiles.value = []
  fileInputKey.value += 1
}

async function loadLectures() {
  lectures.value = []
  availableTests.value = []
  lectureTestsById.value = new Map()
  resetForm()

  if (!selectedMembership.value) {
    return
  }

  loading.value = true

  try {
    const lecturesResponse =
      await lecturesApi.getAll({
        subjectMembershipId:
          selectedMembership.value.id,
      })

    lectures.value =
      listFromResponse(lecturesResponse)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
              Number(right.ordinal ?? 0) ||
            String(left.title ?? '')
              .localeCompare(
                String(right.title ?? ''),
                'ru'
              )
        )

    const [testsResponse, testLists] =
      await Promise.all([
        testsApi.getAll({
          subjectId:
            Number(selectedSubjectId.value),
        }),
        Promise.all(
          lectures.value.map(
            (lecture) =>
              lecturesApi.getTests(
                lecture.id
              )
          )
        ),
      ])

    availableTests.value =
      listFromResponse(testsResponse)
        .sort(
          (left, right) =>
            String(left.title ?? '')
              .localeCompare(
                String(right.title ?? ''),
                'ru'
              )
        )

    lectureTestsById.value =
      new Map(
        lectures.value.map(
          (lecture, index) => [
            Number(lecture.id),
            listFromResponse(
              testLists[index]
            ),
          ]
        )
      )

    const lectureId =
      route.query.lectureId

    if (lectureId) {
      const lecture =
        lectures.value.find(
          (item) =>
            String(item.id) ===
            String(lectureId)
        )

      if (lecture) {
        await editLecture(lecture)
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить лекции'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function loadMaterials(lectureId) {
  materials.value = []

  if (!lectureId) {
    return
  }

  loadingMaterials.value = true

  try {
    const response =
      await lecturesApi.getMaterials(
        lectureId
      )

    materials.value =
      listFromResponse(response)
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить материалы лекции'
      ),
    }
  } finally {
    loadingMaterials.value = false
  }
}

async function editLecture(lecture) {
  form.value = {
    id: lecture.id,
    title: lecture.title || '',
    description:
      lecture.description || '',
    publicVisible:
      Boolean(lecture.publicVisible),
    testIds: lectureTests(lecture.id)
      .map((test) => Number(test.id)),
  }

  pendingFiles.value = []
  fileInputKey.value += 1

  await loadMaterials(lecture.id)
}

async function syncLectureTests(
  lectureId,
  selectedTestIds
) {
  const response =
    await lecturesApi.setTests(
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

  const next = new Map(
    lectureTestsById.value
  )

  next.set(
    Number(lectureId),
    listFromResponse(response)
  )

  lectureTestsById.value = next
}

async function uploadPendingFiles(
  lectureId
) {
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
  if (
    !selectedMembership.value ||
    !selectedSubject.value ||
    !form.value.title.trim()
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Заполните название лекции и выберите предмет.',
    }
    return
  }

  const editingLecture =
    lectures.value.find(
      (item) =>
        Number(item.id) ===
        Number(form.value.id)
    ) ?? null

  const payload = {
    subjectId:
      Number(selectedSubject.value.id),
    subjectMembershipId:
      Number(selectedMembership.value.id),
    courseVersionId: null,
    ordinal:
      editingLecture?.ordinal ||
      nextOrdinal(),
    title: form.value.title.trim(),
    description:
      form.value.description.trim() ||
      null,
    contentFolderKey:
      editingLecture?.contentFolderKey ||
      buildLectureContentKey(
        form.value.title,
        editingLecture?.id || null
      ),
    linkedTestId: null,
    publicVisible:
      form.value.publicVisible,
  }

  saving.value = true

  try {
    const response = form.value.id
      ? await lecturesApi.update(
          form.value.id,
          payload
        )
      : await lecturesApi.create(payload)

    const lecture = response.data

    await syncLectureTests(
      lecture.id,
      form.value.testIds
    )

    await uploadPendingFiles(
      lecture.id
    )

    notice.value = {
      type: 'success',
      message: form.value.id
        ? 'Лекция обновлена.'
        : 'Лекция создана.',
    }

    resetForm()
    await loadLectures()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить лекцию'
      ),
    }
  } finally {
    saving.value = false
  }
}

async function deleteLecture(lecture) {
  if (
    !window.confirm(
      `Удалить лекцию «${lecture.title}»?`
    )
  ) {
    return
  }

  deletingId.value = lecture.id

  try {
    await lecturesApi.remove(lecture.id)

    notice.value = {
      type: 'success',
      message: 'Лекция удалена.',
    }

    resetForm()
    await loadLectures()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось удалить лекцию'
      ),
    }
  } finally {
    deletingId.value = null
  }
}

function onFiles(files) {
  pendingFiles.value = files
}

function removePendingFile(index) {
  pendingFiles.value =
    pendingFiles.value.filter(
      (_, itemIndex) =>
        itemIndex !== index
    )
}

async function removeMaterial(material) {
  if (
    !form.value.id ||
    !window.confirm(
      'Удалить файл лекции?'
    )
  ) {
    return
  }

  try {
    await lecturesApi.removeMaterial(
      form.value.id,
      material.id
    )

    materials.value =
      materials.value.filter(
        (item) =>
          Number(item.id) !==
          Number(material.id)
      )
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось удалить материал'
      ),
    }
  }
}

async function downloadMaterial(material) {
  if (!form.value.id) {
    return
  }

  try {
    const response =
      await lecturesApi.downloadMaterial(
        form.value.id,
        material.id
      )

    const url = URL.createObjectURL(
      response.data
    )

    const anchor =
      document.createElement('a')

    anchor.href = url
    anchor.download =
      material.fileName ||
      `material-${material.id}`

    anchor.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось скачать материал'
      ),
    }
  }
}

watch(
  selectedSubjectId,
  () => {
    if (initialized.value) {
      loadLectures()
    }
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects({
      preferredSubjectId:
        route.query.subjectId,
      preferredMembershipId:
        route.query
          .subjectMembershipId,
    })

    initialized.value = true

    if (selectedSubjectId.value) {
      await loadLectures()
    }

    if (!subjectOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет предметов преподавателя для управления лекциями.',
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        error.message
      ),
    }
  }
})
</script>

<template>
  <TeacherPageShell
    title="Лекции предмета"
    subtitle="Создание лекций, прикрепление материалов и связывание лекций с тестами выбранного предмета."
  >
    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <div class="teacher-layout">
      <div class="teacher-stack">
        <UiCard
          title="Контекст предмета"
          description="Выберите предмет преподавателя."
        >
          <div class="teacher-stack">
            <UiSelect
              v-model="selectedSubjectId"
              label="Предмет"
              :options="subjectOptions"
              placeholder="Выберите предмет"
              :disabled="
                loadingSubjects ||
                !subjectOptions.length
              "
            />

            <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
              <UiButton
                :to="{
                  name: 'teacher-topics',
                  query: routeQuery(),
                }"
              >
                Тематики
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
          :title="
            form.id
              ? 'Редактирование лекции'
              : 'Новая лекция'
          "
        >
          <div class="teacher-stack">
            <UiInput
              v-model="form.title"
              label="Название"
              maxlength="200"
              :disabled="!canEdit"
              required
            />

            <UiTextarea
              v-model="form.description"
              label="Описание"
              maxlength="2000"
              :disabled="!canEdit"
            />

            <div>
              <span class="teacher-field-label">
                Связанные тесты
              </span>

              <UiEmptyState
                v-if="!availableTests.length"
                description="Тесты пока недоступны."
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
                  :value="test.id"
                  :label="test.title || `Тест #${test.id}`"
                  :description="test.description || ''"
                  :disabled="!canEdit"
                />
              </div>
            </div>

            <UiFileInput
              :key="fileInputKey"
              label="Материалы лекции"
              hint="Можно прикрепить несколько рабочих файлов. Старый интерфейс использовал ограничение до 50 МБ на файл."
              multiple
              :disabled="!canEdit"
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
                  @click="removePendingFile(index)"
                >
                  Убрать
                </UiButton>
              </div>
            </div>

            <div
              v-if="form.id"
              class="teacher-stack"
            >
              <span class="teacher-field-label">
                Загруженные материалы
              </span>

              <UiEmptyState
                v-if="loadingMaterials"
                description="Загрузка материалов..."
                compact
              />

              <UiEmptyState
                v-else-if="!materials.length"
                description="Файлы пока не добавлены."
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
                  <span>
                    {{ material.fileName || `Материал #${material.id}` }}
                  </span>

                  <div class="teacher-inline-actions">
                    <UiButton
                      size="sm"
                      @click="downloadMaterial(material)"
                    >
                      Скачать
                    </UiButton>

                    <UiButton
                      variant="danger"
                      size="sm"
                      @click="removeMaterial(material)"
                    >
                      Удалить
                    </UiButton>
                  </div>
                </div>
              </div>
            </div>

            <UiCheckbox
              v-model="form.publicVisible"
              label="Публиковать лекцию"
              :disabled="!canEdit"
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="saving"
                loading-text="Сохранение..."
                :disabled="!canEdit"
                @click="saveLecture"
              >
                Сохранить лекцию
              </UiButton>

              <UiButton
                v-if="form.id"
                @click="resetForm"
              >
                Отмена
              </UiButton>
            </div>
          </div>
        </UiCard>
      </div>

      <div class="teacher-stack">
        <UiCard
          title="Список лекций"
          :description="
            selectedSubject
              ? `Предмет: ${selectedSubject.name}. Всего: ${lectures.length}.`
              : 'Предмет не выбран.'
          "
        >
          <UiTable
            :columns="lectureColumns"
            :rows="lectures"
            :loading="loading"
            empty-message="Для выбранного предмета пока нет лекций."
            :default-sort="{
              key: 'ordinal',
              direction: 'asc',
            }"
          >
            <template #cell-title="{ row }">
              <div class="teacher-stack">
                <strong>{{ row.title }}</strong>
                <span class="teacher-muted">
                  {{ row.description || 'Без описания' }}
                </span>
              </div>
            </template>

            <template #cell-publicVisible="{ row }">
              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success':
                    row.publicVisible,
                }"
              >
                {{ row.publicVisible ? 'Видима' : 'Скрыта' }}
              </span>
            </template>

            <template #cell-actions="{ row }">
              <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
                <UiButton
                  size="sm"
                  @click="editLecture(row)"
                >
                  Изменить
                </UiButton>

                <UiButton
                  variant="danger"
                  size="sm"
                  :loading="deletingId === row.id"
                  loading-text="Удаление..."
                  @click="deleteLecture(row)"
                >
                  Удалить
                </UiButton>
              </div>
            </template>
          </UiTable>
        </UiCard>

        <UiCard
          title="Связанный поток"
          description="Лекции становятся опорой для тестов и учебных назначений. Порядок и ключ контента формируются автоматически, а к одной лекции можно привязать несколько тестов."
        />
      </div>
    </div>
  </TeacherPageShell>
</template>
