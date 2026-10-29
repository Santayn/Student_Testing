<script setup>
import QuestionsContext from '../questions/QuestionsContext.vue'
import QuestionsBank from '../questions/QuestionsBank.vue'

defineOptions({ name: 'QuestionsWidePresentation' })
</script>

<template>
<div class="questions-wide-workspace">
  <aside class="questions-wide-sidebar" aria-label="Контекст предмета">
    <QuestionsContext />
  </aside>
  <section class="questions-wide-main">
    <QuestionsBank />
  </section>
</div>
</template>

<style scoped>
.questions-wide-workspace { display: grid; grid-template-columns: minmax(235px, 29%) minmax(0, 1fr); align-items: start; gap: 16px; }
.questions-wide-sidebar, .questions-wide-main { min-width: 0; }
</style>
