import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const drawer = readFileSync(
  new URL('../src/views/function/employee/EmployeePermissionDrawer.vue', import.meta.url),
  'utf8'
)
const api = readFileSync(
  new URL('../src/views/function/employee/api/employee.js', import.meta.url),
  'utf8'
)
const employeePage = readFileSync(
  new URL('../src/views/function/employee/employee.vue', import.meta.url),
  'utf8'
)

test('employee permissions use one V3 profile tree and one tri-state control', () => {
  assert.match(drawer, /import\s+\{[^}]*ElInput[^}]*ElTree[^}]*ElSegmented[^}]*\}\s+from 'element-plus'/s)
  assert.equal((drawer.match(/<el-tree\b/g) || []).length, 1)
  assert.match(drawer, /<el-segmented/)
  assert.match(drawer, /继承角色/)
  assert.match(drawer, /个人允许/)
  assert.match(drawer, /个人禁用/)
  assert.match(drawer, /角色来源/)
  assert.match(drawer, /permissionVersion/)
  assert.match(drawer, /grants/)
  assert.match(drawer, /denies/)
  assert.doesNotMatch(drawer, /getAllPermissions/)
  assert.doesNotMatch(drawer, /grantPermissionIds|denyPermissionIds|rolePermissionIds/)
})

test('employee permission API exposes only the new profile contract', () => {
  const updateBlock = api.match(
    /export function updateEmployeePermissionOverrides[\s\S]*?\r?\n}\r?\n/
  )?.[0] || ''
  assert.match(api, /\/emp\/employee\/\$\{id\}\/permission-profile/)
  assert.match(updateBlock, /\/emp\/employee\/\$\{id\}\/permission-overrides/)
  assert.match(updateBlock, /method: 'put'/)
  assert.doesNotMatch(updateBlock, /method: 'post'/)
  assert.doesNotMatch(api, /getEmployeePermissionOverrides/)
})

test('employee permission entry requires the dedicated V3 permission', () => {
  const permissionGuard = employeePage.match(/const canManageEmployeePermissions[^\n]+/)?.[0] || ''
  assert.match(permissionGuard, /employee:permission:manage/)
  assert.doesNotMatch(permissionGuard, /employee:update|role:permission:list/)
})
