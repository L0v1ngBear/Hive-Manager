<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">sell</span>
            价格策略中心
          </div>
          <h1 class="function-page-title">价格管理</h1>
          <p class="function-page-desc">维护面料 SKU 基准价、客户等级价和指定客户特价，价格会被后端出库金额计算复用。</p>
        </div>
        <div class="flex items-center gap-3">
          <button @click="downloadTemplate" class="function-action-secondary">
            <span class="material-symbols-outlined text-lg align-middle mr-1">description</span>导入模板
          </button>
          <button @click="triggerImport" class="function-action-secondary">
            <span class="material-symbols-outlined text-lg align-middle mr-1">file_upload</span>导入价格
          </button>
          <button @click="exportExcel" class="function-action-secondary">
            <span class="material-symbols-outlined text-lg align-middle mr-1">file_download</span>导出 Excel
          </button>
          <button @click="openCreate()" class="function-action-primary">
            <span class="material-symbols-outlined text-lg align-middle mr-1">add_circle</span>新增价格
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">SKU 数量</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.skuCount }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">平均基准价</p>
          <h3 class="text-4xl font-black text-primary mt-2">¥{{ money(stats.averagePrice) }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">计划中价格</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.pendingCount }}</h3>
        </div>
        <div class="bg-[#1a365d] text-white p-6 rounded-xl shadow-md">
          <p class="text-xs font-bold uppercase tracking-widest opacity-80">客户特价</p>
          <h3 class="text-4xl font-black mt-2">{{ stats.overrideCount }}</h3>
          <p class="text-xs opacity-70 mt-3">指定客户优先使用特价。</p>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden ring-1 ring-outline-variant/20">
        <div class="px-6 py-4 border-b border-surface-variant/50 flex flex-wrap items-center justify-between gap-4">
          <div class="flex flex-wrap items-center gap-3">
            <div class="relative min-w-[260px]">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
              <input v-model.trim="query.keyword" @keyup.enter="fetchData" class="w-full pl-10 pr-4 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 focus:ring-2 focus:ring-primary text-sm outline-none" placeholder="搜索型号、批号、规格" />
            </div>
            <select v-model="query.status" @change="handleFilter" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部状态</option>
              <option :value="1">生效中</option>
              <option :value="2">计划中</option>
              <option :value="0">已过期</option>
            </select>
            <input v-model.trim="query.batchNo" @keyup.enter="handleFilter" class="w-32 px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" placeholder="批号" />
            <input v-model.trim="query.spec" @keyup.enter="handleFilter" class="w-32 px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" placeholder="规格" />
            <select v-model="query.currency" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部币种</option>
              <option value="CNY">CNY</option>
              <option value="USD">USD</option>
            </select>
            <input v-model.trim="query.priceMin" type="number" min="0" step="0.01" class="w-28 px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" placeholder="最低价" />
            <input v-model.trim="query.priceMax" type="number" min="0" step="0.01" class="w-28 px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" placeholder="最高价" />
            <input v-model="query.effectiveStart" type="date" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" title="生效开始日期" />
            <input v-model="query.effectiveEnd" type="date" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none" title="生效结束日期" />
            <button @click="handleFilter" class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold">查询</button>
            <button @click="resetFilter" class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold">重置</button>
            <TableColumnSettings
              :columns="priceTableColumns"
              @move="movePriceTableColumn"
              @reset="resetPriceTableColumns"
            />
          </div>
          <span class="text-xs text-on-surface-variant">共 {{ pagination.total }} 条价格记录</span>
        </div>

        <div class="overflow-x-auto relative min-h-[260px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
          </div>
          <table class="w-full text-left border-collapse min-w-[960px]">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th
                  v-for="column in priceTableColumns"
                  :key="column.key"
                  class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider"
                  :class="column.align === 'right' ? 'text-right' : ''"
                >
                  {{ column.label }}
                </th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/30">
              <tr v-for="item in rows" :key="item.id" class="cursor-pointer hover:bg-surface-container-high/40 transition-colors" @click="openDetail(item)">
                <td
                  v-for="column in priceTableColumns"
                  :key="column.key"
                  class="px-6 py-4"
                  :class="priceCellClass(column.key)"
                >
                  <template v-if="column.key === 'modelCode'">
                    <div>
                      <p class="text-sm font-bold text-primary">{{ item.modelCode }}</p>
                      <p class="text-[10px] text-on-surface-variant">{{ item.batchNo || '未填写批号' }}</p>
                    </div>
                  </template>
                  <template v-else-if="column.key === 'spec'">
                    <div class="text-xs text-on-surface-variant">{{ item.spec || '--' }}</div>
                  </template>
                  <template v-else-if="column.key === 'basePrice'">¥{{ money(item.basePrice) }} <span class="text-[10px] text-on-surface-variant">/m</span></template>
                  <template v-else-if="column.key === 'currency'">{{ item.currency }}</template>
                  <template v-else-if="column.key === 'effectiveDate'">{{ item.effectiveDate }}</template>
                  <template v-else-if="column.key === 'status'">
                    <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">{{ item.statusLabel }}</span>
                  </template>
                </td>
                <td class="px-6 py-4 text-right space-x-2">
                  <button @click.stop="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">详情</button>
                  <button @click.stop="openCreate(item)" class="text-secondary hover:bg-surface-container-high px-3 py-1.5 rounded-lg text-xs font-bold">调整</button>
                  <button @click.stop="remove(item)" class="text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-lg text-xs font-bold">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td :colspan="priceTableColumnCount" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无价格记录。</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex items-center justify-between text-sm text-on-surface-variant border-t border-surface-variant/50">
          <span>第 {{ query.page }} / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <button @click="changePage(query.page - 1)" :disabled="query.page <= 1" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">上一页</button>
            <button @click="changePage(query.page + 1)" :disabled="query.page >= totalPages" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <PriceCreateDrawer :is-visible="createVisible" :sku-data="editingSku" @close="closeCreate" @success="handleSaved" />

    <transition name="fade">
      <div v-if="detailVisible" @click="detailVisible = false" class="fixed inset-0 bg-black/20 backdrop-blur-[2px] z-40"></div>
    </transition>
    <aside class="fixed top-0 right-0 h-full w-full sm:w-[440px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300" :class="detailVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">价格详情</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ detail?.modelCode || '--' }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1 hover:bg-surface-container-high rounded-full"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-surface-container-low p-4 rounded-xl"><span class="text-[10px] text-on-surface-variant font-bold">当前基准价</span><p class="text-xl font-black text-primary">¥{{ money(detail?.basePrice) }}</p></div>
          <div class="bg-surface-container-low p-4 rounded-xl"><span class="text-[10px] text-on-surface-variant font-bold">生效日期</span><p class="text-xl font-black text-primary">{{ detail?.effectiveDate || '--' }}</p></div>
        </div>
        <section>
          <h4 class="text-xs font-bold text-primary uppercase tracking-widest mb-3">客户等级价格</h4>
          <div class="space-y-2">
            <div v-for="tier in detail?.tierPrices || []" :key="tier.tierCode" class="flex justify-between p-3 bg-surface-container-low rounded-lg text-sm">
              <span class="font-bold">{{ tier.tierName }}</span><span class="font-black text-primary">¥{{ money(tier.finalPrice) }}</span>
            </div>
          </div>
        </section>
        <section>
          <h4 class="text-xs font-bold text-primary uppercase tracking-widest mb-3">客户特价</h4>
          <div v-if="(detail?.overrides || []).length === 0" class="text-xs text-on-surface-variant">暂无客户特价。</div>
          <div v-for="item in detail?.overrides || []" :key="item.id" class="flex justify-between p-3 bg-tertiary-fixed/10 rounded-lg text-sm mb-2">
            <span class="font-bold">{{ item.customerName }}</span><span class="font-black text-primary">¥{{ money(item.price) }}</span>
          </div>
        </section>
        <section>
          <h4 class="text-xs font-bold text-primary uppercase tracking-widest mb-3">调整日志</h4>
          <div v-if="(detail?.logs || []).length === 0" class="text-xs text-on-surface-variant">暂无调整日志。</div>
          <div v-for="(log, index) in detail?.logs || []" :key="index" class="p-3 bg-white shadow-sm rounded-lg border-l-2 border-primary mb-2">
            <p class="text-xs font-bold text-primary">{{ log.remark }}：¥{{ money(log.oldPrice) }} -> ¥{{ money(log.newPrice) }}</p>
            <p class="text-[10px] text-on-surface-variant mt-1">{{ log.createTime || '--' }}</p>
          </div>
        </section>
      </div>
    </aside>
    <input ref="importInputRef" type="file" accept=".xlsx" class="hidden" @change="handleImportChange" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import PriceCreateDrawer from './priceCreate.vue'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import {
  deletePrice,
  downloadPriceImportTemplate,
  exportPriceExcel,
  getPriceDetail,
  getPricePage,
  getPriceStats,
  importPrices
} from './api/price.js'

const route = useRoute()
const defaultPriceTableColumns = [
  { key: 'modelCode', label: '面料型号' },
  { key: 'spec', label: '规格说明' },
  { key: 'basePrice', label: '基准价', align: 'right' },
  { key: 'currency', label: '币种' },
  { key: 'effectiveDate', label: '生效日期' },
  { key: 'status', label: '状态' }
]
const {
  orderedColumns: priceTableColumns,
  moveColumn: movePriceTableColumn,
  resetColumns: resetPriceTableColumns
} = useLocalTableColumns('price.list', defaultPriceTableColumns)
const priceTableColumnCount = computed(() => priceTableColumns.value.length + 1)
const loading = ref(false)
const rows = ref([])
const stats = reactive({ skuCount: 0, averagePrice: 0, pendingCount: 0, overrideCount: 0 })
const pagination = reactive({ total: 0, pages: 0 })
// 分类已下线，列表只保留仍然生效的查询条件。
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: '',
  batchNo: '',
  spec: '',
  currency: '',
  priceMin: '',
  priceMax: '',
  effectiveStart: '',
  effectiveEnd: ''
})
const createVisible = ref(false)
const editingSku = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const importInputRef = ref(null)
const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))

function priceCellClass(key) {
  if (key === 'basePrice') return 'text-right text-sm font-black text-primary'
  if (key === 'currency') return 'text-xs font-bold'
  if (key === 'effectiveDate') return 'text-xs text-on-surface-variant font-medium'
  return ''
}

async function fetchData() {
  loading.value = true
  try {
    // 页面列表直接复用后端分页结果，避免再拼接已经废弃的分类参数。
    const data = await getPricePage(normalizedQuery())
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  Object.assign(stats, await getPriceStats())
}

function handleFilter() {
  query.page = 1
  fetchData()
}

function resetFilter() {
  query.keyword = ''
  query.status = ''
  query.batchNo = ''
  query.spec = ''
  query.currency = ''
  query.priceMin = ''
  query.priceMax = ''
  query.effectiveStart = ''
  query.effectiveEnd = ''
  query.page = 1
  fetchData()
}

function normalizedQuery() {
  return {
    ...query,
    status: query.status === '' ? undefined : Number(query.status),
    batchNo: query.batchNo || undefined,
    spec: query.spec || undefined,
    currency: query.currency || undefined,
    priceMin: query.priceMin || undefined,
    priceMax: query.priceMax || undefined,
    effectiveStart: query.effectiveStart || undefined,
    effectiveEnd: query.effectiveEnd || undefined
  }
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  query.page = page
  fetchData()
}

function openCreate(item) {
  editingSku.value = item || null
  createVisible.value = true
}

function closeCreate() {
  createVisible.value = false
  editingSku.value = null
}

async function handleSaved() {
  closeCreate()
  await Promise.all([fetchData(), fetchStats()])
  ElMessage.success('价格已保存。')
}

async function openDetail(item) {
  detail.value = await getPriceDetail(item.id)
  detailVisible.value = true
}

async function remove(item) {
  await ElMessageBox.confirm(`确认删除 ${item.modelCode} 的价格记录吗？`, '删除确认', { type: 'warning' })
  await deletePrice(item.id)
  ElMessage.success('价格记录已删除。')
  await Promise.all([fetchData(), fetchStats()])
}

async function exportExcel() {
  const blob = await exportPriceExcel(normalizedQuery())
  downloadBlob(blob, `价格表-${Date.now()}.xlsx`)
}

async function downloadTemplate() {
  const blob = await downloadPriceImportTemplate()
  downloadBlob(blob, '价格导入模板.xlsx')
}

function triggerImport() {
  importInputRef.value?.click()
}

async function handleImportChange(event) {
  const [file] = event.target.files || []
  if (!file) return
  try {
    const result = await importPrices(file)
    const failText = (result.failMessages || []).slice(0, 5).join('\n')
    await ElMessageBox.alert(
      `总行数：${result.totalCount}\n成功：${result.successCount}\n失败：${result.failCount}${failText ? `\n\n失败明细：\n${failText}` : ''}`,
      '价格导入结果',
      { confirmButtonText: '关闭' }
    )
    await Promise.all([fetchData(), fetchStats()])
  } finally {
    event.target.value = ''
  }
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusClass(status) {
  if (Number(status) === 1) return 'bg-green-100 text-green-700'
  if (Number(status) === 2) return 'bg-amber-100 text-amber-700'
  return 'bg-slate-100 text-slate-500'
}

function applyRouteKeyword() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== query.keyword) {
    query.keyword = routeKeyword
    query.page = 1
  }
}

onMounted(async () => {
  applyRouteKeyword()
  await Promise.all([fetchData(), fetchStats()])
})

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await fetchData()
  }
)
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
</style>
