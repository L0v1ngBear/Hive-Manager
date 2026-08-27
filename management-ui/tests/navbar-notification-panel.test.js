import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const navbar = readFileSync(
  new URL('../src/layout/components/Navbar.vue', import.meta.url),
  'utf8',
)

test('notification panel uses a wrapping card layout instead of an Element button content wrapper', () => {
  assert.match(navbar, /:width="380"/)
  assert.match(navbar, /class="notification-panel"/)
  assert.match(navbar, /class="notification-item__main"/)
  assert.match(navbar, /class="notification-item__description"/)
  assert.match(navbar, /\.notification-item__description\s*\{[\s\S]*?overflow-wrap:\s*anywhere/)
  assert.doesNotMatch(navbar, /<el-button text class="h-auto w-full justify-start p-0 text-left"/)
})

test('notifications synchronize only on an explicit popover open or business changes without polling', () => {
  assert.match(navbar, /handleNotificationShow\(\)[\s\S]*?refreshNotifications\(true, false\)/)
  assert.match(navbar, /listenApprovalChanged\(refreshNotificationsInBackground\)/)
  assert.match(navbar, /listenOrderWarningChanged\(refreshNotificationsInBackground\)/)
  assert.doesNotMatch(navbar, /window\.addEventListener\('focus', refreshNotificationsInBackground\)/)
  assert.doesNotMatch(navbar, /window\.setInterval\(refreshNotificationListInBackground/)
  assert.doesNotMatch(navbar, /\(\) => \[userStore\.currentTenantCode, userStore\.permissions\]/)
  assert.match(navbar, /const notificationsLoading = ref\(false\)/)
})
