<script setup>
import {
  computed,
} from 'vue'
import {
  useRoute,
} from 'vue-router'

import {
  resolveBreadcrumbs,
  useBreadcrumbContextStore,
} from '@/navigation'

const route = useRoute()

const breadcrumbContext =
  useBreadcrumbContextStore()

const crumbs = computed(() => {
  return resolveBreadcrumbs(
    route,
    breadcrumbContext
      ?.contextForRoute(route) ?? {}
  )
})

const visible = computed(() => {
  return crumbs.value.length > 1
})
</script>

<template>
  <nav
    v-if="visible"
    class="app-breadcrumb"
    aria-label="Хлебные крошки"
  >
    <ol class="app-breadcrumb__list">
      <li
        v-for="(crumb, index) in crumbs"
        :key="`${crumb.key}-${index}`"
        class="app-breadcrumb__item"
      >
        <span
          v-if="index > 0"
          class="app-breadcrumb__separator"
          aria-hidden="true"
        >
          <i class="pi pi-chevron-right" />
        </span>

        <RouterLink
          v-if="crumb.to && !crumb.current"
          class="app-breadcrumb__link"
          :to="crumb.to"
        >
          {{ crumb.label }}
        </RouterLink>

        <span
          v-else
          class="app-breadcrumb__current"
          :aria-current="crumb.current ? 'page' : undefined"
        >
          {{ crumb.label }}
        </span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.app-breadcrumb {
  width: 100%;
  min-width: 0;

  margin: 0 0 14px;
}

.app-breadcrumb__list {
  margin: 0;
  padding: 0;

  display: flex;
  align-items: center;
  gap: 7px;

  min-width: 0;

  list-style: none;

  overflow-x: auto;
  scrollbar-width: thin;
}

.app-breadcrumb__item {
  min-width: 0;

  display: inline-flex;
  align-items: center;
  gap: 7px;

  flex: 0 0 auto;
}

.app-breadcrumb__separator {
  display: inline-flex;
  align-items: center;

  color: var(--st-text-muted);

  font-size: 10px;
}

.app-breadcrumb__link,
.app-breadcrumb__current {
  max-width: 260px;

  display: inline-block;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  font-size: 13px;
  line-height: 1.4;
}

.app-breadcrumb__link {
  color: var(--st-text-secondary);

  text-decoration: none;

  border-radius: 5px;

  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.app-breadcrumb__link:hover {
  color: var(--st-primary);
}

.app-breadcrumb__link:focus-visible {
  outline: 2px solid var(--st-primary);
  outline-offset: 2px;

  color: var(--st-primary);

  box-shadow: var(--st-focus-shadow);
}

.app-breadcrumb__current {
  color: var(--st-text);
  font-weight: 600;
}

@media (max-width: 960px) {
  .app-breadcrumb {
    margin-bottom: 12px;
  }

  .app-breadcrumb__list {
    padding-bottom: 2px;
  }

  .app-breadcrumb__link,
  .app-breadcrumb__current {
    max-width: 190px;
  }
}
</style>
