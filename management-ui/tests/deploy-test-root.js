import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const testsRoot = path.dirname(fileURLToPath(import.meta.url))
export const managementUiRoot = path.resolve(testsRoot, '..')
export const checkoutRoot = path.resolve(managementUiRoot, '..')
export const managementRoot = path.join(checkoutRoot, 'management')

export function resolveDeployRoot(env = process.env, homeDirectory = os.homedir()) {
  const configuredRoot = env.HIVE_DEPLOY_ROOT?.trim()
  return path.resolve(configuredRoot || path.join(homeDirectory, 'Desktop', 'hive部署_全新配置'))
}

export const deployRoot = resolveDeployRoot()
