export async function runBatchOperation(items, operation) {
  const entries = Array.from(items ?? [])
  const settled = await Promise.allSettled(
    entries.map((item, index) => operation(item, index))
  )

  const successes = []
  const failures = []

  settled.forEach((result, index) => {
    const item = entries[index]

    if (result.status === 'fulfilled') {
      successes.push({
        item,
        value: result.value,
      })
      return
    }

    failures.push({
      item,
      error: result.reason,
    })
  })

  return {
    totalCount: entries.length,
    successCount: successes.length,
    failureCount: failures.length,
    successes,
    failures,
  }
}
