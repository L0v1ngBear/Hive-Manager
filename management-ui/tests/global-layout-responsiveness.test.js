import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const style = process.env.GLOBAL_LAYOUT_STYLE ?? read('src/style.css')
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

const cssRule = (source, marker) => ({
  selector: marker,
  declarations: extractCssBlock(source, marker),
})

const mediaRules = (source, marker) => topLevelCssRules(extractCssBlock(source, marker)).map((rule) => {
  const openBrace = rule.indexOf('{')
  return {
    selector: rule.slice(0, openBrace),
    declarations: rule.slice(openBrace + 1, -1),
  }
})

const hasRule = (rules, selector, declarations) => rules.some((rule) => selector.test(rule.selector) && declarations.test(rule.declarations))

const templateSection = (source, startMarker, endMarker) => {
  const start = source.indexOf(startMarker)
  assert.notEqual(start, -1, `Missing template section start: ${startMarker}`)

  const end = source.indexOf(endMarker, start)
  assert.notEqual(end, -1, `Missing template section end: ${endMarker}`)

  return source.slice(start, end + endMarker.length)
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

const responsiveTableWrap = () => cssRule(style, '.function-page-shell .responsive-table-wrap')
const blockCardLayout = /\.responsive-data-table\s*,[\s\S]*\.responsive-data-table td\s*$/
const tableHeader = /\.responsive-data-table thead\s*$/

test('responsive table wrapper keeps horizontal scrolling stable', () => {
  const tableWrap = responsiveTableWrap()

  assert.match(tableWrap.declarations, /overflow-x\s*:\s*auto\s*!important/)
  assert.match(tableWrap.declarations, /scrollbar-gutter\s*:\s*stable/)
})

test('responsive table rules retain native tables at tablet widths', () => {
  const tabletRules = mediaRules(style, '@media (max-width: 900px)')

  assert.equal(hasRule(tabletRules, blockCardLayout, /display\s*:\s*block/), false, '900px rules must retain native table layout')
  assert.equal(hasRule(tabletRules, tableHeader, /display\s*:\s*none/), false, '900px rules must retain table headers')
})

test('responsive table rules convert native tables on compact screens', () => {
  const compactRules = mediaRules(style, '@media (max-width: 640px)')

  assert.equal(hasRule(compactRules, blockCardLayout, /display\s*:\s*block/), true, '640px rules must convert native tables to cards')
  assert.equal(hasRule(compactRules, tableHeader, /display\s*:\s*none/), true, '640px rules must hide native table headers')
})

test('desktop sidebar opens by default and collapsed navigation is icon only', () => {
  const primaryMenu = templateSection(sidebar, 'v-for="item in primaryMenus"', '      <div v-if="secondaryMenus.length"')
  const moreControl = templateSection(sidebar, '<el-tooltip :disabled="!isCollapsed" content="更多功能"', '        <div v-show="showMore"')
  const secondaryMenu = templateSection(sidebar, 'v-for="item in secondaryMenus"', '    </nav>')
  const bottomToggle = templateSection(sidebar, '<div v-if="!props.mobile"', '  </aside>')
  const approvalBadge = templateSection(primaryMenu, '<el-badge', '/>')
  const toggleButton = templateSection(bottomToggle, '<el-button', '</el-button>')
  const unguardedMoreLabels = [...moreControl.matchAll(/<span\b(?![^>]*v-if="!isCollapsed")[^>]*>([\s\S]*?)<\/span>/g)]

  assert.match(sidebar, /const isCollapsed = ref\(false\)/)
  assert.match(primaryMenu, /<span v-if="!isCollapsed"[^>]*>\s*\{\{ item\.name \}\}\s*<\/span>/)
  assert.match(primaryMenu, /<el-tooltip[^>]*:content="item\.name"[^>]*placement="right"/)
  assert.match(secondaryMenu, /<span v-if="!isCollapsed"[^>]*>\s*\{\{ item\.name \}\}\s*<\/span>/)
  assert.match(secondaryMenu, /<el-tooltip[^>]*:content="item\.name"[^>]*placement="right"/)

  assert.match(moreControl, />apps<\/span>/)
  assert.match(moreControl, /<span v-if="!isCollapsed"[^>]*>更多功能<\/span>/)
  assert.match(moreControl, /<span v-if="!isCollapsed"[^>]*>\s*chevron_right\s*<\/span>/)
  assert.equal(unguardedMoreLabels.length, 1, 'collapsed More control must keep only one visible span')
  assert.match(unguardedMoreLabels[0][1], /^\s*apps\s*$/)

  assert.match(bottomToggle, /<el-tooltip[^>]*:content="isCollapsed \? '展开导航' : '收起导航'"[^>]*placement="right"/)
  assert.match(toggleButton, /:aria-label="isCollapsed \? '展开导航' : '收起导航'"/)
  assert.match(toggleButton, />keyboard_double_arrow_right<\/span>/)
  assert.equal(toggleButton.replace(/<[^>]+>/g, '').replace('keyboard_double_arrow_right', '').trim(), '')

  assert.match(approvalBadge, /v-if="item\.path === '\/function\/approval' && approvalPendingCount > 0"/)
  assert.match(approvalBadge, /:value="approvalPendingCount"/)
  assert.match(approvalBadge, /class="approval-menu-badge"/)
  assert.match(approvalBadge, /:class="isCollapsed \? 'absolute right-1\.5 top-1\.5' : 'ml-auto'"/)
})

test('Hive branding is consistent', () => {
  assert.doesNotMatch(sidebar, /\u8f7b\u5de2 Hive/)
  assert.match(sidebar, /\u8702\u5de2 Hive/)
  assert.match(sidebar, /tenantLogoUrl\.value \? `\$\{tenantName\.value\} logo` : '蜂巢 Hive logo'/)
  assert.match(sidebar, /tenantLogoUrl\.value && !userStore\.isPlatformTenant \? tenantName\.value : '蜂巢 Hive'/)
})

test('order page defines responsive summary filters and a compact mobile entry', () => {
  assert.match(order, /order-summary-grid-new/)
  assert.match(order, /order-filter-grid/)
  assert.match(order, /<button\b[^>]*class="[^"]*\border-mobile-summary-toggle\b[^"]*"[^>]*>/)
  assert.match(order, /\.order-mobile-summary-toggle\s*\{[\s\S]{0,320}display\s*:/)
  assert.match(order, /@media\s*\([^)]*(?:max-width|min-width)\s*:[^)]*\)\s*\{[\s\S]{0,2000}\.order-mobile-summary-toggle[\s\S]{0,420}display\s*:/)
})
