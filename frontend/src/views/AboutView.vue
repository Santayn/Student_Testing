<script setup>
import {
  UiCard,
} from '@/components/ui'
</script>

<template>
  <section class="about-view">
    <header class="about-view__header">
      <p class="about-view__eyebrow">
        Student Testing
      </p>

      <h1 class="about-view__title">
        О системе
      </h1>

      <p class="about-view__lead">
        Платформа для организации учебного процесса,
        проведения тестирования и просмотра результатов.
      </p>
    </header>

    <div class="about-view__grid">
      <UiCard title="Студентам">
        <p class="about-view__text">
          Доступ к назначенным предметам, лекциям,
          тестам и истории собственных результатов.
        </p>
      </UiCard>

      <UiCard title="Преподавателям">
        <p class="about-view__text">
          Работа с вопросами, тестами, лекциями,
          учебными шаблонами и преподавательской нагрузкой.
        </p>
      </UiCard>

      <UiCard title="Администраторам">
        <p class="about-view__text">
          Управление пользователями, учебной структурой,
          предметами и назначениями преподавателей.
        </p>
      </UiCard>
    </div>
  </section>
</template>

<style scoped>
.about-view {
  width: min(100%, 900px);

  margin: 0 auto;

  display: grid;
  gap: 22px;
}

.about-view__header {
  padding: 28px;

  display: grid;
  gap: 10px;

  background:
    var(--st-surface);

  border: 1px solid
    var(--st-border);

  border-radius: 16px;

  box-shadow:
    var(--st-shadow-elevated);
}

.about-view__eyebrow,
.about-view__title,
.about-view__lead,
.about-view__text {
  margin: 0;
}

.about-view__eyebrow {
  color:
    var(--st-text-secondary);

  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.about-view__title {
  color:
    var(--st-text);

  font-size: clamp(28px, 5vw, 40px);
  line-height: 1.1;
}

.about-view__lead,
.about-view__text {
  color:
    var(--st-text-secondary);

  font-size: 15px;
  line-height: 1.65;
}

.about-view__grid {
  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 760px) {
  .about-view__grid {
    grid-template-columns: 1fr;
  }

  .about-view__header {
    padding: 22px;
  }
}
</style>
