<script setup>
import {
  computed,
  ref,
} from 'vue'

import {
  useRouter,
} from 'vue-router'

import {
  UiAlert,
  UiButton,
} from '@/components/ui'

import {
  APP_ROLES,
} from '@/router/roles'

import {
  useAuthStore,
} from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const checking = ref(false)
const message = ref('')

const accountLabel = computed(() => {
  return (
    authStore.loginName ||
    'текущая учётная запись'
  )
})

const profileMessage = computed(() => {
  if (authStore.personId) {
    return 'Профиль уже привязан. Для входа в рабочую часть системы администратор должен назначить учётной записи роль.'
  }

  return 'Учётная запись создана. Администратор должен привязать профиль и назначить роль.'
})

function hasApplicationRole() {
  return authStore.hasAnyRole(
    ...APP_ROLES
  )
}

async function recheckAccess() {
  checking.value = true
  message.value = ''

  try {
    await authStore.refreshSession()
    await authStore.loadCurrentUser()

    if (hasApplicationRole()) {
      await router.replace({
        name: 'home',
      })

      return
    }

    message.value =
      'Роль пока не назначена. Повторите проверку после изменения учётной записи администратором.'
  } catch {
    message.value =
      'Не удалось обновить данные учётной записи.'
  } finally {
    checking.value = false
  }
}

async function logout() {
  await authStore.logout()

  await router.replace({
    name: 'login',
  })
}
</script>

<template>
  <section class="account-pending">
    <div
      class="account-pending__icon"
      aria-hidden="true"
    >
      …
    </div>

    <h1>
      Учётная запись ожидает настройки
    </h1>

    <p>
      Аккаунт
      <strong>{{ accountLabel }}</strong>
      успешно создан и авторизован.
    </p>

    <UiAlert
      variant="info"
      :message="profileMessage"
    />

    <UiAlert
      v-if="message"
      variant="warning"
      :message="message"
    />

    <div class="account-pending__actions">
      <UiButton
        variant="primary"
        :loading="checking"
        loading-text="Проверка..."
        @click="recheckAccess"
      >
        Проверить доступ
      </UiButton>

      <UiButton
        :disabled="checking"
        @click="logout"
      >
        Выйти
      </UiButton>
    </div>
  </section>
</template>

<style scoped>
.account-pending {
  width: min(100%, 620px);

  margin: 48px auto;
  padding: 28px;

  display: grid;
  justify-items: center;
  gap: 16px;

  text-align: center;

  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-elevated);
}

.account-pending__icon {
  width: 64px;
  height: 64px;

  display: grid;
  place-items: center;

  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand);
  border-radius: 50%;

  font-size: 32px;
  font-weight: 800;
}

.account-pending h1,
.account-pending p {
  margin: 0;
}

.account-pending h1 {
  color: var(--text);
  font-size: 27px;
}

.account-pending p {
  color: var(--text-secondary);
  line-height: 1.6;
}

.account-pending :deep(.ui-alert) {
  width: 100%;
  text-align: left;
}

.account-pending__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

@media (max-width: 520px) {
  .account-pending {
    margin: 20px auto;
    padding: 22px 18px;
  }

  .account-pending__actions {
    width: 100%;
    flex-direction: column;
  }

  .account-pending__actions :deep(.ui-button) {
    width: 100%;
  }
}
</style>
