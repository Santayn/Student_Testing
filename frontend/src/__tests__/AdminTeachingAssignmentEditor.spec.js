import {
  reactive,
  ref,
} from 'vue'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const {
  getTemplates,
  getVersions,
} = vi.hoisted(() => ({
  getTemplates: vi.fn(),
  getVersions: vi.fn(),
}))

vi.mock('@/api', () => ({
  coursesApi: {
    getTemplates,
    getVersions,
  },
  getApiErrorMessage: (_error, fallback) => fallback,
}))

import {
  useAdminTeachingAssignmentEditor,
} from '@/composables/useAdminTeachingAssignmentEditor'

function setup() {
  const context = reactive({
    semester: '1',
    studyCourse: '2',
    academicYear: '2026',
  })
  const memberships = [
    {
      id: 11,
      subjectId: 5,
      personId: 101,
      role: 1,
      status: 1,
      removedAtUtc: null,
    },
    {
      id: 12,
      subjectId: 5,
      personId: 102,
      role: 1,
      status: 2,
      removedAtUtc: null,
    },
  ]
  const assignments = ref([
    {
      id: 90,
      subjectMembershipId: 11,
      groupId: 21,
      loadTypeId: 31,
      studyCourse: 2,
      semester: 1,
      academicYear: 2026,
      hoursPerWeek: 2,
      status: 1,
      notes: '',
    },
  ])
  const groups = ref([
    { id: 21, name: 'КБ-21', code: '21' },
    { id: 22, name: 'КБ-22', code: '22' },
  ])
  const teacherMemberships = ref(memberships)
  const loadTypes = ref([
    { id: 31, name: 'Лекции' },
  ])

  const editor = useAdminTeachingAssignmentEditor({
    context,
    assignments,
    groups,
    teacherMemberships,
    loadTypes,
    membershipById: (id) => memberships.find(
      (item) => Number(item.id) === Number(id)
    ) ?? null,
    teacherMembershipLabel: (item) => `Teacher ${item.id}`,
  })

  return {
    editor,
    assignments,
  }
}

beforeEach(() => {
  getTemplates.mockReset()
  getVersions.mockReset()
})

describe('useAdminTeachingAssignmentEditor', () => {
  it('opens create state from current period and filters eligible teachers/groups', () => {
    const { editor } = setup()

    editor.openCreateAssignment()
    editor.assignmentForm.subjectId = '5'
    editor.groupSearchQuery.value = '22'

    expect(editor.assignmentForm).toMatchObject({
      semester: '1',
      studyCourse: '2',
      academicYear: '2026',
    })
    expect(editor.assignmentDrawerTitle.value).toBe('Новое назначение нагрузки')
    expect(editor.teacherOptionsForForm.value.map((item) => item.value)).toEqual(['11'])
    expect(editor.filteredGroupsForCreate.value.map((item) => item.id)).toEqual([22])
  })

  it('detects duplicate group-period assignments and validates a valid create form', () => {
    const { editor } = setup()

    editor.openCreateAssignment()
    Object.assign(editor.assignmentForm, {
      subjectId: '5',
      subjectMembershipId: '11',
      groupIds: [21],
      loadTypeId: '31',
      hoursPerWeek: '2',
      status: '1',
    })

    expect(editor.groupHasConflict(21)).toBe(true)
    expect(editor.assignmentValidationMessage()).toContain('уже существует')

    editor.assignmentForm.groupIds = [22]
    expect(editor.assignmentValidationMessage()).toBe('')
    expect(editor.assignmentPayload(22)).toMatchObject({
      subjectMembershipId: 11,
      groupId: 22,
      loadTypeId: 31,
      semester: 1,
      studyCourse: 2,
      academicYear: 2026,
      hoursPerWeek: 2,
      status: 1,
    })
  })

  it('keeps the current inactive teacher membership available while editing', async () => {
    const { editor, assignments } = setup()
    assignments.value[0].subjectMembershipId = 12

    getTemplates.mockResolvedValue({ data: [] })

    await editor.openEditAssignment(assignments.value[0])

    expect(editor.assignmentIsCreate.value).toBe(false)
    expect(editor.teacherOptionsForForm.value).toEqual([
      expect.objectContaining({ value: '11', disabled: false }),
      expect.objectContaining({ value: '12', disabled: false }),
    ])
  })

  it('loads course versions for the selected teacher and subject', async () => {
    const { editor } = setup()

    getTemplates.mockResolvedValue({
      data: [{ id: 41, name: 'Основной курс' }],
    })
    getVersions.mockResolvedValue({
      data: [{
        id: 51,
        versionNumber: 3,
        title: 'Осень',
        published: true,
      }],
    })

    editor.openCreateAssignment()
    editor.assignmentForm.subjectId = '5'
    editor.assignmentForm.subjectMembershipId = '11'

    await editor.onAssignmentTeacherChange()

    expect(getTemplates).toHaveBeenCalledWith({
      subjectId: 5,
      authorPersonId: 101,
    })
    expect(getVersions).toHaveBeenCalledWith(41)
    expect(editor.courseVersionOptions.value).toEqual([
      { value: '', label: 'Без версии курса' },
      {
        value: '51',
        label: 'Основной курс · версия 3 · Осень · опубликована',
      },
    ])
  })
})
