import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const styleSource = readFileSync(new URL('../src/style.css', import.meta.url), 'utf8')
const compact = styleSource.replace(/\s+/g, ' ').toLowerCase()
const readSource = (relativePath) => readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
const navbarSource = readSource('layout/components/Navbar.vue')
const forcePasswordSource = readSource('views/ForcePasswordChange.vue')
const installationTaskSource = readSource('views/function/installationTask/installationTask.vue')
const badProductSource = readSource('views/function/badProduct/badProduct.vue')
const orderSource = readSource('views/function/order/order.vue')
const employeePermissionSource = readSource('views/function/employee/EmployeePermissionDrawer.vue')
const sharedThemeFiles = [
  'layout/index.vue',
  'layout/components/Sidebar.vue',
  'layout/components/Navbar.vue',
  'views/Login.vue',
  'views/JoinOrganization.vue',
  'views/ForcePasswordChange.vue',
  'views/legal/LegalPage.vue',
  'components/BusinessTimeCorrectionPanel.vue',
  'components/ComplianceFooter.vue',
  'components/GlobalRequestOverlay.vue',
  'components/TableColumnSettings.vue'
]
const businessThemeFiles = [
  'views/manual/UserManual.vue',
  'views/function/badProduct/badProduct.vue',
  'views/function/installationTask/installationTask.vue',
  'views/function/order/order.vue',
  'views/function/role/permissionDrawer.vue',
  'views/function/employee/employee.vue',
  'views/function/employee/EmployeePermissionDrawer.vue',
  'views/function/receipt.vue',
  'views/function/label.vue'
]
const printThemeFiles = new Set([
  'views/function/receipt.vue',
  'views/function/label.vue'
])
const oldThemePattern = /#1f3f5f|#0b1f33|rgba\(31,\s*63,\s*95|rgba\(30,\s*64,\s*104/i

function findMatchingBrace(source, blockStart) {
  let depth = 1
  let cursor = blockStart + 1

  while (cursor < source.length && depth > 0) {
    if (source[cursor] === '{') depth += 1
    if (source[cursor] === '}') depth -= 1
    cursor += 1
  }

  assert.equal(depth, 0, 'CSS rule must have balanced braces')
  return cursor
}

function normalizeCss(value) {
  return value.replace(/\s+/g, ' ').trim().toLowerCase()
}

function cssRules(source) {
  const rules = []
  let cursor = 0

  while (cursor < source.length) {
    const blockStart = source.indexOf('{', cursor)
    if (blockStart === -1) break

    const blockEnd = findMatchingBrace(source, blockStart)
    const rawHeader = source
      .slice(cursor, blockStart)
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .trim()
    const selector = rawHeader.slice(rawHeader.lastIndexOf(';') + 1).trim()

    if (/^@(media|supports|container|layer)\b/i.test(selector)) {
      rules.push(...cssRules(source.slice(blockStart + 1, blockEnd - 1)))
    } else if (selector && !selector.startsWith('@')) {
      rules.push({
        selector: normalizeCss(selector),
        declarations: normalizeCss(source.slice(blockStart + 1, blockEnd - 1)),
        start: blockStart,
        end: blockEnd
      })
    }

    cursor = blockEnd
  }

  return rules
}

function cssRule(source, selector) {
  const normalizedSelector = normalizeCss(selector)
  const rule = cssRules(source).find((candidate) => candidate.selector === normalizedSelector)
  assert.ok(rule, `missing CSS rule: ${normalizedSelector}`)
  return rule
}

function vueStyle(source) {
  const styleStart = source.indexOf('<style')
  assert.notEqual(styleStart, -1, 'Vue source must contain a style block')
  const contentStart = source.indexOf('>', styleStart)
  const contentEnd = source.indexOf('</style>', contentStart)
  assert.notEqual(contentStart, -1, 'Vue style block must have an opening tag')
  assert.notEqual(contentEnd, -1, 'Vue style block must have a closing tag')
  return source.slice(contentStart + 1, contentEnd)
}

function contrastRatio(foreground, background) {
  const luminance = (hex) => {
    const channels = hex.match(/[a-f\d]{2}/gi).map((channel) => parseInt(channel, 16) / 255)
    const [red, green, blue] = channels.map((channel) => (
      channel <= 0.04045
        ? channel / 12.92
        : ((channel + 0.055) / 1.055) ** 2.4
    ))
    return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
  }

  const lighter = Math.max(luminance(foreground), luminance(background))
  const darker = Math.min(luminance(foreground), luminance(background))
  return (lighter + 0.05) / (darker + 0.05)
}

function assertNormalTextContrast(foreground, background, label) {
  assert.ok(
    contrastRatio(foreground, background) >= 4.5,
    `${label} must meet WCAG AA normal-text contrast`
  )
}

function stripPrintMediaBlocks(source) {
  let result = ''
  let cursor = 0

  while (cursor < source.length) {
    const mediaStart = source.indexOf('@media print', cursor)
    if (mediaStart === -1) return result + source.slice(cursor)

    result += source.slice(cursor, mediaStart)
    const blockStart = source.indexOf('{', mediaStart)
    assert.notEqual(blockStart, -1, 'print media block must have an opening brace')

    let depth = 1
    let blockEnd = blockStart + 1
    while (blockEnd < source.length && depth > 0) {
      if (source[blockEnd] === '{') depth += 1
      if (source[blockEnd] === '}') depth -= 1
      blockEnd += 1
    }
    assert.equal(depth, 0, 'print media block must have balanced braces')
    cursor = blockEnd
  }

  return result
}

test('global theme exposes the approved teal semantic tokens', () => {
  assert.match(compact, /--color-primary:\s*#0f766e/)
  assert.match(compact, /--color-primary-container:\s*#ccfbf1/)
  assert.match(compact, /--color-on-primary-container:\s*#134e4a/)
  assert.match(compact, /--ys-primary:\s*#0f766e/)
  assert.match(compact, /--ys-primary-dark:\s*#115e59/)
  assert.match(compact, /--ys-on-primary:\s*#ffffff/)
  assert.match(compact, /--ys-on-surface-variant:\s*#475569/)
  assert.match(compact, /--ys-disabled-text:\s*#94a3b8/)
  assert.match(compact, /--ys-disabled-bg:\s*#e2e8f0/)
  assert.match(compact, /--el-color-primary:\s*#0f766e/)
  assert.match(compact, /--primary:\s*15 118 110/)
  assert.match(compact, /--on-primary:\s*255 255 255/)
})

test('deep primary surfaces keep text and icons white', () => {
  const surfaceRule = cssRule(styleSource, `
    .bg-primary,
    .el-button--primary,
    .function-action-primary
  `)
  const descendantRule = cssRule(styleSource, `
    :is(.bg-primary, .el-button--primary, .function-action-primary)
      :is(.text-primary, .text-on-surface, .text-on-surface-variant, .material-symbols-outlined)
  `)

  assert.match(surfaceRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(descendantRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
})

test('warning utility overrides retain status semantics and readable contrast', () => {
  const allowedWarningSelectors = new Map([
    [normalizeCss(`
      .text-amber-400,
      .text-amber-500,
      .text-amber-600,
      .text-amber-700,
      .text-amber-800,
      .text-amber-900,
      .text-amber-950
    `), '#92400e'],
    [normalizeCss(`
      .text-yellow-500,
      .text-yellow-600
    `), '#854d0e'],
    [normalizeCss(`
      .text-orange-500,
      .text-orange-600,
      .text-orange-700
    `), '#9a3412']
  ])
  const warningRules = cssRules(styleSource).filter(({ selector }) => (
    /\.(?:bg|text|border|ring|from|to)-(?:amber|yellow|orange)-/.test(selector)
  ))

  for (const rule of warningRules) {
    assert.doesNotMatch(rule.declarations, /var\(--ys-primary(?:-rgb)?\)/)
    assert.doesNotMatch(rule.declarations, /#eef4fb|#e0e9f3|#c8d7e7|#c8d3df/)
  }
  for (const [selector, expectedForeground] of allowedWarningSelectors) {
    const rule = cssRules(styleSource).find((candidate) => candidate.selector === selector)
    assert.ok(rule, `missing accessible warning foreground rule: ${selector}`)
    assert.match(rule.declarations, new RegExp(`color:\\s*${expectedForeground}`))
  }

  assert.match(
    cssRule(styleSource, `
      .bg-primary-container,
      .hover\\:bg-primary-container:hover
    `).declarations,
    /background-color:\s*var\(--ys-primary-container\)\s*!important/
  )

  assertNormalTextContrast('#92400e', '#fffbeb', 'amber warning text')
  assertNormalTextContrast('#854d0e', '#fefce8', 'yellow warning text')
  assertNormalTextContrast('#9a3412', '#fff7ed', 'orange warning text')
})

test('small foregrounds use opaque semantic colors with AA contrast', () => {
  assert.match(
    cssRule(vueStyle(navbarSource), '.tenant-chip__label').declarations,
    /color:\s*var\(--ys-on-primary-container\)/
  )
  assert.match(
    cssRule(vueStyle(installationTaskSource), '.installation-field').declarations,
    /color:\s*var\(--ys-on-surface-variant\)/
  )
  assert.match(
    cssRule(vueStyle(installationTaskSource), '.installation-filter-field').declarations,
    /color:\s*var\(--ys-on-surface-variant\)/
  )
  assert.match(
    cssRule(vueStyle(badProductSource), '.time-correction-toggle.active small').declarations,
    /color:\s*var\(--ys-on-primary\)/
  )
  assert.match(
    cssRule(vueStyle(orderSource), '.time-correction-toggle.active small').declarations,
    /color:\s*var\(--ys-on-primary\)/
  )

  const securityCheckStart = forcePasswordSource.indexOf('Security Check')
  const securityCheckTagStart = forcePasswordSource.lastIndexOf('<p', securityCheckStart)
  const securityCheckTagEnd = forcePasswordSource.indexOf('>', securityCheckTagStart)
  const securityCheckTag = forcePasswordSource.slice(securityCheckTagStart, securityCheckTagEnd + 1)
  assert.match(securityCheckTag, /\btext-primary-container\b/)
  assert.doesNotMatch(securityCheckTag, /(?:^|\s)text-primary(?:\s|$)/)

  assertNormalTextContrast('#134e4a', '#ccfbf1', 'tenant chip label')
  assertNormalTextContrast('#475569', '#ffffff', 'installation field labels')
  assertNormalTextContrast('#ffffff', '#0f766e', 'active correction control copy')
  assertNormalTextContrast('#ccfbf1', '#020617', 'security check label')
})

test('disabled primary surface families and custom controls are neutral', () => {
  const disabledRule = cssRule(styleSource, `
    .bg-primary:disabled,
    .bg-primary[aria-disabled="true"],
    .el-button--primary.is-disabled,
    .el-button--primary:disabled,
    .el-button--primary[aria-disabled="true"],
    .function-action-primary:disabled,
    .function-action-primary[aria-disabled="true"]
  `)

  assert.match(disabledRule.declarations, /color:\s*var\(--ys-disabled-text\)\s*!important/)
  assert.match(disabledRule.declarations, /border-color:\s*var\(--ys-disabled-bg\)\s*!important/)
  assert.match(disabledRule.declarations, /background:\s*var\(--ys-disabled-bg\)\s*!important/)
  assert.match(disabledRule.declarations, /box-shadow:\s*none\s*!important/)
  assert.match(disabledRule.declarations, /opacity:\s*1\s*!important/)

  const employeeDisabledStart = employeePermissionSource.indexOf(':disabled="submitting || loading || !!loadError"')
  const employeeButtonStart = employeePermissionSource.lastIndexOf('<el-button', employeeDisabledStart)
  const employeeButtonEnd = employeePermissionSource.indexOf('>', employeeDisabledStart)
  const employeeButton = employeePermissionSource.slice(employeeButtonStart, employeeButtonEnd + 1)
  assert.match(employeeButton, /\bbg-primary\b/)
  assert.doesNotMatch(employeeButton, /disabled:opacity-/)

  const orderDisabledRule = cssRule(vueStyle(orderSource), '.status-log-time-editor button:disabled')
  assert.match(orderDisabledRule.declarations, /color:\s*var\(--ys-disabled-text\)/)
  assert.match(orderDisabledRule.declarations, /border-color:\s*var\(--ys-disabled-bg\)/)
  assert.match(orderDisabledRule.declarations, /background:\s*var\(--ys-disabled-bg\)/)
  assert.match(orderDisabledRule.declarations, /box-shadow:\s*none/)
  assert.match(orderDisabledRule.declarations, /opacity:\s*1/)
})

test('primary controls expose non-disabled hover and active feedback before disabled rules', () => {
  const baseRule = cssRule(styleSource, `
    .bg-primary,
    .el-button--primary,
    .function-action-primary
  `)
  const hoverRule = cssRule(styleSource, `
    .el-button--primary:not(.is-disabled):not(:disabled):not([aria-disabled="true"]):hover,
    .function-action-primary:not(:disabled):not([aria-disabled="true"]):hover
  `)
  const activeRule = cssRule(styleSource, `
    .el-button--primary:not(.is-disabled):not(:disabled):not([aria-disabled="true"]):active,
    .function-action-primary:not(:disabled):not([aria-disabled="true"]):active
  `)
  const disabledRule = cssRule(styleSource, `
    .bg-primary:disabled,
    .bg-primary[aria-disabled="true"],
    .el-button--primary.is-disabled,
    .el-button--primary:disabled,
    .el-button--primary[aria-disabled="true"],
    .function-action-primary:disabled,
    .function-action-primary[aria-disabled="true"]
  `)

  assert.match(hoverRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(hoverRule.declarations, /background:\s*var\(--ys-primary-hover\)\s*!important/)
  assert.match(activeRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(activeRule.declarations, /background:\s*var\(--ys-primary-dark\)\s*!important/)
  assert.ok(baseRule.start < hoverRule.start)
  assert.ok(hoverRule.start < activeRule.start)
  assert.ok(activeRule.start < disabledRule.start)
})

test('global compatibility and secondary text rules use semantic theme colors', () => {
  const pageDescription = cssRule(styleSource, '.function-page-desc')
  const responsiveLabel = cssRule(styleSource, '.function-page-shell .responsive-data-table td::before')

  assert.doesNotMatch(pageDescription.declarations, /#64748b/)
  assert.match(pageDescription.declarations, /color:\s*var\(--el-text-color-regular\)/)
  assert.doesNotMatch(responsiveLabel.declarations, /#64748b/)
  assert.match(responsiveLabel.declarations, /color:\s*var\(--el-text-color-regular\)/)
})

test('shared management surfaces do not hardcode the retired blue theme', () => {
  for (const relativePath of sharedThemeFiles) {
    const source = readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
    assert.doesNotMatch(source, oldThemePattern, relativePath)
  }
})

test('business management surfaces do not hardcode the retired blue theme', () => {
  for (const relativePath of businessThemeFiles) {
    const source = readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
    const interactiveSource = printThemeFiles.has(relativePath)
      ? stripPrintMediaBlocks(source)
      : source
    assert.doesNotMatch(interactiveSource, oldThemePattern, relativePath)
  }
})
