<template>
  <div class="function-page-shell h-full min-h-0">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow"><span class="material-symbols-outlined">qr_code_scanner</span>设备巡检中心</div>
          <h1 class="function-page-title">设备巡检记录</h1>
          <p class="function-page-desc">建立固定设备二维码，现场扫码巡检后记录自动沉淀到设备档案。</p>
        </div>
        <el-form :inline="true" class="flex flex-wrap justify-end gap-3">
          <el-form-item>
            <el-input v-model.trim="filters.keyword" clearable placeholder="搜索设备编码、名称、位置或负责人" @keyup.enter="fetchDevices" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="filters.status" clearable placeholder="全部状态" class="w-32">
              <el-option label="启用中" value="enabled" />
              <el-option label="已停用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
          <el-form-item><el-button @click="resetSearch">重置</el-button></el-form-item>
          <el-form-item><el-button @click="exportEquipmentExcel">导出 Excel</el-button></el-form-item>
          <el-form-item><el-button type="primary" @click="openCreate">新增设备</el-button></el-form-item>
        </el-form>
      </header>

      <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <article class="stat-card"><p>设备总数</p><strong>{{ total }}</strong></article>
        <article class="stat-card"><p>固定二维码</p><strong>一次打印</strong></article>
        <article class="stat-card"><p>巡检方式</p><strong>扫码记录</strong></article>
      </section>

      <section class="table-panel">
        <el-table ref="equipmentTableRef" v-loading="loading" :data="devices" class="equipment-table" row-key="id">
          <el-table-column label="设备" min-width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">{{ row.equipmentName }}</el-button>
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
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 'enabled'" link type="danger" @click="handleDisable(row)">停用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && devices.length === 0" description="暂无设备档案" />
        <div class="table-footer">
          <span>共 {{ total }} 条</span>
          <el-pagination :current-page="pageNum" :page-size="pageSize" :page-count="totalPages" :total="total" layout="prev, pager, next" @current-change="changePage" />
        </div>
      </section>
    </div>

    <el-drawer v-model="editorVisible" :title="editingId ? '编辑设备' : '新增设备'" size="720px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="设备名称" required><el-input v-model.trim="form.equipmentName" placeholder="例如：定型机01" /></el-form-item>
        <el-form-item label="设备编码"><el-input v-model.trim="form.equipmentCode" :disabled="!!editingId" placeholder="不填则系统自动生成" /></el-form-item>
        <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
          <el-form-item label="设备类型"><el-input v-model.trim="form.equipmentType" placeholder="生产设备/仓储设备" /></el-form-item>
          <el-form-item label="巡检周期（天）"><el-input-number v-model="form.inspectionCycleDays" :min="1" :max="3650" class="w-full" /></el-form-item>
        </div>
        <el-form-item label="设备位置"><el-input v-model.trim="form.location" placeholder="例如：一车间 A 区" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model.trim="form.responsiblePerson" placeholder="设备责任人" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" class="w-full"><el-option label="启用中" value="enabled" /><el-option label="已停用" value="disabled" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" type="textarea" :rows="4" placeholder="巡检重点、注意事项等" /></el-form-item>
      </el-form>
      <template #footer><div class="flex justify-end gap-3"><el-button @click="closeDrawers">取消</el-button><el-button type="primary" :loading="saving" @click="submitForm">保存设备</el-button></div></template>
    </el-drawer>

    <el-drawer v-model="detailVisible" :title="detail?.equipmentName || '设备详情'" size="760px" destroy-on-close @closed="detail = null">
      <template #header><div><h2 class="text-lg font-bold">{{ detail?.equipmentName || '设备详情' }}</h2><p class="mt-1 text-sm text-on-surface-variant">{{ detail?.equipmentCode }}</p></div></template>
      <section class="detail-callout"><h3>固定巡检二维码</h3><p>打印一次后贴在设备上，员工扫码即可填写巡检记录。</p></section>
      <el-descriptions :column="2" border class="mt-6">
        <el-descriptions-item label="设备类型">{{ detail?.equipmentType || '--' }}</el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ detail?.location || '--' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail?.responsiblePerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近巡检">{{ formatDateTime(detail?.lastInspectionTime) }}</el-descriptions-item>
      </el-descriptions>
      <section class="mt-8">
        <div class="mb-4 flex items-center justify-between"><h3 class="text-lg font-bold">巡检记录</h3><el-button @click="fetchRecords">刷新</el-button></div>
        <div v-loading="recordsLoading" class="min-h-32">
          <div v-if="records.length" class="space-y-3">
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
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElButton, ElDescriptions, ElDescriptionsItem, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElPagination, ElSelect, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { exportTableElementToExcel } from '@/utils/tableExport'
import { disableEquipment, getEquipmentDetail, getEquipmentInspectionRecords, getEquipmentPage, saveEquipment } from './api/equipment'

const loading = ref(false)
const saving = ref(false)
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
const equipmentTableRef = ref(null)

const defaultForm = () => ({ equipmentCode: '', equipmentName: '', equipmentType: '', location: '', responsiblePerson: '', inspectionCycleDays: 7, status: 'enabled', remark: '' })
const form = reactive(defaultForm())
const queryParams = computed(() => ({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: filters.keyword || undefined, status: filters.status || undefined }))

async function fetchDevices() {
  loading.value = true
  try {
    const page = await getEquipmentPage(queryParams.value)
    devices.value = page?.data || []
    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchDevices() }
function resetSearch() { filters.keyword = ''; filters.status = ''; pageNum.value = 1; fetchDevices() }

async function exportEquipmentExcel() {
  try {
    await exportTableElementToExcel(equipmentTableRef.value?.$el, { fileName: '设备巡检记录', sheetName: '设备巡检记录' })
    ElMessage.success('Excel 已导出')
  } catch (error) {
    ElMessage.warning(error?.message || '导出失败，请稍后重试')
  }
}

function changePage(nextPage) { pageNum.value = Math.min(Math.max(1, nextPage), totalPages.value); fetchDevices() }
function resetForm(data = {}) { Object.assign(form, defaultForm(), data) }
function openCreate() { editingId.value = null; resetForm(); editorVisible.value = true }
function openEdit(device) { editingId.value = device.id; resetForm(device); editorVisible.value = true }

async function submitForm() {
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
  await ElMessageBox.confirm(`确认停用设备“${device.equipmentName}”？停用后现场人员无法继续扫码巡检。`, '停用设备', { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' })
  await disableEquipment(device.id)
  ElMessage.success('设备已停用')
  fetchDevices()
}

async function openDetail(device) {
  detailVisible.value = true
  detail.value = await getEquipmentDetail(device.id)
  await fetchRecords()
}

async function fetchRecords() {
  if (!detail.value?.id) {
    records.value = []
    return
  }
  recordsLoading.value = true
  try {
    const page = await getEquipmentInspectionRecords({ equipmentId: detail.value.id, pageNum: 1, pageSize: 20 })
    records.value = page?.data || []
  } finally {
    recordsLoading.value = false
  }
}

function closeDrawers() { editorVisible.value = false; detailVisible.value = false }
function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '--' }

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
