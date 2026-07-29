import request from '@/utils/request.js'
import { uploadAttachmentWithChunks } from '@/utils/chunkedAttachmentUpload.js'

export function getUnreadNotifications() {
  return request({
    url: '/notifications/unread',
    method: 'get'
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

export function getAnnouncements(params = {}) {
  return request({
    url: '/notifications/announcements',
    method: 'get',
    params
  })
}

export function publishAnnouncement(data) {
  return request({
    url: '/notifications/announcements',
    method: 'post',
    data
  })
}

export function uploadAnnouncementAttachment(data) {
  const file = data?.get?.('file')
  return uploadAttachmentWithChunks(file, () => request({
    url: '/notifications/announcements/attachment/upload',
    method: 'post',
    data,
    timeout: 600000
  }), 'announcement')
}

export function downloadAnnouncementAttachment(params) {
  return request({
    url: '/notifications/announcements/attachment/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 600000
  })
}

export function markNotificationRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'post'
  })
}

export function closeNotificationTask(id, data) {
  return request({
    url: `/notifications/${id}/close`,
    method: 'post',
    data
  })
}

export function syncNotifications() {
  return request({
    url: '/notifications/sync',
    method: 'post'
  })
}
