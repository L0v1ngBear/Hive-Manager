<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header"><div><h1 class="function-page-title">{{ scopeMeta.title }}</h1><p class="function-page-desc">{{ scopeMeta.desc }}</p></div><el-button v-permission="'badproduct:save'" type="primary" @click="openCreate">{{ scopeMeta.createText }}</el-button></header>
      <section><el-button v-for="scope in scopeOptions" :key="scope.value" :type="activeScope === scope.value ? 'primary' : 'default'" @click="handleScopeChange(scope.value)">{{ scope.tabTitle }}</el-button></section>
      <section class="bg-surface-container-lowest">
        <el-form :model="query" class="p-5 flex flex-wrap gap-3"><el-form-item label="Search"><el-input v-model.trim="query.keyword" @keyup.enter="handleFilter" /></el-form-item><el-form-item label="Status"><el-select v-model="query.status" clearable><el-option label="Pending" value="pending" /><el-option label="In review" value="pending_audit" /><el-option label="Processed" value="processed" /></el-select></el-form-item><el-form-item label="Type"><el-select v-model="query.type" clearable><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="Date"><DateFilterInput v-model="query.date" /></el-form-item><el-button type="primary" @click="handleFilter">Search</el-button><el-button @click="resetFilter">Reset</el-button><TableColumnSettings :columns="badProductTableColumns" export-module="badproduct" @move="moveBadProductTableColumn" @reset="resetBadProductTableColumns" /></el-form>
        <el-table v-loading="loading" :data="rows" row-key="defectiveId" @row-click="openDetail">
          <el-table-column v-for="column in badProductTableColumns" :key="column.key" :label="column.label" min-width="130"><template #default="{ row: item }"><template v-if="column.key === 'defectiveId'">{{ item.defectiveId }}</template><template v-else-if="column.key === 'orderId'">{{ item.orderId }}</template><template v-else-if="column.key === 'type'">{{ typeLabel(item.type) }}</template><template v-else-if="column.key === 'quantity'">{{ money(item.quantity) }}</template><template v-else-if="column.key === 'lossAmount'">{{ lossAmountLabel(item.lossAmount) }}</template><template v-else-if="column.key === 'creator'">{{ item.creator }}</template><el-tag v-else-if="column.key === 'status'">{{ statusLabel(item.status) }}</el-tag><template v-else-if="column.key === 'createTime'">{{ formatDateTime(item.createTime) }}</template></template></el-table-column>
          <el-table-column label="Actions" width="190"><template #default="{ row: item }"><el-button link @click.stop="openDetail(item)">Detail</el-button><el-button v-permission="'badproduct:save'" link @click.stop="openEdit(item)">Edit</el-button><el-button v-if="item.status === 'pending'" v-permission="'badproduct:process'" link @click.stop="openProcess(item)">Process</el-button></template></el-table-column>
          <template #empty><el-empty description="No records" /></template>
        </el-table>
        <el-pagination background layout="prev, pager, next" :current-page="query.pageNum" :page-size="query.pageSize" :total="pagination.total" @current-change="changePage" />
      </section>
    </div>
    <el-drawer v-model="detailVisible" :title="scopeMeta.detailTitle" size="460px"><el-form v-if="detailRecord" label-position="top"><el-form-item label="Order">{{ detailRecord.orderId }}</el-form-item><el-form-item label="Type">{{ typeLabel(detailRecord.type) }}</el-form-item><el-form-item label="Description">{{ detailRecord.description }}</el-form-item><el-form-item label="Attachment"><el-button v-if="detailRecord.attachmentUrl" link @click="openAttachment(detailRecord.attachmentUrl, detailRecord.attachmentName)">{{ detailRecord.attachmentName }}</el-button><el-empty v-else description="No attachment" /></el-form-item></el-form><el-empty v-else description="No detail" /></el-drawer>
    <el-drawer v-model="formVisible" :title="editingRecord ? scopeMeta.editTitle : scopeMeta.createTitle" size="520px" :before-close="closeForm"><el-form :model="form" label-position="top"><BusinessTimeCorrectionPanel v-model="form.createTime" :active="timeCorrectionMode" data-field="badProduct.createTime" /><el-form-item label="Order"><el-input v-model.trim="form.orderId" /></el-form-item><el-form-item label="Type"><el-select v-model="form.type"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="Quantity"><el-input v-model.trim="form.quantity" data-field="badProduct.quantity" /></el-form-item><el-form-item label="Loss"><el-select v-model="form.lossAmount" data-field="badProduct.lossAmount"><el-option v-for="item in lossAmountOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="Description"><el-input v-model.trim="form.description" type="textarea" /></el-form-item><el-form-item label="Attachment"><DragAttachmentUpload :uploading="attachmentUploading" :file-name="form.attachmentName" :file-url="form.attachmentUrl" :file-size="form.attachmentSize" @select="handleAttachmentFile" @download="openAttachment(form.attachmentUrl, form.attachmentName)" @remove="removeAttachment" /></el-form-item></el-form><template #footer><el-button @click="closeForm">Cancel</el-button><el-button v-permission="'badproduct:save'" type="primary" @click="submitForm">Save</el-button></template></el-drawer>
    <el-drawer v-model="processVisible" :title="scopeMeta.processTitle" size="520px" :before-close="closeProcess"><el-form :model="processForm" label-position="top"><el-form-item label="Responsible"><el-input v-model.trim="processForm.responsiblePerson" data-field="badProduct.responsiblePerson" /></el-form-item><el-form-item label="Method"><el-input v-model.trim="processForm.method" data-field="badProduct.processMethod" /></el-form-item><el-form-item label="Measure"><el-input v-model.trim="processForm.processMeasure" data-field="badProduct.processMeasure" type="textarea" /></el-form-item><el-form-item label="Improvement"><el-input v-model.trim="processForm.improvementPlan" data-field="badProduct.improvementPlan" type="textarea" /></el-form-item><el-form-item label="Remark"><el-input v-model.trim="processForm.remark" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="closeProcess">Cancel</el-button><el-button v-permission="'badproduct:process'" type="primary" @click="submitProcess">Submit</el-button></template></el-drawer>
  </div>
</template>



<script setup>
import { computed, reactive, ref } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElMessage, ElOption, ElPagination, ElSelect, ElTable, ElTableColumn, ElTag } from 'element-plus'
import {
  downloadBadProductAttachment,
  getBadProductPage,
  processBadProduct,
  saveBadProduct,
  uploadBadProductAttachment
} from './api/badProduct.js'
import { warnAndFocusField } from '@/utils/formFocus'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import DateFilterInput from '@/components/DateFilterInput.vue'
import BusinessTimeCorrectionPanel from '@/components/BusinessTimeCorrectionPanel.vue'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useTimeCorrectionMode } from '@/composables/useTimeCorrectionMode'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const qualityTypeOptions = [
  { value: 'raw_material', label: '原材料' },
  { value: 'process_standard', label: '工艺标准' },
  { value: 'process_flow', label: '工艺流程' },
  { value: 'other', label: '其他' }
]
const afterSalesTypeOptions = [
  { value: 'motor', label: '电机' },
  { value: 'manual_track', label: '手动轨道' },
  { value: 'electric_track', label: '电动轨道' },
  { value: 'fabric', label: '面料' },
  { value: 'electric_roller_blind', label: '电动卷帘' },
  { value: 'manual_roller_blind', label: '手动卷帘' },
  { value: 'wear_part', label: '易损件' },
  { value: 'craft', label: '工艺' },
  { value: 'installation', label: '安装' },
  { value: 'measurement', label: '测量' },
  { value: 'after_sales_other', label: '其他' }
]
const allTypeOptions = [...qualityTypeOptions, ...afterSalesTypeOptions]
const lossAmountOptions = [
  { value: '25', label: '0-50' },
  { value: '100', label: '50-200' },
  { value: '350', label: '200-500' },
  { value: '1250', label: '500-2000' },
  { value: '3500', label: '2000-5000' },
  { value: '5001', label: '5000以上' }
]
const scopeOptions = [
  {
    value: 'quality',
    tabTitle: '质量记录',
    tabDesc: '登记生产、运输和质量异常，形成责任与改进闭环。',
    eyebrow: '质量追踪中心',
    title: '质量管理',
    desc: '统一登记质量问题、运输破损和其他质量异常记录，支持处理闭环和损失跟踪。',
    createText: '新增质量记录',
    countText: '条质量记录',
    emptyText: '暂无质量记录。',
    detailTitle: '质量记录详情',
    createTitle: '新增质量记录',
    editTitle: '编辑质量记录',
    processTitle: '处理质量记录',
    formSubtitle: '保存后会形成质量记录。',
    icon: 'fact_check'
  },
  {
    value: 'afterSales',
    tabTitle: '售后管理',
    tabDesc: '记录客户售后、退换货、投诉和赔付协商，便于追踪回访。',
    eyebrow: '客户售后中心',
    title: '售后管理',
    desc: '统一管理客户售后问题、退换货、赔付协商和回访记录，确保每个售后都有处理闭环。',
    createText: '新增售后记录',
    countText: '条售后记录',
    emptyText: '暂无售后记录。',
    detailTitle: '售后记录详情',
    createTitle: '新增售后记录',
    editTitle: '编辑售后记录',
    processTitle: '处理售后记录',
    formSubtitle: '保存后会形成售后记录。',
    icon: 'support_agent'
  }
]
const defaultBadProductTableColumns = [
  { key: 'defectiveId', label: '记录编号' },
  { key: 'orderId', label: '关联订单' },
  { key: 'type', label: '记录类型' },
  { key: 'quantity', label: '数量', align: 'right' },
  { key: 'lossAmount', label: '损失金额', align: 'right' },
  { key: 'creator', label: '登记人' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '登记时间' }
]
const {
  orderedColumns: badProductTableColumns,
  moveColumn: moveBadProductTableColumn,
  resetColumns: resetBadProductTableColumns
} = useLocalTableColumns('bad-product.list', defaultBadProductTableColumns)
const badProductTableColumnCount = computed(() => badProductTableColumns.value.length + 1)

const rows = ref([])
const loading = ref(false)
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: '', type: '', date: '', keyword: '', startDate: '', endDate: '' })
const activeScope = ref('quality')
const detailVisible = ref(false)
const detailRecord = ref(null)
const formVisible = ref(false)
const editingRecord = ref(null)
const processVisible = ref(false)
const processingRecord = ref(null)
const attachmentUploading = ref(false)
const form = reactive(createEmptyForm())
const processForm = reactive({
  responsiblePerson: '',
  method: '',
  processMeasure: '',
  improvementPlan: '',
  remark: ''
})
const {
  timeCorrectionMode,
  closeTimeCorrectionMode
} = useTimeCorrectionMode({
  isAvailable: () => formVisible.value
})

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const scopeMeta = computed(() => scopeOptions.find((item) => item.value === activeScope.value) || scopeOptions[0])
const typeOptions = computed(() => activeScope.value === 'afterSales' ? afterSalesTypeOptions : qualityTypeOptions)
const stats = computed(() => {
  const pending = rows.value.filter((item) => item.status === 'pending').length
  const processed = rows.value.filter((item) => item.status === 'processed').length
  const lossAmount = rows.value.reduce((total, item) => total + Number(item.lossAmount || 0), 0)
  return { pending, processed, lossAmount }
})

function badProductCellClass(key) {
  if (key === 'quantity') return 'text-right text-sm font-bold'
  if (key === 'lossAmount') return 'text-right text-sm font-black text-primary'
  if (key === 'createTime') return 'text-xs text-on-surface-variant'
  if (key !== 'defectiveId' && key !== 'status') return 'text-sm'
  return ''
}

fetchData()

function handleScopeChange(scope) {
  if (activeScope.value === scope) {
    return
  }
  activeScope.value = scope
  query.type = ''
  query.status = ''
  query.pageNum = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getBadProductPage({
      ...query,
      businessScope: activeScope.value,
      status: query.status || undefined,
      type: query.type || undefined,
      date: query.date || undefined,
      keyword: query.keyword || undefined,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  query.pageNum = 1
  fetchData()
}

function resetFilter() {
  query.status = ''
  query.type = ''
  query.date = ''
  query.keyword = ''
  query.startDate = ''
  query.endDate = ''
  query.pageNum = 1
  fetchData()
}

function changePage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) {
    return
  }
  query.pageNum = pageNum
  fetchData()
}

function openDetail(record) {
  detailRecord.value = record
  detailVisible.value = true
}

function ensurePermission(permission) {
  if (userStore.hasPermission(permission)) {
    return true
  }
  ElMessage.warning('当前账号暂无权限')
  return false
}

function openCreate() {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  resetForm()
  editingRecord.value = null
  formVisible.value = true
}

function openEdit(record) {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  editingRecord.value = record
  form.defectiveId = record.defectiveId
  form.orderId = record.orderId || ''
  form.type = normalizeTypeForScope(record.type)
  form.quantity = record.quantity == null ? '' : String(record.quantity)
  form.lossAmount = normalizeLossAmountBucketValue(record.lossAmount)
  form.description = record.description || ''
  form.attachmentName = record.attachmentName || ''
  form.attachmentUrl = record.attachmentUrl || ''
  form.attachmentSize = record.attachmentSize || null
  form.createTime = toDateTimeLocal(record.createTime)
  formVisible.value = true
}

function closeForm(done) {
  formVisible.value = false
  closeTimeCorrectionMode()
  editingRecord.value = null
  resetForm()
  done?.()
}

function openProcess(record) {
  if (!ensurePermission('badproduct:process')) {
    return
  }
  processingRecord.value = record
  resetProcessForm(record)
  processVisible.value = true
}

function closeProcess(done) {
  processVisible.value = false
  processingRecord.value = null
  resetProcessForm()
  done?.()
}

function closePanels() {
  detailVisible.value = false
  closeForm()
  closeProcess()
}

async function handleAttachmentFile(file) {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  if (!file) {
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('附件不能超过 10MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  attachmentUploading.value = true
  try {
    const result = await uploadBadProductAttachment(formData)
    form.attachmentName = result.fileName || file.name
    form.attachmentUrl = result.fileUrl || ''
    form.attachmentSize = result.fileSize || file.size
    ElMessage.success('附件上传成功')
  } finally {
    attachmentUploading.value = false
  }
}

function removeAttachment() {
  form.attachmentName = ''
  form.attachmentUrl = ''
  form.attachmentSize = null
}

async function openAttachment(url, name) {
  if (!url) {
    return
  }
  const blob = await downloadBadProductAttachment({ url, name })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name || 'quality-attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

async function submitForm() {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  if (!form.quantity || Number(form.quantity) <= 0) {
    return warnAndFocusField('请填写有效的异常数量', 'badProduct.quantity')
  }
  if (!form.lossAmount || Number(form.lossAmount) <= 0) {
    return warnAndFocusField('请填写有效的损失金额', 'badProduct.lossAmount')
  }
  if (!validateCreateTimeInput(form.createTime)) {
    return
  }

  await saveBadProduct({
    defectiveId: form.defectiveId || undefined,
    orderId: form.orderId || undefined,
    type: form.type,
    quantity: Number(form.quantity),
    lossAmount: Number(form.lossAmount),
    description: form.description || undefined,
    attachmentName: form.attachmentName || undefined,
    attachmentUrl: form.attachmentUrl || undefined,
    attachmentSize: form.attachmentSize || undefined,
    createTime: formatCreateTimePayload(form.createTime) || undefined
  })
  ElMessage.success(editingRecord.value ? '质量记录已更新' : '质量记录已新增')
  closeForm()
  await fetchData()
}

async function submitProcess() {
  if (!ensurePermission('badproduct:process')) {
    return
  }
  if (!processForm.responsiblePerson) {
    return warnAndFocusField('请填写负责人员', 'badProduct.responsiblePerson')
  }
  if (!processForm.method) {
    return warnAndFocusField('请填写处理方式', 'badProduct.processMethod')
  }
  if (!processForm.processMeasure) {
    return warnAndFocusField('请填写处理措施', 'badProduct.processMeasure')
  }
  if (!processForm.improvementPlan) {
    return warnAndFocusField('请填写改进方案', 'badProduct.improvementPlan')
  }

  await processBadProduct({
    defectiveId: processingRecord.value?.defectiveId,
    method: processForm.method,
    responsiblePerson: processForm.responsiblePerson,
    processMeasure: processForm.processMeasure,
    improvementPlan: processForm.improvementPlan,
    remark: processForm.remark || undefined
  })
  ElMessage.success('质量处理已提交审核')
  closeProcess()
  await fetchData()
}

function typeLabel(value) {
  return allTypeOptions.find((item) => item.value === value)?.label || '其他'
}

function normalizeLossAmountBucketValue(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number) || number <= 0) {
    return ''
  }
  if (number <= 50) return '25'
  if (number <= 200) return '100'
  if (number <= 500) return '350'
  if (number <= 2000) return '1250'
  if (number <= 5000) return '3500'
  return '5001'
}

function lossAmountLabel(value) {
  const bucketValue = normalizeLossAmountBucketValue(value)
  return lossAmountOptions.find((item) => item.value === bucketValue)?.label || '--'
}

function statusLabel(value) {
  if (value === 'processed') return '已处理'
  if (value === 'pending_audit') return '审核中'
  return '待处理'
}

function statusClass(value) {
  if (value === 'processed') return 'bg-emerald-100 text-emerald-700'
  if (value === 'pending_audit') return 'bg-sky-100 text-sky-700'
  return 'bg-amber-100 text-amber-700'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function toDateTimeLocal(value) {
  if (!value) {
    return ''
  }
  const normalized = String(value).replace(' ', 'T')
  return normalized.length >= 16 ? normalized.slice(0, 19) : normalized
}

function formatCreateTimePayload(value) {
  if (!value) {
    return ''
  }
  const text = String(value).trim()
  return text.length === 16 ? `${text}:00` : text
}

function validateCreateTimeInput(value) {
  if (!value) {
    return true
  }
  const payload = formatCreateTimePayload(value)
  const date = new Date(payload)
  if (!Number.isFinite(date.getTime())) {
    warnAndFocusField('登记时间格式不正确，请选择完整日期和时间', 'badProduct.createTime')
    return false
  }
  if (date.getTime() > Date.now()) {
    warnAndFocusField('登记时间不能晚于当前时间', 'badProduct.createTime')
    return false
  }
  return true
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatFileSize(value) {
  const size = Number(value || 0)
  if (!Number.isFinite(size) || size <= 0) {
    return '--'
  }
  if (size < 1024) {
    return `${size}B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function resetProcessForm(record = null) {
  processForm.responsiblePerson = record?.responsiblePerson || ''
  processForm.method = record?.processMethod || ''
  processForm.processMeasure = record?.processMeasure || ''
  processForm.improvementPlan = record?.improvementPlan || ''
  processForm.remark = record?.processRemark || ''
}

function createEmptyForm() {
  return {
    defectiveId: '',
    orderId: '',
    type: defaultTypeForScope(),
    quantity: '',
    lossAmount: '',
    description: '',
    attachmentName: '',
    attachmentUrl: '',
    attachmentSize: null,
    createTime: ''
  }
}

function defaultTypeForScope() {
  return activeScope.value === 'afterSales' ? 'motor' : 'raw_material'
}

function normalizeTypeForScope(value) {
  const options = activeScope.value === 'afterSales' ? afterSalesTypeOptions : qualityTypeOptions
  return options.some((item) => item.value === value) ? value : defaultTypeForScope()
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.drawer-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.time-correction-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  border: 1px solid rgba(31, 63, 95, 0.24);
  background: rgba(238, 244, 251, 0.86);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 900;
  color: #1f3f5f;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.time-correction-toggle small {
  color: rgba(71, 85, 105, 0.72);
  font-size: 10px;
  font-weight: 800;
}

.time-correction-toggle.active {
  border-color: rgba(31, 63, 95, 0.86);
  background: #1f3f5f;
  color: #fff;
}

.time-correction-toggle.active small {
  color: rgba(255, 255, 255, 0.78);
}
</style>
