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

test('login mode tabs expose linked panels and roving keyboard navigation', () => {
  assert.match(source, /id="login-account-tab"[\s\S]*aria-controls="login-account-panel"/)
  assert.match(source, /id="login-scan-tab"[\s\S]*aria-controls="login-scan-panel"/)
  assert.match(source, /:tabindex="loginMode === 'account' \? 0 : -1"/)
  assert.match(source, /:tabindex="loginMode === 'scan' \? 0 : -1"/)
  assert.match(source, /@keydown="handleLoginModeKeydown\(\$event, 'account'\)"/)
  assert.match(source, /@keydown="handleLoginModeKeydown\(\$event, 'scan'\)"/)
  assert.match(source, /id="login-account-panel"[\s\S]*role="tabpanel"[\s\S]*aria-labelledby="login-account-tab"/)
  assert.match(source, /id="login-scan-panel"[\s\S]*role="tabpanel"[\s\S]*aria-labelledby="login-scan-tab"/)
  assert.match(source, /function handleLoginModeKeydown\(event, currentMode\)/)
  assert.match(source, /case 'ArrowRight':/)
  assert.match(source, /case 'ArrowLeft':/)
  assert.match(source, /case 'Home':/)
  assert.match(source, /case 'End':/)
  assert.match(source, /event\.preventDefault\(\)/)
  assert.match(source, /focusLoginModeTab\(nextMode\)/)
})
