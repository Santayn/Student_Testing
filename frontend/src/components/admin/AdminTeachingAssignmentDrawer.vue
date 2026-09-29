<script setup>
import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDrawer,
  UiEmptyState,
  UiInput,
  UiSelect,
  UiTextarea,
} from '@/components/ui'

import {
  ADMIN_TEACHING_COURSE_OPTIONS as COURSE_OPTIONS,
  ADMIN_TEACHING_SEMESTER_OPTIONS as SEMESTER_OPTIONS,
  ADMIN_TEACHING_STATUS_OPTIONS as STATUS_OPTIONS,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentsData'

import {
  ADMIN_TEACHING_MAX_HOURS_PER_WEEK,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentEditor'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '',
  },
  form: {
    type: Object,
    required: true,
  },
  error: {
    type: String,
    default: '',
  },
  loadTypes: {
    type: Array,
    default: () => [],
  },
  subjectOptions: {
    type: Array,
    default: () => [],
  },
  teacherOptions: {
    type: Array,
    default: () => [],
  },
  loadTypeOptions: {
    type: Array,
    default: () => [],
  },
  courseVersionOptions: {
    type: Array,
    default: () => [],
  },
  groupOptions: {
    type: Array,
    default: () => [],
  },
  filteredGroups: {
    type: Array,
    default: () => [],
  },
  groupSearchQuery: {
    type: String,
    default: '',
  },
  saving: {
    type: Boolean,
    default: false,
  },
  loadingCourseVersions: {
    type: Boolean,
    default: false,
  },
  isCreate: {
    type: Boolean,
    default: true,
  },
  groupHasConflict: {
    type: Function,
    default: () => false,
  },
  groupConflictDescription: {
    type: Function,
    default: () => '',
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:group-search-query',
  'subject-change',
  'teacher-change',
  'request-close',
  'save',
])

function updateModelValue(value) {
  emit('update:modelValue', value)
}
</script>

<template>
  <UiDrawer
    :model-value="modelValue"
    :title="title"
    width="48rem"
    @update:model-value="updateModelValue"
  >
    <div class="workload-drawer">
      <UiAlert
        v-if="error"
        variant="danger"
        :message="error"
      />

      <UiAlert
        v-if="!loadTypes.length"
        variant="warning"
        message="Сначала создайте хотя бы один тип нагрузки через действие «Типы нагрузки»."
      />

      <UiCard
        title="Назначение"
        description="Преподаватель выбирается среди активных назначений на выбранный предмет."
        compact
      >
        <div class="admin-form-grid">
          <UiSelect
            v-model="form.subjectId"
            label="Предмет"
            :options="subjectOptions"
            placeholder="Выберите предмет"
            :filter="true"
            filter-placeholder="Поиск предмета"
            required
            :disabled="saving"
            @change="emit('subject-change')"
          />

          <UiSelect
            v-model="form.subjectMembershipId"
            label="Преподаватель"
            :options="teacherOptions"
            placeholder="Выберите преподавателя"
            :filter="true"
            filter-placeholder="Поиск по ФИО или email"
            required
            :disabled="saving || !form.subjectId"
            @change="emit('teacher-change')"
          />

          <UiSelect
            v-model="form.loadTypeId"
            label="Тип нагрузки"
            :options="loadTypeOptions"
            placeholder="Выберите тип нагрузки"
            :filter="true"
            filter-placeholder="Поиск типа"
            required
            :disabled="saving || !loadTypes.length"
          />

          <UiInput
            v-model="form.hoursPerWeek"
            label="Часы в неделю"
            type="number"
            min="0"
            :max="ADMIN_TEACHING_MAX_HOURS_PER_WEEK"
            step="0.25"
            placeholder="2.00"
            required
            :disabled="saving"
          />

          <UiSelect
            v-model="form.courseVersionId"
            label="Версия курса"
            :options="courseVersionOptions"
            placeholder="Без версии курса"
            :filter="true"
            filter-placeholder="Поиск версии"
            :disabled="saving || loadingCourseVersions || !form.subjectMembershipId"
            hint="Необязательно. Показываются версии курса выбранного преподавателя по этому предмету."
          />

          <UiSelect
            v-model="form.status"
            label="Статус"
            :options="STATUS_OPTIONS"
            required
            :disabled="saving"
          />
        </div>
      </UiCard>

      <UiCard
        title="Учебный период"
        description="Изменение периода у существующего назначения может переместить его из текущего списка."
        compact
      >
        <div class="admin-form-grid admin-form-grid--3">
          <UiSelect
            v-model="form.studyCourse"
            label="Курс"
            :options="COURSE_OPTIONS"
            required
            :disabled="saving"
          />

          <UiSelect
            v-model="form.semester"
            label="Семестр"
            :options="SEMESTER_OPTIONS"
            required
            :disabled="saving"
          />

          <UiInput
            v-model="form.academicYear"
            label="Учебный год"
            type="number"
            min="2000"
            step="1"
            required
            :disabled="saving"
          />
        </div>
      </UiCard>

      <UiCard
        v-if="isCreate"
        title="Группы"
        description="Можно создать одинаковую нагрузку сразу для нескольких групп факультета."
        compact
      >
        <UiInput
          :model-value="groupSearchQuery"
          label="Поиск группы"
          placeholder="Название или код группы"
          :disabled="saving"
          @update:model-value="emit('update:group-search-query', $event)"
        />

        <div
          v-if="filteredGroups.length"
          class="workload-group-picker"
        >
          <UiCheckbox
            v-for="group in filteredGroups"
            :key="group.id"
            v-model="form.groupIds"
            mode="multiple"
            :value="group.id"
            :label="group.name"
            :description="
              groupHasConflict(group.id)
                ? groupConflictDescription(group.id)
                : group.code || 'Доступна для назначения'
            "
            :disabled="
              saving ||
              (
                groupHasConflict(group.id) &&
                !form.groupIds.includes(group.id)
              )
            "
          />
        </div>

        <UiEmptyState
          v-else
          description="Группы по поиску не найдены."
          compact
        />
      </UiCard>

      <UiCard
        v-else
        title="Группа"
        description="Для одного существующего назначения выбирается одна группа."
        compact
      >
        <UiSelect
          v-model="form.groupId"
          label="Группа"
          :options="groupOptions"
          placeholder="Выберите группу"
          :filter="true"
          filter-placeholder="Поиск группы"
          required
          :disabled="saving"
        />
      </UiCard>

      <UiTextarea
        v-model="form.notes"
        label="Примечание"
        maxlength="1000"
        placeholder="Необязательное примечание к нагрузке"
        :disabled="saving"
      />
    </div>

    <template #footer>
      <div class="workload-drawer__footer">
        <UiButton
          variant="secondary"
          label="Отмена"
          :disabled="saving"
          @click="emit('request-close')"
        />

        <UiButton
          variant="primary"
          :label="isCreate ? 'Создать нагрузку' : 'Сохранить изменения'"
          :loading="saving"
          loading-text="Сохранение..."
          :disabled="saving || !loadTypes.length"
          @click="emit('save')"
        />
      </div>
    </template>
  </UiDrawer>
</template>

<style scoped>
.workload-drawer {
  display: grid;
  gap: 16px;
}

.workload-group-picker {
  max-height: 360px;
  overflow-y: auto;

  margin-top: 12px;

  display: grid;
  gap: 7px;
}

.workload-drawer__footer {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  .workload-drawer__footer,
  .workload-drawer__footer > * {
    width: 100%;
  }

  .workload-drawer__footer {
    flex-direction: column-reverse;
  }
}
</style>
