<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

const route = useRoute()

const showSidebar = computed(() => {
  return route.meta.sidebar === true
})
</script>

<template>
  <div class="app-shell">
    <AppHeader />

    <div
      class="app-body"
      :class="{
        'app-body--with-sidebar': showSidebar,
      }"
    >
      <AppSidebar v-if="showSidebar" />

      <main class="app-main">
        <RouterView />
      </main>
    </div>

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

  background:
    var(--bg);

  color:
    var(--text);
}

#app {
  min-height: 100vh;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

.app-shell {
  min-height: 100vh;

  display: flex;
  flex-direction: column;
}

.app-body {
  width: min(100%, 1180px);

  margin: 0 auto;
  padding: 20px;

  flex: 1;

  display: block;
}

.app-body--with-sidebar {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.app-main {
  min-width: 0;
  width: 100%;
  flex: 1;
}

@media (max-width: 960px) {
  .app-body,
  .app-body--with-sidebar {
    width: 100%;

    padding: 0;

    display: block;
  }

  .app-main {
    padding: 16px;
  }
}
</style>
