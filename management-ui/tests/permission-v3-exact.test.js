import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

import { hasPermission } from '../src/utils/permission.js'

const root = fileURLToPath(new URL('..', import.meta.url))

test('permission matcher accepts exact grants and exact denies only', () => {
  assert.equal(hasPermission(['order:list'], 'order:list'), true)
  assert.equal(hasPermission(['order:*'], 'order:list'), false)
  assert.equal(hasPermission(['order:status:producing'], 'order:list'), false)
  assert.equal(hasPermission(['order:list', '!order:list'], 'order:list'), false)
})

test('management UI contains no retired wildcard permission checks', () => {
  const files = [
    'src/utils/permission.js',
    'src/router/index.js',
    'src/layout/components/Navbar.vue',
    'src/layout/components/Sidebar.vue',
    'src/views/function/attendance/attendanceManagement.vue',
    'src/views/function/order/order.vue'
  ]

  for (const file of files) {
    const source = fs.readFileSync(`${root}/${file}`, 'utf8')
    assert.doesNotMatch(source, /(?:order|attendance|dashboard):\*/u, file)
    assert.doesNotMatch(source, /(?:sales|production):order:/u, file)
  }
})
