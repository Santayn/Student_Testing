import {
  computed,
  getCurrentInstance,
  inject,
  onBeforeUnmount,
  reactive,
  ref,
  toRaw,
} from 'vue'

import {
  matchedRouteKey,
  onBeforeRouteLeave,
} from 'vue-router'

function cloneValue(value) {
  if (typeof structuredClone === 'function') {
    return structuredClone(toRaw(value))
  }
  return JSON.parse(JSON.stringify(toRaw(value)))
}

function normalizedJson(value) {
  const normalize = (entry) => {
    if (Array.isArray(entry)) return entry.map(normalize)
    if (entry && typeof entry === 'object') {
      return Object.keys(entry)
        .sort()
        .reduce((result, key) => {
          result[key] = normalize(entry[key])
          return result
        }, {})
    }
    return entry
  }

  return JSON.stringify(normalize(value))
}

function replaceReactive(target, source) {
  for (const key of Object.keys(target)) delete target[key]
  Object.assign(target, cloneValue(source ?? {}))
}

export function useOverlayForm(options = {}) {
  const {
    createDefault = () => ({}),
    mapEntity = (entity) => entity ?? {},
  } = options

  const isOpen = ref(false)
  const mode = ref('create')
  const saving = ref(false)
  const confirmCloseVisible = ref(false)
  const form = reactive({})
  const baseline = ref('{}')

  const dirty = computed(() => normalizedJson(form) !== baseline.value)
  const isCreate = computed(() => mode.value === 'create')
  const isEdit = computed(() => mode.value === 'edit')

  const model = computed({
    get: () => isOpen.value,
    set: (nextValue) => {
      if (nextValue) {
        isOpen.value = true
        return
      }
      requestClose()
    },
  })

  function setBaseline() {
    baseline.value = normalizedJson(form)
  }

  function openCreate(initialValues = {}) {
    mode.value = 'create'
    saving.value = false
    confirmCloseVisible.value = false
    replaceReactive(form, {
      ...cloneValue(createDefault()),
      ...cloneValue(initialValues),
    })
    setBaseline()
    isOpen.value = true
  }

  function openEdit(entity) {
    mode.value = 'edit'
    saving.value = false
    confirmCloseVisible.value = false
    replaceReactive(form, mapEntity(entity))
    setBaseline()
    isOpen.value = true
  }

  // Уход со страницы (меню, «Назад» браузера) при открытой изменённой форме
  // спрашивает о том же, что и закрытие формы; ответ решает судьбу перехода.
  let pendingLeave = null

  function settleLeave(allow) {
    if (!pendingLeave) return
    const resolve = pendingLeave
    pendingLeave = null
    resolve(allow)
  }

  function hasUnsavedOpenForm() {
    return isOpen.value && dirty.value && !saving.value
  }

  function handleBeforeUnload(event) {
    if (!hasUnsavedOpenForm()) return
    event.preventDefault()
    event.returnValue = ''
  }

  if (getCurrentInstance()) {
    // Гард маршрута — только в компонентах внутри router-view (в юнит-тестах роутера может не быть).
    if (inject(matchedRouteKey, null)) {
      onBeforeRouteLeave(() => {
        if (!hasUnsavedOpenForm()) return true
        settleLeave(false)
        confirmCloseVisible.value = true
        return new Promise((resolve) => {
          pendingLeave = resolve
        })
      })
    }
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', handleBeforeUnload)
      onBeforeUnmount(() => {
        window.removeEventListener('beforeunload', handleBeforeUnload)
        settleLeave(false)
      })
    }
  }

  function requestClose() {
    if (saving.value) return false
    if (dirty.value) {
      confirmCloseVisible.value = true
      return false
    }
    closeImmediately()
    return true
  }

  function closeImmediately() {
    confirmCloseVisible.value = false
    isOpen.value = false
  }

  function discardAndClose() {
    replaceReactive(form, JSON.parse(baseline.value))
    closeImmediately()
    settleLeave(true)
  }

  function continueEditing() {
    confirmCloseVisible.value = false
    settleLeave(false)
  }

  function beginSaving() {
    saving.value = true
  }

  function finishSaving({ close = true, values } = {}) {
    if (values !== undefined) replaceReactive(form, values)
    saving.value = false
    setBaseline()
    if (close) closeImmediately()
  }

  function failSaving() {
    saving.value = false
  }

  function markClean(values) {
    if (values !== undefined) replaceReactive(form, values)
    setBaseline()
  }

  function resetToBaseline() {
    replaceReactive(form, JSON.parse(baseline.value))
  }

  return {
    form,
    model,
    isOpen,
    mode,
    isCreate,
    isEdit,
    dirty,
    saving,
    confirmCloseVisible,
    openCreate,
    openEdit,
    requestClose,
    closeImmediately,
    discardAndClose,
    continueEditing,
    beginSaving,
    finishSaving,
    failSaving,
    markClean,
    resetToBaseline,
  }
}
