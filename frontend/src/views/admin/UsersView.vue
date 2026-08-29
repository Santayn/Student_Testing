<script setup>
import { computed, onMounted, ref } from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTable from '@/components/admin/AdminTable.vue'

import {
  UiButton,
  UiCard,
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
  const person = user?.person ?? {}

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

function selectedRoleId(user) {
  return userRoleIds(user)[0] ?? ''
}

function roleName(user) {
  const roleId =
    selectedRoleId(user)

  return (
    roles.value.find(
      (role) =>
        Number(role.id) ===
        Number(roleId)
    )?.name ??
    '—'
  )
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
  },
  {
    key: 'role',
    label: 'Роль',
    value: roleName,
    sortValue: roleName,
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
    ] = await Promise.all([
      rolesApi.getAll(),
      usersApi.getAll(),
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

async function applyRole(
  user,
  event
) {
  const roleId = Number(
    event.target.value
  )

  if (!roleId) {
    return
  }

  savingUserId.value = user.id

  try {
    await usersApi.updateRoles(
      user.id,
      {
        roleIds: [roleId],
      }
    )

    showNotice(
      'success',
      `Роль пользователя ${user.login} обновлена.`
    )

    await loadData()
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось изменить роль'
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
    description="Просмотр пользователей и назначение основной роли."
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

        <template #cell-role="{ row }">
          <UiSelect
            :model-value="selectedRoleId(row)"
            :disabled="
              savingUserId === row.id
            "
            @change="
              applyRole(row, $event)
            "
          >
            <option
              value=""
              disabled
            >
              Выберите роль
            </option>

            <option
              v-for="role in roles"
              :key="role.id"
              :value="role.id"
            >
              {{ role.name }}
            </option>
          </UiSelect>
        </template>
      </AdminTable>
    </UiCard>
  </AdminPageShell>
</template>
