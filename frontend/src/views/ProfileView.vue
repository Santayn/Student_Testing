<script setup>
import { computed, onMounted, ref } from 'vue'

import { useAuthStore } from '@/stores/auth'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  subjectsApi,
  teachingApi,
} from '@/api'

const SUBJECT_ROLE_TEACHER = 1
const GROUP_ROLE_STUDENT = 1

const authStore = useAuthStore()

const activeTab = ref('user')
const loading = ref(false)
const error = ref('')

const studentInfo = ref({
  membership: null,
  group: null,
  faculty: null,
  subjects: [],
})

const teacherInfo = ref({
  subjects: [],
  groups: [],
})

const user = computed(() => authStore.user ?? {})
const person = computed(() => user.value.person ?? {})

const firstName = computed(() => {
  return (
    person.value.firstName ??
    user.value.firstName ??
    '-'
  )
})

const lastName = computed(() => {
  return (
    person.value.lastName ??
    user.value.lastName ??
    '-'
  )
})

const middleName = computed(() => {
  return (
    person.value.middleName ??
    user.value.middleName ??
    ''
  )
})

const phoneNumber = computed(() => {
  return (
    person.value.phone ??
    person.value.phoneNumber ??
    user.value.phone ??
    user.value.phoneNumber ??
    '-'
  )
})

const loginName = computed(() => {
  return user.value.login ?? '-'
})

const email = computed(() => {
  return user.value.email ?? person.value.email ?? '-'
})

const personId = computed(() => {
  return (
    user.value.personId ??
    person.value.id ??
    null
  )
})

const fullName = computed(() => {
  const value = [
    lastName.value !== '-' ? lastName.value : '',
    firstName.value !== '-' ? firstName.value : '',
    middleName.value,
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return value || authStore.fullName || loginName.value
})

const roleNames = computed(() => {
  const labels = {
    ADMIN: 'Администратор',
    TEACHER: 'Преподаватель',
    STUDENT: 'Студент',
  }

  return authStore.roles
    .map((role) => {
      if (typeof role === 'string') {
        return labels[role] ?? role
      }

      const value =
        role?.name ??
        role?.code ??
        role?.authority ??
        ''

      return labels[value] ?? value
    })
    .filter(Boolean)
})

const rolesText = computed(() => {
  return roleNames.value.length
    ? roleNames.value.join(', ')
    : '-'
})

const tabs = computed(() => {
  const items = [
    {
      name: 'user',
      label: 'Пользователь',
      visible: true,
    },
  ]

  if (authStore.isStudent) {
    items.push({
      name: 'student',
      label: 'Студент',
      visible: true,
    })
  }

  if (authStore.isTeacher || authStore.isAdmin) {
    items.push({
      name: 'teacher',
      label: 'Преподаватель',
      visible: true,
    })
  }

  return items
})

function arrayData(response) {
  const data = response?.data

  if (Array.isArray(data)) {
    return data
  }

  if (Array.isArray(data?.content)) {
    return data.content
  }

  if (Array.isArray(data?.items)) {
    return data.items
  }

  return []
}

function unique(values) {
  return [
    ...new Set(
      values.filter(
        (value) =>
          value !== null &&
          value !== undefined
      )
    ),
  ]
}

function subjectLabel(subject) {
  return (
    subject?.name ??
    subject?.title ??
    `Предмет #${subject?.id ?? '?'}`
  )
}

function groupLabel(group) {
  return (
    group?.name ??
    group?.code ??
    `Группа #${group?.id ?? '?'}`
  )
}

function facultyLabel(faculty) {
  if (!faculty) {
    return '-'
  }

  return (
    faculty.name ??
    faculty.code ??
    `Факультет #${faculty.id ?? '?'}`
  )
}

async function loadStudentInfo() {
  if (!personId.value) {
    return
  }

  const membershipsResponse =
    await membershipsApi.getGroupMemberships({
      personId: personId.value,
      activeOnly: true,
    })

  const memberships = arrayData(
    membershipsResponse
  )

  const membership = memberships.find(
    (item) =>
      Number(item.role) === GROUP_ROLE_STUDENT
  )

  if (!membership) {
    studentInfo.value = {
      membership: null,
      group: null,
      faculty: null,
      subjects: [],
    }

    return
  }

  const groupResponse =
    await groupsApi.getById(
      membership.groupId
    )

  const group = groupResponse.data ?? null

  let faculty = null

  if (group?.facultyId) {
    const facultyResponse =
      await facultiesApi.getById(
        group.facultyId
      )

    faculty = facultyResponse.data ?? null
  }

  const [
    enrollmentsResponse,
    groupAssignmentsResponse,
  ] = await Promise.all([
    teachingApi.getEnrollments({
      groupMembershipId: membership.id,
    }),

    teachingApi.getAssignments({
      groupId: group?.id,
    }),
  ])

  const enrollments = arrayData(
    enrollmentsResponse
  )

  const groupAssignments = arrayData(
    groupAssignmentsResponse
  )

  const enrolledAssignmentIds = unique(
    enrollments.map(
      (item) =>
        item.teachingAssignmentId
    )
  )

  const enrolledAssignments =
    await Promise.all(
      enrolledAssignmentIds.map(
        async (assignmentId) => {
          const response =
            await teachingApi.getAssignment(
              assignmentId
            )

          return response.data
        }
      )
    )

  const assignments = [
    ...new Map(
      [
        ...groupAssignments,
        ...enrolledAssignments,
      ]
        .filter(
          (item) =>
            item &&
            item.id !== null &&
            item.id !== undefined
        )
        .map(
          (item) => [item.id, item]
        )
    ).values(),
  ]

  const subjectMembershipIds = unique(
    assignments.map(
      (item) =>
        item.subjectMembershipId
    )
  )

  const subjectMemberships =
    await Promise.all(
      subjectMembershipIds.map(
        async (membershipId) => {
          const response =
            await membershipsApi
              .getSubjectMembership(
                membershipId
              )

          return response.data
        }
      )
    )

  const subjectIds = unique(
    subjectMemberships.map(
      (item) => item?.subjectId
    )
  )

  const subjects = await Promise.all(
    subjectIds.map(
      async (subjectId) => {
        const response =
          await subjectsApi.getById(
            subjectId
          )

        return response.data
      }
    )
  )

  studentInfo.value = {
    membership,
    group,
    faculty,
    subjects,
  }
}

async function loadTeacherInfo() {
  if (!personId.value) {
    return
  }

  const membershipsResponse =
    await membershipsApi
      .getSubjectMemberships({
        personId: personId.value,
        activeOnly: true,
      })

  const memberships = arrayData(
    membershipsResponse
  )

  const teacherMemberships =
    memberships.filter(
      (item) =>
        Number(item.role) ===
        SUBJECT_ROLE_TEACHER
    )

  const subjectIds = unique(
    teacherMemberships.map(
      (item) => item.subjectId
    )
  )

  const subjects = await Promise.all(
    subjectIds.map(
      async (subjectId) => {
        const response =
          await subjectsApi.getById(
            subjectId
          )

        return response.data
      }
    )
  )

  const assignmentResponses =
    await Promise.all(
      teacherMemberships.map(
        (membership) =>
          teachingApi.getAssignments({
            subjectMembershipId:
              membership.id,
          })
      )
    )

  const assignments =
    assignmentResponses.flatMap(
      (response) =>
        arrayData(response)
    )

  const groupIds = unique(
    assignments.map(
      (item) => item.groupId
    )
  )

  const groups = await Promise.all(
    groupIds.map(
      async (groupId) => {
        const response =
          await groupsApi.getById(
            groupId
          )

        return response.data
      }
    )
  )

  teacherInfo.value = {
    subjects,
    groups,
  }
}

async function loadProfile() {
  loading.value = true
  error.value = ''

  try {
    await authStore.loadCurrentUser()

    const jobs = []

    if (authStore.isStudent) {
      jobs.push(loadStudentInfo())
    }

    if (
      authStore.isTeacher ||
      authStore.isAdmin
    ) {
      jobs.push(loadTeacherInfo())
    }

    await Promise.all(jobs)

    if (
      !tabs.value.some(
        (tab) =>
          tab.name === activeTab.value
      )
    ) {
      activeTab.value = 'user'
    }
  } catch (requestError) {
    error.value = getApiErrorMessage(
      requestError,
      'Не удалось загрузить профиль'
    )
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-view">
    <header class="profile-header">
      <div>
        <p class="profile-header__eyebrow">
          Личный кабинет
        </p>

        <h1>
          {{ fullName }}
        </h1>

        <p>
          Профиль пользователя, учебные назначения
          и доступные разделы.
        </p>
      </div>

      <button
        class="profile-refresh"
        type="button"
        :disabled="loading"
        @click="loadProfile"
      >
        {{
          loading
            ? 'Обновление...'
            : 'Обновить'
        }}
      </button>
    </header>

    <div
      v-if="error"
      class="profile-alert"
      role="alert"
    >
      <span>
        {{ error }}
      </span>

      <button
        type="button"
        @click="loadProfile"
      >
        Повторить
      </button>
    </div>

    <div
      v-if="loading && !authStore.user"
      class="profile-loading"
    >
      Загрузка профиля...
    </div>

    <template v-else>
      <nav
        class="profile-tabs"
        aria-label="Разделы личного кабинета"
      >
        <button
          v-for="tab in tabs"
          :key="tab.name"
          class="profile-tab"
          :class="{
            'profile-tab--active':
              activeTab === tab.name,
          }"
          type="button"
          :aria-selected="
            activeTab === tab.name
          "
          @click="
            activeTab = tab.name
          "
        >
          {{ tab.label }}
        </button>
      </nav>

      <section
        v-if="activeTab === 'user'"
        class="profile-panel"
      >
        <div class="profile-panel__header">
          <div>
            <h2>Пользователь</h2>

            <p>
              Основная информация об учётной записи.
            </p>
          </div>
        </div>

        <dl class="data-grid">
          <div class="data-item">
            <dt>Имя</dt>
            <dd>{{ firstName }}</dd>
          </div>

          <div class="data-item">
            <dt>Фамилия</dt>
            <dd>{{ lastName }}</dd>
          </div>

          <div
            v-if="middleName"
            class="data-item"
          >
            <dt>Отчество</dt>
            <dd>{{ middleName }}</dd>
          </div>

          <div class="data-item">
            <dt>Логин</dt>
            <dd>{{ loginName }}</dd>
          </div>

          <div class="data-item">
            <dt>Email</dt>
            <dd>{{ email }}</dd>
          </div>

          <div class="data-item">
            <dt>Телефон</dt>
            <dd>{{ phoneNumber }}</dd>
          </div>

          <div class="data-item">
            <dt>Роли</dt>
            <dd>{{ rolesText }}</dd>
          </div>

          <div class="data-item">
            <dt>Person ID</dt>
            <dd>{{ personId ?? '-' }}</dd>
          </div>
        </dl>
      </section>

      <section
        v-if="
          activeTab === 'student' &&
          authStore.isStudent
        "
        class="profile-panel"
      >
        <div class="profile-panel__header">
          <div>
            <h2>Информация студента</h2>

            <p>
              Учебная группа, факультет и предметы.
            </p>
          </div>
        </div>

        <dl class="data-grid">
          <div class="data-item">
            <dt>Группа</dt>

            <dd>
              {{
                studentInfo.group
                  ? groupLabel(
                      studentInfo.group
                    )
                  : 'Нет активной группы'
              }}
            </dd>
          </div>

          <div class="data-item">
            <dt>Факультет</dt>

            <dd>
              {{
                facultyLabel(
                  studentInfo.faculty
                )
              }}
            </dd>
          </div>

          <div class="data-item">
            <dt>GroupMembership</dt>

            <dd>
              {{
                studentInfo.membership?.id
                  ? `#${studentInfo.membership.id}`
                  : '-'
              }}
            </dd>
          </div>
        </dl>

        <div class="profile-list-block">
          <h3>Предметы</h3>

          <ul
            v-if="
              studentInfo.subjects.length
            "
            class="entity-list"
          >
            <li
              v-for="
                subject in
                studentInfo.subjects
              "
              :key="subject.id"
            >
              <RouterLink
                :to="{
                  name: 'subject-details',
                  params: {
                    subjectId: subject.id,
                  },
                }"
              >
                {{
                  subjectLabel(subject)
                }}
              </RouterLink>

              <span>
                #{{ subject.id }}
              </span>
            </li>
          </ul>

          <p
            v-else
            class="empty-state"
          >
            Предметы не найдены.
          </p>
        </div>
      </section>

      <section
        v-if="
          activeTab === 'teacher' &&
          (
            authStore.isTeacher ||
            authStore.isAdmin
          )
        "
        class="profile-panel"
      >
        <div class="profile-panel__header">
          <div>
            <h2>
              Информация преподавателя
            </h2>

            <p>
              Предметы и группы из учебной нагрузки.
            </p>
          </div>
        </div>

        <div class="teacher-grid">
          <div class="profile-list-block">
            <h3>Предметы</h3>

            <ul
              v-if="
                teacherInfo.subjects.length
              "
              class="entity-list"
            >
              <li
                v-for="
                  subject in
                  teacherInfo.subjects
                "
                :key="subject.id"
              >
                <RouterLink
                  :to="{
                    name:
                      'subject-details',
                    params: {
                      subjectId:
                        subject.id,
                    },
                  }"
                >
                  {{
                    subjectLabel(subject)
                  }}
                </RouterLink>

                <span>
                  #{{ subject.id }}
                </span>
              </li>
            </ul>

            <p
              v-else
              class="empty-state"
            >
              Предметы не найдены.
            </p>
          </div>

          <div class="profile-list-block">
            <h3>
              Группы из нагрузки
            </h3>

            <ul
              v-if="
                teacherInfo.groups.length
              "
              class="entity-list"
            >
              <li
                v-for="
                  group in
                  teacherInfo.groups
                "
                :key="group.id"
              >
                <span>
                  {{ groupLabel(group) }}
                </span>

                <span>
                  #{{ group.id }}
                </span>
              </li>
            </ul>

            <p
              v-else
              class="empty-state"
            >
              Группы не найдены.
            </p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.profile-view {
  display: grid;
  gap: 18px;
}

.profile-header {
  padding: 24px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 14px;
}

.profile-header__eyebrow {
  margin: 0 0 6px;

  color:
    var(--brand, #2563eb);

  font-size: 12px;
  font-weight: 800;

  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.profile-header h1 {
  margin: 0;

  font-size: clamp(26px, 4vw, 36px);
  line-height: 1.15;
}

.profile-header p:not(
  .profile-header__eyebrow
) {
  margin: 8px 0 0;

  color:
    var(--text-secondary, #6b7280);

  font-size: 14px;
  line-height: 1.55;
}

.profile-refresh {
  min-height: 40px;

  padding: 8px 13px;

  color:
    var(--text, #111827);

  background:
    var(--surface-secondary, #f3f4f6);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 8px;

  font: inherit;
  font-size: 13px;
  font-weight: 700;

  cursor: pointer;
}

.profile-refresh:disabled {
  opacity: 0.55;
  cursor: default;
}

.profile-alert {
  padding: 11px 13px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  color:
    var(--danger, #dc2626);

  background:
    var(--danger-soft, #fef2f2);

  border: 1px solid
    color-mix(
      in srgb,
      var(--danger, #dc2626) 28%,
      transparent
    );

  border-radius: 9px;

  font-size: 13px;
}

.profile-alert button {
  color: inherit;

  background: transparent;
  border: 0;

  font: inherit;
  font-weight: 700;

  cursor: pointer;
}

.profile-loading {
  padding: 40px;

  color:
    var(--text-secondary, #6b7280);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 12px;

  text-align: center;
}

.profile-tabs {
  padding: 5px;

  display: flex;
  gap: 5px;

  overflow-x: auto;

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 10px;
}

.profile-tab {
  min-height: 38px;

  padding: 8px 13px;

  flex: 0 0 auto;

  color:
    var(--text-secondary, #6b7280);

  background: transparent;

  border: 0;
  border-radius: 7px;

  font: inherit;
  font-size: 13px;
  font-weight: 700;

  cursor: pointer;
}

.profile-tab:hover {
  color:
    var(--text, #111827);

  background:
    var(--surface-secondary, #f3f4f6);
}

.profile-tab--active {
  color: #ffffff;

  background:
    var(--brand, #2563eb);
}

.profile-tab--active:hover {
  color: #ffffff;

  background:
    var(--brand, #2563eb);
}

.profile-panel {
  padding: 22px;

  display: grid;
  gap: 20px;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 14px;
}

.profile-panel__header h2,
.profile-panel__header p {
  margin: 0;
}

.profile-panel__header h2 {
  font-size: 20px;
}

.profile-panel__header p {
  margin-top: 5px;

  color:
    var(--text-secondary, #6b7280);

  font-size: 13px;
}

.data-grid {
  margin: 0;

  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.data-item {
  padding: 12px;

  display: grid;
  gap: 4px;

  background:
    var(--surface-secondary, #f8fafc);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 9px;
}

.data-item dt {
  color:
    var(--text-secondary, #6b7280);

  font-size: 12px;
}

.data-item dd {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: 14px;
  font-weight: 600;
}

.teacher-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-list-block {
  min-width: 0;
}

.profile-list-block h3 {
  margin: 0 0 10px;

  font-size: 15px;
}

.entity-list {
  margin: 0;
  padding: 0;

  display: grid;
  gap: 7px;

  list-style: none;
}

.entity-list li {
  min-height: 42px;

  padding: 9px 11px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  background:
    var(--surface-secondary, #f8fafc);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 8px;

  font-size: 13px;
}

.entity-list li > span:last-child {
  color:
    var(--text-secondary, #6b7280);

  font-size: 12px;
}

.entity-list a {
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  color:
    var(--brand, #2563eb);

  font-weight: 700;
  text-decoration: none;
}

.empty-state {
  margin: 0;

  padding: 16px;

  color:
    var(--text-secondary, #6b7280);

  background:
    var(--surface-secondary, #f8fafc);

  border: 1px dashed
    var(--border, #d1d5db);

  border-radius: 8px;

  font-size: 13px;
  text-align: center;
}

@media (max-width: 720px) {
  .profile-header {
    padding: 20px;

    flex-direction: column;
  }

  .profile-refresh {
    width: 100%;
  }

  .data-grid,
  .teacher-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .profile-panel {
    padding: 17px;
  }

  .profile-tabs {
    border-radius: 8px;
  }

  .profile-tab {
    min-height: 40px;
  }

  .entity-list li {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
