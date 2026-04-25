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

export function markNotificationRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'post'
  })
}

export function syncAiNotifications() {
  return request({
    url: '/notifications/sync-ai',
    method: 'post'
  })
}
