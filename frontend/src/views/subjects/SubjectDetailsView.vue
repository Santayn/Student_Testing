<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main>
    <h1>Предмет {{ route.params.subjectId }}</h1>
  </main>
</template>
