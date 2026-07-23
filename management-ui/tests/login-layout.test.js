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
  assert.match(source, /:hidden="loginMode !== 'account'"/)
  assert.match(source, /class="login-scan-panel"/)
  assert.match(source, /@submit\.prevent="handleLogin"/)
  assert.match(source, /@click="goJoinOrganization"/)
  assert.match(source, /class="login-reset-link"[\s\S]*>忘记密码？<\/a>/)
  assert.doesNotMatch(source, /首次登录/)
  assert.match(source, /scanSession\.qrCodeDataUrl/)
})

test('login split layout collapses to one column on narrow screens', () => {
  assert.match(source, /\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1\.25fr\)\s+minmax\(36rem,\s*1fr\)/)
  assert.match(source, /@media \(max-width: 900px\)[\s\S]*\.login-shell\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/)
  assert.match(source, /@media \(max-width: 900px\)[\s\S]*\.login-brand-panel\s*\{[\s\S]*display:\s*none/)
  assert.match(source, /@media \(max-width: 640px\)[\s\S]*\.login-mobile-lockup\s*\{[\s\S]*display:\s*flex/)
})

test('public login identifies Hive and its developer without exposing tenant branding', () => {
  const site = readFileSync(new URL('../src/config/site.js', import.meta.url), 'utf8')
  const index = readFileSync(new URL('../index.html', import.meta.url), 'utf8')

  assert.match(source, /src="\/logo\.png"/)
  assert.match(source, /开发与技术服务/)
  assert.match(source, /siteConfig\.companyName/)
  assert.match(site, /companyName: '杭州毫端科技有限公司'/)
  assert.match(site, /© 2026 杭州毫端科技有限公司/)
  assert.match(index, /<title>蜂巢 Hive \| 企业信息管理<\/title>/)
  assert.doesNotMatch(`${source}\n${site}\n${index}`, /北京北方新青人窗帘有限公司/)
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

test('login preserves baseline copy and keeps linked panels live for every mode', () => {
  for (const copy of [
    '企业信息管理',
    '专业、高效、可靠、价值，协同工业生产效率。',
    '把经验和流程变成可追踪、可复盘、可优化的数据资产。',
    '快捷登录',
    '使用 Hive 移动端小程序扫码',
    '账号登录',
    '欢迎回来',
    '登录您的 Hive 账户以继续'
  ]) {
    assert.match(source, new RegExp(copy))
  }
  assert.doesNotMatch(source, /让生产协同更清晰、更高效/)
  assert.doesNotMatch(source, /统一业务协同/)
  assert.match(source, /id="login-account-panel"[\s\S]*:hidden="loginMode !== 'account'"/)
  assert.match(source, /id="login-scan-panel"[\s\S]*:hidden="loginMode !== 'scan'"/)
  assert.match(source, /role="status"[\s\S]*aria-live="polite"[\s\S]*aria-atomic="true"/)
})
