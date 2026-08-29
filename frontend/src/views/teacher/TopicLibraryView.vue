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
  topicsApi,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
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

const topics = ref([])
const loading = ref(false)
const saving = ref(false)
const deletingId = ref(null)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const form = ref({
  id: null,
  ordinal: 1,
  name: '',
  description: '',
})

const topicColumns = [
  {
    key: 'ordinal',
    label: 'Порядок',
  },
  {
    key: 'name',
    label: 'Тематика',
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

const contextHint = computed(() => {
  if (!selectedSubject.value) {
    return 'Выберите предмет преподавателя. Тематики используются при создании вопросов и тестов.'
  }

  if (!topics.value.length) {
    return `У предмета «${selectedSubject.value.name}» пока нет тематик.`
  }

  return `У предмета «${selectedSubject.value.name}» тематик: ${topics.value.length}.`
})

function routeQuery(topicId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId =
      selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId =
      selectedMembership.value.id
  }

  if (topicId) {
    query.topicId = topicId
  }

  return query
}

function nextOrdinal() {
  return topics.value.reduce(
    (max, topic) =>
      Math.max(
        max,
        Number(topic.ordinal ?? 0)
      ),
    0
  ) + 1
}

function resetForm() {
  form.value = {
    id: null,
    ordinal: nextOrdinal(),
    name: '',
    description: '',
  }
}

function editTopic(topic) {
  form.value = {
    id: topic.id,
    ordinal: Number(
      topic.ordinal ?? 1
    ),
    name: topic.name ?? '',
    description:
      topic.description ?? '',
  }
}

async function loadTopics() {
  topics.value = []
  resetForm()

  if (!selectedMembership.value) {
    return
  }

  loading.value = true

  try {
    const response =
      await topicsApi.getAll({
        subjectMembershipId:
          selectedMembership.value.id,
      })

    topics.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) -
            Number(right.ordinal ?? 0)
        )

    resetForm()

    const topicId =
      route.query.topicId

    if (topicId) {
      const topic = topics.value.find(
        (item) =>
          String(item.id) ===
          String(topicId)
      )

      if (topic) {
        editTopic(topic)
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить тематики'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function saveTopic() {
  if (
    !selectedSubject.value ||
    !selectedMembership.value ||
    !form.value.name.trim()
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Заполните название темы и выберите предмет.',
    }
    return
  }

  const payload = {
    subjectId:
      Number(selectedSubject.value.id),
    courseLectureId: null,
    subjectMembershipId:
      Number(selectedMembership.value.id),
    ordinal:
      Number(form.value.ordinal) || 1,
    name: form.value.name.trim(),
    description:
      form.value.description.trim() ||
      null,
  }

  saving.value = true

  try {
    if (form.value.id) {
      await topicsApi.update(
        form.value.id,
        payload
      )

      notice.value = {
        type: 'success',
        message: 'Тематика обновлена.',
      }
    } else {
      await topicsApi.create(payload)

      notice.value = {
        type: 'success',
        message: 'Тематика создана.',
      }
    }

    await loadTopics()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить тематику'
      ),
    }
  } finally {
    saving.value = false
  }
}

async function deleteTopic(topic) {
  if (
    !window.confirm(
      `Удалить тематику «${topic.name}»?`
    )
  ) {
    return
  }

  deletingId.value = topic.id

  try {
    await topicsApi.remove(topic.id)

    notice.value = {
      type: 'success',
      message: 'Тематика удалена.',
    }

    await loadTopics()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось удалить тематику'
      ),
    }
  } finally {
    deletingId.value = null
  }
}

watch(
  selectedSubjectId,
  () => {
    if (initialized.value) {
      loadTopics()
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
      await loadTopics()
    }

    if (!subjectOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет предметов преподавателя для управления тематикой.',
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
    title="Тематики предмета"
    subtitle="Создание и редактирование тематик, на которых строятся вопросы и тесты преподавателя."
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
          :description="contextHint"
        >
          <div class="teacher-stack">
            <UiSelect
              v-model="selectedSubjectId"
              label="Предмет преподавателя"
              :options="subjectOptions"
              placeholder="Выберите предмет"
              :disabled="
                loadingSubjects ||
                !subjectOptions.length
              "
            />

            <div
              v-if="selectedMembership"
              class="teacher-inline-actions teacher-inline-actions--mobile-stack"
            >
              <UiButton
                :to="{
                  name: 'teacher-questions',
                  query: routeQuery(
                    form.id
                  ),
                }"
              >
                Вопросы темы
              </UiButton>

              <UiButton
                :to="{
                  name: 'teacher-test-create',
                  query: routeQuery(
                    form.id
                  ),
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
              ? 'Редактирование тематики'
              : 'Новая тематика'
          "
        >
          <div class="teacher-stack">
            <div class="teacher-grid">
              <UiInput
                v-model="form.ordinal"
                label="Порядок"
                type="number"
                min="1"
                step="1"
                :disabled="!canEdit"
              />

              <UiInput
                v-model="form.name"
                label="Название тематики"
                maxlength="200"
                :disabled="!canEdit"
                required
              />
            </div>

            <UiTextarea
              v-model="form.description"
              label="Описание"
              maxlength="2000"
              :disabled="!canEdit"
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="saving"
                loading-text="Сохранение..."
                :disabled="!canEdit"
                @click="saveTopic"
              >
                Сохранить тематику
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

      <UiCard
        title="Тематики предмета"
        :description="
          selectedSubject
            ? `Предмет: ${selectedSubject.name}. Всего: ${topics.length}.`
            : 'Предмет не выбран.'
        "
      >
        <UiTable
          :columns="topicColumns"
          :rows="topics"
          :loading="loading"
          loading-message="Загрузка тематик..."
          empty-message="Для выбранного предмета пока нет тематик."
          :default-sort="{
            key: 'ordinal',
            direction: 'asc',
          }"
        >
          <template #cell-name="{ row }">
            <div class="teacher-stack">
              <strong>{{ row.name }}</strong>
              <span class="teacher-muted">
                {{ row.description || 'Без описания' }}
              </span>
            </div>
          </template>

          <template #cell-actions="{ row }">
            <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
              <UiButton
                size="sm"
                :to="{
                  name: 'teacher-questions',
                  query: routeQuery(row.id),
                }"
              >
                Вопросы
              </UiButton>

              <UiButton
                size="sm"
                :to="{
                  name: 'teacher-test-create',
                  query: routeQuery(row.id),
                }"
              >
                Тест
              </UiButton>

              <UiButton
                size="sm"
                @click="editTopic(row)"
              >
                Изменить
              </UiButton>

              <UiButton
                variant="danger"
                size="sm"
                :loading="deletingId === row.id"
                loading-text="Удаление..."
                @click="deleteTopic(row)"
              >
                Удалить
              </UiButton>
            </div>
          </template>
        </UiTable>
      </UiCard>
    </div>
  </TeacherPageShell>
</template>
