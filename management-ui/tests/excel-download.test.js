import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

import { downloadXlsxBlob, ensureXlsxBlob } from '../src/utils/excelDownload.js'

test('Excel 下载校验拒绝空文件和非 XLSX 响应', async () => {
  await assert.rejects(() => ensureXlsxBlob(new Blob()), /未返回有效 Excel 文件/)
  await assert.rejects(
    () => ensureXlsxBlob(new Blob([JSON.stringify({ msg: '模板生成失败' })], { type: 'application/json' })),
    /模板生成失败/
  )
  await assert.rejects(
    () => ensureXlsxBlob(new Blob(['not-an-xlsx'], { type: 'application/octet-stream' })),
    /未返回有效 Excel 文件/
  )
})

test('Excel 下载校验接受 ZIP 格式的 XLSX 文件头', async () => {
  const blob = new Blob([new Uint8Array([0x50, 0x4b, 0x03, 0x04, 0x00])], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })

  assert.equal(await ensureXlsxBlob(blob), blob)
})

test('页面可接收并展示空文件校验信息，而不会触发无效下载', async () => {
  let message = ''
  const downloaded = await downloadXlsxBlob(new Blob(), '客户导入模板.xlsx', (value) => {
    message = value
  })

  assert.equal(downloaded, false)
  assert.match(message, /未返回有效 Excel 文件/)
})

test('所有 Excel 下载入口统一使用共享文件校验器', async () => {
  const files = [
    '../src/utils/tableExport.js',
    '../src/views/function/attendance/attendanceManagement.vue',
    '../src/views/function/afterSales/afterSales.vue',
    '../src/views/function/customer/customer.vue',
    '../src/views/function/employee/employee.vue',
    '../src/views/function/inventory/inventory.vue',
    '../src/views/function/price/price.vue'
  ]

  for (const file of files) {
    const source = await readFile(new URL(file, import.meta.url), 'utf8')
    assert.match(source, /downloadXlsxBlob/)
  }
})
