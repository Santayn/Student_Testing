<script setup>
defineProps({
  id: {
    type: String,
    required: true,
  },

  label: {
    type: String,
    default: '',
  },

  hint: {
    type: String,
    default: '',
  },

  error: {
    type: String,
    default: '',
  },

  required: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div
    class="ui-field"
    :class="{
      'ui-field--error': error,
    }"
  >
    <label
      v-if="label"
      class="ui-field__label"
      :for="id"
    >
      {{ label }}

      <span
        v-if="required"
        class="ui-field__required"
        aria-hidden="true"
      >
        *
      </span>
    </label>

    <slot />

    <p
      v-if="error"
      :id="`${id}-error`"
      class="ui-field__message ui-field__message--error"
    >
      {{ error }}
    </p>

    <p
      v-else-if="hint"
      :id="`${id}-hint`"
      class="ui-field__message"
    >
      {{ hint }}
    </p>
  </div>
</template>

<style scoped>
.ui-field {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.ui-field__label {
  color: var(--text);

  font-size: 13px;
  font-weight: 700;
}

.ui-field__required {
  color: var(--danger);
}

.ui-field__message {
  margin: 0;

  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.4;
}

.ui-field__message--error {
  color: var(--danger);
}
</style>
