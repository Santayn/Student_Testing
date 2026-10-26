import { describe, expect, it } from 'vitest'

import { useLectureMaterials } from '@/composables/lectures/useLectureMaterials'

function materials() {
  return useLectureMaterials({
    lectureId: 5,
    lecturesApi: {},
    ensureSelectedMembershipActive: async () => {},
    formError: { value: '' },
    getApiErrorMessage: () => '',
  })
}

describe('scans are attached when the lecture is saved', () => {
  it('keeps recognised scans in pending files next to manually picked files', () => {
    const state = materials()
    const scan = new File(['img'], 'page.jpg')
    const manual = new File(['pdf'], 'notes.pdf')
    const other = new File(['doc'], 'plan.docx')

    state.addScanFiles([scan])
    state.setPendingFiles([manual])
    expect(state.pendingFiles.value).toEqual([scan, manual])

    state.setPendingFiles([other])
    expect(state.pendingFiles.value).toEqual([scan, other])
    expect(state.pendingFilesDirty.value).toBe(true)

    state.resetMaterials()
    expect(state.pendingFiles.value).toEqual([])
  })
})
