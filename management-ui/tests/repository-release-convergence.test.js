import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')
const migrationRoot = path.join(repoRoot, 'db-migrations')
const migrationDirectory = path.join(migrationRoot, 'migrations')

const requiredReleaseMigrations = [
  'V20260715_001_order_notes_and_material_approval.sql',
  'V20260715_002_installation_task_installer.sql',
  'V20260717_001_order_multi_shipment.sql'
]

test('repository owns every migration required by the current release', () => {
  const manifestLines = fs.readFileSync(path.join(migrationRoot, 'migration_manifest.txt'), 'utf8')
    .split(/\r?\n/u)
    .map((entry) => entry.trim())
    .filter(Boolean)
  const manifestEntries = manifestLines.filter((entry) => !entry.startsWith('#'))
  const migrationFiles = fs.readdirSync(migrationDirectory)
    .filter((entry) => entry.endsWith('.sql'))
    .sort()
  const expectedManifestEntries = migrationFiles.map((entry) => `migrations/${entry}`)

  assert.ok(
    manifestLines.every((entry) => entry.startsWith('#') || /^migrations\/[^/]+\.sql$/u.test(entry)),
    'migration manifest contains a malformed line'
  )
  assert.deepEqual(manifestEntries, expectedManifestEntries)
  for (const migration of requiredReleaseMigrations) {
    assert.ok(migrationFiles.includes(migration), `repository migration is missing: ${migration}`)
  }
})

test('repository tests never use a workstation deployment directory as source of truth', () => {
  const contractTests = fs.readdirSync(path.join(uiRoot, 'tests'))
    .filter((filename) => filename.endsWith('.test.js'))
  for (const filename of contractTests) {
    const source = fs.readFileSync(path.join(uiRoot, 'tests', filename), 'utf8')
    assert.doesNotMatch(source, /C:[\\/]Users[\\/]HUAWEI[\\/]Desktop/iu, `${filename} reads a desktop release package`)
  }
})
