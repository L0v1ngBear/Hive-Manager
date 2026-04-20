<template>
  <div class="max-w-[1400px] mx-auto h-[calc(100vh-6rem)] flex flex-col lg:flex-row gap-6 pb-6 pt-4 px-4 font-sans text-gray-800">
    <section class="w-full lg:w-[380px] flex flex-col bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden shrink-0">
      <header class="p-5 border-b border-gray-100 bg-white/80 backdrop-blur-md flex justify-between items-center z-10 sticky top-0">
        <div>
          <h2 class="text-lg font-extrabold text-gray-800 flex items-center gap-2">
            待打印任务队列
            <span v-if="pendingOrders.length > 0" class="bg-red-50 text-red-500 text-[11px] px-2.5 py-0.5 rounded-full font-bold border border-red-100 shadow-sm">
              {{ pendingOrders.length }}
            </span>
          </h2>
          <p class="text-xs text-gray-400 mt-1.5 font-medium">出库单自动进入队列，点击预览并打印。</p>
        </div>
        <el-button circle @click="fetchPendingList" :loading="isFetchingList" class="hover:bg-gray-50 border-gray-200 transition-colors">
          <span class="material-symbols-outlined text-gray-500 text-lg">refresh</span>
        </el-button>
      </header>

      <div class="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50/50 custom-scrollbar">
        <div v-if="pendingOrders.length === 0 && !isFetchingList" class="h-full flex flex-col items-center justify-center text-gray-400">
          <div class="bg-gray-100/50 p-4 rounded-full mb-3">
            <span class="material-symbols-outlined text-4xl text-gray-300">inventory_2</span>
          </div>
          <p class="text-sm font-medium">当前暂无待打印出库单</p>
        </div>

        <div
            v-for="item in pendingOrders"
            :key="item.orderNo"
            @click="selectOrder(item)"
            :class="[
            'group p-4 rounded-2xl cursor-pointer transition-all duration-300 relative overflow-hidden',
            selectedOrder?.orderNo === item.orderNo
              ? 'bg-blue-50/50 border-blue-200 ring-1 ring-blue-200 shadow-sm'
              : 'bg-white border-gray-100 shadow-sm hover:shadow-md hover:-translate-y-0.5 hover:border-gray-200 border'
          ]"
        >
          <div
              class="absolute left-0 top-0 bottom-0 w-1.5 rounded-l-2xl transition-colors duration-300"
              :class="selectedOrder?.orderNo === item.orderNo ? 'bg-blue-500' : 'bg-transparent group-hover:bg-gray-200'"
          ></div>

          <div class="flex justify-between items-start mb-3 gap-3 pl-1">
            <span class="text-[15px] font-black text-gray-800 font-mono tracking-tight">{{ item.orderNo }}</span>
            <span class="text-[11px] text-gray-500 bg-gray-100 px-2 py-1 rounded-md font-medium">{{ formatDate(item.createTime) }}</span>
          </div>

          <div class="pl-1 space-y-1.5">
            <div class="text-xs text-gray-500 font-medium truncate flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[14px]">person</span>
              客户：<span class="text-gray-700 font-bold">{{ item.customerName || '--' }}</span>
            </div>
            <div class="text-[11px] text-gray-400 flex items-center gap-1.5 bg-gray-50 p-1.5 rounded-lg border border-gray-50">
              <span class="material-symbols-outlined text-[14px]">format_list_bulleted</span>
              共 <span class="font-bold text-blue-600 px-0.5">{{ item.itemCount }}</span> 条 |
              合计 <span class="font-bold text-gray-600 px-0.5">{{ formatNumber(item.totalMeters) }}</span> 米
            </div>
          </div>
        </div>
      </div>
    </section>

    <aside class="flex-1 bg-white rounded-3xl shadow-sm border border-gray-100 flex flex-col overflow-hidden relative">
      <transition name="fade">
        <div v-if="isLoadingDetail" class="absolute inset-0 bg-white/60 backdrop-blur-md z-20 flex flex-col items-center justify-center">
          <span class="material-symbols-outlined text-blue-500 text-4xl animate-spin mb-3">progress_activity</span>
          <span class="text-sm text-gray-600 font-bold tracking-wide">正在生成高精度预览...</span>
        </div>
      </transition>

      <header class="px-6 py-4 border-b border-gray-100 flex justify-between items-center gap-4 bg-white z-10 flex-wrap shadow-sm">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-blue-500 bg-blue-50 p-1.5 rounded-lg">preview</span>
          <h3 class="text-[15px] font-bold text-gray-800">出库单打印预览</h3>
        </div>
        <div class="flex gap-3 flex-wrap justify-end receipt-actions">
          <el-button v-if="selectedOrder" class="action-btn-print" @click="openBrowserPrint" :loading="isPrinting">
            <span class="material-symbols-outlined text-[18px] mr-1">print</span>直接打印
          </el-button>
          <el-button v-if="selectedOrder" class="action-btn-cancel" @click="handleCancelPrint" :loading="isSubmitting">
            <span class="material-symbols-outlined text-[16px] mr-1">block</span>作废/跳过
          </el-button>
          <el-button class="action-btn-success" :disabled="!selectedOrder" @click="confirmPrinted" :loading="isSubmitting">
            <span class="material-symbols-outlined text-[18px] mr-1">task_alt</span>确认已打印
          </el-button>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto bg-slate-50 bg-[radial-gradient(#cbd5e1_1px,transparent_1px)] [background-size:20px_20px] p-8 flex justify-center custom-scrollbar">
        <div
            id="print-paper-area"
            class="bg-white relative overflow-hidden transition-all duration-500 print-sheet"
            :class="!selectedOrder ? 'opacity-0 scale-95 translate-y-4' : 'opacity-100 scale-100 translate-y-0 paper-shadow'"
        >
          <div v-if="!selectedOrder" class="absolute inset-0 flex flex-col items-center justify-center bg-white/90 backdrop-blur-sm z-10 text-gray-400">
            <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
              <span class="material-symbols-outlined text-3xl text-blue-400">ads_click</span>
            </div>
            <p class="font-medium tracking-wide">请在左侧列表中选择出库单进行预览</p>
          </div>

          <template v-if="selectedOrder">
            <div class="text-center relative mb-5">
              <h1 class="text-[26px] font-black tracking-[0.25em] text-gray-900">星火服装厂</h1>
              <h2 class="text-[17px] font-bold tracking-[0.2em] mt-2 pb-1.5 border-b-2 border-gray-900 inline-block px-6">产品出库单</h2>
              <div class="absolute right-0 bottom-1 text-[11px] font-bold font-mono text-gray-700 bg-gray-50 px-2 py-0.5 border border-gray-200">
                NO: {{ selectedOrder.orderNo }}
              </div>
            </div>

            <div class="flex justify-between text-[13px] font-bold mb-2.5 px-1 text-gray-800">
              <div class="flex gap-2">
                <span class="text-gray-600">客户名称：</span>
                <span class="border-b border-gray-400 pb-0.5 px-2 min-w-[120px]">{{ selectedOrder.customerName || '--' }}</span>
              </div>
              <div class="flex gap-2">
                <span class="text-gray-600">打印日期：</span>
                <span>{{ formatDate(selectedOrder.createTime) }}</span>
              </div>
            </div>

            <table class="w-full text-[12px] border-collapse text-center mb-3 receipt-table" style="table-layout: fixed;">
              <thead>
              <tr class="bg-gray-50/50">
                <th class="border border-gray-800 py-2 w-12 text-gray-700">序号</th>
                <th class="border border-gray-800 py-2 text-gray-700">商品型号</th>
                <th class="border border-gray-800 py-2 text-gray-700">规格</th>
                <th class="border border-gray-800 py-2 w-20 text-gray-700">数量(米)</th>
                <th class="border border-gray-800 py-2 w-24 text-gray-700">单价</th>
                <th class="border border-gray-800 py-2 w-24 text-gray-700">金额(元)</th>
                <th class="border border-gray-800 py-2 text-gray-700">备注(条码)</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, index) in displayTableData" :key="index" class="h-8 hover:bg-gray-50/30 transition-colors">
                <td class="border border-gray-800 text-gray-600">{{ row.id ? index + 1 : '' }}</td>
                <td class="border border-gray-800 text-left px-2 truncate font-medium">{{ row.modelCode }}</td>
                <td class="border border-gray-800">{{ row.spec }}</td>
                <td class="border border-gray-800 font-mono font-bold">{{ row.meters }}</td>
                <td class="border border-gray-800 font-mono">{{ row.price }}</td>
                <td class="border border-gray-800 font-mono font-bold">{{ row.totalAmount }}</td>
                <td class="border border-gray-800 text-left px-2 truncate text-[11px] text-gray-500">{{ row.barcode }}</td>
              </tr>
              <tr class="h-9 font-bold bg-gray-50/50">
                <td class="border border-gray-800" colspan="3">合 计</td>
                <td class="border border-gray-800 font-mono text-[13px]">{{ totalMeters }}</td>
                <td class="border border-gray-800"></td>
                <td class="border border-gray-800 font-mono text-[13px]">{{ totalAmount }}</td>
                <td class="border border-gray-800"></td>
              </tr>
              </tbody>
            </table>

            <div class="text-[12px] mb-6 px-1 flex items-center">
              <span class="text-gray-600 font-bold">大写金额：</span>
              <span class="font-bold border-b border-gray-600 inline-block px-4 pb-0.5 tracking-wider">{{ totalAmountText || '零元整' }}</span>
            </div>

            <div class="flex justify-between text-[13px] font-bold mt-auto pt-6 px-4 text-gray-700">
              <div>制单人：<span class="font-normal border-b border-gray-400 px-4">{{ selectedOrder.operator || '--' }}</span></div>
              <div>库管员：<span class="font-normal border-b border-gray-400 px-8 text-transparent">签名区</span></div>
              <div>送货人：<span class="font-normal border-b border-gray-400 px-8 text-transparent">签名区</span></div>
              <div>收货人(签字)：<span class="font-normal border-b border-gray-400 px-12 text-transparent">签名区</span></div>
            </div>

            <div class="text-[11px] text-gray-500 mt-4 pt-3 border-t border-dashed border-gray-300 text-center absolute bottom-3 w-[calc(100%-20mm)] mx-auto font-medium tracking-widest">
              第一联：存根(白)　　第二联：客户(红)　　第三联：财务(蓝)
            </div>
          </template>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, ElButton } from 'element-plus'
// 你的 API 接口保持不变
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
    printWindow.document.title = `出库单_${selectedOrder.value.orderNo}`
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
    confirmButtonText: '确认作废',
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
  <title>打印出库单</title>
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
      font-family: 'SimSun', '宋体', serif;
    }
    .print-sheet {
      width: 241mm;
      min-height: 140mm;
      padding: 10mm;
      position: relative;
      overflow: hidden;
    }
    /* ... 保持打印样式不变，确保兼容旧版打印机 ... */
    .receipt-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    .receipt-table th, .receipt-table td { border: 1px solid #000; }
    .text-center { text-align: center; }
    .text-left { text-align: left; }
    .font-black { font-weight: 900; }
    .font-bold { font-weight: 700; }
    .font-mono { font-family: 'Consolas', 'Courier New', monospace; }
    .tracking-[0.25em] { letter-spacing: 0.25em; }
    .tracking-[0.2em] { letter-spacing: 0.2em; }
    .tracking-widest { letter-spacing: 0.1em; }
    .text-[26px] { font-size: 26px; }
    .text-[17px] { font-size: 17px; }
    .text-[13px] { font-size: 13px; }
    .text-[12px] { font-size: 12px; }
    .text-[11px] { font-size: 11px; }
    .mb-5 { margin-bottom: 20px; }
    .mb-6 { margin-bottom: 24px; }
    .mb-2\.5 { margin-bottom: 10px; }
    .mt-2 { margin-top: 8px; }
    .mt-4 { margin-top: 16px; }
    .pt-6 { padding-top: 24px; }
    .pt-3 { padding-top: 12px; }
    .pb-1\.5 { padding-bottom: 6px; }
    .py-2 { padding-top: 8px; padding-bottom: 8px; }
    .px-1 { padding-left: 4px; padding-right: 4px; }
    .px-2 { padding-left: 8px; padding-right: 8px; }
    .px-4 { padding-left: 16px; padding-right: 16px; }
    .px-6 { padding-left: 24px; padding-right: 24px; }
    .px-8 { padding-left: 32px; padding-right: 32px; }
    .px-12 { padding-left: 48px; padding-right: 48px; }
    .w-12 { width: 48px; }
    .w-20 { width: 80px; }
    .w-24 { width: 96px; }
    .min-w-[120px] { min-width: 120px; }
    .h-8 { height: 32px; }
    .h-9 { height: 36px; }
    .border-b-2 { border-bottom: 2px solid #000; }
    .border-b { border-bottom: 1px solid #000; }
    .border-t { border-top: 1px solid #000; }
    .border-dashed { border-style: dashed; }
    .inline-block { display: inline-block; }
    .flex { display: flex; }
    .items-center { align-items: center; }
    .justify-between { justify-content: space-between; }
    .gap-2 { gap: 8px; }
    .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .relative { position: relative; }
    .absolute { position: absolute; }
    .right-0 { right: 0; }
    .bottom-1 { bottom: 4px; }
    .bottom-3 { bottom: 12px; }
    .w-full { width: 100%; }
    .w-\[calc\(100\%-20mm\)\] { width: calc(100% - 20mm); }
    .mx-auto { margin-left: auto; margin-right: auto; }
    .mt-auto { margin-top: auto; }
    .text-transparent { color: transparent; }
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
/* 纸张预览区域核心尺寸 */
.print-sheet {
  width: 241mm;
  min-height: 140mm;
  padding: 10mm;
  font-family: 'SimSun', '宋体', serif;
  color: #111827; /* 略微深灰增加屏幕阅读舒适度，打印时会被覆盖为纯黑 */
}

/* 逼真的纸张阴影 */
.paper-shadow {
  box-shadow:
      0 20px 25px -5px rgb(0 0 0 / 0.1),
      0 8px 10px -6px rgb(0 0 0 / 0.1),
      0 0 1px 0 rgb(0 0 0 / 0.2);
}

.receipt-table th,
.receipt-table td {
  border-color: #374151; /* 屏幕上柔和的边框色 */
}

/* --- 按钮样式优化 --- */
:deep(.receipt-actions .el-button) {
  border-radius: 12px; /* 圆角改为现代的12px */
  min-height: 38px;
  padding: 0 16px;
  font-weight: 600;
  transition: all 0.2s ease;
}

:deep(.action-btn-print) {
  border-color: #e2e8f0;
  background: #ffffff;
  color: #334155;
  box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
}
:deep(.action-btn-print:hover) {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
}

:deep(.action-btn-cancel) {
  border-color: transparent;
  background: transparent;
  color: #64748b;
}
:deep(.action-btn-cancel:hover) {
  background: #f1f5f9;
  color: #ef4444;
}

:deep(.action-btn-success) {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #ffffff;
  box-shadow: 0 4px 6px -1px rgb(59 130 246 / 0.3);
}
:deep(.action-btn-success:hover) {
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 6px 8px -1px rgb(59 130 246 / 0.4);
}
:deep(.action-btn-success.is-disabled) {
  background: #cbd5e1;
  border-color: #cbd5e1;
  box-shadow: none;
  transform: none;
}

/* 渐隐动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 优美的滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  border-radius: 10px;
  background-color: #cbd5e1;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}
</style>