import {
  inject,
  provide,
  reactive,
  toValue,
  watchEffect,
} from 'vue'

export const BREADCRUMB_CONTEXT_KEY = Symbol(
  'breadcrumb-context'
)

function positiveId(value) {
  const id = Number(value)

  return Number.isFinite(id) && id > 0
    ? id
    : null
}

function nonEmptyText(value) {
  if (typeof value !== 'string') {
    return null
  }

  const text = value.trim()

  return text || null
}

function routeEntityId(route, key) {
  return positiveId(
    route?.params?.[key] ??
      route?.query?.[key]
  )
}

export function createBreadcrumbContextStore() {
  const subjects = reactive({})
  const lectures = reactive({})
  const tests = reactive({})

  function remember(context = {}) {
    const subjectId = positiveId(
      context.subjectId
    )
    const lectureId = positiveId(
      context.lectureId
    )
    const testId = positiveId(
      context.testId
    )

    const subjectName = nonEmptyText(
      context.subjectName
    )
    const lectureTitle = nonEmptyText(
      context.lectureTitle
    )
    const testTitle = nonEmptyText(
      context.testTitle
    )

    if (subjectId) {
      subjects[subjectId] = {
        ...(subjects[subjectId] ?? {}),
        ...(subjectName
          ? { name: subjectName }
          : {}),
      }
    }

    if (lectureId) {
      lectures[lectureId] = {
        ...(lectures[lectureId] ?? {}),
        ...(lectureTitle
          ? { title: lectureTitle }
          : {}),
        ...(subjectId
          ? { subjectId }
          : {}),
      }
    }

    if (testId) {
      tests[testId] = {
        ...(tests[testId] ?? {}),
        ...(testTitle
          ? { title: testTitle }
          : {}),
        ...(lectureId
          ? { lectureId }
          : {}),
        ...(subjectId
          ? { subjectId }
          : {}),
      }
    }
  }

  function contextForRoute(route) {
    const testId =
      routeEntityId(route, 'testId')

    const testContext = testId
      ? tests[testId]
      : null

    const lectureId =
      routeEntityId(route, 'lectureId') ??
      positiveId(testContext?.lectureId)

    const lectureContext = lectureId
      ? lectures[lectureId]
      : null

    const subjectId =
      routeEntityId(route, 'subjectId') ??
      positiveId(testContext?.subjectId) ??
      positiveId(lectureContext?.subjectId)

    const subjectContext = subjectId
      ? subjects[subjectId]
      : null

    return {
      ...(subjectId
        ? { subjectId }
        : {}),
      ...(subjectContext?.name
        ? {
            subjectName:
              subjectContext.name,
          }
        : {}),
      ...(lectureId
        ? { lectureId }
        : {}),
      ...(lectureContext?.title
        ? {
            lectureTitle:
              lectureContext.title,
          }
        : {}),
      ...(testId
        ? { testId }
        : {}),
      ...(testContext?.title
        ? {
            testTitle:
              testContext.title,
          }
        : {}),
    }
  }

  return {
    remember,
    contextForRoute,
  }
}

export function provideBreadcrumbContext() {
  const store =
    createBreadcrumbContextStore()

  provide(
    BREADCRUMB_CONTEXT_KEY,
    store
  )

  return store
}

export function useBreadcrumbContextStore() {
  return inject(
    BREADCRUMB_CONTEXT_KEY,
    null
  )
}

export function useBreadcrumbContext(source) {
  const store =
    useBreadcrumbContextStore()

  if (!store) {
    return null
  }

  watchEffect(() => {
    store.remember(
      toValue(source) ?? {}
    )
  })

  return store
}
