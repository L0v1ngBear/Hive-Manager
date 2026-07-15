export function resolveInstallationAccess(hasPermission) {
  const canUpdate = Boolean(hasPermission?.('installation:update'))
  const canUpload = Boolean(hasPermission?.('installation:attachment:upload'))
  const canDownload = Boolean(hasPermission?.('installation:attachment:download'))
  return { canUpdate, canUpload, canDownload, canAttach: canUpdate && canUpload }
}
