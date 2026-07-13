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
        <button class="secondary-action" @click="showPrintProfile = !showPrintProfile">
          <span class="material-symbols-outlined">tune</span>
          打印适配
        </button>
        <button class="secondary-action" @click="printCalibrationPage">
          <span class="material-symbols-outlined">straighten</span>
          校准页
        </button>
        <button class="secondary-action" @click="openTemplateEditor">
          <span class="material-symbols-outlined">dashboard_customize</span>
          编辑模板
        </button>
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

    <section v-if="showPrintProfile" class="print-profile-panel">
      <div class="profile-intro">
        <strong>浏览器打印适配</strong>
        <span>不同打印机、热敏纸和驱动会有偏移，先打印校准页，再微调下面参数。</span>
      </div>
      <label>
        <span>纸宽(mm)</span>
        <input v-model.number="printProfile.paperWidthMm" type="number" min="20" max="500" step="0.1" @change="persistPrintProfile" />
      </label>
      <label>
        <span>纸高(mm)</span>
        <input v-model.number="printProfile.paperHeightMm" type="number" min="10" max="500" step="0.1" @change="persistPrintProfile" />
      </label>
      <label>
        <span>边距(mm)</span>
        <input v-model.number="printProfile.pageMarginMm" type="number" min="0" max="30" step="0.1" @change="persistPrintProfile" />
      </label>
      <label>
        <span>左右偏移(mm)</span>
        <input v-model.number="printProfile.offsetXmm" type="number" min="-50" max="50" step="0.1" @change="persistPrintProfile" />
      </label>
      <label>
        <span>上下偏移(mm)</span>
        <input v-model.number="printProfile.offsetYmm" type="number" min="-50" max="50" step="0.1" @change="persistPrintProfile" />
      </label>
      <label>
        <span>缩放</span>
        <input v-model.number="printProfile.scale" type="number" min="0.5" max="1.5" step="0.01" @change="persistPrintProfile" />
      </label>
      <button class="secondary-action compact" @click="syncProfileWithTemplate">使用模板尺寸</button>
      <button class="secondary-action compact" @click="resetCurrentPrintProfile">恢复默认</button>
    </section>

    <section v-if="showTemplateEditor" class="template-editor-panel">
      <div class="template-editor-head">
        <div>
          <strong>模板编辑</strong>
          <span>调整模板名称、纸张尺寸和标签展示内容，保存后预览/打印会立即按新模板展示。</span>
        </div>
        <button class="secondary-action compact" @click="showTemplateEditor = false">收起</button>
      </div>

      <div class="template-editor-grid">
        <label>
          <span>模板名称</span>
          <input v-model.trim="templateForm.name" class="business-input" placeholder="请输入模板名称" />
        </label>
        <label>
          <span>标签标题</span>
          <input v-model.trim="templateForm.title" class="business-input" placeholder="例如：布匹标签" />
        </label>
        <label>
          <span>纸宽(mm)</span>
          <input v-model.number="templateForm.widthMm" class="business-input" type="number" min="20" max="500" step="0.1" />
        </label>
        <label>
          <span>纸高(mm)</span>
          <input v-model.number="templateForm.heightMm" class="business-input" type="number" min="10" max="500" step="0.1" />
        </label>
      </div>

      <div class="template-switch-row">
        <label class="template-check-pill">
          <input v-model="templateForm.showBarcode" type="checkbox" />
          <span>显示条形码</span>
        </label>
        <label class="template-check-pill">
          <input v-model="templateForm.showQr" type="checkbox" />
          <span>显示二维码</span>
        </label>
        <label class="template-check-pill">
          <input v-model="templateForm.isDefault" type="checkbox" />
          <span>设为默认模板</span>
        </label>
      </div>

      <div class="template-field-editor">
        <div v-for="field in templateForm.fields" :key="field.key" class="template-field-row">
          <label class="template-field-visible">
            <input v-model="field.visible" type="checkbox" />
            <span>{{ templateFieldDisplayName(field) }}</span>
          </label>
          <input v-model.trim="field.label" class="business-input" placeholder="显示名称" />
        </div>
      </div>

      <div class="template-actions">
        <button class="secondary-action compact" :disabled="savingTemplate" @click="hydrateTemplateEditor">恢复当前模板</button>
        <button class="primary-action" :disabled="savingTemplate" @click="saveCurrentTemplate">
          <span class="material-symbols-outlined" :class="{ 'animate-spin': savingTemplate }">save</span>
          保存模板
        </button>
      </div>
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
          <div
            v-if="printTarget"
            ref="printAreaRef"
            class="thermal-label"
            :class="{ 'thermal-label--qr': showQrCode && qrDataUrl, 'thermal-label--barcode': showBarcode && barcodeValue }"
            :style="labelStyle"
          >
            <div class="label-business-title">{{ labelTitle }}</div>
            <div class="label-main-grid">
              <div class="label-info-list">
                <div v-for="row in businessRows" :key="row.label" class="label-info-row">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.value }}</strong>
                </div>
              </div>
              <div v-if="showQrCode && qrDataUrl" class="qr-box">
                <img :src="qrDataUrl" alt="业务二维码" />
                <span>扫码流转</span>
              </div>
            </div>
            <div v-if="showBarcode && barcodeValue" class="barcode-box">
              <div class="barcode-svg" v-html="barcodeSvg"></div>
              <span>{{ barcodeValue }}</span>
            </div>
          </div>
          <div v-else class="preview-empty">
            <span class="material-symbols-outlined">ads_click</span>
            <p>请选择左侧记录后预览热敏标签</p>
          </div>
        </div>

        <div v-if="invalidOrderFlowQr" class="qr-invalid-warning" role="alert">
          <span class="material-symbols-outlined">error</span>
          <p>流转二维码格式无效，无法生成二维码</p>
        </div>

        <div v-if="printTarget" class="print-tips">
          <span class="material-symbols-outlined">info</span>
          <p>浏览器打印时请选择热敏打印机，纸张尺寸设为当前标签尺寸，并关闭浏览器“页眉和页脚”。打印窗口关闭后需要确认结果，确认成功后系统才会回写打印任务状态。</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import JsBarcode from 'jsbarcode'
import {
  PRINT_PROFILE_KEYS,
  buildPrintTransformCss,
  injectPrintPageStyle,
  loadPrintProfile,
  normalizePrintProfile,
  openCalibrationPrint,
  resetPrintProfile,
  savePrintProfile
} from '@/utils/printProfile'
import { listLabelTemplateVariables, listLabelTemplates, saveLabelTemplate } from './label/api/label'
import { getPendingPrintTaskCount, listPendingPrintTasks, reportPrintTask } from './label/api/printTask'
import { getEquipmentPage } from './equipment/api/equipment'
import { selectOrderFlowQrValue } from './order/orderFlow.js'

defineOptions({ name: 'LabelPrintCenter' })

const PRINT_SUCCESS = 1
const PRINT_FAILED = 2
const PRINT_TYPE_KEYS = ['label', 'order_flow', 'equipment_inspection']

const printTabs = ref([
  { key: 'label', label: '布匹标签', icon: 'sell', count: 0 },
  { key: 'order_flow', label: '订单流转码', icon: 'qr_code_2', count: 0 },
  { key: 'equipment_inspection', label: '设备巡检码', icon: 'qr_code_scanner', count: 0 }
])

const activeTab = ref('label')
const loading = ref(false)
const pendingTasks = ref([])
const templates = ref([])
const templateVariables = ref([])
const selectedTaskNo = ref('')
const selectedTemplateId = ref('')
const selectedEquipmentKey = ref('')
const equipmentKeyword = ref('')
const equipmentList = ref([])
const equipmentTotal = ref(0)
const printAreaRef = ref(null)
const qrDataUrl = ref('')
const barcodeSvg = ref('')
const pendingPrintTaskNo = ref('')
const showPrintProfile = ref(false)
const showTemplateEditor = ref(false)
const savingTemplate = ref(false)

const TEMPLATE_FIELD_LABEL_MAP = {
  barcode: '条码',
  barCode: '条码',
  clothCode: '布匹编号',
  labelQrPayload: '布匹二维码',
  clothQrPayload: '布匹二维码',
  inventoryQrPayload: '布匹二维码',
  modelCode: '型号',
  model: '型号',
  meters: '米数',
  remainingMeters: '剩余米数',
  totalMeters: '总米数',
  spec: '规格',
  batchNo: '批次',
  inboundTime: '入库时间',
  inTime: '入库时间',
  customerName: '客户',
  orderId: '订单号',
  orderNo: '订单号',
  orderTypeLabel: '订单类型',
  orderCategoryLabel: '订单小项',
  currentStatusText: '当前状态',
  statusText: '当前状态',
  projectName: '项目',
  brandName: '品牌',
  flowBarcode: '流转条码',
  flowQrPayload: '流转二维码',
  flowScanCode: '流转码',
  flowCode: '流转编号',
  equipmentCode: '设备编号',
  inspectionQrPayload: '巡检二维码',
  equipmentName: '设备名称',
  equipmentType: '设备类型',
  location: '位置',
  areaName: '位置',
  responsiblePerson: '负责人',
  inspectionCycleDays: '巡检周期'
}

const templateForm = ref(createEmptyTemplateForm())

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
const activeTemplateDesign = computed(() => normalizeTemplateDesign(
  showTemplateEditor.value ? templateForm.value : parseTemplateDesign(selectedTemplate.value),
  activeTab.value
))

const printTarget = computed(() => {
  if (activeTab.value === 'equipment_inspection') {
    return selectedEquipment.value ? buildEquipmentPayload(selectedEquipment.value) : null
  }
  return selectedTask.value ? normalizeTaskPayload(selectedTask.value) : null
})

const labelSize = computed(() => ({
  width: Number(activeTemplateDesign.value.widthMm || selectedTemplate.value?.widthMm || (activeTab.value === 'label' ? 70 : 60)),
  height: Number(activeTemplateDesign.value.heightMm || selectedTemplate.value?.heightMm || (activeTab.value === 'label' ? 50 : 40))
}))

const printProfile = ref(loadPrintProfile(PRINT_PROFILE_KEYS.LABEL, {
  paperWidthMm: labelSize.value.width,
  paperHeightMm: labelSize.value.height
}))

const effectivePrintProfile = computed(() => normalizePrintProfile({
  ...printProfile.value,
  paperWidthMm: printProfile.value.paperWidthMm || labelSize.value.width,
  paperHeightMm: printProfile.value.paperHeightMm || labelSize.value.height
}))

const labelStyle = computed(() => ({
  width: `${labelSize.value.width}mm`,
  height: `${labelSize.value.height}mm`
}))

const labelTitle = computed(() => {
  if (activeTemplateDesign.value.title) return activeTemplateDesign.value.title
  if (activeTab.value === 'order_flow') return '订单流转码'
  if (activeTab.value === 'equipment_inspection') return '设备巡检码'
  return '布匹标签'
})

const showBarcode = computed(() => activeTemplateDesign.value.showBarcode !== false)
const showQrCode = computed(() => activeTemplateDesign.value.showQr !== false)
const maxBusinessRows = computed(() => {
  if (showQrCode.value && showBarcode.value) return labelSize.value.height <= 40 ? 5 : 6
  if (showQrCode.value) return labelSize.value.height <= 40 ? 5 : 6
  return labelSize.value.height <= 40 ? 5 : 7
})

const barcodeValue = computed(() => {
  const target = printTarget.value || {}
  if (activeTab.value === 'order_flow') {
    return selectOrderFlowQrValue(target.flowBarcode, target.flowScanCode, target.flowQrPayload, target.barcode)
  }
  return safeText(target.barcode || target.flowBarcode || target.flowScanCode || target.equipmentCode || target.clothCode)
})

const qrValue = computed(() => {
  const target = printTarget.value || {}
  if (activeTab.value === 'equipment_inspection') {
    return safeText(target.inspectionQrPayload || target.equipmentCode)
  }
  if (activeTab.value === 'order_flow') {
    return selectOrderFlowQrValue(target.flowQrPayload, target.flowScanCode, target.flowBarcode)
  }
  const barcode = target.barcode || target.barCode || target.clothCode || target.bizNo
  return safeText(target.labelQrPayload || target.clothQrPayload || target.inventoryQrPayload || buildInventoryQrPayload(barcode))
})
const invalidOrderFlowQr = computed(() => activeTab.value === 'order_flow' && Boolean(printTarget.value) && !qrValue.value)

const availableBusinessRows = computed(() => {
  const target = printTarget.value || {}
  if (activeTab.value === 'order_flow') {
    return compactRows([
      ['orderId', '订单号', target.orderId || target.orderNo || target.bizNo],
      ['orderCategoryLabel', '订单小项', target.orderCategoryLabel || target.orderCategoryName],
      ['customerName', '客户', target.customerName],
      ['projectName', '项目', target.projectName],
      ['brandName', '品牌', target.brandName],
      ['currentStatusText', '当前状态', target.currentStatusText || target.statusText]
    ])
  }
  if (activeTab.value === 'equipment_inspection') {
    return compactRows([
      ['equipmentCode', '设备编号', target.equipmentCode],
      ['equipmentName', '设备名称', target.equipmentName],
      ['location', '位置', target.location || target.areaName],
      ['responsiblePerson', '负责人', target.responsiblePerson],
      ['inspectionCycleDays', '巡检周期', target.inspectionCycleDays ? `${target.inspectionCycleDays} 天` : '']
    ])
  }
  return compactRows([
    ['barcode', '条码', target.barcode || target.barCode || target.clothCode],
    ['modelCode', '型号', target.modelCode || target.model],
    ['meters', '米数', formatNumber(target.meters || target.remainingMeters || target.totalMeters)],
    ['spec', '规格', target.spec],
    ['batchNo', '批次', target.batchNo],
    ['inboundTime', '入库时间', formatDate(target.inboundTime || target.inTime || target.createTime)],
    ['customerName', '客户', target.customerName]
  ])
})

const businessRows = computed(() => {
  const rowsByKey = new Map(availableBusinessRows.value.map((row) => [row.key, row]))
  const visibleFields = activeTemplateDesign.value.fields.filter((field) => field.visible !== false)
  const configuredRows = visibleFields
    .map((field) => {
      const row = rowsByKey.get(field.key)
      if (!row || !row.value) return null
      return {
        ...row,
        label: templateFieldDisplayName({ key: field.key, label: field.label || row.label })
      }
    })
    .filter(Boolean)
  if (configuredRows.length > 0) {
    return configuredRows.slice(0, maxBusinessRows.value)
  }
  return availableBusinessRows.value.slice(0, maxBusinessRows.value)
})

watch([printTarget, barcodeValue, qrValue, activeTemplateDesign], () => refreshPreview(), { deep: true })

watch([selectedTemplateId, templateVariables], () => {
  if (showTemplateEditor.value) hydrateTemplateEditor()
}, { deep: true })

onMounted(async () => {
  await loadPrintOverviewCounts()
  await switchTab('label')
  applyLabelPrintStyle()
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
  await loadPrintOverviewCounts()
  await Promise.all([
    loadTemplates(),
    loadTemplateVariables(),
    tabKey === 'equipment_inspection' ? loadEquipmentList() : loadPendingTasks()
  ])
  hydrateTemplateEditor()
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

async function loadTemplateVariables() {
  try {
    templateVariables.value = await listLabelTemplateVariables({ printType: activeTab.value })
  } catch (error) {
    templateVariables.value = []
  }
}

async function loadPrintOverviewCounts() {
  try {
    const counts = await getPendingPrintTaskCount(PRINT_TYPE_KEYS)
    PRINT_TYPE_KEYS.forEach((key) => updateTabCount(key, Number(counts?.[key] || 0)))
  } catch (error) {
    // 数量只是页面提示，失败时不阻塞打印主流程。
  }
  try {
    const equipmentPage = await getEquipmentPage({ pageNum: 1, pageSize: 1 })
    equipmentTotal.value = Number(equipmentPage?.total || equipmentPage?.totalCount || equipmentPage?.records?.length || equipmentPage?.data?.length || 0)
    updateTabCount('equipment_inspection', equipmentTotal.value)
  } catch (error) {
    // 数量只是页面提示，失败时不阻塞打印主流程。
  }
}

async function loadPendingTasks() {
  loading.value = true
  try {
    pendingTasks.value = await listPendingPrintTasks({ printType: activeTab.value, limit: 50 })
    selectedTaskNo.value = pendingTasks.value[0]?.taskNo || ''
    await loadPrintOverviewCounts()
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
    equipmentTotal.value = Number(page?.total || equipmentList.value.length || 0)
    selectedEquipmentKey.value = equipmentList.value[0] ? equipmentKey(equipmentList.value[0]) : ''
    updateTabCount('equipment_inspection', equipmentTotal.value)
  } finally {
    loading.value = false
  }
}

function updateTabCount(key, count) {
  const tab = printTabs.value.find((item) => item.key === key)
  if (tab) tab.count = Number(count || 0)
}

function openTemplateEditor() {
  hydrateTemplateEditor()
  showTemplateEditor.value = true
}

function hydrateTemplateEditor() {
  templateForm.value = normalizeTemplateDesign(parseTemplateDesign(selectedTemplate.value), activeTab.value)
}

async function saveCurrentTemplate() {
  const form = normalizeTemplateDesign(templateForm.value, activeTab.value)
  if (!form.name) {
    ElMessage.warning('请填写模板名称')
    return
  }
  savingTemplate.value = true
  try {
    const saved = await saveLabelTemplate({
      id: selectedTemplate.value?.id || null,
      name: form.name,
      printType: activeTab.value,
      content: buildTemplateContent(form),
      designJson: JSON.stringify(form),
      widthMm: form.widthMm,
      heightMm: form.heightMm,
      isDefault: form.isDefault ? 1 : 0
    })
    ElMessage.success('模板已保存')
    await loadTemplates()
    if (saved?.id) {
      selectedTemplateId.value = String(saved.id)
    }
    hydrateTemplateEditor()
    await refreshPreview()
  } finally {
    savingTemplate.value = false
  }
}

function createEmptyTemplateForm() {
  return normalizeTemplateDesign({}, 'label')
}

function parseTemplateDesign(template = null) {
  const base = {
    id: template?.id || null,
    name: template?.name || defaultTemplateName(activeTab.value),
    widthMm: Number(template?.widthMm || (activeTab.value === 'label' ? 70 : 60)),
    heightMm: Number(template?.heightMm || (activeTab.value === 'label' ? 50 : 40)),
    isDefault: Number(template?.isDefault || 0) === 1
  }
  if (!template?.designJson) {
    return base
  }
  try {
    const parsed = JSON.parse(template.designJson)
    return {
      ...base,
      ...parsed,
      name: template?.name || parsed.name || base.name,
      widthMm: Number(template?.widthMm || parsed.widthMm || base.widthMm),
      heightMm: Number(template?.heightMm || parsed.heightMm || base.heightMm),
      isDefault: Number(template?.isDefault || parsed.isDefault || 0) === 1
    }
  } catch (error) {
    return base
  }
}

function normalizeTemplateDesign(value = {}, printType = activeTab.value) {
  const defaults = defaultTemplateDesign(printType)
  const sourceFields = Array.isArray(value.fields) ? value.fields : []
  const fieldsByKey = new Map(sourceFields.map((field) => [field.key, field]))
  const fields = defaultTemplateFields(printType).map((field) => ({
    key: field.key,
    label: templateFieldDisplayName({ key: field.key, label: fieldsByKey.get(field.key)?.label || field.label }),
    visible: fieldsByKey.has(field.key) ? fieldsByKey.get(field.key)?.visible !== false : field.visible !== false
  }))
  return {
    ...defaults,
    ...value,
    id: value.id || null,
    name: safeText(value.name) || defaults.name,
    title: safeText(value.title) || defaults.title,
    widthMm: normalizeNumber(value.widthMm, defaults.widthMm),
    heightMm: normalizeNumber(value.heightMm, defaults.heightMm),
    showBarcode: value.showBarcode === undefined ? defaults.showBarcode : value.showBarcode !== false,
    showQr: value.showQr === undefined ? defaults.showQr : value.showQr !== false,
    isDefault: Boolean(value.isDefault),
    fields
  }
}

function defaultTemplateDesign(printType = activeTab.value) {
  return {
    name: defaultTemplateName(printType),
    title: defaultTemplateTitle(printType),
    widthMm: printType === 'label' ? 70 : 60,
    heightMm: printType === 'label' ? 50 : 40,
    showBarcode: true,
    showQr: printType !== 'label',
    isDefault: false,
    fields: defaultTemplateFields(printType)
  }
}

function defaultTemplateFields(printType = activeTab.value) {
  const variables = templateVariables.value.length > 0 ? templateVariables.value : fallbackTemplateVariables(printType)
  return variables
    .filter((item) => item.type !== 'barcode' && item.type !== 'qrcode')
    .map((item, index) => ({
      key: item.field,
      label: templateFieldDisplayName({ key: item.field, label: item.name }),
      visible: index < 7
    }))
}

function templateFieldDisplayName(field) {
  const key = typeof field === 'string' ? field : field?.key
  const label = typeof field === 'string' ? '' : field?.label
  const safeLabel = safeText(label)
  if (safeLabel && !isInternalTemplateFieldName(safeLabel, key)) {
    return safeLabel
  }
  return TEMPLATE_FIELD_LABEL_MAP[key] || safeLabel || key || '信息项'
}

function isInternalTemplateFieldName(label, key) {
  if (!label) return false
  if (key && label === key) return true
  return /^[A-Za-z][A-Za-z0-9_]*$/.test(label)
}

function fallbackTemplateVariables(printType = activeTab.value) {
  if (printType === 'order_flow') {
    return [
      { name: '订单号', field: 'orderId', type: 'text' },
      { name: '订单小项', field: 'orderCategoryLabel', type: 'text' },
      { name: '客户', field: 'customerName', type: 'text' },
      { name: '项目', field: 'projectName', type: 'text' },
      { name: '品牌', field: 'brandName', type: 'text' },
      { name: '当前状态', field: 'currentStatusText', type: 'text' }
    ]
  }
  if (printType === 'equipment_inspection') {
    return [
      { name: '设备编号', field: 'equipmentCode', type: 'text' },
      { name: '设备名称', field: 'equipmentName', type: 'text' },
      { name: '位置', field: 'location', type: 'text' },
      { name: '负责人', field: 'responsiblePerson', type: 'text' },
      { name: '巡检周期', field: 'inspectionCycleDays', type: 'text' }
    ]
  }
  return [
    { name: '条码', field: 'barcode', type: 'text' },
    { name: '型号', field: 'modelCode', type: 'text' },
    { name: '米数', field: 'meters', type: 'text' },
    { name: '规格', field: 'spec', type: 'text' },
    { name: '批次', field: 'batchNo', type: 'text' },
    { name: '入库时间', field: 'inboundTime', type: 'text' },
    { name: '客户', field: 'customerName', type: 'text' }
  ]
}

function defaultTemplateName(printType = activeTab.value) {
  if (printType === 'order_flow') return '订单流转码模板'
  if (printType === 'equipment_inspection') return '设备巡检码模板'
  return '布匹标签模板'
}

function defaultTemplateTitle(printType = activeTab.value) {
  if (printType === 'order_flow') return '订单流转码'
  if (printType === 'equipment_inspection') return '设备巡检码'
  return '布匹标签'
}

function normalizeNumber(value, fallback) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? Number(numberValue.toFixed(2)) : fallback
}

function buildTemplateContent(form) {
  const barcodeField = primaryBarcodeField(activeTab.value)
  const qrField = primaryQrField(activeTab.value)
  const lines = [
    `SIZE ${form.widthMm} mm,${form.heightMm} mm`,
    'GAP 2 mm,0 mm',
    'DIRECTION 1',
    'CLS',
    `TEXT 24,18,"TSS24.BF2",0,1,1,"${escapeTemplateText(form.title)}"`
  ]
  let y = 54
  form.fields
    .filter((field) => field.visible !== false)
    .slice(0, 7)
    .forEach((field) => {
      lines.push(`TEXT 24,${y},"TSS24.BF2",0,1,1,"${escapeTemplateText(templateFieldDisplayName(field))}: \${${field.key}}"`)
      y += 34
    })
  if (form.showBarcode && barcodeField) {
    lines.push(`BARCODE 24,${Math.max(y, 88)},"128",54,1,0,2,2,"\${${barcodeField}}"`)
  }
  if (form.showQr && qrField) {
    lines.push(`QRCODE 330,36,L,5,A,0,M2,S7,"\${${qrField}}"`)
  }
  lines.push('PRINT 1,1')
  return lines.join('\r\n')
}

function primaryBarcodeField(printType = activeTab.value) {
  if (printType === 'order_flow') return 'flowBarcode'
  if (printType === 'equipment_inspection') return 'equipmentCode'
  return 'barcode'
}

function primaryQrField(printType = activeTab.value) {
  if (printType === 'order_flow') return 'flowQrPayload'
  if (printType === 'equipment_inspection') return 'inspectionQrPayload'
  return 'labelQrPayload'
}

function escapeTemplateText(value) {
  return safeText(value).replaceAll('"', "'")
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
  if (invalidOrderFlowQr.value) {
    ElMessage.warning('流转二维码格式无效，无法预览或打印')
    return
  }
  await refreshPreview()
  persistPrintProfile()
  pendingPrintTaskNo.value = activeTab.value === 'equipment_inspection' ? '' : selectedTask.value?.taskNo || ''
  await nextTick()
  if (!openCurrentLabelPrintWindow()) {
    pendingPrintTaskNo.value = ''
    ElMessage.error('浏览器拦截了打印窗口，请允许弹窗后重试')
  }
}

async function handleAfterPrint() {
  const taskNo = pendingPrintTaskNo.value
  pendingPrintTaskNo.value = ''
  if (!taskNo) return
  try {
    await ElMessageBox.confirm('请确认纸张已经正常打印出来。只有确认成功后，系统才会把任务标记为已打印。', '打印结果确认', {
      confirmButtonText: '打印成功',
      cancelButtonText: '未打印成功',
      distinguishCancelAndClose: true,
      type: 'info'
    })
    await reportPrintTask({
      taskNo,
      status: PRINT_SUCCESS,
      printChannel: 'browser_thermal',
      deviceName: '浏览器热敏打印',
      errorMessage: ''
    })
    ElMessage.success('已记录打印完成')
    await loadPendingTasks()
  } catch (action) {
    if (action !== 'cancel') return
    await reportPrintTask({
      taskNo,
      status: PRINT_FAILED,
      printChannel: 'browser_thermal',
      deviceName: '浏览器热敏打印',
      errorMessage: '浏览器打印状态回写失败'
    }).catch(() => {})
  }
}

function persistPrintProfile() {
  printProfile.value = savePrintProfile(PRINT_PROFILE_KEYS.LABEL, effectivePrintProfile.value)
  applyLabelPrintStyle()
}

function syncProfileWithTemplate() {
  printProfile.value = savePrintProfile(PRINT_PROFILE_KEYS.LABEL, {
    ...effectivePrintProfile.value,
    paperWidthMm: labelSize.value.width,
    paperHeightMm: labelSize.value.height
  })
  applyLabelPrintStyle()
}

function resetCurrentPrintProfile() {
  printProfile.value = resetPrintProfile(PRINT_PROFILE_KEYS.LABEL, {
    paperWidthMm: labelSize.value.width,
    paperHeightMm: labelSize.value.height
  })
  applyLabelPrintStyle()
}

function printCalibrationPage() {
  persistPrintProfile()
  if (!openCalibrationPrint(effectivePrintProfile.value, '标签打印校准页')) {
    ElMessage.error('浏览器拦截了打印窗口，请允许弹窗后重试')
  }
}

function applyLabelPrintStyle() {
  const profile = effectivePrintProfile.value
  injectPrintPageStyle('hive-label-print-profile', `
    @media print {
      @page {
        size: ${profile.paperWidthMm}mm ${profile.paperHeightMm}mm;
        margin: ${profile.pageMarginMm}mm;
      }
      .thermal-label {
        width: ${labelSize.value.width}mm !important;
        height: ${labelSize.value.height}mm !important;
        transform: ${buildPrintTransformCss(profile)} !important;
        transform-origin: top left !important;
      }
    }
  `)
}

function openCurrentLabelPrintWindow() {
  const labelNode = printAreaRef.value
  if (!labelNode) return false
  const printWindow = window.open('about:blank', '_blank', 'width=520,height=640')
  if (!printWindow) return false
  const profile = effectivePrintProfile.value
  printWindow.document.open()
  printWindow.document.write(buildLabelPrintHtml(labelNode.outerHTML, profile))
  printWindow.document.close()
  printWindow.focus()
  let handled = false
  const finishPrint = () => {
    if (handled) return
    handled = true
    try {
      printWindow.close()
    } catch (error) {
      // 关闭打印预览窗口失败不影响打印结果确认。
    }
    handleAfterPrint()
  }
  printWindow.addEventListener('afterprint', finishPrint)
  setTimeout(() => {
    printWindow.focus()
    printWindow.print()
    setTimeout(finishPrint, 300)
  }, 250)
  return true
}

function buildLabelPrintHtml(labelHtml, profile) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title></title>
  <style>${labelPrintCss(profile)}</style>
</head>
<body>
  <div class="label-print-sheet">${labelHtml}</div>
</body>
</html>`
}

function labelPrintCss(profile) {
  const normalized = normalizePrintProfile(profile)
  const hasQr = Boolean(showQrCode.value && qrDataUrl.value)
  const qrImageSize = labelSize.value.height <= 40 ? 17.5 : 21
  const labelPadding = labelSize.value.height <= 40 ? 2 : 3
  const barcodeHeight = hasQr ? 7 : 11
  const barcodeSvgHeight = hasQr ? 5.5 : 8.5
  return `
    @page { size: ${normalized.paperWidthMm}mm ${normalized.paperHeightMm}mm; margin: ${normalized.pageMarginMm}mm; }
    * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    html, body {
      width: ${normalized.paperWidthMm}mm;
      min-height: ${normalized.paperHeightMm}mm;
      margin: 0;
      padding: 0;
      background: #fff;
      color: #000;
      font-family: Arial, "Microsoft YaHei", sans-serif;
      overflow: hidden;
    }
    .label-print-sheet {
      width: ${normalized.paperWidthMm}mm;
      height: ${normalized.paperHeightMm}mm;
      position: relative;
      overflow: hidden;
      background: #fff;
    }
    .thermal-label {
      width: ${labelSize.value.width}mm !important;
      height: ${labelSize.value.height}mm !important;
      min-height: 0 !important;
      margin: 0 !important;
      padding: ${labelPadding}mm !important;
      border: 0.35mm solid #000 !important;
      box-shadow: none !important;
      color: #000 !important;
      background: #fff !important;
      display: flex !important;
      flex-direction: column !important;
      gap: ${hasQr ? 1 : 1.5}mm !important;
      overflow: hidden !important;
      transform: ${buildPrintTransformCss(normalized)} !important;
      transform-origin: top left !important;
    }
    .label-business-title {
      flex: 0 0 auto;
      border-bottom: 0.3mm solid #000;
      padding-bottom: 0.8mm;
      font-size: ${showQrCode.value ? 3.2 : 4}mm;
      font-weight: 900;
      line-height: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .label-main-grid {
      flex: 1 1 auto;
      min-height: 0;
      display: grid;
      grid-template-columns: minmax(0, 1fr) ${hasQr ? `${qrImageSize + 1}mm` : '0'};
      gap: ${hasQr ? 1.4 : 0}mm;
      align-items: start;
    }
    .label-info-list {
      min-width: 0;
      min-height: 0;
      display: grid;
      gap: ${hasQr ? 0.45 : 0.9}mm;
      overflow: hidden;
    }
    .label-info-row {
      min-width: 0;
      display: grid;
      grid-template-columns: ${hasQr ? 13 : 15}mm minmax(0, 1fr);
      gap: 1mm;
      align-items: baseline;
      font-size: ${hasQr ? 2.45 : 2.9}mm;
      line-height: 1.1;
    }
    .label-info-row span {
      color: #000;
      font-weight: 800;
      white-space: nowrap;
    }
    .label-info-row strong {
      min-width: 0;
      color: #000;
      font-weight: 900;
      word-break: break-all;
      overflow: hidden;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: ${hasQr ? 2 : 1};
    }
    .qr-box {
      display: ${hasQr ? 'grid' : 'none'};
      gap: 0.5mm;
      justify-items: center;
      align-self: start;
      color: #000;
      font-size: 1.9mm;
      font-weight: 900;
      line-height: 1;
    }
    .qr-box img {
      width: ${qrImageSize}mm;
      height: ${qrImageSize}mm;
      display: block;
    }
    .barcode-box {
      flex: 0 0 auto;
      height: ${barcodeHeight}mm;
      display: grid;
      gap: 0.4mm;
      justify-items: stretch;
      margin-top: 0 !important;
      overflow: hidden;
    }
    .barcode-svg {
      height: ${barcodeSvgHeight}mm;
      overflow: hidden;
    }
    .barcode-svg svg {
      width: 100%;
      height: ${barcodeSvgHeight}mm;
      display: block;
    }
    .barcode-box span {
      text-align: center;
      color: #000;
      font-size: ${hasQr ? 1.8 : 2.4}mm;
      font-weight: 900;
      line-height: 1;
      letter-spacing: 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  `
}

function normalizeTaskPayload(task = {}) {
  const payload = task.printPayload || {}
  const barcode = payload.barcode || payload.barCode || payload.clothCode || task.bizNo
  const labelQrPayload = payload.labelQrPayload || payload.clothQrPayload || payload.inventoryQrPayload || buildInventoryQrPayload(barcode)
  return {
    ...payload,
    taskNo: task.taskNo,
    bizNo: task.bizNo,
    barcode,
    barCode: barcode,
    clothCode: barcode,
    labelQrPayload,
    clothQrPayload: labelQrPayload,
    inventoryQrPayload: labelQrPayload,
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
    .map(([key, label, value]) => ({ key, label, value: safeText(value) }))
    .filter((row) => row.value)
}

function compactText(values) {
  return values.map((value) => safeText(value)).filter(Boolean).join(' · ') || '--'
}

function buildInventoryQrPayload(barcode) {
  const code = safeText(barcode)
  if (!code) return ''
  return JSON.stringify({
    version: '1',
    codeType: 'inventory_barcode',
    barcode: code
  })
}

function safeText(value) {
  if (value === 0) return '0'
  if (value === undefined || value === null) return ''
  return String(value).trim()
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
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

.print-profile-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(6, minmax(110px, 1fr)) auto auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border: 1px solid rgba(31, 63, 95, 0.14);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.profile-intro {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #0b1f33;
}

.profile-intro strong {
  font-size: 15px;
  font-weight: 1000;
}

.profile-intro span,
.print-profile-panel label span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.print-profile-panel label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.print-profile-panel input {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  padding: 0 10px;
  color: #0f172a;
  font-weight: 800;
  background: #fff;
}

.template-editor-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(31, 63, 95, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.07);
}

.template-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.template-editor-head strong {
  display: block;
  color: #0b1f33;
  font-size: 16px;
  font-weight: 1000;
}

.template-editor-head span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.template-editor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.template-editor-grid label,
.template-field-row {
  min-width: 0;
}

.template-editor-grid label > span {
  display: block;
  margin-bottom: 6px;
  color: #52657b;
  font-size: 12px;
  font-weight: 900;
}

.template-switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.template-check-pill {
  border: 1px solid rgba(31, 63, 95, 0.14);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 14px;
  color: #1f3f5f;
  background: #f8fbff;
  font-weight: 900;
}

.template-field-editor {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.template-field-row {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px;
  background: #fff;
}

.template-field-visible {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #52657b;
  font-size: 12px;
  font-weight: 900;
}

.template-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
  box-sizing: border-box;
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

.thermal-label--qr {
  gap: 1mm;
  padding: 2mm;
}

.label-business-title {
  flex: 0 0 auto;
  border-bottom: 1px solid #111827;
  padding-bottom: 2mm;
  font-size: 5mm;
  font-weight: 1000;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.thermal-label--qr .label-business-title {
  padding-bottom: 0.8mm;
  font-size: 3.2mm;
}

.label-main-grid {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 24mm;
  gap: 3mm;
  align-items: start;
}

.thermal-label--qr .label-main-grid {
  grid-template-columns: minmax(0, 1fr) 18.5mm;
  gap: 1.4mm;
}

.label-info-list {
  min-width: 0;
  min-height: 0;
  display: grid;
  gap: 1.5mm;
  overflow: hidden;
}

.thermal-label--qr .label-info-list {
  gap: 0.45mm;
}

.label-info-row {
  min-width: 0;
  display: grid;
  grid-template-columns: 16mm minmax(0, 1fr);
  gap: 2mm;
  align-items: baseline;
  font-size: 3.2mm;
  line-height: 1.25;
}

.thermal-label--qr .label-info-row {
  grid-template-columns: 13mm minmax(0, 1fr);
  gap: 1mm;
  font-size: 2.45mm;
  line-height: 1.1;
}

.label-info-row span {
  color: #4b5563;
  font-weight: 800;
  white-space: nowrap;
}

.label-info-row strong {
  min-width: 0;
  color: #111827;
  font-weight: 1000;
  word-break: break-all;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.thermal-label--qr .label-info-row strong {
  -webkit-line-clamp: 2;
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

.thermal-label--qr .qr-box {
  gap: 0.5mm;
  font-size: 1.9mm;
  line-height: 1;
}

.thermal-label--qr .qr-box img {
  width: 17.5mm;
  height: 17.5mm;
}

.barcode-box {
  flex: 0 0 auto;
  height: 12mm;
  display: grid;
  gap: 1mm;
  justify-items: stretch;
  margin-top: auto;
  overflow: hidden;
}

.thermal-label--qr .barcode-box {
  height: 7mm;
  gap: 0.4mm;
  margin-top: 0;
}

.barcode-svg {
  height: 12mm;
  overflow: hidden;
}

.thermal-label--qr .barcode-svg {
  height: 5.5mm;
}

.barcode-svg :deep(svg) {
  width: 100%;
  height: 12mm;
}

.thermal-label--qr .barcode-svg :deep(svg) {
  height: 5.5mm;
}

.barcode-box span {
  text-align: center;
  font-size: 3mm;
  font-weight: 900;
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.thermal-label--qr .barcode-box span {
  font-size: 1.8mm;
  line-height: 1;
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

.qr-invalid-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 22px;
  color: #991b1b;
  background: #fef2f2;
  border-top: 1px solid #fecaca;
  font-size: 13px;
  font-weight: 800;
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
  .print-workbench,
  .print-profile-panel,
  .template-editor-grid,
  .template-field-editor {
    grid-template-columns: 1fr;
  }

  .label-print-header {
    align-items: stretch;
    flex-direction: column;
  }

  .template-field-row {
    grid-template-columns: 1fr;
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
