import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/manual/UserManual.vue', import.meta.url), 'utf8')
const record = readFileSync(new URL('../../docs/management-ui/modules/manual.md', import.meta.url), 'utf8')

test('manual editing surfaces use explicit Element Plus controls', () => {
  for (const tag of ['el-button', 'el-input', 'el-dialog', 'el-empty']) {
    assert.match(source, new RegExp(`<${tag}\\b`))
  }
  for (const imported of ['ElButton', 'ElInput', 'ElDialog', 'ElEmpty', 'ElMessageBox']) {
    assert.match(source, new RegExp(`\\b${imported}\\b`))
  }
  assert.match(source, /v-loading="customManualLoading"/)
  assert.doesNotMatch(source, /<(button|input|textarea)\b/)
  assert.doesNotMatch(source, /window\.confirm/)
})

test('manual preserves its content, API, reset and Markdown contracts', () => {
  for (const contract of [
    'getCustomManual',
    'saveCustomManualContent',
    'resetCustomManualTemplate',
    'hive-full-manual',
    'new Blob',
    'text/markdown'
  ]) {
    assert.match(source, new RegExp(contract))
  }
  assert.match(source, /maxlength="120000"/)
  assert.match(source, /<details\b/)
})

test('manual module record reflects the migrated protected editor', () => {
  assert.match(record, /Element Plus migrated/)
  assert.match(record, /ElDialog/)
  assert.match(record, /ElMessageBox/)
  assert.doesNotMatch(record, /Loading 指令未注册/)
  assert.doesNotMatch(record, /主要是原生 button、input、textarea/)
})
