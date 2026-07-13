import assert from 'node:assert/strict'
import test from 'node:test'
import { createLatestLoadingController } from '../src/views/function/tenant/tenantRequestState.js'

test('closing or switching tenant immediately releases logo loading and invalidates old upload', () => {
  let loading = false
  const controller = createLatestLoadingController((value) => { loading = value })
  const uploadA = controller.begin()
  assert.equal(loading, true)
  controller.invalidate()
  assert.equal(loading, false)
  assert.equal(controller.isCurrent(uploadA), false)
})

test('old upload finally cannot close the newer tenant upload loading', () => {
  let loading = false
  const controller = createLatestLoadingController((value) => { loading = value })
  const uploadA = controller.begin()
  controller.invalidate()
  const uploadB = controller.begin()
  controller.finish(uploadA)
  assert.equal(loading, true)
  assert.equal(controller.isCurrent(uploadB), true)
  controller.finish(uploadB)
  assert.equal(loading, false)
})
