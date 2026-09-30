import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const app = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

test('session refresh waits for initial routing while preserving the no-token login path', () => {
  const readyIndex = app.indexOf('await router.isReady()')
  const tokenGuardIndex = app.indexOf('if (!userStore.token)')
  const refreshIndex = app.indexOf('const session = await getCurrentSession()')
  assert.ok(readyIndex >= 0)
  assert.ok(tokenGuardIndex > readyIndex)
  assert.ok(refreshIndex > tokenGuardIndex)
})
