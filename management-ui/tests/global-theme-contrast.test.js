import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const styleSource = readFileSync(new URL('../src/style.css', import.meta.url), 'utf8')
const compact = styleSource.replace(/\s+/g, ' ').toLowerCase()
const sharedThemeFiles = [
  'layout/index.vue',
  'layout/components/Sidebar.vue',
  'layout/components/Navbar.vue',
  'views/Login.vue',
  'views/JoinOrganization.vue',
  'views/ForcePasswordChange.vue',
  'views/legal/LegalPage.vue',
  'components/BusinessTimeCorrectionPanel.vue',
  'components/ComplianceFooter.vue',
  'components/GlobalRequestOverlay.vue',
  'components/TableColumnSettings.vue'
]
const oldThemePattern = /#1f3f5f|#0b1f33|rgba\(31,\s*63,\s*95|rgba\(30,\s*64,\s*104/i

test('global theme exposes the approved teal semantic tokens', () => {
  assert.match(compact, /--color-primary:\s*#0f766e/)
  assert.match(compact, /--color-primary-container:\s*#ccfbf1/)
  assert.match(compact, /--color-on-primary-container:\s*#134e4a/)
  assert.match(compact, /--ys-primary:\s*#0f766e/)
  assert.match(compact, /--ys-primary-dark:\s*#115e59/)
  assert.match(compact, /--ys-on-primary:\s*#ffffff/)
  assert.match(compact, /--ys-disabled-text:\s*#94a3b8/)
  assert.match(compact, /--ys-disabled-bg:\s*#e2e8f0/)
  assert.match(compact, /--el-color-primary:\s*#0f766e/)
  assert.match(compact, /--primary:\s*15 118 110/)
  assert.match(compact, /--on-primary:\s*255 255 255/)
})

test('deep primary surfaces keep text and icons white', () => {
  assert.match(compact, /\.bg-primary[\s\S]*,[\s\S]*\.el-button--primary[\s\S]*,[\s\S]*\.function-action-primary[\s\S]*\{[^}]*color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(compact, /:is\(\.bg-primary,\s*\.el-button--primary,\s*\.function-action-primary\)[^{]*:is\(\.text-primary,\s*\.text-on-surface,\s*\.text-on-surface-variant,\s*\.material-symbols-outlined\)[^{]*\{[^}]*color:\s*var\(--ys-on-primary\)\s*!important/)
})

test('disabled primary controls use neutral foreground and background colors', () => {
  assert.match(compact, /\.el-button--primary\.is-disabled[^{]*\{[^}]*color:\s*var\(--ys-disabled-text\)\s*!important[^}]*background:\s*var\(--ys-disabled-bg\)\s*!important/)
})

test('global compatibility and secondary text rules use semantic theme colors', () => {
  assert.doesNotMatch(compact, /\.text-amber-400[\s\S]*\.text-orange-700\s*\{[^}]*#1f3f5f/)
  assert.match(compact, /\.text-amber-400[\s\S]*\.text-orange-700\s*\{[^}]*color:\s*var\(--ys-primary\)\s*!important/)
  assert.doesNotMatch(compact, /\.function-page-desc\s*\{[^}]*#64748b/)
  assert.match(compact, /\.function-page-desc\s*\{[^}]*color:\s*var\(--el-text-color-regular\)/)
  assert.doesNotMatch(compact, /\.function-page-shell\s+\.responsive-data-table\s+td::before\s*\{[^}]*#64748b/)
  assert.match(compact, /\.function-page-shell\s+\.responsive-data-table\s+td::before\s*\{[^}]*color:\s*var\(--el-text-color-regular\)/)
})

test('shared management surfaces do not hardcode the retired blue theme', () => {
  for (const relativePath of sharedThemeFiles) {
    const source = readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
    assert.doesNotMatch(source, oldThemePattern, relativePath)
  }
})
