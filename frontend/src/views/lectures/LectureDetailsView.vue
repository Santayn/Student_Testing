<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main>
    <h1>Лекция {{ route.params.lectureId }}</h1>
  </main>
</template>
