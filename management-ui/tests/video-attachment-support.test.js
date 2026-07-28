import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('general and document attachment pickers accept common video formats up to 200MB', () => {
  const component = read('src/components/DragAttachmentUpload.vue')
  const documentPage = read('src/views/function/document/document.vue')
  const pages = [
    'src/views/function/approval/approvalCenter.vue',
    'src/views/function/badProduct/badProduct.vue',
    'src/views/function/installationTask/installationTask.vue',
    'src/views/function/order/order.vue'
  ].map(read).join('\n')

  for (const extension of ['.mp4', '.mov', '.m4v', '.avi', '.mkv', '.webm', '.3gp']) {
    assert.ok(component.includes(extension), `shared attachment picker must accept ${extension}`)
    assert.ok(documentPage.includes(extension), `document picker must accept ${extension}`)
  }
  assert.match(documentPage, /200 \* 1024 \* 1024/)
  assert.doesNotMatch(pages, /10 \* 1024 \* 1024/)
})

test('video attachment requests and reverse proxy allow slow large uploads', () => {
  const apiFiles = [
    'src/views/function/approval/api/approval.js',
    'src/views/function/badProduct/api/badProduct.js',
    'src/views/function/document/api/document.js',
    'src/views/function/installationTask/api/installationTask.js',
    'src/views/function/order/api/order.js'
  ].map(read)
  const nginx = fs.readFileSync(path.join(root, '..', 'deploy/nginx/conf.d/hive.conf'), 'utf8')

  for (const source of apiFiles) assert.match(source, /timeout:\s*600000/)
  assert.match(nginx, /client_max_body_size\s+210m/)
  assert.match(nginx, /client_body_timeout\s+600s/)
  assert.match(nginx, /proxy_send_timeout\s+600s/)
  assert.match(nginx, /proxy_read_timeout\s+600s/)
})

test('order archives up to 800MB use resumable generic chunk uploads', () => {
  const chunkedUpload = read('src/utils/chunkedAttachmentUpload.js')
  const orderPage = read('src/views/function/order/order.vue')
  const orderApi = read('src/views/function/order/api/order.js')

  assert.match(chunkedUpload, /LARGE_FILE_THRESHOLD\s*=\s*20\s*\*\s*1024\s*\*\s*1024/)
  assert.match(chunkedUpload, /Number\(file\?\.size \|\| 0\) > LARGE_FILE_THRESHOLD/)
  assert.match(chunkedUpload, /\/storage\/chunked-attachment\/\$\{module\}\/init/)
  assert.match(chunkedUpload, /\/storage\/chunked-attachment\/\$\{module\}\/\$\{init\.uploadId\}\/part/)
  assert.match(chunkedUpload, /\/storage\/chunked-attachment\/\$\{module\}\/\$\{init\.uploadId\}\/complete/)
  assert.match(orderApi, /uploadAttachmentWithChunks/)
  assert.match(orderPage, /800 \* 1024 \* 1024/)
  assert.match(orderPage, /单个不超过 800MB；已添加/)
  assert.doesNotMatch(orderPage, /大文件自动分片上传/)
})
