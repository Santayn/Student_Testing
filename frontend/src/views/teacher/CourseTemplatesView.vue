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

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  buildCourseTemplateListParams,
  canCreateCourseTemplate,
} from '@/utils/courseTemplateContext'

const route = useRoute()
const authStore = useAuthStore()

const {
  loadingSubjects,
  selectedMembershipId,
  selectedSubjectId,
  selectedMembership,
  membershipOptions,
  ensureSelectedMembershipActive,
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

const templatesRequest = createLatestRequestGuard()
const versionsRequest = createLatestRequestGuard()

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

const templateCreationAllowed = computed(() => {
  return canCreateCourseTemplate({
    isAdmin: authStore.isAdminMode,
  })
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
  const requestId =
    templatesRequest.begin()

  versionsRequest.invalidate()
  loadingVersions.value = false

  templates.value = []
  versions.value = []
  selectedTemplateId.value = null
  resetTemplateForm()
  resetVersionForm()

  const subjectId = Number(
    selectedSubjectId.value || 0
  )

  if (!subjectId) {
    loading.value = false
    return
  }

  loading.value = true

  try {
    const params =
      buildCourseTemplateListParams({
        subjectId,
        isAdmin: authStore.isAdminMode,
        currentPersonId:
          authStore.personId,
        selectedMembership:
          selectedMembership.value,
      })

    if (!params) {
      if (
        templatesRequest.isCurrent(
          requestId
        )
      ) {
        notice.value = {
          type: 'info',
          message:
            'Не удалось определить преподавателя для выбранного предмета.',
        }
      }
      return
    }

    const response =
      await coursesApi.getTemplates(params)

    if (
      !templatesRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

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
    if (
      !templatesRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить шаблоны курса'
      ),
    }
  } finally {
    if (
      templatesRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
  }
}

async function selectTemplate(templateId) {
  selectedTemplateId.value =
    Number(templateId)

  resetVersionForm()
  await loadVersions()
}

async function loadVersions() {
  const requestId =
    versionsRequest.begin()

  versions.value = []

  const templateId = Number(
    selectedTemplateId.value || 0
  )

  if (!templateId) {
    loadingVersions.value = false
    resetVersionForm()
    return
  }

  loadingVersions.value = true

  try {
    const response =
      await coursesApi.getVersions(
        templateId
      )

    if (
      !versionsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

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
    if (
      !versionsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    notice.value = {
      type: 'danger',
      message: getApiErrorMessage(
        error,
        'Не удалось загрузить версии курса'
      ),
    }
  } finally {
    if (
      versionsRequest.isCurrent(
        requestId
      )
    ) {
      loadingVersions.value = false
    }
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

  if (
    !templateForm.value.id &&
    !templateCreationAllowed.value
  ) {
    notice.value = {
      type: 'info',
      message:
        'Администратор может редактировать существующие шаблоны выбранного преподавателя, но новый шаблон должен создать сам преподаватель.',
    }
    return
  }

  const payload = {
    subjectId:
      Number(selectedSubjectId.value),
    name:
      templateForm.value.name.trim(),
    publicVisible:
      templateForm.value.publicVisible,
  }

  savingTemplate.value = true

  try {
    await ensureSelectedMembershipActive()

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
    await ensureSelectedMembershipActive()

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
    changeNotes:
      versionForm.value.changeNotes.trim() ||
      null,
  }

  savingVersion.value = true

  try {
    await ensureSelectedMembershipActive()

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
    await ensureSelectedMembershipActive()

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
        version.id
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
              : templateCreationAllowed
                ? 'Новый шаблон'
                : 'Шаблоны преподавателя'
          "
        >
          <div class="teacher-stack">
            <UiAlert
              v-if="authStore.isAdminMode && !templateForm.id"
              variant="info"
              message="Администратор видит шаблоны выбранного преподавателя и может редактировать существующие. Создание нового шаблона от имени преподавателя backend не поддерживает."
            />

            <UiInput
              v-model="templateForm.name"
              label="Название"
              placeholder="Например: Базовый поток"
              maxlength="200"
              :disabled="
                !selectedSubjectId ||
                (!templateCreationAllowed && !templateForm.id)
              "
              required
            />

            <UiCheckbox
              v-model="templateForm.publicVisible"
              label="Публиковать шаблон"
              :disabled="
                !selectedSubjectId ||
                (!templateCreationAllowed && !templateForm.id)
              "
            />

            <div class="teacher-actions teacher-actions--mobile-stack">
              <UiButton
                v-if="templateCreationAllowed || templateForm.id"
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
