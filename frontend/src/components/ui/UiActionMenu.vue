<script setup>
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import Menu from 'primevue/menu'

const props = defineProps({
  items: { type: Array, default: () => [] },
  ariaLabel: { type: String, default: 'Действия' },
  icon: { type: String, default: 'pi pi-ellipsis-v' },
})

const emit = defineEmits(['open'])
const menu = ref(null)

const menuItems = computed(() => props.items.map((item) => {
  if (item.separator) return { separator: true }
  return {
    ...item,
    class: [item.class, item.danger ? 'st-ui-action-menu__danger' : ''].filter(Boolean).join(' '),
  }
}))

function toggle(event) {
  emit('open', event)
  menu.value?.toggle(event)
}
</script>

<template>
  <Button
    class="st-ui-action-trigger"
    :icon="icon"
    severity="secondary"
    text
    rounded
    :aria-label="ariaLabel"
    aria-haspopup="true"
    @click="toggle"
  />
  <Menu ref="menu" class="st-ui-action-menu" :model="menuItems" popup />
</template>
