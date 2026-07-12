import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

function read(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertElementComponents(source, components, label) {
  for (const component of components) {
    const tag = component.replace(/^El/, '').replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`).slice(1)
    assert.match(source, new RegExp(`<el-${tag}(?:\\s|>)`), `${label} must render ${component}`)
    assert.match(
      source,
      new RegExp(`import\\s*\\{[^}]*\\b${component}\\b[^}]*\\}\\s*from\\s*['\"]element-plus['\"]`, 's'),
      `${label} must explicitly import ${component}`
    )
  }
}

const approval = read('src/views/function/approval/approvalCenter.vue')
const dashboard = read('src/views/dashboard/index.vue')
const navbar = read('src/layout/components/Navbar.vue')
const sidebar = read('src/layout/components/Sidebar.vue')
const login = read('src/views/Login.vue')
const joinOrganization = read('src/views/JoinOrganization.vue')
const forcePasswordChange = read('src/views/ForcePasswordChange.vue')
const noPermission = read('src/views/NoPermission.vue')

test('approval center uses explicit Element Plus workflow controls', () => {
  assertElementComponents(
    approval,
    [
      'ElTabs',
      'ElTabPane',
      'ElBadge',
      'ElTable',
      'ElTableColumn',
      'ElPagination',
      'ElForm',
      'ElFormItem',
      'ElDescriptions',
      'ElDescriptionsItem',
      'ElInput',
      'ElSelect',
      'ElOption',
      'ElButton',
      'ElDatePicker',
      'ElInputNumber',
      'ElTag',
      'ElEmpty'
    ],
    'approval center'
  )
})

test('approval center retains all five approval dispatch families and permission guards', () => {
  for (const type of ['order', 'quality', 'finance', 'leave', 'resignation']) {
    assert.match(approval, new RegExp(`value:\\s*['\"]${type}['\"]`), `approval type ${type} must remain configured`)
  }
  for (const api of [
    'listOrderApprovals',
    'listQualityApprovals',
    'listFinanceApprovals',
    'listLeaveApprovals',
    'listResignationApprovals',
    'auditOrderApproval',
    'auditQualityApproval',
    'auditFinanceApproval',
    'auditLeaveApproval',
    'auditResignationApproval'
  ]) {
    assert.match(approval, new RegExp(`\\b${api}\\b`), `approval API ${api} must remain wired`)
  }
  assert.match(approval, /accessibleTabs/)
  assert.match(approval, /activeTabCanViewList/)
  assert.match(approval, /ElMessage\.warning\(['\"]当前账号暂无权限/)
})

test('dashboard and navigation use explicit Element Plus shell controls', () => {
  assertElementComponents(dashboard, ['ElButton', 'ElEmpty'], 'dashboard')
  assertElementComponents(navbar, ['ElPopover', 'ElDropdown', 'ElDropdownMenu', 'ElDropdownItem', 'ElBadge', 'ElButton'], 'navbar')
  assertElementComponents(sidebar, ['ElBadge', 'ElButton'], 'sidebar')

  assert.match(navbar, /ElMessage\.warning\(/)
  assert.match(navbar, /handleSearchItemClick/)
  assert.match(sidebar, /decorateAccessItems/)
  assert.match(sidebar, /handleMenuNavigate/)
})

test('authentication views use explicitly imported Element Plus form controls', () => {
  assertElementComponents(login, ['ElForm', 'ElFormItem', 'ElInput', 'ElCheckbox', 'ElButton', 'ElDialog'], 'login')
  assertElementComponents(joinOrganization, ['ElForm', 'ElFormItem', 'ElInput', 'ElButton'], 'join organization')
  assertElementComponents(forcePasswordChange, ['ElForm', 'ElFormItem', 'ElInput', 'ElButton'], 'force password change')
  assertElementComponents(noPermission, ['ElButton'], 'no permission')
})

test('login keeps password, memory, scan, and redirect flows unchanged', () => {
  for (const contract of [
    'login({',
    'password: loginForm.password',
    'saveLoginMemory({',
    'createScanLoginSession()',
    'getScanLoginStatus(',
    'normalizeLoginRedirect(',
    "router.push('/join-organization')",
    'resetPassword({'
  ]) {
    assert.ok(login.includes(contract), `login contract must retain ${contract}`)
  }
  assert.match(joinOrganization, /joinOrganization\(\{/)
  assert.match(forcePasswordChange, /changeInitialPassword\(\{/)
})
