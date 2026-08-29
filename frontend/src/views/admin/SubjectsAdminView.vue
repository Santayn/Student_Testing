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
  UiButton,
  UiCard,
  UiInput,
  UiTextarea,
} from '@/components/ui'

import {
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const subjects = ref([])
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  description: '',
})

const notice = ref({
  type: 'info',
  message: '',
})

const subjectColumns = [
  {
    key: 'name',
    label: 'Название',
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

const canSubmit = computed(() => {
  return (
    form.name.trim() &&
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
  form.description = ''
}

function editSubject(subject) {
  editingId.value = subject.id
  form.name = subject.name ?? ''
  form.description =
    subject.description ?? ''

  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  })
}

async function loadSubjects() {
  loading.value = true

  try {
    const response =
      await subjectsApi.getAll()

    subjects.value =
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
        'Не удалось загрузить предметы'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveSubject() {
  if (!canSubmit.value) {
    return
  }

  saving.value = true

  const payload = {
    name: form.name.trim(),
    description:
      form.description.trim() || null,
  }

  try {
    if (editingId.value) {
      await subjectsApi.update(
        editingId.value,
        payload
      )

      showNotice(
        'success',
        'Предмет обновлён.'
      )
    } else {
      await subjectsApi.create(
        payload
      )

      showNotice(
        'success',
        'Предмет создан.'
      )
    }

    clearForm()
    await loadSubjects()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось сохранить предмет'
      )
    )
  } finally {
    saving.value = false
  }
}

async function deleteSubject(subject) {
  if (
    !window.confirm(
      `Удалить предмет «${subject.name}»?`
    )
  ) {
    return
  }

  try {
    await subjectsApi.remove(
      subject.id
    )

    if (
      editingId.value === subject.id
    ) {
      clearForm()
    }

    showNotice(
      'success',
      'Предмет удалён.'
    )

    await loadSubjects()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось удалить предмет'
      )
    )
  }
}

onMounted(loadSubjects)
</script>

<template>
  <AdminPageShell
    title="Предметы"
    description="Управление справочником учебных предметов."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard>
      <div class="admin-card__header">
        <div>
          <h2>
            {{
              editingId
                ? 'Редактирование предмета'
                : 'Новый предмет'
            }}
          </h2>
        </div>
      </div>

      <form
        class="admin-form-grid"
        @submit.prevent="saveSubject"
      >
        <label class="admin-field">
          <span>Название *</span>

          <UiInput
            v-model="form.name"
            required
          />
        </label>

        <label class="admin-field admin-field--wide">
          <span>Описание</span>

          <UiTextarea
            v-model="form.description"
          />
        </label>

        <div class="admin-actions admin-field--wide">
          <UiButton
            variant="primary"
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
          </UiButton>

          <UiButton
            v-if="editingId"
            type="button"
            @click="clearForm"
          >
            Отмена
          </UiButton>
        </div>
      </form>
    </UiCard>

    <UiCard>
      <div class="admin-card__header">
        <div>
          <h2>Список предметов</h2>
          <p>
            Всего: {{ subjects.length }}
          </p>
        </div>

        <UiButton
          type="button"
          :disabled="loading"
          @click="loadSubjects"
        >
          Обновить
        </UiButton>
      </div>

      <AdminTable
        :columns="subjectColumns"
        :rows="subjects"
        :loading="loading"
        loading-message="Загрузка предметов..."
        empty-message="Предметы ещё не созданы."
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
            <UiButton
            size="sm"
              type="button"
              @click="editSubject(row)"
            >
              Изменить
            </UiButton>

            <UiButton
            variant="danger"
            size="sm"
              type="button"
              @click="deleteSubject(row)"
            >
              Удалить
            </UiButton>
          </div>
        </template>
      </AdminTable>
    </UiCard>
  </AdminPageShell>
</template>
