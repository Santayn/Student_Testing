<script setup>
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />

    <main class="app-content">
      <RouterView />
    </main>

    <AppFooter />
  </div>
</template>

<style>
html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
}

#app {
  min-height: 100vh;
}

.app-shell {
  min-height: 100vh;

  display: flex;
  flex-direction: column;
}

.app-content {
  flex: 1;
  width: 100%;
}
</style>
