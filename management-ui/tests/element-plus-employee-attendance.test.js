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

const assertExplicitImports = (source, components) => {
  const importedNames = [...source.matchAll(/import\s*{([\s\S]*?)}\s*from\s*['"]element-plus['"]/g)]
    .map((match) => match[1])
    .join(',')

  for (const component of components) {
    assert.match(importedNames, new RegExp(`(?:^|[,\\s])${component}(?:$|[,\\s])`), `expected explicit ${component} import`)
  }
}

const assertElButtonHandler = (source, handler) => {
  const escapedHandler = handler.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  assert.match(
    source,
    new RegExp(`<el-button\\b(?=[^>]*@click(?:\\.[a-z]+)*=["']${escapedHandler}["'])[^>]*>`),
    `expected ElButton handler ${handler}`
  )
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

test('uses Element Plus options inside employee radio and attendance checkbox groups', () => {
  assertExplicitImports(employeeEditor, ['ElRadio', 'ElRadioGroup'])
  const radioGroups = [...employeeEditor.matchAll(/<el-radio-group\b[\s\S]*?<\/el-radio-group>/g)].map((match) => match[0])
  assert.equal(radioGroups.length, 2)
  for (const group of radioGroups) {
    assert.match(group, /<el-radio(?:\s|>)/)
    assert.doesNotMatch(group, /<input(?:\s|>)/)
  }

  assertExplicitImports(attendance, ['ElCheckbox', 'ElCheckboxGroup'])
  const checkboxGroup = attendance.match(/<el-checkbox-group\b[\s\S]*?<\/el-checkbox-group>/)?.[0] || ''
  assert.match(checkboxGroup, /<el-checkbox(?:\s|>)/)
  assert.doesNotMatch(checkboxGroup, /<input(?:\s|>)/)
})

test('uses ElButton for employee and attendance page commands', () => {
  assertExplicitImports(employeeList, ['ElButton'])
  for (const handler of [
    'openOrganizationDrawer',
    'handleCreateJoinCode',
    'handleTemplateDownload',
    'triggerImport',
    'handleExport',
    'openCreateDrawer'
  ]) {
    assertElButtonHandler(employeeList, handler)
  }

  assertExplicitImports(attendance, ['ElButton'])
  for (const handler of [
    'openRuleDrawer',
    'refreshAll',
    'exportExcel',
    'removeAttendanceLocation(index)',
    'addAttendanceLocation',
    'submitRule'
  ]) {
    assertElButtonHandler(attendance, handler)
  }
  const closeButtons = attendance.match(/<el-button\b(?=[^>]*@click=["']ruleDrawerVisible = false["'])[^>]*>/g) || []
  assert.equal(closeButtons.length, 2)
})

test('removes unreachable legacy tables and their helpers', () => {
  for (const source of [employeeList, attendance]) {
    assert.doesNotMatch(source, /<table\b[^>]*v-if=["']false["']/)
  }
  for (const helper of [
    'employeeTableColumnCount',
    'employeeCellClass',
    'employeeColumnStyle',
    'departmentBadge',
    'pageStart',
    'pageEnd'
  ]) {
    assert.doesNotMatch(employeeList, new RegExp(`\\b${helper}\\b`))
  }
  assert.doesNotMatch(attendance, /\battendanceCellClass\b/)
})

test('keeps employee editor buttons from implicitly submitting the form', () => {
  assert.match(employeeEditor, /<el-form\b(?=[^>]*@submit\.prevent=["']submit["'])[^>]*>/)

  const nativeButtons = [...employeeEditor.matchAll(/<button\b[^>]*>/g)].map((match) => match[0])
  const submitButtons = nativeButtons.filter((button) => /\btype=["']submit["']/.test(button))
  assert.equal(submitButtons.length, 1)
  assert.doesNotMatch(submitButtons[0], /@click(?:\.[a-z]+)*=["']submit["']/)

  for (const button of nativeButtons.filter((button) => !/\btype=["']submit["']/.test(button))) {
    assert.match(button, /\btype=["']button["']/)
  }
})

test('restores configurable employee type columns and the name subtitle', () => {
  assert.match(employeeList, /employeeColumnRenderers[^\n]*employeeType/)
  assert.match(employeeList, /employeeType:\s*['"][^'"]+['"]/)
  assert.match(employeeList, /key === ['"]employeeType['"]\) return formatEmployeeType\(emp\.employeeType\)/)
  assert.match(employeeList, /isEmployeeFieldVisible\(['"]employeeType['"]\)/)
  assert.match(employeeList, /formatEmployeeType\(employee\.employeeType\)/)
})

test('keeps employee and attendance list loading, empty, permission, and request errors exclusive', () => {
  for (const [source, rowsName, retryHandler] of [
    [employeeList, 'employees', 'fetchEmployees'],
    [attendance, 'rows', 'fetchData']
  ]) {
    assert.match(source, /const listError = ref\(null\)/)
    assert.match(source, /status === 401 \|\| status === 403/)
    assert.match(source, /type:\s*['"]permission['"]/)
    assert.match(source, /type:\s*['"]request['"]/)
    assert.match(source, new RegExp(`${rowsName}\\.value = \\[]`))
    assert.match(source, /pagination\.total = 0/)
    assert.match(source, /pagination\.pages = 0/)
    assert.match(source, /listError\.value = resolveListError\(error\)/)
    assert.match(source, /v-if=["']listError["']/)
    assert.match(source, new RegExp(`@click=["']${retryHandler}["']`))
    assert.match(source, /<el-table\b[^>]*v-else/)
    assert.match(source, /<el-empty\b[^>]*v-if=["']!loading["']/)
  }
})
