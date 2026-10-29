import { ref } from 'vue'

import { coursesApi } from '@/api'
import { listFromResponse } from '@/utils/apiData'
import { buildCourseTemplateListParams } from '@/utils/courseTemplateContext'
import { createLatestRequestGuard } from '@/utils/latestRequest'
import {
  courseVersionDescription,
  courseVersionLabel,
} from '@/utils/courseVersionAccess'

/**
 * Versions of all course templates of the selected teacher subject — for
 * placing lectures into a version and assigning tests to a version.
 * A failed load leaves the list empty: the feature degrades to "no versions".
 */
export function useSubjectCourseVersions({
  selectedSubjectId,
  selectedMembership,
  authStore,
}) {
  const courseVersions = ref([])
  const loadingCourseVersions = ref(false)
  const request = createLatestRequestGuard()

  async function loadCourseVersions() {
    const requestId = request.begin()
    courseVersions.value = []

    const params = buildCourseTemplateListParams({
      subjectId: selectedSubjectId.value,
      isAdmin: authStore.isAdminMode,
      currentPersonId: authStore.personId,
      selectedMembership: selectedMembership.value,
    })

    if (!params) {
      loadingCourseVersions.value = false
      return
    }

    loadingCourseVersions.value = true

    try {
      const templates = listFromResponse(await coursesApi.getTemplates(params))
      const lists = await Promise.all(
        templates.map((template) => coursesApi.getVersions(template.id))
      )

      if (!request.isCurrent(requestId)) {
        return
      }

      courseVersions.value = templates
        .flatMap((template, index) =>
          listFromResponse(lists[index]).map((version) => ({
            ...version,
            templateName: template.name,
            label: courseVersionLabel(version, template.name),
            description: courseVersionDescription(version),
          }))
        )
        .sort((left, right) =>
          String(left.templateName ?? '').localeCompare(String(right.templateName ?? ''), 'ru') ||
          Number(right.versionNumber ?? 0) - Number(left.versionNumber ?? 0)
        )
    } catch {
      if (request.isCurrent(requestId)) {
        courseVersions.value = []
      }
    } finally {
      if (request.isCurrent(requestId)) {
        loadingCourseVersions.value = false
      }
    }
  }

  function courseVersionById(versionId) {
    return courseVersions.value.find(
      (version) => Number(version.id) === Number(versionId)
    ) ?? null
  }

  return {
    courseVersions,
    loadingCourseVersions,
    loadCourseVersions,
    courseVersionById,
  }
}
