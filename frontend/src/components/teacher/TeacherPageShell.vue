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

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
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

  color: var(--text-secondary);

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

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 10px;
}

.teacher-stat__label {
  color: var(--text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.teacher-stat__value {
  overflow-wrap: anywhere;

  font-size: 17px;
  font-weight: 800;
}

.teacher-muted {
  color: var(--text-secondary);

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

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
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

  color: var(--text-secondary);

  font-size: 11px;
}

.teacher-status {
  width: fit-content;
  padding: 3px 7px;

  color: var(--text-secondary);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 999px;

  font-size: 11px;
  font-weight: 800;
}

.teacher-status--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.teacher-status--warning {
  color: var(--warning);
  background: var(--warning-soft);
  border-color: var(--warning-border);
}

.teacher-status--danger {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-border);
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
  background: var(--border);
}

.teacher-field-label {
  margin-bottom: 6px;

  display: block;

  color: var(--text);

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

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 8px;
}

.teacher-row-card {
  padding: 14px;

  display: grid;
  gap: 12px;

  background: var(--surface-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
}

.teacher-row-card__header {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}




@media (min-width: 721px) {
  .teacher-publication-fields {
    grid-template-columns: minmax(0, 1fr) !important;
  }
}



.teacher-publication-fields {
  width: 100%;

  display: grid;
  grid-template-columns: minmax(0, 1fr) !important;
  gap: 16px;
}

.teacher-publication-fields > * {
  width: 100%;
  min-width: 0;
}


@media (max-width: 980px) {
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
  .teacher-list-item__header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
