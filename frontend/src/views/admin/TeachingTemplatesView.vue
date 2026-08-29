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
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiSelect,
  UiTextarea,
} from '@/components/ui'

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
      <UiButton
        type="button"
        :disabled="loading"
        @click="loadBaseData"
      >
        Обновить справочники
      </UiButton>
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

    <UiCard>
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

          <UiSelect
            v-model="period.studyCourse"
          >
            <option
              v-for="course in 6"
              :key="course"
              :value="String(course)"
            >
              {{ course }}
            </option>
          </UiSelect>
        </label>

        <label class="admin-field">
          <span>Семестр</span>

          <UiSelect
            v-model="period.semester"
          >
            <option value="1">
              1
            </option>

            <option value="2">
              2
            </option>
          </UiSelect>
        </label>

        <label class="admin-field">
          <span>Учебный год</span>

          <UiInput
            v-model="period.academicYear"
            type="number"
            min="2000"
            step="1"
            required
            placeholder="2026"
          />
        </label>

        <label class="admin-field">
          <span>Факультет</span>

          <UiSelect
            v-model="period.facultyId"
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
          </UiSelect>
        </label>

        <label class="admin-field">
          <span>Статус новых назначений</span>

          <UiSelect
            v-model="period.status"
          >
            <option
              v-for="(label, value) in STATUS_LABELS"
              :key="value"
              :value="String(value)"
            >
              {{ label }}
            </option>
          </UiSelect>
        </label>

        <label class="admin-field admin-field--wide">
          <span>Примечание</span>

          <UiTextarea
            v-model="period.notes"
          />
        </label>
      </div>
    </UiCard>

    <UiCard>
      <div class="admin-card__header">
        <div>
          <h2>Предметы и преподаватели</h2>
          <p>
            Для каждой строки выберите предмет, преподавателя и группы.
          </p>
        </div>

        <UiButton
          type="button"
          @click="addRow"
        >
          + Добавить строку
        </UiButton>
      </div>

      <div class="admin-grid">
        <UiCard
          v-for="(row, index) in rows"
          :key="row.id"
          compact
        >
          <div class="admin-card__header">
            <div>
              <h3>
                Назначение {{ index + 1 }}
              </h3>
            </div>

            <UiButton
            variant="danger"
            size="sm"
              v-if="rows.length > 1"
              type="button"
              @click="removeRow(row.id)"
            >
              Удалить
            </UiButton>
          </div>

          <div class="admin-form-grid">
            <label class="admin-field">
              <span>Предмет</span>

              <UiSelect
                v-model="row.subjectId"
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
              </UiSelect>
            </label>

            <label class="admin-field">
              <span>Преподаватель</span>

              <UiSelect
                v-model="row.teacherMembershipId"
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
              </UiSelect>
            </label>

            <div class="admin-field admin-field--wide">
              <span>Группы</span>

              <UiEmptyState
                v-if="!groups.length"
                description="Для факультета нет групп."
                compact
              />

              <div
                v-else
                class="admin-checkbox-list"
              >
                <UiCheckbox
                  v-for="group in groups"
                  :key="group.id"
                  v-model="row.groupIds"
                  :value="group.id"
                  :label="group.name"
                  :description="
                    group.code ??
                    `#${group.id}`
                  "
                />
              </div>
            </div>
          </div>
        </UiCard>
      </div>

      <div
        class="admin-actions admin-actions--end admin-actions--mobile-stack"
        style="margin-top: 16px;"
      >
        <UiButton
            variant="primary"
          type="button"
          :disabled="saving || loading"
          @click="assignGroups"
        >
          {{
            saving
              ? 'Сохранение...'
              : 'Создать назначения'
          }}
        </UiButton>
      </div>
    </UiCard>

    <UiCard>
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

        <UiButton
          type="button"
          @click="refreshAssignments"
        >
          Обновить
        </UiButton>
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
          <UiSelect
            v-model="row.subjectMembershipId"
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
          </UiSelect>
        </template>

        <template #cell-status="{ row }">
          <UiSelect
            v-model="row.status"
          >
            <option
              v-for="(label, value) in STATUS_LABELS"
              :key="value"
              :value="Number(value)"
            >
              {{ label }}
            </option>
          </UiSelect>
        </template>

        <template #cell-notes="{ row }">
          <UiInput
            v-model="row.notes"
          />
        </template>

        <template #cell-actions="{ row }">
          <UiButton
            variant="primary"
            size="sm"
            type="button"
            :disabled="saving"
            @click="saveAssignment(row)"
          >
            Сохранить
          </UiButton>
        </template>
      </AdminTable>
    </UiCard>
  </AdminPageShell>
</template>
