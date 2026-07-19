import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/Login.vue', import.meta.url), 'utf8')

test('login uses the approved split layout and preserves both login modes', () => {
  assert.match(source, /class="login-shell"/)
  assert.match(source, /class="login-brand-panel"/)
  assert.match(source, /class="login-auth-panel"/)
  assert.match(source, /const loginMode = ref\('account'\)/)
  assert.match(source, /role="tablist"/)
  assert.match(source, /@click="loginMode = 'account'"/)
  assert.match(source, /@click="loginMode = 'scan'"/)
  assert.match(source, /v-if="loginMode === 'account'"/)
  assert.match(source, /class="login-scan-panel"/)
  assert.match(source, /@submit\.prevent="handleLogin"/)
  assert.match(source, /@click="goJoinOrganization"/)
  assert.match(source, /首次登录 \/ 忘记密码/)
  assert.match(source, /scanSession\.qrCodeDataUrl/)
})

test('login split layout collapses to one column on narrow screens', () => {
  assert.match(source, /\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1\.04fr\)\s+minmax\(0,\s*0\.96fr\)/)
  assert.match(source, /@media \(max-width: 900px\)[\s\S]*\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/)
  assert.match(source, /@media \(max-width: 640px\)[\s\S]*\.login-stage/)
})
