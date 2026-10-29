<template>
  <section class="presentation-demo" data-testid="layout-medium">
    <h2>Среднее представление</h2>
    <p>Компактная двухпанельная структура.</p>
    <div class="demo-grid">
      <div>Список</div>
      <div>Редактор</div>
    </div>
  </section>
</template>

<style scoped>
.presentation-demo { padding: 18px; border: 1px solid var(--st-border); border-radius: 12px; }
.demo-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 12px; }
.demo-grid > div { padding: 20px; background: var(--st-surface-muted); border-radius: 8px; }
</style>
