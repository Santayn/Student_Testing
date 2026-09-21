<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'
import {
  useRouter,
} from 'vue-router'

import {
  getApiErrorMessage,
  groupsApi,
  teachingApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiInput,
  UiSelect,
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

const router = useRouter()

const {
  subjectMemberships,
  subjects,
  loadTeacherSubjects,
} = useTeacherSubjects()

const studyCourse = ref(1)
const semester = ref(1)
const academicYear = ref(
  new Date().getFullYear()
)

const assignments = ref([])
const loadTypes = ref([])

const loading = ref(false)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const assignmentsRequest =
  createLatestRequestGuard()

const courseOptions = [1, 2, 3, 4, 5, 6]
  .map((value) => ({
    value,
    label: `${value} курс`,
  }))

const semesterOptions = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
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

const loadTypeById = computed(() => {
  return new Map(
    loadTypes.value.map(
      (item) => [
        Number(item.id),
        item,
      ]
    )
  )
})

const periodLabel = computed(() => {
  return (
    `${studyCourse.value} курс, ` +
    `${semester.value} семестр, ` +
    `${academicYear.value}`
  )
})

const subjectCount = computed(() => {
  return new Set(
    assignments.value
      .map((item) =>
        Number(item.subjectMembershipId)
      )
      .filter(Boolean)
  ).size
})

const groupCount = computed(() => {
  return new Set(
    assignments.value
      .map((item) => Number(item.groupId))
      .filter(Boolean)
  ).size
})

const totalHoursPerWeek = computed(() => {
  return assignments.value.reduce(
    (sum, item) =>
      sum + Number(item.hoursPerWeek ?? 0),
    0
  )
})

const groupedAssignments = computed(() => {
  const groups = new Map()

  assignments.value.forEach(
    (assignment) => {
      const membershipId = Number(
        assignment.subjectMembershipId
      )

      if (!membershipId) {
        return
      }

      if (!groups.has(membershipId)) {
        groups.set(membershipId, [])
      }

      groups.get(membershipId).push(
        assignment
      )
    }
  )

  return [...groups.entries()]
    .map(([subjectMembershipId, items]) => {
      const membership =
        membershipById.value.get(
          Number(subjectMembershipId)
        )

      const subjectId = Number(
        membership?.subjectId
      )

      return {
        subjectMembershipId,
        subjectId,
        subjectName:
          subjectName(subjectId),
        items: [...items].sort(
          (left, right) =>
            groupName(left).localeCompare(
              groupName(right),
              'ru'
            ) ||
            loadTypeName(left.loadTypeId)
              .localeCompare(
                loadTypeName(
                  right.loadTypeId
                ),
                'ru'
              )
        ),
      }
    })
    .sort(
      (left, right) =>
        left.subjectName.localeCompare(
          right.subjectName,
          'ru'
        )
    )
})

function subjectName(subjectId) {
  if (!subjectId) {
    return 'Предмет не определён'
  }

  return (
    subjectById.value.get(
      Number(subjectId)
    )?.name ??
    `Предмет #${subjectId}`
  )
}

function groupName(assignment) {
  return (
    assignment.groupName ||
    assignment.groupCode ||
    `Группа #${assignment.groupId}`
  )
}

function loadTypeName(loadTypeId) {
  return (
    loadTypeById.value.get(
      Number(loadTypeId)
    )?.name ??
    `Тип нагрузки #${loadTypeId}`
  )
}

function statusLabel(status) {
  switch (Number(status)) {
    case 1:
      return 'Активно'
    case 2:
      return 'Черновик'
    case 3:
      return 'Закрыто'
    case 4:
      return 'Приостановлено'
    default:
      return `Статус #${status}`
  }
}

function statusClass(status) {
  switch (Number(status)) {
    case 1:
      return 'teacher-status teacher-status--success'
    case 2:
      return 'teacher-status teacher-status--warning'
    case 3:
      return 'teacher-status'
    case 4:
      return 'teacher-status teacher-status--danger'
    default:
      return 'teacher-status'
  }
}

function formatHours(value) {
  const numeric = Number(value ?? 0)

  if (!Number.isFinite(numeric)) {
    return '0'
  }

  return numeric.toLocaleString('ru-RU', {
    maximumFractionDigits: 2,
  })
}

function subjectRoute(group) {
  if (!group.subjectId) {
    return null
  }

  return {
    name: 'subject-details',
    params: {
      subjectId: group.subjectId,
    },
  }
}

function workloadLectureRoute(group) {
  if (
    !group.subjectId ||
    !group.subjectMembershipId
  ) {
    return null
  }

  return {
    name: 'teacher-lectures',
    query: {
      subjectId: group.subjectId,
      subjectMembershipId:
        group.subjectMembershipId,
    },
  }
}

function openRoute(route) {
  if (!route) {
    return
  }

  router.push(route)
}

async function loadLoadTypes() {
  const response =
    await teachingApi.getLoadTypes()

  loadTypes.value =
    listFromResponse(response)
}

async function refreshAssignments() {
  if (!initialized.value) {
    return
  }

  const requestId =
    assignmentsRequest.begin()

  const periodContext = {
    studyCourse: Number(
      studyCourse.value
    ),
    semester: Number(
      semester.value
    ),
    academicYear: Number(
      academicYear.value
    ),
  }

  const membershipSnapshot =
    subjectMemberships.value.map(
      (membership) => ({
        ...membership,
      })
    )

  loading.value = true
  notice.value.message = ''

  try {
    if (!membershipSnapshot.length) {
      if (
        assignmentsRequest.isCurrent(
          requestId
        )
      ) {
        assignments.value = []
      }

      return
    }

    const responses = await Promise.all(
      membershipSnapshot.map(
        (membership) =>
          teachingApi.getAssignments({
            subjectMembershipId:
              membership.id,
            ...periodContext,
          })
      )
    )

    if (
      !assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    const rawAssignments = responses
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
          .map((item) =>
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

    if (
      !assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

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
  } catch (error) {
    if (
      !assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить назначенную учебную нагрузку.'
      ),
    }
  } finally {
    if (
      assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
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
    await Promise.all([
      loadTeacherSubjects(),
      loadLoadTypes(),
    ])

    initialized.value = true
    await refreshAssignments()
  } catch (error) {
    initialized.value = true

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить данные преподавателя.'
      ),
    }
  }
})
</script>

<template>
  <TeacherPageShell
    title="Моя нагрузка"
    subtitle="Просматривайте учебную нагрузку, назначенную администратором. Изменение предметов, групп, типов нагрузки и часов выполняется только в административном разделе."
  >
    <template #actions>
      <UiButton
        size="sm"
        :loading="loading"
        loading-text="Обновление..."
        @click="refreshAssignments"
      >
        Обновить
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
        <span class="teacher-stat__label">Период</span>
        <span class="teacher-stat__value">{{ periodLabel }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Предметов</span>
        <span class="teacher-stat__value">{{ subjectCount }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Групп</span>
        <span class="teacher-stat__value">{{ groupCount }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Часов в неделю</span>
        <span class="teacher-stat__value">{{ formatHours(totalHoursPerWeek) }}</span>
      </div>
    </section>

    <UiCard
      title="Период"
      description="Фильтр влияет только на просмотр вашей назначенной нагрузки."
    >
      <div class="teacher-grid teacher-grid--3">
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
      title="Назначенная нагрузка"
      description="Страница работает только в режиме просмотра. Изменения нагрузки выполняет администратор системы."
    >
      <UiEmptyState
        v-if="loading"
        description="Загрузка назначенной нагрузки..."
        compact
      />

      <UiEmptyState
        v-else-if="!subjectMemberships.length"
        description="У преподавателя пока нет активных назначений на предметы."
        compact
      />

      <UiEmptyState
        v-else-if="!assignments.length"
        description="По выбранному периоду учебная нагрузка не найдена."
        compact
      />

      <div
        v-else
        class="teacher-stack"
      >
        <section
          v-for="group in groupedAssignments"
          :key="group.subjectMembershipId"
          class="teacher-workload-subject"
        >
          <div class="teacher-workload-subject__header">
            <div class="teacher-workload-subject__heading">
              <span class="teacher-muted">
                Назначение #{{ group.subjectMembershipId }}
              </span>

              <h2 class="teacher-workload-subject__title">
                {{ group.subjectName }}
              </h2>

              <span class="teacher-muted">
                {{ group.items.length }} {{ group.items.length === 1 ? 'запись' : 'записей' }} нагрузки
              </span>
            </div>

            <div class="teacher-actions">
              <UiButton
                size="sm"
                variant="secondary"
                :disabled="!subjectRoute(group)"
                @click="openRoute(subjectRoute(group))"
              >
                К предмету
              </UiButton>

              <UiButton
                size="sm"
                variant="secondary"
                :disabled="!workloadLectureRoute(group)"
                @click="openRoute(workloadLectureRoute(group))"
              >
                Лекции
              </UiButton>
            </div>
          </div>

          <div class="teacher-entity-list">
            <article
              v-for="assignment in group.items"
              :key="assignment.id"
              class="teacher-entity-card"
            >
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  {{ groupName(assignment) }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ loadTypeName(assignment.loadTypeId) }}
                </h3>
              </div>

              <div class="teacher-workload-meta">
                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Статус</span>
                  <span :class="statusClass(assignment.status)">
                    {{ statusLabel(assignment.status) }}
                  </span>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Часов в неделю</span>
                  <strong>{{ formatHours(assignment.hoursPerWeek) }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Группа</span>
                  <strong>{{ groupName(assignment) }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Курс</span>
                  <strong>{{ assignment.studyCourse ?? studyCourse }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Семестр</span>
                  <strong>{{ assignment.semester }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Учебный год</span>
                  <strong>{{ assignment.academicYear }}</strong>
                </div>
              </div>

              <div
                v-if="assignment.courseVersionId"
                class="teacher-muted"
              >
                Версия курса: #{{ assignment.courseVersionId }}
              </div>

              <p
                v-if="assignment.notes"
                class="teacher-workload-notes"
              >
                {{ assignment.notes }}
              </p>
            </article>
          </div>
        </section>
      </div>
    </UiCard>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-workload-subject {
  min-width: 0;
  padding: 14px;

  display: grid;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-workload-subject__header {
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.teacher-workload-subject__heading {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.teacher-workload-subject__title {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: 18px;
  line-height: 1.3;
}

.teacher-workload-meta {
  min-width: 0;

  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.teacher-workload-meta__item {
  min-width: 0;
  padding: 9px 10px;

  display: grid;
  align-content: start;
  gap: 4px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-workload-meta__item strong {
  overflow-wrap: anywhere;
}

.teacher-workload-notes {
  margin: 0;
  padding-top: 10px;

  color: var(--st-text-secondary);

  border-top: 1px solid var(--st-border);

  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .teacher-workload-meta {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .teacher-workload-meta {
    grid-template-columns: 1fr;
  }

  .teacher-workload-subject__header .teacher-actions,
  .teacher-workload-subject__header .teacher-actions > * {
    width: 100%;
  }
}
</style>
