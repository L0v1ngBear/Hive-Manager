<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="installation-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">engineering</span>
            安装交付中心
          </div>
          <h1 class="function-page-title">安装任务</h1>
          <p class="installation-header-desc">
            跟进已完成订单的发货、现场安装、验收和附件回传，安装完成后补齐施工人员信息。
          </p>
        </div>
        <el-button type="primary" :loading="loading" @click="loadTasks">刷新</el-button>
      </header>

      <section class="installation-summary-grid">
        <el-button
          v-for="card in summaryCards"
          :key="card.key"
          plain
          class="installation-summary-card"
          :class="card.className"
          @click="selectStatus(card.status)"
        >
          <span class="material-symbols-outlined">{{ card.icon }}</span>
          <span>
            <small>{{ card.label }}</small>
            <strong>{{ card.count }}</strong>
            <em>{{ card.hint }}</em>
          </span>
        </el-button>
      </section>

      <section class="installation-panel">
        <el-form :model="filters" class="installation-filter-grid" @submit.prevent="loadTasks">
          <el-form-item label="综合搜索" class="installation-filter-field installation-filter-field-wide">
            <el-input
              v-model.trim="filters.keyword"
              placeholder="订单号、客户、项目、品牌或物流单号"
              @keyup.enter="loadTasks"
            />
          </el-form-item>
          <el-form-item label="客户" class="installation-filter-field">
            <el-input v-model.trim="filters.customerName" placeholder="客户名称" @keyup.enter="loadTasks" />
          </el-form-item>
          <el-form-item label="项目" class="installation-filter-field">
            <el-input v-model.trim="filters.projectName" placeholder="项目名称" @keyup.enter="loadTasks" />
          </el-form-item>
          <el-form-item label="安装状态" class="installation-filter-field">
            <el-select v-model="filters.status" clearable placeholder="全部状态">
              <el-option
                v-for="status in taskStatuses"
                :key="status.value"
                :label="status.label"
                :value="status.value"
              />
            </el-select>
          </el-form-item>
          <div class="installation-filter-actions">
            <el-button type="primary" @click="loadTasks">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-form>

        <div
          v-if="requestState === 'loading'"
          v-loading="true"
          class="min-h-[280px]"
          aria-label="安装任务加载中"
        />
        <div v-else-if="requestState === 'permission'" class="min-h-[280px]">
          <el-empty :description="requestErrorMessage">
            <el-button type="primary" @click="loadTasks">重新加载</el-button>
          </el-empty>
        </div>
        <div v-else-if="requestState === 'error'" class="min-h-[280px]">
          <el-empty :description="requestErrorMessage">
            <el-button type="primary" @click="loadTasks">重新加载</el-button>
          </el-empty>
        </div>
        <template v-else>
        <el-table v-loading="false" :data="rows" row-key="id" class="installation-table">
          <el-table-column label="订单信息" min-width="220">
            <template #default="{ row }">
              <div class="installation-order-cell">
                <span class="installation-order-code">{{ row.orderId }}</span>
                <span class="installation-quantity-chip">数量 {{ row.totalQuantity || 0 }}</span>
              </div>
              <div class="installation-muted-line">{{ row.goodsDesc || '暂无商品描述' }}</div>
              <div class="installation-meta-line">
                <span>{{ row.brandName || '未填写品牌' }}</span>
                <span>{{ formatDateTime(row.createTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="客户项目" min-width="210">
            <template #default="{ row }">
              <div class="installation-main-text">{{ row.customerName || '未填写客户' }}</div>
              <div class="installation-muted-line">{{ row.projectName || '未填写项目' }}</div>
              <div class="installation-meta-line">
                <span class="material-symbols-outlined">location_on</span>
                <span>{{ row.deliveryAddress || '未填写安装地址' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="交付物流" min-width="190">
            <template #default="{ row }">
              <div class="installation-main-text">{{ row.deliveryDate || '未填写交付日期' }}</div>
              <div class="installation-logistics">
                <span class="material-symbols-outlined">local_shipping</span>
                <span>
                  {{ row.expressCompany || '未填写物流' }}
                  <template v-if="row.expressNo"> / {{ row.expressNo }}</template>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="安装状态" min-width="160">
            <template #default="{ row }">
              <div class="installation-status-stack">
                <el-tag
                  :type="
                    row.installationStatus === 'completed_accepted'
                      ? 'success'
                      : row.installationStatus === 'shipped_pending_install'
                        ? 'warning'
                        : 'info'
                  "
                >
                  {{ statusLabel(row.installationStatus) }}
                </el-tag>
                <small>更新 {{ formatDateTime(row.updateTime) }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="施工信息" min-width="210">
            <template #default="{ row }">
              <div class="installation-main-text">{{ row.constructionPersonnel || '未填写施工人员' }}</div>
              <div class="installation-muted-line">{{ row.constructionPhone || '未填写联系电话' }}</div>
              <el-tooltip v-if="row.attachmentUrl" :disabled="canDownload" content="暂无 installation:attachment:download 权限"><span><el-button
                link
                type="primary"
                :disabled="!canDownload"
                @click="openAttachment(row.attachmentUrl, row.attachmentName)"
              >
                {{ attachmentLabel(row.attachmentName) }}
              </el-button></span></el-tooltip>
              <span v-else class="installation-empty-attachment">暂无附件</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-tooltip :disabled="canUpdate" content="暂无 installation:update 权限"><span><el-button link type="primary" :disabled="!canUpdate" @click="openEditor(row)">处理</el-button></span></el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无安装任务" />
          </template>
        </el-table>

        <div class="installation-pagination">
          <span>共 {{ pagination.total }} 条</span>
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="filters.current"
            :page-size="filters.size"
            :total="pagination.total"
            @current-change="changePage"
          />
        </div>
        </template>
      </section>
    </div>

    <el-dialog
      v-model="editorVisible"
      title="安装任务"
      width="760px"
      :before-close="beforeCloseEditor"
      :close-on-click-modal="!saving && !attachmentUploading"
    >
      <div class="installation-modal-subtitle">
        <span>{{ editorForm.orderId }}</span>
        <span>{{ editorForm.customerName || '未填写客户' }}</span>
        <span>{{ editorForm.projectName || '未填写项目' }}</span>
        <span>{{ editorForm.deliveryDate || '未填写交付日期' }}</span>
      </div>

      <div class="installation-editor-summary">
        <div>
          <small>当前状态</small>
          <strong>{{ statusLabel(editorForm.status) }}</strong>
        </div>
        <div>
          <small>施工人员</small>
          <strong>{{ editorForm.constructionPersonnel || '待填写' }}</strong>
        </div>
        <div>
          <small>附件</small>
          <strong>{{ editorForm.attachmentName ? '已上传' : '未上传' }}</strong>
        </div>
      </div>

      <el-form :model="editorForm" label-position="top" class="installation-form-grid">
        <el-form-item label="安装状态">
          <el-select v-model="editorForm.status">
            <el-option
              v-for="status in taskStatuses"
              :key="status.value"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物流公司">
          <el-input v-model.trim="editorForm.expressCompany" placeholder="请输入物流公司" />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model.trim="editorForm.expressNo" placeholder="请输入物流单号" />
        </el-form-item>
        <el-form-item label="施工人员">
          <el-input v-model.trim="editorForm.constructionPersonnel" placeholder="请输入施工人员信息" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model.trim="editorForm.constructionPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="施工备注" class="installation-field-full">
          <el-input
            v-model.trim="editorForm.constructionRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入施工备注"
          />
        </el-form-item>
        <el-form-item label="特殊及异常情况说明" class="installation-field-full">
          <el-input
            v-model.trim="editorForm.specialExceptionNote"
            type="textarea"
            :rows="3"
            placeholder="请输入特殊情况、现场异常或需要后续跟进的说明"
          />
        </el-form-item>
        <el-form-item label="验收附件" class="installation-field-full">
          <el-tooltip :disabled="canAttach" content="上传附件需要 installation:update 与 installation:attachment:upload 权限"><div><DragAttachmentUpload
            title="上传施工照片、验收单或交付凭证"
            :disabled="!canAttach"
            :uploading="attachmentUploading"
            :download-disabled="!canDownload"
            :remove-disabled="!canUpdate"
            disabled-reason="当前账号暂无对应附件权限"
            :file-name="editorForm.attachmentName"
            :file-url="editorForm.attachmentUrl"
            :file-size="editorForm.attachmentSize"
            @select="uploadAttachment"
            @download="openAttachment(editorForm.attachmentUrl, editorForm.attachmentName)"
            @remove="removeAttachment"
          /></div></el-tooltip>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="saving || attachmentUploading" @click="closeEditor">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="attachmentUploading || !canUpdate"
          @click="submitEditor"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import { useUserStore } from '@/stores/user'
import { createLatestRequest } from '@/utils/task7LatestRequest'
import { resolveInstallationAccess } from './installationAccess.js'
import {
  downloadInstallationTaskAttachment,
  getInstallationTaskPage,
  updateInstallationTaskStatus,
  uploadInstallationTaskAttachment
} from './api/installationTask'

defineOptions({ name: 'InstallationTask' })

const taskStatuses = [
  { value: 'production_completed', label: '生产完成' },
  { value: 'shipped_pending_install', label: '已发货待安装' },
  { value: 'completed_accepted', label: '已完成已验收' }
]

const filters = reactive({
  current: 1,
  size: 10,
  status: '',
  keyword: '',
  customerName: '',
  projectName: ''
})

const pagination = reactive({
  total: 0,
  pages: 0
})

const rows = ref([])
const userStore = useUserStore()
const listRequest = createLatestRequest()
const installationAccess = computed(() => resolveInstallationAccess((code) => userStore.hasPermission(code)))
const canUpdate = computed(() => installationAccess.value.canUpdate)
const canAttach = computed(() => installationAccess.value.canAttach)
const canDownload = computed(() => installationAccess.value.canDownload)
const requestState = ref('loading')
const requestErrorMessage = ref('')
const loading = computed(() => requestState.value === 'loading')
const editorVisible = ref(false)
const saving = ref(false)
const attachmentUploading = ref(false)
const editorForm = reactive({
  id: null,
  orderId: '',
  customerName: '',
  projectName: '',
  deliveryDate: '',
  status: 'production_completed',
  expressCompany: '',
  expressNo: '',
  constructionPersonnel: '',
  constructionPhone: '',
  constructionRemark: '',
  specialExceptionNote: '',
  attachmentName: '',
  attachmentUrl: '',
  attachmentSize: null
})

const statusMeta = {
  production_completed: {
    icon: 'inventory_2',
    hint: '生产完成待发货',
    summaryClass: 'installation-summary-production',
    rowClass: 'installation-row-production'
  },
  shipped_pending_install: {
    icon: 'local_shipping',
    hint: '已发货待安装',
    summaryClass: 'installation-summary-shipped',
    rowClass: 'installation-row-shipped'
  },
  completed_accepted: {
    icon: 'verified',
    hint: '已完成已验收',
    summaryClass: 'installation-summary-accepted',
    rowClass: 'installation-row-accepted'
  }
}

const statusCounts = computed(() => {
  return rows.value.reduce((map, row) => {
    const key = row.installationStatus || 'production_completed'
    map[key] = (map[key] || 0) + 1
    return map
  }, {})
})

const statusTabs = computed(() => {
  return [
    { value: '', label: '全部', count: pagination.total },
    ...taskStatuses.map((status) => ({
      ...status,
      count: statusCounts.value[status.value] || 0
    }))
  ]
})

const summaryCards = computed(() => {
  const attachmentCount = rows.value.filter((row) => row.attachmentUrl).length
  return [
    {
      key: 'all',
      label: '安装任务总数',
      count: pagination.total,
      hint: '当前筛选范围',
      status: '',
      icon: 'dataset',
      className: 'installation-summary-all'
    },
    ...taskStatuses.map((status) => ({
      key: status.value,
      label: status.label,
      count: statusCounts.value[status.value] || 0,
      hint: statusMeta[status.value]?.hint || '安装状态',
      status: status.value,
      icon: statusMeta[status.value]?.icon || 'radio_button_checked',
      className: statusMeta[status.value]?.summaryClass || ''
    })),
    {
      key: 'attachment',
      label: '本页附件',
      count: attachmentCount,
      hint: '含施工凭证',
      status: filters.status,
      icon: 'attach_file',
      className: 'installation-summary-attachment'
    }
  ]
})

onMounted(() => {
  loadTasks()
})

async function loadTasks() {
  const request = listRequest.begin()
  requestState.value = 'loading'
  requestErrorMessage.value = ''
  rows.value = []
  pagination.total = 0
  pagination.pages = 0
  try {
    const result = await getInstallationTaskPage({...filters})
    request.commit(() => {
      rows.value = result?.data || []
      pagination.total = Number(result?.total || 0)
      pagination.pages = Number(result?.pages || 0)
      requestState.value = 'ready'
    })
  } catch (error) {
    const failure = resolveRequestFailure(error)
    request.commit(() => {
      rows.value = []
      pagination.total = 0
      pagination.pages = 0
      requestState.value = failure.state
      requestErrorMessage.value = failure.message
    })
  }
}

function resolveRequestFailure(error) {
  const status = Number(error?.response?.status || 0)
  if (status === 401) {
    return { state: 'permission', message: '登录状态已失效，请重新登录后重试。' }
  }
  if (status === 403) {
    return { state: 'permission', message: '当前账号暂无安装任务查看权限，请联系管理员。' }
  }
  if (!error?.response) {
    return { state: 'error', message: '网络连接异常，请检查网络后重新加载。' }
  }
  if (status >= 500) {
    return { state: 'error', message: '服务暂时不可用，请稍后重新加载。' }
  }
  return { state: 'error', message: '安装任务加载失败，请重新加载。' }
}

function selectStatus(status) {
  filters.status = status
  filters.current = 1
  loadTasks()
}

function resetFilters() {
  filters.current = 1
  filters.status = ''
  filters.keyword = ''
  filters.customerName = ''
  filters.projectName = ''
  loadTasks()
}

function changePage(page) {
  filters.current = page
  loadTasks()
}

function openEditor(row) {
  if (!canUpdate.value) return
  editorForm.id = row.id
  editorForm.orderId = row.orderId
  editorForm.customerName = row.customerName || ''
  editorForm.projectName = row.projectName || ''
  editorForm.deliveryDate = row.deliveryDate || ''
  editorForm.status = row.installationStatus || 'production_completed'
  editorForm.expressCompany = row.expressCompany || ''
  editorForm.expressNo = row.expressNo || ''
  editorForm.constructionPersonnel = row.constructionPersonnel || ''
  editorForm.constructionPhone = row.constructionPhone || ''
  editorForm.constructionRemark = row.constructionRemark || ''
  editorForm.specialExceptionNote = row.specialExceptionNote || ''
  editorForm.attachmentName = row.attachmentName || ''
  editorForm.attachmentUrl = row.attachmentUrl || ''
  editorForm.attachmentSize = row.attachmentSize || null
  editorVisible.value = true
}

function canCloseEditor() {
  return !saving.value && !attachmentUploading.value
}

function closeEditor() {
  if (!canCloseEditor()) return
  editorVisible.value = false
}

function beforeCloseEditor(done) {
  if (!canCloseEditor()) return
  editorVisible.value = false
  if (typeof done === 'function') done()
}

async function submitEditor() {
  if (!canUpdate.value) return
  if (editorForm.status === 'shipped_pending_install' && (!editorForm.expressCompany.trim() || !editorForm.expressNo.trim())) {
    ElMessage.warning('已发货待安装状态需要填写物流信息')
    return
  }
  if (editorForm.status === 'completed_accepted' && !editorForm.constructionPersonnel.trim()) {
    ElMessage.warning('已完成已验收状态需要填写施工人员信息')
    return
  }
  saving.value = true
  try {
    await updateInstallationTaskStatus({
      id: editorForm.id,
      status: editorForm.status,
      expressCompany: editorForm.expressCompany,
      expressNo: editorForm.expressNo,
      constructionPersonnel: editorForm.constructionPersonnel,
      constructionPhone: editorForm.constructionPhone,
      constructionRemark: editorForm.constructionRemark,
      specialExceptionNote: editorForm.specialExceptionNote,
      attachmentName: editorForm.attachmentName,
      attachmentUrl: editorForm.attachmentUrl,
      attachmentSize: editorForm.attachmentSize
    })
    ElMessage.success('安装任务已更新')
    editorVisible.value = false
    await loadTasks()
  } finally {
    saving.value = false
  }
}

async function uploadAttachment(file) {
  if (!canAttach.value) return
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('附件不能超过 10MB')
    return
  }
  attachmentUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const result = await uploadInstallationTaskAttachment(formData)
    editorForm.attachmentName = result.fileName || file.name
    editorForm.attachmentUrl = result.fileUrl || ''
    editorForm.attachmentSize = result.fileSize || file.size
    ElMessage.success('附件上传成功')
  } finally {
    attachmentUploading.value = false
  }
}

function removeAttachment() {
  if (!canUpdate.value) return
  editorForm.attachmentName = ''
  editorForm.attachmentUrl = ''
  editorForm.attachmentSize = null
}

async function openAttachment(url, name) {
  if (!canDownload.value) return
  if (!url) return
  const blob = await downloadInstallationTaskAttachment({ url, name })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name || 'installation-task-attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

function statusLabel(status) {
  return taskStatuses.find((item) => item.value === status)?.label || '生产完成'
}

function statusClass(status) {
  return {
    'installation-status-production': status === 'production_completed',
    'installation-status-shipped': status === 'shipped_pending_install',
    'installation-status-accepted': status === 'completed_accepted'
  }
}

function statusIcon(status) {
  return statusMeta[status || 'production_completed']?.icon || 'radio_button_checked'
}

function rowAccentClass(status) {
  return statusMeta[status || 'production_completed']?.rowClass || 'installation-row-production'
}

function attachmentLabel(name) {
  const text = String(name || '查看附件')
  return text.length > 12 ? `${text.slice(0, 12)}...` : text
}

function formatDateTime(value) {
  if (!value) return '暂无时间'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.installation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.installation-panel {
  overflow: hidden;
  border: 1px solid rgba(31, 63, 95, 0.12);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.installation-filter-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
  padding: 18px;
  border-bottom: 1px solid rgba(31, 63, 95, 0.1);
  background: rgba(248, 250, 252, 0.72);
}

.installation-filter-grid > .box-input {
  grid-column: span 3;
}

.installation-filter-actions {
  grid-column: span 3;
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
}

.installation-primary-btn,
.installation-secondary-btn,
.installation-row-btn,
.installation-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 10px;
  font-weight: 800;
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.installation-primary-btn {
  min-height: 40px;
  padding: 0 18px;
  background: #1f3f5f;
  color: #fff;
  box-shadow: 0 12px 24px rgba(31, 63, 95, 0.18);
}

.installation-secondary-btn {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(31, 63, 95, 0.16);
  background: #fff;
  color: #1f2937;
}

.installation-row-btn {
  min-height: 34px;
  padding: 0 14px;
  background: rgba(31, 63, 95, 0.08);
  color: #1f3f5f;
}

.installation-icon-btn {
  width: 38px;
  height: 38px;
  background: rgba(31, 63, 95, 0.08);
  color: #1f3f5f;
}

.installation-primary-btn:disabled,
.installation-secondary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.installation-table thead {
  background: rgba(248, 250, 252, 0.8);
}

.installation-row:hover {
  background: rgba(31, 63, 95, 0.035);
}

.installation-status-pill {
  display: inline-flex;
  min-width: 96px;
  justify-content: center;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 900;
}

.installation-status-production {
  background: #e0f2fe;
  color: #0369a1;
}

.installation-status-shipped {
  background: #fef3c7;
  color: #92400e;
}

.installation-status-accepted {
  background: #dcfce7;
  color: #166534;
}

.installation-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 18px;
  border-top: 1px solid rgba(31, 63, 95, 0.1);
  color: rgba(31, 63, 95, 0.72);
  font-size: 13px;
  font-weight: 800;
}

.installation-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.42);
  padding: 20px;
}

.installation-modal {
  width: min(760px, 100%);
  max-height: 92vh;
  overflow-y: auto;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
}

.installation-modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid rgba(31, 63, 95, 0.1);
}

.installation-modal-head h2 {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 950;
  color: #0f172a;
}

.installation-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 22px 24px;
}

.installation-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  color: rgba(31, 63, 95, 0.72);
}

.installation-field-full {
  grid-column: 1 / -1;
}

.installation-textarea {
  min-height: 96px;
  resize: vertical;
}

.installation-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 24px 24px;
  border-top: 1px solid rgba(31, 63, 95, 0.1);
}

@media (max-width: 980px) {
  .installation-header {
    align-items: stretch;
    flex-direction: column;
  }

  .installation-filter-grid > .box-input,
  .installation-filter-actions {
    grid-column: span 12;
  }

  .installation-filter-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .installation-form-grid {
    grid-template-columns: 1fr;
  }

  .installation-pagination {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}

.installation-header {
  align-items: flex-end;
  border-radius: 18px;
  border: 1px solid rgba(31, 63, 95, 0.1);
  background:
    linear-gradient(135deg, rgba(31, 63, 95, 0.08), rgba(255, 255, 255, 0.96) 48%),
    #fff;
  padding: 22px 24px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.installation-header-desc {
  margin-top: 8px;
  max-width: 680px;
  color: rgba(71, 85, 105, 0.86);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.7;
}

.installation-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.installation-summary-card {
  --summary-color: #1f3f5f;
  --summary-bg: rgba(31, 63, 95, 0.08);
  display: flex;
  min-height: 132px;
  align-items: flex-start;
  gap: 14px;
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--summary-color) 18%, transparent);
  background: linear-gradient(145deg, var(--summary-bg), rgba(255, 255, 255, 0.98) 62%);
  padding: 18px;
  text-align: left;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.055);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.installation-summary-card:hover {
  border-color: color-mix(in srgb, var(--summary-color) 32%, transparent);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.installation-summary-card > .material-symbols-outlined {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--summary-color) 12%, white);
  color: var(--summary-color);
  font-size: 22px;
}

.installation-summary-card span:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.installation-summary-card small {
  color: rgba(71, 85, 105, 0.78);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.installation-summary-card strong {
  color: var(--summary-color);
  font-size: 34px;
  font-weight: 950;
  line-height: 1;
}

.installation-summary-card em {
  color: rgba(71, 85, 105, 0.72);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.installation-summary-all {
  --summary-color: #1f3f5f;
  --summary-bg: rgba(31, 63, 95, 0.08);
}

.installation-summary-production {
  --summary-color: #0369a1;
  --summary-bg: rgba(14, 165, 233, 0.11);
}

.installation-summary-shipped {
  --summary-color: #b45309;
  --summary-bg: rgba(245, 158, 11, 0.12);
}

.installation-summary-accepted {
  --summary-color: #15803d;
  --summary-bg: rgba(34, 197, 94, 0.11);
}

.installation-summary-attachment {
  --summary-color: #6d5d2f;
  --summary-bg: rgba(109, 93, 47, 0.1);
}

.installation-panel {
  border-radius: 18px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.07);
}

.installation-filter-grid {
  align-items: end;
  gap: 12px;
  padding: 18px 20px;
}

.installation-filter-field {
  display: flex;
  min-width: 0;
  grid-column: span 2;
  flex-direction: column;
  gap: 7px;
  color: rgba(31, 63, 95, 0.78);
  font-size: 12px;
  font-weight: 950;
}

.installation-filter-field-wide {
  grid-column: span 4;
}

.installation-filter-field .box-input {
  width: 100%;
}

.installation-filter-actions {
  grid-column: span 4;
}

.installation-table {
  min-width: 1060px;
}

.installation-table .th-cell {
  color: rgba(31, 63, 95, 0.7);
  font-size: 12px;
  letter-spacing: 0;
}

.installation-row {
  --row-accent: #1f3f5f;
  background: linear-gradient(90deg, color-mix(in srgb, var(--row-accent) 5%, white), #fff 18%);
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.installation-row > .td-cell:first-child {
  box-shadow: inset 4px 0 0 color-mix(in srgb, var(--row-accent) 58%, transparent);
}

.installation-row:hover {
  background: linear-gradient(90deg, color-mix(in srgb, var(--row-accent) 8%, white), #fff 24%);
}

.installation-row-production {
  --row-accent: #0369a1;
}

.installation-row-shipped {
  --row-accent: #b45309;
}

.installation-row-accepted {
  --row-accent: #15803d;
}

.installation-order-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.installation-order-code,
.installation-main-text {
  min-width: 0;
  color: rgb(var(--primary));
  font-size: 14px;
  font-weight: 950;
}

.installation-order-code {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.installation-quantity-chip {
  flex: 0 0 auto;
  border-radius: 999px;
  background: rgba(31, 63, 95, 0.08);
  padding: 3px 8px;
  color: rgba(31, 63, 95, 0.76);
  font-size: 11px;
  font-weight: 950;
}

.installation-muted-line {
  margin-top: 6px;
  max-width: 260px;
  overflow: hidden;
  color: rgba(71, 85, 105, 0.8);
  font-size: 12px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.installation-meta-line,
.installation-logistics {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  margin-top: 7px;
  color: rgba(100, 116, 139, 0.78);
  font-size: 11px;
  font-weight: 850;
}

.installation-meta-line span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.installation-meta-line .material-symbols-outlined,
.installation-logistics .material-symbols-outlined {
  flex: 0 0 auto;
  color: rgba(31, 63, 95, 0.5);
  font-size: 15px;
}

.installation-status-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.installation-status-stack small {
  color: rgba(100, 116, 139, 0.72);
  font-size: 11px;
  font-weight: 850;
}

.installation-status-pill {
  gap: 5px;
  min-width: 132px;
  border: 1px solid transparent;
}

.installation-status-pill .material-symbols-outlined {
  font-size: 15px;
}

.installation-status-production {
  border-color: rgba(3, 105, 161, 0.18);
  background: #e0f2fe;
  color: #0369a1;
}

.installation-status-shipped {
  border-color: rgba(180, 83, 9, 0.18);
  background: #fef3c7;
  color: #92400e;
}

.installation-status-accepted {
  border-color: rgba(21, 128, 61, 0.18);
  background: #dcfce7;
  color: #166534;
}

.installation-attachment-link,
.installation-empty-attachment {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  border-radius: 999px;
  padding: 5px 9px;
  font-size: 11px;
  font-weight: 950;
}

.installation-attachment-link {
  background: rgba(31, 63, 95, 0.08);
  color: rgb(var(--primary));
}

.installation-attachment-link .material-symbols-outlined {
  font-size: 14px;
}

.installation-empty-attachment {
  background: rgba(148, 163, 184, 0.12);
  color: rgba(100, 116, 139, 0.72);
}

.installation-row-btn {
  min-width: 82px;
  background: rgba(31, 63, 95, 0.1);
}

.installation-row-btn:hover,
.installation-primary-btn:hover,
.installation-secondary-btn:hover {
  transform: translateY(-1px);
}

.installation-modal {
  width: min(820px, 100%);
  border-radius: 18px;
}

.installation-modal-head {
  background: linear-gradient(135deg, rgba(31, 63, 95, 0.08), rgba(255, 255, 255, 0.98));
}

.installation-modal-subtitle {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 9px;
}

.installation-modal-subtitle span {
  border-radius: 999px;
  background: rgba(31, 63, 95, 0.08);
  padding: 4px 9px;
  color: rgba(31, 63, 95, 0.72);
  font-size: 12px;
  font-weight: 900;
}

.installation-editor-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 18px 24px 0;
}

.installation-editor-summary > div {
  border-radius: 14px;
  border: 1px solid rgba(31, 63, 95, 0.1);
  background: rgba(248, 250, 252, 0.84);
  padding: 14px;
}

.installation-editor-summary small {
  display: block;
  color: rgba(100, 116, 139, 0.74);
  font-size: 11px;
  font-weight: 950;
}

.installation-editor-summary strong {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: rgb(var(--primary));
  font-size: 15px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.installation-form-grid {
  padding-top: 18px;
}

@media (max-width: 1180px) {
  .installation-summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .installation-filter-field,
  .installation-filter-field-wide,
  .installation-filter-actions {
    grid-column: span 6;
  }
}

@media (max-width: 760px) {
  .installation-header {
    align-items: stretch;
    padding: 18px;
  }

  .installation-summary-grid,
  .installation-editor-summary {
    grid-template-columns: 1fr;
  }

  .installation-filter-field,
  .installation-filter-field-wide,
  .installation-filter-actions {
    grid-column: span 12;
  }

  .installation-filter-actions {
    justify-content: stretch;
  }

  .installation-filter-actions .installation-primary-btn,
  .installation-filter-actions .installation-secondary-btn {
    flex: 1;
  }
}
</style>
