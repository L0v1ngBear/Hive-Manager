import assert from 'node:assert/strict'
import test from 'node:test'

import { createLatestRequest, createSubmitGuard } from '../src/utils/latestRequest.js'

const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

test('only the latest deferred request may commit or finish loading', async () => {
  const latest = createLatestRequest()
  const first = deferred()
  const second = deferred()
  const committed = []
  let loading = false

  const run = async (source) => {
    const request = latest.begin()
    loading = true
    try {
      const value = await source.promise
      request.commit(() => committed.push(value))
    } finally {
      request.commit(() => { loading = false })
    }
  }

  const firstRun = run(first)
  const secondRun = run(second)
  first.resolve('旧条件')
  await firstRun
  assert.deepEqual(committed, [])
  assert.equal(loading, true)
  second.resolve('新条件')
  await secondRun
  assert.deepEqual(committed, ['新条件'])
  assert.equal(loading, false)
})

test('submit guard ignores a second call until the first settles', async () => {
  const guard = createSubmitGuard()
  const pending = deferred()
  let calls = 0
  const submit = () => guard.run(async () => {
    calls += 1
    await pending.promise
  })

  const first = submit()
  const second = submit()
  assert.equal(calls, 1)
  assert.equal(guard.pending, true)
  assert.equal(await second, false)
  pending.resolve()
  assert.equal(await first, true)
  assert.equal(guard.pending, false)
})
