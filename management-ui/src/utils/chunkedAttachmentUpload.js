import request from '@/utils/request.js'

const LARGE_FILE_THRESHOLD = 20 * 1024 * 1024
const STORAGE_KEY_PREFIX = 'hive:chunked-attachment:'

export function uploadAttachmentWithChunks(file, fallbackUpload, module, options = {}) {
  if (!shouldUseChunks(file)) return fallbackUpload()
  return uploadFileInChunks(file, module, options)
}

async function uploadFileInChunks(file, module, options) {
  const key = `${STORAGE_KEY_PREFIX}${module}:${file.name}:${file.size}:${file.lastModified}`
  const storedId = safeStorageGet(key)
  const init = await request({
    url: `/storage/chunked-attachment/${module}/init`, method: 'post',
    data: { uploadId: storedId || undefined, fileName: file.name, contentType: file.type, fileSize: file.size },
    timeout: 30000, showGlobalLoading: false
  })
  safeStorageSet(key, init.uploadId)
  let completed = false
  try {
    const uploaded = new Set(init.uploadedParts || [])
    for (let partNumber = 1; partNumber <= init.totalParts; partNumber += 1) {
      if (uploaded.has(partNumber)) continue
      const start = (partNumber - 1) * init.chunkSize
      const formData = new FormData()
      formData.append('file', file.slice(start, Math.min(start + init.chunkSize, file.size)), file.name)
      await request({
        url: `/storage/chunked-attachment/${module}/${init.uploadId}/part/${partNumber}`,
        method: 'post', data: formData, timeout: 120000, showGlobalLoading: false
      })
    }
    const result = typeof options.complete === 'function'
      ? await options.complete(init.uploadId)
      : await request({
          url: `/storage/chunked-attachment/${module}/${init.uploadId}/complete`, method: 'post',
          timeout: 1800000, showGlobalLoading: false
        })
    completed = true
    return result
  } finally {
    if (completed) safeStorageRemove(key)
  }
}

function shouldUseChunks(file) {
  return Number(file?.size || 0) > LARGE_FILE_THRESHOLD
}
function safeStorageGet(key) { try { return sessionStorage.getItem(key) } catch { return null } }
function safeStorageSet(key, value) { try { sessionStorage.setItem(key, value) } catch { /* browser storage unavailable */ } }
function safeStorageRemove(key) { try { sessionStorage.removeItem(key) } catch { /* browser storage unavailable */ } }
