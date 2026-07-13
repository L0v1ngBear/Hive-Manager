import os from 'node:os'
import path from 'node:path'

export function resolveDeployRoot(env = process.env, homeDirectory = os.homedir()) {
  const configuredRoot = env.HIVE_DEPLOY_ROOT?.trim()
  return path.resolve(configuredRoot || path.join(homeDirectory, 'Desktop', 'hive部署_全新配置'))
}

export const deployRoot = resolveDeployRoot()
