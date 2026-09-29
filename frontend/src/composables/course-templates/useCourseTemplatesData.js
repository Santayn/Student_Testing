import {
  computed,
  ref,
} from 'vue'

import {
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  buildCourseTemplateListParams,
} from '@/utils/courseTemplateContext'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

/**
 * Owns read-only course-template workspace state for the selected teacher
 * subject: templates, versions, list filters and stale-request protection.
 * Editing/mutations deliberately stay in CourseTemplatesView.
 */
export function useCourseTemplatesData({
  selectedSubjectId,
  selectedMembership,
  authStore,
  route,
  notice,
  beforeTemplateSelection = () => {},
  onOpenRouteVersion = () => {},
}) {
  const templates = ref([])
  const versions = ref([])
  const selectedTemplateId = ref(null)

  const loading = ref(false)
  const loadingVersions = ref(false)
  const handledRouteVersionKey = ref('')

  const templateSearchQuery = ref('')
  const templateVisibilityFilter = ref('all')
  const templateSortMode = ref('name-asc')
  const versionSearchQuery = ref('')
  const versionPublicationFilter = ref('all')
  const versionSortMode = ref('version-desc')

  const templatesRequest = createLatestRequestGuard()
  const versionsRequest = createLatestRequestGuard()

  const selectedTemplate = computed(() => {
    return templates.value.find(
      (item) =>
        Number(item.id) === Number(selectedTemplateId.value)
    ) ?? null
  })

  const templateVisibilityOptions = [
    { value: 'all', label: 'Все шаблоны' },
    { value: 'visible', label: 'Опубликованные' },
    { value: 'hidden', label: 'Черновики' },
  ]

  const templateSortOptions = [
    { value: 'name-asc', label: 'Название А–Я' },
    { value: 'name-desc', label: 'Название Я–А' },
    { value: 'id-desc', label: 'Сначала новые' },
  ]

  const versionPublicationOptions = [
    { value: 'all', label: 'Все версии' },
    { value: 'published', label: 'Опубликованные' },
    { value: 'draft', label: 'Черновики' },
  ]

  const versionSortOptions = [
    { value: 'version-desc', label: 'Сначала новые версии' },
    { value: 'version-asc', label: 'Сначала старые версии' },
    { value: 'title-asc', label: 'Название А–Я' },
  ]

  const hasActiveTemplateFilters = computed(() => {
    return Boolean(templateSearchQuery.value.trim()) ||
      templateVisibilityFilter.value !== 'all' ||
      templateSortMode.value !== 'name-asc'
  })

  const filteredTemplates = computed(() => {
    const query = templateSearchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = templates.value.filter((template) => {
      if (
        templateVisibilityFilter.value === 'visible' &&
        !template.publicVisible
      ) {
        return false
      }

      if (
        templateVisibilityFilter.value === 'hidden' &&
        template.publicVisible
      ) {
        return false
      }

      if (!query) {
        return true
      }

      return [template.id, template.name]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })

    return [...result].sort((left, right) => {
      if (templateSortMode.value === 'name-desc') {
        return String(right.name ?? '').localeCompare(
          String(left.name ?? ''),
          'ru'
        )
      }

      if (templateSortMode.value === 'id-desc') {
        return Number(right.id ?? 0) - Number(left.id ?? 0)
      }

      return String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    })
  })

  const hasActiveVersionFilters = computed(() => {
    return Boolean(versionSearchQuery.value.trim()) ||
      versionPublicationFilter.value !== 'all' ||
      versionSortMode.value !== 'version-desc'
  })

  const filteredVersions = computed(() => {
    const query = versionSearchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = versions.value.filter((version) => {
      if (
        versionPublicationFilter.value === 'published' &&
        !version.published
      ) {
        return false
      }

      if (
        versionPublicationFilter.value === 'draft' &&
        version.published
      ) {
        return false
      }

      if (!query) {
        return true
      }

      return [
        version.versionNumber,
        version.title,
        version.description,
        version.changeNotes,
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })

    return [...result].sort((left, right) => {
      if (versionSortMode.value === 'version-asc') {
        return Number(left.versionNumber ?? 0) - Number(right.versionNumber ?? 0)
      }

      if (versionSortMode.value === 'title-asc') {
        return String(left.title ?? '').localeCompare(
          String(right.title ?? ''),
          'ru'
        )
      }

      return Number(right.versionNumber ?? 0) - Number(left.versionNumber ?? 0)
    })
  })

  const templateFilterResultText = computed(() => {
    if (!selectedMembership.value || !selectedSubjectId.value) {
      return 'Сначала выберите предмет преподавателя.'
    }

    return `Показано: ${filteredTemplates.value.length} из ${templates.value.length}`
  })

  const versionFilterResultText = computed(() => {
    if (!selectedTemplate.value) {
      return 'Сначала выберите шаблон курса.'
    }

    return `Показано: ${filteredVersions.value.length} из ${versions.value.length}`
  })

  function resetTemplateFilters() {
    templateSearchQuery.value = ''
    templateVisibilityFilter.value = 'all'
    templateSortMode.value = 'name-asc'
  }

  function resetVersionFilters() {
    versionSearchQuery.value = ''
    versionPublicationFilter.value = 'all'
    versionSortMode.value = 'version-desc'
  }

  async function selectTemplate(
    templateId,
    { openRouteVersion = false } = {}
  ) {
    selectedTemplateId.value = Number(templateId)
    beforeTemplateSelection()
    resetVersionFilters()
    await loadVersions({ openRouteVersion })
  }

  async function loadTemplates({
    preferredTemplateId = null,
    openRouteVersion = false,
  } = {}) {
    const requestId = templatesRequest.begin()

    versionsRequest.invalidate()
    loadingVersions.value = false

    const previousTemplateId =
      preferredTemplateId ?? selectedTemplateId.value

    templates.value = []
    versions.value = []
    selectedTemplateId.value = null

    const subjectId = Number(selectedSubjectId.value || 0)

    if (!subjectId) {
      loading.value = false
      return
    }

    loading.value = true

    try {
      const params = buildCourseTemplateListParams({
        subjectId,
        isAdmin: authStore.isAdminMode,
        currentPersonId: authStore.personId,
        selectedMembership: selectedMembership.value,
      })

      if (!params) {
        if (templatesRequest.isCurrent(requestId)) {
          notice.value = {
            type: 'info',
            message: 'Не удалось определить преподавателя для выбранного предмета.',
          }
        }
        return
      }

      const response = await coursesApi.getTemplates(params)

      if (!templatesRequest.isCurrent(requestId)) {
        return
      }

      templates.value = listFromResponse(response)

      const routeTemplateId = route.query.templateId
      const desiredTemplateId =
        preferredTemplateId ?? routeTemplateId ?? previousTemplateId

      if (
        desiredTemplateId &&
        templates.value.some(
          (item) => String(item.id) === String(desiredTemplateId)
        )
      ) {
        await selectTemplate(Number(desiredTemplateId), {
          openRouteVersion,
        })
      } else if (templates.value.length === 1) {
        await selectTemplate(templates.value[0].id, {
          openRouteVersion,
        })
      }
    } catch (error) {
      if (!templatesRequest.isCurrent(requestId)) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить шаблоны курса'
        ),
      }
    } finally {
      if (templatesRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  async function loadVersions({ openRouteVersion = false } = {}) {
    const requestId = versionsRequest.begin()

    versions.value = []

    const templateId = Number(selectedTemplateId.value || 0)

    if (!templateId) {
      loadingVersions.value = false
      return
    }

    loadingVersions.value = true

    try {
      const response = await coursesApi.getVersions(templateId)

      if (!versionsRequest.isCurrent(requestId)) {
        return
      }

      versions.value = listFromResponse(response)
        .sort(
          (left, right) =>
            Number(right.versionNumber ?? 0) -
            Number(left.versionNumber ?? 0)
        )

      if (openRouteVersion && route.query.versionId) {
        const routeVersionKey = `${templateId}:${route.query.versionId}`

        if (handledRouteVersionKey.value !== routeVersionKey) {
          handledRouteVersionKey.value = routeVersionKey

          const version = versions.value.find(
            (item) => String(item.id) === String(route.query.versionId)
          )

          if (version) {
            onOpenRouteVersion(version)
          }
        }
      }
    } catch (error) {
      if (!versionsRequest.isCurrent(requestId)) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить версии курса'
        ),
      }
    } finally {
      if (versionsRequest.isCurrent(requestId)) {
        loadingVersions.value = false
      }
    }
  }

  return {
    templates,
    versions,
    selectedTemplateId,
    selectedTemplate,
    loading,
    loadingVersions,
    handledRouteVersionKey,
    templateSearchQuery,
    templateVisibilityFilter,
    templateSortMode,
    versionSearchQuery,
    versionPublicationFilter,
    versionSortMode,
    templateVisibilityOptions,
    templateSortOptions,
    versionPublicationOptions,
    versionSortOptions,
    hasActiveTemplateFilters,
    filteredTemplates,
    hasActiveVersionFilters,
    filteredVersions,
    templateFilterResultText,
    versionFilterResultText,
    resetTemplateFilters,
    resetVersionFilters,
    loadTemplates,
    selectTemplate,
    loadVersions,
  }
}
