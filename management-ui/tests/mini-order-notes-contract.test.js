import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const miniRoot = 'D:/productHiveFrontend/client'
const detailSource = fs.readFileSync(`${miniRoot}/pages/orderDetail/orderDetail.js`, 'utf8')
const detailTemplate = fs.readFileSync(`${miniRoot}/pages/orderDetail/orderDetail.wxml`, 'utf8')
const createSource = fs.readFileSync(`${miniRoot}/pages/salesOrderCreate/salesOrderCreate.js`, 'utf8')

test('mini order detail supports adding and updating notes with a complete order payload', () => {
  assert.match(detailSource, /canCreateNotes/)
  assert.match(detailSource, /onAddOrderNote/)
  assert.match(detailSource, /onDiscardUnsavedOrderNote/)
  assert.match(detailSource, /customerName:\s*orderDetail\.customerName/)
  assert.match(detailSource, /projectName:\s*orderDetail\.projectName/)
  assert.match(detailSource, /items:\s*\(orderDetail\.items/)
  assert.doesNotMatch(detailSource, /data:\s*\{\s*notes\s*\}/)
  assert.match(detailTemplate, /bindtap="onAddOrderNote"/)
  assert.match(createSource, /notes,/)
})
