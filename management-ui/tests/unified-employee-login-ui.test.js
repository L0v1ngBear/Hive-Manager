import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const readSource = (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('employee login describes supported account identifiers and first activation', async () => {
  const source = await readSource('../src/views/Login.vue')

  assert.match(source, /placeholder="请输入工号、手机号或登录账号"/)
  assert.doesNotMatch(source, /员工编号或邮箱/)
  assert.match(source, /首次登录\s*\/\s*忘记密码/)
  assert.match(source, /sendPasswordResetCode/)
  assert.match(source, /resetPassword/)
  assert.match(source, /loginForm\.username\s*=\s*phone/)
})

test('employee creation presents one-time activation guidance from EmployeeCreateVO', async () => {
  const source = await readSource('../src/views/function/employee/employeeCreate.vue')

  assert.match(source, /const\s+createResult\s*=\s*await\s+createEmployee\(payload\)/)
  assert.match(source, /createResult\.empNo/)
  assert.match(source, /createResult\.phoneMask/)
  assert.match(source, /createResult\.activationRequired/)
  assert.match(source, /员工档案创建成功/)
  assert.match(source, /首次登录\s*\/\s*忘记密码/)
  assert.doesNotMatch(source, /默认密码|初始密码|sharedPassword/)
})
