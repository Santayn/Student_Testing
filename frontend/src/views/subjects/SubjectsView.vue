<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  subjectsApi,
  teachingApi,
} from '@/api'

import SubjectsPageShell from '@/components/subjects/SubjectsPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiInput,
  UiTable,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

const SUBJECT_ROLE_TEACHER = 1
const GROUP_ROLE_STUDENT = 1

const authStore =
  useAuthStore()

const loading = ref(false)
const error = ref('')

const subjects = ref([])
const group = ref(null)
const faculty = ref(null)

const filter = ref('')

const columns = [
  {
    key: 'name',
    label: 'Предмет',
    value: (row) =>
      row.name ||
      `Предмет #${row.id}`,
  },
  {
    key: 'id',
    label: 'ID',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

const filteredSubjects = computed(() => {
  const query =
    filter.value
      .trim()
      .toLowerCase()

  if (!query) {
    return subjects.value
  }

  return subjects.value.filter(
    (subject) =>
      String(
        subject.name ?? ''
      )
        .toLowerCase()
        .includes(query)
  )
})

const metaText = computed(() => {
  if (authStore.isStudent) {
    if (!group.value) {
      return (
        'Для текущего пользователя ' +
        'не найдена активная учебная группа.'
      )
    }

    const groupName =
      group.value.name ||
      group.value.code ||
      group.value.id

    const facultyName =
      faculty.value?.name ||
      faculty.value?.id ||
      '-'

    return (
      `Группа: ${groupName}, ` +
      `факультет: ${facultyName}`
    )
  }

  if (
    authStore.isTeacher ||
    authStore.isAdmin
  ) {
    return (
      'Показаны предметы, доступные ' +
      'преподавателю или администратору.'
    )
  }

  return (
    'Для вашей роли доступных ' +
    'предметов не найдено.'
  )
})

function subjectRoute(subject) {
  const query = {}

  if (faculty.value?.id) {
    query.facultyId =
      faculty.value.id
  }

  return {
    name: 'subject-details',

    params: {
      subjectId: subject.id,
    },

    query,
  }
}

async function loadTeacherSubjects() {
  const personId =
    authStore.personId

  if (
    !personId &&
    authStore.isAdmin
  ) {
    const response =
      await subjectsApi.getAll()

    return listFromResponse(
      response
    )
  }

  if (!personId) {
    return []
  }

  const membershipsResponse =
    await membershipsApi
      .getSubjectMemberships({
        personId,
        activeOnly: true,
      })

  const memberships =
    listFromResponse(
      membershipsResponse
    )

  const subjectIds =
    uniqueNumbers(
      memberships
        .filter(
          (item) =>
            Number(item.role) ===
            SUBJECT_ROLE_TEACHER
        )
        .map(
          (item) =>
            item.subjectId
        )
    )

  const responses =
    await Promise.all(
      subjectIds.map(
        (subjectId) =>
          subjectsApi.getById(
            subjectId
          )
      )
    )

  return responses
    .map(
      (response) =>
        response.data
    )
    .filter(Boolean)
}

async function loadStudentSubjects() {
  const personId =
    authStore.personId

  if (!personId) {
    return []
  }

  const membershipsResponse =
    await membershipsApi
      .getGroupMemberships({
        personId,
        activeOnly: true,
      })

  const memberships =
    listFromResponse(
      membershipsResponse
    )

  const groupMembership =
    memberships.find(
      (item) =>
        Number(item.role) ===
        GROUP_ROLE_STUDENT
    )

  if (!groupMembership) {
    group.value = null
    faculty.value = null

    return []
  }

  const groupResponse =
    await groupsApi.getById(
      groupMembership.groupId
    )

  group.value =
    groupResponse.data ?? null

  if (group.value?.facultyId) {
    const facultyResponse =
      await facultiesApi.getById(
        group.value.facultyId
      )

    faculty.value =
      facultyResponse.data ?? null
  } else {
    faculty.value = null
  }

  const [
    enrollmentsResponse,
    groupAssignmentsResponse,
  ] = await Promise.all([
    teachingApi.getEnrollments({
      groupMembershipId:
        groupMembership.id,
    }),

    teachingApi.getAssignments({
      groupId: group.value?.id,
    }),
  ])

  const enrollments =
    listFromResponse(
      enrollmentsResponse
    )

  const groupAssignments =
    listFromResponse(
      groupAssignmentsResponse
    )

  const enrolledAssignmentIds =
    uniqueNumbers(
      enrollments.map(
        (item) =>
          item.teachingAssignmentId
      )
    )

  const enrolledResponses =
    await Promise.all(
      enrolledAssignmentIds.map(
        (assignmentId) =>
          teachingApi.getAssignment(
            assignmentId
          )
      )
    )

  const assignmentsMap =
    new Map()

  ;[
    ...groupAssignments,

    ...enrolledResponses
      .map(
        (response) =>
          response.data
      )
      .filter(Boolean),
  ].forEach((assignment) => {
    if (
      assignment?.id !==
        null &&
      assignment?.id !==
        undefined
    ) {
      assignmentsMap.set(
        assignment.id,
        assignment
      )
    }
  })

  const assignmentMembershipIds =
    uniqueNumbers(
      [...assignmentsMap.values()]
        .map(
          (assignment) =>
            assignment
              .subjectMembershipId
        )
    )

  const subjectMembershipResponses =
    await Promise.all(
      assignmentMembershipIds.map(
        (membershipId) =>
          membershipsApi
            .getSubjectMembership(
              membershipId
            )
      )
    )

  const subjectIds =
    uniqueNumbers(
      subjectMembershipResponses
        .map(
          (response) =>
            response.data
        )
        .filter(Boolean)
        .map(
          (membership) =>
            membership.subjectId
        )
    )

  const subjectResponses =
    await Promise.all(
      subjectIds.map(
        (subjectId) =>
          subjectsApi.getById(
            subjectId
          )
      )
    )

  return subjectResponses
    .map(
      (response) =>
        response.data
    )
    .filter(Boolean)
}

async function loadSubjects() {
  loading.value = true
  error.value = ''

  try {
    /*
     * Старый frontend отдавал приоритет
     * STUDENT-контексту, если у пользователя
     * одновременно несколько ролей.
     */
    if (authStore.isStudent) {
      subjects.value =
        await loadStudentSubjects()
    } else if (
      authStore.isTeacher ||
      authStore.isAdmin
    ) {
      group.value = null
      faculty.value = null

      subjects.value =
        await loadTeacherSubjects()
    } else {
      subjects.value = []
    }

    subjects.value =
      [...subjects.value].sort(
        (left, right) =>
          String(
            left.name ?? ''
          ).localeCompare(
            String(
              right.name ?? ''
            ),
            'ru',
            {
              sensitivity: 'base',
            }
          )
      )
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить список предметов.'
      )
  } finally {
    loading.value = false
  }
}

onMounted(loadSubjects)
</script>

<template>
  <SubjectsPageShell
    title="Мои предметы"
    subtitle="Для студентов отображаются предметы по текущей группе, для преподавателей и администраторов — доступные дисциплины."
  >
    <template #actions>
      <UiButton
        :loading="loading"
        loading-text="Обновление..."
        @click="loadSubjects"
      >
        Обновить
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiAlert
      variant="info"
      :message="metaText"
    />

    <UiCard
      title="Поиск"
      description="Фильтр применяется по названию предмета."
      compact
    >
      <UiInput
        v-model="filter"
        type="search"
        label="Поиск по предметам"
        placeholder="Введите название предмета"
      />
    </UiCard>

    <UiCard
      title="Предметы"
      :description="
        filteredSubjects.length
          ? `Найдено предметов: ${filteredSubjects.length}`
          : 'Список доступных предметов'
      "
    >
      <UiTable
        :columns="columns"
        :rows="filteredSubjects"
        :loading="loading"
        loading-message="Загрузка предметов..."
        empty-message="Подходящие предметы не найдены."
        :default-sort="{
          key: 'name',
          direction: 'asc',
        }"
      >
        <template #cell-name="{ row }">
          <strong>
            {{
              row.name ||
              `Предмет #${row.id}`
            }}
          </strong>
        </template>

        <template #cell-actions="{ row }">
          <UiButton
            size="sm"
            variant="primary"
            :to="subjectRoute(row)"
          >
            Открыть
          </UiButton>
        </template>
      </UiTable>
    </UiCard>
  </SubjectsPageShell>
</template>
