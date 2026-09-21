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
  UiEmptyState,
  UiSearchInput,
  UiTag,
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
    subtitle="Откройте предмет, чтобы перейти к его лекциям и учебным материалам."
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

    <section class="subjects-overview">
      <div class="subjects-overview__copy">
        <strong>{{ metaText }}</strong>
        <span>Доступно предметов: {{ subjects.length }}</span>
      </div>

      <UiSearchInput
        v-model="filter"
        class="subjects-overview__search"
        label="Поиск"
        placeholder="Название предмета"
      />
    </section>

    <UiEmptyState
      v-if="loading && !subjects.length"
      title="Загружаем предметы"
      description="Список появится после получения учебного контекста."
    />

    <UiEmptyState
      v-else-if="!error && !filteredSubjects.length"
      title="Предметы не найдены"
      :description="
        filter.trim()
          ? 'Попробуйте изменить поисковый запрос.'
          : 'Для текущего учебного контекста доступных предметов нет.'
      "
    />

    <div v-else class="subjects-grid">
      <article
        v-for="subjectItem in filteredSubjects"
        :key="subjectItem.id"
        class="subject-tile"
      >
        <div class="subject-tile__icon" aria-hidden="true">
          <i class="pi pi-book" />
        </div>

        <div class="subject-tile__body">
          <div class="subject-tile__heading">
            <h2>
              {{ subjectItem.name || `Предмет #${subjectItem.id}` }}
            </h2>
            <UiTag :value="`#${subjectItem.id}`" />
          </div>

          <p>
            Перейдите в предмет, чтобы открыть доступные лекции и продолжить обучение.
          </p>
        </div>

        <UiButton
          variant="primary"
          :to="subjectRoute(subjectItem)"
        >
          Открыть предмет
        </UiButton>
      </article>
    </div>
  </SubjectsPageShell>
</template>

<style scoped>
.subjects-overview {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  align-items: end;
  gap: 16px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
}

.subjects-overview__copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.subjects-overview__copy strong {
  color: var(--st-text);
  line-height: 1.45;
}

.subjects-overview__copy span {
  color: var(--st-text-secondary);
  font-size: 13px;
}

.subjects-overview__search {
  min-width: 0;
}

.subjects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 280px), 1fr));
  gap: 14px;
}

.subject-tile {
  min-width: 0;
  padding: 18px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.subject-tile__icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 12px;
  font-size: 18px;
}

.subject-tile__body {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.subject-tile__heading {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.subject-tile h2,
.subject-tile p {
  margin: 0;
}

.subject-tile h2 {
  min-width: 0;
  color: var(--st-text);
  font-size: 17px;
  line-height: 1.3;
  overflow-wrap: anywhere;
}

.subject-tile p {
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.subject-tile :deep(.st-ui-link-button) {
  grid-column: 1 / -1;
  justify-self: start;
}

@media (max-width: 720px) {
  .subjects-overview {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
}

@media (max-width: 480px) {
  .subject-tile {
    grid-template-columns: 1fr;
  }

  .subject-tile__icon {
    width: 40px;
    height: 40px;
  }

  .subject-tile :deep(.st-ui-link-button) {
    width: 100%;
    justify-content: center;
  }
}
</style>
