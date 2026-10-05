<script setup>
import {
  computed,
  ref,
} from 'vue'

import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiFileInput,
} from '@/components/ui'

import {
  useDatabaseBackupDownload,
} from '@/composables/admin/database-backups/useDatabaseBackupDownload'

import {
  useDatabaseRestore,
} from '@/composables/admin/database-backups/useDatabaseRestore'

import {
  DATABASE_BACKUP_FILE_ACCEPT,
  formatDatabaseBackupBytes,
  formatDatabaseRestoreTime,
} from '@/utils/databaseBackup'

import {
  focusFirstInvalidField,
} from '@/utils/formErrorLifecycle'

const backupFileAccept =
  DATABASE_BACKUP_FILE_ACCEPT

const restoreFormElement = ref(null)
const restoreConfirmVisible = ref(false)

const {
  creating,
  errorMessage: createErrorMessage,
  lastFileName,
  createBackup: createBackupFile,
  clearFailure: clearCreateFailure,
} = useDatabaseBackupDownload()

const {
  file: backupFile,
  fileError,
  restoring,
  result: restoreResult,
  selectedFileName,
  selectedFileSizeBytes,
  canRestore,
  errorMessage: restoreErrorMessage,
  setFiles,
  restoreBackup: restoreBackupFile,
  clearFailure: clearRestoreFailure,
} = useDatabaseRestore()

const selectedFileSize = computed(
  () =>
    formatDatabaseBackupBytes(
      selectedFileSizeBytes.value
    )
)

const restoredFileSize = computed(
  () =>
    formatDatabaseBackupBytes(
      restoreResult.value?.sizeBytes
    )
)

const restoredAt = computed(
  () =>
    formatDatabaseRestoreTime(
      restoreResult.value?.restoredAtUtc
    )
)

const operationsBusy = computed(
  () =>
    creating.value ||
    restoring.value
)

async function handleFileChange(files) {
  restoreConfirmVisible.value = false
  clearRestoreFailure()

  const validation =
    setFiles(files)

  if (!validation.valid) {
    await focusFirstInvalidField(
      restoreFormElement.value
    )
  }
}

async function createBackup() {
  if (operationsBusy.value) {
    return
  }

  clearCreateFailure()
  await createBackupFile()
}

async function requestRestore() {
  if (
    operationsBusy.value ||
    !backupFile.value ||
    fileError.value
  ) {
    await focusFirstInvalidField(
      restoreFormElement.value
    )
    return
  }

  restoreConfirmVisible.value = true
}

function closeRestoreConfirmation() {
  if (restoring.value) {
    return
  }

  restoreConfirmVisible.value = false
}

async function confirmRestore() {
  if (
    restoring.value ||
    !canRestore.value
  ) {
    return
  }

  clearRestoreFailure()

  const result =
    await restoreBackupFile()

  if (!result) {
    return
  }

  restoreConfirmVisible.value = false
}

function reloadApplication() {
  globalThis.location?.reload?.()
}
</script>

<template>
  <AdminPageShell
    title="Резервные копии"
    description="Создавайте полную SQL-копию базы данных и восстанавливайте состояние системы из ранее сохранённого файла."
  >
    <div class="database-backups-workspace">
      <UiCard
        title="Создание резервной копии"
        description="Сформируйте полную SQL-копию схемы и данных. Файл будет скачан на это устройство."
      >
        <div class="database-backups-card">
          <UiAlert
            v-if="createErrorMessage"
            variant="danger"
            title="Не удалось создать резервную копию"
            :message="createErrorMessage"
            closable
            @close="clearCreateFailure"
          />

          <UiAlert
            v-else-if="lastFileName"
            variant="success"
            title="Резервная копия создана"
            :message="`Файл «${lastFileName}» сформирован и передан браузеру для скачивания.`"
          />

          <div class="database-backups-info-grid">
            <div class="database-backups-info">
              <span>Формат</span>
              <strong>SQL</strong>
            </div>

            <div class="database-backups-info">
              <span>Содержимое</span>
              <strong>Схема и данные</strong>
            </div>
          </div>

          <div class="database-backups-actions">
            <UiButton
              variant="primary"
              icon="pi pi-download"
              label="Скачать резервную копию"
              :loading="creating"
              loading-text="Создание копии..."
              :disabled="restoring"
              @click="createBackup"
            />
          </div>
        </div>
      </UiCard>

      <section
        class="database-backups-danger-zone"
        aria-labelledby="database-restore-title"
      >
        <div class="database-backups-danger-zone__heading">
          <div>
            <p class="database-backups-danger-zone__eyebrow">
              Опасная операция
            </p>

            <h2 id="database-restore-title">
              Восстановление базы данных
            </h2>

            <p>
              Восстановление заменяет текущее состояние базы данными из SQL-файла. После завершения текущие данные интерфейса могут стать устаревшими.
            </p>
          </div>
        </div>

        <UiAlert
          variant="warning"
          title="Перед восстановлением"
          message="Убедитесь, что выбран нужный файл. Во время восстановления не запускайте другие административные операции."
        />

        <form
          ref="restoreFormElement"
          class="database-backups-restore-form"
          @submit.prevent="requestRestore"
        >
          <UiFileInput
            id="database-backup-restore-file"
            :accept="backupFileAccept"
            label="SQL-файл резервной копии"
            hint="Поддерживаются файлы .sql размером до 50 МБ."
            :error="fileError"
            :disabled="operationsBusy"
            required
            @files-change="handleFileChange"
          />

          <div
            v-if="selectedFileName"
            class="database-backups-selected-file"
          >
            <div>
              <span>Выбранный файл</span>
              <strong>{{ selectedFileName }}</strong>
            </div>

            <span v-if="selectedFileSize">
              {{ selectedFileSize }}
            </span>
          </div>

          <UiAlert
            v-if="restoreErrorMessage"
            variant="danger"
            title="Не удалось восстановить базу"
            :message="restoreErrorMessage"
            closable
            @close="clearRestoreFailure"
          />

          <UiAlert
            v-if="restoreResult"
            variant="success"
            title="База данных восстановлена"
          >
            <div class="database-backups-result">
              <span>
                Файл:
                <strong>
                  {{ restoreResult.fileName || selectedFileName }}
                </strong>
              </span>

              <span v-if="restoredFileSize">
                Размер:
                <strong>{{ restoredFileSize }}</strong>
              </span>

              <span v-if="restoredAt">
                Восстановлено:
                <strong>{{ restoredAt }}</strong>
              </span>

              <p>
                Данные приложения были заменены. Перезагрузите интерфейс, чтобы заново получить пользователя, роли и актуальные данные из восстановленной базы.
              </p>
            </div>
          </UiAlert>

          <div class="database-backups-actions database-backups-actions--restore">
            <UiButton
              v-if="restoreResult"
              variant="primary"
              icon="pi pi-refresh"
              label="Перезагрузить приложение"
              :disabled="operationsBusy"
              @click="reloadApplication"
            />

            <UiButton
              v-else
              type="submit"
              variant="danger"
              icon="pi pi-history"
              label="Восстановить базу"
              :disabled="!canRestore || creating"
              :loading="restoring"
              loading-text="Восстановление..."
            />
          </div>
        </form>
      </section>
    </div>

    <UiDialog
      v-model="restoreConfirmVisible"
      title="Восстановить базу данных?"
      width="34rem"
      :closable="!restoring"
      :close-on-escape="!restoring"
      :dismissable-mask="false"
    >
      <div class="database-backups-confirm">
        <UiAlert
          variant="warning"
          message="Текущее состояние базы будет заменено содержимым выбранной резервной копии."
        />

        <dl class="database-backups-confirm__details">
          <div>
            <dt>Файл</dt>
            <dd>{{ selectedFileName }}</dd>
          </div>

          <div v-if="selectedFileSize">
            <dt>Размер</dt>
            <dd>{{ selectedFileSize }}</dd>
          </div>
        </dl>
      </div>

      <template #footer>
        <div class="database-backups-dialog-actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="restoring"
            @click="closeRestoreConfirmation"
          />

          <UiButton
            variant="danger"
            label="Восстановить базу"
            :loading="restoring"
            loading-text="Восстановление..."
            @click="confirmRestore"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.database-backups-workspace,
.database-backups-card,
.database-backups-restore-form,
.database-backups-confirm,
.database-backups-result {
  display: grid;
  gap: 14px;
}

.database-backups-info-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.database-backups-info {
  min-width: 0;
  padding: 12px;

  display: grid;
  gap: 4px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.database-backups-info span,
.database-backups-selected-file span,
.database-backups-confirm dt {
  color: var(--st-text-secondary);
  font-size: 12px;
}

.database-backups-info strong,
.database-backups-selected-file strong,
.database-backups-confirm dd {
  min-width: 0;
  color: var(--st-text);
  overflow-wrap: anywhere;
}

.database-backups-actions,
.database-backups-dialog-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.database-backups-danger-zone {
  min-width: 0;
  padding: 18px;

  display: grid;
  gap: 16px;

  background:
    color-mix(
      in srgb,
      var(--st-danger) 4%,
      var(--st-surface)
    );
  border:
    1px solid
    color-mix(
      in srgb,
      var(--st-danger) 35%,
      var(--st-border)
    );
  border-radius: 14px;
}

.database-backups-danger-zone__heading h2,
.database-backups-danger-zone__heading p {
  margin: 0;
}

.database-backups-danger-zone__heading h2 {
  font-size: 20px;
}

.database-backups-danger-zone__heading > div {
  display: grid;
  gap: 7px;
}

.database-backups-danger-zone__heading p:last-child {
  max-width: 820px;
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.database-backups-danger-zone__eyebrow {
  color: var(--st-danger);
  font-size: 11px;
  font-weight: var(--st-font-weight-bold);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.database-backups-selected-file {
  padding: 12px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.database-backups-selected-file > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.database-backups-result span {
  display: block;
  color: var(--st-text-secondary);
}

.database-backups-result p {
  margin: 2px 0 0;
  line-height: 1.55;
}

.database-backups-confirm__details {
  margin: 0;
  display: grid;
  gap: 10px;
}

.database-backups-confirm__details > div {
  padding: 10px 12px;

  display: grid;
  gap: 3px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.database-backups-confirm dd {
  margin: 0;
}

@media (max-width: 640px) {
  .database-backups-danger-zone {
    padding: 14px;
  }

  .database-backups-selected-file {
    align-items: flex-start;
    flex-direction: column;
  }

  .database-backups-actions,
  .database-backups-dialog-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .database-backups-actions > *,
  .database-backups-dialog-actions > * {
    width: 100%;
  }
}
</style>
