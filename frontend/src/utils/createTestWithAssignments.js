export class TestCreationFlowError extends Error {
  constructor({
    cause,
    testId,
    createdAssignmentCount,
    rollbackSucceeded,
    rollbackError = null,
  }) {
    super(
      rollbackSucceeded
        ? 'Test assignment creation failed and the created test was rolled back.'
        : 'Test assignment creation failed and rollback also failed.'
    )

    this.name = 'TestCreationFlowError'
    this.cause = cause
    this.testId = testId
    this.createdAssignmentCount =
      createdAssignmentCount
    this.rollbackSucceeded =
      rollbackSucceeded
    this.rollbackError = rollbackError
  }
}

export async function createTestWithAssignments({
  createTest,
  createAssignment,
  deleteTest,
  testPayload,
  assignmentIds,
  assignmentPayload,
}) {
  const testResponse = await createTest(
    testPayload
  )
  const test = testResponse.data
  let createdAssignmentCount = 0

  try {
    for (const assignmentId of assignmentIds) {
      await createAssignment(
        test.id,
        assignmentPayload(assignmentId)
      )
      createdAssignmentCount += 1
    }

    return {
      test,
      createdAssignmentCount,
    }
  } catch (cause) {
    try {
      await deleteTest(test.id)
    } catch (rollbackError) {
      throw new TestCreationFlowError({
        cause,
        testId: test.id,
        createdAssignmentCount,
        rollbackSucceeded: false,
        rollbackError,
      })
    }

    throw new TestCreationFlowError({
      cause,
      testId: test.id,
      createdAssignmentCount,
      rollbackSucceeded: true,
    })
  }
}
