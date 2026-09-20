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

import {
  loadStudentLearningContext,
} from '@/utils/studentLearningContext'

const SUBJECT_ROLE_TEACHER = 1
const authStore =
  useAuthStore()

const loading = ref(false)
const error = ref('')

const subjects = ref([])
const groups = ref([])
const faculties = ref([])

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
  if (authStore.isAdminMode) {
    return 'Показаны все предметы, доступные администратору.'
  }

  if (authStore.isStudentMode) {
    if (!groups.value.length) {
      return (
        'Для текущего пользователя ' +
        'не найдена активная учебная группа.'
      )
    }

    const groupNames = groups.value
      .map(
        (item) =>
          item.name ||
          item.code ||
          `#${item.id}`
      )
      .join(', ')

    const facultyNames = faculties.value.length
      ? faculties.value
          .map(
            (item) =>
              item.name ||
              item.code ||
              `#${item.id}`
          )
          .join(', ')
      : '-'

    return (
      `Активные группы: ${groupNames}. ` +
      `Факультеты: ${facultyNames}.`
    )
  }

  if (authStore.isTeacherMode) {
    return 'Показаны предметы текущего преподавателя.'
  }

  return (
    'Для вашей роли доступных ' +
    'предметов не найдено.'
  )
})

function subjectRoute(subject) {
  const query = {}

  if (faculties.value.length === 1) {
    query.facultyId =
      faculties.value[0].id
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

  if (authStore.isAdminMode) {
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
  const context =
    await loadStudentLearningContext({
      personId: authStore.personId,
      membershipsApi,
      groupsApi,
      facultiesApi,
      teachingApi,
      subjectsApi,
    })

  groups.value = context.groups
  faculties.value = context.faculties

  return context.subjects
}

async function loadSubjects() {
  loading.value = true
  error.value = ''

  try {
    /*
     * ADMIN — отдельный глобальный контекст.
     * Даже если учётная запись дополнительно имеет STUDENT/TEACHER,
     * административная страница предметов должна показывать весь список.
     */
    if (authStore.isAdminMode) {
      groups.value = []
      faculties.value = []

      subjects.value =
        await loadTeacherSubjects()
    } else if (authStore.isStudentMode) {
      subjects.value =
        await loadStudentSubjects()
    } else if (authStore.isTeacherMode) {
      groups.value = []
      faculties.value = []

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
    subtitle="Для студентов отображаются предметы по активным учебным группам, для преподавателей и администраторов — доступные дисциплины."
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
