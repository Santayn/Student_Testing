<template>
  <section class="presentation-demo" data-testid="layout-wide">
    <h2>Широкое представление</h2>
    <p>Полноразмерное рабочее пространство с боковой панелью.</p>
    <div class="demo-grid">
      <div>Навигация</div>
      <div>Список</div>
      <div>Редактор</div>
    </div>
  </section>
</template>

<style scoped>
.presentation-demo { padding: 18px; border: 1px solid var(--st-border); border-radius: 12px; }
.demo-grid { display: grid; grid-template-columns: minmax(0, 0.6fr) minmax(0, 1fr) minmax(0, 1.5fr); gap: 12px; }
.demo-grid > div { padding: 20px; background: var(--st-surface-muted); border-radius: 8px; }
</style>
