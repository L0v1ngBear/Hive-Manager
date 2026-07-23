import request from '@/utils/request.js'

const VIDEO_EXTENSIONS = ['mp4', 'mov', 'm4v', 'avi', 'mkv', 'webm', '3gp']
const STORAGE_KEY_PREFIX = 'hive:chunked-video:'

export function uploadAttachmentWithVideoChunks(file, fallbackUpload, module) {
  if (!isVideo(file)) return fallbackUpload()
  return uploadVideoInChunks(file, module)
}

async function uploadVideoInChunks(file, module) {
  const key = `${STORAGE_KEY_PREFIX}${module}:${file.name}:${file.size}:${file.lastModified}`
  const storedId = safeStorageGet(key)
  const init = await request({
    url: `/storage/chunked-video/${module}/init`, method: 'post',
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
        url: `/storage/chunked-video/${module}/${init.uploadId}/part/${partNumber}`,
        method: 'post', data: formData, timeout: 120000, showGlobalLoading: false
      })
    }
    const result = await request({
      url: `/storage/chunked-video/${module}/${init.uploadId}/complete`, method: 'post',
      timeout: 600000, showGlobalLoading: false
    })
    completed = true
    return result
  } finally {
    if (completed) safeStorageRemove(key)
  }
}

function isVideo(file) {
  if (!file) return false
  if (String(file.type || '').toLowerCase().startsWith('video/')) return true
  const extension = String(file.name || '').split('.').pop().toLowerCase()
  return VIDEO_EXTENSIONS.includes(extension)
}
function safeStorageGet(key) { try { return sessionStorage.getItem(key) } catch { return null } }
function safeStorageSet(key, value) { try { sessionStorage.setItem(key, value) } catch { /* browser storage unavailable */ } }
function safeStorageRemove(key) { try { sessionStorage.removeItem(key) } catch { /* browser storage unavailable */ } }
