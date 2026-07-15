import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')
const deployRoot = 'C:/Users/HUAWEI/Desktop/hive部署_全新配置'

function read(...segments) {
  return fs.readFileSync(path.join(...segments), 'utf8')
}

test('order notes and material approval are part of the authoritative permission catalog', () => {
  const catalog = read(
    repoRoot,
    'management/src/main/java/my/hive/shared/permission/PermissionCatalogV3.java'
  )
  const migration = read(
    deployRoot,
    'db-migrations/migrations/V20260715_001_order_notes_and_material_approval.sql'
  )
  const manifest = read(deployRoot, 'db-migrations/migration_manifest.txt')

  for (const code of [
    'order:note:view',
    'order:note:create',
    'order:note:update',
    'order:audit:material'
  ]) {
    assert.ok(catalog.includes(code), `missing Permission V3 leaf: ${code}`)
    assert.ok(migration.includes(code), `missing permission seed: ${code}`)
  }

  assert.match(migration, /CREATE TABLE(?: IF NOT EXISTS)? `sales_order_note`/u)
  assert.match(migration, /DROP COLUMN `remark`/u)
  assert.ok(
    manifest.includes('migrations/V20260715_001_order_notes_and_material_approval.sql'),
    'order-note migration must be registered in migration_manifest.txt'
  )
})
