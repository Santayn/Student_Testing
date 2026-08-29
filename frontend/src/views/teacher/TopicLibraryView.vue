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
  selectedMembershipId,
  selectedMembership,
  selectedSubjectId,
  selectedSubject,
  membershipOptions,
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
    label: 'Тема',
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
  if (!selectedMembership.value) {
    return 'Выберите предмет преподавателя. Темы группируют вопросы банка и используются в правилах формирования тестов.'
  }

  if (!selectedSubject.value) {
    return `Выбрано назначение #${selectedMembership.value.id}.`
  }

  if (!topics.value.length) {
    return `У предмета «${selectedSubject.value.name}» в выбранном назначении пока нет тем.`
  }

  return `У предмета «${selectedSubject.value.name}» в выбранном назначении тем: ${topics.value.length}.`
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
      let topic = topics.value.find(
        (item) =>
          String(item.id) ===
          String(topicId)
      )

      if (!topic) {
        try {
          const topicResponse =
            await topicsApi.getOne(
              topicId
            )

          const candidate =
            topicResponse.data

          if (
            candidate &&
            String(
              candidate
                .subjectMembershipId
            ) ===
              String(
                selectedMembership
                  .value.id
              )
          ) {
            topic = candidate
          }
        } catch {
          /*
           * GET /topics/{id} возвращает 400, если темы нет.
           * Для страницы достаточно оставить форму новой темы.
           */
        }
      }

      if (topic) {
        editTopic(topic)
      } else {
        notice.value = {
          type: 'warning',
          message:
            'Тема из ссылки не относится к выбранному назначению преподавателя или больше не существует.',
        }
      }
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить темы'
      ),
    }
  } finally {
    loading.value = false
  }
}


function topicSaveErrorMessage(error) {
  const status =
    error?.response?.status

  const backendMessage =
    String(
      error?.response?.data
        ?.message ?? ''
    )

  if (
    status === 409 &&
    backendMessage
      .toLowerCase()
      .includes('ordinal')
  ) {
    return (
      'Тема с таким порядковым номером уже существует ' +
      'в выбранном назначении преподавателя.'
    )
  }

  return getApiErrorMessage(
    error,
    'Не удалось сохранить тему'
  )
}

function topicDeleteErrorMessage(error) {
  if (
    error?.response?.status === 409
  ) {
    return (
      'Тему не удалось удалить из-за конфликта связанных данных. ' +
      'Проверьте, используется ли она в вопросах или правилах формирования тестов.'
    )
  }

  return getApiErrorMessage(
    error,
    'Не удалось удалить тему'
  )
}

async function saveTopic() {
  const membership =
    selectedMembership.value

  const ordinal =
    Number(form.value.ordinal)

  const name =
    form.value.name.trim()

  const description =
    form.value.description.trim()

  if (!membership) {
    notice.value = {
      type: 'danger',
      message:
        'Выберите предмет преподавателя.',
    }
    return
  }

  if (
    !Number.isInteger(ordinal) ||
    ordinal <= 0
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Порядковый номер темы должен быть целым числом больше нуля.',
    }
    return
  }

  if (!name) {
    notice.value = {
      type: 'danger',
      message:
        'Введите название темы.',
    }
    return
  }

  if (name.length > 200) {
    notice.value = {
      type: 'danger',
      message:
        'Название темы не может быть длиннее 200 символов.',
    }
    return
  }

  if (description.length > 2000) {
    notice.value = {
      type: 'danger',
      message:
        'Описание темы не может быть длиннее 2000 символов.',
    }
    return
  }

  const duplicateOrdinal =
    topics.value.find(
      (topic) =>
        Number(topic.ordinal) ===
          ordinal &&
        String(topic.id) !==
          String(
            form.value.id ?? ''
          )
    )

  if (duplicateOrdinal) {
    notice.value = {
      type: 'danger',
      message:
        'Тема с таким порядковым номером уже существует в выбранном назначении преподавателя.',
    }
    return
  }

  const payload = {
    /*
     * subjectId и subjectMembershipId всегда берутся
     * из одного membership-контекста.
     */
    subjectId:
      Number(
        membership.subjectId
      ),

    courseLectureId: null,

    subjectMembershipId:
      Number(membership.id),

    ordinal,
    name,

    description:
      description || null,
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
        message: 'Тема обновлена.',
      }
    } else {
      await topicsApi.create(payload)

      notice.value = {
        type: 'success',
        message: 'Тема создана.',
      }
    }

    await loadTopics()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message:
        topicSaveErrorMessage(
          error
        ),
    }
  } finally {
    saving.value = false
  }
}

async function deleteTopic(topic) {
  if (
    !window.confirm(
      `Удалить тему «${topic.name}»?`
    )
  ) {
    return
  }

  deletingId.value = topic.id

  try {
    await topicsApi.remove(topic.id)

    notice.value = {
      type: 'success',
      message: 'Тема удалена.',
    }

    await loadTopics()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message:
        topicDeleteErrorMessage(
          error
        ),
    }
  } finally {
    deletingId.value = null
  }
}

watch(
  selectedMembershipId,
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

    if (selectedMembershipId.value) {
      await loadTopics()
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет активных назначений преподавателя на предметы для управления темами.',
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
    title="Темы предмета"
    subtitle="Темы группируют вопросы банка и используются в правилах формирования тестов по разделам предмета."
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
              v-model="selectedMembershipId"
              label="Предмет преподавателя"
              :options="membershipOptions"
              placeholder="Выберите предмет"
              :disabled="
                loadingSubjects ||
                !membershipOptions.length
              "
            />

            <UiAlert
              variant="info"
              message="Тема — это раздел предмета для группировки вопросов банка. При создании теста тема может использоваться как правило, из какого раздела и сколько вопросов выбрать."
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
              ? 'Редактирование темы'
              : 'Новая тема'
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
                label="Название темы"
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
                Сохранить тему
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
        title="Темы предмета"
        :description="
          selectedSubject && selectedMembership
            ? `Предмет: ${selectedSubject.name}. Всего тем: ${topics.length}.`
            : 'Предмет не выбран.'
        "
      >
        <UiTable
          :columns="topicColumns"
          :rows="topics"
          :loading="loading"
          loading-message="Загрузка тем..."
          empty-message="Для выбранного назначения преподавателя пока нет тем."
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
