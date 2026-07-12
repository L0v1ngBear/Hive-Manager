<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header"><div><div class="function-page-eyebrow"><span class="material-symbols-outlined">sell</span>价格策略中心</div><h1 class="function-page-title">价格管理</h1><p class="function-page-desc">维护面料 SKU 基准价、客户等级价和指定客户特价。</p></div><div class="flex items-center gap-3"><el-button @click="downloadTemplate">导入模板</el-button><el-upload action="#" accept=".xlsx" :auto-upload="false" :show-file-list="false" :on-change="handleImportUpload"><el-button>导入价格</el-button></el-upload><el-button @click="exportExcel">导出 Excel</el-button><el-button type="primary" @click="openCreate()">新增价格</el-button></div></header>
      <section class="grid grid-cols-1 gap-4 md:grid-cols-4"><el-statistic title="SKU 数量" :value="stats.skuCount" /><el-statistic title="平均基准价" :precision="2" :value="Number(stats.averagePrice || 0)" /><el-statistic title="计划中价格" :value="stats.pendingCount" /><el-statistic title="客户特价" :value="stats.overrideCount" /></section>
      <section class="bg-surface-container-lowest overflow-hidden rounded-lg shadow-sm">
        <el-form :model="query" inline class="p-4"><el-form-item><el-input v-model.trim="query.keyword" placeholder="搜索型号、批号、规格" @keyup.enter="fetchData" /></el-form-item><el-form-item><el-select v-model="query.status" placeholder="全部状态" clearable><el-option :value="1" label="生效中" /><el-option :value="2" label="计划中" /><el-option :value="0" label="已过期" /></el-select></el-form-item><el-form-item><el-input v-model.trim="query.batchNo" placeholder="批号" /></el-form-item><el-form-item><el-input v-model.trim="query.spec" placeholder="规格" /></el-form-item><el-form-item><el-select v-model="query.currency" placeholder="全部币种" clearable><el-option value="CNY" label="CNY" /><el-option value="USD" label="USD" /></el-select></el-form-item><el-form-item><el-input-number v-model="query.priceMin" :min="0" :precision="2" placeholder="最低价" controls-position="right" /></el-form-item><el-form-item><el-input-number v-model="query.priceMax" :min="0" :precision="2" placeholder="最高价" controls-position="right" /></el-form-item><el-form-item><el-date-picker v-model="query.effectiveStart" type="date" value-format="YYYY-MM-DD" placeholder="生效开始" /></el-form-item><el-form-item><el-date-picker v-model="query.effectiveEnd" type="date" value-format="YYYY-MM-DD" placeholder="生效结束" /></el-form-item><el-form-item><el-button type="primary" @click="handleFilter">查询</el-button><el-button @click="resetFilter">重置</el-button></el-form-item></el-form>
        <el-table v-loading="loading" :data="rows" row-key="id" @row-click="openDetail"><el-table-column prop="modelCode" label="面料型号" min-width="150" /><el-table-column prop="batchNo" label="批号" min-width="120" /><el-table-column prop="spec" label="规格说明" min-width="160" /><el-table-column label="基准价" align="right"><template #default="{ row }">¥{{ money(row.basePrice) }}</template></el-table-column><el-table-column prop="currency" label="币种" /><el-table-column prop="effectiveDate" label="生效日期" min-width="120" /><el-table-column label="状态"><template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : Number(row.status) === 2 ? 'warning' : 'info'">{{ row.statusLabel }}</el-tag></template></el-table-column><el-table-column label="操作" fixed="right" width="180"><template #default="{ row }"><el-button link type="primary" @click.stop="openDetail(row)">详情</el-button><el-button link type="primary" @click.stop="openCreate(row)">调整</el-button><el-button link type="danger" @click.stop="remove(row)">删除</el-button></template></el-table-column><template #empty><el-empty description="暂无价格记录" /></template></el-table>
        <div class="flex justify-end p-4"><el-pagination v-model:current-page="query.page" :page-size="query.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="changePage" /></div>
      </section>
    </div>
    <PriceCreateDrawer :is-visible="createVisible" :sku-data="editingSku" @close="closeCreate" @success="handleSaved" />
    <el-drawer :model-value="detailVisible" title="价格详情" size="440px" @update:model-value="(visible) => { detailVisible = visible }"><el-descriptions v-if="detail" :column="1" border><el-descriptions-item label="型号">{{ detail.modelCode }}</el-descriptions-item><el-descriptions-item label="基准价">¥{{ money(detail.basePrice) }}</el-descriptions-item><el-descriptions-item label="生效日期">{{ detail.effectiveDate }}</el-descriptions-item></el-descriptions><el-empty v-else description="暂无详情" /></el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElDatePicker, ElDescriptions, ElDescriptionsItem, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElPagination, ElSelect, ElStatistic, ElTable, ElTableColumn, ElTag, ElUpload } from 'element-plus'
import { useRoute } from 'vue-router'
import PriceCreateDrawer from './priceCreate.vue'
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
/* const defaultPriceTableColumns = [
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

*/
const loading = ref(false)
const rows = ref([])
const stats = reactive({ skuCount: 0, averagePrice: 0, pendingCount: 0, overrideCount: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ page: 1, size: 10, keyword: '', status: '', batchNo: '', spec: '', currency: '', priceMin: undefined, priceMax: undefined, effectiveStart: '', effectiveEnd: '' })
const createVisible = ref(false)
const editingSku = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))

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

async function handleImportUpload(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const result = await importPrices(file)
  const failText = (result.failMessages || []).slice(0, 5).join('\n')
  await ElMessageBox.alert(
    `总行数：${result.totalCount}\n成功：${result.successCount}\n失败：${result.failCount}${failText ? `\n\n失败明细：\n${failText}` : ''}`,
    '价格导入结果',
    { confirmButtonText: '关闭' }
  )
  await Promise.all([fetchData(), fetchStats()])
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
