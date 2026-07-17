import assert from 'node:assert/strict'
import test from 'node:test'
import { groupActionIds, groupLeafIds, permissionGroups } from '../src/views/function/role/permissionPresentation.js'

const tree = [
  { id: 1, permName: '订单管理', children: [
    { id: 11, permName: '查看订单', permCode: 'order:list' },
    { id: 12, permName: '编辑订单', permCode: 'order:update' }
  ]},
  { id: 2, permName: '员工管理', children: [
    { id: 21, permName: '查看员工', permCode: 'employee:list' }
  ]}
]

test('permissionGroups keeps business groups and searchable leaves', () => {
  assert.deepEqual(permissionGroups(tree, '编辑', false, []), [
    { id: 1, name: '订单管理', permissions: [{ id: 12, name: '编辑订单', code: 'order:update' }] }
  ])
})

test('permissionGroups can show only selected leaves', () => {
  assert.deepEqual(permissionGroups(tree, '', true, [21])[0].permissions.map((item) => item.id), [21])
})

test('group actions keep the full business group while search filters its presentation', () => {
  const visibleGroup = permissionGroups(tree, '编辑', false, [])[0]

  assert.deepEqual(visibleGroup.permissions.map((item) => item.id), [12])
  assert.deepEqual(groupActionIds(tree, visibleGroup.id), [11, 12])
})

test('groupLeafIds returns unique numeric leaf ids', () => {
  assert.deepEqual(groupLeafIds({
    permissions: [{ id: '11' }, { id: 11 }, { id: '12' }, { id: 12 }]
  }), [11, 12])
})
