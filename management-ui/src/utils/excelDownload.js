async function extractBlobErrorMessage(blob) {
  const text = await blob.text()
  if (!text) return '下载失败，系统未返回有效 Excel 文件'
  try {
    const payload = JSON.parse(text)
    return payload?.msg || payload?.message || payload?.data?.msg || payload?.data?.message || '下载失败，请稍后重试'
  } catch {
    return text.replace(/\s+/g, ' ').trim().slice(0, 160) || '下载失败，请稍后重试'
  }
}

export async function ensureXlsxBlob(blob) {
  if (typeof Blob === 'undefined' || !(blob instanceof Blob) || blob.size === 0) {
    throw new Error('下载失败，系统未返回有效 Excel 文件')
  }
  const contentType = String(blob.type || '').toLowerCase()
  if (contentType.includes('json') || contentType.includes('text') || contentType.includes('html')) {
    throw new Error(await extractBlobErrorMessage(blob))
  }
  const signature = new Uint8Array(await blob.slice(0, 4).arrayBuffer())
  if (signature.length < 2 || signature[0] !== 0x50 || signature[1] !== 0x4b) {
    throw new Error('下载失败，系统未返回有效 Excel 文件，请刷新后重试')
  }
  return blob
}

export async function downloadXlsxBlob(blob, fileName, onInvalid) {
  try {
    await ensureXlsxBlob(blob)
  } catch (error) {
    if (typeof onInvalid === 'function') {
      onInvalid(error?.message || '下载失败，系统未返回有效 Excel 文件')
      return false
    }
    throw error
  }
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  try {
    link.click()
  } finally {
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(url), 0)
  }
  return true
}
