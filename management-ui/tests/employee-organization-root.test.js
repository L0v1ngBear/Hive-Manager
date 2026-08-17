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

test('keeps multiple leaderless employees under a virtual organization root', () => {
  const roots = buildEmployeeHierarchy([
    employee(10, '销售张三', { positionName: '销售专员' }),
    employee(20, '张老板', { positionName: '总经理 · CEO' }),
    employee(21, '财务李四', { leaderId: 20, positionName: '财务经理' })
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '组织架构')
  assert.deepEqual(
    chart.data.children.map((item) => item.label).sort(),
    ['张老板', '销售张三']
  )
  assert.deepEqual(chart.data.children.find((item) => item.label === '张老板').children.map((item) => item.label), ['财务李四'])
  assert.equal(chart.topLevelCount, 2)
  assert.equal(chart.unassignedCount, 2)
  assert.equal(chart.data.isVirtualRoot, true)
})

test('does not infer a hierarchy from position keywords', () => {
  const roots = buildEmployeeHierarchy([
    employee(1, '普通负责人', { positionName: '部门负责人' }),
    employee(2, '公司负责人', { positionName: 'ceo' })
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '组织架构')
  assert.deepEqual(chart.data.children.map((item) => item.label), ['普通负责人', '公司负责人'])
})

test('preserves all configured top-level roots and their descendants', () => {
  const roots = buildEmployeeHierarchy([
    employee(1, '负责人甲'),
    employee(2, '员工甲一', { leaderId: 1 }),
    employee(3, '员工甲二', { leaderId: 1 }),
    employee(4, '负责人乙')
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '组织架构')
  assert.deepEqual(chart.data.children.map((item) => item.label), ['负责人甲', '负责人乙'])
  assert.deepEqual(chart.data.children[0].children.map((item) => item.label), ['员工甲一', '员工甲二'])
})

test('preserves original root order under the virtual root', () => {
  const roots = buildEmployeeHierarchy([
    employee(8, '负责人甲'),
    employee(3, '负责人乙')
  ])

  const chart = buildOrganizationChart(roots)

  assert.equal(chart.data.label, '组织架构')
  assert.deepEqual(chart.data.children.map((item) => item.label), ['负责人甲', '负责人乙'])
})
