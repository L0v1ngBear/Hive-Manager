import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { deployRoot } from './deploy-test-root.js'

const checkedRoots = [
  'README.md',
  'SMOKE_TEST.md',
  'RELEASE_NOTES.md',
  'docs',
  'scripts',
  'db-migrations/README.md',
  'db-migrations/线上完整迁移执行清单.md',
  'db-migrations/MySQL存储异常处理.md',
  'db-migrations/scripts'
]

function collectFiles(relativePath) {
  const absolutePath = path.join(deployRoot, relativePath)
  const stat = fs.statSync(absolutePath)
  if (stat.isFile()) {
    return [absolutePath]
  }
  return fs.readdirSync(absolutePath, { withFileTypes: true }).flatMap((entry) => {
    const childRelativePath = path.join(relativePath, entry.name)
    if (entry.isDirectory()) {
      return collectFiles(childRelativePath)
    }
    return [path.join(deployRoot, childRelativePath)]
  })
}

const files = checkedRoots.flatMap(collectFiles)
for (const file of files) {
  const content = fs.readFileSync(file, 'utf8')
  assert.ok(!content.includes('116.62.130.38'), `deploy package must not hard-code server IP: ${file}`)
  assert.ok(
    !content.includes('hive专用\\migration-system') && !content.includes('hive专用/migration-system'),
    `deploy package must not reference retired migration-system upload path: ${file}`
  )
}

const forbiddenDirectories = new Set([
  '.git',
  '.cache',
  '.vite',
  'node_modules',
  'target',
  'src',
  'test',
  'tests',
  'smoke-reports',
  'backups',
  'data'
])
const forbiddenFilePattern = /\.(zip|7z|rar|bak|tmp|log|map)$/i

function walkReleaseTree(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolutePath = path.join(directory, entry.name)
    const relativePath = path.relative(deployRoot, absolutePath)
    if (entry.isDirectory()) {
      assert.ok(
        !forbiddenDirectories.has(entry.name),
        `deploy package must not include transient directory: ${relativePath}`
      )
      walkReleaseTree(absolutePath)
      continue
    }
    assert.ok(
      !forbiddenFilePattern.test(entry.name) && !entry.name.endsWith('~') && !entry.name.endsWith('.old'),
      `deploy package must not include transient file: ${relativePath}`
    )
  }
}

walkReleaseTree(deployRoot)

console.log('deploy package cleanliness checks passed')
