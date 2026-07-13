import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const legacyTokens = ['deliveryDate', 'delivery_date']
const informationTokens = ['informationChannel', 'information_channel']
const cancelTokens = ['cancelReason', 'cancel_reason']

function tokensInBuffer(buffer) {
  const text = buffer.toString('latin1')
  return [...legacyTokens, ...informationTokens, ...cancelTokens]
    .filter((token) => text.includes(token))
}

function scanDirectory(directory) {
  const tokens = new Set()
  const stack = [directory]
  while (stack.length > 0) {
    const current = stack.pop()
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const absolutePath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(absolutePath)
        continue
      }
      for (const token of tokensInBuffer(fs.readFileSync(absolutePath))) tokens.add(token)
    }
  }
  return [...tokens]
}

function scanArchive(archivePath) {
  const listing = spawnSync('jar', ['tf', archivePath], { encoding: 'utf8' })
  if (listing.status !== 0) {
    return { tokens: [], error: `cannot inspect archive ${archivePath}: ${listing.stderr || listing.error}` }
  }

  const isBackendJar = archivePath.toLowerCase().endsWith('.jar')
  const entries = listing.stdout.split(/\r?\n/).filter((entry) => {
    if (!entry) return false
    if (isBackendJar) {
      return /^BOOT-INF\/classes\/.*\/(?:order|installation)\/.*\.(?:class|xml|sql)$/.test(entry)
    }
    return /\.(?:js|json|wxml|wxss)$/.test(entry)
  })
  if (entries.length === 0) return { tokens: [], error: `no order artifacts found in ${archivePath}` }

  const temporaryDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'hive-order-artifact-'))
  try {
    const extraction = spawnSync('jar', ['xf', archivePath, ...entries], {
      cwd: temporaryDirectory,
      encoding: 'utf8',
      maxBuffer: 8 * 1024 * 1024
    })
    if (extraction.status !== 0) {
      return { tokens: [], error: `cannot extract archive ${archivePath}: ${extraction.stderr}` }
    }
    return { tokens: scanDirectory(temporaryDirectory) }
  } finally {
    fs.rmSync(temporaryDirectory, { recursive: true, force: true })
  }
}

function scanArtifact(artifactPath) {
  if (!artifactPath || !fs.existsSync(artifactPath)) {
    return { tokens: [], error: `missing artifact: ${artifactPath || '(not declared)'}` }
  }
  if (fs.statSync(artifactPath).isDirectory()) return { tokens: scanDirectory(artifactPath) }
  return scanArchive(artifactPath)
}

function evaluateArtifacts(artifacts, releaseInfo) {
  const issues = []
  for (const artifact of artifacts) {
    const result = scanArtifact(artifact.path)
    if (result.error) issues.push(`${artifact.name}: ${result.error}`)
    const tokens = result.tokens || []
    const stale = legacyTokens.filter((token) => tokens.includes(token))
    if (stale.length > 0) issues.push(`${artifact.name}: legacy ${stale.join(', ')}`)
    if (!informationTokens.some((token) => tokens.includes(token))) {
      issues.push(`${artifact.name}: missing informationChannel contract`)
    }
    if (!cancelTokens.some((token) => tokens.includes(token))) {
      issues.push(`${artifact.name}: missing cancelReason contract`)
    }
  }
  if (!/^OrderInformationChannelContract=READY$/m.test(releaseInfo || '')) {
    issues.push('release metadata: OrderInformationChannelContract=READY is absent')
  }

  return {
    ok: issues.length === 0,
    issues,
    message: issues.length === 0
      ? 'Order information-channel artifact contract passed'
      : 'BLOCKED: integrate and rebuild all four order information-channel artifacts before enabling V20260713_001'
  }
}

export function inspectArtifactDirectories(directories, releaseInfo) {
  const names = ['mini backend', 'management backend', 'management UI', 'mini program']
  return evaluateArtifacts(directories.map((artifactPath, index) => ({
    name: names[index] || `artifact ${index + 1}`,
    path: artifactPath
  })), releaseInfo)
}

export function inspectDeploymentArtifacts(deployRoot) {
  const releaseInfoPath = path.join(deployRoot, 'RELEASE_BUILD_INFO.txt')
  const releaseInfo = fs.existsSync(releaseInfoPath)
    ? fs.readFileSync(releaseInfoPath, 'utf8')
    : ''
  const miniProgramName = releaseInfo.match(/^MiniProgramPackage=(.+)$/m)?.[1]?.trim()

  return evaluateArtifacts([
    {
      name: 'mini backend',
      path: path.join(deployRoot, 'backend/Hive_Back-0.0.1-SNAPSHOT.jar')
    },
    {
      name: 'management backend',
      path: path.join(deployRoot, 'management-backend/management-0.0.1-SNAPSHOT.jar')
    },
    {
      name: 'management UI',
      path: path.join(deployRoot, 'management-ui/dist')
    },
    {
      name: 'mini program',
      path: miniProgramName ? path.join(deployRoot, miniProgramName) : null
    }
  ], releaseInfo)
}
