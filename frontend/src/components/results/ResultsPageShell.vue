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
  <main
    class="results-page"
    :class="{
      'results-page--narrow':
        narrow,
    }"
  >
    <header class="results-page__header">
      <div class="results-page__header-copy">
        <h1 class="results-page__title">
          {{ title }}
        </h1>

        <p
          v-if="subtitle"
          class="results-page__subtitle"
        >
          {{ subtitle }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="results-page__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <div class="results-page__content">
      <slot />
    </div>
  </main>
</template>

<style scoped>
.results-page {
  width: min(1180px, 100%);

  margin: 0 auto;
  padding: 24px;

  display: grid;
  gap: 18px;
}

.results-page--narrow {
  width: min(920px, 100%);
}

.results-page__header {
  padding: 20px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 14px;
}

.results-page__header-copy {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.results-page__title,
.results-page__subtitle {
  margin: 0;
}

.results-page__title {
  font-size: clamp(
    24px,
    4vw,
    32px
  );
  line-height: 1.15;
}

.results-page__subtitle {
  max-width: 780px;

  color: var(--text-secondary);

  font-size: 14px;
  line-height: 1.55;
}

.results-page__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.results-page__content {
  min-width: 0;

  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .results-page {
    padding: 14px;
  }

  .results-page__header {
    padding: 16px;

    flex-direction: column;
  }

  .results-page__actions {
    width: 100%;

    justify-content: stretch;
  }

  .results-page__actions :deep(.ui-button) {
    flex: 1 1 auto;
  }
}

@media (max-width: 480px) {
  .results-page__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .results-page__actions :deep(.ui-button) {
    width: 100%;
  }
}
</style>
