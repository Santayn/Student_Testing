<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main>
    <h1>Тест {{ route.params.testId }}</h1>
  </main>
</template>
