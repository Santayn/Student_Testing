<script setup>
import QuestionsContext from '../questions/QuestionsContext.vue'
import QuestionsBank from '../questions/QuestionsBank.vue'

defineOptions({ name: 'QuestionsMediumPresentation' })
</script>

<template>
<section class="questions-medium-workspace" aria-label="Банк вопросов">
  <QuestionsContext />
  <QuestionsBank />
</section>
</template>

<style scoped>
.questions-medium-workspace { display: grid; min-width: 0; gap: 16px; }
</style>
