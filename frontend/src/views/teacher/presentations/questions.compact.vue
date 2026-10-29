<script setup>
import QuestionsContext from '../questions/QuestionsContext.vue'
import QuestionsBank from '../questions/QuestionsBank.vue'

defineOptions({ name: 'QuestionsCompactPresentation' })
</script>

<template>
<section class="questions-compact-workspace" aria-label="Банк вопросов">
  <QuestionsContext />
  <QuestionsBank compact />
</section>
</template>

<style scoped>
.questions-compact-workspace { display: grid; min-width: 0; gap: 12px; }
</style>
