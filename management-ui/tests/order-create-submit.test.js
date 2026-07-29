import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const orderPage = readFileSync(
  new URL('../src/views/function/order/order.vue', import.meta.url),
  'utf8',
)

test('order create payload keeps its meaningful item filter available', () => {
  assert.match(
    orderPage,
    /function buildOrderPayload\(\)[\s\S]*?\.filter\(isOrderItemMeaningful\)/,
  )
  assert.match(orderPage, /function isOrderItemMeaningful\(item = \{\}\)/)
})

test('empty order item rows are excluded without removing filled rows', () => {
  const helperSource = orderPage.match(
    /function isOrderItemMeaningful\(item = \{\}\) \{([\s\S]*?)\n\}/,
  )?.[0]

  assert.ok(helperSource, 'isOrderItemMeaningful helper should exist')
  const isOrderItemMeaningful = Function(
    'normalizeText',
    `${helperSource}; return isOrderItemMeaningful`,
  )((value) => String(value || '').trim())

  assert.equal(isOrderItemMeaningful({}), false)
  assert.equal(isOrderItemMeaningful({ modelCode: '  A-01  ' }), true)
  assert.equal(isOrderItemMeaningful({ quantity: 0 }), true)
  assert.equal(isOrderItemMeaningful({ weight: '0' }), true)
  assert.equal(isOrderItemMeaningful({ spec: 0 }), true)
})
