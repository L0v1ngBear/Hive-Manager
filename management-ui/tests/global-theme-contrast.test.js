import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { join, relative } from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const styleSource = readFileSync(new URL('../src/style.css', import.meta.url), 'utf8')
const compact = styleSource.replace(/\s+/g, ' ').toLowerCase()
const sourceRoot = fileURLToPath(new URL('../src/', import.meta.url))
const readSource = (relativePath) => readFileSync(new URL(`../src/${relativePath}`, import.meta.url), 'utf8')
const navbarSource = readSource('layout/components/Navbar.vue')
const permissionDirectiveSource = readSource('directives/permission.js')
const permissionStateSource = readSource('directives/permissionState.js')
const forcePasswordSource = readSource('views/ForcePasswordChange.vue')
const installationTaskSource = readSource('views/function/installationTask/installationTask.vue')
const badProductSource = readSource('views/function/badProduct/badProduct.vue')
const orderSource = readSource('views/function/order/order.vue')
const receiptSource = readSource('views/function/receipt.vue')
const employeePermissionSource = readSource('views/function/employee/EmployeePermissionDrawer.vue')
const employeeSource = readSource('views/function/employee/employee.vue')
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
const alphaPrimaryForegroundAllowlist = [
  {
    relativePath: 'views/Login.vue',
    token: 'text-primary/5',
    rationale: 'Oversized, pointer-events-none HIVE watermark behind the login content.'
  },
  {
    relativePath: 'views/function/approval/approvalCenter.vue',
    token: 'text-primary/5',
    rationale: 'Absolutely positioned dataset watermark behind the approval summary card.'
  }
]

function listSourceFiles(directory = sourceRoot) {
  const files = []

  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const fullPath = join(directory, entry.name)
    if (entry.isDirectory()) {
      files.push(...listSourceFiles(fullPath))
    } else if (/\.(?:css|js|ts|vue)$/i.test(entry.name)) {
      files.push(fullPath)
    }
  }

  return files
}

function sourceRelativePath(filePath) {
  return relative(sourceRoot, filePath).replaceAll('\\', '/')
}

function findNextOpeningBrace(source, start = 0) {
  let cursor = start
  let quote = null
  let escaped = false
  let inComment = false

  while (cursor < source.length) {
    const current = source[cursor]
    const next = source[cursor + 1]

    if (inComment) {
      if (current === '*' && next === '/') {
        inComment = false
        cursor += 2
        continue
      }
    } else if (quote) {
      if (escaped) {
        escaped = false
      } else if (current === '\\') {
        escaped = true
      } else if (current === quote) {
        quote = null
      }
    } else if (current === '/' && next === '*') {
      inComment = true
      cursor += 2
      continue
    } else if (current === '"' || current === "'") {
      quote = current
    } else if (current === '\\') {
      cursor += 2
      continue
    } else if (current === '{') {
      return cursor
    }
    cursor += 1
  }

  return -1
}

function findMatchingBrace(source, blockStart) {
  let depth = 1
  let cursor = blockStart + 1
  let quote = null
  let escaped = false
  let inComment = false

  while (cursor < source.length && depth > 0) {
    const current = source[cursor]
    const next = source[cursor + 1]

    if (inComment) {
      if (current === '*' && next === '/') {
        inComment = false
        cursor += 2
        continue
      }
    } else if (quote) {
      if (escaped) {
        escaped = false
      } else if (current === '\\') {
        escaped = true
      } else if (current === quote) {
        quote = null
      }
    } else if (current === '/' && next === '*') {
      inComment = true
      cursor += 2
      continue
    } else if (current === '"' || current === "'") {
      quote = current
    } else if (current === '\\') {
      cursor += 2
      continue
    } else if (current === '{') {
      depth += 1
    } else if (current === '}') {
      depth -= 1
    }
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
    const blockStart = findNextOpeningBrace(source, cursor)
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
  const styles = vueStyles(source)
  assert.ok(styles.length > 0, 'Vue source must contain a style block')
  return styles[0]
}

function vueStyles(source) {
  const styles = []
  const styleTagPattern = /^<style\b[^>]*>/gm
  let match

  while ((match = styleTagPattern.exec(source))) {
    const contentStart = match.index + match[0].length
    const contentEnd = source.indexOf('</style>', contentStart)
    assert.notEqual(contentEnd, -1, 'Vue style block must have a closing tag')
    styles.push(source.slice(contentStart, contentEnd))
    styleTagPattern.lastIndex = contentEnd + '</style>'.length
  }

  return styles
}

function isAlphaPrimaryForeground(value) {
  const normalized = value.toLowerCase()
  const referencesPrimary = (
    /--ys-primary(?:-rgb)?|var\(--primary\)|#0f766e/.test(normalized)
    || /15\s*(?:,|\s)\s*118\s*(?:,|\s)\s*110/.test(normalized)
    || /^(?:text|fill|stroke)-primary\//.test(normalized)
  )
  const usesAlpha = (
    /\/\s*(?:0?\.\d+|\d+%)/.test(normalized)
    || /rgba\([^)]*,\s*(?:0?\.\d+|\d+%)\s*\)/.test(normalized)
    || /color-mix\([^)]*transparent/.test(normalized)
    || /^(?:text|fill|stroke)-primary\//.test(normalized)
  )
  return referencesPrimary && usesAlpha
}

function alphaPrimaryForegrounds(relativePath, source) {
  const findings = []
  const lineNumber = (index) => source.slice(0, index).split(/\r?\n/).length
  const cssForegroundPattern = /(?<![-\w])(color|fill|stroke)\s*:\s*([^;\r\n}]+)/gmi
  const utilityPattern = /(?:text|fill|stroke)-(?:primary\/[^\s"'`<>]+|\[[^\]\r\n]+\])/gi
  const attributePattern = /\b(fill|stroke)\s*=\s*["']([^"']+)["']/gi

  for (const match of source.matchAll(cssForegroundPattern)) {
    if (isAlphaPrimaryForeground(match[2])) {
      findings.push({ relativePath, line: lineNumber(match.index), token: match[0].trim() })
    }
  }
  for (const match of source.matchAll(utilityPattern)) {
    if (isAlphaPrimaryForeground(match[0])) {
      findings.push({ relativePath, line: lineNumber(match.index), token: match[0] })
    }
  }
  for (const match of source.matchAll(attributePattern)) {
    if (isAlphaPrimaryForeground(match[2])) {
      findings.push({ relativePath, line: lineNumber(match.index), token: match[0] })
    }
  }

  return findings
}

function isInteractiveDisabledSelector(selector) {
  const enabledPredicatesRemoved = selector.replace(/:not\([^)]*disabled[^)]*\)/gi, '')
  if (/:disabled\b|\[aria-disabled\s*=\s*["']?true/i.test(enabledPredicatesRemoved)) return true
  if (/\.is-permission-disabled\b/i.test(enabledPredicatesRemoved)) return true

  const classNames = [...enabledPredicatesRemoved.matchAll(/\.([\w-]+)/g)].map((match) => match[1])
  const namesControl = (className) => /(?:action|button|btn|control|menu-item|trigger|upload)/i.test(className)
  const explicitControlState = classNames.some((className) => (
    /disabled/i.test(className) && namesControl(className)
  ))
  const pairedControlState = classNames.some((className) => /^is-disabled$/i.test(className))
    && classNames.some(namesControl)
  const disabledInteractiveElement = (
    /(?:^|[\s>,+~])(?:button|input|select|textarea|a)\b[^,{]*\.(?:disabled|is-disabled)\b/i.test(enabledPredicatesRemoved)
    || /\[role\s*=\s*["']?(?:button|menuitem)["']?\][^,{]*\.(?:disabled|is-disabled)\b/i.test(enabledPredicatesRemoved)
  )

  return explicitControlState || pairedControlState || disabledInteractiveElement
}

function disabledRuleViolations(relativePath, source) {
  const violations = []
  const styleBlocks = relativePath.endsWith('.vue')
    ? vueStyles(source)
    : (relativePath.endsWith('.css') ? [source] : [])

  for (const styleBlock of styleBlocks) {
    for (const rule of cssRules(styleBlock)) {
      if (!isInteractiveDisabledSelector(rule.selector)) continue

      for (const opacity of rule.declarations.matchAll(/(?:^|;)\s*opacity:\s*([.\d]+)/g)) {
        if (Number(opacity[1]) < 1) {
          violations.push(`${relativePath}: ${rule.selector} uses opacity ${opacity[1]}`)
        }
      }

      for (const property of ['color', 'fill', 'stroke', 'background', 'background-color', 'border', 'border-color']) {
        const declarationPattern = new RegExp(`(?:^|;)\\s*${property}:\\s*([^;]+)`, 'g')
        for (const declaration of rule.declarations.matchAll(declarationPattern)) {
          const expectedToken = ['color', 'fill', 'stroke'].includes(property)
            ? '--ys-disabled-text'
            : '--ys-disabled-bg'
          if (!declaration[1].includes(expectedToken)) {
            violations.push(`${relativePath}: ${rule.selector} sets non-neutral ${property}`)
          }
        }
      }

      for (const shadow of rule.declarations.matchAll(/(?:^|;)\s*box-shadow:\s*([^;]+)/g)) {
        if (!/^none(?:\s*!important)?$/.test(shadow[1].trim())) {
          violations.push(`${relativePath}: ${rule.selector} keeps a disabled shadow`)
        }
      }
    }
  }

  for (const match of source.matchAll(/disabled:opacity-[^\s"'`<>]+/g)) {
    violations.push(`${relativePath}: ${match[0]} bypasses the shared opacity contract`)
  }

  return violations
}

function disabledRepresentationViolations(relativePath, source) {
  const violations = []
  const lineNumber = (index) => source.slice(0, index).split(/\r?\n/).length

  for (const match of source.matchAll(/(['"`])([^'"`\r\n]*cursor-not-allowed[^'"`\r\n]*)\1/g)) {
    const classValue = match[2]
    if (/\bopacity-(?!100\b)\d+\b/.test(classValue)) {
      violations.push(`${relativePath}:${lineNumber(match.index)} disabled class uses reduced opacity`)
    }
    if (/\b(?:text|fill|stroke)-[^\s]+\/(?:\d+|\[[^\]]+\])/.test(classValue)) {
      violations.push(`${relativePath}:${lineNumber(match.index)} disabled class uses an alpha foreground`)
    }
  }

  for (const match of source.matchAll(/[^\r\n]*disabled[^\r\n]*(?:text|fill|stroke)-[^\s'"`<>]+\/(?:\d+|\[[^\]]+\])[^\r\n]*/gi)) {
    violations.push(`${relativePath}:${lineNumber(match.index)} disabled branch uses an alpha foreground`)
  }

  if (relativePath === 'directives/permission.js') {
    for (const match of source.matchAll(/el\.style\.(opacity|filter)\s*=/g)) {
      violations.push(`${relativePath}:${lineNumber(match.index)} permission directive writes inline ${match[1]}`)
    }
  }

  return violations
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
    const blockStart = findNextOpeningBrace(source, mediaStart)
    assert.notEqual(blockStart, -1, 'print media block must have an opening brace')

    const blockEnd = findMatchingBrace(source, blockStart)
    cursor = blockEnd
  }

  return result
}

for (const [name, declaration] of [
  ['double-quoted opening braces', 'content: "{";'],
  ['single-quoted closing braces', "content: '}';"],
  ['escaped quotes with braces', String.raw`content: "escaped \" quote with { {"; --single: 'escaped \' quote with }';`],
  ['block comments containing braces', '/* ignored { { brace } */']
]) {
  test(`CSS rule scanner ignores ${name}`, () => {
    const fixture = `.probe { ${declaration} color: #0f172a; } .after { color: #475569; }`
    const probe = cssRule(fixture, '.probe')
    const after = cssRule(fixture, '.after')
    assert.match(probe.declarations, /color:\s*#0f172a/)
    assert.match(after.declarations, /color:\s*#475569/)
  })
}

for (const [name, prefix] of [
  ['a top-level comment brace', '/* top-level { ignored */'],
  ['a top-level quoted brace', '@custom "{";']
]) {
  test(`CSS rule scanner ignores ${name} before a rule`, () => {
    const fixture = `${prefix}\n.probe { color: #0f172a; } .after { color: #475569; }`
    assert.match(cssRule(fixture, '.probe').declarations, /color:\s*#0f172a/)
    assert.match(cssRule(fixture, '.after').declarations, /color:\s*#475569/)
  })
}

test('CSS rule scanner ignores escaped braces while finding an opening brace', () => {
  const fixture = String.raw`.class\{name { color: #0f172a; } .after { color: #475569; }`
  assert.match(cssRule(fixture, String.raw`.class\{name`).declarations, /color:\s*#0f172a/)
  assert.match(cssRule(fixture, '.after').declarations, /color:\s*#475569/)
})

test('CSS rule scanner ignores escaped braces while matching a rule block', () => {
  for (const fixture of [
    String.raw`.probe { --escaped-open: \{; color: #0f172a; } .after { color: #475569; }`,
    String.raw`.probe { --escaped-close: \}; color: #0f172a; } .after { color: #475569; }`
  ]) {
    assert.match(cssRule(fixture, '.probe').declarations, /color:\s*#0f172a/)
    assert.match(cssRule(fixture, '.after').declarations, /color:\s*#475569/)
  }
})

test('alpha-primary foreground scanner covers fill, stroke, utilities, and attributes', () => {
  const fixtures = [
    ['fixture.css', '.icon { fill: rgb(var(--ys-primary-rgb) / 0.4); }'],
    ['fixture.css', '.icon { stroke: rgba(15, 118, 110, 0.45); }'],
    ['fixture.vue', '<svg class="fill-primary/40 stroke-primary/50"></svg>'],
    ['fixture.vue', '<path fill="rgba(15, 118, 110, 0.4)" stroke="rgb(var(--ys-primary-rgb) / 45%)" />']
  ]

  for (const [relativePath, source] of fixtures) {
    assert.ok(alphaPrimaryForegrounds(relativePath, source).length > 0, source)
  }
})

test('disabled selector audit recognizes controls without classifying status labels', () => {
  const controlFixture = `
    .permission-action-disabled {
      color: rgba(100, 116, 139, 0.5);
      background: rgba(226, 232, 240, 0.45);
      opacity: 0.55;
    }
    .disabled-control {
      color: #b45309;
    }
    button.disabled {
      background: #dcfce7;
    }
  `
  const statusFixture = `
    .employee-disabled-status {
      color: #475569;
      background: #f1f5f9;
      opacity: 0.75;
    }
    .permission-result.is-disabled {
      color: #475569;
      background: #f1f5f9;
    }
  `
  const violations = disabledRuleViolations('fixture.css', controlFixture)
  assert.ok(violations.some((item) => item.includes('opacity 0.55')))
  assert.ok(violations.some((item) => item.includes('non-neutral color')))
  assert.ok(violations.some((item) => item.includes('non-neutral background')))
  assert.ok(violations.some((item) => item.includes('.disabled-control')))
  assert.ok(violations.some((item) => item.includes('button.disabled')))
  assert.deepEqual(disabledRuleViolations('fixture.css', statusFixture), [])
})

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

  assert.equal(warningRules.length, allowedWarningSelectors.size)
  for (const rule of warningRules) {
    assert.doesNotMatch(rule.declarations, /--ys-primary(?:\b|-)/)
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

test('employee inactive badges retain accessible business-status semantics', () => {
  assert.match(employeePermissionSource, /isEffective\(data\) \? 'is-enabled' : 'is-inactive'/)
  const permissionBadge = cssRule(vueStyle(employeePermissionSource), '.permission-result.is-inactive')
  assert.match(permissionBadge.declarations, /background:\s*#f1f5f9/)
  assert.match(permissionBadge.declarations, /color:\s*#475569/)
  assert.doesNotMatch(permissionBadge.declarations, /--ys-disabled/)

  assert.match(employeeSource, /Number\(node\.\$\$data\?\.status\) === 1 \? 'enabled' : 'inactive'/)
  for (const [selector, background] of [
    ['.org-chart-status.inactive', '#e2e8f0'],
    ['.org-chart-card.root .org-chart-status.inactive', '#f1f5f9']
  ]) {
    const statusBadge = cssRule(vueStyle(employeeSource), selector)
    assert.match(statusBadge.declarations, new RegExp(`background:\\s*${background}`))
    assert.match(statusBadge.declarations, /color:\s*#475569/)
    assert.doesNotMatch(statusBadge.declarations, /--ys-disabled/)
    assertNormalTextContrast('#475569', background, selector)
  }
  assertNormalTextContrast('#475569', '#f1f5f9', 'permission inactive badge')
})

test('management source has no semantic alpha-primary foregrounds', () => {
  const findings = []
  for (const filePath of listSourceFiles()) {
    const relativePath = sourceRelativePath(filePath)
    findings.push(...alphaPrimaryForegrounds(relativePath, readFileSync(filePath, 'utf8')))
  }

  const usedAllowlistEntries = new Set()
  const semanticFindings = findings.filter((finding) => {
    const allowlistIndex = alphaPrimaryForegroundAllowlist.findIndex((entry, index) => (
      !usedAllowlistEntries.has(index)
      && entry.relativePath === finding.relativePath
      && entry.token === finding.token
    ))
    if (allowlistIndex === -1) return true
    usedAllowlistEntries.add(allowlistIndex)
    return false
  })

  for (const entry of alphaPrimaryForegroundAllowlist) {
    assert.ok(entry.rationale.length > 20, `${entry.relativePath} allowlist needs a rationale`)
  }
  assert.deepEqual(
    semanticFindings,
    [],
    `alpha-primary semantic foregrounds:\n${semanticFindings.map((item) => `${item.relativePath}:${item.line} ${item.token}`).join('\n')}`
  )
  assert.equal(usedAllowlistEntries.size, alphaPrimaryForegroundAllowlist.length)
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

test('all native and aria-disabled button-like controls use the shared neutral cascade', () => {
  const genericDisabledRule = cssRule(styleSource, `
    button:disabled,
    button[aria-disabled="true"],
    [role="button"][aria-disabled="true"],
    [role="menuitem"][aria-disabled="true"],
    .permission-action-disabled[aria-disabled="true"],
    .is-permission-disabled[aria-disabled="true"],
    .el-button.is-disabled,
    .el-button[aria-disabled="true"]
  `)
  const genericDisabledDescendantRule = cssRule(styleSource, `
    :is(button:disabled, button[aria-disabled="true"], [role="button"][aria-disabled="true"], [role="menuitem"][aria-disabled="true"], .permission-action-disabled[aria-disabled="true"], .is-permission-disabled[aria-disabled="true"], .el-button.is-disabled, .el-button[aria-disabled="true"])
      *
  `)

  assert.match(genericDisabledRule.declarations, /color:\s*var\(--ys-disabled-text\)\s*!important/)
  assert.match(genericDisabledRule.declarations, /border-color:\s*var\(--ys-disabled-bg\)\s*!important/)
  assert.match(genericDisabledRule.declarations, /background:\s*var\(--ys-disabled-bg\)\s*!important/)
  assert.match(genericDisabledRule.declarations, /box-shadow:\s*none\s*!important/)
  assert.match(genericDisabledRule.declarations, /opacity:\s*1\s*!important/)
  assert.match(genericDisabledDescendantRule.declarations, /color:\s*var\(--ys-disabled-text\)\s*!important/)

  const liveControls = [
    [orderSource, 'order-warning-refresh-btn'],
    [orderSource, 'order-stale-refresh-btn'],
    [receiptSource, 'editor-save-btn']
  ]
  for (const [source, className] of liveControls) {
    const classIndex = source.indexOf(`class="${className}"`)
    const tagStart = source.lastIndexOf('<el-button', classIndex)
    const tagEnd = source.indexOf('>', classIndex)
    const openingTag = source.slice(tagStart, tagEnd + 1)
    assert.notEqual(classIndex, -1, `missing live ${className} control`)
    assert.match(openingTag, /<el-button\b/)
    assert.match(openingTag, /:disabled=/)
  }

  for (const [source, selector] of [
    [vueStyle(orderSource), '.order-warning-refresh-btn:disabled'],
    [vueStyle(orderSource), '.order-table-row .order-stale-refresh-btn:disabled'],
    [vueStyle(receiptSource), '.editor-save-btn:disabled']
  ]) {
    const declarations = cssRule(source, selector).declarations
    assert.match(declarations, /color:\s*var\(--ys-disabled-text\)/)
    assert.match(declarations, /border-color:\s*var\(--ys-disabled-bg\)/)
    assert.match(declarations, /background:\s*var\(--ys-disabled-bg\)/)
    assert.match(declarations, /box-shadow:\s*none/)
    assert.match(declarations, /opacity:\s*1/)
  }

  assert.match(
    cssRule(vueStyle(orderSource), '.order-table-row .order-stale-refresh-btn:disabled').declarations,
    /color:\s*var\(--ys-disabled-text\)\s*!important/
  )
  assert.match(
    cssRule(vueStyle(orderSource), '.order-table-row .order-stale-refresh-btn:disabled .material-symbols-outlined').declarations,
    /color:\s*var\(--ys-disabled-text\)\s*!important/
  )
})

test('order rows preserve table semantics while permission-disabled controls stay neutral', () => {
  const rowClassIndex = orderSource.indexOf('v-for="row in visibleOrderRows"')
  const rowStart = orderSource.lastIndexOf('<tr', rowClassIndex)
  const rowEnd = orderSource.indexOf('>', rowClassIndex)
  const rowTag = orderSource.slice(rowStart, rowEnd + 1)

  assert.match(rowTag, /@click="openDetail\(row\.orderId, row\)"/)
  assert.doesNotMatch(rowTag, /role="button"/)
  assert.doesNotMatch(rowTag, /\btabindex=/)
  assert.doesNotMatch(rowTag, /\baria-disabled=/)
  assert.doesNotMatch(rowTag, /@keydown/)
  assert.doesNotMatch(rowTag, /permissionDisabledClass|order-detail-disabled/)

  const detailClassIndex = orderSource.indexOf(':disabled="!canViewOrderDetail(row)"', rowEnd)
  const detailButtonStart = orderSource.lastIndexOf('<el-button', detailClassIndex)
  const detailButtonEnd = orderSource.indexOf('>', detailClassIndex)
  const detailButton = orderSource.slice(detailButtonStart, detailButtonEnd + 1)
  assert.match(detailButton, /:class="permissionDisabledClass\(!canViewOrderDetail\(row\)\)"/)
  assert.match(detailButton, /:disabled="!canViewOrderDetail\(row\)"/)
  assert.match(detailButton, /@click\.stop="openDetail\(row\.orderId, row\)"/)

  const localDisabledRule = cssRule(vueStyle(orderSource), `
    .permission-action-disabled,
    .permission-action-disabled:hover
  `)
  const localDescendantRule = cssRule(
    vueStyle(orderSource),
    '.permission-action-disabled *'
  )
  for (const declaration of [
    /color:\s*var\(--ys-disabled-text\)\s*!important/,
    /border-color:\s*var\(--ys-disabled-bg\)\s*!important/,
    /background:\s*var\(--ys-disabled-bg\)\s*!important/,
    /box-shadow:\s*none\s*!important/,
    /opacity:\s*1\s*!important/,
    /transform:\s*none\s*!important/
  ]) {
    assert.match(localDisabledRule.declarations, declaration)
  }
  assert.match(localDescendantRule.declarations, /color:\s*var\(--ys-disabled-text\)\s*!important/)
})

test('permission directive implementation preserves native disabled and avoids inline appearance', () => {
  assert.match(permissionDirectiveSource, /createPermissionDirectiveLifecycle/)
  assert.match(permissionStateSource, /beforeUpdate/)
  for (const [relativePath, source] of [
    ['directives/permission.js', permissionDirectiveSource],
    ['directives/permissionState.js', permissionStateSource]
  ]) {
    assert.doesNotMatch(
      source,
      /\b(?:el|element)\s*(?:\.disabled|\[\s*['"]disabled['"]\s*\])\s*=|\b(?:el|element)\.(?:set|remove|toggle)Attribute\(\s*['"]disabled['"]/,
      `${relativePath} must not mutate native disabled state`
    )
    assert.doesNotMatch(
      source,
      /\b(?:el|element)\.style\.(?:opacity|filter)\s*=/,
      `${relativePath} must not write inline disabled appearance`
    )
  }
})

test('source-local disabled rules never restore status colors or reduced opacity', () => {
  const violations = []
  for (const filePath of listSourceFiles()) {
    const relativePath = sourceRelativePath(filePath)
    try {
      const source = readFileSync(filePath, 'utf8')
      violations.push(...disabledRuleViolations(relativePath, source))
      violations.push(...disabledRepresentationViolations(relativePath, source))
    } catch (error) {
      assert.fail(`${relativePath}: ${error.message}`)
    }
  }
  assert.deepEqual(violations, [], `disabled-state violations:\n${violations.join('\n')}`)
})

test('primary controls expose non-disabled hover and active feedback before disabled rules', () => {
  const baseRule = cssRule(styleSource, `
    .bg-primary,
    .el-button--primary,
    .function-action-primary
  `)
  const hoverRule = cssRule(styleSource, `
    .bg-primary:not(:disabled):not([aria-disabled="true"]):hover,
    .el-button--primary:not(.is-disabled):not(:disabled):not([aria-disabled="true"]):hover,
    .function-action-primary:not(:disabled):not([aria-disabled="true"]):hover
  `)
  const activeRule = cssRule(styleSource, `
    .bg-primary:not(:disabled):not([aria-disabled="true"]):active,
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
  const genericDisabledRule = cssRule(styleSource, `
    button:disabled,
    button[aria-disabled="true"],
    [role="button"][aria-disabled="true"],
    [role="menuitem"][aria-disabled="true"],
    .permission-action-disabled[aria-disabled="true"],
    .is-permission-disabled[aria-disabled="true"],
    .el-button.is-disabled,
    .el-button[aria-disabled="true"]
  `)
  const hoverTransformRule = cssRule(styleSource, `
    .function-action-primary:not(:disabled):not([aria-disabled="true"]):hover,
    .function-action-secondary:not(:disabled):not([aria-disabled="true"]):hover,
    .function-action-dark:not(:disabled):not([aria-disabled="true"]):hover
  `)
  const activeTransformRule = cssRule(styleSource, `
    .function-action-primary:not(:disabled):not([aria-disabled="true"]):active,
    .function-action-secondary:not(:disabled):not([aria-disabled="true"]):active,
    .function-action-dark:not(:disabled):not([aria-disabled="true"]):active
  `)

  assert.match(hoverRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(hoverRule.declarations, /background:\s*var\(--ys-primary-hover\)\s*!important/)
  assert.match(activeRule.declarations, /color:\s*var\(--ys-on-primary\)\s*!important/)
  assert.match(activeRule.declarations, /background:\s*var\(--ys-primary-dark\)\s*!important/)
  assertNormalTextContrast('#ffffff', '#0f766e', 'primary hover copy')
  assertNormalTextContrast('#ffffff', '#115e59', 'primary active copy')
  assert.match(hoverTransformRule.declarations, /transform:\s*translatey\(-1px\)/)
  assert.match(activeTransformRule.declarations, /transform:\s*scale\(0\.97\)/)
  assert.doesNotMatch(styleSource, /\.function-action-primary:hover\s*,/)
  assert.doesNotMatch(styleSource, /\.function-action-primary:active\s*,/)
  assert.ok(baseRule.start < hoverRule.start)
  assert.ok(hoverRule.start < activeRule.start)
  assert.ok(activeRule.start < disabledRule.start)
  assert.ok(activeRule.start < genericDisabledRule.start)
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
