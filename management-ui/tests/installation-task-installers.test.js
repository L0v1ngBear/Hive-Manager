import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

import {
  MAX_INSTALLERS,
  addInstaller,
  buildInstallerPayload,
  cloneInstallers,
  createInstaller,
  installerPreview,
  removeInstaller,
  validateInstallers
} from '../src/views/function/installationTask/installationTaskInstallers.js'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

test('adds blank installers up to 20 and removes the selected row', () => {
  assert.deepEqual(createInstaller(), { name: '', phone: '' })
  const original = [{ name: '甲', phone: '100' }]
  const added = addInstaller(original)
  assert.equal(added.added, true)
  assert.deepEqual(added.installers, [original[0], { name: '', phone: '' }])
  assert.notEqual(added.installers, original)
  assert.deepEqual(removeInstaller(added.installers, 0), [{ name: '', phone: '' }])

  const full = Array.from({ length: MAX_INSTALLERS }, (_, index) => ({ name: `人员${index}`, phone: `${index}` }))
  assert.deepEqual(addInstaller(full), { added: false, installers: full })
})

test('deep clones installer rows for editor refill without retaining response-only fields', () => {
  const source = [{ id: 8, name: '甲', phone: '100', sortOrder: 0 }]
  const result = cloneInstallers(source)
  assert.deepEqual(result, [{ name: '甲', phone: '100' }])
  assert.notEqual(result[0], source[0])
  result[0].name = '已修改'
  assert.equal(source[0].name, '甲')
  assert.deepEqual(cloneInstallers(null), [])
})

test('builds a trimmed installers-only payload in row order', () => {
  assert.deepEqual(buildInstallerPayload([
    { id: 1, name: ' 甲 ', phone: ' 010-1 ', sortOrder: 9 },
    { name: '乙', phone: '100 转 2' }
  ]), [
    { name: '甲', phone: '010-1' },
    { name: '乙', phone: '100 转 2' }
  ])
})

test('validates required fields, lengths, exact duplicate pairs, count, and completed state', () => {
  assert.equal(validateInstallers([], 'production_completed').valid, true)
  assert.match(validateInstallers([], 'completed_accepted').message, /至少需要一名/)
  assert.match(validateInstallers([{ name: '', phone: '100' }], 'production_completed').message, /姓名/)
  assert.match(validateInstallers([{ name: '甲', phone: '' }], 'production_completed').message, /联系电话/)
  assert.match(validateInstallers([{ name: '甲'.repeat(51), phone: '100' }], 'production_completed').message, /50/)
  assert.match(validateInstallers([{ name: '甲', phone: '1'.repeat(41) }], 'production_completed').message, /40/)
  assert.match(validateInstallers([
    { name: '甲', phone: '100' },
    { name: ' 甲 ', phone: ' 100 ' }
  ], 'production_completed').message, /重复/)
  assert.equal(validateInstallers([
    { name: '甲', phone: '100' },
    { name: '乙', phone: '100' }
  ], 'completed_accepted').valid, true)
  assert.match(validateInstallers(
    Array.from({ length: 21 }, (_, index) => ({ name: `人员${index}`, phone: `${index}` })),
    'production_completed'
  ).message, /20/)
})

test('previews the first three installers and reports the remaining count', () => {
  const preview = installerPreview([
    { name: '甲', phone: '100' },
    { name: '乙', phone: '200' },
    { name: '丙', phone: '300' },
    { name: '丁', phone: '400' }
  ])
  assert.deepEqual(preview.visible.map((item) => item.name), ['甲', '乙', '丙'])
  assert.equal(preview.remaining, 1)
  assert.deepEqual(installerPreview([]), { visible: [], remaining: 0 })
})

test('installation task component uses installers array and removes retired request fields', () => {
  const source = fs.readFileSync(
    path.join(repoRoot, 'management-ui/src/views/function/installationTask/installationTask.vue'),
    'utf8'
  )
  assert.match(source, /editorForm\.installers/)
  assert.match(source, /addInstallerRow/)
  assert.match(source, /removeInstallerRow/)
  assert.match(source, /installers:\s*buildInstallerPayload/)
  assert.match(source, /另有/)
  assert.doesNotMatch(source, /constructionPersonnel/)
  assert.doesNotMatch(source, /constructionPhone/)
})

test('active source and living installation contracts contain no retired single-person field names', () => {
  const retiredNames = [`construction${'Personnel'}`, `construction${'Phone'}`]
  const roots = [
    'management/src/main/java',
    'management-ui/src',
    'docs/management-ui/modules/installation-task.md',
    'docs/api/unified-api-catalog.md',
    'docs/migrations/unified-backend-migrations.md'
  ]
  for (const relativePath of roots.flatMap((entry) => collectFiles(path.join(repoRoot, entry)))) {
    const source = fs.readFileSync(relativePath, 'utf8')
    for (const retiredName of retiredNames) {
      assert.ok(!source.includes(retiredName), `${path.relative(repoRoot, relativePath)} retains ${retiredName}`)
    }
  }
})

function collectFiles(target) {
  const stat = fs.statSync(target)
  if (stat.isFile()) return [target]
  return fs.readdirSync(target, { withFileTypes: true }).flatMap((entry) => {
    const child = path.join(target, entry.name)
    return entry.isDirectory() ? collectFiles(child) : [child]
  })
}
