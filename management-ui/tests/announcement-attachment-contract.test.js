import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const publish = read('src/views/function/announcement/publish.vue')
const list = read('src/views/function/announcement/announcement.vue')
const api = read('src/api/notification.js')
const controller = read('../management/src/main/java/my/hive/api/notification/NotificationController.java')
const service = read('../management/src/main/java/my/hive/domain/notification/service/EnterpriseAnnouncementService.java')
const mapper = read('../management/src/main/java/my/hive/domain/notification/mapper/EnterpriseAnnouncementMapper.java')
const request = read('../management/src/main/java/my/hive/domain/notification/model/dto/AnnouncementPublishRequest.java')
const entity = read('../management/src/main/java/my/hive/domain/notification/model/entity/EnterpriseAnnouncement.java')
const vo = read('../management/src/main/java/my/hive/domain/notification/model/vo/NotificationVO.java')
const storage = read('../management/src/main/java/my/hive/infrastructure/storage/BusinessAttachmentService.java')
const chunked = read('../management/src/main/java/my/hive/api/storage/ChunkedVideoUploadController.java')
const migrationPath = '../db-migrations/migrations/V20260729_002_enterprise_announcement_attachment.sql'
const migration = read(migrationPath)
const manifest = read('../db-migrations/migration_manifest.txt')
const checksums = read('../db-migrations/migration_checksums.sha256')

test('announcement publisher uploads one optional attachment and sends its storage reference', () => {
  assert.match(publish, /import DragAttachmentUpload from/)
  assert.match(publish, /<DragAttachmentUpload[\s\S]*?@select="uploadAttachment"[\s\S]*?@download="openAttachment"[\s\S]*?@remove="removeAttachment"/)
  assert.match(publish, /attachmentName:\s*form\.attachmentUrl \? form\.attachmentName : undefined/)
  assert.match(publish, /attachmentUrl:\s*form\.attachmentUrl \|\| undefined/)
  assert.match(publish, /attachmentSize:\s*form\.attachmentUrl \? form\.attachmentSize : undefined/)
  assert.match(publish, /:disabled="attachmentUploading"/)
})

test('announcement list exposes authenticated attachment downloads', () => {
  assert.match(list, /v-if="item\.attachmentUrl"/)
  assert.match(list, /@click="openAttachment\(item\)"/)
  assert.match(api, /uploadAttachmentWithChunks\(file,[\s\S]*?'announcement'\)/)
  assert.match(api, /url:\s*'\/notifications\/announcements\/attachment\/upload'/)
  assert.match(api, /url:\s*'\/notifications\/announcements\/attachment\/download'/)
  assert.match(api, /responseType:\s*'blob'/)
})

test('announcement attachment fields persist and remain nullable for old announcements', () => {
  for (const source of [request, entity, vo]) {
    for (const field of ['attachmentName', 'attachmentUrl', 'attachmentSize']) {
      assert.match(source, new RegExp(`\\b${field}\\b`))
    }
  }
  for (const column of ['attachment_name', 'attachment_url', 'attachment_size']) {
    assert.match(mapper, new RegExp(`\\b${column}\\b`))
    assert.match(migration, new RegExp(`column_name = '${column}'`))
  }
  assert.equal((migration.match(/DEFAULT NULL/g) || []).length, 3)
  assert.match(service, /businessAttachmentService\.upload\(file,\s*"announcement"\)/)
  assert.match(service, /businessAttachmentService\.load\(attachmentUrl,\s*"announcement"\)/)
  assert.match(storage, /"announcement"/)
  assert.match(chunked, /"announcement",\s*PermissionCatalogV3\.CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH/)
})

test('announcement attachment endpoints keep publish and list permissions separate', () => {
  assert.match(controller, /@PostMapping\("\/announcements\/attachment\/upload"\)[\s\S]*?CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH/)
  assert.match(controller, /@GetMapping\("\/announcements\/attachment\/download"\)[\s\S]*?CODE_NOTIFICATION_ANNOUNCEMENT_LIST/)
})

test('announcement attachment migration is append-only and checksum registered', () => {
  assert.equal(manifest.trim().split(/\r?\n/).at(-1), `migrations/V20260729_002_enterprise_announcement_attachment.sql`)
  const digest = crypto.createHash('sha256').update(migration).digest('hex')
  assert.match(checksums, new RegExp(`^${digest}  migrations/V20260729_002_enterprise_announcement_attachment\\.sql$`, 'm'))
})
