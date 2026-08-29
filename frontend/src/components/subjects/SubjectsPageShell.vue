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
    class="subjects-page"
    :class="{
      'subjects-page--narrow':
        narrow,
    }"
  >
    <header class="subjects-page__header">
      <div class="subjects-page__header-copy">
        <h1 class="subjects-page__title">
          {{ title }}
        </h1>

        <p
          v-if="subtitle"
          class="subjects-page__subtitle"
        >
          {{ subtitle }}
        </p>
      </div>

      <div
        v-if="$slots.actions"
        class="subjects-page__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <div class="subjects-page__content">
      <slot />
    </div>
  </main>
</template>

<style scoped>
.subjects-page {
  width: min(1180px, 100%);

  margin: 0 auto;
  padding: 24px;

  display: grid;
  gap: 18px;
}

.subjects-page--narrow {
  width: min(880px, 100%);
}

.subjects-page__header {
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

.subjects-page__header-copy {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.subjects-page__title,
.subjects-page__subtitle {
  margin: 0;
}

.subjects-page__title {
  font-size: clamp(
    24px,
    4vw,
    32px
  );
  line-height: 1.15;
}

.subjects-page__subtitle {
  max-width: 760px;

  color: var(--text-secondary);

  font-size: 14px;
  line-height: 1.55;
}

.subjects-page__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.subjects-page__content {
  min-width: 0;

  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .subjects-page {
    padding: 14px;
  }

  .subjects-page__header {
    padding: 16px;

    flex-direction: column;
  }

  .subjects-page__actions {
    width: 100%;

    justify-content: stretch;
  }

  .subjects-page__actions :deep(.ui-button) {
    flex: 1 1 auto;
  }
}

@media (max-width: 480px) {
  .subjects-page__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .subjects-page__actions :deep(.ui-button) {
    width: 100%;
  }
}
</style>
