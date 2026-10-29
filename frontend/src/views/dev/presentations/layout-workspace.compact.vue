<template>
  <section class="presentation-demo" data-testid="layout-compact">
    <h2>Компактное представление</h2>
    <p>Одна колонка, последовательное взаимодействие.</p>
    <div class="demo-grid">
      <div>Список</div>
      <div>Редактор</div>
    </div>
  </section>
</template>

<style scoped>
.presentation-demo { padding: 18px; border: 1px solid var(--st-border); border-radius: 12px; }
.demo-grid { display: grid; gap: 12px; }
.demo-grid > div { padding: 20px; background: var(--st-surface-muted); border-radius: 8px; }
</style>
