<template>
  <div class="receipt-page-shell">
    <header class="receipt-hero function-page-header">
      <div>
        <div class="function-page-eyebrow">
          <span class="material-symbols-outlined">print</span>
          出库打印中心
        </div>
        <h1 class="function-page-title">出库单打印与模板</h1>
        <p class="function-page-desc">
          选择待打印出库单并套用模板，支持浏览器连续纸打印和模板字段自定义排版。
        </p>
      </div>
    </header>

    <nav class="receipt-tabs">
      <button :class="{ active: activeMode === 'print' }" @click="activeMode = 'print'">
        <span class="material-symbols-outlined">print</span>
        出库单打印
      </button>
      <button :class="{ active: activeMode === 'template' }" @click="activeMode = 'template'">
        <span class="material-symbols-outlined">dashboard_customize</span>
        模板设置
      </button>
    </nav>

  <div v-if="activeMode === 'print'" class="receipt-workspace">
    <section class="queue-panel">
      <header class="queue-head">
        <div>
          <h2>待打印出库单</h2>
          <p>选择单据后预览，浏览器打印按 241-1 连续纸尺寸分页。</p>
        </div>
        <button class="icon-btn" :disabled="isFetchingList" @click="fetchPendingList">
          <span class="material-symbols-outlined">refresh</span>
        </button>
      </header>

      <div class="queue-list">
        <div v-if="pendingOrders.length === 0 && !isFetchingList" class="empty-state">
          <span class="material-symbols-outlined">inventory_2</span>
          <p>当前暂无待打印出库单</p>
        </div>

        <button
          v-for="item in pendingOrders"
          :key="item.orderNo"
          class="queue-card"
          :class="{ active: selectedOrder?.orderNo === item.orderNo }"
          @click="selectOrder(item)"
        >
          <div class="queue-row">
            <strong>{{ item.orderNo }}</strong>
            <span>{{ formatDate(item.createTime) }}</span>
          </div>
          <p>客户：{{ item.customerName || '--' }}</p>
          <small>共 {{ item.itemCount || 0 }} 条，合计 {{ formatNumber(item.totalMeters) }} 米</small>
        </button>
      </div>
    </section>

    <section class="preview-panel">
      <header class="preview-head">
        <div>
          <h2>出库单打印预览</h2>
          <p v-if="selectedOrder">共 {{ printPages.length }} 页，每页都是完整出库单格式。</p>
          <p v-else>请选择左侧待打印任务。</p>
        </div>

        <div class="receipt-actions">
          <select v-if="selectedOrder" v-model="selectedTemplateId" class="template-select" @change="handleTemplateChange">
            <option v-for="template in receiptTemplates" :key="template.id" :value="template.id">
              {{ template.name }}{{ template.isDefault === 1 ? '（默认）' : '' }}
            </option>
          </select>
          <button v-if="selectedOrder" class="btn btn-print" :disabled="isPrinting" @click="openBrowserPrint">
            <span class="material-symbols-outlined">print</span>
            浏览器打印
          </button>
          <button v-if="selectedOrder" class="btn btn-cancel" :disabled="isSubmitting" @click="handleCancelPrint">
            <span class="material-symbols-outlined">block</span>
            作废/跳过
          </button>
          <button class="btn btn-success" :disabled="!selectedOrder || isSubmitting" @click="confirmPrinted">
            <span class="material-symbols-outlined">task_alt</span>
            确认已打印
          </button>
        </div>
      </header>

      <div class="preview-scroll">
        <div v-if="isLoadingDetail" class="loading-mask">
          <span class="material-symbols-outlined animate-spin">progress_activity</span>
          <p>正在生成打印预览...</p>
        </div>

        <div v-if="!selectedOrder" class="preview-empty">
          <span class="material-symbols-outlined">ads_click</span>
          <p>请在左侧选择一张出库单</p>
        </div>

        <div v-if="selectedOrder" id="print-paper-area" class="paper-stack">
          <article v-for="page in printPages" :key="page.pageNo" class="receipt-page" :style="paperStyle">
            <header class="receipt-top">
              <div class="receipt-title-block">
                <h1>{{ templateConfig.title }}</h1>
                <p>{{ templateConfig.subtitle }}</p>
              </div>
            </header>

            <section class="receipt-info">
              <div class="customer-line">
                <div>客户名称：<span>{{ printableText(selectedOrder.customerName) }}</span></div>
                <div>单据编号：<span>{{ selectedOrder.orderNo || '--' }}</span></div>
              </div>
              <div class="order-line">
                <span>项目名称：<b>{{ printableText(selectedOrder.projectName) }}</b></span>
                <span>录单日期：<b>{{ formatDateOnly(selectedOrder.createTime) }}</b></span>
                <span>制单人：<b>{{ selectedOrder.operator || '--' }}</b></span>
                <span>第 {{ page.pageNo }} 页 / 共 {{ page.totalPages }} 页</span>
              </div>
            </section>

            <table class="receipt-print-table">
              <thead>
                <tr>
                  <th v-for="column in visibleColumns" :key="column.key">{{ column.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIndex) in page.rows" :key="`${page.pageNo}-${rowIndex}`" class="data-row">
                  <td
                    v-for="column in visibleColumns"
                    :key="column.key"
                    :class="{ 'text-left': isTextColumn(column.key) }"
                  >
                    {{ row.id ? renderReceiptCell(row, column.key) : '' }}
                  </td>
                </tr>
                <tr class="total-row">
                  <td :colspan="visibleColumns.length">
                    <div class="total-line">
                      <span>合计大写：{{ page.pageNo === page.totalPages ? amountUpper(summary.totalAmount) : '' }}</span>
                      <span>合计数：{{ formatNumber(page.pageMeters) }}</span>
                      <span>小计：{{ money(page.pageAmount) }} 元</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <footer class="receipt-bottom">
              <div v-if="templateConfig.showLogistics" class="logistics-row">
                <span>物流公司：</span>
                <b></b>
                <span>物流单号：</span>
                <b></b>
              </div>
              <p v-if="templateConfig.notice" class="notice">{{ templateConfig.notice }}</p>
              <div v-if="templateConfig.showSignature" class="signature-row">
                <span>收货仓库：{{ templateConfig.warehouse || '成品仓库' }}</span>
                <span>送货人签字：____________</span>
                <span>收货人签字：____________</span>
              </div>
            </footer>
          </article>
        </div>

        <aside v-if="selectedOrder && printableRows.length" class="remark-editor">
          <div class="remark-editor-head">
            <strong>打印备注</strong>
            <span>每行可单独填写，不填则打印为空</span>
          </div>
          <div class="remark-editor-list">
            <label v-for="row in printableRows" :key="getRowKey(row)" class="remark-editor-row">
              <span>{{ row.modelCode || row.barcode || '未命名货物' }}</span>
              <input
                :value="printRemarkMap[getRowKey(row)] || ''"
                maxlength="30"
                placeholder="本行备注"
                @input="setRowRemark(row, $event.target.value)"
              />
            </label>
          </div>
        </aside>
      </div>
    </section>
  </div>

    <section v-else class="template-workspace">
      <div class="template-main-card">
        <header class="template-page-head">
          <div>
            <h2>出库单模板设置</h2>
            <p>这里维护打印模板；打印页面只负责选择模板和执行打印。</p>
          </div>
          <div class="template-actions">
            <select v-model="selectedTemplateId" class="template-select" @change="handleTemplateChange">
              <option v-for="template in receiptTemplates" :key="template.id" :value="template.id">
                {{ template.name }}{{ template.isDefault === 1 ? '（默认）' : '' }}
              </option>
            </select>
            <button class="btn btn-template" @click="createNewReceiptTemplate">
              <span class="material-symbols-outlined">add</span>
              新建模板
            </button>
            <button class="btn btn-print" :disabled="isTemplateSaving" @click="saveCurrentReceiptTemplate">
              <span class="material-symbols-outlined">save</span>
              保存为默认
            </button>
          </div>
        </header>

        <div class="template-designer-body">
          <div class="template-editor standalone">
            <div class="template-grid">
              <label>
                <span>模板名称</span>
                <input v-model.trim="templateDraftName" maxlength="30" placeholder="请输入模板名称" />
              </label>
              <label>
                <span>主标题</span>
                <input v-model.trim="templateConfig.title" maxlength="20" />
              </label>
              <label>
                <span>副标题</span>
                <input v-model.trim="templateConfig.subtitle" maxlength="20" />
              </label>
              <label>
                <span>每页行数</span>
                <input v-model.number="templateConfig.rowsPerPage" type="number" min="4" max="10" />
              </label>
              <label>
                <span>收货仓库</span>
                <input v-model.trim="templateConfig.warehouse" maxlength="20" />
              </label>
              <label class="template-check">
                <input v-model="templateConfig.showLogistics" type="checkbox" />
                <span>显示物流信息</span>
              </label>
              <label class="template-check">
                <input v-model="templateConfig.showSignature" type="checkbox" />
                <span>显示签字区</span>
              </label>
              <label class="template-wide">
                <span>底部提示语</span>
                <input v-model.trim="templateConfig.notice" maxlength="80" />
              </label>
            </div>

            <div v-if="receiptVariables.length" class="variable-strip">
              <span v-for="item in receiptVariables" :key="item.field">{{ item.label }}</span>
            </div>

            <div class="column-editor">
              <div class="column-editor-head">
                <strong>列名与排版</strong>
                <span>字段固定，支持改列名、显隐和调整顺序</span>
              </div>
              <div class="column-editor-list">
                <div v-for="(column, index) in templateConfig.columns" :key="column.key" class="column-editor-row">
                  <label class="column-visible">
                    <input v-model="column.visible" type="checkbox" />
                  </label>
                  <span class="column-field">{{ getColumnFieldName(column.key) }}</span>
                  <input v-model.trim="column.label" maxlength="12" />
                  <div class="column-move-actions">
                    <button :disabled="index === 0" @click="moveColumn(index, -1)">上移</button>
                    <button :disabled="index === templateConfig.columns.length - 1" @click="moveColumn(index, 1)">下移</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="template-live-preview">
            <div class="template-live-preview-head">
              <strong>实时预览</strong>
              <span>{{ templateConfig.paperWidthMm }}mm × {{ templateConfig.paperHeightMm }}mm</span>
            </div>
            <div class="template-preview-canvas">
              <article class="receipt-page preview-scale" :style="paperStyle">
                <header class="receipt-top">
                  <div class="receipt-title-block">
                    <h1>{{ templateConfig.title }}</h1>
                    <p>{{ templateConfig.subtitle }}</p>
                  </div>
                </header>

                <section class="receipt-info">
                  <div class="customer-line">
                    <div>客户名称：<span>示例客户</span></div>
                    <div>单据编号：<span>CK20260414001</span></div>
                  </div>
                  <div class="order-line">
                    <span>项目名称：<b>春季面料项目</b></span>
                    <span>录单日期：<b>2026-04-14</b></span>
                    <span>制单人：<b>仓库管理员</b></span>
                    <span>第 1 页 / 共 1 页</span>
                  </div>
                </section>

                <table class="receipt-print-table">
                  <thead>
                    <tr>
                      <th v-for="column in visibleColumns" :key="column.key">{{ column.label }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in templatePreviewRows" :key="index" class="data-row">
                      <td
                        v-for="column in visibleColumns"
                        :key="column.key"
                        :class="{ 'text-left': isTextColumn(column.key) }"
                      >
                        {{ renderPreviewCell(row, column.key) }}
                      </td>
                    </tr>
                    <tr class="total-row">
                      <td :colspan="visibleColumns.length">
                        <div class="total-line">
                          <span>合计大写：人民币 7855.05 元</span>
                          <span>合计数：206.50</span>
                          <span>小计：7855.05 元</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <footer class="receipt-bottom">
                  <div v-if="templateConfig.showLogistics" class="logistics-row">
                    <span>物流公司：</span>
                    <b></b>
                    <span>物流单号：</span>
                    <b></b>
                  </div>
                  <p v-if="templateConfig.notice" class="notice">{{ templateConfig.notice }}</p>
                  <div v-if="templateConfig.showSignature" class="signature-row">
                    <span>收货仓库：{{ templateConfig.warehouse || '成品仓库' }}</span>
                    <span>送货人签字：____________</span>
                    <span>收货人签字：____________</span>
                  </div>
                </footer>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelPrint,
  getPendingPrintOrders,
  getPrintDetail,
  listReceiptTemplateVariables,
  listReceiptTemplates,
  markPrinted,
  saveReceiptTemplate,
  setDefaultReceiptTemplate
} from './receipt/api/receipt.js'

const isFetchingList = ref(false)
const isLoadingDetail = ref(false)
const isPrinting = ref(false)
const isSubmitting = ref(false)
const isTemplateSaving = ref(false)
const activeMode = ref('print')
const pendingOrders = ref([])
const selectedOrder = ref(null)
const tableData = ref([])
const printRemarkMap = ref({})
const receiptTemplates = ref([])
const receiptVariables = ref([])
const selectedTemplateId = ref(null)
const templateDraftName = ref('系统默认出库单')
const templateConfig = ref(defaultTemplateConfig())

onMounted(async () => {
  await Promise.all([fetchPendingList(), fetchReceiptTemplates(), fetchReceiptVariables()])
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
    printRemarkMap.value = {}
  } finally {
    isLoadingDetail.value = false
  }
}

async function fetchReceiptTemplates() {
  const templates = await listReceiptTemplates()
  receiptTemplates.value = Array.isArray(templates) ? templates : []
  const defaultTemplate = receiptTemplates.value.find((item) => item.isDefault === 1) || receiptTemplates.value[0]
  if (defaultTemplate) {
    selectedTemplateId.value = defaultTemplate.id
    applyReceiptTemplate(defaultTemplate)
  }
}

async function fetchReceiptVariables() {
  const variables = await listReceiptTemplateVariables()
  receiptVariables.value = Array.isArray(variables) ? variables : []
}

function handleTemplateChange() {
  const template = receiptTemplates.value.find((item) => String(item.id) === String(selectedTemplateId.value))
  if (template) {
    applyReceiptTemplate(template)
  }
}

function applyReceiptTemplate(template) {
  templateDraftName.value = template.name || '自定义出库单模板'
  templateConfig.value = normalizeTemplateConfig({
    ...defaultTemplateConfig(),
    ...parseTemplateConfig(template.designJson || template.content)
  })
}

async function saveCurrentReceiptTemplate() {
  const name = templateDraftName.value || currentTemplateName()
  if (!name || !name.trim()) {
    ElMessage.warning('模板名称不能为空')
    return
  }

  isTemplateSaving.value = true
  try {
    const config = normalizeTemplateConfig(templateConfig.value)
    const designJson = JSON.stringify(config, null, 2)
    const saved = await saveReceiptTemplate({
      id: selectedTemplateId.value || undefined,
      name: name.trim(),
      printType: 'receipt',
      content: buildReceiptTemplateContent(config),
      designJson,
      widthMm: config.paperWidthMm,
      heightMm: config.paperHeightMm,
      isDefault: 1
    })
    await setDefaultReceiptTemplate(saved.id)
    ElMessage.success('出库单模板已保存并设为默认')
    await fetchReceiptTemplates()
    selectedTemplateId.value = saved.id
    applyReceiptTemplate(saved)
  } finally {
    isTemplateSaving.value = false
  }
}

function createNewReceiptTemplate() {
  selectedTemplateId.value = null
  templateDraftName.value = '自定义出库单模板'
  templateConfig.value = defaultTemplateConfig()
}

const summary = computed(() => ({
  totalMeters: tableData.value.reduce((sum, item) => sum + Number(item.meters || 0), 0),
  totalAmount: tableData.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
}))

const printPages = computed(() => {
  const sourceRows = tableData.value.length ? tableData.value : []
  const pages = []
  const rowsPerPage = activeRowsPerPage.value

  // 明细为空时也生成一张空白格式，避免打印窗口没有单据骨架。
  for (let start = 0; start < sourceRows.length || start === 0; start += rowsPerPage) {
    const rows = sourceRows.slice(start, start + rowsPerPage)
    while (rows.length < rowsPerPage) {
      rows.push(createBlankRow())
    }
    pages.push(rows)
    if (sourceRows.length === 0) break
  }

  return pages.map((rows, index) => ({
    pageNo: index + 1,
    totalPages: pages.length,
    rows,
    pageMeters: rows.reduce((sum, item) => sum + Number(item.meters || 0), 0),
    pageAmount: rows.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
  }))
})

const printableRows = computed(() => tableData.value.filter((row) => row.id))
const visibleColumns = computed(() => {
  const columns = Array.isArray(templateConfig.value.columns) ? templateConfig.value.columns : defaultReceiptColumns()
  const visible = columns.filter((column) => column.visible !== false)
  return visible.length ? visible : defaultReceiptColumns().filter((column) => column.visible !== false)
})
const templatePreviewRows = computed(() => {
  const examples = [
    { modelCode: '978-1-56915-43-9', spec: '160', meters: '120.50', price: '32.50', amount: '3916.25', remark: 'A区使用' },
    { modelCode: '978-0-12-65584-4', spec: '180', meters: '86.00', price: '45.80', amount: '3938.80', remark: '' }
  ]
  const rows = examples.slice(0, activeRowsPerPage.value)
  while (rows.length < activeRowsPerPage.value) {
    rows.push({ modelCode: '', spec: '', meters: '', price: '', amount: '', remark: '' })
  }
  return rows
})
const activeRowsPerPage = computed(() => Math.max(4, Math.min(10, Number(templateConfig.value.rowsPerPage || 7))))
const paperStyle = computed(() => ({
  width: `${Number(templateConfig.value.paperWidthMm || 215.9)}mm`,
  height: `${Number(templateConfig.value.paperHeightMm || 139.7)}mm`
}))

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

    printWindow.document.open()
    printWindow.document.write(buildPrintHtml(printable.innerHTML))
    printWindow.document.close()
    printWindow.document.title = `出库单_${selectedOrder.value.orderNo}`
    printWindow.focus()
    setTimeout(() => printWindow.print(), 300)
  } finally {
    isPrinting.value = false
  }
}

async function confirmPrinted() {
  if (!selectedOrder.value) return
  await ElMessageBox.confirm('确认这张出库单已经打印成功吗？确认后该单据会移出待打印队列。', '打印确认', {
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
  <title>出库单打印</title>
  <style>${printCss()}</style>
</head>
<body>${content}</body>
</html>`
}

function printCss() {
  const width = Number(templateConfig.value.paperWidthMm || 215.9)
  const height = Number(templateConfig.value.paperHeightMm || 139.7)
  return `
    @page { size: ${width}mm ${height}mm; margin: 0; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; background: #fff; color: #000; font-family: SimSun, "宋体", NSimSun, serif; font-weight: 600; text-rendering: geometricPrecision; -webkit-font-smoothing: none; }
    .paper-stack { display: block; }
    .receipt-page { width: ${width}mm; height: ${height}mm; padding: 6mm 8mm 5mm; page-break-after: always; break-after: page; overflow: hidden; position: relative; background: white; }
    .receipt-page:last-child { page-break-after: auto; break-after: auto; }
    .receipt-top { min-height: 20mm; display: flex; justify-content: center; align-items: flex-start; }
    .receipt-title-block { text-align: center; padding-top: 5mm; }
    .receipt-title-block h1 { margin: 0; font-size: 21px; letter-spacing: 8px; font-weight: 800; }
    .receipt-title-block p { margin: 3mm 0 0; font-size: 13px; letter-spacing: 4px; color: #000; font-weight: 700; }
    .receipt-info { margin-top: 2mm; margin-bottom: 2mm; font-size: 13px; line-height: 1.8; color: #000; font-weight: 700; }
    .customer-line, .order-line { display: flex; align-items: center; justify-content: space-between; gap: 5mm; }
    .customer-line span { display: inline-block; min-width: 36mm; border-bottom: 1.2px solid #000; text-align: center; min-height: 5mm; }
    .order-line b { font-weight: 800; }
    .receipt-print-table { width: 100%; border-collapse: collapse; table-layout: fixed; font-size: 12px; color: #000; }
    .receipt-print-table th, .receipt-print-table td { border: 1.2px solid #000; height: 7mm; text-align: center; vertical-align: middle; padding: 0 1.6mm; font-weight: 700; }
    .receipt-print-table th { height: 6.2mm; }
    .receipt-print-table .col-name { width: 30mm; }
    .receipt-print-table .col-remark { width: 36mm; }
    .receipt-print-table .text-left { text-align: left; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
    .receipt-print-table .total-row td { height: 6.8mm; font-weight: 700; }
    .total-line { display: flex; justify-content: space-between; gap: 6mm; }
    .receipt-bottom { margin-top: 3mm; font-size: 12px; color: #000; font-weight: 700; }
    .logistics-row { display: flex; align-items: center; gap: 6mm; margin-bottom: 2.2mm; }
    .logistics-row b { width: 31mm; border-bottom: 1.2px solid #000; height: 4mm; display: inline-block; }
    .notice { margin: 0 0 3mm; line-height: 1.5; letter-spacing: .8px; }
    .signature-row { display: flex; justify-content: space-between; font-size: 13px; }
    @media print { body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
  `
}

function createBlankRow() {
  return { id: '', modelCode: '', spec: '', meters: '', price: '', totalAmount: '', barcode: '', remark: '' }
}

function defaultReceiptColumns() {
  return [
    { key: 'modelCode', label: '货物名称', visible: true },
    { key: 'spec', label: '规格', visible: true },
    { key: 'meters', label: '数量/米', visible: true },
    { key: 'blank1', label: '数量/米', visible: true },
    { key: 'blank2', label: '数量/米', visible: true },
    { key: 'blank3', label: '数量/米', visible: true },
    { key: 'totalMeters', label: '总米数', visible: true },
    { key: 'price', label: '单价', visible: true },
    { key: 'amount', label: '金额', visible: true },
    { key: 'remark', label: '备注', visible: true }
  ]
}

function normalizeColumns(columns) {
  const defaults = defaultReceiptColumns()
  if (!Array.isArray(columns) || columns.length === 0) return defaults
  const sourceMap = new Map(columns.map((column) => [column.key, column]))
  const orderedKeys = columns.map((column) => column.key).filter((key) => defaults.some((item) => item.key === key))
  const missingKeys = defaults.map((item) => item.key).filter((key) => !orderedKeys.includes(key))
  return [...orderedKeys, ...missingKeys].map((key) => {
    const fallback = defaults.find((item) => item.key === key)
    const source = sourceMap.get(key) || {}
    return {
      key,
      label: source.label || fallback.label,
      visible: source.visible !== false
    }
  })
}

function moveColumn(index, direction) {
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= templateConfig.value.columns.length) return
  const columns = [...templateConfig.value.columns]
  const current = columns[index]
  columns[index] = columns[nextIndex]
  columns[nextIndex] = current
  templateConfig.value = { ...templateConfig.value, columns }
}

function getColumnFieldName(key) {
  const names = {
    modelCode: '货物名称字段',
    spec: '规格字段',
    meters: '米数字段',
    blank1: '空白数量列',
    blank2: '空白数量列',
    blank3: '空白数量列',
    totalMeters: '总米数字段',
    price: '单价字段',
    amount: '金额字段',
    remark: '行备注字段'
  }
  return names[key] || key
}

function isTextColumn(key) {
  return ['modelCode', 'remark'].includes(key)
}

function renderReceiptCell(row, key) {
  const map = {
    modelCode: row.modelCode || row.barcode || '',
    spec: row.spec || '',
    meters: formatNumber(row.meters),
    blank1: '',
    blank2: '',
    blank3: '',
    totalMeters: formatNumber(row.meters),
    price: money(row.price),
    amount: money(row.totalAmount),
    remark: getRowRemark(row)
  }
  return map[key] ?? ''
}

function renderPreviewCell(row, key) {
  const map = {
    modelCode: row.modelCode,
    spec: row.spec,
    meters: row.meters,
    blank1: '',
    blank2: '',
    blank3: '',
    totalMeters: row.meters,
    price: row.price,
    amount: row.amount,
    remark: row.remark
  }
  return map[key] ?? ''
}

function getRowKey(row) {
  return String(row.id || row.barcode || row.modelCode || '')
}

function getRowRemark(row) {
  return printRemarkMap.value[getRowKey(row)] || ''
}

function setRowRemark(row, value) {
  printRemarkMap.value = {
    ...printRemarkMap.value,
    [getRowKey(row)]: value.trim()
  }
}

function formatDate(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatDateOnly(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatNumber(value) {
  return Number(value || 0).toFixed(2)
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function amountUpper(value) {
  return `人民币 ${money(value)} 元`
}

function printableText(value) {
  const text = String(value || '').trim()
  if (!text || /^\?+$/.test(text)) return ''
  return text
}

function defaultTemplateConfig() {
  return {
    title: '面料销售码单',
    subtitle: '出库凭证',
    paperWidthMm: 215.9,
    paperHeightMm: 139.7,
    rowsPerPage: 7,
    warehouse: '成品仓库',
    notice: '请您与发货单核对本页货物，若有质量问题请在 15 天内告知；开剪后概不退换！感谢合作，共赢发展。',
    showLogistics: true,
    showSignature: true,
    columns: defaultReceiptColumns()
  }
}

function parseTemplateConfig(raw) {
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return typeof parsed === 'object' && parsed ? parsed : {}
  } catch {
    return {}
  }
}

function normalizeTemplateConfig(config) {
  const defaults = defaultTemplateConfig()
  return {
    ...defaults,
    ...config,
    title: config.title || defaults.title,
    subtitle: config.subtitle || defaults.subtitle,
    paperWidthMm: Number(config.paperWidthMm || defaults.paperWidthMm),
    paperHeightMm: Number(config.paperHeightMm || defaults.paperHeightMm),
    rowsPerPage: Math.max(4, Math.min(10, Number(config.rowsPerPage || defaults.rowsPerPage))),
    showLogistics: Boolean(config.showLogistics),
    showSignature: Boolean(config.showSignature),
    columns: normalizeColumns(config.columns)
  }
}

function currentTemplateName() {
  const template = receiptTemplates.value.find((item) => String(item.id) === String(selectedTemplateId.value))
  return template?.name || '自定义出库单模板'
}

function buildReceiptTemplateContent(config) {
  return [
    `${config.title} - ${config.subtitle}`,
    '单据编号：${orderNo}',
    '客户名称：${customerName}',
    '项目名称：${projectName}',
    '录单日期：${createDate}',
    '制单人：${operator}',
    `列配置：${config.columns.filter((column) => column.visible !== false).map((column) => `${column.label}(${column.key})`).join(' / ')}`,
    '明细字段：${modelCode} / ${spec} / ${meters} / ${price} / ${amount} / ${remark}',
    '本页：${pageNo}/${totalPages}，合计米数：${pageMeters}，小计：${pageAmount}，总金额：${totalAmount}'
  ].join('\n')
}
</script>

<style scoped>
.receipt-page-shell {
  min-height: calc(100vh - 5rem);
  background:
    radial-gradient(circle at 8% 0%, rgba(255, 196, 41, 0.18), transparent 32%),
    linear-gradient(180deg, #fffdf8 0%, #fffaf0 100%);
}

.receipt-hero {
  max-width: 1500px;
  margin: 0 auto;
  padding: 1rem 1rem 0;
}

.receipt-tabs {
  max-width: 1500px;
  margin: 0 auto;
  padding: 1rem 1rem 0;
  display: flex;
  gap: .75rem;
}

.receipt-tabs button {
  border: 1px solid #f0d48e;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #5f5a4e;
  padding: .7rem 1rem;
  display: inline-flex;
  align-items: center;
  gap: .4rem;
  font-weight: 900;
  cursor: pointer;
}

.receipt-tabs button.active {
  background: linear-gradient(135deg, #ffd43b 0%, #f5a400 58%, #f08a00 100%);
  border-color: #f5a400;
  color: #fff;
  box-shadow: 0 14px 30px rgba(245, 164, 0, 0.24);
}

.receipt-workspace {
  height: calc(100vh - 16rem);
  max-width: 1500px;
  margin: 0 auto;
  padding: 1rem;
  display: flex;
  gap: 1.25rem;
  color: #1f2937;
}

.template-workspace {
  max-width: 1500px;
  margin: 0 auto;
  padding: 1rem;
}

.template-main-card {
  border: 1px solid #e5e7eb;
  border-radius: 1.25rem;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 1px 4px rgb(15 23 42 / 5%);
}

.template-page-head {
  padding: 1.25rem;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.template-page-head h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
}

.template-page-head p {
  margin: .35rem 0 0;
  color: #64748b;
  font-size: .8rem;
}

.template-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: .65rem;
}

.queue-panel {
  width: 360px;
  flex-shrink: 0;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1.25rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 4px rgb(15 23 42 / 5%);
}

.queue-head {
  padding: 1.25rem;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.queue-head h2,
.preview-head h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
}

.queue-head p,
.preview-head p {
  margin: .35rem 0 0;
  color: #64748b;
  font-size: .75rem;
}

.icon-btn {
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: white;
  color: #64748b;
}

.queue-list {
  flex: 1;
  overflow: auto;
  background: #f8fafc;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: .75rem;
}

.queue-card {
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: #fff;
  padding: 1rem;
  text-align: left;
  transition: .2s;
  cursor: pointer;
}

.queue-card:hover,
.queue-card.active {
  border-color: #f5a400;
  box-shadow: 0 8px 24px rgb(69 95 136 / 12%);
  transform: translateY(-1px);
}

.queue-row {
  display: flex;
  justify-content: space-between;
  gap: .75rem;
  align-items: center;
}

.queue-row strong {
  font-family: Consolas, monospace;
  color: #1f2937;
}

.queue-row span,
.queue-card small {
  color: #64748b;
  font-size: .75rem;
}

.queue-card p {
  margin: .75rem 0 .4rem;
  color: #475569;
  font-size: .8rem;
  font-weight: 700;
}

.empty-state,
.preview-empty {
  height: 100%;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  gap: .75rem;
}

.empty-state .material-symbols-outlined,
.preview-empty .material-symbols-outlined {
  font-size: 3rem;
}

.preview-panel {
  flex: 1;
  min-width: 0;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1.25rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.preview-head {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.receipt-actions {
  display: flex;
  gap: .65rem;
  flex-wrap: wrap;
  align-items: center;
}

.template-select {
  height: 2.55rem;
  min-width: 180px;
  border: 1px solid #dbe3ef;
  border-radius: .8rem;
  padding: 0 .8rem;
  color: #334155;
  font-weight: 800;
  outline: none;
  background: white;
}

.template-select:focus {
  border-color: #f5a400;
  box-shadow: 0 0 0 3px rgb(69 95 136 / 12%);
}

.btn {
  border: 0;
  border-radius: .8rem;
  padding: .7rem 1rem;
  font-size: .82rem;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  cursor: pointer;
}

.btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.btn-print {
  background: #f5a400;
  color: white;
}

.btn-template {
  background: #f8fafc;
  color: #b56f00;
  border: 1px solid #dbe3ef;
}

.btn-cancel {
  background: #f1f5f9;
  color: #64748b;
}

.btn-success {
  background: #16a34a;
  color: white;
}

.preview-scroll {
  flex: 1;
  overflow: auto;
  background: #eef2f7;
  padding: 2rem;
  position: relative;
}

.loading-mask {
  position: absolute;
  inset: 0;
  background: rgb(255 255 255 / 72%);
  backdrop-filter: blur(4px);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #b56f00;
  font-weight: 900;
}

.loading-mask .material-symbols-outlined {
  font-size: 2.4rem;
}

.paper-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.remark-editor {
  width: min(215.9mm, 100%);
  margin: 1rem auto 0;
  padding: 1rem;
  border: 1px solid #dbe3ef;
  border-radius: 1rem;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 10px 28px rgb(15 23 42 / 10%);
}

.template-editor {
  padding-bottom: 1rem;
  margin-bottom: 1rem;
  border-bottom: 1px dashed #dbe3ef;
}

.template-editor.standalone {
  padding: 1.25rem;
  margin: 0;
  border-bottom: 0;
}

.template-designer-body {
  display: grid;
  grid-template-columns: minmax(360px, 520px) minmax(0, 1fr);
  gap: 1rem;
  padding: 1.25rem;
}

.template-designer-body .template-editor.standalone {
  padding: 0;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: .75rem;
}

.template-grid label {
  display: flex;
  flex-direction: column;
  gap: .35rem;
}

.template-grid label > span {
  color: #64748b;
  font-size: .75rem;
  font-weight: 900;
}

.template-grid input[type="text"],
.template-grid input[type="number"],
.template-grid label:not(.template-check) input {
  height: 2.3rem;
  border: 1px solid #dbe3ef;
  border-radius: .7rem;
  padding: 0 .75rem;
  color: #1f2937;
  outline: none;
}

.template-grid input:focus {
  border-color: #f5a400;
  box-shadow: 0 0 0 3px rgb(69 95 136 / 12%);
}

.template-wide {
  grid-column: span 2;
}

.template-check {
  flex-direction: row !important;
  align-items: center;
  justify-content: flex-start;
  padding-top: 1.35rem;
}

.template-check input {
  width: 1rem;
  height: 1rem;
}

.variable-strip {
  display: flex;
  gap: .45rem;
  flex-wrap: wrap;
  margin-top: .85rem;
}

.variable-strip span {
  border-radius: 999px;
  background: #eef2f7;
  color: #b56f00;
  padding: .28rem .6rem;
  font-size: .72rem;
  font-weight: 800;
}

.column-editor {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed #dbe3ef;
}

.column-editor-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: .75rem;
}

.column-editor-head strong {
  color: #1f2937;
  font-size: .95rem;
}

.column-editor-head span {
  color: #64748b;
  font-size: .74rem;
}

.column-editor-list {
  display: flex;
  flex-direction: column;
  gap: .5rem;
}

.column-editor-row {
  display: grid;
  grid-template-columns: 28px minmax(90px, 1fr) minmax(120px, 1.2fr) auto;
  gap: .5rem;
  align-items: center;
  padding: .5rem;
  border: 1px solid #e5e7eb;
  border-radius: .75rem;
  background: #fff;
}

.column-visible {
  display: flex;
  align-items: center;
  justify-content: center;
}

.column-visible input {
  width: 1rem;
  height: 1rem;
}

.column-field {
  color: #64748b;
  font-size: .72rem;
  font-weight: 900;
}

.column-editor-row > input {
  height: 2rem;
  border: 1px solid #dbe3ef;
  border-radius: .6rem;
  padding: 0 .65rem;
  color: #1f2937;
  outline: none;
}

.column-editor-row > input:focus {
  border-color: #f5a400;
  box-shadow: 0 0 0 3px rgb(69 95 136 / 12%);
}

.column-move-actions {
  display: flex;
  gap: .35rem;
}

.column-move-actions button {
  border: 1px solid #dbe3ef;
  border-radius: .55rem;
  background: #f8fafc;
  color: #b56f00;
  padding: .35rem .5rem;
  font-size: .72rem;
  font-weight: 900;
  cursor: pointer;
}

.column-move-actions button:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.template-live-preview {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: #f8fafc;
  overflow: hidden;
}

.template-live-preview-head {
  padding: .85rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-live-preview-head strong {
  color: #1f2937;
}

.template-live-preview-head span {
  color: #64748b;
  font-size: .78rem;
  font-weight: 800;
}

.template-preview-canvas {
  padding: 1.25rem;
  overflow: auto;
  background:
    linear-gradient(45deg, rgb(226 232 240 / 45%) 25%, transparent 25%),
    linear-gradient(-45deg, rgb(226 232 240 / 45%) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgb(226 232 240 / 45%) 75%),
    linear-gradient(-45deg, transparent 75%, rgb(226 232 240 / 45%) 75%);
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-size: 16px 16px;
}

.preview-scale {
  transform: scale(.68);
  transform-origin: top left;
  margin-right: -32%;
  margin-bottom: -17%;
  box-shadow: 0 16px 34px rgb(15 23 42 / 20%);
}

.remark-editor-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: .85rem;
}

.remark-editor-head strong {
  color: #1f2937;
  font-size: .95rem;
}

.remark-editor-head span {
  color: #64748b;
  font-size: .75rem;
}

.remark-editor-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: .7rem;
}

.remark-editor-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  align-items: center;
  gap: .65rem;
}

.remark-editor-row span {
  overflow: hidden;
  color: #475569;
  font-size: .78rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark-editor-row input {
  height: 2.25rem;
  border: 1px solid #dbe3ef;
  border-radius: .7rem;
  padding: 0 .8rem;
  color: #1f2937;
  outline: none;
}

.remark-editor-row input:focus {
  border-color: #f5a400;
  box-shadow: 0 0 0 3px rgb(69 95 136 / 12%);
}

.receipt-page {
  width: 215.9mm;
  height: 139.7mm;
  padding: 6mm 8mm 5mm;
  background: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 12px 30px rgb(15 23 42 / 18%);
  font-family: SimSun, "宋体", NSimSun, serif;
  color: #000;
  font-weight: 600;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: none;
}

.receipt-top {
  min-height: 20mm;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.receipt-title-block {
  text-align: center;
  padding-top: 5mm;
}

.receipt-title-block h1 {
  margin: 0;
  font-size: 21px;
  letter-spacing: 8px;
  font-weight: 800;
}

.receipt-title-block p {
  margin: 3mm 0 0;
  font-size: 13px;
  letter-spacing: 4px;
  color: #000;
  font-weight: 700;
}

.receipt-info {
  margin-top: 2mm;
  margin-bottom: 2mm;
  font-size: 13px;
  line-height: 1.8;
  color: #000;
  font-weight: 700;
}

.customer-line,
.order-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 5mm;
}

.customer-line span {
  display: inline-block;
  min-width: 36mm;
  border-bottom: 1.2px solid #000;
  text-align: center;
  min-height: 5mm;
}

.order-line b {
  font-weight: 800;
}

.receipt-print-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
  color: #000;
}

.receipt-print-table th,
.receipt-print-table td {
  border: 1.2px solid #000;
  height: 7mm;
  text-align: center;
  vertical-align: middle;
  padding: 0 1.6mm;
  font-weight: 700;
}

.receipt-print-table th {
  height: 6.2mm;
}

.receipt-print-table .col-name {
  width: 30mm;
}

.receipt-print-table .col-remark {
  width: 36mm;
}

.receipt-print-table .text-left {
  text-align: left;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.receipt-print-table .total-row td {
  height: 6.8mm;
  font-weight: 700;
}

.total-line {
  display: flex;
  justify-content: space-between;
  gap: 6mm;
}

.receipt-bottom {
  margin-top: 3mm;
  font-size: 12px;
  color: #000;
  font-weight: 700;
}

.logistics-row {
  display: flex;
  align-items: center;
  gap: 6mm;
  margin-bottom: 2.2mm;
}

.logistics-row b {
  width: 31mm;
  border-bottom: 1.2px solid #000;
  height: 4mm;
  display: inline-block;
}

.notice {
  margin: 0 0 3mm;
  line-height: 1.5;
  letter-spacing: .8px;
}

.signature-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .receipt-workspace {
    height: auto;
    flex-direction: column;
  }

  .queue-panel {
    width: 100%;
  }

  .remark-editor-list {
    grid-template-columns: 1fr;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }

  .template-wide {
    grid-column: span 1;
  }

  .template-designer-body {
    grid-template-columns: 1fr;
  }

  .preview-scale {
    transform: scale(.52);
    margin-right: -48%;
    margin-bottom: -28%;
  }
}
</style>
