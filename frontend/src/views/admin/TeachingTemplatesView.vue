<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTable from '@/components/admin/AdminTable.vue'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  subjectsApi,
  teachingApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

const TEACHER_ROLE = 1

const DEFAULT_LOAD_TYPE_NAME =
  'Основная нагрузка'

const DEFAULT_LOAD_TYPE_DESCRIPTION =
  'Системный тип нагрузки для шаблонов факультета'

const STATUS_LABELS = {
  1: 'Активно',
  2: 'Черновик',
  3: 'Закрыто',
  4: 'В паузе',
}

let rowSequence = 0

const faculties = ref([])
const facultySubjects = ref([])
const groups = ref([])
const people = ref([])
const teacherMemberships = ref([])
const assignments = ref([])
const loadTypes = ref([])

const defaultLoadTypeId = ref(null)

const loading = ref(false)
const saving = ref(false)

const period = reactive({
  studyCourse: '1',
  semester: '1',
  academicYear: '',
  facultyId: '',
  status: '1',
  notes: '',
})

const rows = ref([])

const notice = ref({
  type: 'info',
  message: '',
})

function createRow() {
  rowSequence += 1

  return {
    id: rowSequence,
    subjectId: '',
    teacherMembershipId: '',
    groupIds: [],
  }
}

function showNotice(type, message) {
  notice.value = {
    type,
    message,
  }
}

function clearNotice() {
  notice.value.message = ''
}

function setDefaultAcademicYear() {
  period.academicYear =
    String(new Date().getFullYear())
}

function personLabel(personId) {
  const person = people.value.find(
    (item) =>
      Number(item.id) ===
      Number(personId)
  )

  if (!person) {
    return `Преподаватель #${personId}`
  }

  return (
    [
      person.lastName,
      person.firstName,
      person.middleName,
    ]
      .filter(Boolean)
      .join(' ')
      .trim() ||
    person.fullName ||
    `Преподаватель #${person.id}`
  )
}

function subjectName(subjectId) {
  return (
    facultySubjects.value.find(
      (item) =>
        Number(item.id) ===
        Number(subjectId)
    )?.name ??
    `Предмет #${subjectId}`
  )
}

function groupName(groupId) {
  return (
    groups.value.find(
      (item) =>
        Number(item.id) ===
        Number(groupId)
    )?.name ??
    `Группа #${groupId}`
  )
}

function membershipById(id) {
  return teacherMemberships.value.find(
    (item) =>
      Number(item.id) === Number(id)
  )
}

function teacherNameForMembership(id) {
  const membership =
    membershipById(id)

  return membership
    ? personLabel(
        membership.personId
      )
    : `Membership #${id}`
}

const assignmentColumns = computed(() => [
  {
    key: 'subject',
    label: 'Предмет',
    value: (row) =>
      subjectName(
        membershipById(
          row.subjectMembershipId
        )?.subjectId
      ),
  },
  {
    key: 'group',
    label: 'Группа',
    value: (row) =>
      groupName(row.groupId),
  },
  {
    key: 'teacher',
    label: 'Преподаватель',
    value: (row) =>
      teacherNameForMembership(
        row.subjectMembershipId
      ),
  },
  {
    key: 'status',
    label: 'Статус',
    value: (row) =>
      STATUS_LABELS[
        Number(row.status)
      ] ?? row.status,
  },
  {
    key: 'notes',
    label: 'Примечание',
  },
  {
    key: 'actions',
    label: 'Действие',
    sortable: false,
  },
])

function teachersForSubject(subjectId) {
  return teacherMemberships.value.filter(
    (item) =>
      Number(item.subjectId) ===
      Number(subjectId)
  )
}

const selectedFaculty = computed(() => {
  return faculties.value.find(
    (item) =>
      Number(item.id) ===
      Number(period.facultyId)
  )
})

const activeAssignments = computed(() => {
  return assignments.value.filter(
    (item) =>
      Number(item.status) !== 3
  )
})

const summary = computed(() => ({
  subjects:
    facultySubjects.value.length,
  groups: groups.value.length,
  assignments:
    activeAssignments.value.length,
}))

function normalizeRow(row) {
  const teachers =
    teachersForSubject(
      row.subjectId
    )

  if (
    !teachers.some(
      (item) =>
        Number(item.id) ===
        Number(
          row.teacherMembershipId
        )
    )
  ) {
    row.teacherMembershipId = ''
  }

  row.groupIds = uniqueNumbers(
    row.groupIds
  ).filter(
    (groupId) =>
      groups.value.some(
        (group) =>
          Number(group.id) ===
          Number(groupId)
      )
  )
}

function addRow() {
  rows.value.push(
    createRow()
  )
}

function removeRow(rowId) {
  rows.value =
    rows.value.filter(
      (row) =>
        row.id !== rowId
    )

  if (!rows.value.length) {
    addRow()
  }
}

function assignmentExists(
  subjectMembershipId,
  groupId
) {
  return assignments.value.some(
    (item) =>
      Number(
        item.subjectMembershipId
      ) ===
        Number(
          subjectMembershipId
        ) &&
      Number(item.groupId) ===
        Number(groupId) &&
      Number(item.semester) ===
        Number(period.semester) &&
      Number(item.studyCourse) ===
        Number(period.studyCourse) &&
      String(item.academicYear) ===
        String(period.academicYear) &&
      Number(item.status) !== 3
  )
}

async function ensureDefaultLoadType() {
  const preferred =
    loadTypes.value.find(
      (item) =>
        String(
          item.name ?? ''
        ).toLowerCase() ===
        DEFAULT_LOAD_TYPE_NAME.toLowerCase()
    )

  if (preferred) {
    defaultLoadTypeId.value =
      preferred.id
    return
  }

  const response =
    await teachingApi.createLoadType({
      name:
        DEFAULT_LOAD_TYPE_NAME,
      description:
        DEFAULT_LOAD_TYPE_DESCRIPTION,
    })

  defaultLoadTypeId.value =
    response.data?.id

  const refreshed =
    await teachingApi.getLoadTypes()

  loadTypes.value =
    listFromResponse(refreshed)

  if (!defaultLoadTypeId.value) {
    defaultLoadTypeId.value =
      loadTypes.value.find(
        (item) =>
          String(
            item.name ?? ''
          ).toLowerCase() ===
          DEFAULT_LOAD_TYPE_NAME.toLowerCase()
      )?.id ?? null
  }
}

async function ensureSubjectLoadType(
  subjectMembershipId
) {
  if (!defaultLoadTypeId.value) {
    await ensureDefaultLoadType()
  }

  const response =
    await teachingApi
      .getSubjectLoadTypes({
        subjectMembershipId,
        teachingLoadTypeId:
          defaultLoadTypeId.value,
      })

  const existing =
    listFromResponse(response)

  if (
    existing.some(
      (item) =>
        Number(
          item.teachingLoadTypeId
        ) ===
        Number(
          defaultLoadTypeId.value
        )
    )
  ) {
    return
  }

  await teachingApi
    .addLoadTypeToSubjectMembership(
      subjectMembershipId,
      {
        teachingLoadTypeId:
          Number(
            defaultLoadTypeId.value
          ),
        notes:
          'Автоматически добавлено из шаблона нагрузки',
      }
    )
}

async function loadBaseData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      peopleResponse,
      membershipsResponse,
      loadTypesResponse,
    ] = await Promise.all([
      facultiesApi.getAll(),
      usersApi.getPeople(),
      membershipsApi
        .getSubjectMemberships({
          activeOnly: false,
        }),
      teachingApi.getLoadTypes(),
    ])

    faculties.value =
      listFromResponse(
        facultiesResponse
      )

    people.value =
      listFromResponse(
        peopleResponse
      )

    teacherMemberships.value =
      listFromResponse(
        membershipsResponse
      ).filter(
        (item) =>
          Number(item.role) ===
          TEACHER_ROLE
      )

    loadTypes.value =
      listFromResponse(
        loadTypesResponse
      )

    await ensureDefaultLoadType()

    if (
      !period.facultyId &&
      faculties.value.length
    ) {
      period.facultyId = String(
        faculties.value[0].id
      )
    }
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить данные шаблонов'
      )
    )
  } finally {
    loading.value = false
  }
}

async function loadFacultyContext() {
  if (!period.facultyId) {
    facultySubjects.value = []
    groups.value = []
    assignments.value = []
    return
  }

  loading.value = true

  try {
    const [
      subjectsResponse,
      groupsResponse,
    ] = await Promise.all([
      facultiesApi.getSubjects(
        Number(period.facultyId)
      ),
      groupsApi.getAll({
        facultyId:
          Number(period.facultyId),
      }),
    ])

    facultySubjects.value =
      listFromResponse(
        subjectsResponse
      )

    groups.value =
      listFromResponse(
        groupsResponse
      )

    rows.value.forEach(
      normalizeRow
    )

    await refreshAssignments()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить факультет'
      )
    )
  } finally {
    loading.value = false
  }
}

async function refreshAssignments() {
  if (!period.facultyId) {
    assignments.value = []
    return
  }

  try {
    const response =
      await teachingApi
        .getAssignments({
          facultyId:
            Number(period.facultyId),
          studyCourse:
            Number(
              period.studyCourse
            ),
          semester:
            Number(period.semester),
          academicYear:
            Number(
              period.academicYear
            ),
        })

    assignments.value =
      listFromResponse(response)
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить назначения'
      )
    )
  }
}

async function assignGroups() {
  const completeRows =
    rows.value.filter(
      (row) =>
        Number(row.subjectId) > 0 &&
        Number(
          row.teacherMembershipId
        ) > 0 &&
        uniqueNumbers(
          row.groupIds
        ).length > 0
    )

  if (
    !period.facultyId ||
    !Number(period.academicYear)
  ) {
    showNotice(
      'error',
      'Заполните факультет и учебный год.'
    )
    return
  }

  if (!completeRows.length) {
    showNotice(
      'error',
      'Добавьте предмет, преподавателя и хотя бы одну группу.'
    )
    return
  }

  const tasks = []

  for (const row of completeRows) {
    for (
      const groupId of
      uniqueNumbers(row.groupIds)
    ) {
      if (
        !assignmentExists(
          row.teacherMembershipId,
          groupId
        )
      ) {
        tasks.push({
          subjectMembershipId:
            Number(
              row.teacherMembershipId
            ),
          groupId,
        })
      }
    }
  }

  if (!tasks.length) {
    showNotice(
      'info',
      'Все выбранные назначения уже существуют.'
    )
    return
  }

  saving.value = true

  try {
    const membershipIds =
      uniqueNumbers(
        tasks.map(
          (item) =>
            item.subjectMembershipId
        )
      )

    for (
      const membershipId of
      membershipIds
    ) {
      await ensureSubjectLoadType(
        membershipId
      )
    }

    const results =
      await Promise.allSettled(
        tasks.map(
          (task) =>
            teachingApi
              .createAssignment({
                subjectMembershipId:
                  task.subjectMembershipId,
                groupId:
                  task.groupId,
                loadTypeId:
                  Number(
                    defaultLoadTypeId.value
                  ),
                courseVersionId:
                  null,
                semester:
                  Number(
                    period.semester
                  ),
                studyCourse:
                  Number(
                    period.studyCourse
                  ),
                academicYear:
                  Number(
                    period.academicYear
                  ),
                hoursPerWeek: 0,
                status:
                  Number(period.status),
                notes:
                  period.notes.trim() ||
                  null,
              })
        )
      )

    const successCount =
      results.filter(
        (item) =>
          item.status ===
          'fulfilled'
      ).length

    const failed =
      results.filter(
        (item) =>
          item.status ===
          'rejected'
      ).length

    if (!successCount) {
      throw (
        results.find(
          (item) =>
            item.status ===
            'rejected'
        )?.reason ??
        new Error(
          'Не удалось создать назначения'
        )
      )
    }

    showNotice(
      failed ? 'warning' : 'success',
      failed
        ? `Создано: ${successCount}. Не удалось создать: ${failed}.`
        : `Создано назначений: ${successCount}.`
    )

    period.notes = ''
    await refreshAssignments()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось создать назначения'
      )
    )
  } finally {
    saving.value = false
  }
}

async function saveAssignment(
  assignment
) {
  saving.value = true

  try {
    await ensureSubjectLoadType(
      assignment.subjectMembershipId
    )

    await teachingApi
      .updateAssignment(
        assignment.id,
        {
          subjectMembershipId:
            Number(
              assignment.subjectMembershipId
            ),
          groupId:
            Number(
              assignment.groupId
            ),
          loadTypeId:
            assignment.loadTypeId ??
            Number(
              defaultLoadTypeId.value
            ),
          courseVersionId:
            assignment.courseVersionId ??
            null,
          semester:
            Number(
              assignment.semester
            ),
          studyCourse:
            Number(
              assignment.studyCourse
            ),
          academicYear:
            Number(
              assignment.academicYear
            ),
          hoursPerWeek:
            assignment.hoursPerWeek ??
            0,
          status:
            Number(
              assignment.status
            ),
          notes:
            assignment.notes ?? null,
        }
      )

    showNotice(
      'success',
      'Назначение обновлено.'
    )

    await refreshAssignments()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось обновить назначение'
      )
    )
  } finally {
    saving.value = false
  }
}

watch(
  () => period.facultyId,
  loadFacultyContext
)

watch(
  [
    () => period.studyCourse,
    () => period.semester,
    () => period.academicYear,
  ],
  () => {
    if (period.facultyId) {
      refreshAssignments()
    }
  }
)

setDefaultAcademicYear()
rows.value = [createRow()]

onMounted(loadBaseData)
</script>

<template>
  <AdminPageShell
    title="Шаблоны нагрузки"
    description="Назначение преподавателей на группы по предметам и учебному периоду."
  >
    <template #actions>
      <button
        class="admin-btn"
        type="button"
        :disabled="loading"
        @click="loadBaseData"
      >
        Обновить справочники
      </button>
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-summary">
      <div class="admin-stat">
        <span class="admin-stat__label">
          Факультет
        </span>

        <strong
          class="admin-stat__value"
          style="font-size: 15px;"
        >
          {{
            selectedFaculty?.name ??
            'Не выбран'
          }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Предметов
        </span>

        <strong class="admin-stat__value">
          {{ summary.subjects }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Групп
        </span>

        <strong class="admin-stat__value">
          {{ summary.groups }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных назначений
        </span>

        <strong class="admin-stat__value">
          {{ summary.assignments }}
        </strong>
      </div>
    </section>

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>Параметры шаблона</h2>
          <p>
            Учебный период и факультет определяют область назначений.
          </p>
        </div>
      </div>

      <div class="admin-form-grid admin-form-grid--4">
        <label class="admin-field">
          <span>Курс</span>

          <select
            v-model="period.studyCourse"
            class="admin-select"
          >
            <option
              v-for="course in 6"
              :key="course"
              :value="String(course)"
            >
              {{ course }}
            </option>
          </select>
        </label>

        <label class="admin-field">
          <span>Семестр</span>

          <select
            v-model="period.semester"
            class="admin-select"
          >
            <option value="1">
              1
            </option>

            <option value="2">
              2
            </option>
          </select>
        </label>

        <label class="admin-field">
          <span>Учебный год</span>

          <input
            v-model="period.academicYear"
            class="admin-input"
            type="number"
            min="2000"
            step="1"
            required
            placeholder="2026"
          >
        </label>

        <label class="admin-field">
          <span>Факультет</span>

          <select
            v-model="period.facultyId"
            class="admin-select"
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

        <label class="admin-field">
          <span>Статус новых назначений</span>

          <select
            v-model="period.status"
            class="admin-select"
          >
            <option
              v-for="(label, value) in STATUS_LABELS"
              :key="value"
              :value="String(value)"
            >
              {{ label }}
            </option>
          </select>
        </label>

        <label class="admin-field admin-field--wide">
          <span>Примечание</span>

          <textarea
            v-model="period.notes"
            class="admin-textarea"
          />
        </label>
      </div>
    </section>

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>Предметы и преподаватели</h2>
          <p>
            Для каждой строки выберите предмет, преподавателя и группы.
          </p>
        </div>

        <button
          class="admin-btn"
          type="button"
          @click="addRow"
        >
          + Добавить строку
        </button>
      </div>

      <div class="admin-grid">
        <article
          v-for="(row, index) in rows"
          :key="row.id"
          class="admin-card admin-card--compact"
        >
          <div class="admin-card__header">
            <div>
              <h3>
                Назначение {{ index + 1 }}
              </h3>
            </div>

            <button
              v-if="rows.length > 1"
              class="admin-btn admin-btn--danger admin-btn--small"
              type="button"
              @click="removeRow(row.id)"
            >
              Удалить
            </button>
          </div>

          <div class="admin-form-grid">
            <label class="admin-field">
              <span>Предмет</span>

              <select
                v-model="row.subjectId"
                class="admin-select"
                @change="normalizeRow(row)"
              >
                <option value="">
                  Выберите предмет
                </option>

                <option
                  v-for="subject in facultySubjects"
                  :key="subject.id"
                  :value="String(subject.id)"
                >
                  {{ subject.name }}
                </option>
              </select>
            </label>

            <label class="admin-field">
              <span>Преподаватель</span>

              <select
                v-model="row.teacherMembershipId"
                class="admin-select"
                :disabled="!row.subjectId"
              >
                <option value="">
                  Выберите преподавателя
                </option>

                <option
                  v-for="
                    membership in
                    teachersForSubject(
                      row.subjectId
                    )
                  "
                  :key="membership.id"
                  :value="String(membership.id)"
                >
                  {{
                    personLabel(
                      membership.personId
                    )
                  }}
                </option>
              </select>
            </label>

            <div class="admin-field admin-field--wide">
              <span>Группы</span>

              <div
                v-if="!groups.length"
                class="admin-empty"
              >
                Для факультета нет групп.
              </div>

              <div
                v-else
                class="admin-checkbox-list"
              >
                <label
                  v-for="group in groups"
                  :key="group.id"
                  class="admin-checkbox"
                >
                  <input
                    v-model="row.groupIds"
                    type="checkbox"
                    :value="group.id"
                  >

                  <span class="admin-checkbox__copy">
                    <span class="admin-checkbox__title">
                      {{ group.name }}
                    </span>

                    <span class="admin-checkbox__meta">
                      {{ group.code ?? `#${group.id}` }}
                    </span>
                  </span>
                </label>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div
        class="admin-actions admin-actions--end admin-actions--mobile-stack"
        style="margin-top: 16px;"
      >
        <button
          class="admin-btn admin-btn--primary"
          type="button"
          :disabled="saving || loading"
          @click="assignGroups"
        >
          {{
            saving
              ? 'Сохранение...'
              : 'Создать назначения'
          }}
        </button>
      </div>
    </section>

    <section class="admin-card">
      <div class="admin-card__header">
        <div>
          <h2>Текущие назначения</h2>

          <p>
            {{
              period.academicYear
            }},
            курс
            {{ period.studyCourse }},
            семестр
            {{ period.semester }}
          </p>
        </div>

        <button
          class="admin-btn"
          type="button"
          @click="refreshAssignments"
        >
          Обновить
        </button>
      </div>

      <AdminTable
        :columns="assignmentColumns"
        :rows="assignments"
        empty-message="Назначения для выбранного периода не найдены."
        :default-sort="{
          key: 'subject',
          direction: 'asc',
        }"
      >
        <template #cell-teacher="{ row }">
          <select
            v-model="row.subjectMembershipId"
            class="admin-select"
          >
            <option
              v-for="
                membership in
                teachersForSubject(
                  membershipById(
                    row.subjectMembershipId
                  )?.subjectId
                )
              "
              :key="membership.id"
              :value="membership.id"
            >
              {{
                personLabel(
                  membership.personId
                )
              }}
            </option>
          </select>
        </template>

        <template #cell-status="{ row }">
          <select
            v-model="row.status"
            class="admin-select"
          >
            <option
              v-for="(label, value) in STATUS_LABELS"
              :key="value"
              :value="Number(value)"
            >
              {{ label }}
            </option>
          </select>
        </template>

        <template #cell-notes="{ row }">
          <input
            v-model="row.notes"
            class="admin-input"
          >
        </template>

        <template #cell-actions="{ row }">
          <button
            class="admin-btn admin-btn--small admin-btn--primary"
            type="button"
            :disabled="saving"
            @click="saveAssignment(row)"
          >
            Сохранить
          </button>
        </template>
      </AdminTable>
    </section>
  </AdminPageShell>
</template>
