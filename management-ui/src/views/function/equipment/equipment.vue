<template>
  <div class="function-page-shell function-page-shell--compact h-full min-h-0">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow"><span class="material-symbols-outlined">qr_code_scanner</span>设备巡检中心</div>
          <h1 class="function-page-title">设备巡检记录</h1>
          <p class="function-page-desc">建立固定设备二维码，现场扫码巡检后记录自动沉淀到设备档案。</p>
        </div>
      </header>

      <el-form :inline="true" class="function-filter-form equipment-filter-form" @submit.prevent="handleSearch">
          <el-form-item>
            <el-input v-model.trim="filters.keyword" aria-label="设备关键词" clearable placeholder="搜索设备编码、名称、位置或负责人" @keyup.enter="fetchDevices" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="filters.status" aria-label="设备状态" clearable placeholder="全部状态" class="w-32">
              <el-option label="启用中" value="enabled" />
              <el-option label="已停用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item class="function-filter-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-tooltip :disabled="canExport" content="暂无 equipment:export 权限"><span><el-button :disabled="!canExport" @click="exportEquipmentExcel">导出 Excel</el-button></span></el-tooltip>
            <el-tooltip :disabled="canCreate" content="暂无 equipment:create 权限"><span><el-button type="primary" :disabled="!canCreate" @click="openCreate">新增设备</el-button></span></el-tooltip>
          </el-form-item>
      </el-form>

      <section class="function-stats-grid grid-cols-1 md:grid-cols-3 equipment-stats-grid">
        <article class="function-stat-card stat-card"><p>设备总数</p><strong>{{ total }}</strong></article>
        <article class="function-stat-card stat-card"><p>固定二维码</p><strong>一次打印</strong></article>
        <article class="function-stat-card stat-card"><p>巡检方式</p><strong>扫码记录</strong></article>
      </section>

      <section class="function-list-panel table-panel">
        <el-result
          v-if="listFailure"
          :icon="listFailure.kind === 'forbidden' ? 'warning' : 'error'"
          :title="listFailure.title"
          :sub-title="listFailure.message"
        >
          <template #extra>
            <el-button type="primary" :loading="loading" @click="fetchDevices">重试</el-button>
          </template>
        </el-result>
        <template v-else>
          <div class="function-table-scroll">
            <el-table v-loading="loading" :data="devices" class="equipment-table" row-key="id">
              <el-table-column label="设备" min-width="170">
              <template #default="{ row }">
                <el-tooltip :disabled="canViewDetail" content="暂无 equipment:detail 权限"><span><el-button link type="primary" :disabled="!canViewDetail" @click="openDetail(row)">{{ row.equipmentName }}</el-button></span></el-tooltip>
                <p class="mt-1 text-xs text-on-surface-variant">{{ row.equipmentCode }}</p>
              </template>
            </el-table-column>
            <el-table-column label="类型/位置" min-width="180">
              <template #default="{ row }"><p>{{ row.equipmentType || '--' }}</p><p class="mt-1 text-xs text-on-surface-variant">{{ row.location || '--' }}</p></template>
            </el-table-column>
            <el-table-column prop="responsiblePerson" label="负责人" min-width="120"><template #default="{ row }">{{ row.responsiblePerson || '--' }}</template></el-table-column>
            <el-table-column label="巡检周期" min-width="110"><template #default="{ row }">{{ row.inspectionCycleDays ?? 7 }} 天</template></el-table-column>
            <el-table-column label="最近巡检" min-width="150"><template #default="{ row }">{{ formatDateTime(row.lastInspectionTime) }}</template></el-table-column>
            <el-table-column label="状态" min-width="100"><template #default="{ row }"><el-tag :type="row.status === 'enabled' ? 'success' : 'info'">{{ row.status === 'enabled' ? '启用中' : '已停用' }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-tooltip :disabled="canViewDetail" content="暂无 equipment:detail 权限"><span><el-button link type="primary" :disabled="!canViewDetail" @click="openDetail(row)">详情</el-button></span></el-tooltip>
                <el-tooltip :disabled="canViewInspection" content="暂无 equipment:inspection:list 权限"><span><el-button link type="primary" :disabled="!canViewInspection" @click="openInspection(row)">巡检记录</el-button></span></el-tooltip>
                <el-tooltip :disabled="canUpdate" content="暂无 equipment:update 权限"><span><el-button link type="primary" :disabled="!canUpdate" @click="openEdit(row)">编辑</el-button></span></el-tooltip>
                <el-tooltip v-if="row.status === 'enabled'" :disabled="canDisable" content="暂无 equipment:disable 权限"><span><el-button link type="danger" :disabled="!canDisable" @click="handleDisable(row)">停用</el-button></span></el-tooltip>
              </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无设备档案" />
              </template>
            </el-table>
          </div>
          <div class="table-footer">
            <span>共 {{ total }} 条</span>
            <el-pagination :current-page="pageNum" :page-size="pageSize" :page-count="totalPages" :total="total" :disabled="loading" layout="prev, pager, next" @current-change="changePage" />
          </div>
        </template>
      </section>
    </div>

    <el-drawer v-model="editorVisible" :title="editingId ? '编辑设备' : '新增设备'" size="720px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="设备名称" required><el-input v-model.trim="form.equipmentName" placeholder="例如：定型机01" /></el-form-item>
        <el-form-item label="设备编码">
          <el-tooltip :disabled="!editingId" content="设备码已用于固定二维码，创建后不可修改。" placement="top">
            <div class="w-full">
              <el-input v-model.trim="form.equipmentCode" :disabled="!!editingId" placeholder="不填则系统自动生成" />
            </div>
          </el-tooltip>
        </el-form-item>
        <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
          <el-form-item label="设备类型"><el-input v-model.trim="form.equipmentType" placeholder="生产设备/仓储设备" /></el-form-item>
          <el-form-item label="巡检周期（天）"><el-input-number v-model="form.inspectionCycleDays" :min="1" :max="3650" class="w-full" /></el-form-item>
        </div>
        <el-form-item label="设备位置"><el-input v-model.trim="form.location" placeholder="例如：一车间 A 区" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model.trim="form.responsiblePerson" placeholder="设备责任人" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" class="w-full"><el-option label="启用中" value="enabled" /><el-option label="已停用" value="disabled" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" type="textarea" :rows="4" placeholder="巡检重点、注意事项等" /></el-form-item>
      </el-form>
      <template #footer><div class="flex justify-end gap-3"><el-button @click="closeDrawers">取消</el-button><el-tooltip :disabled="canSubmitEquipment" :content="`暂无 ${editingId ? 'equipment:update' : 'equipment:create'} 权限`"><span><el-button type="primary" :disabled="!canSubmitEquipment" :loading="saving" @click="submitForm">保存设备</el-button></span></el-tooltip></div></template>
    </el-drawer>

    <el-drawer v-model="detailVisible" :title="selectedDevice?.equipmentName || '设备档案'" size="760px" destroy-on-close @closed="handleDetailClosed">
      <template #header><div><h2 class="text-lg font-bold">{{ selectedDevice?.equipmentName || '设备档案' }}</h2><p class="mt-1 text-sm text-on-surface-variant">{{ selectedDevice?.equipmentCode }}</p></div></template>
      <div v-loading="detailLoading" class="min-h-48">
      <el-result v-if="drawerMode === 'detail' && detailFailure" :icon="detailFailure.kind === 'forbidden' ? 'warning' : 'error'" :title="detailFailure.title" :sub-title="detailFailure.message"><template #extra><el-button type="primary" :loading="detailLoading" @click="retryDetail">重试</el-button></template></el-result>
      <template v-else>
      <template v-if="canViewDetail && detail">
      <section class="detail-callout"><h3>固定巡检二维码</h3><p>打印一次后贴在设备上，员工扫码即可填写巡检记录。</p></section>
      <el-descriptions v-if="canViewDetail && detail" :column="2" border class="mt-6">
        <el-descriptions-item label="设备类型">{{ detail?.equipmentType || '--' }}</el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ detail?.location || '--' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail?.responsiblePerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近巡检">{{ formatDateTime(detail?.lastInspectionTime) }}</el-descriptions-item>
      </el-descriptions>
      </template>
      <el-empty v-else-if="drawerMode === 'detail' && !detailLoading" description="暂无设备详情" />
      <section v-if="canViewInspection" class="mt-8">
        <div class="mb-4 flex items-center justify-between"><h3 class="text-lg font-bold">巡检记录</h3><el-tooltip :disabled="canViewInspection" content="暂无 equipment:inspection:list 权限"><span><el-button :disabled="!canViewInspection" @click="fetchRecords">刷新</el-button></span></el-tooltip></div>
        <div v-if="canViewInspection" v-loading="recordsLoading" class="min-h-32">
          <el-result v-if="recordsFailure" :icon="recordsFailure.kind === 'forbidden' ? 'warning' : 'error'" :title="recordsFailure.title" :sub-title="recordsFailure.message"><template #extra><el-button type="primary" :loading="recordsLoading" @click="retryRecords">重试</el-button></template></el-result>
          <div v-else-if="records.length" class="space-y-3">
            <article v-for="record in records" :key="record.id" class="record-card">
              <div class="flex items-center justify-between gap-3"><el-tag :type="record.inspectionResult === 'normal' ? 'success' : 'danger'">{{ record.inspectionResult === 'normal' ? '正常' : '异常' }}</el-tag><span class="text-xs text-on-surface-variant">{{ formatDateTime(record.inspectionTime) }}</span></div>
              <p v-if="record.abnormalDesc" class="mt-3 text-sm text-rose-700">{{ record.abnormalDesc }}</p>
              <p v-if="record.remark" class="mt-2 text-sm">{{ record.remark }}</p>
              <p class="mt-3 text-xs text-on-surface-variant">巡检人：{{ record.inspectorName || '--' }}</p>
            </article>
          </div>
          <el-empty v-else-if="!recordsLoading" description="暂无巡检记录" />
        </div>
      </section>
      </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElButton, ElDescriptions, ElDescriptionsItem, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElPagination, ElResult, ElSelect, ElTable, ElTableColumn, ElTag, ElTooltip } from 'element-plus'
import { exportRowsToExcel } from '@/utils/tableExport'
import { useUserStore } from '@/stores/user'
import { buildEquipmentExport } from './equipmentExport.js'
import { createLatestRequestGate, planEquipmentDrawerOpen, resolveEquipmentAccess, resolveInspectionEquipmentId } from './equipmentAccess.js'
import { disableEquipment, getEquipmentDetail, getEquipmentInspectionRecords, getEquipmentPage, saveEquipment } from './api/equipment'

const loading = ref(false)
const saving = ref(false)
const listFailure = ref(null)
const devices = ref([])
const total = ref(0)
const totalPages = ref(1)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '', status: '' })
const editorVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const detail = ref(null)
const records = ref([])
const recordsLoading = ref(false)
const userStore = useUserStore()
const canCreate = computed(() => userStore.hasPermission('equipment:create'))
const canUpdate = computed(() => userStore.hasPermission('equipment:update'))
const canDisable = computed(() => userStore.hasPermission('equipment:disable'))
const canExport = computed(() => userStore.hasPermission('equipment:export'))
const canSubmitEquipment = computed(() => editingId.value ? canUpdate.value : canCreate.value)
const equipmentAccess = computed(() => resolveEquipmentAccess((code) => userStore.hasPermission(code)))
const canViewList = computed(() => equipmentAccess.value.canViewList)
const canViewDetail = computed(() => equipmentAccess.value.canViewDetail)
const canViewInspection = computed(() => equipmentAccess.value.canViewInspection)
const detailLoading = ref(false)
const detailFailure = ref(null)
const recordsFailure = ref(null)
let detailRequestId = 0
let recordsRequestId = 0
let selectedDevice = null
const drawerMode = ref('detail')
const listRequestGate = createLatestRequestGate()

const defaultForm = () => ({ equipmentCode: '', equipmentName: '', equipmentType: '', location: '', responsiblePerson: '', inspectionCycleDays: 7, status: 'enabled', remark: '' })
const form = reactive(defaultForm())
const queryParams = computed(() => ({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: filters.keyword || undefined, status: filters.status || undefined }))

async function fetchDevices() {
  const requestId = listRequestGate.begin()
  loading.value = true
  listFailure.value = null
  devices.value = []
  total.value = 0
  totalPages.value = 1
  if (!canViewList.value) {
    listFailure.value = { kind: 'forbidden', title: '暂无权限查看设备列表', message: '当前账号缺少 equipment:list 权限，请联系管理员授权。' }
    loading.value = false
    return
  }
  try {
    const page = await getEquipmentPage(queryParams.value)
    if (!listRequestGate.isLatest(requestId)) return
    devices.value = page?.data || []
    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))
  } catch (error) {
    if (!listRequestGate.isLatest(requestId)) return
    listFailure.value = resolveRequestFailure(error)
  } finally {
    if (listRequestGate.isLatest(requestId)) loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchDevices() }
function resetSearch() { filters.keyword = ''; filters.status = ''; pageNum.value = 1; fetchDevices() }

async function exportEquipmentExcel() {
  if (!canExport.value) return
  try {
    const { headers, rows } = buildEquipmentExport(devices.value)
    await exportRowsToExcel({ headers, rows, fileName: '设备巡检记录', sheetName: '设备巡检记录', sourceModule: 'equipment' })
    ElMessage.success('Excel 已导出')
  } catch (error) {
    ElMessage.warning(error?.message || '导出失败，请稍后重试')
  }
}

function changePage(nextPage) { pageNum.value = Math.min(Math.max(1, nextPage), totalPages.value); fetchDevices() }
function resetForm(data = {}) { Object.assign(form, defaultForm(), data) }
function openCreate() { if (!canCreate.value) return; editingId.value = null; resetForm(); editorVisible.value = true }
function openEdit(device) { if (!canUpdate.value) return; editingId.value = device.id; resetForm(device); editorVisible.value = true }

async function submitForm() {
  if (!canSubmitEquipment.value) return
  if (!form.equipmentName?.trim()) {
    ElMessage.warning('请填写设备名称')
    return
  }
  saving.value = true
  try {
    await saveEquipment({ ...form, id: editingId.value || undefined })
    ElMessage.success('设备档案已保存')
    editorVisible.value = false
    await fetchDevices()
  } finally {
    saving.value = false
  }
}

async function handleDisable(device) {
  if (!canDisable.value) return
  await ElMessageBox.confirm(`确认停用设备“${device.equipmentName}”？停用后现场人员无法继续扫码巡检。`, '停用设备', { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' })
  await disableEquipment(device.id)
  ElMessage.success('设备已停用')
  fetchDevices()
}

function openInspection(device) {
  const plan = planEquipmentDrawerOpen('inspection', equipmentAccess.value)
  if (!plan.open) return
  drawerMode.value = 'inspection'
  selectedDevice = device
  detailRequestId += 1
  detail.value = null
  detailFailure.value = null
  detailLoading.value = false
  records.value = []
  recordsFailure.value = null
  detailVisible.value = true
  if (plan.requestInspection) fetchRecords(device.id)
}

async function openDetail(device) {
  const plan = planEquipmentDrawerOpen('detail', equipmentAccess.value)
  if (!plan.open) return
  drawerMode.value = 'detail'
  selectedDevice = device
  detailVisible.value = true
  detail.value = null
  records.value = []
  detailFailure.value = null
  recordsFailure.value = null
  recordsRequestId += 1
  const requestId = ++detailRequestId
  detailLoading.value = true
  try {
    const nextDetail = await getEquipmentDetail(device.id)
    if (requestId !== detailRequestId) return
    detail.value = nextDetail || null
    if (detail.value && plan.requestInspection) await fetchRecords(device.id)
  } catch (error) {
    if (requestId !== detailRequestId) return
    detailFailure.value = resolveRequestFailure(error, '设备详情')
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false
  }
}

async function fetchRecords(equipmentId = detail.value?.id) {
  if (!canViewInspection.value) return
  if (!equipmentId) {
    records.value = []
    return
  }
  const requestId = ++recordsRequestId
  records.value = []
  recordsFailure.value = null
  recordsLoading.value = true
  try {
    const page = await getEquipmentInspectionRecords({ equipmentId, pageNum: 1, pageSize: 20 })
    if (requestId !== recordsRequestId) return
    records.value = page?.data || []
  } catch (error) {
    if (requestId !== recordsRequestId) return
    recordsFailure.value = resolveRequestFailure(error, '巡检记录')
  } finally {
    if (requestId === recordsRequestId) recordsLoading.value = false
  }
}

function retryDetail() { if (selectedDevice) openDetail(selectedDevice) }
function retryRecords() {
  const equipmentId = resolveInspectionEquipmentId(selectedDevice, detail.value)
  if (equipmentId) fetchRecords(equipmentId)
}

function handleDetailClosed() {
  detailRequestId += 1
  recordsRequestId += 1
  selectedDevice = null
  drawerMode.value = 'detail'
  detail.value = null
  records.value = []
  detailFailure.value = null
  recordsFailure.value = null
  detailLoading.value = false
  recordsLoading.value = false
}

function closeDrawers() { editorVisible.value = false; detailVisible.value = false; detailRequestId += 1; recordsRequestId += 1 }
function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '--' }

function getRequestStatusCode(error) {
  const rawStatusCode = error?.response?.status
    ?? error?.response?.data?.code
    ?? error?.statusCode
    ?? error?.code
  const statusCode = Number(rawStatusCode)
  return Number.isFinite(statusCode) ? statusCode : 0
}

function resolveRequestFailure(error, subject = '设备列表') {
  const statusCode = getRequestStatusCode(error)
  if (statusCode === 401) {
    return { kind: 'unauthorized', title: '登录状态已失效', message: `请重新登录后再重试${subject}。` }
  }
  if (statusCode === 403) {
    return { kind: 'forbidden', title: subject === '设备列表' ? '暂无权限查看设备列表' : `暂无权限查看${subject}`, message: `请联系管理员确认${subject}权限。` }
  }
  if (statusCode >= 500) {
    return { kind: 'request', title: subject === '设备列表' ? '设备列表加载失败' : `${subject}加载失败`, message: '服务暂时不可用，请稍后重试。' }
  }
  return { kind: 'request', title: subject === '设备列表' ? '设备列表加载失败' : `${subject}加载失败`, message: '网络连接异常，请检查网络后重试。' }
}

fetchDevices()
</script>

<style scoped>
.stat-card { border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: rgb(var(--surface-container-lowest)); padding: 1.25rem; }
.stat-card p { font-size: .875rem; color: rgb(var(--on-surface-variant)); }
.stat-card strong { display: block; margin-top: .5rem; font-size: 1.5rem; color: rgb(var(--on-surface)); }
.table-panel { overflow: hidden; border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: rgb(var(--surface-container-lowest)); }
.table-footer { display: flex; align-items: center; justify-content: space-between; gap: 1rem; border-top: 1px solid rgb(148 163 184 / 0.16); padding: 1rem 1.5rem; font-size: .875rem; color: rgb(var(--on-surface-variant)); }
.detail-callout { border: 1px solid rgb(var(--primary) / 0.2); border-radius: 8px; background: rgb(var(--primary) / 0.05); padding: 1.25rem; }
.detail-callout h3 { font-weight: 700; color: rgb(var(--primary)); }
.detail-callout p { margin-top: .5rem; font-size: .875rem; color: rgb(var(--on-surface-variant)); }
.record-card { border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; padding: 1rem; }
@media (max-width: 640px) { .table-footer { align-items: flex-start; flex-direction: column; } }
</style>
