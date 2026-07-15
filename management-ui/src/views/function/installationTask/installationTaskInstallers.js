export const MAX_INSTALLERS = 20
export const MAX_INSTALLER_NAME_LENGTH = 50
export const MAX_INSTALLER_PHONE_LENGTH = 40

export function createInstaller() {
  return { name: '', phone: '' }
}

export function addInstaller(source) {
  const installers = Array.isArray(source) ? source : []
  if (installers.length >= MAX_INSTALLERS) {
    return { added: false, installers }
  }
  return { added: true, installers: [...installers, createInstaller()] }
}

export function removeInstaller(source, index) {
  const installers = Array.isArray(source) ? source : []
  return installers.filter((_, rowIndex) => rowIndex !== index)
}

export function cloneInstallers(source) {
  if (!Array.isArray(source)) return []
  return source.map((installer) => ({
    name: String(installer?.name || ''),
    phone: String(installer?.phone || '')
  }))
}

export function buildInstallerPayload(source) {
  return cloneInstallers(source).map((installer) => ({
    name: installer.name.trim(),
    phone: installer.phone.trim()
  }))
}

export function validateInstallers(source, status) {
  const installers = buildInstallerPayload(source)
  if (installers.length > MAX_INSTALLERS) {
    return invalid(`安装人员最多添加 ${MAX_INSTALLERS} 名`)
  }
  const uniquePairs = new Set()
  for (let index = 0; index < installers.length; index += 1) {
    const installer = installers[index]
    const row = index + 1
    if (!installer.name) return invalid(`第 ${row} 名安装人员的姓名不能为空`)
    if (!installer.phone) return invalid(`第 ${row} 名安装人员的联系电话不能为空`)
    if (Array.from(installer.name).length > MAX_INSTALLER_NAME_LENGTH) {
      return invalid(`第 ${row} 名安装人员的姓名最多 50 字`)
    }
    if (Array.from(installer.phone).length > MAX_INSTALLER_PHONE_LENGTH) {
      return invalid(`第 ${row} 名安装人员的联系电话最多 40 字`)
    }
    const pair = JSON.stringify([installer.name, installer.phone])
    if (uniquePairs.has(pair)) return invalid('不能重复添加相同的安装人员和联系电话')
    uniquePairs.add(pair)
  }
  if (status === 'completed_accepted' && installers.length === 0) {
    return invalid('已完成已验收状态至少需要一名安装人员')
  }
  return { valid: true, message: '' }
}

export function installerPreview(source) {
  const installers = cloneInstallers(source)
  return {
    visible: installers.slice(0, 3),
    remaining: Math.max(installers.length - 3, 0)
  }
}

function invalid(message) {
  return { valid: false, message }
}
