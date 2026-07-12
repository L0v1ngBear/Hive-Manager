import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (relativePath) => readFileSync(new URL(relativePath, import.meta.url), 'utf8')

const employeeList = read('../src/views/function/employee/employee.vue')
const employeeEditor = read('../src/views/function/employee/employeeCreate.vue')
const employeePermissionDrawer = read('../src/views/function/employee/EmployeePermissionDrawer.vue')
const attendance = read('../src/views/function/attendance/attendanceManagement.vue')

const assertUsesComponents = (source, components) => {
  for (const component of components) {
    assert.match(source, new RegExp(`<${component}(?:\\s|>)`), `expected ${component}`)
  }
}

test('migrates employee list and editor controls to Element Plus', () => {
  assertUsesComponents(employeeList, ['el-table', 'el-pagination', 'el-date-picker'])
  assertUsesComponents(employeeEditor, ['el-drawer', 'el-form', 'el-input', 'el-select', 'el-date-picker', 'el-radio-group'])
  assert.match(employeeList, /buildEmployeeOrganizationChart/)
  assert.match(employeePermissionDrawer, /<el-tree-select(?:\s|>)/)
})

test('migrates attendance controls to Element Plus', () => {
  assertUsesComponents(attendance, ['el-table', 'el-pagination', 'el-date-picker', 'el-time-picker', 'el-input-number'])
})
