import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

// Assignable leaf permissions from V20260713_003_permission_catalog_v3.sql.
const permissionV3Leaves = new Set([
  'dashboard:view',
  'notification:announcement:list',
  'notification:announcement:publish',
  'order:list',
  'order:detail',
  'order:create',
  'order:update',
  'order:print',
  'order:warning:list',
  'order:warning:setting',
  'order:audit:shipment',
  'order:audit:cancel',
  'order:scope:sales:self',
  'order:scope:sales:department',
  'order:scope:production:self',
  'order:scope:production:department',
  'order:scope:assigned',
  'order:scope:installation:department',
  'order:scope:tenant',
  'order:status:budgeting:view',
  'order:status:budgeting:advance',
  'order:status:budgeting:cancel',
  'order:status:budget-completed:view',
  'order:status:pending-confirm:view',
  'order:status:pending-confirm:advance',
  'order:status:pending-confirm:cancel',
  'order:status:pending-pay:view',
  'order:status:pending-pay:advance',
  'order:status:pending-pay:rollback',
  'order:status:pending-pay:cancel',
  'order:status:pending-material:view',
  'order:status:pending-material:advance',
  'order:status:pending-material:rollback',
  'order:status:pending-material:cancel',
  'order:status:producing:view',
  'order:status:producing:advance',
  'order:status:producing:rollback',
  'order:status:producing:cancel',
  'order:status:pending-ship:view',
  'order:status:pending-ship:advance',
  'order:status:pending-ship:rollback',
  'order:status:pending-ship:cancel',
  'order:status:shipped:view',
  'order:status:shipped:advance',
  'order:status:shipped:rollback',
  'order:status:shipped:cancel',
  'order:status:completed:view',
  'order:status:completed:rollback',
  'order:status:pending-cancel:view',
  'order:status:cancelled:view',
  'inventory:list',
  'inventory:detail',
  'inventory:warning:list',
  'inventory:warning:setting',
  'inventory:record:list',
  'inventory:trend',
  'inventory:barcode:search',
  'inventory:model:search',
  'inventory:cloth:in',
  'inventory:cloth:out',
  'inventory:import',
  'inventory:export',
  'print:receipt:list',
  'print:receipt:detail',
  'print:receipt:execute',
  'print:receipt:update',
  'print:receipt:cancel',
  'print:label:list',
  'print:label:detail',
  'print:label:create',
  'print:label:update',
  'print:label:upload',
  'print:label:default',
  'print:label:disable',
  'quality:list',
  'quality:detail',
  'quality:create',
  'quality:update',
  'quality:process',
  'quality:audit',
  'quality:attachment:upload',
  'quality:attachment:download',
  'quality:export',
  'customer:list',
  'customer:detail',
  'customer:create',
  'customer:update',
  'customer:delete',
  'customer:import',
  'customer:export',
  'price:list',
  'price:detail',
  'price:create',
  'price:update',
  'price:publish',
  'price:delete',
  'price:import',
  'price:export',
  'approval:list',
  'approval:leave:list',
  'approval:leave:submit',
  'approval:leave:detail',
  'approval:leave:audit',
  'approval:finance:list',
  'approval:finance:submit',
  'approval:finance:detail',
  'approval:finance:audit',
  'approval:resignation:list',
  'approval:resignation:submit',
  'approval:resignation:detail',
  'approval:resignation:audit',
  'approval:auditor:list',
  'approval:auditor:setting',
  'installation:list',
  'installation:detail',
  'installation:update',
  'installation:attachment:upload',
  'installation:attachment:download',
  'installation:export',
  'attendance:punch',
  'attendance:record:list',
  'attendance:rule:list',
  'attendance:rule:update',
  'attendance:export',
  'equipment:list',
  'equipment:detail',
  'equipment:create',
  'equipment:update',
  'equipment:disable',
  'equipment:inspection:list',
  'equipment:inspection:submit',
  'equipment:export',
  'employee:list',
  'employee:detail',
  'employee:create',
  'employee:update',
  'employee:status',
  'employee:delete',
  'employee:import',
  'employee:export',
  'employee:permission:manage',
  'role:list',
  'role:create',
  'role:update',
  'role:delete',
  'role:permission:list',
  'role:permission:update',
  'document:list',
  'document:folder:create',
  'document:file:upload',
  'document:file:download',
  'document:rename',
  'document:move',
  'document:delete'
])

const root = fileURLToPath(new URL('..', import.meta.url))
const sourceRoot = path.join(root, 'src')
const permissionRoots = new Set([
  ...Array.from(permissionV3Leaves, (code) => code.split(':')[0]),
  'badproduct',
  'label',
  'production',
  'receipt',
  'sales'
])

function sourceFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(directory, entry.name)
    if (entry.isDirectory()) return sourceFiles(absolutePath)
    return /\.(?:js|ts|vue)$/u.test(entry.name) ? [absolutePath] : []
  })
}

test('all management UI permission codes are assignable V3 leaves', () => {
  const invalid = []
  const permissionLiteral = /['"`]([a-z][a-z0-9-]*(?::[a-z0-9-]+)+)['"`]/gu

  for (const file of sourceFiles(sourceRoot)) {
    const source = fs.readFileSync(file, 'utf8')
    for (const match of source.matchAll(permissionLiteral)) {
      const code = match[1]
      if (!permissionRoots.has(code.split(':')[0])) continue
      if (!permissionV3Leaves.has(code)) {
        invalid.push(`${path.relative(root, file)}: ${code}`)
      }
    }
  }

  assert.deepEqual(invalid, [])
})
