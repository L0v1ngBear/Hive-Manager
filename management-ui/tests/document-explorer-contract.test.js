import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

import {
  buildDocumentFolderTree,
  pushExplorerLocation,
  stepExplorerLocation
} from '../src/views/function/document/documentExplorer.js'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('document explorer renders Windows-style navigation, command, content and status regions', () => {
  const page = read('../src/views/function/document/document.vue')

  for (const contract of [
    'document-navigation-pane',
    '<el-tree',
    'document-topbar',
    'document-address-bar',
    'document-command-bar',
    'document-list-view',
    'document-grid-view',
    'document-statusbar',
    'document-context-menu'
  ]) {
    assert.ok(page.includes(contract), `missing explorer contract: ${contract}`)
  }

  assert.match(page, /@click="navigateHistory\(-1\)"/)
  assert.match(page, /@click="navigateHistory\(1\)"/)
  assert.match(page, /@row-dblclick="handleDoubleClick"/)
  assert.match(page, /@row-contextmenu="handleTableContextMenu"/)
  assert.match(page, /@contextmenu\.prevent\.stop="showContextMenu\(doc, \$event\)"/)
  assert.match(page, /setViewMode\('list'\)/)
  assert.match(page, /setViewMode\('grid'\)/)
})

test('document explorer keeps uploads, rename, move, delete and download permission scoped', () => {
  const page = read('../src/views/function/document/document.vue')
  const api = read('../src/views/function/document/api/document.js')

  for (const permission of [
    'document:list',
    'document:folder:create',
    'document:file:upload',
    'document:file:download',
    'document:rename',
    'document:move',
    'document:delete',
    'document:export'
  ]) {
    assert.ok(page.includes(permission), `missing permission guard: ${permission}`)
  }

  assert.match(page, /type="file"[\s\S]*?multiple[\s\S]*?:accept="documentAccept"/)
  assert.match(page, /@drop\.prevent="handleDocumentDrop"/)
  assert.match(page, /200 \* 1024 \* 1024/)
  assert.match(api, /url: '\/document\/rename'[\s\S]*?method: 'put'/)
  assert.match(page, /await renameDocument\(document\.id, name\)/)
  assert.match(page, /await moveDocument\(movingDocument\.value\.id, moveTargetParentId\.value\)/)
  assert.match(page, /await deleteDocument\(document\.id\)/)
  assert.match(page, /downloadDocumentFile\(doc\.id\)/)
})

test('folder tree preserves nesting and safely promotes orphaned or cyclic folders', () => {
  const tree = buildDocumentFolderTree([
    { id: 2, parentId: 1, name: '合同' },
    { id: 1, parentId: 0, name: '客户资料' },
    { id: 3, parentId: 999, name: '待归档' },
    { id: 4, parentId: 5, name: '循环甲' },
    { id: 5, parentId: 4, name: '循环乙' }
  ])

  const customer = tree.find((node) => node.id === 1)
  assert.equal(customer.children[0].id, 2)
  assert.ok(tree.some((node) => node.id === 3))
  assert.ok(tree.some((node) => node.id === 4 || node.id === 5))
})

test('explorer history truncates forward entries and respects both boundaries', () => {
  let state = pushExplorerLocation([0], 0, 11)
  state = pushExplorerLocation(state.history, state.index, 22)
  assert.deepEqual(state, { history: [0, 11, 22], index: 2 })

  const back = stepExplorerLocation(state.history, state.index, -1)
  assert.deepEqual(back, { targetId: 11, index: 1, changed: true })

  state = pushExplorerLocation(state.history, back.index, 33)
  assert.deepEqual(state, { history: [0, 11, 33], index: 2 })
  assert.equal(stepExplorerLocation(state.history, state.index, 1).changed, false)
  assert.equal(stepExplorerLocation(state.history, 0, -1).changed, false)
})
