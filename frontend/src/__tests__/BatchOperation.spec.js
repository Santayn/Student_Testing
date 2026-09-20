import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  runBatchOperation,
} from '@/utils/batchOperation'

describe('batch operation', () => {
  it('keeps successful results when another item fails', async () => {
    const operation = vi.fn(async (item) => {
      if (item === 2) {
        throw new Error('second failed')
      }

      return `saved-${item}`
    })

    const result = await runBatchOperation(
      [1, 2, 3],
      operation
    )

    expect(operation).toHaveBeenCalledTimes(3)
    expect(result.totalCount).toBe(3)
    expect(result.successCount).toBe(2)
    expect(result.failureCount).toBe(1)
    expect(result.successes).toEqual([
      {
        item: 1,
        value: 'saved-1',
      },
      {
        item: 3,
        value: 'saved-3',
      },
    ])
    expect(result.failures).toHaveLength(1)
    expect(result.failures[0].item).toBe(2)
    expect(result.failures[0].error).toBeInstanceOf(Error)
  })

  it('returns a normal report when every item fails', async () => {
    const result = await runBatchOperation(
      [10, 20],
      async (item) => {
        throw new Error(`failed-${item}`)
      }
    )

    expect(result.successCount).toBe(0)
    expect(result.failureCount).toBe(2)
    expect(
      result.failures.map(({ item }) => item)
    ).toEqual([10, 20])
  })

  it('handles an empty batch without calling the operation', async () => {
    const operation = vi.fn()

    const result = await runBatchOperation(
      [],
      operation
    )

    expect(operation).not.toHaveBeenCalled()
    expect(result).toEqual({
      totalCount: 0,
      successCount: 0,
      failureCount: 0,
      successes: [],
      failures: [],
    })
  })
})
