<script setup>
import AppSidebar from '@/components/layout/AppSidebar.vue'
</script>

<template>
  <div class="workspace-layout">
    <AppSidebar />

    <div class="workspace-layout__content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.workspace-layout {
  width: min(100%, 1180px);

  margin: 0 auto;
  padding: 20px;

  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.workspace-layout__content {
  min-width: 0;
  flex: 1;
}

@media (max-width: 960px) {
  .workspace-layout {
    width: 100%;

    padding: 0;

    display: block;
  }

  .workspace-layout__content {
    padding: 16px;
  }
}
</style>
