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

.admin-page-header,
.admin-card {
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

.admin-card {
  padding: 20px;
}

.admin-card--compact {
  padding: 16px;
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

.admin-input,
.admin-select,
.admin-textarea {
  width: 100%;

  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 9px;

  font: inherit;
  font-size: 14px;

  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.admin-input,
.admin-select {
  min-height: 42px;
  padding: 8px 10px;
}

.admin-textarea {
  min-height: 92px;
  padding: 10px;

  resize: vertical;
}

.admin-input:focus,
.admin-select:focus,
.admin-textarea:focus {
  outline: 2px solid var(--focus-ring);
  border-color: var(--brand);
}

.admin-input:disabled,
.admin-select:disabled,
.admin-textarea:disabled {
  opacity: 0.62;
  cursor: not-allowed;
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

.admin-btn {
  min-height: 38px;

  padding: 7px 12px;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;

  color: var(--text);
  background: var(--surface-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;

  font: inherit;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    transform 0.15s ease;
}

.admin-btn:hover:not(:disabled) {
  border-color: var(--brand);
}

.admin-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.admin-btn:disabled {
  opacity: 0.52;
  cursor: default;
}

.admin-btn--primary {
  color: var(--text-on-brand);
  background: var(--brand);
  border-color: var(--brand);
}

.admin-btn--primary:hover:not(:disabled) {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.admin-btn--danger {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-border);
}

.admin-btn--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.admin-btn--small {
  min-height: 32px;
  padding: 5px 9px;

  font-size: 12px;
}

.admin-notice {
  padding: 11px 13px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  border: 1px solid var(--border);
  border-radius: 9px;

  font-size: 13px;
  line-height: 1.45;
}

.admin-notice--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success);
}

.admin-notice--error {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-border);
}

.admin-notice--warning {
  color: var(--warning);
  background: var(--warning-soft);
  border-color: var(--warning-border);
}

.admin-notice--info {
  color: var(--brand);
  background: var(--brand-soft);
  border-color: var(--brand);
}

.admin-notice__close {
  padding: 0;

  color: inherit;
  background: transparent;
  border: 0;

  font: inherit;
  font-size: 17px;

  cursor: pointer;
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

.admin-empty {
  padding: 26px 16px;

  color: var(--text-secondary);
  background: var(--surface-secondary);
  border: 1px dashed var(--border);
  border-radius: 9px;

  font-size: 13px;
  text-align: center;
}

.admin-checkbox-list {
  max-height: 330px;
  overflow-y: auto;

  display: grid;
  gap: 7px;
}

.admin-checkbox {
  min-height: 42px;

  padding: 8px 10px;

  display: flex;
  align-items: flex-start;
  gap: 9px;

  color: var(--text);
  background: var(--surface-secondary);
  border: 1px solid var(--border);
  border-radius: 8px;

  font-size: 13px;
  cursor: pointer;
}

.admin-checkbox:hover {
  border-color: var(--brand);
}

.admin-checkbox input {
  margin-top: 2px;

  accent-color: var(--brand);
}

.admin-checkbox__copy {
  min-width: 0;

  display: grid;
  gap: 2px;
}

.admin-checkbox__title {
  font-weight: 700;
}

.admin-checkbox__meta {
  color: var(--text-secondary);

  font-size: 12px;
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

  .admin-card {
    padding: 17px;
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

  .admin-toolbar .admin-input,
  .admin-toolbar .admin-select,
  .admin-toolbar .admin-btn {
    width: 100%;
  }

  .admin-actions--mobile-stack {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-actions--mobile-stack .admin-btn {
    width: 100%;
  }
}
</style>
