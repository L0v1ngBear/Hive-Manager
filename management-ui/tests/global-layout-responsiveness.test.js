import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const style = read('src/style.css')
const sidebar = read('src/layout/components/Sidebar.vue')
const price = read('src/views/function/price/price.vue')
const equipment = read('src/views/function/equipment/equipment.vue')
const order = read('src/views/function/order/order.vue')

const extractCssBlock = (source, marker) => {
  const markerStart = source.indexOf(marker)
  assert.notEqual(markerStart, -1, `Missing CSS block marker: ${marker}`)

  const openBrace = source.indexOf('{', markerStart)
  assert.notEqual(openBrace, -1, `Missing opening brace for CSS block: ${marker}`)

  let depth = 0
  for (let index = openBrace; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') depth -= 1
    if (depth === 0) return source.slice(openBrace + 1, index)
  }

  assert.fail(`Unclosed CSS block: ${marker}`)
}

const topLevelCssRules = (block) => {
  const rules = []
  let depth = 0
  let ruleStart = 0

  for (let index = 0; index < block.length; index += 1) {
    if (block[index] === '{') depth += 1
    if (block[index] === '}') {
      depth -= 1
      if (depth === 0) {
        rules.push(block.slice(ruleStart, index + 1))
        ruleStart = index + 1
      }
    }
  }

  return rules
}

test('tablet rules do not force every direct business grid to one column', () => {
  const tabletRules = topLevelCssRules(extractCssBlock(style, '@media (max-width: 900px)'))
  const directBusinessGrid = /\.responsive-page-frame \.function-page-container\s*>\s*(?:\.grid|section\.grid|div\.grid)/
  const oneColumn = /grid-template-columns\s*:\s*minmax\(0,\s*1fr\)\s*!important/

  assert.equal(
    tabletRules.some((rule) => directBusinessGrid.test(rule) && oneColumn.test(rule)),
    false,
    '900px rules must not collapse direct business grids to one column',
  )
})

test('shared list layouts expose stable stats filters and horizontal tables', () => {
  assert.match(style, /\.function-stats-grid\s*\{[\s\S]{0,360}display\s*:\s*grid[\s\S]{0,360}grid-template-columns\s*:/)
  assert.match(style, /\.function-filter-form\s*\{[\s\S]{0,360}display\s*:\s*grid[\s\S]{0,360}grid-template-columns\s*:/)
  assert.match(style, /\.function-table-scroll\s*\{[\s\S]{0,240}overflow-x\s*:\s*auto/)
  assert.match(price, /class="[^"]*\bfunction-stats-grid\b[^"]*"/)
  assert.match(price, /class="[^"]*\bfunction-filter-form\b[^"]*"/)
  assert.match(equipment, /class="function-filter-form/)
})

test('desktop sidebar opens by default and collapsed navigation is icon only', () => {
  assert.match(sidebar, /const isCollapsed = ref\(false\)/)
  assert.doesNotMatch(sidebar, /isCollapsed \? 'text-\[10px\]/)
  assert.match(sidebar, /v-if="!isCollapsed"[\s\S]{0,140}item\.name/)
  assert.match(sidebar, /:content="item\.name"/)
})

test('Hive branding is consistent', () => {
  assert.doesNotMatch(sidebar, /\u8f7b\u5de2 Hive/)
  assert.match(sidebar, /\u8702\u5de2 Hive/)
})

test('order page defines responsive summary filters and a compact mobile entry', () => {
  assert.match(order, /order-summary-grid-new/)
  assert.match(order, /order-filter-grid/)
  assert.match(order, /<button\b[^>]*class="[^"]*\border-mobile-summary-toggle\b[^"]*"[^>]*>/)
  assert.match(order, /\.order-mobile-summary-toggle\s*\{[\s\S]{0,320}display\s*:/)
  assert.match(order, /@media\s*\([^)]*(?:max-width|min-width)\s*:[^)]*\)\s*\{[\s\S]{0,2000}\.order-mobile-summary-toggle[\s\S]{0,420}display\s*:/)
})
