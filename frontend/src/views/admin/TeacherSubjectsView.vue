<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiLoadingState,
  UiFilterBar,
  UiSelect,
  UiTag,
  UiTextarea,
} from '@/components/ui'

import {
  getApiErrorMessage,
  membershipsApi,
} from '@/api'

import {
  assignSubjectToTeacher,
} from '@/utils/teacherSubjectAssignment'

import {
  useAdminTeacherSubjectsData,
} from '@/composables/admin/teacher-subjects/useAdminTeacherSubjectsData'

const REMOVED_STATUS = 3

const notes = ref('')
const mutatingSubjectId = ref(null)

const removeTarget = ref(null)
const removeConfirmVisible = ref(false)
const removeError = ref('')

const {
  sortOptions,
  teachers,
  subjects,
  memberships,
  teacherId,
  searchQuery,
  sortMode,
  loadingBase,
  loadingMemberships,
  loading,
  notice,
  teacherOptions,
  selectedTeacher,
  pausedTeacherMemberships,
  assignedSubjects,
  availableSubjects,
  hasActiveFilters,
  filteredAssignedSubjects,
  filteredAvailableSubjects,
  filterResultText,
  showNotice,
  clearNotice,
  personName,
  resetFilters,
  loadBaseData,
  loadMemberships,
} = useAdminTeacherSubjectsData()

const saving = computed(() => {
  return mutatingSubjectId.value !== null
})

function relationErrorMessage(
  error,
  fallback
) {
  const message = getApiErrorMessage(
    error,
    fallback
  )

  const normalized =
    message.toLowerCase()

  if (
    normalized.includes(
      'active subject membership already exists'
    )
  ) {
    return (
      'Этот предмет уже назначен преподавателю. ' +
      'Обновите страницу и повторите действие.'
    )
  }

  return message
}

function closeRemoveConfirm() {
  if (saving.value) {
    return
  }

  removeConfirmVisible.value = false
  removeTarget.value = null
  removeError.value = ''
}

function requestRemoveSubject(subject) {
  removeTarget.value = subject
  removeError.value = ''
  removeConfirmVisible.value = true
}

async function addSubject(subject) {
  const targetTeacherId =
    Number(teacherId.value)
  const subjectId =
    Number(subject?.id)

  if (
    saving.value ||
    !Number.isInteger(
      targetTeacherId
    ) ||
    targetTeacherId <= 0 ||
    !Number.isInteger(subjectId) ||
    subjectId <= 0
  ) {
    return
  }

  mutatingSubjectId.value = subjectId
  clearNotice()

  try {
    const result =
      await assignSubjectToTeacher({
        api: membershipsApi,
        memberships:
          memberships.value,
        personId: targetTeacherId,
        subjectId,
        notes: notes.value,
      })

    if (
      Number(teacherId.value) !==
      targetTeacherId
    ) {
      await loadMemberships()
      return
    }

    showNotice(
      'success',
      result.action === 'reactivated'
        ? `Назначение предмета «${subject.name}» восстановлено.`
        : `Предмет «${subject.name}» назначен преподавателю.`
    )

    notes.value = ''
    await loadMemberships()
  } catch (error) {
    if (
      Number(teacherId.value) !==
      targetTeacherId
    ) {
      return
    }

    showNotice(
      'error',
      relationErrorMessage(
        error,
        'Не удалось назначить предмет преподавателю.'
      )
    )
  } finally {
    if (
      mutatingSubjectId.value ===
      subjectId
    ) {
      mutatingSubjectId.value = null
    }
  }
}

async function removeSubject() {
  const targetTeacherId =
    Number(teacherId.value)
  const target = removeTarget.value
  const membershipId = Number(
    target?.membership?.id
  )
  const subjectId = Number(
    target?.id
  )

  if (
    saving.value ||
    !Number.isInteger(
      targetTeacherId
    ) ||
    targetTeacherId <= 0 ||
    !Number.isInteger(
      membershipId
    ) ||
    membershipId <= 0 ||
    !Number.isInteger(subjectId) ||
    subjectId <= 0
  ) {
    return
  }

  mutatingSubjectId.value = subjectId
  removeError.value = ''
  clearNotice()

  try {
    await membershipsApi
      .updateSubjectMembershipStatus(
        membershipId,
        {
          status: REMOVED_STATUS,
        }
      )

    if (
      Number(teacherId.value) !==
      targetTeacherId
    ) {
      await loadMemberships()
      return
    }

    removeConfirmVisible.value = false
    removeTarget.value = null

    showNotice(
      'success',
      `Предмет «${target.name}» снят с преподавателя.`
    )

    await loadMemberships()
  } catch (error) {
    if (
      Number(teacherId.value) !==
      targetTeacherId
    ) {
      return
    }

    removeError.value =
      relationErrorMessage(
        error,
        'Не удалось снять предмет с преподавателя.'
      )
  } finally {
    if (
      mutatingSubjectId.value ===
      subjectId
    ) {
      mutatingSubjectId.value = null
    }
  }
}

watch(
  teacherId,
  () => {
    closeRemoveConfirm()
    clearNotice()
  }
)

onMounted(async () => {
  await Promise.all([
    loadBaseData(),
    loadMemberships(),
  ])
})
</script>

<template>
  <AdminPageShell
    title="Преподаватели и предметы"
    description="Настраивайте предметы, которые закреплены за выбранным преподавателем."
  >
    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Преподаватель"
      description="Выберите преподавателя, для которого нужно настроить список предметов."
    >
      <label class="teacher-subjects__teacher-field">
        <span>Преподаватель</span>

        <UiSelect
          v-model="teacherId"
          :disabled="loading || saving"
          :options="teacherOptions"
          option-label="label"
          option-value="value"
          placeholder="Выберите преподавателя"
          :filter="true"
          filter-placeholder="Поиск по ФИО или email"
        />
      </label>

      <div
        v-if="selectedTeacher"
        class="teacher-subjects__context"
      >
        <UiTag
          :value="personName(selectedTeacher)"
        />

        <UiTag
          variant="info"
          :value="`Назначено предметов: ${assignedSubjects.length}`"
        />

        <UiTag
          v-if="pausedTeacherMemberships.length"
          variant="warning"
          :value="`Приостановлено: ${pausedTeacherMemberships.length}`"
        />
      </div>

      <UiEmptyState
        v-else-if="!loadingBase && !teachers.length"
        description="Преподаватели ещё не созданы или им не назначена соответствующая роль."
        compact
      />
    </UiCard>

    <template v-if="selectedTeacher">
      <UiFilterBar
        v-model="searchQuery"
        aria-label="Фильтры предметов преподавателя"
        search-placeholder="Поиск по названию, описанию или примечанию"
        :result-text="filterResultText"
        :reset-disabled="!hasActiveFilters"
        @reset="resetFilters"
      >
        <template #filters>
          <UiSelect
            v-model="sortMode"
            :options="sortOptions"
            option-label="label"
            option-value="value"
            aria-label="Сортировка предметов"
          />
        </template>
      </UiFilterBar>

      <div class="teacher-subjects__columns">
        <UiCard
          title="Назначенные предметы"
          description="Активные предметы выбранного преподавателя."
        >
          <template #actions>
            <UiTag variant="info" :value="`Назначено · ${assignedSubjects.length}`" />
          </template>
          <UiLoadingState
            v-if="loadingMemberships"
            compact
            label="Загрузка назначений..."
          />

          <UiEmptyState
            v-else-if="!assignedSubjects.length"
            description="У преподавателя пока нет назначенных предметов."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredAssignedSubjects.length"
            description="Среди назначенных предметов ничего не найдено."
            compact
          />

          <div
            v-else
            class="teacher-subjects__list"
          >
            <article
              v-for="subject in filteredAssignedSubjects"
              :key="subject.membership.id"
              class="teacher-subjects__item"
            >
              <div class="teacher-subjects__item-copy">
                <div class="teacher-subjects__item-heading">
                  <h3>{{ subject.name }}</h3>
                  <UiTag
                    value="Активно"
                    variant="success"
                  />
                </div>

                <p>
                  {{
                    subject.description ||
                    'Описание предмета не указано.'
                  }}
                </p>

                <p
                  v-if="subject.membership.notes"
                  class="teacher-subjects__note"
                >
                  Примечание: {{ subject.membership.notes }}
                </p>
              </div>

              <UiButton
                variant="secondary"
                size="sm"
                label="Снять с преподавателя"
                icon="pi pi-times"
                :loading="mutatingSubjectId === Number(subject.id)"
                loading-text="Снятие..."
                :disabled="saving"
                @click="requestRemoveSubject(subject)"
              />
            </article>
          </div>
        </UiCard>

        <UiCard
          title="Доступные предметы"
          description="Предметы, которые можно назначить или восстановить преподавателю."
        >
          <template #actions>
            <UiTag variant="secondary" :value="`Доступно · ${availableSubjects.length}`" />
          </template>
          <label class="teacher-subjects__notes-field">
            <span>Примечание к новому назначению</span>

            <UiTextarea
              v-model="notes"
              :disabled="saving"
              maxlength="1000"
              placeholder="Необязательно. Применяется к следующему добавляемому предмету."
            />
          </label>

          <UiLoadingState
            v-if="loadingMemberships"
            compact
            label="Загрузка доступных предметов..."
          />

          <UiEmptyState
            v-else-if="!subjects.length"
            description="В справочнике пока нет предметов."
            compact
          />

          <UiEmptyState
            v-else-if="!availableSubjects.length"
            description="Все предметы уже назначены этому преподавателю."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredAvailableSubjects.length"
            description="Среди доступных предметов ничего не найдено."
            compact
          />

          <div
            v-else
            class="teacher-subjects__list"
          >
            <article
              v-for="subject in filteredAvailableSubjects"
              :key="subject.id"
              class="teacher-subjects__item"
            >
              <div class="teacher-subjects__item-copy">
                <div class="teacher-subjects__item-heading">
                  <h3>{{ subject.name }}</h3>

                  <UiTag
                    v-if="subject.pausedMembership"
                    value="Приостановлено"
                    variant="warning"
                  />
                </div>

                <p>
                  {{
                    subject.description ||
                    'Описание предмета не указано.'
                  }}
                </p>

                <p
                  v-if="subject.pausedMembership?.notes"
                  class="teacher-subjects__note"
                >
                  Сохранённое примечание: {{ subject.pausedMembership.notes }}
                </p>
              </div>

              <UiButton
                variant="primary"
                size="sm"
                :label="subject.pausedMembership ? 'Восстановить' : 'Добавить'"
                :icon="subject.pausedMembership ? 'pi pi-refresh' : 'pi pi-plus'"
                :loading="mutatingSubjectId === Number(subject.id)"
                :loading-text="subject.pausedMembership ? 'Восстановление...' : 'Добавление...'"
                :disabled="saving"
                @click="addSubject(subject)"
              />
            </article>
          </div>
        </UiCard>
      </div>
    </template>

    <UiDialog
      v-model="removeConfirmVisible"
      title="Снять предмет с преподавателя?"
      width="30rem"
      :closable="!saving"
      :close-on-escape="!saving"
      @update:model-value="(visible) => {
        if (!visible) closeRemoveConfirm()
      }"
    >
      <div class="teacher-subjects__confirm">
        <p>
          Предмет
          <strong>«{{ removeTarget?.name }}»</strong>
          перестанет быть активным назначением преподавателя
          <strong>«{{ personName(selectedTeacher) }}»</strong>.
        </p>

        <UiAlert
          variant="warning"
          message="Связь будет снята, но её история сохранится. При необходимости назначение можно будет восстановить позже."
        />

        <UiAlert
          v-if="removeError"
          variant="danger"
          :message="removeError"
        />
      </div>

      <template #footer>
        <div class="teacher-subjects__dialog-actions">
          <UiButton
            variant="ghost"
            label="Отмена"
            :disabled="saving"
            @click="closeRemoveConfirm"
          />

          <UiButton
            variant="danger"
            label="Снять предмет"
            icon="pi pi-times"
            :loading="saving"
            loading-text="Снятие..."
            @click="removeSubject"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.teacher-subjects__teacher-field,
.teacher-subjects__notes-field {
  display: grid;
  gap: 7px;
}

.teacher-subjects__teacher-field {
  max-width: 520px;
}

.teacher-subjects__teacher-field > span,
.teacher-subjects__notes-field > span {
  color: var(--st-text);

  font-size: 13px;
  font-weight: var(--st-font-weight-bold);
}

.teacher-subjects__context {
  margin-top: 12px;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-subjects__columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.teacher-subjects__notes-field {
  margin-bottom: 14px;
}

.teacher-subjects__list {
  display: grid;
  gap: 10px;
}

.teacher-subjects__item {
  min-width: 0;
  padding: 14px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.teacher-subjects__item-copy {
  min-width: 0;
}

.teacher-subjects__item-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-subjects__item h3,
.teacher-subjects__item p,
.teacher-subjects__confirm p {
  margin: 0;
}

.teacher-subjects__item h3 {
  color: var(--st-text);

  font-size: 15px;
  line-height: 1.35;
}

.teacher-subjects__item p {
  margin-top: 5px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.teacher-subjects__item .teacher-subjects__note {
  color: var(--st-text);
}

.teacher-subjects__confirm {
  display: grid;
  gap: 14px;
}

.teacher-subjects__dialog-actions {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .teacher-subjects__columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .teacher-subjects__item {
    align-items: stretch;
    flex-direction: column;
  }

  .teacher-subjects__item > :deep(.ui-button) {
    width: 100%;
  }

  .teacher-subjects__dialog-actions,
  .teacher-subjects__dialog-actions > :deep(.ui-button) {
    width: 100%;
  }
}
</style>
