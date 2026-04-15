<template>
  <div class="h-full min-h-0 max-w-7xl mx-auto flex flex-col gap-4">
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <article class="rounded-2xl bg-surface-container-lowest ring-1 ring-outline-variant/15 p-4">
        <div class="text-xs font-bold tracking-[0.2em] text-on-surface-variant">待打印单据</div>
        <div class="mt-3 text-3xl font-black text-primary">{{ pendingOrders.length }}</div>
        <div class="mt-2 text-sm text-on-surface-variant">已提交到管理端、等待纸质打印的出库单</div>
      </article>
      <article class="rounded-2xl bg-surface-container-lowest ring-1 ring-outline-variant/15 p-4">
        <div class="text-xs font-bold tracking-[0.2em] text-on-surface-variant">当前选中</div>
        <div class="mt-3 text-2xl font-black text-on-surface">{{ selectedOrder?.orderNo || '--' }}</div>
        <div class="mt-2 text-sm text-on-surface-variant">{{ selectedOrder?.customerName || '请选择左侧待打印单据' }}</div>
      </article>
      <article class="rounded-2xl bg-surface-container-lowest ring-1 ring-outline-variant/15 p-4">
        <div class="text-xs font-bold tracking-[0.2em] text-on-surface-variant">打印状态</div>
        <div class="mt-3 text-2xl font-black text-on-surface">{{ selectedOrder ? '可打印' : '待选择' }}</div>
        <div class="mt-2 text-sm text-on-surface-variant">打印后可直接标记完成，或作废当前单据</div>
      </article>
    </section>

    <div class="flex-1 min-h-0 flex flex-col xl:flex-row gap-4">
      <section class="xl:w-[360px] 2xl:w-[400px] rounded-2xl bg-surface-container-lowest ring-1 ring-outline-variant/15 overflow-hidden flex flex-col">
        <header class="p-4 border-b border-outline-variant/15 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-black text-on-surface">待打印队列</h2>
            <p class="text-sm text-on-surface-variant mt-1">按出库完成时间排序，点击左侧卡片进入打印预览</p>
          </div>
          <el-button circle @click="fetchPendingList" :loading="isFetchingList" title="刷新待打印列表">
            <span class="material-symbols-outlined text-lg">refresh</span>
          </el-button>
        </header>

        <div class="px-4 py-3 border-b border-outline-variant/10 flex items-center justify-between text-sm">
          <span class="text-on-surface-variant">待处理数量</span>
          <span class="inline-flex items-center gap-2 rounded-full bg-error/10 px-3 py-1 text-error font-bold">
            <span class="material-symbols-outlined text-base">print</span>
            {{ pendingOrders.length }} 单
          </span>
        </div>

        <div class="flex-1 overflow-y-auto p-3 space-y-3">
          <div v-if="!isFetchingList && pendingOrders.length === 0" class="h-full min-h-[240px] flex flex-col items-center justify-center text-on-surface-variant">
            <span class="material-symbols-outlined text-5xl mb-3 opacity-60">inventory_2</span>
            <p class="text-base font-bold">当前没有待打印出库单</p>
            <p class="text-sm mt-2">小程序完成出库后，会自动进入这里等待管理端打印。</p>
          </div>

          <button
            v-for="item in pendingOrders"
            :key="item.orderNo"
            type="button"
            @click="selectOrder(item)"
            :class="[
              'w-full text-left rounded-2xl border p-4 transition-all shadow-sm',
              selectedOrder?.orderNo === item.orderNo
                ? 'border-primary bg-primary/5 shadow-primary/10'
                : 'border-outline-variant/15 bg-surface hover:border-primary/30 hover:bg-surface-container'
            ]"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="text-sm font-black text-on-surface font-mono">{{ item.orderNo }}</div>
                <div class="mt-1 text-sm text-on-surface-variant">{{ item.customerName || '未填写客户名称' }}</div>
              </div>
              <span class="rounded-full bg-surface-container px-2.5 py-1 text-xs font-bold text-on-surface-variant">
                {{ formatDate(item.createTime) }}
              </span>
            </div>
            <div class="mt-3 grid grid-cols-3 gap-2 text-xs text-on-surface-variant">
              <div class="rounded-xl bg-surface-container-low px-3 py-2">
                <div>条目数</div>
                <div class="mt-1 font-black text-on-surface">{{ item.itemCount || 0 }}</div>
              </div>
              <div class="rounded-xl bg-surface-container-low px-3 py-2">
                <div>总米数</div>
                <div class="mt-1 font-black text-on-surface">{{ formatNumber(item.totalMeters) }}</div>
              </div>
              <div class="rounded-xl bg-surface-container-low px-3 py-2">
                <div>经办人</div>
                <div class="mt-1 font-black text-on-surface truncate">{{ item.operator || '--' }}</div>
              </div>
            </div>
          </button>
        </div>
      </section>

      <section class="flex-1 min-h-0 rounded-2xl bg-surface-container-lowest ring-1 ring-outline-variant/15 overflow-hidden flex flex-col relative">
        <div v-if="isLoadingDetail" class="absolute inset-0 z-20 bg-white/70 backdrop-blur-sm flex flex-col items-center justify-center">
          <span class="material-symbols-outlined text-4xl text-primary animate-spin">progress_activity</span>
          <p class="mt-3 text-sm font-bold text-primary">正在加载打印明细...</p>
        </div>

        <header class="p-4 border-b border-outline-variant/15 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h3 class="text-lg font-black text-on-surface">出库单打印预览</h3>
            <p class="text-sm text-on-surface-variant mt-1">支持直接打印、打印后标记完成，以及异常单据作废处理。</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <el-button @click="previewPrint" :disabled="!selectedOrder" :loading="isPrinting" plain>
              <span class="material-symbols-outlined text-lg mr-1">preview</span>
              打印预览
            </el-button>
            <el-button type="primary" @click="executePrint" :disabled="!selectedOrder" :loading="isPrinting">
              <span class="material-symbols-outlined text-lg mr-1">print</span>
              直接打印
            </el-button>
            <el-button type="success" plain @click="handleMarkPrinted" :disabled="!selectedOrder" :loading="isSubmitting">
              <span class="material-symbols-outlined text-lg mr-1">task_alt</span>
              标记已打印
            </el-button>
            <el-button type="danger" plain @click="handleCancelPrint" :disabled="!selectedOrder" :loading="isSubmitting">
              <span class="material-symbols-outlined text-lg mr-1">block</span>
              作废单据
            </el-button>
          </div>
        </header>

        <div class="px-4 py-3 border-b border-outline-variant/10 bg-surface flex flex-wrap gap-x-6 gap-y-2 text-sm">
          <span class="text-on-surface-variant">单号：<strong class="text-on-surface font-mono">{{ selectedOrder?.orderNo || '--' }}</strong></span>
          <span class="text-on-surface-variant">客户：<strong class="text-on-surface">{{ selectedOrder?.customerName || '--' }}</strong></span>
          <span class="text-on-surface-variant">经办：<strong class="text-on-surface">{{ selectedOrder?.operator || '--' }}</strong></span>
          <span class="text-on-surface-variant">总金额：<strong class="text-on-surface">¥{{ totalAmount }}</strong></span>
        </div>

        <div class="flex-1 min-h-0 overflow-auto bg-[linear-gradient(180deg,#dfe7f1_0%,#edf2f7_100%)] p-4 md:p-6">
          <div class="mx-auto w-full max-w-[980px] min-h-[560px] rounded-[28px] border border-slate-300/70 bg-white shadow-[0_18px_60px_rgba(15,23,42,0.12)] p-6 md:p-8">
            <div v-if="!selectedOrder" class="h-full min-h-[500px] flex flex-col items-center justify-center text-center text-slate-400">
              <span class="material-symbols-outlined text-6xl mb-4">print_add</span>
              <div class="text-xl font-black text-slate-500">请选择待打印出库单</div>
              <p class="mt-2 text-sm max-w-md leading-6">左侧队列会展示所有待处理出库单。选中后即可查看内容、预览排版并发起打印。</p>
            </div>

            <article v-else id="receipt-print-sheet" class="text-black" style="font-family: 'SimSun', '宋体', serif;">
              <div class="flex items-start justify-between gap-4 border-b-2 border-black pb-4">
                <div>
                  <div class="text-[32px] font-black tracking-[0.35em] leading-none">出库单</div>
                  <div class="mt-2 text-sm tracking-[0.25em]">仓储发货凭证</div>
                </div>
                <div class="text-right text-sm leading-7">
                  <div>单号：<span class="font-bold font-mono">{{ selectedOrder.orderNo }}</span></div>
                  <div>日期：<span class="font-bold">{{ formatDate(selectedOrder.createTime) }}</span></div>
                  <div>客户：<span class="font-bold">{{ selectedOrder.customerName || '--' }}</span></div>
                </div>
              </div>

              <div class="mt-4 grid grid-cols-2 gap-4 text-sm">
                <div class="rounded-2xl border border-black/80 px-4 py-3">
                  <div class="text-xs text-slate-500">经办人</div>
                  <div class="mt-1 text-base font-bold">{{ selectedOrder.operator || '--' }}</div>
                </div>
                <div class="rounded-2xl border border-black/80 px-4 py-3">
                  <div class="text-xs text-slate-500">条目统计</div>
                  <div class="mt-1 text-base font-bold">{{ tableData.length }} 项 / {{ totalMeters }} 米</div>
                </div>
              </div>

              <table class="mt-4 w-full border-collapse text-center text-[13px]" style="table-layout: fixed;">
                <thead>
                  <tr>
                    <th class="sheet-th w-14">序号</th>
                    <th class="sheet-th">面料型号</th>
                    <th class="sheet-th w-20">规格</th>
                    <th class="sheet-th w-24">米数</th>
                    <th class="sheet-th w-24">单价</th>
                    <th class="sheet-th w-28">金额</th>
                    <th class="sheet-th">条码 / 备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in printableRows" :key="index">
                    <td class="sheet-td">{{ row.id ? index + 1 : '' }}</td>
                    <td class="sheet-td px-2 text-left">{{ row.modelCode }}</td>
                    <td class="sheet-td">{{ row.spec }}</td>
                    <td class="sheet-td font-mono">{{ row.meters }}</td>
                    <td class="sheet-td font-mono">{{ row.price }}</td>
                    <td class="sheet-td font-mono">{{ row.totalAmount }}</td>
                    <td class="sheet-td px-2 text-left">{{ row.note }}</td>
                  </tr>
                  <tr class="font-bold">
                    <td class="sheet-td" colspan="3">合计</td>
                    <td class="sheet-td font-mono">{{ totalMeters }}</td>
                    <td class="sheet-td"></td>
                    <td class="sheet-td font-mono">{{ totalAmount }}</td>
                    <td class="sheet-td"></td>
                  </tr>
                </tbody>
              </table>

              <div class="mt-4 rounded-2xl border border-black/80 px-4 py-3 text-sm">
                <span class="text-slate-500">金额大写：</span>
                <span class="font-bold">{{ totalAmountText }}</span>
              </div>

              <div class="mt-8 grid grid-cols-4 gap-4 text-sm">
                <div>制单人：{{ selectedOrder.operator || '--' }}</div>
                <div>仓管员：__________</div>
                <div>送货人：__________</div>
                <div>签收人：__________</div>
              </div>

              <div class="mt-6 text-center text-xs tracking-[0.2em] text-slate-500">
                第一联：存根联　　第二联：客户联　　第三联：财务联
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelPrint, getPendingPrintOrders, getPrintDetail, markPrinted } from './receipt/api/receipt.js'

const ROWS_PER_PAGE = 8

const isFetchingList = ref(false)
const isLoadingDetail = ref(false)
const isPrinting = ref(false)
const isSubmitting = ref(false)
const pendingOrders = ref([])
const selectedOrder = ref(null)
const tableData = ref([])

onMounted(fetchPendingList)

async function fetchPendingList(keepSelection = false) {
  const previousOrderNo = keepSelection ? selectedOrder.value?.orderNo : null
  isFetchingList.value = true
  try {
    const list = await getPendingPrintOrders()
    pendingOrders.value = Array.isArray(list) ? list : []
    if (!previousOrderNo) {
      selectedOrder.value = null
      tableData.value = []
      return
    }
    const matched = pendingOrders.value.find((item) => item.orderNo === previousOrderNo)
    if (matched) {
      await selectOrder(matched, true)
    } else {
      selectedOrder.value = null
      tableData.value = []
    }
  } finally {
    isFetchingList.value = false
  }
}

async function selectOrder(order, silent = false) {
  if (!order?.orderNo) {
    return
  }
  if (!silent && selectedOrder.value?.orderNo === order.orderNo) {
    return
  }
  isLoadingDetail.value = true
  try {
    const detail = await getPrintDetail({ orderNo: order.orderNo })
    selectedOrder.value = detail
    tableData.value = Array.isArray(detail?.items) ? detail.items : []
  } finally {
    isLoadingDetail.value = false
  }
}

const printableRows = computed(() => {
  const rows = tableData.value.map((item) => ({
    ...item,
    modelCode: item.modelCode || '--',
    spec: item.spec == null || item.spec === '' ? '--' : String(item.spec),
    meters: formatNumber(item.meters),
    price: money(item.price),
    totalAmount: money(item.totalAmount),
    note: item.barcode || item.remark || '--'
  }))
  while (rows.length < ROWS_PER_PAGE) {
    rows.push({ id: '', modelCode: '', spec: '', meters: '', price: '', totalAmount: '', note: '' })
  }
  return rows.slice(0, ROWS_PER_PAGE)
})

const totalMeters = computed(() => formatNumber(tableData.value.reduce((sum, item) => sum + Number(item?.meters || 0), 0)))
const totalAmount = computed(() => money(tableData.value.reduce((sum, item) => sum + Number(item?.totalAmount || 0), 0)))
const totalAmountText = computed(() => {
  const amount = Number(totalAmount.value || 0)
  return amount <= 0 ? '零元整' : toChineseCurrency(amount)
})

async function previewPrint() {
  if (!selectedOrder.value) {
    return
  }
  await printReceipt()
}

async function executePrint() {
  if (!selectedOrder.value) {
    return
  }
  await printReceipt()
  try {
    await ElMessageBox.confirm(
      `请确认出库单 ${selectedOrder.value.orderNo} 已打印完成。确认后该单据将从待打印队列移除。`,
      '打印确认',
      {
        confirmButtonText: '已打印完成',
        cancelButtonText: '稍后再处理',
        type: 'warning'
      }
    )
    await handleMarkPrinted(true)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

async function handleMarkPrinted(skipConfirm = false) {
  if (!selectedOrder.value) {
    return
  }
  if (!skipConfirm) {
    await ElMessageBox.confirm(
      `确认将出库单 ${selectedOrder.value.orderNo} 标记为已打印吗？`,
      '标记已打印',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
  }
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
  if (!selectedOrder.value) {
    return
  }
  await ElMessageBox.confirm(
    `确认作废出库单 ${selectedOrder.value.orderNo} 吗？作废后将不会继续出现在待打印列表中。`,
    '作废确认',
    {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  isSubmitting.value = true
  try {
    await cancelPrint({ orderNo: selectedOrder.value.orderNo })
    ElMessage.success('出库单已作废')
    await fetchPendingList()
  } finally {
    isSubmitting.value = false
  }
}

async function printReceipt() {
  const target = document.getElementById('receipt-print-sheet')
  if (!target) {
    ElMessage.warning('请先选择需要打印的出库单')
    return
  }
  isPrinting.value = true
  try {
    const iframe = document.createElement('iframe')
    iframe.style.position = 'fixed'
    iframe.style.right = '0'
    iframe.style.bottom = '0'
    iframe.style.width = '0'
    iframe.style.height = '0'
    iframe.style.border = '0'
    document.body.appendChild(iframe)

    const doc = iframe.contentWindow?.document
    if (!doc) {
      throw new Error('打印窗口创建失败')
    }

    doc.open()
    doc.write(`
      <!doctype html>
      <html lang="zh-CN">
        <head>
          <meta charset="UTF-8" />
          <title>出库单打印</title>
          <style>
            @page { size: A4 portrait; margin: 12mm; }
            body { margin: 0; color: #000; background: #fff; font-family: "SimSun", "宋体", serif; }
            .sheet-th, .sheet-td {
              border: 1px solid #000;
              padding: 8px 6px;
              vertical-align: middle;
            }
            table { width: 100%; border-collapse: collapse; table-layout: fixed; }
          </style>
        </head>
        <body>${target.outerHTML}</body>
      </html>
    `)
    doc.close()

    await new Promise((resolve) => setTimeout(resolve, 180))
    iframe.contentWindow?.focus()
    iframe.contentWindow?.print()
    await new Promise((resolve) => setTimeout(resolve, 200))
    iframe.remove()
  } finally {
    isPrinting.value = false
  }
}

function formatDate(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatNumber(value) {
  return Number(value || 0).toFixed(2)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function toChineseCurrency(value) {
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const sectionUnits = ['元', '万', '亿', '兆']
  const digitUnits = ['', '拾', '佰', '仟']
  const scaled = Math.round(Number(value || 0) * 100)
  if (!scaled) {
    return '零元整'
  }

  let integerPart = Math.floor(scaled / 100)
  const decimalPart = scaled % 100
  let result = ''
  let sectionIndex = 0

  while (integerPart > 0) {
    let section = integerPart % 10000
    let sectionText = ''
    let digitIndex = 0
    let zeroPending = false

    while (section > 0) {
      const digit = section % 10
      if (digit === 0) {
        if (!zeroPending && sectionText) {
          sectionText = `${digits[0]}${sectionText}`
        }
        zeroPending = true
      } else {
        sectionText = `${digits[digit]}${digitUnits[digitIndex]}${sectionText}`
        zeroPending = false
      }
      section = Math.floor(section / 10)
      digitIndex += 1
    }

    if (sectionText) {
      result = `${sectionText}${sectionUnits[sectionIndex]}${result}`
    }
    integerPart = Math.floor(integerPart / 10000)
    sectionIndex += 1
  }

  result = result
    .replace(/零+/g, '零')
    .replace(/零(万|亿|兆|元)/g, '$1')
    .replace(/亿万/g, '亿')

  if (decimalPart === 0) {
    return `${result}整`
  }

  const jiao = Math.floor(decimalPart / 10)
  const fen = decimalPart % 10
  let decimalText = ''
  if (jiao > 0) {
    decimalText += `${digits[jiao]}角`
  }
  if (fen > 0) {
    decimalText += `${digits[fen]}分`
  }
  return `${result}${decimalText}`
}
</script>

<style scoped>
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  border-radius: 9999px;
  background-color: rgba(100, 116, 139, 0.35);
}

.sheet-th,
.sheet-td {
  border: 1px solid #000;
  padding: 8px 6px;
  vertical-align: middle;
}
</style>
