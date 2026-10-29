<script setup>
import { useLayoutStore } from '@/stores/layout'
import ResponsivePresentation from '@/components/layout/ResponsivePresentation.vue'
import { getPresentationKeys } from '@/config/presentationRegistry'

const layout = useLayoutStore()
const registeredKeys = getPresentationKeys()
</script>

<template>
  <section class="layout-preview">
    <h1>Проверка адаптивных представлений</h1>
    <p>
      Размер окна: {{ layout.width }} × {{ layout.height }} px;
      глобальный режим: {{ layout.mode }}.
    </p>
    <p>
      Ниже режим выбирается по ширине самого контейнера,
      а не окна браузера. Меняйте ширину окна для проверки lazy load.
    </p>
    <p>Зарегистрированные представления: {{ registeredKeys.join(', ') }}</p>
    <ResponsivePresentation view-key="dev/layout-workspace" />
  </section>
</template>

<style scoped>
.layout-preview {
  display: grid;
  gap: 16px;
  width: 100%;
  max-width: 100%;
}
.layout-preview h1,
.layout-preview p {
  margin: 0;
}
</style>
