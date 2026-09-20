<script setup>
import { computed, ref } from 'vue'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  loading: { type: Boolean, default: false },
  loadingMessage: { type: String, default: 'Загрузка...' },
  emptyMessage: { type: String, default: 'Нет данных.' },
  defaultSort: { type: Object, default: null },
  paginator: { type: Boolean, default: false },
  pageSize: { type: Number, default: 10 },
  pageSizeOptions: { type: Array, default: () => [10, 20, 50] },
  striped: { type: Boolean, default: false },
})

const emit = defineEmits(['sort-change', 'row-click'])
const sortKey = ref(props.defaultSort?.key ?? '')
const sortDirection = ref(props.defaultSort?.direction === 'desc' ? 'desc' : 'asc')

function getByPath(object, path) {
  if (!object || !path) return undefined
  return String(path).split('.').reduce((value, key) => value?.[key], object)
}

function cellValue(row, column) {
  if (typeof column.value === 'function') return column.value(row)
  return getByPath(row, column.key)
}

function sortValue(row, column) {
  if (typeof column.sortValue === 'function') return column.sortValue(row)
  return cellValue(row, column)
}

function compareValues(left, right) {
  const leftEmpty = left === null || left === undefined || left === ''
  const rightEmpty = right === null || right === undefined || right === ''
  if (leftEmpty && rightEmpty) return 0
  if (leftEmpty) return 1
  if (rightEmpty) return -1
  if (typeof left === 'number' && typeof right === 'number') return left - right
  return String(left).localeCompare(String(right), 'ru', { numeric: true, sensitivity: 'base' })
}

const sortedRows = computed(() => {
  if (!sortKey.value) return props.rows
  const column = props.columns.find((item) => item.key === sortKey.value)
  if (!column) return props.rows
  const direction = sortDirection.value === 'desc' ? -1 : 1
  return props.rows
    .map((row, index) => ({ row, index }))
    .sort((left, right) => {
      const result = compareValues(sortValue(left.row, column), sortValue(right.row, column))
      return result === 0 ? left.index - right.index : result * direction
    })
    .map((entry) => entry.row)
})

function toggleSort(column) {
  if (column.sortable === false || !column.key) return
  if (sortKey.value !== column.key) {
    sortKey.value = column.key
    sortDirection.value = 'asc'
  } else if (sortDirection.value === 'asc') {
    sortDirection.value = 'desc'
  } else {
    sortKey.value = ''
    sortDirection.value = 'asc'
  }
  emit('sort-change', sortKey.value ? { key: sortKey.value, direction: sortDirection.value } : null)
}

function sortIcon(column) {
  if (sortKey.value !== column.key) return 'pi pi-sort-alt'
  return sortDirection.value === 'asc' ? 'pi pi-sort-amount-up-alt' : 'pi pi-sort-amount-down'
}

function rowKeyValue(row, index) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, index)
  return getByPath(row, props.rowKey) ?? index
}
</script>

<template>
  <DataTable
    class="st-ui-table"
    :value="sortedRows"
    :data-key="typeof rowKey === 'string' ? rowKey : undefined"
    :loading="loading"
    :paginator="paginator"
    :rows="pageSize"
    :rows-per-page-options="pageSizeOptions"
    :striped-rows="striped"
    row-hover
    @row-click="emit('row-click', $event)"
  >
    <template v-if="$slots.header" #header><slot name="header" /></template>

    <Column
      v-for="column in columns"
      :key="column.key"
      :field="column.key"
      :header="column.label"
      :style="column.style"
      :header-style="column.headerStyle"
      :body-style="column.bodyStyle"
    >
      <template #header>
        <button
          v-if="column.sortable !== false && column.key"
          type="button"
          class="st-ui-table__sort-button"
          :class="{ 'st-ui-table__sort-button--active': sortKey === column.key }"
          @click="toggleSort(column)"
        >
          <span>{{ column.label }}</span>
          <i class="st-ui-table__sort-icon" :class="sortIcon(column)" aria-hidden="true" />
        </button>
        <span v-else>{{ column.label }}</span>
      </template>

      <template #body="slotProps">
        <slot
          :name="`cell-${column.key}`"
          :row="slotProps.data"
          :column="column"
          :value="cellValue(slotProps.data, column)"
          :index="slotProps.index"
        >
          {{ cellValue(slotProps.data, column) ?? '—' }}
        </slot>
      </template>
    </Column>

    <template #empty>
      <div class="st-ui-table__empty">
        {{ loading ? loadingMessage : emptyMessage }}
      </div>
    </template>
  </DataTable>
</template>
