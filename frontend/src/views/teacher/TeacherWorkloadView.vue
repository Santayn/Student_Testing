<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
} from '@/components/ui'
import TeacherPageShell from '@/components/teacher/TeacherPageShell.vue'
import { useTeacherSubjects } from '@/composables/useTeacherSubjects'
import { useTeacherWorkloadData } from '@/composables/useTeacherWorkloadData'
import { useTeacherWorkloadPresentation } from '@/composables/useTeacherWorkloadPresentation'

const router = useRouter()
const {
  subjectMemberships,
  subjects,
  loadTeacherSubjects,
} = useTeacherSubjects()

const workload = useTeacherWorkloadData({
  subjectMemberships,
  loadTeacherSubjects,
})
const {
  studyCourse,
  semester,
  academicYear,
  assignments,
  loadTypes,
  loading,
  notice,
  refreshAssignments,
} = workload

const {
  searchQuery,
  subjectFilter,
  groupFilter,
  loadTypeFilter,
  statusFilter,
  subjectFilterOptions,
  groupFilterOptions,
  loadTypeFilterOptions,
  statusFilterOptions,
  hasActiveWorkspaceFilters,
  filteredAssignments,
  workloadResultText,
  periodLabel,
  subjectCount,
  groupCount,
  totalHoursPerWeek,
  groupedAssignments,
  groupName,
  loadTypeName,
  statusLabel,
  statusClass,
  formatHours,
  resetWorkspaceFilters,
} = useTeacherWorkloadPresentation({
  subjectMemberships,
  subjects,
  assignments,
  loadTypes,
  studyCourse,
  semester,
  academicYear,
})

const courseOptions = [1, 2, 3, 4, 5, 6].map((value) => ({
  value,
  label: `${value} курс`,
}))
const semesterOptions = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
]

function subjectRoute(group) {
  if (!group.subjectId) {
    return null
  }

  return {
    name: 'subject-details',
    params: {
      subjectId: group.subjectId,
    },
  }
}

function workloadLectureRoute(group) {
  if (
    !group.subjectId ||
    !group.subjectMembershipId
  ) {
    return null
  }

  return {
    name: 'teacher-lectures',
    query: {
      subjectId: group.subjectId,
      subjectMembershipId:
        group.subjectMembershipId,
    },
  }
}

function openRoute(route) {
  if (!route) {
    return
  }

  router.push(route)
}

onMounted(workload.initialize)
onBeforeUnmount(workload.dispose)
</script>

<template>
  <TeacherPageShell
    title="Моя нагрузка"
    subtitle="Просматривайте учебную нагрузку, назначенную администратором. Изменение предметов, групп, типов нагрузки и часов выполняется только в административном разделе."
  >
    <template #actions>
      <UiButton
        size="sm"
        :loading="loading"
        loading-text="Обновление..."
        @click="refreshAssignments"
      >
        Обновить
      </UiButton>
    </template>

    <UiAlert
      v-if="notice.message"
      :variant="notice.type"
      :message="notice.message"
      closable
      @close="notice.message = ''"
    />

    <section class="teacher-stat-grid">
      <div class="teacher-stat">
        <span class="teacher-stat__label">Период</span>
        <span class="teacher-stat__value">{{ periodLabel }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Предметов</span>
        <span class="teacher-stat__value">{{ subjectCount }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Групп</span>
        <span class="teacher-stat__value">{{ groupCount }}</span>
      </div>

      <div class="teacher-stat">
        <span class="teacher-stat__label">Часов в неделю</span>
        <span class="teacher-stat__value">{{ formatHours(totalHoursPerWeek) }}</span>
      </div>
    </section>

    <UiCard
      title="Период"
      description="Фильтр влияет только на просмотр вашей назначенной нагрузки."
    >
      <div class="teacher-grid teacher-grid--3">
        <UiSelect
          v-model="studyCourse"
          label="Курс"
          :options="courseOptions"
        />

        <UiSelect
          v-model="semester"
          label="Семестр"
          :options="semesterOptions"
        />

        <UiInput
          v-model="academicYear"
          label="Учебный год"
          type="number"
          min="2000"
          step="1"
          required
        />
      </div>
    </UiCard>

    <UiCard
      title="Назначенная нагрузка"
      description="Страница работает только в режиме просмотра. Изменения нагрузки выполняет администратор системы."
    >
      <div class="teacher-stack">
        <UiFilterBar
          v-if="assignments.length"
          v-model="searchQuery"
          search-placeholder="Предмет, группа, тип нагрузки или примечание"
          :result-text="workloadResultText"
          :reset-disabled="!hasActiveWorkspaceFilters"
          @reset="resetWorkspaceFilters"
        >
          <template #filters>
            <UiSelect
              v-model="subjectFilter"
              label="Предмет"
              :options="subjectFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="groupFilter"
              label="Группа"
              :options="groupFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="loadTypeFilter"
              label="Тип нагрузки"
              :options="loadTypeFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="statusFilter"
              label="Статус"
              :options="statusFilterOptions"
              size="sm"
            />
          </template>
        </UiFilterBar>

      <UiEmptyState
        v-if="loading"
        description="Загрузка назначенной нагрузки..."
        compact
      />

      <UiEmptyState
        v-else-if="!subjectMemberships.length"
        description="У преподавателя пока нет активных назначений на предметы."
        compact
      />

      <UiEmptyState
        v-else-if="!assignments.length"
        description="По выбранному периоду учебная нагрузка не найдена."
        compact
      />

      <UiEmptyState
        v-else-if="!filteredAssignments.length"
        description="По текущему поиску и фильтрам записи нагрузки не найдены."
        compact
      >
        <template #actions>
          <UiButton
            variant="secondary"
            size="sm"
            label="Сбросить фильтры"
            @click="resetWorkspaceFilters"
          />
        </template>
      </UiEmptyState>

      <div
        v-else
        class="teacher-stack"
      >
        <section
          v-for="group in groupedAssignments"
          :key="group.subjectMembershipId"
          class="teacher-workload-subject"
        >
          <div class="teacher-workload-subject__header">
            <div class="teacher-workload-subject__heading">
              <span class="teacher-muted">
                Учебная нагрузка
              </span>

              <h2 class="teacher-workload-subject__title">
                {{ group.subjectName }}
              </h2>

              <span class="teacher-muted">
                {{ group.items.length }} {{ group.items.length === 1 ? 'запись' : 'записей' }} нагрузки
              </span>
            </div>

            <div class="teacher-actions">
              <UiButton
                size="sm"
                variant="secondary"
                :disabled="!subjectRoute(group)"
                @click="openRoute(subjectRoute(group))"
              >
                К предмету
              </UiButton>

              <UiButton
                size="sm"
                variant="secondary"
                :disabled="!workloadLectureRoute(group)"
                @click="openRoute(workloadLectureRoute(group))"
              >
                Лекции
              </UiButton>
            </div>
          </div>

          <div class="teacher-entity-list">
            <article
              v-for="assignment in group.items"
              :key="assignment.id"
              class="teacher-entity-card"
            >
              <div class="teacher-entity-card__heading">
                <span class="teacher-entity-card__eyebrow">
                  {{ groupName(assignment) }}
                </span>

                <h3 class="teacher-entity-card__title">
                  {{ loadTypeName(assignment.loadTypeId) }}
                </h3>
              </div>

              <div class="teacher-workload-meta">
                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Статус</span>
                  <span :class="statusClass(assignment.status)">
                    {{ statusLabel(assignment.status) }}
                  </span>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Часов в неделю</span>
                  <strong>{{ formatHours(assignment.hoursPerWeek) }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Группа</span>
                  <strong>{{ groupName(assignment) }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Курс</span>
                  <strong>{{ assignment.studyCourse ?? studyCourse }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Семестр</span>
                  <strong>{{ assignment.semester }}</strong>
                </div>

                <div class="teacher-workload-meta__item">
                  <span class="teacher-muted">Учебный год</span>
                  <strong>{{ assignment.academicYear }}</strong>
                </div>
              </div>

              <div
                v-if="assignment.courseVersionId"
                class="teacher-muted"
              >
                Версия курса: #{{ assignment.courseVersionId }}
              </div>

              <p
                v-if="assignment.notes"
                class="teacher-workload-notes"
              >
                {{ assignment.notes }}
              </p>
            </article>
          </div>
        </section>
      </div>
      </div>
    </UiCard>
  </TeacherPageShell>
</template>

<style scoped>
.teacher-workload-subject {
  min-width: 0;
  padding: 14px;

  display: grid;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.teacher-workload-subject__header {
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.teacher-workload-subject__heading {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.teacher-workload-subject__title {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: 18px;
  line-height: 1.3;
}

.teacher-workload-meta {
  min-width: 0;

  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.teacher-workload-meta__item {
  min-width: 0;
  padding: 9px 10px;

  display: grid;
  align-content: start;
  gap: 4px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-workload-meta__item strong {
  overflow-wrap: anywhere;
}

.teacher-workload-notes {
  margin: 0;
  padding-top: 10px;

  color: var(--st-text-secondary);

  border-top: 1px solid var(--st-border);

  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

@media (max-width: 900px) {
  .teacher-workload-meta {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .teacher-workload-meta {
    grid-template-columns: 1fr;
  }

  .teacher-workload-subject__header .teacher-actions,
  .teacher-workload-subject__header .teacher-actions > * {
    width: 100%;
  }
}
</style>
