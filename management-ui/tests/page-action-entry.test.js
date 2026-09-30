import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import postcss from 'postcss'

const source = name => readFileSync(new URL(`../src/views/function/${name}.vue`, import.meta.url), 'utf8')

test('action groups only control layout and leave permissions and palette to each page', () => {
  const component = readFileSync(new URL('../src/components/PageActionGroup.vue', import.meta.url), 'utf8')
  assert.match(component, /role="group" :aria-label="label"/)
  assert.match(component, /<slot\s*\/>/)
  assert.doesNotMatch(component, /@click|fetch\(|request\(|router\.|v-permission/)
  const css = component.match(/<style scoped>([\s\S]*?)<\/style>/)[1]
  postcss.parse(css).walkDecls(decl => {
    assert.doesNotMatch(decl.prop, /color|background|shadow|opacity|filter/)
  })
})

test('maintenance controls have named groups while primary entry points stay in page headers', () => {
  for (const [name, primary, secondary] of [
    ['employee/employee', 'openCreateDrawer', 'handleTemplateDownload'],
    ['inventory/inventory', 'openInDrawer', 'openWarningSetting'],
    ['customer/customer', 'openCreateDrawer', 'triggerCustomerImport'],
    ['price/price', 'openCreate()', 'exportExcel'],
    ['label', 'printCurrentLabel', 'openTemplateEditor'],
  ]) {
    const text = source(name)
    const header = text.slice(text.indexOf('function-page-header') > -1 ? text.indexOf('function-page-header') : text.indexOf('label-print-header'), text.indexOf('<PageActionGroup'))
    assert.ok(header.includes(`@click="${primary}"`), `${name}: primary action stays at the top`)
    assert.ok(!header.includes(`@click="${secondary}"`), `${name}: maintenance must not crowd the primary action`)
    assert.match(text, /<PageActionGroup label="[^"]+">/)
    const group = text.slice(text.indexOf('<PageActionGroup'), text.indexOf('</PageActionGroup>'))
    assert.ok(group.includes(`@click="${secondary}"`), `${name}: secondary entry remains directly available`)
  }
})

test('equipment create/export stay visible when filters are collapsed', () => {
  const text = source('equipment/equipment')
  const header = text.slice(0, text.indexOf('</header>'))
  const filter = text.slice(text.indexOf('<el-form v-filter-collapse'), text.indexOf('</el-form>'))
  for (const [handler, permission] of [['openCreate', 'canCreate'], ['exportEquipmentExcel', 'canExport']]) {
    assert.ok(header.includes(`@click="${handler}"`))
    assert.ok(header.includes(`:disabled="!${permission}"`))
    assert.ok(!filter.includes(`@click="${handler}"`))
  }
})
