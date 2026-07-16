import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const sourceContractPath = path.join(uiRoot, 'tests', 'permission-v3-catalog.test.js')
const retiredPermissionCodes = [
  'table:export',
  'customer:page',
  'customer:add',
  'inventory:record:recent',
  'receipt:print:list',
  'receipt:print:detail',
  'receipt:print:mark',
  'receipt:print:cancel',
  'label:template:list',
  'label:template:detail',
  'label:template:save',
  'label:template:upload',
  'label:template:default',
  'label:template:disable',
  'badproduct:list',
  'badproduct:save',
  'badproduct:process',
  'approval:order:audit',
  'document:breadcrumbs'
]

function parsePermissionCatalog() {
  const source = fs.readFileSync(sourceContractPath, 'utf8')
  const list = source.match(/const permissionV3Leaves = new Set\(\[([\s\S]*?)\]\)/u)?.[1] || ''
  const assignableCodes = new Set(Array.from(list.matchAll(/'([^']+)'/gu), (match) => match[1]))
  assert.ok(assignableCodes.size > 100, 'failed to parse the V3 assignable permission catalog')
  return assignableCodes
}

function isKnownNonPermissionLiteral(value) {
  return value === 'about:blank'
    || /^(?:hover|focus|active|disabled|sm|md|lg|xl|2xl):[a-z0-9-]+$/u.test(value)
    || /^update:[a-z0-9-]+$/u.test(value)
    || /^(?:xlink:href|xmlns:xlink)$/u.test(value)
}

function filesRecursively(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(directory, entry.name)
    if (entry.isDirectory()) return filesRecursively(absolutePath)
    return /\.(?:html|js|css)$/u.test(entry.name) ? [absolutePath] : []
  })
}

test('management UI release artifact contains only assignable V3 permission codes', () => {
  const distRoot = path.resolve(process.env.MANAGEMENT_UI_DIST || path.join(uiRoot, 'dist'))
  assert.ok(fs.existsSync(path.join(distRoot, 'index.html')), `management UI dist is missing: ${distRoot}`)

  const assignableCodes = parsePermissionCatalog()
  const permissionLiteral = /['"`]([a-z][a-z0-9-]*(?::[a-z0-9-]+)+)['"`]/gu
  const invalid = []

  for (const file of filesRecursively(distRoot)) {
    const source = fs.readFileSync(file, 'utf8')
    for (const retiredCode of retiredPermissionCodes) {
      if (source.includes(retiredCode)) {
        invalid.push(`${path.relative(distRoot, file)}: retired ${retiredCode}`)
      }
    }
    for (const match of source.matchAll(permissionLiteral)) {
      const code = match[1]
      if (isKnownNonPermissionLiteral(code)) continue
      if (!assignableCodes.has(code)) {
        invalid.push(`${path.relative(distRoot, file)}: ${code}`)
      }
    }
  }

  assert.deepEqual([...new Set(invalid)].sort(), [])
})
