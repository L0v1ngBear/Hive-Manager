<template>
  <div class="space-y-6">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight text-primary">订单管理中心</h1>
        <p class="mt-2 text-sm text-on-surface-variant">统一查看销售订单与生产订单，并支持详情和状态维护。</p>
      </div>
      <div class="flex gap-3">
        <div class="rounded-xl bg-surface-container-high px-4 py-3 text-sm font-bold text-primary">销售 {{ salesTotal }}</div>
        <div class="rounded-xl bg-surface-container-high px-4 py-3 text-sm font-bold text-primary">生产 {{ productionTotal }}</div>
        <div class="rounded-xl bg-surface-container-high px-4 py-3 text-sm font-bold text-primary">{{ healthText }}</div>
      </div>
    </header>

    <section class="overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
      <div class="flex border-b border-outline-variant/10">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="relative px-6 py-4 text-sm font-bold transition-colors"
          :class="currentTab === tab.id ? 'text-primary' : 'text-on-surface-variant hover:text-primary'"
          @click="switchTab(tab.id)"
        >
          {{ tab.label }}
          <span v-if="currentTab === tab.id" class="absolute inset-x-0 bottom-0 h-[3px] bg-primary"></span>
        </button>
      </div>

      <div class="grid grid-cols-1 gap-4 border-b border-outline-variant/10 bg-surface-container-low/30 px-6 py-5 md:grid-cols-[minmax(0,1fr)_220px_auto]">
        <input
          v-model.trim="filters.keyword"
          class="rounded-lg border border-outline-variant/20 bg-surface-container-low px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary/30 focus:outline-none"
          :placeholder="currentTab === 'sales' ? '搜索订单号、客户、电话或商品描述' : '搜索生产单号、销售单号、客户、项目或型号'"
          @keyup.enter="refreshCurrentTab"
        />
        <select
          v-model="filters.status"
          class="rounded-lg border border-outline-variant/20 bg-surface-container-low px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary/30 focus:outline-none"
          @change="refreshCurrentTab"
        >
          <option value="">全部状态</option>
          <option v-for="status in currentStatuses" :key="status.value" :value="status.value">{{ status.label }}</option>
        </select>
        <button class="rounded-lg bg-primary px-5 py-2.5 text-sm font-bold text-on-primary" @click="refreshCurrentTab">查询</button>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1080px] w-full border-collapse text-left">
          <thead class="bg-surface-container-low/50">
            <tr>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">{{ currentTab === 'sales' ? '订单号' : '生产单号' }}</th>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">{{ currentTab === 'sales' ? '客户' : '关联销售单' }}</th>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">{{ currentTab === 'sales' ? '描述' : '客户 / 项目' }}</th>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">{{ currentTab === 'sales' ? '数量 / 金额' : '型号 / 工序' }}</th>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">状态</th>
              <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">时间</th>
              <th class="px-6 py-4 text-right text-xs font-black tracking-wider text-on-surface-variant uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr
              v-for="row in currentRows"
              :key="row.orderId"
              class="group transition-colors hover:bg-surface-container-high/30"
            >
              <td class="px-6 py-4">
                <div class="font-bold text-primary">{{ row.orderId }}</div>
                <div v-if="currentTab === 'sales'" class="mt-1 text-xs text-on-surface-variant">明细 {{ row.detailCount || 0 }} 项</div>
                <div v-else class="mt-1 text-xs text-on-surface-variant">数量 {{ row.quantity || 0 }}</div>
              </td>
              <td class="px-6 py-4">
                <template v-if="currentTab === 'sales'">
                  <div class="font-bold text-primary">{{ row.customerName || '未填写客户' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ row.customerPhone || '未填写电话' }}</div>
                </template>
                <template v-else>
                  <div class="text-sm text-on-surface-variant">{{ row.salesOrderId || '未关联' }}</div>
                </template>
              </td>
              <td class="px-6 py-4">
                <template v-if="currentTab === 'sales'">
                  <div class="max-w-[240px] truncate text-sm" :title="row.goodsDesc">{{ row.goodsDesc || '未填写商品描述' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ row.expressCompany || '未发货' }}{{ row.expressNo ? ' / ' + row.expressNo : '' }}</div>
                </template>
                <template v-else>
                  <div class="font-bold text-primary">{{ row.customerName || '未填写客户' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ row.projectName || '未填写项目' }}</div>
                </template>
              </td>
              <td class="px-6 py-4">
                <template v-if="currentTab === 'sales'">
                  <div class="font-bold text-primary">{{ row.totalQuantity || 0 }} 件</div>
                  <div class="mt-1 text-xs text-on-surface-variant">￥{{ formatAmount(row.totalAmount) }}</div>
                </template>
                <template v-else>
                  <div class="font-bold text-primary">{{ row.modelCode || '未填写型号' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ processLabel(row.process) }} / {{ row.fabric || '未填写面料' }}</div>
                </template>
              </td>
              <td class="px-6 py-4">
                <span class="inline-flex rounded-full px-3 py-1 text-xs font-bold" :class="currentTab === 'sales' ? salesStatusClass(row.status) : productionStatusClass(row.status)">
                  {{ currentTab === 'sales' ? salesStatusLabel(row.status) : productionStatusLabel(row.status) }}
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-on-surface-variant">{{ formatDateTime(currentTab === 'sales' ? row.createTime : row.deliveryDate || row.createTime) }}</td>
              <td class="px-6 py-4 text-right">
                <div class="flex justify-end gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                  <button class="rounded-md p-1.5 text-secondary hover:bg-white" @click="openDetail(row.orderId)">
                    <span class="material-symbols-outlined text-[18px]">visibility</span>
                  </button>
                  <button class="rounded-md p-1.5 text-primary hover:bg-white" @click="openEdit(row)">
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!currentLoading && currentRows.length === 0">
              <td colspan="7" class="px-6 py-14 text-center text-sm text-on-surface-variant">暂无订单数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between border-t border-outline-variant/10 bg-surface-container-low/30 px-6 py-4 text-sm">
        <span class="text-on-surface-variant">共 {{ currentTotal }} 条</span>
        <div class="flex items-center gap-2">
          <button class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50" :disabled="currentPage <= 1 || currentLoading" @click="changePage(currentPage - 1)">上一页</button>
          <span>{{ currentPage }} / {{ currentPages }}</span>
          <button class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50" :disabled="currentPage >= currentPages || currentLoading" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </section>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="860px" destroy-on-close>
      <div v-if="detailLoading" class="py-12 text-center text-on-surface-variant">加载中...</div>
      <div v-else-if="currentTab === 'sales' && salesDetail" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-xl bg-surface-container-low p-4"><div class="text-xs text-on-surface-variant">订单号</div><div class="mt-2 font-bold text-primary">{{ salesDetail.orderId }}</div></div>
          <div class="rounded-xl bg-surface-container-low p-4"><div class="text-xs text-on-surface-variant">状态</div><div class="mt-2 font-bold text-primary">{{ salesStatusLabel(salesDetail.status) }}</div></div>
        </div>
        <div class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4 text-sm leading-6 text-on-surface-variant">{{ salesDetail.goodsDesc || '暂无商品描述' }}</div>
        <div class="space-y-3">
          <div v-for="item in salesDetail.items || []" :key="item.id" class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4">
            <div class="font-bold text-primary">{{ item.modelCode || '未填写型号' }}</div>
            <div class="mt-1 text-sm text-on-surface-variant">规格：{{ item.spec || '未填写' }} / 数量：{{ item.quantity || 0 }}</div>
          </div>
          <div v-if="!(salesDetail.items || []).length" class="text-sm text-on-surface-variant">暂无明细项</div>
        </div>
      </div>
      <div v-else-if="productionDetail" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-xl bg-surface-container-low p-4"><div class="text-xs text-on-surface-variant">生产单号</div><div class="mt-2 font-bold text-primary">{{ productionDetail.orderId }}</div></div>
          <div class="rounded-xl bg-surface-container-low p-4"><div class="text-xs text-on-surface-variant">状态</div><div class="mt-2 font-bold text-primary">{{ productionStatusLabel(productionDetail.status) }}</div></div>
        </div>
        <div class="space-y-3">
          <div v-for="log in productionDetail.logs || []" :key="log.id" class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4">
            <div class="font-bold text-primary">{{ log.oldStatus || '未设置' }} → {{ log.newStatus || '未设置' }}</div>
            <div class="mt-1 text-sm text-on-surface-variant">{{ log.remark || '无备注' }}</div>
            <div class="mt-2 text-xs text-on-surface-variant">{{ log.operator || '系统' }} / {{ formatDateTime(log.createTime) }}</div>
          </div>
          <div v-if="!(productionDetail.logs || []).length" class="text-sm text-on-surface-variant">暂无状态日志</div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="editTitle" width="560px" destroy-on-close>
      <div v-if="editType === 'sales'" class="space-y-4">
        <el-select v-model="salesForm.status" class="w-full" placeholder="选择订单状态">
          <el-option v-for="status in salesStatuses" :key="status.value" :label="status.label" :value="status.value" />
        </el-select>
        <el-input v-model="salesForm.deliveryDate" placeholder="预计发货日期"></el-input>
        <el-input v-model="salesForm.expressCompany" placeholder="物流公司"></el-input>
        <el-input v-model="salesForm.expressNo" placeholder="物流单号"></el-input>
        <el-input v-model="salesForm.remark" type="textarea" :rows="3" placeholder="备注"></el-input>
      </div>
      <div v-else class="space-y-4">
        <el-select v-model="productionForm.status" class="w-full" placeholder="选择订单状态">
          <el-option v-for="status in productionStatuses" :key="status.value" :label="status.label" :value="status.value" />
        </el-select>
        <el-select v-model="productionForm.process" class="w-full" clearable placeholder="选择当前工序">
          <el-option v-for="process in processOptions" :key="process.value" :label="process.label" :value="process.value" />
        </el-select>
        <el-input v-model="productionForm.remark" type="textarea" :rows="3" placeholder="备注"></el-input>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkOrderModuleHealth, getProductionOrderDetail, getProductionOrderPage, getSalesOrderDetail, getSalesOrderPage, updateProductionOrder, updateSalesOrder } from './api/order'

const tabs = [{ id: 'sales', label: '销售订单' }, { id: 'production', label: '生产订单' }]
const currentTab = ref('sales')
const filters = reactive({ keyword: '', status: '' })
const salesStatuses = [{ value: 'PENDING', label: '待处理' }, { value: 'CONFIRMED', label: '已确认' }, { value: 'SHIPPED', label: '已发货' }, { value: 'COMPLETED', label: '已完成' }, { value: 'CANCELLED', label: '已取消' }]
const productionStatuses = [{ value: 'WAITING', label: '待排产' }, { value: 'IN_PRODUCTION', label: '生产中' }, { value: 'COMPLETED', label: '已完成' }, { value: 'SHIPPED', label: '已发货' }]
const processOptions = [{ value: 0, label: '整经' }, { value: 1, label: '浆纱' }, { value: 2, label: '织造' }, { value: 3, label: '验布' }, { value: 4, label: '卷布' }]

const salesState = reactive({ list: [], total: 0, pages: 1, pageNum: 1, loading: false })
const productionState = reactive({ list: [], total: 0, pages: 1, pageNum: 1, loading: false })
const detailVisible = ref(false)
const detailLoading = ref(false)
const editVisible = ref(false)
const editType = ref('sales')
const editingOrderId = ref('')
const saving = ref(false)
const salesDetail = ref(null)
const productionDetail = ref(null)
const healthInfo = ref(null)
const salesForm = reactive({ status: '', deliveryDate: '', expressCompany: '', expressNo: '', remark: '' })
const productionForm = reactive({ status: '', process: null, remark: '' })

const salesTotal = computed(() => salesState.total)
const productionTotal = computed(() => productionState.total)
const healthText = computed(() => {
  if (!healthInfo.value) return '检查中'
  return Object.entries(healthInfo.value).filter(([key]) => key.endsWith('Ready')).every(([, value]) => Boolean(value)) ? '订单表完整' : '存在异常'
})
const currentStatuses = computed(() => currentTab.value === 'sales' ? salesStatuses : productionStatuses)
const currentRows = computed(() => currentTab.value === 'sales' ? salesState.list : productionState.list)
const currentLoading = computed(() => currentTab.value === 'sales' ? salesState.loading : productionState.loading)
const currentTotal = computed(() => currentTab.value === 'sales' ? salesState.total : productionState.total)
const currentPage = computed(() => currentTab.value === 'sales' ? salesState.pageNum : productionState.pageNum)
const currentPages = computed(() => Math.max(1, currentTab.value === 'sales' ? salesState.pages : productionState.pages))
const currentTabLabel = computed(() => tabs.find(item => item.id === currentTab.value)?.label || '')
const detailTitle = computed(() => currentTab.value === 'sales' ? '销售订单详情' : '生产订单详情')
const editTitle = computed(() => editType.value === 'sales' ? '编辑销售订单' : '编辑生产订单')

onMounted(async () => {
  await Promise.all([loadHealth(), fetchSalesOrders(), fetchProductionOrders()])
})

async function loadHealth() {
  try { healthInfo.value = await checkOrderModuleHealth() } catch { healthInfo.value = null }
}

async function fetchSalesOrders() {
  salesState.loading = true
  try {
    const page = await getSalesOrderPage({ pageNum: salesState.pageNum, pageSize: 10, keyword: filters.keyword || undefined, status: currentTab.value === 'sales' ? filters.status || undefined : undefined })
    salesState.list = page.data || []
    salesState.total = page.total || 0
    salesState.pages = page.pages || 1
  } finally { salesState.loading = false }
}

async function fetchProductionOrders() {
  productionState.loading = true
  try {
    const page = await getProductionOrderPage({ pageNum: productionState.pageNum, pageSize: 10, keyword: filters.keyword || undefined, status: currentTab.value === 'production' ? filters.status || undefined : undefined })
    productionState.list = page.data || []
    productionState.total = page.total || 0
    productionState.pages = page.pages || 1
  } finally { productionState.loading = false }
}

function switchTab(tabId) { currentTab.value = tabId; filters.status = '' }
function refreshCurrentTab() { if (currentTab.value === 'sales') { salesState.pageNum = 1; return fetchSalesOrders() } productionState.pageNum = 1; return fetchProductionOrders() }
function changePage(page) { if (currentTab.value === 'sales') { salesState.pageNum = page; return fetchSalesOrders() } productionState.pageNum = page; return fetchProductionOrders() }

async function openDetail(orderId) {
  detailVisible.value = true
  detailLoading.value = true
  salesDetail.value = null
  productionDetail.value = null
  try {
    if (currentTab.value === 'sales') salesDetail.value = await getSalesOrderDetail(orderId)
    else productionDetail.value = await getProductionOrderDetail(orderId)
  } finally { detailLoading.value = false }
}

function openEdit(row) {
  editingOrderId.value = row.orderId
  if (currentTab.value === 'sales') {
    editType.value = 'sales'
    Object.assign(salesForm, { status: row.status || '', deliveryDate: row.deliveryDate || '', expressCompany: row.expressCompany || '', expressNo: row.expressNo || '', remark: row.remark || '' })
  } else {
    editType.value = 'production'
    Object.assign(productionForm, { status: row.status || '', process: row.process ?? null, remark: '' })
  }
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    if (editType.value === 'sales') {
      await updateSalesOrder(editingOrderId.value, { ...salesForm })
      await fetchSalesOrders()
      if (salesDetail.value?.orderId === editingOrderId.value) salesDetail.value = await getSalesOrderDetail(editingOrderId.value)
    } else {
      await updateProductionOrder(editingOrderId.value, { ...productionForm })
      await fetchProductionOrders()
      if (productionDetail.value?.orderId === editingOrderId.value) productionDetail.value = await getProductionOrderDetail(editingOrderId.value)
    }
    editVisible.value = false
    ElMessage.success('订单更新成功')
  } finally { saving.value = false }
}

function closeDetail() { detailVisible.value = false }
function salesStatusLabel(status) { return salesStatuses.find(item => item.value === status)?.label || status || '未设置' }
function productionStatusLabel(status) { return productionStatuses.find(item => item.value === status)?.label || status || '未设置' }
function processLabel(process) { return processOptions.find(item => item.value === process)?.label || '未设置' }
function formatAmount(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function formatDateTime(value) { if (!value) return '未设置'; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false }) }
function salesStatusClass(status) { return ({ PENDING: 'bg-orange-50 text-orange-700', CONFIRMED: 'bg-blue-50 text-blue-700', SHIPPED: 'bg-indigo-50 text-indigo-700', COMPLETED: 'bg-emerald-50 text-emerald-700', CANCELLED: 'bg-slate-100 text-slate-600' })[status] || 'bg-slate-100 text-slate-600' }
function productionStatusClass(status) { return ({ WAITING: 'bg-amber-50 text-amber-700', IN_PRODUCTION: 'bg-blue-50 text-blue-700', COMPLETED: 'bg-emerald-50 text-emerald-700', SHIPPED: 'bg-indigo-50 text-indigo-700' })[status] || 'bg-slate-100 text-slate-600' }
</script>
