<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  facultiesApi,
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

const faculties = ref([])
const subjects = ref([])
const assignedSubjects = ref([])

const facultyId = ref('')
const addSelection = ref([])
const removeSelection = ref([])

const loading = ref(false)
const saving = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const assignedIds = computed(() => {
  return new Set(
    assignedSubjects.value.map(
      (item) => Number(item.id)
    )
  )
})

const availableSubjects = computed(() => {
  return subjects.value.filter(
    (subject) =>
      !assignedIds.value.has(
        Number(subject.id)
      )
  )
})

const selectedFaculty = computed(() => {
  return faculties.value.find(
    (item) =>
      Number(item.id) ===
      Number(facultyId.value)
  )
})

function showNotice(type, message) {
  notice.value = {
    type,
    message,
  }
}

function clearNotice() {
  notice.value.message = ''
}

async function loadBaseData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      subjectsResponse,
    ] = await Promise.all([
      facultiesApi.getAll(),
      subjectsApi.getAll(),
    ])

    faculties.value =
      listFromResponse(
        facultiesResponse
      )

    subjects.value =
      listFromResponse(
        subjectsResponse
      ).sort(
        (a, b) =>
          String(
            a.name ?? ''
          ).localeCompare(
            String(b.name ?? ''),
            'ru'
          )
      )

    if (
      !facultyId.value &&
      faculties.value.length
    ) {
      facultyId.value = String(
        faculties.value[0].id
      )
    }
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить справочники'
      )
    )
  } finally {
    loading.value = false
  }
}

async function loadAssignedSubjects() {
  addSelection.value = []
  removeSelection.value = []

  if (!facultyId.value) {
    assignedSubjects.value = []
    return
  }

  loading.value = true

  try {
    const response =
      await facultiesApi.getSubjects(
        Number(facultyId.value)
      )

    assignedSubjects.value =
      listFromResponse(response).sort(
        (a, b) =>
          String(
            a.name ?? ''
          ).localeCompare(
            String(b.name ?? ''),
            'ru'
          )
      )
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить предметы факультета'
      )
    )
  } finally {
    loading.value = false
  }
}

async function addSubjects() {
  const ids = uniqueNumbers(
    addSelection.value
  )

  if (
    !facultyId.value ||
    !ids.length
  ) {
    return
  }

  saving.value = true

  try {
    await Promise.all(
      ids.map(
        (subjectId) =>
          facultiesApi.addSubject(
            Number(facultyId.value),
            subjectId
          )
      )
    )

    showNotice(
      'success',
      `Добавлено предметов: ${ids.length}.`
    )

    await loadAssignedSubjects()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось добавить предметы'
      )
    )
  } finally {
    saving.value = false
  }
}

async function removeSubjects() {
  const ids = uniqueNumbers(
    removeSelection.value
  )

  if (
    !facultyId.value ||
    !ids.length
  ) {
    return
  }

  saving.value = true

  try {
    await Promise.all(
      ids.map(
        (subjectId) =>
          facultiesApi.removeSubject(
            Number(facultyId.value),
            subjectId
          )
      )
    )

    showNotice(
      'success',
      `Удалено предметов: ${ids.length}.`
    )

    await loadAssignedSubjects()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось удалить предметы'
      )
    )
  } finally {
    saving.value = false
  }
}

watch(
  facultyId,
  loadAssignedSubjects
)

onMounted(loadBaseData)
</script>

<template>
  <AdminPageShell
    title="Предметы факультетов"
    description="Настройка списка предметов, доступных конкретному факультету."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-card">
      <label class="admin-field">
        <span>Факультет</span>

        <select
          v-model="facultyId"
          class="admin-select"
          :disabled="loading"
        >
          <option value="">
            Выберите факультет
          </option>

          <option
            v-for="faculty in faculties"
            :key="faculty.id"
            :value="String(faculty.id)"
          >
            {{ faculty.name }}
          </option>
        </select>
      </label>

      <div
        v-if="selectedFaculty"
        class="admin-chip-list"
        style="margin-top: 12px;"
      >
        <span class="admin-chip">
          {{
            selectedFaculty.code ??
            'Без кода'
          }}
        </span>

        <span class="admin-chip">
          Назначено:
          {{ assignedSubjects.length }}
        </span>
      </div>
    </section>

    <section
      v-if="facultyId"
      class="admin-grid admin-grid--2"
    >
      <div class="admin-card">
        <div class="admin-card__header">
          <div>
            <h2>Доступные предметы</h2>
            <p>
              Не назначены выбранному факультету.
            </p>
          </div>
        </div>

        <div
          v-if="!availableSubjects.length"
          class="admin-empty"
        >
          Нет доступных предметов.
        </div>

        <div
          v-else
          class="admin-checkbox-list"
        >
          <label
            v-for="subject in availableSubjects"
            :key="subject.id"
            class="admin-checkbox"
          >
            <input
              v-model="addSelection"
              type="checkbox"
              :value="subject.id"
            >

            <span class="admin-checkbox__copy">
              <span class="admin-checkbox__title">
                {{ subject.name }}
              </span>

              <span class="admin-checkbox__meta">
                {{
                  subject.description ??
                  `Предмет #${subject.id}`
                }}
              </span>
            </span>
          </label>
        </div>

        <div
          class="admin-actions admin-actions--mobile-stack"
          style="margin-top: 14px;"
        >
          <button
            class="admin-btn admin-btn--primary"
            type="button"
            :disabled="
              saving ||
              !addSelection.length
            "
            @click="addSubjects"
          >
            Добавить выбранные
          </button>
        </div>
      </div>

      <div class="admin-card">
        <div class="admin-card__header">
          <div>
            <h2>Назначенные предметы</h2>
            <p>
              Уже доступны факультету.
            </p>
          </div>
        </div>

        <div
          v-if="!assignedSubjects.length"
          class="admin-empty"
        >
          Предметы ещё не назначены.
        </div>

        <div
          v-else
          class="admin-checkbox-list"
        >
          <label
            v-for="subject in assignedSubjects"
            :key="subject.id"
            class="admin-checkbox"
          >
            <input
              v-model="removeSelection"
              type="checkbox"
              :value="subject.id"
            >

            <span class="admin-checkbox__copy">
              <span class="admin-checkbox__title">
                {{ subject.name }}
              </span>

              <span class="admin-checkbox__meta">
                {{
                  subject.description ??
                  `Предмет #${subject.id}`
                }}
              </span>
            </span>
          </label>
        </div>

        <div
          class="admin-actions admin-actions--mobile-stack"
          style="margin-top: 14px;"
        >
          <button
            class="admin-btn admin-btn--danger"
            type="button"
            :disabled="
              saving ||
              !removeSelection.length
            "
            @click="removeSubjects"
          >
            Удалить выбранные
          </button>
        </div>
      </div>
    </section>
  </AdminPageShell>
</template>
