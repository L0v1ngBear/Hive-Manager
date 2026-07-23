import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

function read(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const navbar = read('src/layout/components/Navbar.vue')
const authApi = read('src/api/auth.js')
const forcedPasswordChange = read('src/views/ForcePasswordChange.vue')

test('logged-in user menu exposes a complete password change dialog', () => {
  assert.match(navbar, /修改密码/)
  assert.match(navbar, /v-model="passwordDialogVisible"/)
  assert.match(navbar, /v-model="passwordForm\.oldPassword"/)
  assert.match(navbar, /v-model="passwordForm\.newPassword"/)
  assert.match(navbar, /v-model="passwordForm\.confirmPassword"/)
  assert.match(navbar, /changePassword\(\{/)
  assert.match(navbar, /userStore\.logout\(\)[\s\S]*router\.replace\('\/login'\)/)
  for (const component of ['ElDialog', 'ElForm', 'ElFormItem', 'ElInput', 'ElButton']) {
    assert.match(
      navbar,
      new RegExp(`import\\s*\\{[^}]*\\b${component}\\b[^}]*\\}\\s*from\\s*['"]element-plus['"]`, 's')
    )
  }
})

test('password change uses an authenticated non-public API and suppresses duplicate global errors', () => {
  assert.match(authApi, /export function changePassword\(data\)/)
  assert.match(authApi, /url:\s*['"]\/auth\/admin\/password['"]/)
  assert.match(authApi, /url:\s*['"]\/auth\/admin\/password['"][\s\S]*silent:\s*true/)
})

test('forced initial password change relogs in after session revocation', () => {
  assert.match(forcedPasswordChange, /userStore\.markPasswordChanged\(\)/)
  assert.match(forcedPasswordChange, /userStore\.logout\(\)/)
  assert.match(forcedPasswordChange, /router\.replace\('\/login'\)/)
  assert.match(forcedPasswordChange, /请使用新密码重新登录/)
})
