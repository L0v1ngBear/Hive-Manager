import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const readSource = (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('navbar shows the employee position from the signed-in session instead of a fixed label', async () => {
  const navbar = await readSource('../src/layout/components/Navbar.vue')

  assert.doesNotMatch(navbar, /运营管理/)
  assert.match(
    navbar,
    /const roleLabel = computed\(\(\) => userStore\.userInfo\?\.positionName \|\| '未设置职位'\)/
  )
})

test('user store keeps the employee position returned by login and the session refresh', async () => {
  const store = await readSource('../src/stores/user.js')

  const persisted = store.match(/positionName: loginData\.positionName/g) || []
  assert.equal(persisted.length, 2)
})

test('navbar position and the employee list read the same positionName fact', async () => {
  const navbar = await readSource('../src/layout/components/Navbar.vue')
  const employeeList = await readSource('../src/views/function/employee/employee.vue')

  assert.match(navbar, /positionName/)
  assert.match(employeeList, /positionName/)
})
