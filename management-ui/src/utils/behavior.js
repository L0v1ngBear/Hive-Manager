import request from '@/utils/request'

const SESSION_KEY = 'hive_behavior_session_id'
const MAX_QUEUE_SIZE = 50
const FLUSH_INTERVAL = 5000
const SENSITIVE_KEY_PATTERN = /(password|passwd|pwd|token|secret|authorization|auth|key|credential|openid|session|cookie|phone|mobile|idcard|identity)/i
const queue = []
let flushTimer = null

function ensureSessionId() {
  let sessionId = sessionStorage.getItem(SESSION_KEY)
  if (!sessionId) {
    sessionId = `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
    sessionStorage.setItem(SESSION_KEY, sessionId)
  }
  return sessionId
}

export function trackBehavior(event) {
  if (!event?.eventType) {
    return
  }
  queue.push({
    ...event,
    metadata: sanitizeMetadata(event.metadata),
    sessionId: ensureSessionId(),
    clientTime: new Date().toISOString().slice(0, 19)
  })
  if (queue.length >= 10) {
    flushBehavior()
    return
  }
  scheduleFlush()
}

function sanitizeMetadata(value, depth = 0) {
  if (value == null || depth > 4) {
    return value == null ? value : '[TRUNCATED]'
  }
  if (Array.isArray(value)) {
    return value.slice(0, 20).map((item) => sanitizeMetadata(item, depth + 1))
  }
  if (typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value)
        .slice(0, 50)
        .map(([key, item]) => [
          key,
          SENSITIVE_KEY_PATTERN.test(key) ? '[REDACTED]' : sanitizeMetadata(item, depth + 1)
        ])
    )
  }
  if (typeof value === 'string') {
    if (/bearer\s+[a-z0-9._-]+/i.test(value) || value.length > 300) {
      return '[REDACTED]'
    }
    return value
  }
  return value
}

export function trackPageView(route) {
  trackBehavior({
    eventType: 'page_view',
    pagePath: route.fullPath,
    module: resolveModule(route.path),
    targetType: 'route',
    targetId: route.name || route.path,
    action: 'view',
    source: 'router',
    metadata: {
      title: route.meta?.title || ''
    }
  })
}

export async function flushBehavior() {
  if (!queue.length) {
    return
  }
  const payload = queue.splice(0, MAX_QUEUE_SIZE)
  clearTimeout(flushTimer)
  flushTimer = null
  try {
    await request({
      url: '/behavior-events/batch',
      method: 'post',
      data: payload,
      silent: true
    })
  } catch {
    // 行为采集不能影响主业务；失败时丢弃本批次，避免本地队列无限增长。
  }
}

function scheduleFlush() {
  if (flushTimer) {
    return
  }
  flushTimer = setTimeout(flushBehavior, FLUSH_INTERVAL)
}

function resolveModule(path) {
  if (!path) {
    return 'unknown'
  }
  if (path.startsWith('/dashboard/ai-advices')) return 'ai_advice'
  if (path.startsWith('/dashboard')) return 'dashboard'
  if (path.startsWith('/function/inventory')) return 'inventory'
  if (path.startsWith('/function/order')) return 'order'
  if (path.startsWith('/function/customer')) return 'customer'
  if (path.startsWith('/function/bad-product')) return 'quality'
  if (path.startsWith('/function/approval')) return 'approval'
  if (path.startsWith('/function/attendance')) return 'attendance'
  if (path.startsWith('/platform/tenant')) return 'tenant'
  return path.split('/').filter(Boolean)[0] || 'unknown'
}
