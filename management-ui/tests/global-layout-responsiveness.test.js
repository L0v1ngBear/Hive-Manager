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

const unguardedCollapsedSpans = (section) => [...section.matchAll(/<span\b(?![^>]*v-if="!isCollapsed")[^>]*>[\s\S]*?<\/span>/g)]

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
  const unguardedPrimarySpans = unguardedCollapsedSpans(primaryMenu)
  const unguardedSecondarySpans = unguardedCollapsedSpans(secondaryMenu)
  const unguardedMoreSpans = unguardedCollapsedSpans(moreControl)

  assert.match(sidebar, /const isCollapsed = ref\(false\)/)
  assert.match(primaryMenu, /<span v-if="!isCollapsed"[^>]*>\s*\{\{ item\.name \}\}\s*<\/span>/)
  assert.match(primaryMenu, /<el-tooltip[^>]*:content="item\.name"[^>]*placement="right"/)
  assert.match(secondaryMenu, /<span v-if="!isCollapsed"[^>]*>\s*\{\{ item\.name \}\}\s*<\/span>/)
  assert.match(secondaryMenu, /<el-tooltip[^>]*:content="item\.name"[^>]*placement="right"/)
  for (const [menuName, unguardedSpans] of [['primary', unguardedPrimarySpans], ['secondary', unguardedSecondarySpans]]) {
    assert.equal(unguardedSpans.length, 1, `collapsed ${menuName} menu must keep only the icon span`)
    assert.match(unguardedSpans[0][0], /class="[^"]*\bmaterial-symbols-outlined\b[^"]*"/)
    assert.match(unguardedSpans[0][0], />\s*\{\{ item\.icon \}\}\s*<\/span>/)
  }

  assert.match(moreControl, />apps<\/span>/)
  assert.match(moreControl, /<span v-if="!isCollapsed"[^>]*>更多功能<\/span>/)
  assert.match(moreControl, /<span v-if="!isCollapsed"[^>]*>\s*chevron_right\s*<\/span>/)
  assert.equal(unguardedMoreSpans.length, 1, 'collapsed More control must keep only one visible span')
  assert.match(unguardedMoreSpans[0][0], />\s*apps\s*<\/span>/)

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
  const summary = cssRule(order, '.order-summary-grid-new')
  const categorySummary = cssRule(order, '.order-category-summary-grid-new')
  const tabletSummaryRules = mediaRules(order, '@media (min-width: 641px) and (max-width: 1280px)')
  const compactSummaryRules = mediaRules(order, '@media (max-width: 640px)')
  const tabletFilterRules = mediaRules(order, '@media (min-width: 641px) and (max-width: 1279px)')
  const filterGrid = cssRule(order, '.order-filter-grid')
  const dateInputs = cssRule(order, '.order-filter-date-inputs')

  assert.match(summary.declarations, /grid-template-columns\s*:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(categorySummary.declarations, /grid-template-columns\s*:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/)
  assert.equal(hasRule(tabletSummaryRules, /\.order-summary-grid-new/, /grid-template-columns\s*:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/), true)
  assert.equal(hasRule(tabletSummaryRules, /\.order-category-summary-grid-new/, /grid-template-columns\s*:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/), true)
  assert.equal(hasRule(compactSummaryRules, /\.order-summary-grid-new/, /grid-template-columns\s*:\s*minmax\(0,\s*1fr\)/), true)
  assert.equal(hasRule(compactSummaryRules, /\.order-category-summary-grid-new/, /grid-template-columns\s*:\s*minmax\(0,\s*1fr\)/), true)

  assert.match(filterGrid.declarations, /grid-template-columns\s*:\s*repeat\(12,\s*minmax\(0,\s*1fr\)\)/)
  assert.equal(hasRule(tabletFilterRules, /\.order-filter-grid/, /grid-template-columns\s*:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/), true)
  assert.match(order, /class="order-filter-date-label">创建时间<\/span>/)
  assert.match(cssRule(order, '.order-filter-date-label').declarations, /white-space\s*:\s*nowrap/)
  assert.match(dateInputs.declarations, /display\s*:\s*grid/)
  assert.match(dateInputs.declarations, /grid-template-columns\s*:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)

  const mobileToggle = order.match(/<button\b[^>]*class="[^"]*\border-mobile-summary-toggle\b[^"]*"[^>]*>[\s\S]*?<\/button>/)?.[0] || ''
  assert.match(mobileToggle, /:aria-expanded="mobileSummaryExpanded"/)
  assert.match(mobileToggle, /aria-controls="order-secondary-summaries"/)
  assert.match(mobileToggle, /@click="mobileSummaryExpanded = !mobileSummaryExpanded"/)
  assert.match(order, /const mobileSummaryExpanded = ref\(false\)/)
  assert.match(order, /id="order-secondary-summaries"[^>]*:class="\{ 'order-secondary-summaries-open': mobileSummaryExpanded \}"/)
  assert.match(cssRule(order, '.order-mobile-summary-toggle').declarations, /display\s*:\s*none/)
  assert.equal(hasRule(compactSummaryRules, /\.order-mobile-summary-toggle/, /display\s*:\s*(?:inline-)?flex/), true)
  assert.equal(hasRule(compactSummaryRules, /\.order-secondary-summaries\s*$/, /display\s*:\s*none/), true)
  assert.equal(hasRule(compactSummaryRules, /\.order-secondary-summaries-open/, /display\s*:\s*grid/), true)

  const globalTabletRules = mediaRules(style, '@media (max-width: 1280px)')
  assert.equal(hasRule(globalTabletRules, /order-(?:category-)?summary-grid-new/, /grid-template-columns[^;]*!important/), false)
})

test('order selection remains click-only and empty rows stay compact', () => {
  const summaryButtons = templateSection(order, '<div class="order-summary-grid-new"', '        </div>')
  const statusButtons = templateSection(order, '<div class="status-chip-row order-primary-status-tabs', '        </div>')

  assert.match(summaryButtons, /@click="selectSummaryCard\(card\)"/)
  assert.match(statusButtons, /@click="selectStatus\(status\.value\)"/)
  assert.doesNotMatch(summaryButtons, /@(?:mouse(?:enter|over)|pointer(?:enter|over))=/)
  assert.doesNotMatch(statusButtons, /@(?:mouse(?:enter|over)|pointer(?:enter|over))=/)
  assert.equal((order.match(/class="order-empty-state-cell"/g) || []).length, 2)
  assert.match(cssRule(order, '.order-empty-state-cell').declarations, /height\s*:\s*12rem/)
})

test('price uses shared stat cards, accessible filters, grouped actions and a scrolling table', () => {
  const statsSection = templateSection(price, '<section v-else v-loading="statsLoading"', '</section>')
  const filterForm = templateSection(price, '<el-form :model="query"', '</el-form>')
  const tableScroll = templateSection(price, '<div class="function-table-scroll">', '</div>')

  assert.match(style, /\.function-stat-card\s*\{[\s\S]{0,420}min-width\s*:\s*0[\s\S]{0,420}border-radius\s*:\s*8px[\s\S]{0,420}padding\s*:\s*1\.25rem/)
  assert.match(statsSection, /class="[^"]*\bfunction-stats-grid\b[^"]*"/)
  assert.equal((statsSection.match(/<el-statistic\b[^>]*class="function-stat-card"/g) || []).length, 4)
  assert.match(filterForm, /class="function-filter-form p-4"/)

  for (const [model, label] of [
    ['query.keyword', '价格关键词'],
    ['query.status', '价格状态'],
    ['query.batchNo', '价格批号'],
    ['query.spec', '价格规格'],
    ['query.currency', '价格币种'],
    ['query.priceMin', '最低价格'],
    ['query.priceMax', '最高价格'],
    ['query.effectiveStart', '生效开始日期'],
    ['query.effectiveEnd', '生效结束日期'],
  ]) {
    assert.match(filterForm, new RegExp(`<el-(?:input|select|input-number|date-picker)\\b(?=[^>]*v-model(?:\\.trim)?="${model.replace('.', '\\.')}")(?=[^>]*aria-label="${label}")[^>]*>`))
  }

  assert.match(filterForm, /<div class="function-filter-actions">[\s\S]*@click="handleFilter"[\s\S]*@click="resetFilter"[\s\S]*<\/div>/)
  assert.match(tableScroll, /<el-table\b[\s\S]*<el-table-column label="操作" fixed="right" width="200"[\s\S]*<\/el-table>/)
})

test('equipment uses accessible shared filters, grouped actions and a scrolling table', () => {
  const filterForm = templateSection(equipment, '<el-form :inline="true"', '</el-form>')
  const tableScroll = templateSection(equipment, '<div class="function-table-scroll">', '</div>')
  const actions = templateSection(filterForm, '<el-form-item class="function-filter-actions">', '</el-form-item>')

  assert.match(filterForm, /class="function-filter-form equipment-filter-form"/)
  assert.match(filterForm, /<el-input\b(?=[^>]*v-model\.trim="filters\.keyword")(?=[^>]*aria-label="设备关键词")[^>]*>/)
  assert.match(filterForm, /<el-select\b(?=[^>]*v-model="filters\.status")(?=[^>]*aria-label="设备状态")[^>]*>/)
  assert.match(actions, /@click="handleSearch"/)
  assert.match(actions, /@click="resetSearch"/)
  assert.match(actions, /@click="exportEquipmentExcel"/)
  assert.match(actions, /@click="openCreate"/)
  assert.match(tableScroll, /<el-table\b[\s\S]*<el-table-column label="操作" width="210" fixed="right"[\s\S]*<\/el-table>/)
})
