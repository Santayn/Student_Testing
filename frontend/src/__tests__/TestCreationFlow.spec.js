import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  createTestWithAssignments,
  TestCreationFlowError,
} from '@/utils/createTestWithAssignments'

function params(overrides = {}) {
  return {
    createTest: vi.fn().mockResolvedValue({
      data: { id: 101 },
    }),
    createAssignment: vi
      .fn()
      .mockResolvedValue({
        data: { id: 201 },
      }),
    deleteTest: vi.fn().mockResolvedValue(),
    testPayload: { title: 'Test' },
    assignmentIds: [11, 12, 13],
    assignmentPayload: (id) => ({
      teachingAssignmentId: id,
    }),
    ...overrides,
  }
}

describe('test creation flow', () => {
  it('creates assignments sequentially after the test', async () => {
    const calls = []
    const options = params({
      createAssignment: vi.fn(
        async (_testId, payload) => {
          calls.push(
            payload.teachingAssignmentId
          )
          return { data: {} }
        }
      ),
    })

    const result =
      await createTestWithAssignments(
        options
      )

    expect(calls).toEqual([11, 12, 13])
    expect(
      result.createdAssignmentCount
    ).toBe(3)
    expect(
      options.deleteTest
    ).not.toHaveBeenCalled()
  })

  it('rolls back the created test when an assignment fails', async () => {
    const failure = new Error(
      'assignment failed'
    )
    const createAssignment = vi
      .fn()
      .mockResolvedValueOnce({ data: {} })
      .mockRejectedValueOnce(failure)

    const options = params({
      createAssignment,
    })

    await expect(
      createTestWithAssignments(options)
    ).rejects.toMatchObject({
      name: 'TestCreationFlowError',
      testId: 101,
      createdAssignmentCount: 1,
      rollbackSucceeded: true,
      cause: failure,
    })

    expect(createAssignment).toHaveBeenCalledTimes(
      2
    )
    expect(options.deleteTest).toHaveBeenCalledWith(
      101
    )
  })

  it('reports the test id when rollback also fails', async () => {
    const assignmentFailure = new Error(
      'assignment failed'
    )
    const rollbackFailure = new Error(
      'delete failed'
    )
    const options = params({
      createAssignment: vi
        .fn()
        .mockRejectedValue(
          assignmentFailure
        ),
      deleteTest: vi
        .fn()
        .mockRejectedValue(rollbackFailure),
    })

    let thrown
    try {
      await createTestWithAssignments(options)
    } catch (error) {
      thrown = error
    }

    expect(thrown).toBeInstanceOf(
      TestCreationFlowError
    )
    expect(thrown).toMatchObject({
      testId: 101,
      createdAssignmentCount: 0,
      rollbackSucceeded: false,
      cause: assignmentFailure,
      rollbackError: rollbackFailure,
    })
  })
})
