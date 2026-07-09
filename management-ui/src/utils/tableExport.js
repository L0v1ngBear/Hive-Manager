import request from '@/utils/request'

const ACTION_HEADER_PATTERN = /^(操作|动作|Actions?)$/i
const MAX_CURRENT_PAGE_ROWS = 2000

function normalizeCellText(value) {
  return String(value || '')
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function safeFileName(value) {
  const name = normalizeCellText(value || '列表数据')
    .replace(/[\\/:*?"<>|]/g, '_')
    .slice(0, 80)
  return name || '列表数据'
}

function timestamp() {
  const date = new Date()
  const pad = (num) => String(num).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    '_',
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join('')
}

function isVisible(element) {
  if (!element) return false
  const style = window.getComputedStyle(element)
  return style.display !== 'none' && style.visibility !== 'hidden'
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function ensureXlsxBlob(blob) {
  if (typeof Blob === 'undefined' || !(blob instanceof Blob) || blob.size === 0) {
    throw new Error('导出失败，系统未返回有效 Excel 文件')
  }
  const contentType = String(blob.type || '').toLowerCase()
  if (contentType.includes('json') || contentType.includes('text') || contentType.includes('html')) {
    throw new Error(await extractBlobErrorMessage(blob))
  }
  const signature = new Uint8Array(await blob.slice(0, 4).arrayBuffer())
  const isXlsxZip = signature[0] === 0x50 && signature[1] === 0x4b
  if (!isXlsxZip) {
    throw new Error('导出失败，系统未返回有效 Excel 文件，请刷新后重试')
  }
}

async function extractBlobErrorMessage(blob) {
  const text = await blob.text()
  if (!text) {
    return '导出失败，系统未返回有效 Excel 文件'
  }
  try {
    const payload = JSON.parse(text)
    return payload?.msg || payload?.message || payload?.data?.msg || payload?.data?.message || '导出失败，请稍后重试'
  } catch (error) {
    return text.replace(/\s+/g, ' ').trim().slice(0, 160) || '导出失败，请稍后重试'
  }
}

function getTableTitle(table) {
  const pageRoot = table.closest('.function-page-shell') || document
  const title = pageRoot.querySelector('.function-page-title, h1, h2')
  return title?.textContent || document.title || '列表数据'
}

function getHeaders(table) {
  const thList = Array.from(table.querySelectorAll('thead th')).filter(isVisible)
  return thList.map((th, index) => {
    const text = normalizeCellText(th.textContent)
    return {
      index,
      text,
      skip: ACTION_HEADER_PATTERN.test(text)
    }
  })
}

function collectTableData(table, maxRows) {
  const headerMeta = getHeaders(table)
  const headers = headerMeta.filter((item) => !item.skip).map((item) => item.text || `列${item.index + 1}`)
  const rows = []

  Array.from(table.querySelectorAll('tbody tr')).forEach((tr) => {
    if (!isVisible(tr)) return
    const cells = Array.from(tr.children).filter(isVisible)
    const hasColspan = cells.some((cell) => Number(cell.getAttribute('colspan') || 1) > 1)
    if (hasColspan || cells.length === 0) return

    const row = cells
      .map((cell, index) => ({
        index,
        value: normalizeCellText(cell.innerText || cell.textContent)
      }))
      .filter((cell) => !headerMeta[cell.index]?.skip)
      .map((cell) => cell.value)

    if (row.some(Boolean)) {
      rows.push(row)
      if (rows.length > maxRows) {
        throw new Error(`当前页可导出数据不能超过 ${maxRows} 行，请缩小筛选范围或使用全量导出`)
      }
    }
  })

  return { headers, rows }
}

async function downloadTableExport({ title, fileName, sheetName, sourceModule, headers, rows, timeout }) {
  const baseName = safeFileName(fileName || title)
  const outputFileName = `${baseName}_${timestamp()}.xlsx`
  const blob = await request({
    url: '/export/table',
    method: 'post',
    data: {
      sheetName: safeFileName(sheetName || title).slice(0, 31) || 'Sheet1',
      fileName: outputFileName,
      sourceModule: sourceModule || '',
      headers,
      rows
    },
    responseType: 'blob',
    timeout
  })

  await ensureXlsxBlob(blob)
  downloadBlob(blob, outputFileName)
}

export async function exportTableElementToExcel(table, options = {}) {
  if (!table) {
    throw new Error('未找到可导出的列表表格')
  }

  const maxRows = Number(options.maxRows || MAX_CURRENT_PAGE_ROWS)
  const { headers, rows } = collectTableData(table, maxRows)
  if (headers.length === 0) {
    throw new Error('当前列表缺少可导出的表头')
  }
  if (rows.length === 0) {
    throw new Error('当前列表暂无可导出的数据')
  }

  const title = options.title || getTableTitle(table)
  await downloadTableExport({
    title,
    fileName: options.fileName,
    sheetName: options.sheetName,
    sourceModule: options.sourceModule,
    headers,
    rows,
    timeout: 30000
  })
}

export async function exportRowsToExcel(options = {}) {
  const headers = Array.isArray(options.headers) ? options.headers.map(normalizeCellText).filter(Boolean) : []
  const rows = Array.isArray(options.rows)
    ? options.rows.map((row) => (Array.isArray(row) ? row : []).map(normalizeCellText)).filter((row) => row.some(Boolean))
    : []
  if (headers.length === 0) {
    throw new Error('缺少可导出的表头')
  }
  if (rows.length === 0) {
    throw new Error('暂无可导出的数据')
  }

  const title = options.title || '列表数据'
  await downloadTableExport({
    title,
    fileName: options.fileName,
    sheetName: options.sheetName,
    sourceModule: options.sourceModule,
    headers,
    rows,
    timeout: Number(options.timeout || 60000)
  })
}
