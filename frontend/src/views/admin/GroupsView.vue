<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTable from '@/components/admin/AdminTable.vue'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const faculties = ref([])
const groups = ref([])

const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  code: '',
  facultyId: '',
})

const notice = ref({
  type: 'info',
  message: '',
})

const canSubmit = computed(() => {
  return (
    form.name.trim() &&
    form.code.trim() &&
    Number(form.facultyId) > 0 &&
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

function facultyName(facultyId) {
  const faculty = faculties.value.find(
    (item) =>
      Number(item.id) ===
      Number(facultyId)
  )

  return (
    faculty?.name ??
    `Факультет #${facultyId ?? '?'}`
  )
}

const groupColumns = [
  {
    key: 'name',
    label: 'Название',
  },
  {
    key: 'code',
    label: 'Код',
  },
  {
    key: 'faculty',
    label: 'Факультет',
    value: (row) =>
      facultyName(row.facultyId),
    sortValue: (row) =>
      facultyName(row.facultyId),
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

function clearForm() {
  editingId.value = null
  form.name = ''
  form.code = ''
  form.facultyId = ''
}

function editGroup(group) {
  editingId.value = group.id
  form.name = group.name ?? ''
  form.code = group.code ?? ''
  form.facultyId = String(
    group.facultyId ?? ''
  )

  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  })
}

async function loadData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      groupsResponse,
    ] = await Promise.all([
      facultiesApi.getAll(),
      groupsApi.getAll(),
    ])

    faculties.value =
      listFromResponse(
        facultiesResponse
      )

    groups.value =
      listFromResponse(
        groupsResponse
      ).sort(
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
        'Не удалось загрузить группы'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveGroup() {
  if (!canSubmit.value) {
    return
  }

  saving.value = true

  const payload = {
    name: form.name.trim(),
    code: form.code.trim(),
    facultyId: Number(
      form.facultyId
    ),
  }

  try {
    if (editingId.value) {
      await groupsApi.update(
        editingId.value,
        payload
      )

      showNotice(
        'success',
        'Группа обновлена.'
      )
    } else {
      await groupsApi.create(
        payload
      )

      showNotice(
        'success',
        'Группа создана.'
      )
    }

    clearForm()
    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось сохранить группу'
      )
    )
  } finally {
    saving.value = false
  }
}

async function deleteGroup(group) {
  if (
    !window.confirm(
      `Удалить группу «${group.name}»?`
    )
  ) {
    return
  }

  try {
    await groupsApi.remove(
      group.id
    )

    if (
      editingId.value === group.id
    ) {
      clearForm()
    }

    showNotice(
      'success',
      'Группа удалена.'
    )

    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось удалить группу'
      )
    )
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Группы"
    description="Учебные группы и их привязка к факультетам."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>
            {{
              editingId
                ? 'Редактирование группы'
                : 'Новая группа'
            }}
          </h2>
        </div>
      </div>

      <form
        class="admin-form-grid--3 admin-form-grid"
        @submit.prevent="saveGroup"
      >
        <label class="admin-field">
          <span>Название *</span>

          <input
            v-model="form.name"
            class="admin-input"
            required
          >
        </label>

        <label class="admin-field">
          <span>Код *</span>

          <input
            v-model="form.code"
            class="admin-input"
            required
          >
        </label>

        <label class="admin-field">
          <span>Факультет *</span>

          <select
            v-model="form.facultyId"
            class="admin-select"
            required
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
          <h2>Список групп</h2>
          <p>
            Всего: {{ groups.length }}
          </p>
        </div>

        <button
          class="admin-btn"
          type="button"
          :disabled="loading"
          @click="loadData"
        >
          Обновить
        </button>
      </div>

      <AdminTable
        :columns="groupColumns"
        :rows="groups"
        :loading="loading"
        loading-message="Загрузка групп..."
        empty-message="Группы ещё не созданы."
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

        <template #cell-actions="{ row }">
          <div class="admin-table__actions">
            <button
              class="admin-btn admin-btn--small"
              type="button"
              @click="editGroup(row)"
            >
              Изменить
            </button>

            <button
              class="admin-btn admin-btn--danger admin-btn--small"
              type="button"
              @click="deleteGroup(row)"
            >
              Удалить
            </button>
          </div>
        </template>
      </AdminTable>
    </section>
  </AdminPageShell>
</template>
