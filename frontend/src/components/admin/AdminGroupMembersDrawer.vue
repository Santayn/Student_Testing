<script setup>
import {
  UiAlert,
  UiButton,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
} from '@/components/ui'

defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: 'Состав группы',
  },
  busy: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  notice: {
    type: Object,
    default: () => ({
      type: 'info',
      message: '',
    }),
  },
  search: {
    type: String,
    default: '',
  },
  resultText: {
    type: String,
    default: '',
  },
  currentMemberships: {
    type: Array,
    default: () => [],
  },
  filteredCurrentMemberships: {
    type: Array,
    default: () => [],
  },
  availableStudents: {
    type: Array,
    default: () => [],
  },
  filteredAvailableStudents: {
    type: Array,
    default: () => [],
  },
  addingPersonId: {
    type: [Number, String],
    default: null,
  },
  removingMembershipId: {
    type: [Number, String],
    default: null,
  },
  removeConfirmVisible: {
    type: Boolean,
    default: false,
  },
  removeError: {
    type: String,
    default: '',
  },
  removeTarget: {
    type: Object,
    default: null,
  },
  group: {
    type: Object,
    default: null,
  },
  personById: {
    type: Function,
    required: true,
  },
  personName: {
    type: Function,
    required: true,
  },
  personContact: {
    type: Function,
    required: true,
  },
  pausedStudentMembership: {
    type: Function,
    required: true,
  },
  availableStudentActionLabel: {
    type: Function,
    required: true,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'after-hide',
  'clear-notice',
  'update:search',
  'reset-search',
  'add-student',
  'request-remove',
  'close-remove',
  'confirm-remove',
])

function handleRemoveDialogModel(value) {
  if (!value) {
    emit('close-remove')
  }
}
</script>

<template>
  <UiDrawer
    :model-value="modelValue"
    :title="title"
    width="48rem"
    :closable="!busy"
    :close-on-escape="!busy"
    :dismissable="false"
    @update:model-value="emit('update:modelValue', $event)"
    @after-hide="emit('after-hide')"
  >
    <div class="admin-group-members">
      <p class="admin-group-members__intro">
        Управляйте студентами этой группы. Добавление выполняется сразу, а удаление из состава требует подтверждения.
      </p>

      <UiAlert
        v-if="notice.message"
        :variant="notice.type"
        :message="notice.message"
        closable
        @close="emit('clear-notice')"
      />

      <UiFilterBar
        :model-value="search"
        search-placeholder="ФИО, email или телефон"
        :result-text="resultText"
        :reset-disabled="!search.trim()"
        @update:model-value="emit('update:search', $event)"
        @reset="emit('reset-search')"
      />

      <UiEmptyState
        v-if="loading"
        description="Загрузка состава группы..."
        compact
      />

      <template v-else>
        <section class="admin-group-members__section">
          <div class="admin-group-members__section-heading">
            <div>
              <h3>Текущие участники</h3>
              <p>Студенты, которые сейчас входят в группу.</p>
            </div>

            <span class="admin-group-members__count">
              {{ currentMemberships.length }}
            </span>
          </div>

          <UiEmptyState
            v-if="!currentMemberships.length"
            description="В группе пока нет студентов."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredCurrentMemberships.length"
            description="Среди участников ничего не найдено."
            compact
          />

          <div
            v-else
            class="admin-group-members__list"
          >
            <article
              v-for="membership in filteredCurrentMemberships"
              :key="membership.id"
              class="admin-group-member-card"
            >
              <div class="admin-group-member-card__body">
                <strong>
                  {{ personName(personById(membership.personId)) }}
                </strong>

                <span
                  v-if="personContact(personById(membership.personId))"
                  class="admin-group-member-card__meta"
                >
                  {{ personContact(personById(membership.personId)) }}
                </span>

                <span
                  v-if="membership.notes"
                  class="admin-group-member-card__notes"
                >
                  {{ membership.notes }}
                </span>
              </div>

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-user-minus"
                label="Убрать"
                :disabled="addingPersonId !== null"
                :loading="removingMembershipId === membership.id"
                loading-text="Удаление..."
                @click="emit('request-remove', membership)"
              />
            </article>
          </div>
        </section>

        <section class="admin-group-members__section">
          <div class="admin-group-members__section-heading">
            <div>
              <h3>Доступные студенты</h3>
              <p>Активные пользователи с ролью «Студент», которых сейчас нет в этой группе.</p>
            </div>

            <span class="admin-group-members__count">
              {{ availableStudents.length }}
            </span>
          </div>

          <UiEmptyState
            v-if="!availableStudents.length"
            description="Нет доступных студентов для добавления."
            compact
          />

          <UiEmptyState
            v-else-if="!filteredAvailableStudents.length"
            description="Среди доступных студентов ничего не найдено."
            compact
          />

          <div
            v-else
            class="admin-group-members__list"
          >
            <article
              v-for="person in filteredAvailableStudents"
              :key="person.id"
              class="admin-group-member-card"
            >
              <div class="admin-group-member-card__body">
                <strong>{{ personName(person) }}</strong>

                <span
                  v-if="personContact(person)"
                  class="admin-group-member-card__meta"
                >
                  {{ personContact(person) }}
                </span>

                <span
                  v-if="pausedStudentMembership(person.id)"
                  class="admin-group-member-card__hint"
                >
                  Ранее состоял в группе — назначение будет восстановлено.
                </span>
              </div>

              <UiButton
                variant="primary"
                size="sm"
                icon="pi pi-user-plus"
                :label="availableStudentActionLabel(person)"
                :disabled="removingMembershipId !== null"
                :loading="addingPersonId === person.id"
                loading-text="Добавление..."
                @click="emit('add-student', person)"
              />
            </article>
          </div>
        </section>
      </template>
    </div>
  </UiDrawer>

  <UiDialog
    :model-value="removeConfirmVisible"
    title="Убрать студента из группы?"
    width="31rem"
    :closable="removingMembershipId === null"
    :close-on-escape="removingMembershipId === null"
    :dismissable-mask="false"
    @update:model-value="handleRemoveDialogModel"
  >
    <div class="admin-group-member-delete">
      <UiAlert
        v-if="removeError"
        variant="danger"
        :message="removeError"
      />

      <p>
        <strong>{{ personName(removeTarget?.person) }}</strong>
        перестанет входить в группу
        <strong>«{{ group?.name }}»</strong>.
        Историческая запись назначения сохранится в системе.
      </p>
    </div>

    <template #footer>
      <div class="admin-group-member-delete__actions">
        <UiButton
          variant="secondary"
          label="Отмена"
          :disabled="removingMembershipId !== null"
          @click="emit('close-remove')"
        />

        <UiButton
          variant="danger"
          label="Убрать из группы"
          :loading="removingMembershipId !== null"
          loading-text="Удаление..."
          @click="emit('confirm-remove')"
        />
      </div>
    </template>
  </UiDialog>
</template>

<style scoped>
.admin-group-members {
  min-width: 0;

  display: grid;
  gap: 18px;
}

.admin-group-members__intro {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.6;
}

.admin-group-members__section {
  min-width: 0;
  padding-top: 2px;

  display: grid;
  gap: 10px;
}

.admin-group-members__section + .admin-group-members__section {
  padding-top: 18px;
  border-top: 1px solid var(--st-border);
}

.admin-group-members__section-heading {
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-group-members__section-heading h3,
.admin-group-members__section-heading p {
  margin: 0;
}

.admin-group-members__section-heading h3 {
  color: var(--st-text);

  font-size: 16px;
  line-height: 1.35;
}

.admin-group-members__section-heading p {
  margin-top: 4px;

  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.5;
}

.admin-group-members__count {
  min-width: 32px;
  min-height: 32px;
  padding: 5px 9px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color: var(--st-primary);
  background: var(--st-primary-soft);
  border-radius: 999px;

  font-size: 12px;
  font-weight: 800;
}

.admin-group-members__list {
  min-width: 0;

  display: grid;
  gap: 8px;
}

.admin-group-member-card {
  min-width: 0;
  padding: 12px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.admin-group-member-card__body {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.admin-group-member-card__body strong,
.admin-group-member-card__meta,
.admin-group-member-card__notes,
.admin-group-member-card__hint {
  overflow-wrap: anywhere;
}

.admin-group-member-card__meta,
.admin-group-member-card__notes,
.admin-group-member-card__hint {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.45;
}

.admin-group-member-card__hint {
  color: var(--st-primary);
}

.admin-group-member-delete {
  display: grid;
  gap: 14px;
}

.admin-group-member-delete p {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.admin-group-member-delete strong {
  color: var(--st-text);
}

.admin-group-member-delete__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 640px) {
  .admin-group-member-card {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-group-member-card > :last-child,
  .admin-group-member-delete__actions > * {
    width: 100%;
  }

  .admin-group-member-delete__actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
