<script setup>
import { computed, onMounted, ref } from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTable from '@/components/admin/AdminTable.vue'

import {
  UiButton,
  UiCard,
  UiCheckbox,
  UiSelect,
} from '@/components/ui'

import {
  getApiErrorMessage,
  rolesApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const roles = ref([])
const users = ref([])
const people = ref([])
const roleSelectionByUser = ref({})
const personSelectionByUser = ref({})

const roleFilter = ref('')
const loading = ref(false)
const savingUserId = ref(null)

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

function personName(user) {
  const person =
    people.value.find(
      (item) =>
        Number(item.id) ===
        Number(user?.personId)
    ) ??
    user?.person ??
    {}

  const value = [
    person.lastName ?? user?.lastName,
    person.firstName ?? user?.firstName,
    person.middleName ?? user?.middleName,
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return value || '—'
}

function personEmail(user) {
  return (
    people.value.find(
      (item) =>
        Number(item.id) ===
        Number(user?.personId)
    )?.email ??
    user?.person?.email ??
    '—'
  )
}

function availablePeopleFor(user) {
  const boundPersonIds = new Set(
    users.value
      .filter(
        (item) =>
          Number(item.id) !==
          Number(user.id)
      )
      .map(
        (item) =>
          Number(item.personId)
      )
      .filter(Number.isFinite)
  )

  return people.value.filter(
    (person) =>
      !boundPersonIds.has(
        Number(person.id)
      )
  )
}

function userRoleIds(user) {
  if (Array.isArray(user?.roleIds)) {
    return user.roleIds.map(Number)
  }

  if (Array.isArray(user?.roles)) {
    return user.roles
      .map((role) => {
        if (typeof role === 'number') {
          return role
        }

        if (typeof role === 'object') {
          return Number(role.id)
        }

        const found = roles.value.find(
          (item) =>
            String(item.name) ===
            String(role)
        )

        return Number(found?.id)
      })
      .filter(Number.isFinite)
  }

  return []
}

function roleNames(user) {
  const names = userRoleIds(user)
    .map(
      (roleId) =>
        roles.value.find(
          (role) =>
            Number(role.id) ===
            Number(roleId)
        )?.name
    )
    .filter(Boolean)

  return names.join(', ') || '—'
}

const userColumns = [
  {
    key: 'login',
    label: 'Пользователь',
  },
  {
    key: 'fullName',
    label: 'ФИО',
    value: personName,
    sortValue: personName,
  },
  {
    key: 'email',
    label: 'Email',
    value: personEmail,
    sortValue: personEmail,
  },
  {
    key: 'personBinding',
    label: 'Привязка профиля',
  },
  {
    key: 'roles',
    label: 'Роли',
    value: roleNames,
    sortValue: roleNames,
  },
]

const filteredUsers = computed(() => {
  const filter = String(roleFilter.value)

  if (!filter) {
    return users.value
  }

  const roleId = Number(filter)

  return users.value.filter(
    (user) =>
      userRoleIds(user).includes(roleId)
  )
})

async function loadData() {
  loading.value = true
  clearNotice()

  try {
    const [
      rolesResponse,
      usersResponse,
      peopleResponse,
    ] = await Promise.all([
      rolesApi.getAll(),
      usersApi.getAll(),
      usersApi.getPeople(),
    ])

    roles.value = listFromResponse(
      rolesResponse
    ).sort(
      (a, b) =>
        String(a.name ?? '').localeCompare(
          String(b.name ?? ''),
          'ru'
        )
    )

    users.value = listFromResponse(
      usersResponse
    ).sort(
      (a, b) =>
        String(a.login ?? '').localeCompare(
          String(b.login ?? ''),
          'ru'
        )
    )

    people.value = listFromResponse(
      peopleResponse
    ).sort(
      (a, b) =>
        personName(a).localeCompare(
          personName(b),
          'ru'
        )
    )

    roleSelectionByUser.value =
      Object.fromEntries(
        users.value.map((user) => [
          user.id,
          userRoleIds(user),
        ])
      )

    personSelectionByUser.value =
      Object.fromEntries(
        users.value.map((user) => [
          user.id,
          user.personId == null
            ? ''
            : String(user.personId),
        ])
      )
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить пользователей'
      )
    )
  } finally {
    loading.value = false
  }
}

async function applyRoles(user) {
  const roleIds = [
    ...new Set(
      (
        roleSelectionByUser.value[
          user.id
        ] ?? []
      )
        .map(Number)
        .filter(Number.isFinite)
    )
  ]

  if (!roleIds.length) {
    roleSelectionByUser.value[user.id] =
      userRoleIds(user)

    showNotice(
      'warning',
      'У пользователя должна остаться хотя бы одна роль.'
    )

    return
  }

  savingUserId.value = user.id

  try {
    await usersApi.updateRoles(
      user.id,
      {
        roleIds,
      }
    )

    showNotice(
      'success',
      `Роли пользователя ${user.login} обновлены.`
    )

    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось изменить роли'
      )
    )
  } finally {
    savingUserId.value = null
  }
}

async function applyPersonBinding(
  user,
  event
) {
  const rawValue =
    event.target.value

  const personId =
    rawValue === ''
      ? null
      : Number(rawValue)

  savingUserId.value = user.id

  try {
    await usersApi.updatePersonBinding(
      user.id,
      personId
    )

    showNotice(
      'success',
      personId == null
        ? `Профиль пользователя ${user.login} отвязан.`
        : `Профиль пользователя ${user.login} обновлён.`
    )

    await loadData()
  } catch (error) {
    personSelectionByUser.value[
      user.id
    ] =
      user.personId == null
        ? ''
        : String(user.personId)

    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось изменить привязку профиля'
      )
    )
  } finally {
    savingUserId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Роли пользователей"
    description="Просмотр пользователей и назначение одной или нескольких ролей."
  >
    <template #actions>
      <UiButton
        type="button"
        :disabled="loading"
        @click="loadData"
      >
        {{ loading ? 'Загрузка...' : 'Обновить' }}
      </UiButton>
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard>
      <div class="admin-toolbar">
        <div>
          <strong>Пользователи</strong>
          <div class="admin-muted">
            Найдено: {{ filteredUsers.length }}
          </div>
        </div>

        <label class="admin-field">
          <span>Фильтр по роли</span>

          <UiSelect
            v-model="roleFilter"
          >
            <option value="">
              Все роли
            </option>

            <option
              v-for="role in roles"
              :key="role.id"
              :value="String(role.id)"
            >
              {{ role.name }}
            </option>
          </UiSelect>
        </label>
      </div>
    </UiCard>

    <UiCard>
      <AdminTable
        :columns="userColumns"
        :rows="filteredUsers"
        :loading="loading"
        loading-message="Загрузка пользователей..."
        empty-message="Пользователи не найдены."
        :default-sort="{
          key: 'login',
          direction: 'asc',
        }"
      >
        <template #cell-login="{ row }">
          <strong>
            {{ row.login }}
          </strong>

          <div class="admin-muted">
            #{{ row.id }}
          </div>
        </template>

        <template #cell-roles="{ row }">
          <div class="user-role-list">
            <UiCheckbox
              v-for="role in roles"
              :key="role.id"
              v-model="
                roleSelectionByUser[
                  row.id
                ]
              "
              :value="Number(role.id)"
              :label="role.name"
              :disabled="
                savingUserId === row.id
              "
              @change="applyRoles(row)"
            />
          </div>
        </template>

        <template #cell-personBinding="{ row }">
          <UiSelect
            v-model="
              personSelectionByUser[
                row.id
              ]
            "
            :disabled="
              savingUserId === row.id
            "
            @change="
              applyPersonBinding(
                row,
                $event
              )
            "
          >
            <option value="">
              Без профиля
            </option>

            <option
              v-for="person in availablePeopleFor(row)"
              :key="person.id"
              :value="String(person.id)"
            >
              {{ person.lastName }}
              {{ person.firstName }}
              (#{{ person.id }})
            </option>
          </UiSelect>
        </template>
      </AdminTable>
    </UiCard>
  </AdminPageShell>
</template>

<style scoped>
.user-role-list {
  min-width: 260px;

  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-role-list :deep(.ui-checkbox) {
  min-height: auto;
  padding: 6px 9px;
}
</style>
