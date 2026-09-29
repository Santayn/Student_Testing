import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  lecturesApi,
  testsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createAbortableRequestGuard,
} from '@/utils/latestRequest'

export function useLectureManagementData({
  selectedMembership,
  selectedSubjectId,
  route,
  notice,
  onOpenRouteLecture = async () => {},
}) {
  const lectures = ref([])
  const availableTests = ref([])
  const lectureTestsById = ref(new Map())
  const loading = ref(false)
  const handledRouteLectureKey = ref('')

  const searchQuery = ref('')
  const visibilityFilter = ref('all')
  const testFilter = ref('all')
  const sortMode = ref('ordinal')

  const lecturesRequest = createAbortableRequestGuard()

  const visibilityOptions = [
    { value: 'all', label: 'Все лекции' },
    { value: 'visible', label: 'Опубликованные' },
    { value: 'hidden', label: 'Скрытые' },
  ]

  const testFilterOptions = [
    { value: 'all', label: 'Все связи с тестами' },
    { value: 'with-tests', label: 'Есть связанные тесты' },
    { value: 'without-tests', label: 'Без связанных тестов' },
  ]

  const sortOptions = [
    { value: 'ordinal', label: 'По порядку' },
    { value: 'title-asc', label: 'Название А–Я' },
    { value: 'title-desc', label: 'Название Я–А' },
  ]

  function lectureTests(lectureId) {
    return lectureTestsById.value.get(Number(lectureId)) ?? []
  }

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      visibilityFilter.value !== 'all' ||
      testFilter.value !== 'all' ||
      sortMode.value !== 'ordinal'
  })

  const filteredLectures = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = lectures.value.filter((lecture) => {
      if (
        visibilityFilter.value === 'visible' &&
        !lecture.publicVisible
      ) {
        return false
      }

      if (
        visibilityFilter.value === 'hidden' &&
        lecture.publicVisible
      ) {
        return false
      }

      const linkedTests = lectureTests(lecture.id)

      if (
        testFilter.value === 'with-tests' &&
        !linkedTests.length
      ) {
        return false
      }

      if (
        testFilter.value === 'without-tests' &&
        linkedTests.length
      ) {
        return false
      }

      if (!query) {
        return true
      }

      const haystack = [
        lecture.id,
        lecture.ordinal,
        lecture.title,
        lecture.description,
        ...linkedTests.map((test) => test.title),
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')

      return haystack.includes(query)
    })

    return [...result].sort((left, right) => {
      if (sortMode.value === 'title-asc') {
        return String(left.title ?? '').localeCompare(
          String(right.title ?? ''),
          'ru'
        )
      }

      if (sortMode.value === 'title-desc') {
        return String(right.title ?? '').localeCompare(
          String(left.title ?? ''),
          'ru'
        )
      }

      return (
        Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0) ||
        String(left.title ?? '').localeCompare(
          String(right.title ?? ''),
          'ru'
        )
      )
    })
  })

  const filterResultText = computed(() => {
    if (!selectedMembership.value) {
      return 'Сначала выберите предмет преподавателя.'
    }

    return `Показано: ${filteredLectures.value.length} из ${lectures.value.length}`
  })

  function lectureTestSummary(lectureId) {
    const tests = lectureTests(lectureId)

    if (!tests.length) {
      return 'Нет связанных тестов'
    }

    return tests
      .map((test) => test.title || `Тест #${test.id}`)
      .join(', ')
  }

  function resetFilters() {
    searchQuery.value = ''
    visibilityFilter.value = 'all'
    testFilter.value = 'all'
    sortMode.value = 'ordinal'
  }

  async function loadLectures({ openRouteLecture = false } = {}) {
    const { requestId, signal } = lecturesRequest.begin()

    const membershipId = Number(selectedMembership.value?.id ?? 0)
    const subjectId = Number(selectedSubjectId.value ?? 0)

    lectures.value = []
    availableTests.value = []
    lectureTestsById.value = new Map()

    if (!membershipId) {
      loading.value = false
      return
    }

    loading.value = true

    try {
      const lecturesResponse = await lecturesApi.getAll(
        { subjectMembershipId: membershipId },
        { signal }
      )

      const nextLectures = listFromResponse(lecturesResponse)
        .sort(
          (left, right) =>
            Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0) ||
            String(left.title ?? '').localeCompare(
              String(right.title ?? ''),
              'ru'
            )
        )

      const [testsResponse, testLists] = await Promise.all([
        testsApi.getAll({ subjectId }, { signal }),
        Promise.all(
          nextLectures.map((lecture) =>
            lecturesApi.getTests(lecture.id, { signal })
          )
        ),
      ])

      if (!lecturesRequest.isCurrent(requestId)) {
        return
      }

      lectures.value = nextLectures
      availableTests.value = listFromResponse(testsResponse)
        .sort((left, right) =>
          String(left.title ?? '').localeCompare(
            String(right.title ?? ''),
            'ru'
          )
        )

      lectureTestsById.value = new Map(
        nextLectures.map((lecture, index) => [
          Number(lecture.id),
          listFromResponse(testLists[index]),
        ])
      )

      if (openRouteLecture && route.query.lectureId) {
        const routeLectureKey = `${membershipId}:${route.query.lectureId}`

        if (handledRouteLectureKey.value !== routeLectureKey) {
          handledRouteLectureKey.value = routeLectureKey

          const lecture = nextLectures.find(
            (item) => String(item.id) === String(route.query.lectureId)
          )

          if (lecture) {
            await onOpenRouteLecture(lecture)
          } else {
            notice.value = {
              type: 'info',
              message: 'Лекция из ссылки не найдена в выбранном назначении преподавателя.',
            }
          }
        }
      }
    } catch (error) {
      if (!lecturesRequest.isCurrent(requestId)) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить лекции'
        ),
      }
    } finally {
      if (lecturesRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  function resetRouteLectureHandling() {
    handledRouteLectureKey.value = ''
  }

  function dispose() {
    lecturesRequest.invalidate()
  }

  return {
    lectures,
    availableTests,
    lectureTestsById,
    loading,
    searchQuery,
    visibilityFilter,
    testFilter,
    sortMode,
    visibilityOptions,
    testFilterOptions,
    sortOptions,
    hasActiveFilters,
    filteredLectures,
    filterResultText,
    lectureTests,
    lectureTestSummary,
    resetFilters,
    loadLectures,
    resetRouteLectureHandling,
    dispose,
  }
}
