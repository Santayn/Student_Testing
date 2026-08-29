<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  getApiErrorMessage,
  groupsApi,
  lecturesApi,
  teachingApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiSelect,
  UiTable,
} from '@/components/ui'

import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'

import {
  useTeacherSubjects,
} from '@/composables/useTeacherSubjects'

import {
  listFromResponse,
} from '@/utils/apiData'

const {
  subjectMemberships,
  subjects,
  loadTeacherSubjects,
} = useTeacherSubjects()

let rowSequence = 0

const studyCourse = ref(1)
const semester = ref(1)
const academicYear = ref(
  new Date().getFullYear()
)

const assignments = ref([])
const lectureAssignmentsByTeachingId =
  ref(new Map())
const lectureCatalogBySubjectId =
  ref(new Map())

const subjectRows = ref([
  createSubjectRow(),
])

const loading = ref(false)
const assigning = ref(false)
const savingStatusId = ref(null)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const courseOptions = [1, 2, 3, 4, 5, 6]
  .map((value) => ({
    value,
    label: `${value} курс`,
  }))

const semesterOptions = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
]

const statusOptions = [
  { value: 1, label: 'Активно' },
  { value: 2, label: 'Черновик' },
  { value: 3, label: 'Закрыто' },
  { value: 4, label: 'В паузе' },
]

const membershipById = computed(() => {
  return new Map(
    subjectMemberships.value.map(
      (item) => [
        Number(item.id),
        item,
      ]
    )
  )
})

const membershipBySubjectId = computed(() => {
  return new Map(
    subjectMemberships.value.map(
      (item) => [
        Number(item.subjectId),
        item,
      ]
    )
  )
})

const subjectById = computed(() => {
  return new Map(
    subjects.value.map(
      (item) => [
        Number(item.id),
        item,
      ]
    )
  )
})

const activeAssignments = computed(() => {
  return assignments.value.filter(
    (item) =>
      Number(item.status) === 1
  )
})

const availableSubjectIds = computed(() => {
  return [
    ...new Set(
      activeAssignments.value
        .map(assignmentSubjectId)
        .filter(Boolean)
        .map(Number)
    ),
  ]
})

const currentLectureAssignments = computed(() => {
  return [
    ...lectureAssignmentsByTeachingId
      .value.values(),
  ].flat()
})

const activeGroupCount = computed(() => {
  return new Set(
    activeAssignments.value
      .map((item) => Number(item.groupId))
      .filter(Boolean)
  ).size
})

const selectedTasks = computed(() => {
  return subjectRows.value.flatMap(
    (row) => {
      if (!row.subjectId) {
        return []
      }

      const lectureIds = [
        ...new Set(
          row.lectureIds
            .map(Number)
            .filter(Boolean)
        ),
      ]

      const assignmentIds = [
        ...new Set(
          row.teachingAssignmentIds
            .map(Number)
            .filter(Boolean)
        ),
      ]

      return lectureIds.flatMap(
        (lectureId) =>
          assignmentIds.map(
            (teachingAssignmentId) => ({
              rowId: row.id,
              subjectId:
                Number(row.subjectId),
              lectureId,
              teachingAssignmentId,
            })
          )
      )
    }
  )
})

const pendingTasks = computed(() => {
  return selectedTasks.value.filter(
    (task) =>
      !existingLectureAssignment(
        task
      )
  )
})

const duplicateTasks = computed(() => {
  return selectedTasks.value.filter(
    (task) =>
      existingLectureAssignment(task)
  )
})

const incompleteRows = computed(() => {
  return subjectRows.value.filter(
    (row) =>
      row.subjectId &&
      (!row.lectureIds.length ||
        !row.teachingAssignmentIds.length)
  )
})

const canAssign = computed(() => {
  return (
    pendingTasks.value.length > 0 &&
    incompleteRows.value.length === 0 &&
    !assigning.value
  )
})

const periodLabel = computed(() => {
  return (
    `${studyCourse.value} курс, ` +
    `${semester.value} семестр, ` +
    `${academicYear.value}`
  )
})

const groupedCurrentAssignments = computed(() => {
  const assignmentById = new Map(
    assignments.value.map(
      (item) => [
        Number(item.id),
        item,
      ]
    )
  )

  const groups = new Map()

  currentLectureAssignments.value
    .forEach((lectureAssignment) => {
      const assignment =
        assignmentById.get(
          Number(
            lectureAssignment
              .teachingAssignmentId
          )
        )

      if (!assignment) {
        return
      }

      const subjectId =
        assignmentSubjectId(assignment)

      if (!groups.has(subjectId)) {
        groups.set(subjectId, [])
      }

      groups.get(subjectId).push({
        ...lectureAssignment,
        assignment,
        subjectId,
        lecture:
          lectureMeta(
            lectureAssignment
              .courseLectureId
          ),
      })
    })

  return [...groups.entries()]
    .map(([subjectId, items]) => ({
      subjectId,
      subjectName:
        subjectName(subjectId),
      items: items.sort(
        (left, right) =>
          Number(
            left.lecture?.ordinal ?? 0
          ) -
            Number(
              right.lecture?.ordinal ?? 0
            ) ||
          String(
            left.lecture?.title ?? ''
          ).localeCompare(
            String(
              right.lecture?.title ?? ''
            ),
            'ru'
          ) ||
          String(
            groupName(
              left.assignment.groupId
            )
          ).localeCompare(
            String(
              groupName(
                right.assignment.groupId
              )
            ),
            'ru'
          )
      ),
    }))
    .sort(
      (left, right) =>
        left.subjectName.localeCompare(
          right.subjectName,
          'ru'
        )
    )
})

const currentColumns = [
  {
    key: 'lecture',
    label: 'Лекция',
    value: (row) =>
      row.lecture
        ? `${row.lecture.ordinal}. ${row.lecture.title}`
        : `Лекция #${row.courseLectureId}`,
  },
  {
    key: 'group',
    label: 'Группа',
    value: (row) =>
      groupName(row.assignment.groupId),
  },
  {
    key: 'status',
    label: 'Статус',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

function createSubjectRow(
  subjectId = null
) {
  rowSequence += 1

  return {
    id: rowSequence,
    subjectId,
    lectureIds: [],
    teachingAssignmentIds: [],
  }
}

function subjectName(subjectId) {
  return (
    subjectById.value.get(
      Number(subjectId)
    )?.name ??
    `Предмет #${subjectId}`
  )
}

function assignmentSubjectId(
  assignment
) {
  return (
    membershipById.value.get(
      Number(
        assignment.subjectMembershipId
      )
    )?.subjectId ?? null
  )
}

function groupName(groupId) {
  const assignment =
    assignments.value.find(
      (item) =>
        Number(item.groupId) ===
        Number(groupId)
    )

  return (
    assignment?.groupName ||
    assignment?.groupCode ||
    `Группа #${groupId}`
  )
}

function activeAssignmentsForSubject(
  subjectId
) {
  return activeAssignments.value.filter(
    (assignment) =>
      Number(
        assignmentSubjectId(
          assignment
        )
      ) === Number(subjectId)
  )
}

function lectureCatalog(subjectId) {
  return (
    lectureCatalogBySubjectId.value.get(
      Number(subjectId)
    ) ?? []
  )
}

function lectureMeta(lectureId) {
  const requested = Number(lectureId)

  for (
    const lectures of
    lectureCatalogBySubjectId.value.values()
  ) {
    const lecture = lectures.find(
      (item) =>
        Number(item.id) === requested
    )

    if (lecture) {
      return lecture
    }
  }

  return null
}

function existingLectureAssignment(task) {
  return (
    lectureAssignmentsByTeachingId.value
      .get(
        Number(
          task.teachingAssignmentId
        )
      ) ?? []
  ).find(
    (item) =>
      Number(item.courseLectureId) ===
      Number(task.lectureId)
  ) ?? null
}

function subjectOptionsForRow(row) {
  const selectedElsewhere = new Set(
    subjectRows.value
      .filter(
        (item) =>
          item.id !== row.id &&
          item.subjectId
      )
      .map(
        (item) =>
          Number(item.subjectId)
      )
  )

  return availableSubjectIds.value
    .filter(
      (subjectId) =>
        !selectedElsewhere.has(
          Number(subjectId)
        ) ||
        Number(subjectId) ===
          Number(row.subjectId)
    )
    .map((subjectId) => ({
      value: subjectId,
      label: subjectName(subjectId),
    }))
}

function normalizeRows() {
  const available = new Set(
    availableSubjectIds.value
  )

  subjectRows.value.forEach(
    (row) => {
      if (
        !row.subjectId ||
        !available.has(
          Number(row.subjectId)
        )
      ) {
        row.lectureIds = []
        row.teachingAssignmentIds = []
        return
      }

      const assignmentIds = new Set(
        activeAssignmentsForSubject(
          row.subjectId
        ).map(
          (item) => Number(item.id)
        )
      )

      row.teachingAssignmentIds =
        row.teachingAssignmentIds
          .map(Number)
          .filter(
            (id) =>
              assignmentIds.has(id)
          )

      const lectureIds = new Set(
        lectureCatalog(row.subjectId)
          .map((item) => Number(item.id))
      )

      row.lectureIds =
        row.lectureIds
          .map(Number)
          .filter(
            (id) =>
              lectureIds.has(id)
          )
    }
  )

  if (!subjectRows.value.length) {
    subjectRows.value = [
      createSubjectRow(),
    ]
  }
}

async function ensureLectureCatalog(
  subjectId
) {
  const numericSubjectId =
    Number(subjectId)

  if (
    !numericSubjectId ||
    lectureCatalogBySubjectId.value.has(
      numericSubjectId
    )
  ) {
    return
  }

  const membership =
    membershipBySubjectId.value.get(
      numericSubjectId
    )

  if (!membership) {
    return
  }

  const response =
    await lecturesApi.getAll({
      subjectMembershipId:
        membership.id,
    })

  const next = new Map(
    lectureCatalogBySubjectId.value
  )

  next.set(
    numericSubjectId,
    listFromResponse(response)
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
  )

  lectureCatalogBySubjectId.value = next
}

async function loadLectureAssignments() {
  const pairs =
    await Promise.all(
      assignments.value.map(
        async (assignment) => ({
          teachingAssignmentId:
            assignment.id,
          response:
            await teachingApi
              .getLectureAssignments({
                teachingAssignmentId:
                  assignment.id,
              }),
        })
      )
    )

  lectureAssignmentsByTeachingId.value =
    new Map(
      pairs.map((pair) => [
        Number(
          pair.teachingAssignmentId
        ),
        listFromResponse(
          pair.response
        ),
      ])
    )
}

async function refreshAssignments() {
  if (!initialized.value) {
    return
  }

  loading.value = true
  notice.value.message = ''

  try {
    if (!subjectMemberships.value.length) {
      assignments.value = []
      lectureAssignmentsByTeachingId.value =
        new Map()
      lectureCatalogBySubjectId.value =
        new Map()
      normalizeRows()
      return
    }

    const responses =
      await Promise.all(
        subjectMemberships.value.map(
          (membership) =>
            teachingApi.getAssignments({
              subjectMembershipId:
                membership.id,
              studyCourse:
                Number(studyCourse.value),
              semester:
                Number(semester.value),
              academicYear:
                Number(academicYear.value),
            })
        )
      )

    const rawAssignments =
      responses
        .flatMap(listFromResponse)
        .filter(
          (item, index, items) =>
            items.findIndex(
              (other) =>
                Number(other.id) ===
                Number(item.id)
            ) === index
        )

    const groupIds = [
      ...new Set(
        rawAssignments
          .map(
            (item) =>
              Number(item.groupId)
          )
          .filter(Boolean)
      ),
    ]

    const groupResponses =
      await Promise.all(
        groupIds.map(
          (groupId) =>
            groupsApi.getById(groupId)
        )
      )

    const groupsById = new Map(
      groupResponses
        .map((response) => response.data)
        .filter(Boolean)
        .map((group) => [
          Number(group.id),
          group,
        ])
    )

    assignments.value =
      rawAssignments.map(
        (item) => ({
          ...item,
          groupName:
            groupsById.get(
              Number(item.groupId)
            )?.name ?? null,
          groupCode:
            groupsById.get(
              Number(item.groupId)
            )?.code ?? null,
        })
      )

    await loadLectureAssignments()

    lectureCatalogBySubjectId.value =
      new Map()

    await Promise.all(
      availableSubjectIds.value.map(
        ensureLectureCatalog
      )
    )

    normalizeRows()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить персональную нагрузку'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function changeRowSubject(
  row,
  subjectId
) {
  row.subjectId = subjectId
    ? Number(subjectId)
    : null

  row.lectureIds = []
  row.teachingAssignmentIds = []

  if (row.subjectId) {
    await ensureLectureCatalog(
      row.subjectId
    )
  }
}

function addRow() {
  subjectRows.value.push(
    createSubjectRow()
  )
}

function removeRow(rowId) {
  subjectRows.value =
    subjectRows.value.filter(
      (row) => row.id !== rowId
    )

  if (!subjectRows.value.length) {
    subjectRows.value = [
      createSubjectRow(),
    ]
  }
}

async function assignLectures() {
  if (incompleteRows.value.length) {
    notice.value = {
      type: 'danger',
      message:
        'Заполните все выбранные строки: в каждой строке должны быть предмет, лекции и группы.',
    }
    return
  }

  if (!pendingTasks.value.length) {
    notice.value = {
      type: 'danger',
      message:
        'Нет новых назначений для сохранения.',
    }
    return
  }

  assigning.value = true

  try {
    const results =
      await Promise.allSettled(
        pendingTasks.value.map(
          (task) =>
            teachingApi
              .createLectureAssignment(
                task.teachingAssignmentId,
                {
                  courseLectureId:
                    Number(task.lectureId),
                  availableFromUtc: null,
                  dueToUtc: null,
                  closedAtUtc: null,
                  required: true,
                  minProgressPercent: 100,
                  status: 1,
                }
              )
        )
      )

    const successCount =
      results.filter(
        (item) =>
          item.status === 'fulfilled'
      ).length

    const errors = results
      .filter(
        (item) =>
          item.status === 'rejected'
      )
      .map(
        (item) =>
          getApiErrorMessage(
            item.reason,
            'Не удалось создать назначение.'
          )
      )

    notice.value = {
      type: errors.length
        ? 'warning'
        : 'success',
      message:
        `Создано назначений: ${successCount}.` +
        (errors.length
          ? ` Ошибки: ${errors.join(' | ')}`
          : ''),
    }

    await refreshAssignments()
  } finally {
    assigning.value = false
  }
}

async function saveStatus(row) {
  savingStatusId.value = row.id

  try {
    await teachingApi
      .updateLectureAssignmentStatus(
        row.id,
        {
          status: Number(row.status),
        }
      )

    notice.value = {
      type: 'success',
      message:
        'Статус назначения обновлён.',
    }

    await refreshAssignments()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось обновить статус назначения'
      ),
    }
  } finally {
    savingStatusId.value = null
  }
}

watch(
  [studyCourse, semester, academicYear],
  () => {
    refreshAssignments()
  }
)

onMounted(async () => {
  try {
    await loadTeacherSubjects()
    initialized.value = true

    if (!subjectMemberships.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет предметов преподавателя для учебной нагрузки.',
      }
    }

    await refreshAssignments()
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
    title="Персональная нагрузка"
    subtitle="Соберите выдачу лекций: выберите предмет, лекции и группы из вашей текущей нагрузки по выбранному периоду."
  >
    <template #actions>
      <UiButton
        :to="{
          name: 'teacher-lectures',
        }"
      >
        Лекции
      </UiButton>
    </template>

    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <section class="teacher-stat-grid">
      <div class="teacher-stat">
        <span class="teacher-stat__label">
          Период
        </span>
        <span class="teacher-stat__value">
          {{ periodLabel }}
        </span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">
          Предметов в нагрузке
        </span>
        <span class="teacher-stat__value">
          {{ availableSubjectIds.length }}
        </span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">
          Групп в выборке
        </span>
        <span class="teacher-stat__value">
          {{ activeGroupCount }}
        </span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">
          Назначенных лекций
        </span>
        <span class="teacher-stat__value">
          {{ currentLectureAssignments.length }}
        </span>
      </div>
    </section>

    <div class="teacher-layout">
      <div class="teacher-stack">
        <UiCard title="Период нагрузки">
          <div class="teacher-grid--3 teacher-grid">
            <UiSelect
              v-model="studyCourse"
              label="Курс"
              :options="courseOptions"
            />

            <UiSelect
              v-model="semester"
              label="Семестр"
              :options="semesterOptions"
            />

            <UiInput
              v-model="academicYear"
              label="Учебный год"
              type="number"
              min="2000"
              step="1"
              required
            />
          </div>
        </UiCard>

        <UiCard
          title="Предметы, лекции и группы"
          description="Каждая строка описывает одну комбинацию: предмет, выбранные лекции и учебные группы из текущей нагрузки по периоду."
        >
          <template #actions>
            <UiButton
              size="sm"
              :disabled="!availableSubjectIds.length"
              @click="addRow"
            >
              Добавить предмет
            </UiButton>
          </template>

          <UiEmptyState
            v-if="loading"
            description="Загрузка текущей нагрузки..."
            compact
          />

          <UiEmptyState
            v-else-if="!availableSubjectIds.length"
            description="По выбранному периоду активных назначений пока нет."
            compact
          />

          <div
            v-else
            class="teacher-stack"
          >
            <article
              v-for="row in subjectRows"
              :key="row.id"
              class="teacher-row-card"
            >
              <div class="teacher-row-card__header">
                <UiSelect
                  :model-value="row.subjectId || ''"
                  label="Предмет"
                  :options="subjectOptionsForRow(row)"
                  placeholder="Выберите предмет"
                  @update:model-value="
                    changeRowSubject(
                      row,
                      $event
                    )
                  "
                />

                <UiButton
                  variant="danger"
                  size="sm"
                  :disabled="subjectRows.length === 1"
                  @click="removeRow(row.id)"
                >
                  Удалить
                </UiButton>
              </div>

              <div class="teacher-selection-grid">
                <div class="teacher-stack">
                  <strong>Лекции</strong>

                  <UiEmptyState
                    v-if="!row.subjectId"
                    description="Сначала выберите предмет."
                    compact
                  />

                  <UiEmptyState
                    v-else-if="!lectureCatalog(row.subjectId).length"
                    description="У этого предмета пока нет лекций."
                    compact
                  />

                  <div
                    v-else
                    class="teacher-scroll-list"
                  >
                    <UiCheckbox
                      v-for="lecture in lectureCatalog(row.subjectId)"
                      :key="lecture.id"
                      v-model="row.lectureIds"
                      :value="lecture.id"
                      :label="`${lecture.ordinal}. ${lecture.title}`"
                      :description="lecture.description || 'Без описания'"
                    />
                  </div>
                </div>

                <div class="teacher-stack">
                  <strong>Группы</strong>

                  <UiEmptyState
                    v-if="!row.subjectId"
                    description="Сначала выберите предмет."
                    compact
                  />

                  <UiEmptyState
                    v-else-if="!activeAssignmentsForSubject(row.subjectId).length"
                    description="Для выбранного предмета нет активных групп по периоду."
                    compact
                  />

                  <div
                    v-else
                    class="teacher-scroll-list"
                  >
                    <UiCheckbox
                      v-for="assignment in activeAssignmentsForSubject(row.subjectId)"
                      :key="assignment.id"
                      v-model="row.teachingAssignmentIds"
                      :value="assignment.id"
                      :label="groupName(assignment.groupId)"
                      :description="periodLabel"
                    />
                  </div>
                </div>
              </div>
            </article>
          </div>
        </UiCard>

        <UiCard
          title="План назначений"
          :description="
            `Новых: ${pendingTasks.length}. Уже назначено: ${duplicateTasks.length}. Всего комбинаций: ${selectedTasks.length}.`
          "
        >
          <div class="teacher-stack">
            <UiEmptyState
              v-if="!selectedTasks.length"
              description="Выберите предмет, лекции и группы."
              compact
            />

            <div
              v-else
              class="teacher-list"
            >
              <div
                v-for="row in subjectRows.filter(item => item.subjectId)"
                :key="row.id"
                class="teacher-list-item"
              >
                <strong>
                  {{ subjectName(row.subjectId) }}
                </strong>

                <span class="teacher-muted">
                  Лекций: {{ row.lectureIds.length }},
                  групп: {{ row.teachingAssignmentIds.length }}.
                </span>
              </div>
            </div>

            <UiButton
              variant="primary"
              size="lg"
              :loading="assigning"
              loading-text="Назначение..."
              :disabled="!canAssign"
              @click="assignLectures"
            >
              Назначить выбранные лекции
            </UiButton>
          </div>
        </UiCard>
      </div>

      <UiCard
        title="Текущие назначения лекций"
        description="Просматривайте уже выданные лекции по группам. Статус можно менять прямо здесь."
      >
        <UiEmptyState
          v-if="loading"
          description="Загрузка назначений..."
          compact
        />

        <UiEmptyState
          v-else-if="!assignments.length"
          description="По выбранному периоду учебная нагрузка не найдена."
          compact
        />

        <UiEmptyState
          v-else-if="!groupedCurrentAssignments.length"
          description="Для активных групп назначенных лекций пока нет."
          compact
        />

        <div
          v-else
          class="teacher-stack"
        >
          <UiCard
            v-for="group in groupedCurrentAssignments"
            :key="group.subjectId"
            :title="group.subjectName"
            :description="`${group.items.length} назначений / ${periodLabel}`"
            compact
          >
            <UiTable
              :columns="currentColumns"
              :rows="group.items"
              :default-sort="{
                key: 'lecture',
                direction: 'asc',
              }"
            >
              <template #cell-status="{ row }">
                <UiSelect
                  v-model="row.status"
                  :options="statusOptions"
                  size="sm"
                />
              </template>

              <template #cell-actions="{ row }">
                <UiButton
                  variant="primary"
                  size="sm"
                  :loading="savingStatusId === row.id"
                  loading-text="Сохранение..."
                  @click="saveStatus(row)"
                >
                  Сохранить
                </UiButton>
              </template>
            </UiTable>
          </UiCard>
        </div>
      </UiCard>
    </div>
  </TeacherPageShell>
</template>
