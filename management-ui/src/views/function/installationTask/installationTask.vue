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
        <button class="installation-primary-btn" :disabled="loading" @click="loadTasks">
          <span class="material-symbols-outlined text-[20px]" :class="{ 'animate-spin': loading }">sync</span>
          刷新
        </button>
      </header>

      <section class="installation-summary-grid">
        <button
          v-for="card in summaryCards"
          :key="card.key"
          type="button"
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
        </button>
      </section>

      <section class="installation-panel">
        <div class="installation-filter-grid">
          <label class="installation-filter-field installation-filter-field-wide">
            <span>综合搜索</span>
            <input
              v-model.trim="filters.keyword"
              class="box-input"
              placeholder="订单号、客户、项目、品牌或物流单号"
              @keyup.enter="loadTasks"
            >
          </label>
          <label class="installation-filter-field">
            <span>客户</span>
            <input
              v-model.trim="filters.customerName"
              class="box-input"
              placeholder="客户名称"
              @keyup.enter="loadTasks"
            >
          </label>
          <label class="installation-filter-field">
            <span>项目</span>
            <input
              v-model.trim="filters.projectName"
              class="box-input"
              placeholder="项目名称"
              @keyup.enter="loadTasks"
            >
          </label>
          <div class="installation-filter-actions">
            <button class="installation-primary-btn" @click="loadTasks">
              <span class="material-symbols-outlined text-[18px]">search</span>
              查询
            </button>
            <button class="installation-secondary-btn" @click="resetFilters">
              <span class="material-symbols-outlined text-[18px]">restart_alt</span>
              重置
            </button>
          </div>
        </div>

        <div class="responsive-table-wrap">
          <table class="responsive-data-table installation-table w-full text-left">
            <thead>
            <tr>
              <th class="th-cell">订单信息</th>
              <th class="th-cell">客户项目</th>
              <th class="th-cell">交付物流</th>
              <th class="th-cell">安装状态</th>
              <th class="th-cell">施工信息</th>
              <th class="th-cell text-right">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="loading">
              <td class="td-cell text-center text-on-surface-variant" colspan="6">加载中...</td>
            </tr>
            <tr v-else-if="!rows.length">
              <td class="td-cell text-center text-on-surface-variant" colspan="6">暂无安装任务</td>
            </tr>
            <tr v-for="row in rows" v-else :key="row.id" class="installation-row" :class="rowAccentClass(row.installationStatus)">
              <td class="td-cell" data-label="订单信息">
                <div class="installation-order-cell">
                  <span class="installation-order-code">{{ row.orderId }}</span>
                  <span class="installation-quantity-chip">数量 {{ row.totalQuantity || 0 }}</span>
                </div>
                <div class="installation-muted-line">{{ row.goodsDesc || '暂无商品描述' }}</div>
                <div class="installation-meta-line">
                  <span>{{ row.brandName || '未填写品牌' }}</span>
                  <span>{{ formatDateTime(row.createTime) }}</span>
                </div>
              </td>
              <td class="td-cell" data-label="客户项目">
                <div class="installation-main-text">{{ row.customerName || '未填写客户' }}</div>
                <div class="installation-muted-line">{{ row.projectName || '未填写项目' }}</div>
                <div class="installation-meta-line">
                  <span class="material-symbols-outlined">location_on</span>
                  <span>{{ row.deliveryAddress || '未填写安装地址' }}</span>
                </div>
              </td>
              <td class="td-cell" data-label="信息渠道与物流">
                <div class="installation-main-text">{{ row.informationChannel || '未填写信息渠道' }}</div>
                <div class="installation-logistics">
                  <span class="material-symbols-outlined">local_shipping</span>
                  <span>
                    {{ row.expressCompany || '未填写物流' }}
                    <template v-if="row.expressNo"> / {{ row.expressNo }}</template>
                  </span>
                </div>
              </td>
              <td class="td-cell" data-label="安装状态">
                <div class="installation-status-stack">
                  <span class="installation-status-pill" :class="statusClass(row.installationStatus)">
                    <span class="material-symbols-outlined">{{ statusIcon(row.installationStatus) }}</span>
                    {{ statusLabel(row.installationStatus) }}
                  </span>
                  <small>更新 {{ formatDateTime(row.updateTime) }}</small>
                </div>
              </td>
              <td class="td-cell" data-label="施工信息">
                <div class="installation-main-text">{{ row.constructionPersonnel || '未填写施工人员' }}</div>
                <div class="installation-muted-line">{{ row.constructionPhone || '未填写联系电话' }}</div>
                <button
                  v-if="row.attachmentUrl"
                  type="button"
                  class="installation-attachment-link"
                  @click="openAttachment(row.attachmentUrl, row.attachmentName)"
                >
                  <span class="material-symbols-outlined">attach_file</span>
                  {{ attachmentLabel(row.attachmentName) }}
                </button>
                <div v-else class="installation-empty-attachment">暂无附件</div>
              </td>
              <td class="td-cell text-right" data-label="操作">
                <button class="installation-row-btn" @click="openEditor(row)">
                  <span class="material-symbols-outlined text-[18px]">edit_square</span>
                  处理
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="installation-pagination">
          <span>共 {{ pagination.total }} 条</span>
          <button class="installation-secondary-btn" :disabled="filters.current <= 1" @click="changePage(filters.current - 1)">上一页</button>
          <span>{{ filters.current }} / {{ pagination.pages || 1 }}</span>
          <button class="installation-secondary-btn" :disabled="filters.current >= (pagination.pages || 1)" @click="changePage(filters.current + 1)">下一页</button>
        </div>
      </section>
    </div>

    <div v-if="editorVisible" class="installation-modal-mask" @click.self="closeEditor">
      <div class="installation-modal">
        <div class="installation-modal-head">
          <div>
            <p class="function-page-eyebrow">安装任务</p>
            <h2>{{ editorForm.orderId }}</h2>
            <div class="installation-modal-subtitle">
              <span>{{ editorForm.customerName || '未填写客户' }}</span>
              <span>{{ editorForm.projectName || '未填写项目' }}</span>
              <span>{{ editorForm.informationChannel || '未填写信息渠道' }}</span>
            </div>
          </div>
          <button class="installation-icon-btn" @click="closeEditor">
            <span class="material-symbols-outlined">close</span>
          </button>
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

        <div class="installation-form-grid">
          <label class="installation-field">
            <span>安装状态</span>
            <select v-model="editorForm.status" class="box-input">
              <option v-for="status in taskStatuses" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </label>
          <label class="installation-field">
            <span>物流公司</span>
            <input v-model.trim="editorForm.expressCompany" class="box-input" placeholder="请输入物流公司">
          </label>
          <label class="installation-field">
            <span>物流单号</span>
            <input v-model.trim="editorForm.expressNo" class="box-input" placeholder="请输入物流单号">
          </label>
          <label class="installation-field">
            <span>施工人员</span>
            <input v-model.trim="editorForm.constructionPersonnel" class="box-input" placeholder="请输入施工人员信息">
          </label>
          <label class="installation-field">
            <span>联系电话</span>
            <input v-model.trim="editorForm.constructionPhone" class="box-input" placeholder="请输入联系电话">
          </label>
          <label class="installation-field installation-field-full">
            <span>施工备注</span>
            <textarea v-model.trim="editorForm.constructionRemark" class="box-input installation-textarea" placeholder="请输入施工备注"></textarea>
          </label>
          <label class="installation-field installation-field-full">
            <span>特殊及异常情况说明</span>
            <textarea v-model.trim="editorForm.specialExceptionNote" class="box-input installation-textarea" placeholder="请输入特殊情况、现场异常或需要后续跟进的说明"></textarea>
          </label>
          <div class="installation-field installation-field-full">
            <span>验收附件</span>
            <DragAttachmentUpload
              title="上传施工照片、验收单或交付凭证"
              :uploading="attachmentUploading"
              :file-name="editorForm.attachmentName"
              :file-url="editorForm.attachmentUrl"
              :file-size="editorForm.attachmentSize"
              @select="uploadAttachment"
              @download="openAttachment(editorForm.attachmentUrl, editorForm.attachmentName)"
              @remove="removeAttachment"
            />
          </div>
        </div>

        <div class="installation-modal-actions">
          <button class="installation-secondary-btn" :disabled="saving" @click="closeEditor">取消</button>
          <button class="installation-primary-btn" :disabled="saving || attachmentUploading" @click="submitEditor">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
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
const loading = ref(false)
const editorVisible = ref(false)
const saving = ref(false)
const attachmentUploading = ref(false)
const editorForm = reactive({
  id: null,
  orderId: '',
  customerName: '',
  projectName: '',
  informationChannel: '',
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
  loading.value = true
  try {
    const result = await getInstallationTaskPage({...filters})
    rows.value = result?.data || []
    pagination.total = Number(result?.total || 0)
    pagination.pages = Number(result?.pages || 0)
  } finally {
    loading.value = false
  }
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
  editorForm.id = row.id
  editorForm.orderId = row.orderId
  editorForm.customerName = row.customerName || ''
  editorForm.projectName = row.projectName || ''
  editorForm.informationChannel = row.informationChannel || ''
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

function closeEditor() {
  if (saving.value || attachmentUploading.value) return
  editorVisible.value = false
}

async function submitEditor() {
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
  editorForm.attachmentName = ''
  editorForm.attachmentUrl = ''
  editorForm.attachmentSize = null
}

async function openAttachment(url, name) {
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
