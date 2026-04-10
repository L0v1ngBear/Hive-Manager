<template>
  <div class="max-w-7xl mx-auto h-[calc(100vh-8rem)] flex flex-col lg:flex-row gap-6 pb-6">
    <section class="w-full lg:w-[400px] flex flex-col bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 overflow-hidden">
      <header class="p-5 border-b border-outline-variant/20 bg-surface flex justify-between items-center z-10">
        <div>
          <h2 class="text-lg font-black text-on-surface flex items-center gap-2">
            待打印任务队列
            <span class="bg-error/10 text-error text-[10px] px-2 py-0.5 rounded-full font-bold">{{ pendingOrders.length }}</span>
          </h2>
          <p class="text-xs text-on-surface-variant mt-1">小程序结单后会进入这里，由 PC 端统一打印。</p>
        </div>
        <el-button circle @click="fetchPendingList" :loading="isFetchingList">
          <span class="material-symbols-outlined text-lg">refresh</span>
        </el-button>
      </header>

      <div class="flex-1 overflow-y-auto p-3 space-y-2 bg-surface-container-lowest">
        <div v-if="pendingOrders.length === 0 && !isFetchingList" class="h-full flex flex-col items-center justify-center text-on-surface-variant/50">
          <span class="material-symbols-outlined text-4xl mb-2">task_alt</span>
          <p class="text-sm font-medium">当前暂无待打印单据</p>
        </div>

        <div v-for="item in pendingOrders" :key="item.orderNo" @click="selectOrder(item)" :class="[
          'p-4 rounded-xl cursor-pointer transition-all border relative overflow-hidden',
          selectedOrder?.orderNo === item.orderNo ? 'bg-primary/5 border-primary/30 ring-1 ring-primary/30' : 'bg-surface hover:bg-surface-container border-outline-variant/20'
        ]">
          <div v-if="selectedOrder?.orderNo === item.orderNo" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>
          <div class="flex justify-between items-start mb-2">
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
        <span class="text-sm text-primary font-bold">正在生成单据排版...</span>
      </div>

      <header class="p-4 border-b border-outline-variant/20 flex justify-between items-center bg-surface z-10">
        <h3 class="text-sm font-bold text-on-surface">出库单打印预览（241mm × 140mm）</h3>
        <div class="flex gap-2">
          <el-button v-if="selectedOrder" plain type="danger" @click="handleCancelPrint">作废/跳过</el-button>
          <el-button type="success" :disabled="!selectedOrder" @click="executePrint">
            <span class="material-symbols-outlined text-lg mr-1">print</span>开始打印
          </el-button>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto bg-[#e5e7eb] p-8 flex justify-center">
        <div id="print-paper-area" class="bg-white shadow-xl relative overflow-hidden transition-opacity" :class="!selectedOrder ? 'opacity-0' : 'opacity-100'" style="width: 241mm; min-height: 140mm; padding: 10mm; font-family: 'SimSun', '宋体', serif; color: #000;">
          <div v-if="!selectedOrder" class="absolute inset-0 flex flex-col items-center justify-center bg-white z-10 text-gray-400 print:hidden opacity-100">
            <span class="material-symbols-outlined text-5xl mb-2">touch_app</span>
            <p>请在左侧列表中选择单据进行打印</p>
          </div>

          <template v-if="selectedOrder">
            <div class="text-center relative mb-4">
              <h1 class="text-2xl font-black tracking-widest">星火服装厂</h1>
              <h2 class="text-lg font-bold tracking-widest mt-1 pb-2 border-b-2 border-black inline-block px-4">产品出库单</h2>
              <div class="absolute right-0 bottom-0 text-xs font-bold font-mono">单号：{{ selectedOrder.orderNo }}</div>
            </div>

            <div class="flex justify-between text-[13px] font-bold mb-2">
              <div>购货单位：{{ selectedOrder.customerName || '--' }}</div>
              <div>日期：{{ formatDate(selectedOrder.createTime) }}</div>
            </div>

            <table class="w-full text-[12px] border-collapse border border-black text-center mb-2" style="table-layout: fixed;">
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

            <div class="flex justify-between text-[13px] font-bold mt-auto pt-4">
              <div>制单人：{{ selectedOrder.operator || '--' }}</div>
              <div>库管员：________</div>
              <div>送货人：________</div>
              <div>收货人(签字)：_______________</div>
            </div>

            <div class="text-[10px] text-gray-600 mt-2 text-center absolute bottom-2 w-full left-0">第一联：存根(白) &nbsp;&nbsp;&nbsp; 第二联：客户(红) &nbsp;&nbsp;&nbsp; 第三联：财务(蓝)</div>
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
const pendingOrders = ref([])
const selectedOrder = ref(null)
const tableData = ref([])
const ROWS_PER_PAGE = 5

onMounted(fetchPendingList)

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

async function executePrint() {
  if (!selectedOrder.value) return
  window.print()
  try {
    await ElMessageBox.confirm('请确认纸质出库单已打印成功，确认后该单据将移出待打印队列。', '打印确认', {
      confirmButtonText: '打印成功',
      cancelButtonText: '稍后再打',
      type: 'warning'
    })
    await markPrinted({ orderNo: selectedOrder.value.orderNo })
    ElMessage.success('已标记为打印完成。')
    await fetchPendingList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
    }
  }
}

async function handleCancelPrint() {
  if (!selectedOrder.value) return
  await ElMessageBox.confirm(`确认作废/跳过单据 ${selectedOrder.value.orderNo} 吗？`, '操作确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await cancelPrint({ orderNo: selectedOrder.value.orderNo })
  ElMessage.success('操作成功。')
  await fetchPendingList()
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
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-thumb { border-radius: 4px; background-color: #cbd5e1; }

@media print {
  @page { size: 241mm 140mm; margin: 0; }
  body { -webkit-print-color-adjust: exact; color: #000; background: #fff; }
  body * { visibility: hidden; }
  #print-paper-area, #print-paper-area * { visibility: visible; }
  #print-paper-area {
    box-shadow: none !important;
    border: none !important;
    margin: 0 !important;
    position: fixed !important;
    left: 0 !important;
    top: 0 !important;
  }
  table, th, td { border-color: #000 !important; }
}
</style>