<script setup>
import { computed, ref } from 'vue'

import UiEmptyState from './UiEmptyState.vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true,
  },

  rows: {
    type: Array,
    default: () => [],
  },

  rowKey: {
    type: [String, Function],
    default: 'id',
  },

  loading: {
    type: Boolean,
    default: false,
  },

  loadingMessage: {
    type: String,
    default: 'Загрузка...',
  },

  emptyMessage: {
    type: String,
    default: 'Нет данных.',
  },

  defaultSort: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits([
  'sort-change',
])

const sortKey = ref(
  props.defaultSort?.key ?? ''
)

const sortDirection = ref(
  props.defaultSort?.direction === 'desc'
    ? 'desc'
    : 'asc'
)

const sortableColumns = computed(() => {
  return props.columns.filter(
    (column) =>
      column.sortable !== false &&
      column.key
  )
})

function getByPath(object, path) {
  if (!object || !path) {
    return undefined
  }

  return String(path)
    .split('.')
    .reduce(
      (value, key) => value?.[key],
      object
    )
}

function cellValue(row, column) {
  if (
    typeof column.value ===
    'function'
  ) {
    return column.value(row)
  }

  return getByPath(
    row,
    column.key
  )
}

function sortValue(row, column) {
  if (
    typeof column.sortValue ===
    'function'
  ) {
    return column.sortValue(row)
  }

  return cellValue(row, column)
}

function compareValues(left, right) {
  const leftEmpty =
    left === null ||
    left === undefined ||
    left === ''

  const rightEmpty =
    right === null ||
    right === undefined ||
    right === ''

  if (leftEmpty && rightEmpty) {
    return 0
  }

  if (leftEmpty) {
    return 1
  }

  if (rightEmpty) {
    return -1
  }

  if (
    typeof left === 'number' &&
    typeof right === 'number'
  ) {
    return left - right
  }

  return String(left).localeCompare(
    String(right),
    'ru',
    {
      numeric: true,
      sensitivity: 'base',
    }
  )
}

const sortedRows = computed(() => {
  if (!sortKey.value) {
    return props.rows
  }

  const column =
    props.columns.find(
      (item) =>
        item.key === sortKey.value
    )

  if (!column) {
    return props.rows
  }

  const direction =
    sortDirection.value === 'desc'
      ? -1
      : 1

  return props.rows
    .map((row, index) => ({
      row,
      index,
    }))
    .sort((left, right) => {
      const result =
        compareValues(
          sortValue(
            left.row,
            column
          ),
          sortValue(
            right.row,
            column
          )
        )

      return result === 0
        ? left.index - right.index
        : result * direction
    })
    .map((entry) => entry.row)
})

function toggleSort(column) {
  if (
    column.sortable === false ||
    !column.key
  ) {
    return
  }

  if (sortKey.value !== column.key) {
    sortKey.value = column.key
    sortDirection.value = 'asc'
  } else if (
    sortDirection.value === 'asc'
  ) {
    sortDirection.value = 'desc'
  } else {
    sortKey.value = ''
    sortDirection.value = 'asc'
  }

  emit(
    'sort-change',
    sortKey.value
      ? {
          key: sortKey.value,
          direction:
            sortDirection.value,
        }
      : null
  )
}

function sortIcon(column) {
  if (sortKey.value !== column.key) {
    return '↕'
  }

  return sortDirection.value === 'asc'
    ? '↑'
    : '↓'
}

function ariaSort(column) {
  if (
    column.sortable === false ||
    !column.key
  ) {
    return undefined
  }

  if (sortKey.value !== column.key) {
    return 'none'
  }

  return sortDirection.value === 'asc'
    ? 'ascending'
    : 'descending'
}

function rowKeyValue(row, index) {
  if (
    typeof props.rowKey ===
    'function'
  ) {
    return props.rowKey(row, index)
  }

  return (
    getByPath(row, props.rowKey) ??
    index
  )
}
</script>

<template>
  <div class="ui-table">
    <div
      v-if="sortableColumns.length"
      class="ui-table__mobile-sort"
    >
      <span class="ui-table__mobile-sort-label">
        Сортировка:
      </span>

      <button
        v-for="column in sortableColumns"
        :key="column.key"
        class="ui-table__mobile-sort-button"
        :class="{
          'ui-table__mobile-sort-button--active':
            sortKey === column.key,
        }"
        type="button"
        @click="toggleSort(column)"
      >
        {{ column.label }}
        <span aria-hidden="true">
          {{ sortIcon(column) }}
        </span>
      </button>
    </div>

    <UiEmptyState
      v-if="loading && !rows.length"
      :description="loadingMessage"
      compact
    />

    <UiEmptyState
      v-else-if="!rows.length"
      :description="emptyMessage"
      compact
    />

    <div
      v-else
      class="ui-table__wrap"
    >
      <table class="ui-table__table">
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :aria-sort="ariaSort(column)"
            >
              <button
                v-if="
                  column.sortable !== false &&
                  column.key
                "
                class="ui-table__sort-button"
                type="button"
                @click="toggleSort(column)"
              >
                <span>{{ column.label }}</span>
                <span
                  class="ui-table__sort-icon"
                  aria-hidden="true"
                >
                  {{ sortIcon(column) }}
                </span>
              </button>

              <span v-else>
                {{ column.label }}
              </span>
            </th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(row, index) in sortedRows"
            :key="rowKeyValue(row, index)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              :data-label="column.label"
            >
              <slot
                :name="`cell-${column.key}`"
                :row="row"
                :column="column"
                :value="cellValue(row, column)"
              >
                {{
                  cellValue(row, column) ?? '—'
                }}
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.ui-table,
.ui-table__wrap,
.ui-table__table {
  width: 100%;
}

.ui-table__wrap {
  overflow-x: auto;

  border: 1px solid var(--border);
  border-radius: 10px;
}

.ui-table__table {
  min-width: 700px;

  border-collapse: collapse;

  color: var(--text);
  background: var(--surface);
}

.ui-table__table th,
.ui-table__table td {
  padding: 11px 12px;

  border-bottom: 1px solid var(--border);

  text-align: left;
  vertical-align: middle;

  font-size: 13px;
}

.ui-table__table th {
  color: var(--text-secondary);
  background: var(--surface-secondary);

  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.ui-table__table tbody tr:last-child td {
  border-bottom: 0;
}

.ui-table__sort-button {
  width: 100%;
  padding: 0;

  display: inline-flex;
  align-items: center;
  gap: 7px;

  color: inherit;
  background: transparent;
  border: 0;

  font: inherit;
  font-weight: inherit;

  cursor: pointer;
}

.ui-table__sort-button:hover {
  color: var(--brand);
}

.ui-table__sort-button:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 4px;
}

.ui-table__sort-icon {
  color: var(--brand);
}

.ui-table__mobile-sort {
  display: none;
}

@media (max-width: 640px) {
  .ui-table__mobile-sort {
    margin-bottom: 10px;
    padding: 9px;

    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    background: var(--surface-secondary);
    border: 1px solid var(--border);
    border-radius: 9px;
  }

  .ui-table__mobile-sort-label {
    width: 100%;

    color: var(--text-secondary);

    font-size: 11px;
    font-weight: 800;
  }

  .ui-table__mobile-sort-button {
    min-height: 31px;
    padding: 5px 8px;

    color: var(--text);
    background: var(--surface);

    border: 1px solid var(--border);
    border-radius: 7px;

    font: inherit;
    font-size: 11px;
    font-weight: 700;

    cursor: pointer;
  }

  .ui-table__mobile-sort-button--active {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
  }

  .ui-table__wrap {
    overflow: visible;
    border: 0;
  }

  .ui-table__table {
    min-width: 0;
    display: block;
    background: transparent;
  }

  .ui-table__table thead {
    display: none;
  }

  .ui-table__table tbody {
    display: grid;
    gap: 10px;
  }

  .ui-table__table tr {
    display: block;

    overflow: hidden;

    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
  }

  .ui-table__table td {
    min-height: 40px;
    padding: 9px 11px;

    display: grid;
    grid-template-columns:
      minmax(100px, 38%) 1fr;
    gap: 10px;

    border-bottom: 1px solid var(--border);
  }

  .ui-table__table td::before {
    content: attr(data-label);

    color: var(--text-secondary);

    font-size: 11px;
    font-weight: 800;
  }

  .ui-table__table td:last-child {
    border-bottom: 0;
  }
}
</style>
