<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTable from '@/components/admin/AdminTable.vue'

import {
  facultiesApi,
  getApiErrorMessage,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const faculties = ref([])
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  code: '',
  description: '',
})

const notice = ref({
  type: 'info',
  message: '',
})

const facultyColumns = [
  {
    key: 'name',
    label: 'Название',
  },
  {
    key: 'code',
    label: 'Код',
  },
  {
    key: 'description',
    label: 'Описание',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

const formTitle = computed(() => {
  return editingId.value
    ? 'Редактирование факультета'
    : 'Новый факультет'
})

const canSubmit = computed(() => {
  return (
    form.name.trim() &&
    form.code.trim() &&
    !saving.value
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

function clearForm() {
  editingId.value = null
  form.name = ''
  form.code = ''
  form.description = ''
}

function editFaculty(faculty) {
  editingId.value = faculty.id
  form.name = faculty.name ?? ''
  form.code = faculty.code ?? ''
  form.description =
    faculty.description ?? ''

  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  })
}

async function loadFaculties() {
  loading.value = true

  try {
    const response =
      await facultiesApi.getAll()

    faculties.value =
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
        'Не удалось загрузить факультеты'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveFaculty() {
  if (!canSubmit.value) {
    return
  }

  saving.value = true
  clearNotice()

  const payload = {
    name: form.name.trim(),
    code: form.code.trim(),
    description:
      form.description.trim() || null,
  }

  try {
    if (editingId.value) {
      await facultiesApi.update(
        editingId.value,
        payload
      )

      showNotice(
        'success',
        'Факультет обновлён.'
      )
    } else {
      await facultiesApi.create(
        payload
      )

      showNotice(
        'success',
        'Факультет создан.'
      )
    }

    clearForm()
    await loadFaculties()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось сохранить факультет'
      )
    )
  } finally {
    saving.value = false
  }
}

async function deleteFaculty(faculty) {
  if (
    !window.confirm(
      `Удалить факультет «${faculty.name}»?`
    )
  ) {
    return
  }

  try {
    await facultiesApi.remove(
      faculty.id
    )

    if (
      editingId.value === faculty.id
    ) {
      clearForm()
    }

    showNotice(
      'success',
      'Факультет удалён.'
    )

    await loadFaculties()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось удалить факультет'
      )
    )
  }
}

onMounted(loadFaculties)
</script>

<template>
  <AdminPageShell
    title="Факультеты"
    description="Создание, редактирование и удаление факультетов."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>{{ formTitle }}</h2>
          <p>
            Код должен однозначно определять факультет.
          </p>
        </div>
      </div>

      <form
        class="admin-form-grid"
        @submit.prevent="saveFaculty"
      >
        <label class="admin-field">
          <span>Название *</span>

          <input
            v-model="form.name"
            class="admin-input"
            type="text"
            required
          >
        </label>

        <label class="admin-field">
          <span>Код *</span>

          <input
            v-model="form.code"
            class="admin-input"
            type="text"
            required
          >
        </label>

        <label class="admin-field admin-field--wide">
          <span>Описание</span>

          <textarea
            v-model="form.description"
            class="admin-textarea"
          />
        </label>

        <div class="admin-actions admin-field--wide">
          <button
            class="admin-btn admin-btn--primary"
            type="submit"
            :disabled="!canSubmit"
          >
            {{
              saving
                ? 'Сохранение...'
                : editingId
                  ? 'Сохранить'
                  : 'Создать'
            }}
          </button>

          <button
            v-if="editingId"
            class="admin-btn"
            type="button"
            @click="clearForm"
          >
            Отмена
          </button>
        </div>
      </form>
    </section>

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>Список факультетов</h2>
          <p>
            Всего: {{ faculties.length }}
          </p>
        </div>

        <button
          class="admin-btn"
          type="button"
          :disabled="loading"
          @click="loadFaculties"
        >
          Обновить
        </button>
      </div>

      <AdminTable
        :columns="facultyColumns"
        :rows="faculties"
        :loading="loading"
        loading-message="Загрузка факультетов..."
        empty-message="Факультеты ещё не созданы."
        :default-sort="{
          key: 'name',
          direction: 'asc',
        }"
      >
        <template #cell-name="{ row }">
          <strong>
            {{ row.name }}
          </strong>
        </template>

        <template #cell-description="{ row }">
          {{ row.description ?? '—' }}
        </template>

        <template #cell-actions="{ row }">
          <div class="admin-table__actions">
            <button
              class="admin-btn admin-btn--small"
              type="button"
              @click="editFaculty(row)"
            >
              Изменить
            </button>

            <button
              class="admin-btn admin-btn--danger admin-btn--small"
              type="button"
              @click="deleteFaculty(row)"
            >
              Удалить
            </button>
          </div>
        </template>
      </AdminTable>
    </section>
  </AdminPageShell>
</template>
