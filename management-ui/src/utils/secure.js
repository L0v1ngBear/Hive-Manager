function base64ToUint8Array(base64) {
  const binaryString = window.atob(base64)
  const bytes = new Uint8Array(binaryString.length)
  for (let index = 0; index < binaryString.length; index += 1) {
    bytes[index] = binaryString.charCodeAt(index)
  }
  return bytes
}

async function importAesKey(rawKey) {
  return window.crypto.subtle.importKey(
    'raw',
    rawKey,
    { name: 'AES-CBC' },
    false,
    ['decrypt']
  )
}

async function importMacKey(rawKey) {
  return window.crypto.subtle.importKey(
    'raw',
    rawKey,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['verify']
  )
}

export async function decryptPayload(responseKey, payload) {
  if (!payload?.iv || !payload?.ciphertext || !payload?.mac) {
    return payload
  }
  if (!responseKey) {
    throw new Error('登录状态异常，请重新登录')
  }
  if (!window.crypto?.subtle) {
    throw new Error('当前浏览器不支持响应解密')
  }

  const keyMaterial = base64ToUint8Array(responseKey)
  if (keyMaterial.length < 64) {
    throw new Error('登录信息异常，请重新登录')
  }

  const cipherKey = keyMaterial.slice(0, 32)
  const macKey = keyMaterial.slice(32, 64)
  const key = await importAesKey(cipherKey)
  const verifyKey = await importMacKey(macKey)
  const iv = base64ToUint8Array(payload.iv)
  const ciphertext = base64ToUint8Array(payload.ciphertext)
  const signatureRaw = base64ToUint8Array(payload.mac)
  const signContent = new TextEncoder().encode(`${payload.iv}.${payload.ciphertext}`)
  const valid = await window.crypto.subtle.verify(
    'HMAC',
    verifyKey,
    signatureRaw,
    signContent
  )
  if (!valid) {
    throw new Error('响应验签失败')
  }
  const plainBuffer = await window.crypto.subtle.decrypt(
    { name: 'AES-CBC', iv },
    key,
    ciphertext
  )
  const plainText = new TextDecoder().decode(plainBuffer)
  return JSON.parse(plainText)
}
