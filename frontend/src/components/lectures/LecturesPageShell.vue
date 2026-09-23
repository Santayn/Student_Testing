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

  narrow: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <section
    class="lectures-page"
    :class="{
      'lectures-page--narrow':
        narrow,
    }"
  >
    <header class="lectures-page__header">
      <div class="lectures-page__header-copy">
        <h1 class="lectures-page__title">
          {{ title }}
        </h1>

        <p
          v-if="subtitle"
          class="lectures-page__subtitle"
        >
          {{ subtitle }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="lectures-page__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <div class="lectures-page__content">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.lectures-page {
  color: var(--st-text);
  width: 100%;

  margin: 0 auto;
  padding: 0 0 24px;

  display: grid;
  gap: 18px;
}

.lectures-page--narrow {
  width: min(920px, 100%);
}

.lectures-page__header {
  padding: 20px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;

  color: var(--st-text);
  background: var(--st-surface);

  border: 1px solid var(--st-border);
  border-radius: 14px;
}

.lectures-page__header-copy {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.lectures-page__title,
.lectures-page__subtitle {
  margin: 0;
}

.lectures-page__title {
  font-size: clamp(
    24px,
    4vw,
    32px
  );
  line-height: 1.15;
}

.lectures-page__subtitle {
  max-width: 760px;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.55;
}

.lectures-page__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.lectures-page__content {
  min-width: 0;

  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .lectures-page {
    padding: 0 0 18px;
  }

  .lectures-page__header {
    padding: 16px;

    flex-direction: column;
  }

  .lectures-page__actions {
    width: 100%;

    justify-content: stretch;
  }

  .lectures-page__actions :deep(.st-ui-button),
  .lectures-page__actions :deep(.st-ui-link-button) {
    flex: 1 1 auto;
  }
}

@media (max-width: 480px) {
  .lectures-page__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .lectures-page__actions :deep(.st-ui-button),
  .lectures-page__actions :deep(.st-ui-link-button) {
    width: 100%;
  }
}
</style>
