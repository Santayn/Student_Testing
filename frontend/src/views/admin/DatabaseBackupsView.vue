<script setup>
import { computed, ref } from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiButton,
  UiCard,
  UiFileInput,
} from '@/components/ui'

import {
  databaseBackupsApi,
  getApiErrorMessage,
} from '@/api'

const backupFile = ref(null)
const creating = ref(false)
const restoring = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const selectedFileName = computed(() => {
  return backupFile.value?.name ?? ''
})

const canRestore = computed(() => {
  return Boolean(backupFile.value) && !restoring.value
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

function backupFileName(response) {
  const disposition =
    response.headers?.['content-disposition'] ??
    response.headers?.['Content-Disposition'] ??
    ''

  const utf8Name = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Name?.[1]) {
    return decodeURIComponent(utf8Name[1].replace(/"/g, ''))
  }

  const plainName = disposition.match(/filename="?([^";]+)"?/i)
  if (plainName?.[1]) {
    return plainName[1]
  }

  const stamp = new Date()
    .toISOString()
    .replace(/[:.]/g, '-')

  return `student-test-database-backup-${stamp}.sql`
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function backupErrorMessage(error, fallback) {
  const data = error?.response?.data

  if (data instanceof Blob) {
    try {
      const text = await data.text()
      const payload = JSON.parse(text)
      return (
        payload?.message ||
        payload?.detail ||
        payload?.title ||
        fallback
      )
    } catch {
      return fallback
    }
  }

  return getApiErrorMessage(error, fallback)
}

function handleFileChange(files) {
  backupFile.value = files[0] ?? null
}

async function createBackup() {
  creating.value = true
  clearNotice()

  try {
    const response = await databaseBackupsApi.create()
    downloadBlob(response.data, backupFileName(response))
    showNotice('success', 'Резервная копия сформирована и скачана.')
  } catch (error) {
    showNotice(
      'error',
      await backupErrorMessage(
        error,
        'Не удалось создать резервную копию'
      )
    )
  } finally {
    creating.value = false
  }
}

async function restoreBackup() {
  if (!backupFile.value) {
    showNotice('warning', 'Выберите SQL-файл резервной копии.')
    return
  }

  const confirmed = window.confirm(
    'Восстановление заменит текущие данные базы. Продолжить?'
  )

  if (!confirmed) {
    return
  }

  restoring.value = true
  clearNotice()

  try {
    await databaseBackupsApi.restore(backupFile.value)
    showNotice('success', 'Резервная копия загружена и восстановлена.')
  } catch (error) {
    showNotice(
      'error',
      await backupErrorMessage(
        error,
        'Не удалось восстановить базу из резервной копии'
      )
    )
  } finally {
    restoring.value = false
  }
}
</script>

<template>
  <AdminPageShell
    title="Резервные копии"
    description="Скачивание и загрузка полной SQL-копии базы данных."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <section class="admin-grid admin-grid--2">
      <UiCard>
        <div class="admin-card__header">
          <div>
            <h2>Скачать копию</h2>
            <p>
              Файл содержит схему и данные приложения.
            </p>
          </div>
        </div>

        <div class="backup-actions">
          <UiButton
            variant="primary"
            type="button"
            :loading="creating"
            loading-text="Создание..."
            @click="createBackup"
          >
            Скачать SQL-копию
          </UiButton>
        </div>
      </UiCard>

      <UiCard>
        <div class="admin-card__header">
          <div>
            <h2>Восстановить из копии</h2>
            <p>
              Загрузите SQL-файл и подтвердите восстановление.
            </p>
          </div>
        </div>

        <form
          class="backup-form"
          @submit.prevent="restoreBackup"
        >
          <UiFileInput
            accept=".sql,application/sql,text/plain"
            label="Файл резервной копии"
            hint="Поддерживается SQL-файл из этого раздела."
            :disabled="restoring"
            @files-change="handleFileChange"
          />

          <div
            v-if="selectedFileName"
            class="admin-muted"
          >
            Выбран файл: {{ selectedFileName }}
          </div>

          <div class="admin-actions">
            <UiButton
              variant="danger"
              type="submit"
              :disabled="!canRestore"
              :loading="restoring"
              loading-text="Восстановление..."
            >
              Загрузить и восстановить
            </UiButton>
          </div>
        </form>
      </UiCard>
    </section>
  </AdminPageShell>
</template>

<style scoped>
.backup-actions,
.backup-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
</style>
