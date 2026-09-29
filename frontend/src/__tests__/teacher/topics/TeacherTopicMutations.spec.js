import { reactive, ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const {
  create,
  update,
  remove,
} = vi.hoisted(() => ({
  create: vi.fn(),
  update: vi.fn(),
  remove: vi.fn(),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (error, fallback) =>
    error?.message || fallback,
  topicsApi: {
    create,
    update,
    remove,
  },
}))

import {
  useTeacherTopicMutations,
} from '@/composables/teacher/useTeacherTopicMutations'

function createState({ id = null } = {}) {
  const form = reactive({
    id,
    ordinal: 2,
    name: ' Графы ',
    description: ' Теория графов ',
  })
  const selectedMembership = ref({ id: 10, subjectId: 20 })
  const topics = ref([{ id: 1, ordinal: 1, name: 'Основы' }])
  const notice = ref({ type: 'info', message: '' })
  const ensureSelectedMembershipActive = vi.fn().mockResolvedValue(undefined)
  const beginSaving = vi.fn()
  const finishSaving = vi.fn()
  const failSaving = vi.fn()
  const loadTopics = vi.fn().mockResolvedValue(undefined)

  return {
    form,
    selectedMembership,
    topics,
    notice,
    ensureSelectedMembershipActive,
    beginSaving,
    finishSaving,
    failSaving,
    loadTopics,
    state: useTeacherTopicMutations({
      form,
      selectedMembership,
      topics,
      notice,
      ensureSelectedMembershipActive,
      beginSaving,
      finishSaving,
      failSaving,
      loadTopics,
    }),
  }
}

describe('teacher topic mutations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('revalidates membership before creating a normalized topic payload', async () => {
    create.mockResolvedValue({ data: { id: 7 } })
    const ctx = createState()

    await ctx.state.saveTopic()

    expect(ctx.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(create).toHaveBeenCalledWith({
      subjectId: 20,
      courseLectureId: null,
      subjectMembershipId: 10,
      ordinal: 2,
      name: 'Графы',
      description: 'Теория графов',
    })
    expect(ctx.loadTopics).toHaveBeenCalledTimes(1)
    expect(ctx.finishSaving).toHaveBeenCalledWith({ close: true })
  })

  it('updates an existing topic after revalidation', async () => {
    update.mockResolvedValue({ data: {} })
    const ctx = createState({ id: 7 })

    await ctx.state.saveTopic()

    expect(ctx.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(update).toHaveBeenCalledWith(
      7,
      expect.objectContaining({
        subjectMembershipId: 10,
        ordinal: 2,
        name: 'Графы',
      })
    )
  })

  it('blocks duplicate ordinals before any mutation', async () => {
    const ctx = createState()
    ctx.topics.value.push({ id: 9, ordinal: 2, name: 'Дубликат' })

    await ctx.state.saveTopic()

    expect(create).not.toHaveBeenCalled()
    expect(ctx.ensureSelectedMembershipActive).not.toHaveBeenCalled()
    expect(ctx.state.formError.value).toContain('порядковым номером')
  })

  it('revalidates membership before deleting and refreshes the list', async () => {
    remove.mockResolvedValue({ data: {} })
    const ctx = createState()
    const topic = { id: 5, name: 'Удаляемая тема' }

    ctx.state.requestDeleteTopic(topic)
    await ctx.state.deleteTopic()

    expect(ctx.ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(remove).toHaveBeenCalledWith(5)
    expect(ctx.loadTopics).toHaveBeenCalledTimes(1)
    expect(ctx.state.deleteConfirmVisible.value).toBe(false)
  })
})
