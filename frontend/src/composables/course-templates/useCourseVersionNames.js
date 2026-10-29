import { ref, watch } from 'vue'

import { coursesApi } from '@/api'
import { createLatestRequestGuard } from '@/utils/latestRequest'

/**
 * Names of the course versions referenced by teaching assignments
 * (TeachingAssignment.courseVersionId): versionId → «vN · название».
 * A version the user cannot read stays unnamed (shown by id).
 */
export function useCourseVersionNames(assignments) {
  const versionNames = ref(new Map())
  const request = createLatestRequestGuard()

  async function loadVersionNames(list) {
    const requestId = request.begin()
    const ids = [...new Set((list ?? []).map((item) => Number(item.courseVersionId)).filter(Boolean))]

    if (!ids.length) {
      versionNames.value = new Map()
      return
    }

    const responses = await Promise.all(
      ids.map((id) => coursesApi.getVersion(id).catch(() => null))
    )

    if (!request.isCurrent(requestId)) {
      return
    }

    versionNames.value = new Map(
      ids.map((id, index) => {
        const version = responses[index]?.data
        return [id, version ? `v${version.versionNumber} · ${version.title}` : null]
      })
    )
  }

  function courseVersionName(versionId) {
    return versionNames.value.get(Number(versionId)) ?? `версия ${versionId}`
  }

  watch(assignments, (list) => {
    loadVersionNames(list)
  }, { immediate: true })

  return {
    courseVersionName,
  }
}
