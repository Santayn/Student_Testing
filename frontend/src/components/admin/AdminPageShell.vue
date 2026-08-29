<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  eyebrow: {
    type: String,
    default: 'Администрирование',
  },
})
</script>

<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-header__copy">
        <p class="admin-page-header__eyebrow">
          {{ eyebrow }}
        </p>

        <h1>{{ title }}</h1>

        <p
          v-if="description"
          class="admin-page-header__description"
        >
          {{ description }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="admin-page-header__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <slot />
  </div>
</template>

<style>
.admin-page {
  display: grid;
  gap: 18px;
}

.admin-page-header {
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
}

.admin-page-header {
  padding: 24px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.admin-page-header__copy {
  min-width: 0;
}

.admin-page-header__eyebrow {
  margin: 0 0 6px;

  color: var(--brand);

  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.admin-page-header h1 {
  margin: 0;

  font-size: clamp(25px, 4vw, 34px);
  line-height: 1.15;
}

.admin-page-header__description {
  max-width: 760px;

  margin: 8px 0 0;

  color: var(--text-secondary);

  font-size: 14px;
  line-height: 1.55;
}

.admin-page-header__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-card__header {
  margin-bottom: 16px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.admin-card__header h2,
.admin-card__header h3,
.admin-card__header p {
  margin: 0;
}

.admin-card__header h2 {
  font-size: 19px;
}

.admin-card__header h3 {
  font-size: 16px;
}

.admin-card__header p {
  margin-top: 5px;

  color: var(--text-secondary);

  font-size: 13px;
  line-height: 1.5;
}

.admin-grid {
  display: grid;
  gap: 14px;
}

.admin-grid--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.admin-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.admin-form-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.admin-form-grid--4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-field {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.admin-field--wide {
  grid-column: 1 / -1;
}

.admin-field > span,
.admin-field > label {
  color: var(--text);

  font-size: 13px;
  font-weight: 700;
}


.admin-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-actions--end {
  justify-content: flex-end;
}



.admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-toolbar__group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-summary {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.admin-stat {
  padding: 13px;

  display: grid;
  gap: 3px;

  background: var(--surface-secondary);
  border: 1px solid var(--border);
  border-radius: 9px;
}

.admin-stat__label {
  color: var(--text-secondary);

  font-size: 12px;
}

.admin-stat__value {
  color: var(--text);

  font-size: 20px;
  font-weight: 800;
}


.admin-muted {
  color: var(--text-secondary);
}


.admin-checkbox-list {
  max-height: 330px;
  overflow-y: auto;

  display: grid;
  gap: 7px;
}


.admin-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.admin-chip {
  padding: 5px 8px;

  color: var(--text);
  background: var(--surface-secondary);
  border: 1px solid var(--border);
  border-radius: 999px;

  font-size: 12px;
}

.admin-divider {
  height: 1px;
  margin: 4px 0;

  background: var(--border);
}

@media (max-width: 820px) {
  .admin-page-header {
    padding: 20px;

    flex-direction: column;
  }

  .admin-page-header__actions {
    width: 100%;
  }

  .admin-page-header__actions > * {
    flex: 1;
  }

  .admin-grid--2,
  .admin-grid--3,
  .admin-form-grid,
  .admin-form-grid--3,
  .admin-form-grid--4 {
    grid-template-columns: 1fr;
  }

  .admin-card__header {
    flex-direction: column;
  }
}

@media (max-width: 640px) {

  .admin-toolbar,
  .admin-toolbar__group {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-toolbar .admin-field {
    width: 100%;
  }

  .admin-actions--mobile-stack {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-actions--mobile-stack > .ui-button {
    width: 100%;
  }
}
</style>
