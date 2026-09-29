import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import { ref } from 'vue'

const mocks = vi.hoisted(() => ({
  getSubjectMembership: vi.fn(),
  addLoadTypeToSubjectMembership: vi.fn(),
  createAssignment: vi.fn(),
  updateAssignment: vi.fn(),
  getApiErrorMessage: vi.fn((error, fallback = '') =>
    error?.message || fallback
  ),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: mocks.getApiErrorMessage,
  membershipsApi: {
    getSubjectMembership: mocks.getSubjectMembership,
  },
  teachingApi: {
    addLoadTypeToSubjectMembership:
      mocks.addLoadTypeToSubjectMembership,
    createAssignment: mocks.createAssignment,
    updateAssignment: mocks.updateAssignment,
  },
}))

import {
  useAdminTeachingAssignmentMutations,
} from '@/composables/admin/teaching-assignments/useAdminTeachingAssignmentMutations'

function buildState({
  create = true,
  groupIds = [30],
} = {}) {
  const assignmentForm = {
    id: create ? null : 700,
    subjectMembershipId: 10,
    loadTypeId: 20,
    groupIds,
    groupId: 30,
    studyCourse: 2,
    semester: 1,
    academicYear: 2026,
    status: 1,
  }

  const assignmentIsCreate = ref(create)
  const assignmentSaving = ref(false)
  const assignmentFormError = ref('')
  const assignments = ref(
    create
      ? []
      : [{
          id: 700,
          subjectMembershipId: 10,
          groupId: 30,
        }]
  )
  const groups = ref([
    { id: 30, name: 'КБ-30' },
    { id: 31, name: 'КБ-31' },
  ])
  const context = {
    studyCourse: 2,
    semester: 1,
    academicYear: 2026,
  }

  const beginAssignmentSaving = vi.fn(() => {
    assignmentSaving.value = true
  })
  const finishAssignmentSaving = vi.fn(() => {
    assignmentSaving.value = false
  })
  const failAssignmentSaving = vi.fn(() => {
    assignmentSaving.value = false
  })
  const refreshAssignments = vi.fn()
  const showNotice = vi.fn()
  const assignmentPayload = vi.fn((groupId) => ({
    groupId,
    subjectMembershipId:
      assignmentForm.subjectMembershipId,
  }))

  const state = useAdminTeachingAssignmentMutations({
    assignmentForm,
    assignmentIsCreate,
    assignmentSaving,
    assignmentFormError,
    assignmentValidationMessage: () => '',
    assignmentPayload,
    beginAssignmentSaving,
    finishAssignmentSaving,
    failAssignmentSaving,
    assignments,
    groups,
    groupName: (id) =>
      groups.value.find((item) => item.id === id)?.name ?? String(id),
    context,
    refreshAssignments,
    showNotice,
  })

  return {
    state,
    assignmentForm,
    assignmentFormError,
    refreshAssignments,
    showNotice,
    finishAssignmentSaving,
    failAssignmentSaving,
  }
}

describe('admin teaching assignment mutations', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    mocks.getSubjectMembership.mockResolvedValue({
      data: {
        id: 10,
        role: 1,
        status: 1,
        removedAtUtc: null,
      },
    })
    mocks.addLoadTypeToSubjectMembership.mockResolvedValue({})
    mocks.createAssignment.mockResolvedValue({ id: 1 })
    mocks.updateAssignment.mockResolvedValue({ id: 700 })
  })

  it('revalidates membership and binds load type before creating workload', async () => {
    const { state } = buildState()

    await state.saveAssignment()

    expect(mocks.getSubjectMembership).toHaveBeenCalledWith(10)
    expect(mocks.addLoadTypeToSubjectMembership).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ teachingLoadTypeId: 20 })
    )
    expect(mocks.createAssignment).toHaveBeenCalledTimes(1)
    expect(mocks.getSubjectMembership.mock.invocationCallOrder[0])
      .toBeLessThan(mocks.createAssignment.mock.invocationCallOrder[0])
  })

  it('keeps failed groups selected after partial batch creation', async () => {
    mocks.createAssignment
      .mockResolvedValueOnce({ id: 1 })
      .mockRejectedValueOnce(new Error('conflict'))

    const {
      state,
      assignmentForm,
      assignmentFormError,
      finishAssignmentSaving,
      refreshAssignments,
    } = buildState({ groupIds: [30, 31] })

    await state.saveAssignment()

    expect(refreshAssignments).toHaveBeenCalledTimes(1)
    expect(finishAssignmentSaving).toHaveBeenCalledWith(
      expect.objectContaining({
        close: false,
        values: expect.objectContaining({
          groupIds: [31],
        }),
      })
    )
    expect(assignmentForm.groupIds).toEqual([30, 31])
    expect(assignmentFormError.value).toContain('КБ-31')
  })

  it('updates an existing assignment after revalidation', async () => {
    const {
      state,
      refreshAssignments,
      showNotice,
    } = buildState({ create: false })

    await state.saveAssignment()

    expect(mocks.getSubjectMembership).toHaveBeenCalledWith(10)
    expect(mocks.updateAssignment).toHaveBeenCalledWith(
      700,
      expect.objectContaining({ groupId: 30 })
    )
    expect(refreshAssignments).toHaveBeenCalledTimes(1)
    expect(showNotice).toHaveBeenCalledWith(
      'success',
      'Учебная нагрузка обновлена.'
    )
  })
})
