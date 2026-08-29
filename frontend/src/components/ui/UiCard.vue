<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },

  description: {
    type: String,
    default: '',
  },

  compact: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <section
    class="ui-card"
    :class="{
      'ui-card--compact':
        compact,
    }"
  >
    <header
      v-if="
        title ||
        description ||
        $slots.header ||
        $slots.actions
      "
      class="ui-card__header"
    >
      <div class="ui-card__header-copy">
        <slot name="header">
          <h2
            v-if="title"
            class="ui-card__title"
          >
            {{ title }}
          </h2>

          <p
            v-if="description"
            class="ui-card__description"
          >
            {{ description }}
          </p>
        </slot>
      </div>

      <div
        v-if="$slots.actions"
        class="ui-card__actions"
      >
        <slot name="actions" />
      </div>
    </header>

    <div class="ui-card__content">
      <slot />
    </div>

    <footer
      v-if="$slots.footer"
      class="ui-card__footer"
    >
      <slot name="footer" />
    </footer>
  </section>
</template>

<style scoped>
.ui-card {
  padding: 20px;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 14px;
}

.ui-card--compact {
  padding: 16px;
}

.ui-card__header {
  margin-bottom: 16px;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.ui-card__header-copy {
  min-width: 0;
}

.ui-card__title,
.ui-card__description {
  margin: 0;
}

.ui-card__title {
  font-size: 19px;
}

.ui-card__description {
  margin-top: 5px;

  color: var(--text-secondary);

  font-size: 13px;
  line-height: 1.5;
}

.ui-card__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ui-card__content {
  min-width: 0;
}

.ui-card__footer {
  margin-top: 16px;
}

@media (max-width: 820px) {
  .ui-card {
    padding: 17px;
  }

  .ui-card__header {
    flex-direction: column;
  }

  .ui-card__actions {
    width: 100%;
  }
}
</style>
