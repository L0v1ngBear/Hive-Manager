import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'

function read(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const userStore = read('src/stores/user.js')
const loginMemory = read('src/utils/loginMemory.js')
const loginView = read('src/views/Login.vue')

assert.ok(
  !userStore.includes('resolveWriteStorage'),
  'remember login must not switch auth session storage to localStorage'
)
assert.ok(
  userStore.includes("const readStorageItem = (key, fallback = '') => authStorage.getItem(key) || fallback"),
  'auth session data should be restored only from sessionStorage'
)

for (const key of ['token', 'responseKey', 'expireAt']) {
  assert.ok(
    !userStore.includes(`targetStorage.setItem('${key}'`),
    `${key} must not be written through remember-controlled targetStorage`
  )
  assert.ok(
    userStore.includes(`authStorage.setItem('${key}'`),
    `${key} should be written only to sessionStorage`
  )
}

assert.ok(
  userStore.includes('clearPersistentAuthStorage()'),
  'legacy auth values in localStorage should be cleared after auth writes'
)
assert.ok(loginMemory.includes("const LOGIN_MEMORY_KEY = 'hive.web.login.memory'"))
assert.ok(!loginMemory.includes('token'))
assert.ok(!loginMemory.includes('responseKey'))
assert.ok(!loginView.includes('仅保存登录状态和账号'))
assert.ok(loginView.includes('仅保存账号'))

console.log('auth storage security checks passed')
