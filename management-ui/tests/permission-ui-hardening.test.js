import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'

function read(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertContains(file, expected, message) {
  assert.ok(read(file).includes(expected), `${message}: ${file}`)
}

const badProductView = 'src/views/function/badProduct/badProduct.vue'
assertContains(badProductView, `v-permission="'badproduct:save'"`, 'quality save actions must use unified disabled permission UI')
assertContains(badProductView, `v-permission="'badproduct:process'"`, 'quality process actions must use unified disabled permission UI')

const approvalView = 'src/views/function/approval/approvalCenter.vue'
assertContains(approvalView, `badproduct:process`, 'approval center must include quality audit permission checks')
assertContains(approvalView, `approval:finance:audit`, 'approval owner configuration must be permission-gated')
assertContains(approvalView, `approval:finance:submit`, 'finance submit UI must be permission-gated')
assertContains(approvalView, `approval:resignation:submit`, 'resignation submit UI must be permission-gated')
assertContains(approvalView, 'accessibleTabs', 'approval tabs must hide concrete list content by permission')
assertContains(approvalView, 'activeTabMeta', 'approval list loading must be based on the active tab permission')

const router = 'src/router/index.js'
assertContains(router, `'badproduct:process'`, 'quality audit users must be able to reach approval center')

const sidebar = 'src/layout/components/Sidebar.vue'
assertContains(sidebar, `'badproduct:process'`, 'sidebar approval entry must be visible disabled/enabled for quality audit users')

const navbar = 'src/layout/components/Navbar.vue'
assertContains(navbar, `'badproduct:process'`, 'navbar approval entry must be visible disabled/enabled for quality audit users')
assertContains(navbar, `v-permission="'notification:announcement:publish'"`, 'navbar notification sync button must use unified disabled permission UI')
assertContains(navbar, 'canSyncNotifications', 'navbar notification sync must guard tenant-wide sync calls by permission')

console.log('permission UI hardening checks passed')
