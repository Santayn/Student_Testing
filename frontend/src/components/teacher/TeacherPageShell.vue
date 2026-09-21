<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },

  subtitle: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <section class="teacher-page">
    <header class="teacher-page__header">
      <div class="teacher-page__heading">
        <h1 class="teacher-page__title">
          {{ title }}
        </h1>

        <p
          v-if="subtitle"
          class="teacher-page__subtitle"
        >
          {{ subtitle }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="teacher-page__header-actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <slot />
  </section>
</template>

<style>
.teacher-page {
  color: var(--st-text);
  width: 100%;
  min-width: 0;

  display: grid;
  gap: 18px;
}

.teacher-page__header {
  padding: 18px 20px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;

  color: var(--st-text);
  background: var(--st-surface);

  border: 1px solid var(--st-border);
  border-radius: 14px;
}

.teacher-page__heading {
  min-width: 0;
}

.teacher-page__title,
.teacher-page__subtitle {
  margin: 0;
}

.teacher-page__title {
  font-size: clamp(24px, 3vw, 32px);
}

.teacher-page__subtitle {
  margin-top: 6px;

  max-width: 820px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.55;
}

.teacher-page__header-actions,
.teacher-actions,
.teacher-inline-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-layout {
  min-width: 0;

  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    minmax(320px, 0.82fr);
  gap: 16px;
  align-items: start;
}

.teacher-stack {
  min-width: 0;

  display: grid;
  gap: 16px;
}

.teacher-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.teacher-grid--3 {
  grid-template-columns:
    repeat(3, minmax(0, 1fr));
}

.teacher-grid--4 {
  grid-template-columns:
    repeat(4, minmax(0, 1fr));
}

.teacher-stat-grid {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.teacher-stat {
  min-width: 0;
  padding: 13px 14px;

  display: grid;
  gap: 4px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.teacher-stat__label {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.teacher-stat__value {
  overflow-wrap: anywhere;

  font-size: 17px;
  font-weight: 800;
}

.teacher-muted {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.5;
}

.teacher-list {
  display: grid;
  gap: 9px;
}

.teacher-list-item {
  padding: 12px;

  display: grid;
  gap: 8px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.teacher-list-item__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.teacher-list-item__title {
  min-width: 0;

  font-weight: 800;
  overflow-wrap: anywhere;
}

.teacher-list-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;

  color: var(--st-text-secondary);

  font-size: 11px;
}

.teacher-status {
  width: fit-content;
  padding: 3px 7px;

  color: var(--st-text-secondary);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 999px;

  font-size: 11px;
  font-weight: 800;
}

.teacher-status--success {
  color: var(--st-success);
  background: var(--st-success-soft);
  border-color: var(--st-success);
}

.teacher-status--warning {
  color: var(--st-warning);
  background: var(--st-warning-soft);
  border-color: var(--st-warning);
}

.teacher-status--danger {
  color: var(--st-danger);
  background: var(--st-danger-soft);
  border-color: var(--st-danger);
}

.teacher-selection-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.teacher-scroll-list {
  max-height: 370px;
  overflow: auto;

  padding-right: 3px;

  display: grid;
  gap: 7px;
}

.teacher-divider {
  height: 1px;
  background: var(--st-border);
}

.teacher-field-label {
  margin-bottom: 6px;

  display: block;

  color: var(--st-text);

  font-size: 13px;
  font-weight: 700;
}

.teacher-file-list {
  display: grid;
  gap: 7px;
}

.teacher-file-item {
  padding: 9px 10px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 8px;
}

.teacher-row-card {
  padding: 14px;

  display: grid;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.teacher-row-card__header {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}



.teacher-entity-list {
  min-width: 0;

  display: grid;
  gap: 10px;
}


.teacher-entity-card {
  min-width: 0;
  padding: 13px 14px;

  display: grid;
  gap: 10px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 11px;
}

.teacher-entity-card--selected {
  border-color: var(--st-primary);
  box-shadow: 0 0 0 1px var(--st-primary-soft);
}

.teacher-entity-card__header {
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.teacher-entity-card__heading {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.teacher-entity-card__eyebrow {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.teacher-entity-card__title {
  margin: 0;

  color: var(--st-text);

  font-size: 14px;
  font-weight: 800;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.teacher-entity-card__description {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.teacher-entity-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;

  color: var(--st-text-secondary);

  font-size: 11px;
}

.teacher-entity-card__meta > span {
  overflow-wrap: anywhere;
}

.teacher-entity-card__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}


.teacher-choice-list {
  display: grid;
  gap: 7px;
}

.teacher-choice-row {
  min-width: 0;
  padding: 9px 10px;

  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.teacher-choice-row__copy {
  min-width: 0;

  display: grid;
  gap: 3px;
}

.teacher-choice-row__title {
  color: var(--st-text);

  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.teacher-choice-row__meta {
  color: var(--st-text-secondary);

  font-size: 11px;
  overflow-wrap: anywhere;
}

.teacher-summary-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 14px;

  color: var(--st-text-secondary);

  font-size: 12px;
}

.teacher-summary-strip strong {
  color: var(--st-text);
}



@media (max-width: 1180px) {
  .teacher-layout {
    grid-template-columns: 1fr;
  }

  .teacher-stat-grid {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }

  .teacher-grid--4 {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .teacher-grid--3 {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .teacher-page__header {
    padding: 16px;

    flex-direction: column;
  }

  .teacher-page__header-actions {
    width: 100%;
  }

  .teacher-page__header-actions > .ui-button {
    flex: 1 1 auto;
  }

  .teacher-grid,
  .teacher-grid--3,
  .teacher-grid--4,
  .teacher-selection-grid,
  .teacher-stat-grid {
    grid-template-columns: 1fr;
  }

  .teacher-actions--mobile-stack,
  .teacher-inline-actions--mobile-stack {
    align-items: stretch;
    flex-direction: column;
  }

  .teacher-actions--mobile-stack > .ui-button,
  .teacher-inline-actions--mobile-stack > .ui-button {
    width: 100%;
  }

  .teacher-row-card__header {
    grid-template-columns: 1fr;
  }

  .teacher-file-item,
  .teacher-list-item__header,
  .teacher-entity-card__header {
    align-items: stretch;
    flex-direction: column;
  }

  .teacher-choice-row {
    grid-template-columns: 1fr;
  }

  .teacher-entity-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .teacher-entity-card__actions > .ui-button,
  .teacher-entity-card__actions > a {
    width: 100%;
  }
}
</style>
