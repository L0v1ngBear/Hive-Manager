import request from '@/utils/request.js'

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

export function syncAiNotifications() {
  return request({
    url: '/notifications/sync-ai',
    method: 'post'
  })
}
