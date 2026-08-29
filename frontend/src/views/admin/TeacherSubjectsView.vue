<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiSelect,
  UiTextarea,
} from '@/components/ui'

import {
  getApiErrorMessage,
  membershipsApi,
  subjectsApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

const TEACHER_ROLE = 1
const REMOVED_STATUS = 3

const teachers = ref([])
const subjects = ref([])
const memberships = ref([])

const teacherId = ref('')
const availableSelection = ref([])
const assignedSelection = ref([])
const notes = ref('')

const loading = ref(false)
const saving = ref(false)

const notice = ref({
  type: 'info',
  message: '',
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

function personName(person) {
  return (
    [
      person?.lastName,
      person?.firstName,
      person?.middleName,
    ]
      .filter(Boolean)
      .join(' ')
      .trim() ||
    person?.fullName ||
    `Преподаватель #${person?.id ?? '?'}`
  )
}

function subjectName(subjectId) {
  return (
    subjects.value.find(
      (item) =>
        Number(item.id) ===
        Number(subjectId)
    )?.name ??
    `Предмет #${subjectId}`
  )
}

const selectedTeacher = computed(() => {
  return teachers.value.find(
    (item) =>
      Number(item.id) ===
      Number(teacherId.value)
  )
})

const teacherMemberships = computed(() => {
  if (!teacherId.value) {
    return []
  }

  return memberships.value.filter(
    (item) =>
      Number(item.personId) ===
        Number(teacherId.value) &&
      Number(item.role) ===
        TEACHER_ROLE
  )
})

const assignedSubjectIds = computed(() => {
  return new Set(
    teacherMemberships.value.map(
      (item) =>
        Number(item.subjectId)
    )
  )
})

const freeSubjects = computed(() => {
  return subjects.value.filter(
    (subject) =>
      !assignedSubjectIds.value.has(
        Number(subject.id)
      )
  )
})

async function loadData() {
  loading.value = true

  try {
    const [
      teachersResponse,
      subjectsResponse,
      membershipsResponse,
    ] = await Promise.all([
      usersApi.getPeople({
        role: 'TEACHER',
      }),
      subjectsApi.getAll(),
      membershipsApi
        .getSubjectMemberships({
          activeOnly: true,
        }),
    ])

    teachers.value =
      listFromResponse(
        teachersResponse
      ).sort(
        (a, b) =>
          personName(a).localeCompare(
            personName(b),
            'ru'
          )
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

    memberships.value =
      listFromResponse(
        membershipsResponse
      ).filter(
        (item) =>
          Number(item.role) ===
          TEACHER_ROLE
      )

    if (
      !teacherId.value &&
      teachers.value.length
    ) {
      teacherId.value = String(
        teachers.value[0].id
      )
    }

    availableSelection.value = []
    assignedSelection.value = []
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить назначения преподавателей'
      )
    )
  } finally {
    loading.value = false
  }
}

async function addSubjects() {
  const subjectIds =
    uniqueNumbers(
      availableSelection.value
    )

  if (
    !teacherId.value ||
    !subjectIds.length
  ) {
    return
  }

  saving.value = true

  try {
    for (
      const subjectId of subjectIds
    ) {
      await membershipsApi
        .addPersonToSubject(
          subjectId,
          {
            personId: Number(
              teacherId.value
            ),
            role: TEACHER_ROLE,
            notes:
              notes.value.trim() ||
              null,
          }
        )
    }

    notes.value = ''

    showNotice(
      'success',
      'Предметы назначены преподавателю.'
    )

    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось назначить предметы'
      )
    )
  } finally {
    saving.value = false
  }
}

async function removeSubjects() {
  const ids = uniqueNumbers(
    assignedSelection.value
  )

  if (!ids.length) {
    return
  }

  saving.value = true

  try {
    for (
      const membershipId of ids
    ) {
      await membershipsApi
        .updateSubjectMembershipStatus(
          membershipId,
          {
            status:
              REMOVED_STATUS,
          }
        )
    }

    showNotice(
      'success',
      'Предметы сняты с преподавателя.'
    )

    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось снять предметы'
      )
    )
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Преподаватели и предметы"
    description="Назначение преподавателей на учебные предметы."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-summary">
      <div class="admin-stat">
        <span class="admin-stat__label">
          Преподавателей
        </span>

        <strong class="admin-stat__value">
          {{ teachers.length }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Предметов
        </span>

        <strong class="admin-stat__value">
          {{ subjects.length }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных назначений
        </span>

        <strong class="admin-stat__value">
          {{ memberships.length }}
        </strong>
      </div>
    </section>

    <UiCard>
      <label class="admin-field">
        <span>Преподаватель</span>

        <UiSelect
          v-model="teacherId"
        >
          <option value="">
            Выберите преподавателя
          </option>

          <option
            v-for="teacher in teachers"
            :key="teacher.id"
            :value="String(teacher.id)"
          >
            {{ personName(teacher) }}
          </option>
        </UiSelect>
      </label>

      <div
        v-if="selectedTeacher"
        class="admin-chip-list"
        style="margin-top: 12px;"
      >
        <span class="admin-chip">
          {{
            personName(
              selectedTeacher
            )
          }}
        </span>

        <span class="admin-chip">
          Назначено:
          {{ teacherMemberships.length }}
        </span>
      </div>
    </UiCard>

    <section
      v-if="teacherId"
      class="admin-grid admin-grid--2"
    >
      <UiCard>
        <div class="admin-card__header">
          <div>
            <h2>
              Доступные предметы
            </h2>

            <p>
              Выберите один или несколько предметов.
            </p>
          </div>
        </div>

        <UiEmptyState
          v-if="!freeSubjects.length"
          description="Все предметы уже назначены."
          compact
        />

        <div
          v-else
          class="admin-checkbox-list"
        >
          <UiCheckbox
            v-for="subject in freeSubjects"
            :key="subject.id"
            v-model="availableSelection"
            :value="subject.id"
            :label="subject.name"
            :description="
              subject.description ??
              `#${subject.id}`
            "
          />
        </div>

        <label
          class="admin-field"
          style="margin-top: 14px;"
        >
          <span>Примечание</span>

          <UiTextarea
            v-model="notes"
            placeholder="Необязательное примечание"
          />
        </label>

        <UiButton
            variant="primary"
          type="button"
          style="margin-top: 12px;"
          :disabled="
            saving ||
            !availableSelection.length
          "
          @click="addSubjects"
        >
          Назначить выбранные
        </UiButton>
      </UiCard>

      <UiCard>
        <div class="admin-card__header">
          <div>
            <h2>
              Текущие предметы
            </h2>

            <p>
              Активные назначения преподавателя.
            </p>
          </div>
        </div>

        <UiEmptyState
          v-if="
            !teacherMemberships.length
          "
          description="У преподавателя пока нет предметов."
          compact
        />

        <div
          v-else
          class="admin-checkbox-list"
        >
          <UiCheckbox
            v-for="
              membership in
              teacherMemberships
            "
            :key="membership.id"
            v-model="assignedSelection"
            :value="membership.id"
            :label="
              subjectName(
                membership.subjectId
              )
            "
            :description="
              membership.notes ??
              `Назначение #${membership.id}`
            "
          />
        </div>

        <UiButton
            variant="danger"
          type="button"
          style="margin-top: 12px;"
          :disabled="
            saving ||
            !assignedSelection.length
          "
          @click="removeSubjects"
        >
          Снять выбранные
        </UiButton>
      </UiCard>
    </section>
  </AdminPageShell>
</template>
