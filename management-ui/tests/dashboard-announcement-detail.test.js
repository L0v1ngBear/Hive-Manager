import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const dashboard = fs.readFileSync(path.join(uiRoot, 'src/views/dashboard/index.vue'), 'utf8')

test('dashboard announcement cards open the selected announcement locally', () => {
  assert.equal(
    dashboard.match(/@click="openAnnouncementDetail\(item\)"/g)?.length,
    2,
    'both regular and important announcement cards must open their selected announcement'
  )
  assert.match(dashboard, /<el-dialog[\s\S]*v-model="announcementDetailVisible"[\s\S]*title="公告详情"/)
  assert.match(dashboard, /selectedAnnouncement\.title/)
  assert.match(dashboard, /selectedAnnouncement\.content/)
  assert.match(dashboard, /function openAnnouncementDetail\(item\)[\s\S]*selectedAnnouncement\.value = item[\s\S]*announcementDetailVisible\.value = true/)
})

test('dashboard keeps the announcement center as an explicit secondary action', () => {
  assert.match(dashboard, />查看全部公告<\/el-button>/)
  assert.match(dashboard, /function openAnnouncementCenter\(\)[\s\S]*router\.push\('\/function\/announcement'\)/)
  assert.match(dashboard, /type="button"[\s\S]*@click="openAnnouncementDetail\(item\)"/)
})
