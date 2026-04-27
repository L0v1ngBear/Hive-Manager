<template>
  <div class="h-full min-h-0 flex flex-col gap-4 overflow-hidden bg-surface text-on-surface">
    <section class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15">
      <div class="flex items-center justify-between gap-4">
        <div>
          <p class="text-[11px] font-black tracking-[0.28em] uppercase text-primary/70">平台运维</p>
          <h1 class="mt-1 text-3xl font-black tracking-tight text-primary">运维日志</h1>
          <p class="mt-1 text-sm text-on-surface-variant">仅 super 可见，用于排查接口异常、慢请求和关键业务操作。</p>
        </div>
        <button
          class="rounded-2xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition-all hover:opacity-90 active:scale-95"
          @click="loadLogs"
        >
          <span class="material-symbols-outlined mr-1.5 text-[18px] align-[-3px]">refresh</span>
          刷新
        </button>
      </div>
    </section>

    <section class="grid grid-cols-3 gap-4">
      <article v-for="item in levelCards" :key="item.level" class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs font-black tracking-[0.2em] text-on-surface-variant">{{ item.title }}</p>
            <p class="mt-3 text-3xl font-black" :class="item.textClass">{{ item.count }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl" :class="item.iconClass">
            <span class="material-symbols-outlined text-[24px]">{{ item.icon }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="flex-1 min-h-0 rounded-3xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/15 overflow-hidden flex flex-col">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant/10 px-5 py-3">
        <div class="flex flex-wrap items-center gap-3">
          <input v-model.trim="query.keyword" class="w-72 rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" placeholder="搜索 trace、租户、业务号、错误信息" @keyup.enter="handleSearch" />
          <select v-model="query.logLevel" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" @change="handleSearch">
            <option value="">全部级别</option>
            <option value="INFO">INFO 信息</option>
            <option value="WARN">WARN 警告</option>
            <option value="ERROR">ERROR 错误</option>
          </select>
          <select v-model="query.success" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" @change="handleSearch">
            <option value="">全部结果</option>
            <option value="1">成功</option>
            <option value="0">失败</option>
          </select>
          <input v-model="query.startTime" type="datetime-local" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" @change="handleSearch" />
          <input v-model="query.endTime" type="datetime-local" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none" @change="handleSearch" />
        </div>
        <p class="text-xs font-bold text-on-surface-variant">共 {{ page.total }} 条</p>
      </div>

      <div class="flex-1 min-h-0 overflow-auto">
        <table class="w-full min-w-[1280px] border-collapse text-left">
          <thead class="sticky top-0 z-10 bg-surface-container-low">
            <tr>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">级别</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">时间</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">租户/用户</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">模块动作</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">业务编号</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">耗时</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">接口</th>
              <th class="px-4 py-3 text-xs font-black text-on-surface-variant">说明/错误</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="row in logs" :key="row.id" class="cursor-pointer hover:bg-surface-container-low/55" @click="openDetail(row)">
              <td class="px-4 py-3">
                <span class="rounded-xl px-2.5 py-1 text-xs font-black" :class="levelClass(row.logLevel)">{{ row.logLevel }}</span>
              </td>
              <td class="px-4 py-3 text-xs font-bold text-on-surface-variant">{{ formatTime(row.createTime) }}</td>
              <td class="px-4 py-3 text-sm">
                <p class="font-black text-primary">{{ row.tenantCode || '-' }}</p>
                <p class="text-xs text-on-surface-variant">UID: {{ row.userId || '-' }}</p>
              </td>
              <td class="px-4 py-3 text-sm">
                <p class="font-black">{{ row.module }}/{{ row.action }}</p>
                <p class="text-xs text-on-surface-variant">{{ row.bizType || '-' }}</p>
              </td>
              <td class="px-4 py-3 text-xs font-bold text-on-surface-variant">{{ row.bizNo || '-' }}</td>
              <td class="px-4 py-3 text-sm font-black" :class="row.slow ? 'text-amber-700' : 'text-primary'">{{ row.durationMs }}ms</td>
              <td class="px-4 py-3 text-xs">
                <p class="font-black">{{ row.requestMethod || '-' }}</p>
                <p class="max-w-[220px] truncate text-on-surface-variant">{{ row.requestUri || '-' }}</p>
              </td>
              <td class="px-4 py-3 text-xs">
                <p class="max-w-[280px] truncate font-bold">{{ row.description || '-' }}</p>
                <p v-if="row.errorMessage" class="mt-1 max-w-[280px] truncate text-rose-700">{{ row.errorMessage }}</p>
              </td>
            </tr>
            <tr v-if="!loading && logs.length === 0">
              <td colspan="8" class="px-5 py-12 text-center text-sm text-on-surface-variant">暂无日志</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between border-t border-outline-variant/10 px-5 py-3">
        <button class="rounded-xl px-4 py-2 text-sm font-black text-primary hover:bg-primary/10 disabled:opacity-40" :disabled="page.current <= 1" @click="changePage(page.current - 1)">上一页</button>
        <span class="text-xs font-bold text-on-surface-variant">第 {{ page.current }} / {{ page.pages || 1 }} 页</span>
        <button class="rounded-xl px-4 py-2 text-sm font-black text-primary hover:bg-primary/10 disabled:opacity-40" :disabled="page.current >= page.pages" @click="changePage(page.current + 1)">下一页</button>
      </div>
    </section>

    <transition name="fade">
      <div v-if="selectedLog" class="fixed inset-0 z-50 flex justify-end bg-black/20 backdrop-blur-sm" @click.self="closeDetail">
        <aside class="h-full w-[560px] max-w-full overflow-y-auto bg-surface-container-lowest p-6 shadow-2xl">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-[11px] font-black tracking-[0.25em] text-primary/70">LOG DETAIL</p>
              <h2 class="mt-1 text-2xl font-black text-primary">日志详情</h2>
              <p class="mt-1 text-xs font-bold text-on-surface-variant">{{ selectedLog.traceId || '无 traceId' }}</p>
            </div>
            <button class="rounded-2xl bg-surface-container-low px-3 py-2 text-sm font-black text-on-surface-variant hover:text-primary" @click="closeDetail">
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div class="mt-6 grid grid-cols-2 gap-3">
            <div v-for="item in detailFields" :key="item.label" class="rounded-2xl bg-surface-container-low p-3">
              <p class="text-[11px] font-black text-on-surface-variant">{{ item.label }}</p>
              <p class="mt-1 break-all text-sm font-black text-on-surface">{{ item.value || '-' }}</p>
            </div>
          </div>

          <section v-if="selectedLog.errorMessage" class="mt-4 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-rose-800">
            <p class="text-sm font-black">{{ selectedLog.errorType || '错误信息' }}</p>
            <p class="mt-2 whitespace-pre-wrap text-sm font-bold">{{ selectedLog.errorMessage }}</p>
          </section>

          <section class="mt-4 space-y-4">
            <div class="rounded-2xl bg-surface-container-low p-4">
              <div class="mb-2 flex items-center justify-between">
                <p class="text-sm font-black text-primary">请求参数</p>
                <button class="text-xs font-black text-primary" @click="copyText(selectedLog.argsJson)">复制</button>
              </div>
              <pre class="max-h-72 overflow-auto whitespace-pre-wrap break-words text-xs leading-5 text-on-surface-variant">{{ formatJson(selectedLog.argsJson) }}</pre>
            </div>
            <div class="rounded-2xl bg-surface-container-low p-4">
              <div class="mb-2 flex items-center justify-between">
                <p class="text-sm font-black text-primary">返回结果</p>
                <button class="text-xs font-black text-primary" @click="copyText(selectedLog.resultJson)">复制</button>
              </div>
              <pre class="max-h-72 overflow-auto whitespace-pre-wrap break-words text-xs leading-5 text-on-surface-variant">{{ formatJson(selectedLog.resultJson) }}</pre>
            </div>
          </section>
        </aside>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchOperationLogPage } from '@/api/operationLog'

const loading = ref(false)
const logs = ref([])
const query = reactive({
  keyword: '',
  logLevel: '',
  success: '',
  startTime: '',
  endTime: '',
})
const page = reactive({
  current: 1,
  size: 20,
  total: 0,
  pages: 0,
})

const levelCards = computed(() => {
  const counts = logs.value.reduce((acc, item) => {
    acc[item.logLevel] = (acc[item.logLevel] || 0) + 1
    return acc
  }, {})
  return [
    { level: 'INFO', title: '信息日志', count: counts.INFO || 0, icon: 'info', textClass: 'text-primary', iconClass: 'bg-primary/10 text-primary' },
    { level: 'WARN', title: '警告日志', count: counts.WARN || 0, icon: 'warning', textClass: 'text-amber-700', iconClass: 'bg-amber-100 text-amber-700' },
    { level: 'ERROR', title: '错误日志', count: counts.ERROR || 0, icon: 'error', textClass: 'text-rose-700', iconClass: 'bg-rose-100 text-rose-700' },
  ]
})

async function loadLogs() {
  loading.value = true
  try {
    const result = await fetchOperationLogPage({
      current: page.current,
      size: page.size,
      keyword: query.keyword || undefined,
      logLevel: query.logLevel || undefined,
      success: query.success === '' ? undefined : Number(query.success),
      startTime: normalizeDateTime(query.startTime),
      endTime: normalizeDateTime(query.endTime),
    })
    logs.value = result?.data || []
    page.total = result?.total || 0
    page.pages = result?.pages || 0
  } finally {
    loading.value = false
  }
}

const selectedLog = ref(null)

const detailFields = computed(() => {
  const row = selectedLog.value || {}
  return [
    { label: '级别', value: row.logLevel },
    { label: '结果', value: row.success === 1 ? '成功' : '失败' },
    { label: '租户', value: row.tenantCode },
    { label: '用户ID', value: row.userId },
    { label: '模块动作', value: `${row.module || '-'}/${row.action || '-'}` },
    { label: '业务编号', value: row.bizNo },
    { label: '耗时', value: `${row.durationMs || 0}ms` },
    { label: '客户端IP', value: row.clientIp },
    { label: '接口', value: `${row.requestMethod || '-'} ${row.requestUri || '-'}` },
    { label: '时间', value: formatTime(row.createTime) },
  ]
})

function handleSearch() {
  page.current = 1
  loadLogs()
}

function changePage(nextPage) {
  page.current = nextPage
  loadLogs()
}

function levelClass(level) {
  if (level === 'ERROR') return 'bg-rose-100 text-rose-700'
  if (level === 'WARN') return 'bg-amber-100 text-amber-700'
  return 'bg-primary/10 text-primary'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function normalizeDateTime(value) {
  if (!value) return undefined
  return value.length === 16 ? `${value}:00` : value
}

function openDetail(row) {
  selectedLog.value = row
}

function closeDetail() {
  selectedLog.value = null
}

function formatJson(value) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

async function copyText(value) {
  if (!value || !navigator?.clipboard) return
  await navigator.clipboard.writeText(formatJson(value))
}

onMounted(loadLogs)
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
