<template>
  <div class="max-w-7xl mx-auto h-[calc(100vh-8rem)] flex flex-col lg:flex-row gap-6 pb-6">
    <section class="w-full lg:w-[400px] flex flex-col bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 overflow-hidden">
      <header class="p-5 border-b border-outline-variant/20 bg-surface flex justify-between items-center z-10">
        <div>
          <h2 class="text-lg font-black text-on-surface flex items-center gap-2">
            待打印任务队列
            <span class="bg-error/10 text-error text-[10px] px-2 py-0.5 rounded-full font-bold">{{ pendingOrders.length }}</span>
          </h2>
          <p class="text-xs text-on-surface-variant mt-1">小程序出库后会进入这里，电脑端可直接预览并通过浏览器打印到三联打印机。</p>
        </div>
        <el-button circle @click="fetchPendingList" :loading="isFetchingList">
          <span class="material-symbols-outlined text-lg">refresh</span>
        </el-button>
      </header>

      <div class="flex-1 overflow-y-auto p-3 space-y-2 bg-surface-container-lowest">
        <div v-if="pendingOrders.length === 0 && !isFetchingList" class="h-full flex flex-col items-center justify-center text-on-surface-variant/50">
          <span class="material-symbols-outlined text-4xl mb-2">task_alt</span>
          <p class="text-sm font-medium">当前暂无待打印出库单</p>
        </div>

        <div
          v-for="item in pendingOrders"
          :key="item.orderNo"
          @click="selectOrder(item)"
          :class="[
            'p-4 rounded-xl cursor-pointer transition-all border relative overflow-hidden',
            selectedOrder?.orderNo === item.orderNo ? 'bg-primary/5 border-primary/30 ring-1 ring-primary/30' : 'bg-surface hover:bg-surface-container border-outline-variant/20'
          ]"
        >
          <div v-if="selectedOrder?.orderNo === item.orderNo" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
          <div class="flex justify-between items-start mb-2 gap-3">
            <span class="text-sm font-black text-on-surface font-mono">{{ item.orderNo }}</span>
            <span class="text-xs text-on-surface-variant bg-surface-container px-2 py-0.5 rounded">{{ formatDate(item.createTime) }}</span>
          </div>
          <div class="text-xs text-on-surface-variant font-medium truncate mb-1">客户：<span class="text-on-surface">{{ item.customerName || '--' }}</span></div>
          <div class="text-xs text-on-surface-variant">包含 <span class="font-bold text-primary">{{ item.itemCount }}</span> 条明细 | 合计 {{ formatNumber(item.totalMeters) }} 米 | 经办：{{ item.operator || '--' }}</div>
        </div>
      </div>
    </section>

    <aside class="flex-1 bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 flex flex-col overflow-hidden relative">
      <div v-if="isLoadingDetail" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-20 flex flex-col items-center justify-center">
        <span class="material-symbols-outlined text-primary text-3xl animate-spin mb-2">progress_activity</span>
        <span class="text-sm text-primary font-bold">正在生成出库单预览...</span>
      </div>

      <header class="p-4 border-b border-outline-variant/20 flex justify-between items-center gap-3 bg-surface z-10 flex-wrap">
        <div>
          <h3 class="text-sm font-bold text-on-surface">出库单打印预览</h3>
        </div>
        <div class="flex gap-2 flex-wrap justify-end receipt-actions">
          <el-button v-if="selectedOrder" class="receipt-action-button" @click="openBrowserPrint" :loading="isPrinting">
            <span class="material-symbols-outlined text-[18px] mr-1">print</span>浏览器打印
          </el-button>
          <el-button v-if="selectedOrder" class="receipt-action-button receipt-action-button--danger" @click="handleCancelPrint" :loading="isSubmitting">作废/跳过</el-button>
          <el-button class="receipt-action-button receipt-action-button--success" :disabled="!selectedOrder" @click="confirmPrinted" :loading="isSubmitting">
            <span class="material-symbols-outlined text-[18px] mr-1">task_alt</span>确认已打印
          </el-button>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto bg-[#e5e7eb] p-8 flex justify-center">
        <div
          id="print-paper-area"
          class="bg-white shadow-xl relative overflow-hidden transition-opacity print-sheet"
          :class="!selectedOrder ? 'opacity-0' : 'opacity-100'"
        >
          <div v-if="!selectedOrder" class="absolute inset-0 flex flex-col items-center justify-center bg-white z-10 text-gray-400 opacity-100">
            <span class="material-symbols-outlined text-5xl mb-2">touch_app</span>
            <p>请在左侧列表中选择出库单进行打印</p>
          </div>

          <template v-if="selectedOrder">
            <div class="text-center relative mb-4">
              <h1 class="text-2xl font-black tracking-widest">星火服装厂</h1>
              <h2 class="text-lg font-bold tracking-widest mt-1 pb-2 border-b-2 border-black inline-block px-4">产品出库单</h2>
              <div class="absolute right-0 bottom-0 text-xs font-bold font-mono">单号：{{ selectedOrder.orderNo }}</div>
            </div>

            <div class="flex justify-between text-[13px] font-bold mb-2 gap-4">
              <div>客户：{{ selectedOrder.customerName || '--' }}</div>
              <div>日期：{{ formatDate(selectedOrder.createTime) }}</div>
            </div>

            <table class="w-full text-[12px] border-collapse border border-black text-center mb-2 receipt-table" style="table-layout: fixed;">
              <thead>
                <tr>
                  <th class="border border-black py-1.5 w-12">序号</th>
                  <th class="border border-black py-1.5">商品型号</th>
                  <th class="border border-black py-1.5">规格</th>
                  <th class="border border-black py-1.5 w-20">数量(米)</th>
                  <th class="border border-black py-1.5 w-24">单价</th>
                  <th class="border border-black py-1.5 w-24">金额</th>
                  <th class="border border-black py-1.5">备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in displayTableData" :key="index" class="h-8">
                  <td class="border border-black">{{ row.id ? index + 1 : '' }}</td>
                  <td class="border border-black text-left px-2 truncate">{{ row.modelCode }}</td>
                  <td class="border border-black">{{ row.spec }}</td>
                  <td class="border border-black font-mono">{{ row.meters }}</td>
                  <td class="border border-black font-mono">{{ row.price }}</td>
                  <td class="border border-black font-mono">{{ row.totalAmount }}</td>
                  <td class="border border-black text-left px-1 truncate">{{ row.barcode }}</td>
                </tr>
                <tr class="h-8 font-bold">
                  <td class="border border-black" colspan="3">合计</td>
                  <td class="border border-black font-mono">{{ totalMeters }}</td>
                  <td class="border border-black"></td>
                  <td class="border border-black font-mono">{{ totalAmount }}</td>
                  <td class="border border-black"></td>
                </tr>
              </tbody>
            </table>

            <div class="text-[11px] mb-4">大写金额：<span class="font-bold border-b border-black inline-block w-64 pb-0.5">{{ totalAmountText }}</span></div>

            <div class="flex justify-between text-[13px] font-bold mt-auto pt-4 gap-4 flex-wrap">
              <div>制单人：{{ selectedOrder.operator || '--' }}</div>
              <div>库管员：________</div>
              <div>送货人：________</div>
              <div>收货人(签字)：______________</div>
            </div>

            <div class="text-[10px] text-gray-600 mt-2 text-center absolute bottom-2 w-full left-0">第一联：存根(白)　　第二联：客户(红)　　第三联：财务(蓝)</div>
          </template>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelPrint, getPendingPrintOrders, getPrintDetail, markPrinted } from './receipt/api/receipt.js'

const isFetchingList = ref(false)
const isLoadingDetail = ref(false)
const isPrinting = ref(false)
const isSubmitting = ref(false)
const pendingOrders = ref([])
const selectedOrder = ref(null)
const tableData = ref([])
const ROWS_PER_PAGE = 5

onMounted(async () => {
  await fetchPendingList()
})

async function fetchPendingList() {
  isFetchingList.value = true
  selectedOrder.value = null
  tableData.value = []
  try {
    pendingOrders.value = await getPendingPrintOrders()
  } finally {
    isFetchingList.value = false
  }
}

async function selectOrder(order) {
  if (selectedOrder.value?.orderNo === order.orderNo) return
  isLoadingDetail.value = true
  try {
    const detail = await getPrintDetail({ orderNo: order.orderNo })
    selectedOrder.value = detail
    tableData.value = detail.items || []
  } finally {
    isLoadingDetail.value = false
  }
}

const displayTableData = computed(() => {
  const rows = tableData.value.map((item) => ({
    ...item,
    meters: formatNumber(item.meters),
    price: money(item.price),
    totalAmount: money(item.totalAmount),
    spec: item.spec == null ? '' : item.spec
  }))
  while (rows.length < ROWS_PER_PAGE) {
    rows.push({ id: '', modelCode: '', spec: '', meters: '', price: '', totalAmount: '', barcode: '' })
  }
  return rows.slice(0, ROWS_PER_PAGE)
})

const totalMeters = computed(() => formatNumber(tableData.value.reduce((sum, item) => sum + Number(item.meters || 0), 0)))
const totalAmount = computed(() => money(tableData.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)))
const totalAmountText = computed(() => totalAmount.value === '0.00' ? '' : `人民币 ${totalAmount.value} 元`)

async function openBrowserPrint() {
  if (!selectedOrder.value) return
  isPrinting.value = true
  try {
    const printable = document.getElementById('print-paper-area')
    if (!printable) {
      ElMessage.error('未找到打印内容')
      return
    }
    const printWindow = window.open('about:blank', '_blank', 'width=1200,height=900')
    if (!printWindow) {
      ElMessage.error('浏览器拦截了打印窗口，请允许弹窗后重试')
      return
    }
    const html = buildPrintHtml(printable.innerHTML)
    printWindow.document.open()
    printWindow.document.write(html)
    printWindow.document.close()
    printWindow.document.title = ''
    printWindow.focus()
    setTimeout(() => {
      printWindow.print()
    }, 300)
  } finally {
    isPrinting.value = false
  }
}

async function confirmPrinted() {
  if (!selectedOrder.value) return
  await ElMessageBox.confirm('确认这张出库单已经通过浏览器打印成功吗？确认后该单据会移出待打印队列。', '打印确认', {
    confirmButtonText: '确认已打印',
    cancelButtonText: '取消',
    type: 'warning'
  })
  isSubmitting.value = true
  try {
    await markPrinted({ orderNo: selectedOrder.value.orderNo })
    ElMessage.success('已标记为打印完成')
    await fetchPendingList()
  } finally {
    isSubmitting.value = false
  }
}

async function handleCancelPrint() {
  if (!selectedOrder.value) return
  await ElMessageBox.confirm(`确认作废/跳过单据 ${selectedOrder.value.orderNo} 吗？`, '操作确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  isSubmitting.value = true
  try {
    await cancelPrint({ orderNo: selectedOrder.value.orderNo })
    ElMessage.success('操作成功')
    await fetchPendingList()
  } finally {
    isSubmitting.value = false
  }
}

function buildPrintHtml(content) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title></title>
  <style>
    @page {
      size: 241mm 140mm;
      margin: 0;
    }
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      background: #fff;
      color: #000;
      font-family: SimSun, '宋体', serif;
    }
    .print-sheet {
      width: 241mm;
      min-height: 140mm;
      padding: 10mm;
      position: relative;
      overflow: hidden;
    }
    .receipt-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    .receipt-table th, .receipt-table td { border: 1px solid #000; }
    .text-center { text-align: center; }
    .text-left { text-align: left; }
    .text-right { text-align: right; }
    .font-black { font-weight: 900; }
    .font-bold { font-weight: 700; }
    .font-mono { font-family: 'Consolas', 'Courier New', monospace; }
    .tracking-widest { letter-spacing: 0.2em; }
    .text-2xl { font-size: 24px; }
    .text-lg { font-size: 18px; }
    .text-[13px] { font-size: 13px; }
    .text-[12px] { font-size: 12px; }
    .text-[11px] { font-size: 11px; }
    .text-[10px] { font-size: 10px; }
    .mb-4 { margin-bottom: 16px; }
    .mb-2 { margin-bottom: 8px; }
    .mt-1 { margin-top: 4px; }
    .mt-2 { margin-top: 8px; }
    .pt-4 { padding-top: 16px; }
    .pb-2 { padding-bottom: 8px; }
    .pb-0\.5 { padding-bottom: 2px; }
    .py-1\.5 { padding-top: 6px; padding-bottom: 6px; }
    .px-1 { padding-left: 4px; padding-right: 4px; }
    .px-2 { padding-left: 8px; padding-right: 8px; }
    .px-4 { padding-left: 16px; padding-right: 16px; }
    .w-12 { width: 48px; }
    .w-20 { width: 80px; }
    .w-24 { width: 96px; }
    .w-64 { width: 256px; }
    .h-8 { height: 32px; }
    .border-b-2 { border-bottom: 2px solid #000; }
    .border-b { border-bottom: 1px solid #000; }
    .inline-block { display: inline-block; }
    .flex { display: flex; }
    .justify-between { justify-content: space-between; }
    .items-start { align-items: flex-start; }
    .gap-4 { gap: 16px; }
    .gap-3 { gap: 12px; }
    .gap-2 { gap: 8px; }
    .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .relative { position: relative; }
    .absolute { position: absolute; }
    .right-0 { right: 0; }
    .bottom-0 { bottom: 0; }
    .bottom-2 { bottom: 8px; }
    .left-0 { left: 0; }
    .w-full { width: 100%; }
    .mt-auto { margin-top: auto; }
    .flex-wrap { flex-wrap: wrap; }
    @media print {
      body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    }
  </style>
</head>
<body>
  <div class="print-sheet">${content}</div>
</body>
</html>`
}

function formatDate(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatNumber(value) {
  return Number(value || 0).toFixed(2)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.print-sheet {
  width: 241mm;
  min-height: 140mm;
  padding: 10mm;
  font-family: 'SimSun', '宋体', serif;
  color: #000;
}

.receipt-table th,
.receipt-table td {
  border-color: #000;
}

:deep(.receipt-actions .el-button) {
  border-radius: 999px;
  min-height: 40px;
  padding: 0 18px;
  border-width: 1px;
  font-weight: 700;
}

:deep(.receipt-action-button) {
  border-color: rgba(15, 23, 42, 0.12);
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

:deep(.receipt-action-button:hover) {
  border-color: rgba(37, 99, 235, 0.28);
  color: #1d4ed8;
  background: #eff6ff;
}

:deep(.receipt-action-button--danger) {
  border-color: rgba(220, 38, 38, 0.14);
  color: #b91c1c;
  background: #fff5f5;
  box-shadow: 0 8px 18px rgba(220, 38, 38, 0.08);
}

:deep(.receipt-action-button--danger:hover) {
  border-color: rgba(220, 38, 38, 0.24);
  color: #991b1b;
  background: #fee2e2;
}

:deep(.receipt-action-button--success) {
  border-color: #0f766e;
  background: linear-gradient(135deg, #14b8a6, #0f766e);
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(15, 118, 110, 0.22);
}

:deep(.receipt-action-button--success:hover) {
  border-color: #115e59;
  background: linear-gradient(135deg, #0f766e, #115e59);
  color: #ffffff;
}

:deep(.receipt-action-button.is-disabled),
:deep(.receipt-action-button.is-disabled:hover) {
  background: #e5e7eb;
  border-color: #e5e7eb;
  color: #94a3b8;
  box-shadow: none;
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { border-radius: 4px; background-color: #cbd5e1; }
</style>
