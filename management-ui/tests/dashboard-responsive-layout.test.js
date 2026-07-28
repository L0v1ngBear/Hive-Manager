import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const dashboard = readFileSync(
  new URL('../src/views/dashboard/index.vue', import.meta.url),
  'utf8',
)

const cssBlock = (marker) => {
  const start = dashboard.indexOf(marker)
  assert.notEqual(start, -1, `missing CSS marker: ${marker}`)
  const open = dashboard.indexOf('{', start)
  let depth = 0
  for (let index = open; index < dashboard.length; index += 1) {
    if (dashboard[index] === '{') depth += 1
    if (dashboard[index] === '}') depth -= 1
    if (depth === 0) return dashboard.slice(open + 1, index)
  }
  assert.fail(`unclosed CSS block: ${marker}`)
}

test('dashboard responds to available content width instead of viewport-only breakpoints', () => {
  const overview = cssBlock('.dashboard-overview')

  assert.match(overview, /container-name\s*:\s*dashboard/)
  assert.match(overview, /container-type\s*:\s*inline-size/)
  assert.match(dashboard, /@container dashboard \(min-width: 30rem\)/)
  assert.match(dashboard, /@container dashboard \(min-width: 48rem\)/)
  assert.match(dashboard, /@container dashboard \(min-width: 68rem\)/)
  assert.match(dashboard, /@container dashboard \(min-width: 72rem\)/)
  assert.match(dashboard, /@container dashboard \(min-width: 88rem\)/)
  assert.match(overview, /width\s*:\s*100%/)
  assert.match(overview, /margin-inline\s*:\s*0/)
  assert.doesNotMatch(dashboard, /max-w-7xl/)
  assert.doesNotMatch(overview, /width\s*:\s*min\(100%,\s*80rem\)/)
  assert.doesNotMatch(dashboard, /xl:grid-cols-\[minmax/)
  assert.doesNotMatch(dashboard, /md:grid-cols-2 xl:grid-cols-1/)
})

test('dashboard exposes complete compact tablet and wide-screen grid transitions', () => {
  const compact = cssBlock('@container dashboard (min-width: 30rem)')
  const tablet = cssBlock('@container dashboard (min-width: 48rem)')
  const desktop = cssBlock('@container dashboard (min-width: 68rem)')
  const wide = cssBlock('@container dashboard (min-width: 72rem)')
  const large = cssBlock('@container dashboard (min-width: 88rem)')

  assert.match(compact, /\.dashboard-summary-grid[\s\S]*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(compact, /\.dashboard-side-grid[\s\S]*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(tablet, /\.dashboard-summary-grid[\s\S]*repeat\(3,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(tablet, /\.dashboard-announcement-grid[\s\S]*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(desktop, /\.dashboard-content-grid[\s\S]*minmax\(20rem,\s*1fr\)/)
  assert.match(desktop, /\.dashboard-announcement-overview[\s\S]*grid-column:\s*auto/)
  assert.match(desktop, /\.dashboard-side-grid[\s\S]*grid-column:\s*1\s*\/\s*-1/)
  assert.match(desktop, /\.dashboard-side-grid[\s\S]*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(wide, /\.dashboard-hero[\s\S]*minmax\(32rem,\s*1\.15fr\)/)
  assert.match(wide, /\.dashboard-quick-grid[\s\S]*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(large, /\.dashboard-hero[\s\S]*minmax\(48rem,\s*1\.25fr\)/)
  assert.match(large, /\.dashboard-quick-grid[\s\S]*repeat\(3,\s*minmax\(0,\s*1fr\)\)/)
})

test('dashboard cards and headers can shrink and wrap without clipping', () => {
  assert.match(dashboard, /class="dashboard-content-grid"/)
  assert.match(dashboard, /class="dashboard-side-grid"/)
  assert.match(dashboard, /dashboard-announcement-grid__full/)
  assert.match(cssBlock('.dashboard-panel'), /min-width\s*:\s*0/)
  assert.match(cssBlock('.dashboard-panel-header'), /flex-wrap\s*:\s*wrap/)
  assert.match(cssBlock('.dashboard-greeting'), /overflow-wrap\s*:\s*anywhere/)
  assert.doesNotMatch(cssBlock('.dashboard-greeting'), /white-space\s*:\s*nowrap/)
  assert.match(cssBlock('.dashboard-summary-card'), /min-block-size\s*:\s*6\.5rem/)
  assert.doesNotMatch(cssBlock('.dashboard-summary-card'), /(?:^|\n)\s*block-size\s*:\s*6\.5rem/)
  assert.match(dashboard, /class="dashboard-announcement-grid__full dashboard-empty"/)
  assert.match(dashboard, /:image-size="72"/)
})
