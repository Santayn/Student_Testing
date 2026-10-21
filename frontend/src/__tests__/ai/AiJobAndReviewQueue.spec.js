import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import { effectScope } from 'vue'

const api = vi.hoisted(() => ({
  getJob: vi.fn(),
  getReviewQueue: vi.fn(),
  gradeResponse: vi.fn(),
  acceptResponse: vi.fn(),
  regradeResponse: vi.fn(),
}))

vi.mock('@/api/ai.api', () => ({
  aiApi: { getJob: api.getJob },
}))

vi.mock('@/api', async (importOriginal) => ({
  ...(await importOriginal()),
  resultsApi: {
    getReviewQueue: api.getReviewQueue,
    gradeResponse: api.gradeResponse,
    acceptResponse: api.acceptResponse,
    regradeResponse: api.regradeResponse,
  },
}))

import { useAiJob } from '@/composables/ai/useAiJob'
import { useReviewQueue } from '@/composables/results/useReviewQueue'

describe('AI job polling', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('polls a queued job until it is done', async () => {
    const scope = effectScope()
    const job = scope.run(() => useAiJob({ intervalMs: 100 }))
    api.getJob
      .mockResolvedValueOnce({ data: { id: 3, status: 'running', progress: 'Распознаю страницу 1 из 2' } })
      .mockResolvedValueOnce({ data: { id: 3, status: 'done', result: { markdown: '## Тема' } } })

    const promise = job.track(Promise.resolve({ data: { id: 3, status: 'queued' } }))
    await vi.advanceTimersByTimeAsync(100)
    expect(job.progress.value).toBe('Распознаю страницу 1 из 2')
    expect(job.busy.value).toBe(true)
    await vi.advanceTimersByTimeAsync(100)

    await expect(promise).resolves.toMatchObject({ status: 'done', result: { markdown: '## Тема' } })
    expect(job.busy.value).toBe(false)
    scope.stop()
  })

  it('exposes the failure reason of a failed job', async () => {
    const scope = effectScope()
    const job = scope.run(() => useAiJob({ intervalMs: 100 }))
    api.getJob.mockResolvedValueOnce({ data: { id: 4, status: 'failed', error: 'На сканах не найден текст.' } })

    const promise = job.track(Promise.resolve({ data: { id: 4, status: 'queued' } }))
    await vi.advanceTimersByTimeAsync(100)

    await expect(promise).resolves.toBeNull()
    expect(job.error.value).toBe('На сканах не найден текст.')
    scope.stop()
  })
})

describe('review queue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads the queue and saves a teacher grade', async () => {
    const item = { responseId: 7, awardedPoints: 1, questionPoints: 2, reviewStatus: 'needs_teacher' }
    api.getReviewQueue.mockResolvedValue({ data: { items: [item], counts: { needs_teacher: 1 } } })
    api.gradeResponse.mockResolvedValue({ data: { ...item, awardedPoints: 2, reviewStatus: 'teacher_confirmed' } })
    const scope = effectScope()
    const queue = scope.run(() => useReviewQueue())

    await queue.load()
    expect(api.getReviewQueue).toHaveBeenCalledWith({ status: 'needs_teacher' }, expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(queue.items.value).toHaveLength(1)
    expect(queue.counts.needs_teacher).toBe(1)

    const outcome = await queue.grade(item, '2', '  Хорошо  ')

    expect(api.gradeResponse).toHaveBeenCalledWith(7, { awardedPoints: 2, comment: 'Хорошо' })
    expect(outcome).toEqual({ ok: true, message: 'Оценка сохранена.' })
    expect(api.getReviewQueue).toHaveBeenCalledTimes(2)
    scope.stop()
  })
})
