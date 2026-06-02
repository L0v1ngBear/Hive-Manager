<template>
  <div class="label-print-page">
    <header class="label-print-header">
      <div>
        <div class="function-page-eyebrow">
          <span class="material-symbols-outlined">print</span>
          标签打印中心
        </div>
        <h1 class="function-page-title">网页热敏标签打印</h1>
        <p class="function-page-desc">布匹标签、订单流转码、设备巡检码统一在这里预览和打印。</p>
      </div>
      <div class="header-actions">
        <button class="secondary-action" :disabled="loading" @click="reloadCurrentTab">
          <span class="material-symbols-outlined" :class="{ 'animate-spin': loading }">sync</span>
          刷新
        </button>
        <button class="primary-action" :disabled="!printTarget" @click="printCurrentLabel">
          <span class="material-symbols-outlined">local_printshop</span>
          浏览器打印
        </button>
      </div>
    </header>

    <section class="print-tabs">
      <button
        v-for="tab in printTabs"
        :key="tab.key"
        type="button"
        class="print-tab"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <span class="material-symbols-outlined">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
        <strong>{{ tab.count }}</strong>
      </button>
    </section>

    <main class="print-workbench">
      <aside class="task-panel">
        <div class="panel-title-row">
          <div>
            <h2>{{ activeMeta.taskTitle }}</h2>
            <p>{{ activeMeta.taskDesc }}</p>
          </div>
        </div>

        <template v-if="activeTab === 'equipment_inspection'">
          <div class="search-row">
            <input v-model.trim="equipmentKeyword" class="business-input" placeholder="搜索设备名称或编号" @keyup.enter="loadEquipmentList" />
            <button class="secondary-action compact" @click="loadEquipmentList">搜索</button>
          </div>
          <div class="task-list">
            <button
              v-for="equipment in equipmentList"
              :key="equipment.id || equipment.equipmentCode"
              type="button"
              class="task-card"
              :class="{ active: selectedEquipmentKey === equipmentKey(equipment) }"
              @click="selectEquipment(equipment)"
            >
              <span class="task-name">{{ equipment.equipmentName || equipment.equipmentCode || '设备' }}</span>
              <span class="task-meta">{{ equipment.equipmentCode || '--' }} · {{ equipment.location || equipment.areaName || '未设置位置' }}</span>
            </button>
            <div v-if="!loading && equipmentList.length === 0" class="empty-state">暂无设备数据</div>
          </div>
        </template>

        <template v-else>
          <div class="task-list">
            <button
              v-for="task in pendingTasks"
              :key="task.taskNo"
              type="button"
              class="task-card"
              :class="{ active: selectedTaskNo === task.taskNo }"
              @click="selectTask(task)"
            >
              <span class="task-name">{{ taskTitle(task) }}</span>
              <span class="task-meta">{{ taskSubtitle(task) }}</span>
              <span v-if="task.retryCount" class="retry-badge">重试 {{ task.retryCount }}</span>
            </button>
            <div v-if="!loading && pendingTasks.length === 0" class="empty-state">暂无待打印任务</div>
          </div>
        </template>
      </aside>

      <section class="preview-panel">
        <div class="preview-head">
          <div>
            <h2>打印预览</h2>
            <p>{{ printTarget ? `${labelSize.width}mm × ${labelSize.height}mm` : '请选择一条业务记录' }}</p>
          </div>
          <div class="template-select">
            <label>打印模板</label>
            <select v-model="selectedTemplateId" @change="refreshPreview">
              <option v-for="template in templates" :key="template.id || template.name" :value="String(template.id || '')">
                {{ template.name || '默认模板' }}
              </option>
            </select>
          </div>
        </div>

        <div class="preview-stage">
          <div v-if="printTarget" ref="printAreaRef" class="thermal-label" :style="labelStyle">
            <div class="label-business-title">{{ labelTitle }}</div>
            <div class="label-main-grid">
              <div class="label-info-list">
                <div v-for="row in businessRows" :key="row.label" class="label-info-row">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.value }}</strong>
                </div>
              </div>
              <div v-if="qrDataUrl" class="qr-box">
                <img :src="qrDataUrl" alt="业务二维码" />
                <span>扫码流转</span>
              </div>
            </div>
            <div v-if="barcodeValue" class="barcode-box">
              <div class="barcode-svg" v-html="barcodeSvg"></div>
              <span>{{ barcodeValue }}</span>
            </div>
          </div>
          <div v-else class="preview-empty">
            <span class="material-symbols-outlined">ads_click</span>
            <p>请选择左侧记录后预览热敏标签</p>
          </div>
        </div>

        <div v-if="printTarget" class="print-tips">
          <span class="material-symbols-outlined">info</span>
          <p>浏览器打印时请选择热敏打印机，并把纸张尺寸设置为当前标签尺寸。打印成功后系统会自动回写打印任务状态。</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import JsBarcode from 'jsbarcode'
import { listLabelTemplates } from './label/api/label'
import { listPendingPrintTasks, reportPrintTask } from './label/api/printTask'
import { getEquipmentPage } from './equipment/api/equipment'

defineOptions({ name: 'LabelPrintCenter' })

const PRINT_SUCCESS = 1
const PRINT_FAILED = 2

const printTabs = ref([
  { key: 'label', label: '布匹标签', icon: 'sell', count: 0 },
  { key: 'order_flow', label: '订单流转码', icon: 'qr_code_2', count: 0 },
  { key: 'equipment_inspection', label: '设备巡检码', icon: 'qr_code_scanner', count: 0 }
])

const activeTab = ref('label')
const loading = ref(false)
const pendingTasks = ref([])
const templates = ref([])
const selectedTaskNo = ref('')
const selectedTemplateId = ref('')
const selectedEquipmentKey = ref('')
const equipmentKeyword = ref('')
const equipmentList = ref([])
const printAreaRef = ref(null)
const qrDataUrl = ref('')
const barcodeSvg = ref('')
const pendingPrintTaskNo = ref('')

const activeMeta = computed(() => {
  if (activeTab.value === 'order_flow') {
    return {
      taskTitle: '待打印订单流转码',
      taskDesc: '订单审核通过后自动生成，员工扫码即可更新订单流转状态。'
    }
  }
  if (activeTab.value === 'equipment_inspection') {
    return {
      taskTitle: '设备巡检固定码',
      taskDesc: '选择设备后打印固定巡检码，贴到设备上长期使用。'
    }
  }
  return {
    taskTitle: '待打印布匹标签',
    taskDesc: '库存入库、补打标签生成的标签任务会集中展示在这里。'
  }
})

const selectedTask = computed(() => pendingTasks.value.find((task) => task.taskNo === selectedTaskNo.value) || null)
const selectedEquipment = computed(() => equipmentList.value.find((item) => equipmentKey(item) === selectedEquipmentKey.value) || null)
const selectedTemplate = computed(() => templates.value.find((item) => String(item.id || '') === selectedTemplateId.value) || templates.value[0] || null)

const printTarget = computed(() => {
  if (activeTab.value === 'equipment_inspection') {
    return selectedEquipment.value ? buildEquipmentPayload(selectedEquipment.value) : null
  }
  return selectedTask.value ? normalizeTaskPayload(selectedTask.value) : null
})

const labelSize = computed(() => ({
  width: Number(selectedTemplate.value?.widthMm || (activeTab.value === 'label' ? 70 : 60)),
  height: Number(selectedTemplate.value?.heightMm || (activeTab.value === 'label' ? 50 : 40))
}))

const labelStyle = computed(() => ({
  width: `${labelSize.value.width}mm`,
  minHeight: `${labelSize.value.height}mm`
}))

const labelTitle = computed(() => {
  if (activeTab.value === 'order_flow') return '订单流转码'
  if (activeTab.value === 'equipment_inspection') return '设备巡检码'
  return '布匹标签'
})

const barcodeValue = computed(() => {
  const target = printTarget.value || {}
  return safeText(target.barcode || target.flowBarcode || target.flowScanCode || target.equipmentCode || target.clothCode)
})

const qrValue = computed(() => {
  const target = printTarget.value || {}
  if (activeTab.value === 'equipment_inspection') {
    return safeText(target.inspectionQrPayload || target.equipmentCode)
  }
  if (activeTab.value === 'order_flow') {
    return safeText(target.flowQrPayload || target.flowScanCode || target.flowBarcode || target.orderId)
  }
  return ''
})

const businessRows = computed(() => {
  const target = printTarget.value || {}
  if (activeTab.value === 'order_flow') {
    return compactRows([
      ['订单号', target.orderId || target.orderNo || target.bizNo],
      ['订单小项', target.orderCategoryLabel || target.orderCategoryName],
      ['客户', target.customerName],
      ['项目', target.projectName],
      ['品牌', target.brandName],
      ['当前状态', target.currentStatusText || target.statusText]
    ])
  }
  if (activeTab.value === 'equipment_inspection') {
    return compactRows([
      ['设备编号', target.equipmentCode],
      ['设备名称', target.equipmentName],
      ['位置', target.location || target.areaName],
      ['负责人', target.responsiblePerson],
      ['巡检周期', target.inspectionCycleDays ? `${target.inspectionCycleDays} 天` : '']
    ])
  }
  return compactRows([
    ['条码', target.barcode || target.barCode || target.clothCode],
    ['型号', target.modelCode || target.model],
    ['米数', formatNumber(target.meters || target.remainingMeters || target.totalMeters)],
    ['规格', target.spec],
    ['批次', target.batchNo],
    ['入库时间', formatDate(target.inboundTime || target.inTime || target.createTime)],
    ['客户', target.customerName]
  ])
})

watch([printTarget, barcodeValue, qrValue], () => refreshPreview(), { deep: true })

onMounted(async () => {
  await switchTab('label')
  window.addEventListener('afterprint', handleAfterPrint)
})

onUnmounted(() => {
  window.removeEventListener('afterprint', handleAfterPrint)
})

async function switchTab(tabKey) {
  activeTab.value = tabKey
  selectedTaskNo.value = ''
  selectedEquipmentKey.value = ''
  pendingPrintTaskNo.value = ''
  await Promise.all([loadTemplates(), tabKey === 'equipment_inspection' ? loadEquipmentList() : loadPendingTasks()])
}

async function reloadCurrentTab() {
  await switchTab(activeTab.value)
}

async function loadTemplates() {
  try {
    templates.value = await listLabelTemplates({ printType: activeTab.value })
    selectedTemplateId.value = String((templates.value.find((item) => Number(item.isDefault) === 1) || templates.value[0])?.id || '')
  } catch (error) {
    templates.value = []
    selectedTemplateId.value = ''
  }
}

async function loadPendingTasks() {
  loading.value = true
  try {
    pendingTasks.value = await listPendingPrintTasks({ printType: activeTab.value, limit: 50 })
    selectedTaskNo.value = pendingTasks.value[0]?.taskNo || ''
    updateTabCount(activeTab.value, pendingTasks.value.length)
  } finally {
    loading.value = false
  }
}

async function loadEquipmentList() {
  loading.value = true
  try {
    const page = await getEquipmentPage({
      pageNum: 1,
      pageSize: 50,
      keyword: equipmentKeyword.value || undefined
    })
    equipmentList.value = page?.data || page?.records || []
    selectedEquipmentKey.value = equipmentList.value[0] ? equipmentKey(equipmentList.value[0]) : ''
    updateTabCount('equipment_inspection', equipmentList.value.length)
  } finally {
    loading.value = false
  }
}

function updateTabCount(key, count) {
  const tab = printTabs.value.find((item) => item.key === key)
  if (tab) tab.count = Number(count || 0)
}

function selectTask(task) {
  selectedTaskNo.value = task.taskNo
}

function selectEquipment(equipment) {
  selectedEquipmentKey.value = equipmentKey(equipment)
}

function equipmentKey(equipment = {}) {
  return String(equipment.id || equipment.equipmentCode || equipment.equipmentName || '')
}

async function refreshPreview() {
  await nextTick()
  renderBarcode()
  await renderQrCode()
}

function renderBarcode() {
  const value = barcodeValue.value
  if (!value) {
    barcodeSvg.value = ''
    return
  }
  try {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    JsBarcode(svg, value, {
      format: 'CODE128',
      displayValue: false,
      margin: 0,
      width: 1.4,
      height: 48
    })
    barcodeSvg.value = svg.outerHTML
  } catch (error) {
    barcodeSvg.value = ''
  }
}

async function renderQrCode() {
  const value = qrValue.value
  if (!value) {
    qrDataUrl.value = ''
    return
  }
  try {
    qrDataUrl.value = await QRCode.toDataURL(value, {
      margin: 1,
      width: 112,
      errorCorrectionLevel: 'M'
    })
  } catch (error) {
    qrDataUrl.value = ''
  }
}

async function printCurrentLabel() {
  if (!printTarget.value) {
    ElMessage.warning('请选择需要打印的业务记录')
    return
  }
  await refreshPreview()
  pendingPrintTaskNo.value = activeTab.value === 'equipment_inspection' ? '' : selectedTask.value?.taskNo || ''
  await nextTick()
  window.print()
}

async function handleAfterPrint() {
  const taskNo = pendingPrintTaskNo.value
  pendingPrintTaskNo.value = ''
  if (!taskNo) return
  try {
    await reportPrintTask({
      taskNo,
      status: PRINT_SUCCESS,
      printChannel: 'browser_thermal',
      deviceName: '浏览器热敏打印',
      errorMessage: ''
    })
    ElMessage.success('已记录打印完成')
    await loadPendingTasks()
  } catch (error) {
    await reportPrintTask({
      taskNo,
      status: PRINT_FAILED,
      printChannel: 'browser_thermal',
      deviceName: '浏览器热敏打印',
      errorMessage: '浏览器打印状态回写失败'
    }).catch(() => {})
  }
}

function normalizeTaskPayload(task = {}) {
  const payload = task.printPayload || {}
  return {
    ...payload,
    taskNo: task.taskNo,
    bizNo: task.bizNo,
    barcode: payload.barcode || payload.barCode || payload.clothCode || task.bizNo,
    orderId: payload.orderId || payload.orderNo || task.bizNo,
    printDate: formatDate(new Date())
  }
}

function buildEquipmentPayload(equipment = {}) {
  const equipmentCode = safeText(equipment.equipmentCode)
  return {
    ...equipment,
    equipmentCode,
    inspectionQrPayload: equipment.inspectionQrPayload || (equipmentCode ? `HIVE_EQUIPMENT:${equipmentCode}` : ''),
    inspectionCycleDays: equipment.inspectionCycleDays || 7
  }
}

function taskTitle(task = {}) {
  const payload = task.printPayload || {}
  if (activeTab.value === 'order_flow') {
    return payload.orderId || payload.orderNo || task.bizNo || task.taskNo
  }
  return payload.barcode || payload.barCode || payload.clothCode || task.bizNo || task.taskNo
}

function taskSubtitle(task = {}) {
  const payload = task.printPayload || {}
  if (activeTab.value === 'order_flow') {
    return compactText([payload.customerName, payload.orderCategoryLabel || payload.orderCategoryName, payload.currentStatusText])
  }
  return compactText([payload.modelCode || payload.model, formatNumber(payload.meters || payload.remainingMeters), payload.spec])
}

function compactRows(rows) {
  return rows
    .map(([label, value]) => ({ label, value: safeText(value) }))
    .filter((row) => row.value)
    .slice(0, 7)
}

function compactText(values) {
  return values.map((value) => safeText(value)).filter(Boolean).join(' · ') || '--'
}

function safeText(value) {
  if (value === 0) return '0'
  if (value === undefined || value === null) return ''
  return String(value).trim()
}

function formatNumber(value) {
  const text = safeText(value)
  if (!text) return ''
  const numberValue = Number(text)
  if (!Number.isFinite(numberValue)) return text
  return `${Number(numberValue.toFixed(2))} 米`
}

function formatDate(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped>
.label-print-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: clamp(18px, 2vw, 32px);
  background:
    radial-gradient(circle at 12% -6%, rgba(31, 63, 95, 0.14), transparent 34%),
    linear-gradient(180deg, #f7f9fc 0%, #ffffff 100%);
}

.label-print-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.primary-action,
.secondary-action {
  border: 1px solid rgba(31, 63, 95, 0.16);
  border-radius: 18px;
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 18px;
  font-weight: 900;
  transition: 0.2s ease;
}

.primary-action {
  color: #fff;
  background: linear-gradient(135deg, #0b1f33 0%, #1f3f5f 100%);
  box-shadow: 0 18px 36px rgba(15, 31, 51, 0.18);
}

.secondary-action {
  color: #1f3f5f;
  background: rgba(255, 255, 255, 0.86);
}

.secondary-action.compact {
  min-height: 42px;
  border-radius: 14px;
}

.primary-action:disabled,
.secondary-action:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.print-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.print-tab {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 22px;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  padding: 0 18px;
  color: #52657b;
  background: rgba(255, 255, 255, 0.82);
  font-weight: 900;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.print-tab strong {
  margin-left: auto;
  min-width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #1f3f5f;
  background: #e8eef6;
}

.print-tab.active {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, #0b1f33 0%, #1f3f5f 100%);
}

.print-tab.active strong {
  color: #0b1f33;
  background: #fff;
}

.print-workbench {
  min-height: 0;
  flex: 1;
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 18px;
}

.task-panel,
.preview-panel {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.task-panel {
  display: flex;
  flex-direction: column;
}

.panel-title-row,
.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.panel-title-row h2,
.preview-head h2 {
  color: #0b1f33;
  font-size: 18px;
  font-weight: 1000;
}

.panel-title-row p,
.preview-head p {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.search-row {
  display: flex;
  gap: 10px;
  padding: 18px 18px 0;
}

.business-input,
.template-select select {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  min-height: 42px;
  padding: 0 14px;
  color: #0f172a;
  background: #f7f9fc;
  outline: none;
}

.template-select {
  min-width: 220px;
}

.template-select label {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.task-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
}

.task-card {
  position: relative;
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  padding: 16px;
  color: #475569;
  background: #fff;
  text-align: left;
}

.task-card.active {
  border-color: rgba(31, 63, 95, 0.42);
  background: #edf4fb;
  box-shadow: inset 4px 0 0 #1f3f5f;
}

.task-name {
  color: #0b1f33;
  font-size: 15px;
  font-weight: 1000;
  word-break: break-all;
}

.task-meta {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.retry-badge {
  position: absolute;
  right: 12px;
  top: 12px;
  border-radius: 999px;
  padding: 2px 8px;
  color: #b45309;
  background: #fef3c7;
  font-size: 11px;
  font-weight: 900;
}

.preview-panel {
  display: flex;
  flex-direction: column;
}

.preview-stage {
  flex: 1;
  min-height: 420px;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    linear-gradient(90deg, rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(rgba(148, 163, 184, 0.12) 1px, transparent 1px);
  background-size: 18px 18px;
}

.thermal-label {
  color: #111827;
  background: #fff;
  border: 1px solid #111827;
  display: flex;
  flex-direction: column;
  gap: 3mm;
  padding: 4mm;
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.20);
  overflow: hidden;
}

.label-business-title {
  border-bottom: 1px solid #111827;
  padding-bottom: 2mm;
  font-size: 5mm;
  font-weight: 1000;
  line-height: 1;
}

.label-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 24mm;
  gap: 3mm;
  align-items: start;
}

.label-info-list {
  min-width: 0;
  display: grid;
  gap: 1.5mm;
}

.label-info-row {
  display: grid;
  grid-template-columns: 16mm minmax(0, 1fr);
  gap: 2mm;
  align-items: baseline;
  font-size: 3.2mm;
  line-height: 1.25;
}

.label-info-row span {
  color: #4b5563;
  font-weight: 800;
}

.label-info-row strong {
  color: #111827;
  font-weight: 1000;
  word-break: break-all;
}

.qr-box {
  display: grid;
  gap: 1mm;
  justify-items: center;
  font-size: 2.4mm;
  font-weight: 900;
}

.qr-box img {
  width: 22mm;
  height: 22mm;
}

.barcode-box {
  display: grid;
  gap: 1mm;
  justify-items: stretch;
  margin-top: auto;
}

.barcode-svg {
  height: 12mm;
  overflow: hidden;
}

.barcode-svg :deep(svg) {
  width: 100%;
  height: 12mm;
}

.barcode-box span {
  text-align: center;
  font-size: 3mm;
  font-weight: 900;
  letter-spacing: 0.2mm;
  word-break: break-all;
}

.preview-empty,
.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  padding: 42px 16px;
  color: #94a3b8;
  font-weight: 800;
  text-align: center;
}

.preview-empty .material-symbols-outlined {
  font-size: 54px;
}

.print-tips {
  display: flex;
  gap: 10px;
  padding: 16px 22px;
  color: #52657b;
  background: #f8fafc;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .print-tabs,
  .print-workbench {
    grid-template-columns: 1fr;
  }

  .label-print-header {
    align-items: stretch;
    flex-direction: column;
  }
}

@media print {
  body * {
    visibility: hidden !important;
  }

  .thermal-label,
  .thermal-label * {
    visibility: visible !important;
  }

  .thermal-label {
    position: fixed;
    left: 0;
    top: 0;
    margin: 0;
    box-shadow: none;
    page-break-inside: avoid;
  }

  @page {
    size: 70mm 50mm;
    margin: 0;
  }
}
</style>
