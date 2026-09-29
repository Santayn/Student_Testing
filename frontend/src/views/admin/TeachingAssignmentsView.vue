<script setup>
import {
  onMounted,
  watch,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'
import AdminTeachingAssignmentDrawer from '@/components/admin/AdminTeachingAssignmentDrawer.vue'
import AdminLoadTypeManager from '@/components/admin/AdminLoadTypeManager.vue'

import {
  UiButton,
  UiCard,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiUnsavedChangesConfirm,
} from '@/components/ui'

import {
  ADMIN_TEACHING_COURSE_OPTIONS as COURSE_OPTIONS,
  ADMIN_TEACHING_SEMESTER_OPTIONS as SEMESTER_OPTIONS,
  ADMIN_TEACHING_SORT_OPTIONS as SORT_OPTIONS,
  ADMIN_TEACHING_STATUS_LABELS as STATUS_LABELS,
  useAdminTeachingAssignmentsData,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentsData'

import {
  useAdminTeachingAssignmentEditor,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentEditor'

import {
  useAdminTeachingAssignmentMutations,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentMutations'

import {
  useAdminLoadTypeManager,
} from '@/composables/admin/teaching-assignments/useAdminLoadTypeManager'


const {
  faculties,
  facultySubjects,
  groups,
  people,
  teacherMemberships,
  assignments,
  loadTypes,
  loadingBase,
  loadingContext,
  loadingAssignments,
  loading,
  initialized,
  context,
  searchQuery,
  subjectFilter,
  teacherFilter,
  groupFilter,
  loadTypeFilter,
  statusFilter,
  sortMode,
  notice,
  selectedFaculty,
  facultyOptions,
  subjectOptions,
  groupOptions,
  loadTypeOptions,
  assignmentSubjectFilterOptions,
  assignmentTeacherFilterOptions,
  assignmentGroupFilterOptions,
  assignmentLoadTypeFilterOptions,
  assignmentStatusFilterOptions,
  summary,
  hasActiveFilters,
  filteredAssignments,
  filterResultText,
  showNotice,
  clearNotice,
  resetFilters,
  personById,
  personLabel,
  teacherMembershipLabel,
  subjectName,
  groupName,
  membershipById,
  loadTypeName,
  assignmentSubjectName,
  teacherNameForAssignment,
  statusVariant,
  formatHours,
  assignmentPeriodLabel,
  reloadLoadTypes,
  loadBaseData,
  loadFacultyContext,
  refreshAssignments,
  reloadAll,
} = useAdminTeachingAssignmentsData()

const {
  assignmentForm,
  assignmentDrawerModel,
  assignmentIsCreate,
  assignmentSaving,
  assignmentCloseConfirmVisible,
  assignmentFormError,
  groupSearchQuery,
  loadingCourseVersions,
  assignmentDrawerTitle,
  teacherOptionsForForm,
  filteredGroupsForCreate,
  courseVersionOptions,
  groupHasConflict,
  groupConflictDescription,
  assignmentValidationMessage,
  openCreateAssignment,
  openEditAssignment,
  onAssignmentSubjectChange,
  onAssignmentTeacherChange,
  assignmentPayload,
  requestCloseAssignmentDrawer,
  discardAssignmentAndClose,
  continueAssignmentEditing,
  beginAssignmentSaving,
  finishAssignmentSaving,
  failAssignmentSaving,
} = useAdminTeachingAssignmentEditor({
  context,
  assignments,
  groups,
  teacherMemberships,
  loadTypes,
  membershipById,
  teacherMembershipLabel,
})

const {
  loadTypeForm,
  loadTypeDialogModel,
  loadTypeIsCreate,
  loadTypeDirty,
  loadTypeSaving,
  loadTypeCloseConfirmVisible,
  loadTypeFormError,
  loadTypeSearchQuery,
  loadTypeDialogTitle,
  loadTypeEditorTitle,
  filteredLoadTypes,
  openLoadTypeManager,
  startNewLoadType,
  editLoadType,
  cancelLoadTypeChanges,
  saveLoadType,
  continueLoadTypeEditing,
  discardLoadTypeAndClose,
} = useAdminLoadTypeManager({
  loadTypes,
  reloadLoadTypes,
  showNotice,
})

const {
  saveAssignment,
} = useAdminTeachingAssignmentMutations({
  assignmentForm,
  assignmentIsCreate,
  assignmentSaving,
  assignmentFormError,
  assignmentValidationMessage,
  assignmentPayload,
  beginAssignmentSaving,
  finishAssignmentSaving,
  failAssignmentSaving,
  assignments,
  groups,
  groupName,
  context,
  refreshAssignments,
  showNotice,
})

watch(
  () => context.facultyId,
  () => {
    if (!initialized.value) {
      return
    }

    loadFacultyContext()
  }
)

watch(
  [
    () => context.studyCourse,
    () => context.semester,
    () => context.academicYear,
  ],
  () => {
    if (
      !initialized.value ||
      !context.facultyId
    ) {
      return
    }

    refreshAssignments()
  }
)

onMounted(async () => {
  await loadBaseData()
  initialized.value = true

  if (context.facultyId) {
    await loadFacultyContext()
  }
})
</script>

<template>
  <AdminPageShell
    title="Учебная нагрузка"
    description="Настраивайте назначения преподавателей на группы: предмет, тип нагрузки, часы, учебный период и статус."
  >
    <template #actions>
      <UiButton
        variant="secondary"
        icon="pi pi-tags"
        label="Типы нагрузки"
        :disabled="loading || assignmentDrawerModel"
        @click="openLoadTypeManager"
      />

      <UiButton
        variant="secondary"
        icon="pi pi-refresh"
        label="Обновить"
        :disabled="loading || assignmentDrawerModel"
        @click="reloadAll"
      />

      <UiButton
        variant="primary"
        icon="pi pi-plus"
        label="Добавить нагрузку"
        :disabled="
          loading ||
          !selectedFaculty ||
          !facultySubjects.length ||
          !groups.length
        "
        @click="openCreateAssignment"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Учебный период"
      description="Контекст ограничивает список нагрузки. Период конкретного назначения можно изменить в боковой панели."
    >
      <div class="admin-form-grid admin-form-grid--4">
        <UiSelect
          v-model="context.facultyId"
          label="Факультет"
          :options="facultyOptions"
          placeholder="Выберите факультет"
          :filter="true"
          filter-placeholder="Поиск факультета"
          :disabled="loadingBase || assignmentDrawerModel"
        />

        <UiSelect
          v-model="context.studyCourse"
          label="Курс"
          :options="COURSE_OPTIONS"
          :disabled="assignmentDrawerModel"
        />

        <UiSelect
          v-model="context.semester"
          label="Семестр"
          :options="SEMESTER_OPTIONS"
          :disabled="assignmentDrawerModel"
        />

        <UiInput
          v-model="context.academicYear"
          label="Учебный год"
          type="number"
          min="2000"
          step="1"
          placeholder="2026"
          :disabled="assignmentDrawerModel"
        />
      </div>
    </UiCard>

    <section class="admin-summary workload-summary">
      <div class="admin-stat">
        <span class="admin-stat__label">
          Назначений
        </span>
        <strong class="admin-stat__value">
          {{ summary.assignments }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных
        </span>
        <strong class="admin-stat__value">
          {{ summary.active }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных часов / нед.
        </span>
        <strong class="admin-stat__value">
          {{ formatHours(summary.hours) }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Преподавателей
        </span>
        <strong class="admin-stat__value">
          {{ summary.teachers }}
        </strong>
      </div>
    </section>

    <UiCard
      title="Назначения"
      :description="selectedFaculty ? `Факультет: ${selectedFaculty.name}.` : 'Выберите факультет.'"
    >
      <div class="workload-stack">
        <UiFilterBar
          v-model="searchQuery"
          aria-label="Фильтры учебной нагрузки"
          search-placeholder="Предмет, преподаватель, группа, тип или примечание"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="subjectFilter"
              :options="assignmentSubjectFilterOptions"
              aria-label="Фильтр по предмету"
              size="sm"
            />

            <UiSelect
              v-model="teacherFilter"
              :options="assignmentTeacherFilterOptions"
              aria-label="Фильтр по преподавателю"
              :filter="true"
              filter-placeholder="Поиск преподавателя"
              size="sm"
            />

            <UiSelect
              v-model="groupFilter"
              :options="assignmentGroupFilterOptions"
              aria-label="Фильтр по группе"
              :filter="true"
              filter-placeholder="Поиск группы"
              size="sm"
            />

            <UiSelect
              v-model="loadTypeFilter"
              :options="assignmentLoadTypeFilterOptions"
              aria-label="Фильтр по типу нагрузки"
              size="sm"
            />

            <UiSelect
              v-model="statusFilter"
              :options="assignmentStatusFilterOptions"
              aria-label="Фильтр по статусу"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              :options="SORT_OPTIONS"
              aria-label="Сортировка нагрузки"
              size="sm"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loadingAssignments || loadingContext"
          description="Загрузка учебной нагрузки..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedFaculty"
          description="Выберите факультет, чтобы увидеть учебную нагрузку."
          compact
        />

        <UiEmptyState
          v-else-if="!assignments.length"
          description="Для выбранного факультета и периода нагрузка ещё не назначена."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredAssignments.length"
          description="По текущим фильтрам назначения не найдены."
          compact
        />

        <div
          v-else
          class="workload-grid"
        >
          <UiCard
            v-for="assignment in filteredAssignments"
            :key="assignment.id"
            compact
          >
            <article class="workload-assignment">
              <div class="workload-assignment__header">
                <div class="workload-assignment__heading">
                  <h3>
                    {{ assignmentSubjectName(assignment) }}
                  </h3>

                  <p>
                    {{ teacherNameForAssignment(assignment) }}
                  </p>
                </div>

                <div class="workload-assignment__tags">
                  <UiTag
                    :variant="statusVariant(assignment.status)"
                    :value="STATUS_LABELS[Number(assignment.status)] ?? 'Статус'"
                  />

                  <UiTag
                    variant="info"
                    :value="loadTypeName(assignment.loadTypeId)"
                  />
                </div>
              </div>

              <dl class="workload-assignment__meta">
                <div>
                  <dt>Группа</dt>
                  <dd>{{ groupName(assignment.groupId) }}</dd>
                </div>

                <div>
                  <dt>Часы в неделю</dt>
                  <dd>{{ formatHours(assignment.hoursPerWeek) }}</dd>
                </div>

                <div>
                  <dt>Период</dt>
                  <dd>{{ assignmentPeriodLabel(assignment) }}</dd>
                </div>

                <div>
                  <dt>Версия курса</dt>
                  <dd>
                    {{ assignment.courseVersionId ? 'Привязана' : 'Не выбрана' }}
                  </dd>
                </div>
              </dl>

              <p
                v-if="assignment.notes"
                class="workload-assignment__notes"
              >
                {{ assignment.notes }}
              </p>

              <div class="workload-assignment__actions">
                <UiButton
                  variant="secondary"
                  size="sm"
                  icon="pi pi-pencil"
                  label="Изменить"
                  @click="openEditAssignment(assignment)"
                />
              </div>
            </article>
          </UiCard>
        </div>
      </div>
    </UiCard>

    <AdminTeachingAssignmentDrawer
      v-model="assignmentDrawerModel"
      :title="assignmentDrawerTitle"
      :form="assignmentForm"
      :error="assignmentFormError"
      :load-types="loadTypes"
      :subject-options="subjectOptions"
      :teacher-options="teacherOptionsForForm"
      :load-type-options="loadTypeOptions"
      :course-version-options="courseVersionOptions"
      :group-options="groupOptions"
      :filtered-groups="filteredGroupsForCreate"
      :group-search-query="groupSearchQuery"
      :saving="assignmentSaving"
      :loading-course-versions="loadingCourseVersions"
      :is-create="assignmentIsCreate"
      :group-has-conflict="groupHasConflict"
      :group-conflict-description="groupConflictDescription"
      @update:group-search-query="groupSearchQuery = $event"
      @subject-change="onAssignmentSubjectChange"
      @teacher-change="onAssignmentTeacherChange"
      @request-close="requestCloseAssignmentDrawer"
      @save="saveAssignment"
    />

    <UiUnsavedChangesConfirm
      v-model="assignmentCloseConfirmVisible"
      :busy="assignmentSaving"
      @continue="continueAssignmentEditing"
      @discard="discardAssignmentAndClose"
    />

    <AdminLoadTypeManager
      v-model="loadTypeDialogModel"
      v-model:close-confirm-visible="loadTypeCloseConfirmVisible"
      :title="loadTypeDialogTitle"
      :editor-title="loadTypeEditorTitle"
      :form="loadTypeForm"
      :error="loadTypeFormError"
      :search-query="loadTypeSearchQuery"
      :filtered-load-types="filteredLoadTypes"
      :saving="loadTypeSaving"
      :is-create="loadTypeIsCreate"
      :dirty="loadTypeDirty"
      @update:search-query="loadTypeSearchQuery = $event"
      @start-new="startNewLoadType"
      @edit="editLoadType"
      @cancel="cancelLoadTypeChanges"
      @save="saveLoadType"
      @continue-editing="continueLoadTypeEditing"
      @discard="discardLoadTypeAndClose"
    />
  </AdminPageShell>
</template>

<style scoped>
.workload-stack {
  display: grid;
  gap: 16px;
}

.workload-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(330px, 1fr)
  );
  gap: 12px;
}

.workload-assignment {
  min-width: 0;
  height: 100%;

  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workload-assignment__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.workload-assignment__heading {
  min-width: 0;
}

.workload-assignment__heading h3,
.workload-assignment__heading p {
  margin: 0;
}

.workload-assignment__heading h3 {
  color: var(--st-text);

  font-size: 17px;
  line-height: 1.35;
}

.workload-assignment__heading p {
  margin-top: 4px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.45;
}

.workload-assignment__tags {
  max-width: 50%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.workload-assignment__meta {
  margin: 0;

  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.workload-assignment__meta > div {
  min-width: 0;
  padding: 10px;

  display: grid;
  gap: 3px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.workload-assignment__meta dt {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.workload-assignment__meta dd {
  margin: 0;

  color: var(--st-text);

  overflow-wrap: anywhere;

  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.workload-assignment__notes {
  margin: 0;
  padding: 10px 12px;

  color: var(--st-text-secondary);
  background: var(--st-surface-muted);
  border-radius: 9px;

  overflow-wrap: anywhere;

  font-size: 13px;
  line-height: 1.5;
}

.workload-assignment__actions {
  margin-top: auto;

  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .workload-grid,
  .workload-assignment__meta {
    grid-template-columns: 1fr;
  }

  .workload-assignment__header {
    flex-direction: column;
  }

  .workload-assignment__tags {
    max-width: none;
    justify-content: flex-start;
  }

  .workload-assignment__actions,
  .workload-assignment__actions > * {
    width: 100%;
  }
}
</style>
