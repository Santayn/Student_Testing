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
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
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
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

const route = useRoute()
const authStore = useAuthStore()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
  membershipOptions,
  loadTeacherSubjects,
} = useTeacherSubjects()

const templates = ref([])
const versions = ref([])
const selectedTemplateId = ref(null)

const loading = ref(false)
const loadingVersions = ref(false)
const savingTemplate = ref(false)
const savingVersion = ref(false)
const deletingTemplateId = ref(null)
const publishingVersionId = ref(null)
const initialized = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const templateForm = ref({
  id: null,
  name: '',
  publicVisible: true,
})

const versionForm = ref({
  id: null,
  versionNumber: 1,
  title: '',
  description: '',
  changeNotes: '',
  published: false,
})

const selectedTemplate = computed(() => {
  return templates.value.find(
    (item) =>
      Number(item.id) ===
      Number(selectedTemplateId.value)
  ) ?? null
})

const templateColumns = [
  {
    key: 'name',
    label: 'Название',
  },
  {
    key: 'publicVisible',
    label: 'Статус',
    value: (row) =>
      row.publicVisible
        ? 'Виден'
        : 'Черновик',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

const versionColumns = [
  {
    key: 'versionNumber',
    label: 'Версия',
  },
  {
    key: 'title',
    label: 'Название',
  },
  {
    key: 'description',
    label: 'Описание',
  },
  {
    key: 'published',
    label: 'Статус',
    value: (row) =>
      row.published
        ? 'Опубликована'
        : 'Черновик',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

function routeQuery(versionId = null) {
  const query = {}

  if (selectedSubjectId.value) {
    query.subjectId =
      selectedSubjectId.value
  }

  if (selectedMembership.value) {
    query.subjectMembershipId =
      selectedMembership.value.id
  }

  if (selectedTemplateId.value) {
    query.templateId =
      selectedTemplateId.value
  }

  if (versionId) {
    query.versionId = versionId
  }

  return query
}

function resetTemplateForm() {
  templateForm.value = {
    id: null,
    name: '',
    publicVisible: true,
  }
}

function resetVersionForm() {
  const maxVersion =
    versions.value.reduce(
      (max, version) =>
        Math.max(
          max,
          Number(
            version.versionNumber ?? 0
          )
        ),
      0
    )

  versionForm.value = {
    id: null,
    versionNumber: maxVersion + 1,
    title:
      selectedTemplate.value?.name ?? '',
    description: '',
    changeNotes: '',
    published: false,
  }
}

async function loadTemplates() {
  templates.value = []
  versions.value = []
  selectedTemplateId.value = null
  resetTemplateForm()
  resetVersionForm()

  if (!selectedSubjectId.value) {
    return
  }

  loading.value = true

  try {
    const response =
      await coursesApi.getTemplates({
        subjectId:
          Number(selectedSubjectId.value),
        authorPersonId:
          authStore.personId,
      })

    templates.value =
      listFromResponse(response)

    const preferredTemplateId =
      route.query.templateId

    if (
      preferredTemplateId &&
      templates.value.some(
        (item) =>
          String(item.id) ===
          String(preferredTemplateId)
      )
    ) {
      await selectTemplate(
        Number(preferredTemplateId)
      )
    } else if (
      templates.value.length === 1
    ) {
      await selectTemplate(
        templates.value[0].id
      )
    }
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить шаблоны курса'
      ),
    }
  } finally {
    loading.value = false
  }
}

async function selectTemplate(templateId) {
  selectedTemplateId.value =
    Number(templateId)

  resetVersionForm()
  await loadVersions()
}

async function loadVersions() {
  versions.value = []

  if (!selectedTemplateId.value) {
    resetVersionForm()
    return
  }

  loadingVersions.value = true

  try {
    const response =
      await coursesApi.getVersions(
        selectedTemplateId.value
      )

    versions.value =
      listFromResponse(response)
        .sort(
          (left, right) =>
            Number(
              left.versionNumber ?? 0
            ) -
            Number(
              right.versionNumber ?? 0
            )
        )

    resetVersionForm()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить версии курса'
      ),
    }
  } finally {
    loadingVersions.value = false
  }
}

function editTemplate(template) {
  templateForm.value = {
    id: template.id,
    name: template.name || '',
    publicVisible:
      Boolean(template.publicVisible),
  }
}

async function saveTemplate() {
  if (
    !selectedSubjectId.value ||
    !templateForm.value.name.trim()
  ) {
    notice.value = {
      type: 'danger',
      message:
        'Заполните название шаблона и выберите предмет.',
    }
    return
  }

  const current =
    templates.value.find(
      (item) =>
        Number(item.id) ===
        Number(templateForm.value.id)
    )

  const payload = {
    subjectId:
      Number(selectedSubjectId.value),
    authorPersonId:
      current?.authorPersonId ||
      authStore.personId,
    name:
      templateForm.value.name.trim(),
    publicVisible:
      templateForm.value.publicVisible,
  }

  savingTemplate.value = true

  try {
    if (templateForm.value.id) {
      await coursesApi.updateTemplate(
        templateForm.value.id,
        payload
      )

      notice.value = {
        type: 'success',
        message: 'Шаблон обновлён.',
      }
    } else {
      await coursesApi.createTemplate(
        payload
      )

      notice.value = {
        type: 'success',
        message: 'Шаблон создан.',
      }
    }

    await loadTemplates()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить шаблон'
      ),
    }
  } finally {
    savingTemplate.value = false
  }
}

async function deleteTemplate(template) {
  if (
    !window.confirm(
      `Удалить шаблон «${template.name}»?`
    )
  ) {
    return
  }

  deletingTemplateId.value =
    template.id

  try {
    await coursesApi.removeTemplate(
      template.id
    )

    notice.value = {
      type: 'success',
      message: 'Шаблон удалён.',
    }

    await loadTemplates()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось удалить шаблон'
      ),
    }
  } finally {
    deletingTemplateId.value = null
  }
}

function editVersion(version) {
  versionForm.value = {
    id: version.id,
    versionNumber:
      Number(version.versionNumber ?? 1),
    title: version.title || '',
    description:
      version.description || '',
    changeNotes:
      version.changeNotes || '',
    published:
      Boolean(version.published),
  }
}

async function saveVersion() {
  if (!selectedTemplateId.value) {
    notice.value = {
      type: 'danger',
      message: 'Выберите шаблон курса.',
    }
    return
  }

  if (!versionForm.value.title.trim()) {
    notice.value = {
      type: 'danger',
      message:
        'Заполните название версии.',
    }
    return
  }

  const basePayload = {
    versionNumber:
      Number(
        versionForm.value.versionNumber
      ) || 1,
    title:
      versionForm.value.title.trim(),
    description:
      versionForm.value.description.trim() ||
      null,
    createdByPersonId:
      authStore.personId,
    changeNotes:
      versionForm.value.changeNotes.trim() ||
      null,
  }

  savingVersion.value = true

  try {
    if (versionForm.value.id) {
      await coursesApi.updateVersion(
        versionForm.value.id,
        basePayload
      )

      notice.value = {
        type: 'success',
        message: 'Версия обновлена.',
      }
    } else {
      await coursesApi.createVersion(
        selectedTemplateId.value,
        {
          ...basePayload,
          published:
            versionForm.value.published,
          publishedByPersonId:
            versionForm.value.published
              ? authStore.personId
              : null,
        }
      )

      notice.value = {
        type: 'success',
        message: 'Версия создана.',
      }
    }

    await loadVersions()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось сохранить версию'
      ),
    }
  } finally {
    savingVersion.value = false
  }
}

async function publishVersion(version) {
  publishingVersionId.value =
    version.id

  try {
    if (version.published) {
      await coursesApi.unpublishVersion(
        version.id
      )

      notice.value = {
        type: 'success',
        message:
          'Публикация версии снята.',
      }
    } else {
      await coursesApi.publishVersion(
        version.id,
        {
          publishedByPersonId:
            authStore.personId,
        }
      )

      notice.value = {
        type: 'success',
        message: 'Версия опубликована.',
      }
    }

    await loadVersions()
  } catch (error) {
    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось изменить публикацию версии'
      ),
    }
  } finally {
    publishingVersionId.value = null
  }
}

watch(
  selectedMembershipId,
  () => {
    if (initialized.value) {
      loadTemplates()
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
      await loadTemplates()
    }

    if (!membershipOptions.value.length) {
      notice.value = {
        type: 'info',
        message:
          'Нет предметов преподавателя для работы с шаблонами курса.',
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
    title="Шаблоны курса"
    subtitle="Шаблоны и версии связывают предмет, лекции и тесты."
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
          description="Сначала выберите предмет, затем создайте шаблон курса и его версии."
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

            <UiButton
              :to="{
                name: 'teacher-topics',
                query: routeQuery(),
              }"
            >
              Темы предмета
            </UiButton>
          </div>
        </UiCard>

        <UiCard
          :title="
            templateForm.id
              ? 'Редактирование шаблона'
              : 'Новый шаблон'
          "
        >
          <div class="teacher-stack">
            <UiInput
              v-model="templateForm.name"
              label="Название"
              placeholder="Например: Базовый поток"
              maxlength="200"
              :disabled="!selectedSubjectId"
              required
            />

            <UiCheckbox
              v-model="templateForm.publicVisible"
              label="Публиковать шаблон"
              :disabled="!selectedSubjectId"
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="savingTemplate"
                loading-text="Сохранение..."
                :disabled="!selectedSubjectId"
                @click="saveTemplate"
              >
                Сохранить
              </UiButton>

              <UiButton
                v-if="templateForm.id"
                @click="resetTemplateForm"
              >
                Отмена
              </UiButton>
            </div>
          </div>
        </UiCard>

        <UiCard
          :title="
            versionForm.id
              ? 'Редактирование версии'
              : 'Новая версия'
          "
          :description="
            selectedTemplate
              ? `Шаблон: ${selectedTemplate.name}`
              : 'Сначала выберите шаблон курса.'
          "
        >
          <div class="teacher-stack">
            <div class="teacher-grid">
              <UiInput
                v-model="versionForm.versionNumber"
                label="Номер версии"
                type="number"
                min="1"
                step="1"
                :disabled="!selectedTemplateId"
              />

              <UiInput
                v-model="versionForm.title"
                label="Название"
                maxlength="200"
                :disabled="!selectedTemplateId"
                required
              />
            </div>

            <UiTextarea
              v-model="versionForm.description"
              label="Описание"
              maxlength="2000"
              :disabled="!selectedTemplateId"
            />

            <UiTextarea
              v-model="versionForm.changeNotes"
              label="Примечания к изменениям"
              maxlength="2000"
              :disabled="!selectedTemplateId"
            />

            <UiCheckbox
              v-if="!versionForm.id"
              v-model="versionForm.published"
              label="Опубликовать версию"
              :disabled="!selectedTemplateId"
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                variant="primary"
                :loading="savingVersion"
                loading-text="Сохранение..."
                :disabled="!selectedTemplateId"
                @click="saveVersion"
              >
                Сохранить версию
              </UiButton>

              <UiButton
                v-if="versionForm.id"
                @click="resetVersionForm"
              >
                Отмена
              </UiButton>
            </div>
          </div>
        </UiCard>
      </div>

      <div class="teacher-stack">
        <UiCard
          title="Шаблоны курса"
          :description="`Всего: ${templates.length}`"
        >
          <UiTable
            :columns="templateColumns"
            :rows="templates"
            :loading="loading"
            empty-message="У выбранного предмета пока нет шаблонов курса."
            :default-sort="{
              key: 'name',
              direction: 'asc',
            }"
          >
            <template #cell-name="{ row }">
              <div class="teacher-stack">
                <strong>{{ row.name }}</strong>
                <span class="teacher-muted">
                  ID {{ row.id }}
                </span>
              </div>
            </template>

            <template #cell-publicVisible="{ row }">
              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success':
                    row.publicVisible,
                }"
              >
                {{ row.publicVisible ? 'Виден' : 'Черновик' }}
              </span>
            </template>

            <template #cell-actions="{ row }">
              <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
                <UiButton
                  :variant="
                    Number(selectedTemplateId) ===
                    Number(row.id)
                      ? 'primary'
                      : 'secondary'
                  "
                  size="sm"
                  @click="selectTemplate(row.id)"
                >
                  Версии
                </UiButton>

                <UiButton
                  size="sm"
                  @click="editTemplate(row)"
                >
                  Изменить
                </UiButton>

                <UiButton
                  variant="danger"
                  size="sm"
                  :loading="deletingTemplateId === row.id"
                  loading-text="Удаление..."
                  @click="deleteTemplate(row)"
                >
                  Удалить
                </UiButton>
              </div>
            </template>
          </UiTable>
        </UiCard>

        <UiCard
          title="Версии выбранного шаблона"
          :description="`Всего: ${versions.length}`"
        >
          <UiTable
            :columns="versionColumns"
            :rows="versions"
            :loading="loadingVersions"
            empty-message="Версии не выбраны или пока не созданы."
            :default-sort="{
              key: 'versionNumber',
              direction: 'asc',
            }"
          >
            <template #cell-versionNumber="{ row }">
              <strong>
                v{{ row.versionNumber }}
              </strong>
            </template>

            <template #cell-description="{ row }">
              <div class="teacher-stack">
                <span>
                  {{ row.description || '—' }}
                </span>
                <span
                  v-if="row.changeNotes"
                  class="teacher-muted"
                >
                  {{ row.changeNotes }}
                </span>
              </div>
            </template>

            <template #cell-published="{ row }">
              <span
                class="teacher-status"
                :class="{
                  'teacher-status--success':
                    row.published,
                }"
              >
                {{ row.published ? 'Опубликована' : 'Черновик' }}
              </span>
            </template>

            <template #cell-actions="{ row }">
              <div class="teacher-inline-actions teacher-inline-actions--mobile-stack">
                <UiButton
                  size="sm"
                  @click="editVersion(row)"
                >
                  Изменить
                </UiButton>

                <UiButton
                  :variant="
                    row.published
                      ? 'secondary'
                      : 'primary'
                  "
                  size="sm"
                  :loading="publishingVersionId === row.id"
                  @click="publishVersion(row)"
                >
                  {{ row.published ? 'Снять публикацию' : 'Опубликовать' }}
                </UiButton>

                <UiButton
                  size="sm"
                  :to="{
                    name: 'teacher-lectures',
                    query: routeQuery(row.id),
                  }"
                >
                  Лекции
                </UiButton>
              </div>
            </template>
          </UiTable>
        </UiCard>
      </div>
    </div>
  </TeacherPageShell>
</template>
