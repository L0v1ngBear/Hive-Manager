import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildEmployeeHierarchy,
  buildOrganizationChart
} from '../src/views/function/employee/employeeOrganization.js'

const employee = (id, name, options = {}) => ({
  id,
  name,
  departmentName: options.departmentName || '综合管理部',
  positionName: options.positionName || '员工',
  leaderId: options.leaderId ?? null,
  leaderName: options.leaderName || '',
  status: options.status ?? 1
})

test('keeps a single configured leader as the organization root', () => {
  const roots = buildEmployeeHierarchy([
    employee(1, '张老板', { positionName: 'CEO' }),
    employee(2, '销售张三', { leaderId: 1 })
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '张老板')
  assert.equal(chart.data.isOrganizationRoot, true)
  assert.deepEqual(chart.data.children.map((item) => item.label), ['销售张三'])
  assert.equal(chart.topLevelCount, 1)
  assert.equal(chart.unassignedCount, 0)
})

test('selects the boss keyword root and reparents other leaderless employees', () => {
  const roots = buildEmployeeHierarchy([
    employee(10, '销售张三', { positionName: '销售专员' }),
    employee(20, '张老板', { positionName: '总经理 · CEO' }),
    employee(21, '财务李四', { leaderId: 20, positionName: '财务经理' })
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '张老板')
  assert.deepEqual(
    chart.data.children.map((item) => item.label).sort(),
    ['财务李四', '销售张三']
  )
  assert.equal(chart.topLevelCount, 1)
  assert.equal(chart.unassignedCount, 1)
  assert.doesNotMatch(JSON.stringify(chart.data), /组织架构/)
})

test('matches CEO without depending on letter case', () => {
  const roots = buildEmployeeHierarchy([
    employee(1, '普通负责人', { positionName: '部门负责人' }),
    employee(2, '公司负责人', { positionName: 'ceo' })
  ])

  assert.equal(buildOrganizationChart(roots).data.label, '公司负责人')
})

test('falls back to the root with the most descendants', () => {
  const roots = buildEmployeeHierarchy([
    employee(1, '负责人甲'),
    employee(2, '员工甲一', { leaderId: 1 }),
    employee(3, '员工甲二', { leaderId: 1 }),
    employee(4, '负责人乙')
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '负责人甲')
  assert.deepEqual(
    chart.data.children.map((item) => item.label).sort(),
    ['员工甲一', '员工甲二', '负责人乙']
  )
})

test('uses the original root order as the final tie break', () => {
  const roots = buildEmployeeHierarchy([
    employee(8, '负责人甲'),
    employee(3, '负责人乙')
  ])

  assert.equal(buildOrganizationChart(roots).data.label, '负责人甲')
})
