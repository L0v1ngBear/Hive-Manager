import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const uiRoot = fileURLToPath(new URL('..', import.meta.url))
const repoRoot = path.resolve(uiRoot, '..')

const productionFiles = [
  'management/src/main/java/my/hive/domain/installation/model/dto/InstallationTaskStatusUpdateRequest.java',
  'management/src/main/java/my/hive/domain/installation/model/entity/InstallationTask.java',
  'management/src/main/java/my/hive/domain/installation/model/vo/InstallationTaskVO.java',
  'management/src/main/java/my/hive/domain/installation/service/InstallationTaskService.java',
  'management-ui/src/views/function/installationTask/installationTask.vue'
]

test('installation tasks use only the multi-installer contract after the schema migration', () => {
  for (const relativePath of [
    'management/src/main/java/my/hive/domain/installation/model/dto/InstallationTaskInstallerRequest.java',
    'management/src/main/java/my/hive/domain/installation/model/entity/InstallationTaskInstaller.java',
    'management/src/main/java/my/hive/domain/installation/model/vo/InstallationTaskInstallerVO.java',
    'management-ui/src/views/function/installationTask/installationTaskInstallers.js'
  ]) {
    assert.ok(fs.existsSync(path.join(repoRoot, relativePath)), `multi-installer source is missing: ${relativePath}`)
  }

  for (const relativePath of productionFiles) {
    const source = fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
    assert.doesNotMatch(source, /constructionPersonnel|constructionPhone/u, `retired installer field remains in ${relativePath}`)
  }

  const request = fs.readFileSync(path.join(repoRoot, productionFiles[0]), 'utf8')
  const view = fs.readFileSync(path.join(repoRoot, productionFiles[2]), 'utf8')
  assert.match(request, /List<InstallationTaskInstallerRequest>\s+installers/u)
  assert.match(view, /List<InstallationTaskInstallerVO>\s+installers/u)
})
