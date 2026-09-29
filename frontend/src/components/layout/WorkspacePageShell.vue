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
  eyebrow: {
    type: String,
    default: '',
  },
  narrow: {
    type: Boolean,
    default: false,
  },
  narrowWidth: {
    type: String,
    default: '920px',
  },
})
</script>

<template>
  <section
    class="workspace-page-shell"
    :class="{
      'workspace-page-shell--narrow': narrow,
    }"
    :style="{
      '--workspace-page-narrow-width': narrowWidth,
    }"
  >
    <header class="workspace-page-shell__header">
      <div class="workspace-page-shell__copy">
        <p
          v-if="eyebrow"
          class="workspace-page-shell__eyebrow"
        >
          {{ eyebrow }}
        </p>

        <h1 class="workspace-page-shell__title">
          {{ title }}
        </h1>

        <p
          v-if="subtitle"
          class="workspace-page-shell__subtitle"
        >
          {{ subtitle }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="workspace-page-shell__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <div class="workspace-page-shell__content">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.workspace-page-shell {
  width: 100%;
  min-width: 0;
  margin: 0 auto;
  padding: 0 0 var(--st-space-section);

  display: grid;
  gap: 18px;

  color: var(--st-text);
}

.workspace-page-shell--narrow {
  width: min(var(--workspace-page-narrow-width), 100%);
}

.workspace-page-shell__header {
  padding: var(--st-space-card);

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;

  color: var(--st-text);
  background: var(--st-surface);

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.workspace-page-shell__copy {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.workspace-page-shell__eyebrow,
.workspace-page-shell__title,
.workspace-page-shell__subtitle {
  margin: 0;
}

.workspace-page-shell__eyebrow {
  color: var(--st-primary);

  font-size: var(--st-font-xs);
  font-weight: 800;
  line-height: var(--st-line-normal);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workspace-page-shell__title {
  font-size: var(--st-font-page-title);
  line-height: var(--st-line-tight);
}

.workspace-page-shell__subtitle {
  max-width: 800px;

  color: var(--st-text-secondary);

  font-size: var(--st-font-md);
  line-height: var(--st-line-relaxed);
}

.workspace-page-shell__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.workspace-page-shell__content {
  min-width: 0;

  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .workspace-page-shell {
    padding-bottom: 18px;
  }

  .workspace-page-shell__header {
    padding: 16px;

    flex-direction: column;
  }

  .workspace-page-shell__actions {
    width: 100%;

    justify-content: stretch;
  }

  .workspace-page-shell__actions :deep(.st-ui-button),
  .workspace-page-shell__actions :deep(.st-ui-link-button) {
    min-height: 44px;
    flex: 1 1 auto;
  }
}

@media (max-width: 480px) {
  .workspace-page-shell__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .workspace-page-shell__actions :deep(.st-ui-button),
  .workspace-page-shell__actions :deep(.st-ui-link-button) {
    width: 100%;
  }
}
</style>
