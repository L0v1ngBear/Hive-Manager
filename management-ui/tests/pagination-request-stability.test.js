import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const readView = (path) => readFileSync(new URL(`../../management-ui/src/views/function/${path}`, import.meta.url), 'utf8')
const views = [
  ['attendance/attendanceManagement.vue', 'async function fetchData'],
  ['badProduct/badProduct.vue', 'async function fetchData'],
  ['employee/employee.vue', 'const fetchEmployees = async'],
  ['equipment/equipment.vue', 'async function fetchDevices'],
  ['installationTask/installationTask.vue', 'async function loadTasks']
]

test('server-paged tables keep their current total while a page request is in flight', () => {
  for (const [path, functionStart] of views) {
    const source = readView(path)
    const start = source.indexOf(functionStart)
    const requestStart = source.indexOf('requestState.value = \'loading\'', start) >= 0
      ? source.indexOf('requestState.value = \'loading\'', start)
      : source.indexOf('loading.value = true', start)
    const tryStart = source.indexOf('try {', requestStart)
    const loadingSetup = source.slice(requestStart, tryStart)

    assert.ok(start >= 0, `${path} has its page-loading function`)
    assert.ok(requestStart >= 0 && tryStart >= 0, `${path} has a page-request loading phase`)
    assert.doesNotMatch(loadingSetup, /pagination\.total = 0|pagination\.pages = 0|total\.value = 0|totalPages\.value = 1/)
  }
})
