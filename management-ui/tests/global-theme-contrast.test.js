import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const styleSource = readFileSync(new URL('../src/style.css', import.meta.url), 'utf8')
const compact = styleSource.replace(/\s+/g, ' ').toLowerCase()

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
