<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <div class="function-page-container space-y-6 p-2 md:p-4">
      <header class="function-page-header">
        <div>
          <el-button class="inventory-back-btn" @click="goBack">
            <span class="material-symbols-outlined text-[18px]">arrow_back</span>
            返回库存管理
          </el-button>
          <div class="function-page-eyebrow mt-4">
            <span class="material-symbols-outlined text-[16px]">format_list_bulleted</span>
            单匹布明细
          </div>
          <h1 class="function-page-title break-all">{{ modelCode || '--' }}</h1>
          <p class="function-page-desc">
            按型号和规格查看每匹布的条码、米数、状态和自定义信息，避免在汇总抽屉中堆叠过长明细。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <el-select v-model="timeOrder" class="min-w-36" @change="fetchDetail">
            <el-option label="先进先出" value="fifo" />
            <el-option label="先进后出" value="lifo" />
          </el-select>
          <el-button type="primary" class="function-action-primary px-6 py-4" @click="fetchDetail">
            <span class="material-symbols-outlined text-[20px]">refresh</span>
            刷新明细
          </el-button>
        </div>
      </header>

      <section v-if="canReadInventory" class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div class="inventory-detail-stat">
          <p class="inventory-detail-stat-label">规格</p>
          <h3 class="inventory-detail-stat-value">{{ meter(spec) }}</h3>
        </div>
        <div class="inventory-detail-stat">
          <p class="inventory-detail-stat-label">布匹数</p>
          <h3 class="inventory-detail-stat-value">{{ detailRows.length }}</h3>
        </div>
        <div class="inventory-detail-stat">
          <p class="inventory-detail-stat-label">剩余米数</p>
          <h3 class="inventory-detail-stat-value text-blue-600">{{ meter(summaryRemainingMeters) }}</h3>
        </div>
      </section>

      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 bg-slate-50/70 px-6 py-5">
          <div>
            <h2 class="text-base font-black text-slate-900">单匹布列表</h2>
            <p class="mt-1 text-xs text-slate-500">当前筛选状态：{{ statusFilterLabel }} / {{ timeOrderLabel }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <TableColumnSettings
              :columns="inventoryDetailTableColumns"
              export-module="inventory"
              @move="moveInventoryDetailTableColumn"
              @reset="resetInventoryDetailTableColumns"
            />
            <span class="rounded-lg border border-slate-100 bg-white px-3 py-1.5 text-xs font-medium text-slate-500 shadow-sm">
              共 <b class="text-slate-800">{{ detailRows.length }}</b> 匹
            </span>
          </div>
        </div>

        <div v-if="!canReadInventory" class="min-h-[360px] p-8">
          <el-empty description="暂无 inventory:warning:list 权限，无法查看库存明细" />
        </div>
        <div v-else-if="listLoadError" class="min-h-[360px] p-8 text-center">
          <el-empty :description="listLoadError.message"><el-button type="primary" @click="fetchDetail">重试</el-button></el-empty>
        </div>
        <div v-else-if="loading" v-loading="loading" class="min-h-[360px]" element-loading-text="正在加载单匹布明细" />
        <div v-else class="responsive-table-wrap relative">
          <table v-if="detailRows.length" class="responsive-data-table w-full border-collapse text-left">
            <thead class="bg-slate-50/80">
              <tr>
                <th
                  v-for="column in inventoryDetailTableColumns"
                  :key="column.key"
                  class="inventory-th"
                  :class="column.align === 'right' ? 'text-right' : ''"
                >
                  {{ column.label }}
                </th>
                <th class="inventory-th text-right">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="cloth in detailRows"
                :key="cloth.id || cloth.barcode"
                class="cursor-pointer hover:bg-blue-50/40"
                @click="openClothDetail(cloth)"
              >
                <td
                  v-for="column in inventoryDetailTableColumns"
                  :key="column.key"
                  :data-label="column.label"
                  class="px-6 py-4"
                  :class="inventoryDetailCellClass(column.key)"
                >
                  <template v-if="column.key === 'barcode'">
                    <div class="max-w-[280px] break-all font-mono text-sm font-black text-slate-800">{{ cloth.barcode || '--' }}</div>
                  </template>
                  <template v-else-if="column.key === 'totalMeters'">{{ meter(cloth.totalMeters) }}</template>
                  <template v-else-if="column.key === 'remainingMeters'">{{ meter(cloth.remainingMeters) }}</template>
                  <template v-else-if="column.key === 'status'">
                    <el-tag :type="statusTagType(cloth.status)" size="small">
                      {{ cloth.statusName || statusLabel(cloth.status) }}
                    </el-tag>
                  </template>
                  <template v-else-if="column.key === 'inTime'">{{ formatDateTime(cloth.inTime) }}</template>
                  <template v-else-if="column.key === 'updateTime'">{{ formatDateTime(cloth.updateTime) }}</template>
                  <template v-else-if="column.key === 'customFields'">
                    <div class="flex max-w-[320px] flex-wrap gap-2">
                      <span
                        v-for="field in customInventoryFields"
                        :key="`${cloth.id || cloth.barcode}-${field.key}`"
                        class="rounded-lg bg-slate-50 px-2.5 py-1 text-[11px] font-bold text-slate-500"
                      >
                        {{ field.label }}：{{ customFieldValue(cloth, field) }}
                      </span>
                    </div>
                  </template>
                </td>
                <td class="px-6 py-4 text-right" data-label="操作">
                  <el-button
                    @click.stop="openClothDetail(cloth)"
                    class="mr-2 rounded-lg bg-slate-50 px-3 py-2 text-xs font-bold text-slate-700 transition-colors hover:bg-slate-100"
                  >
                    详情
                  </el-button>
                  <el-tooltip :disabled="canOutInventory" content="暂无 inventory:cloth:out 权限">
                  <span><el-button
                    @click.stop="openOutDrawer(cloth)"
                    :disabled="!canOutInventory || Number(cloth.remainingMeters || 0) <= 0"
                    class="rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700 transition-colors hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    扫码出库
                  </el-button></span></el-tooltip>
                </td>
              </tr>
            </tbody>
          </table>
          <el-empty v-else-if="listLoaded" description="暂无单匹布明细" />
        </div>
      </section>
    </div>

    <el-drawer v-model="outVisible" title="扫码出库" size="min(92vw, 480px)" destroy-on-close>
      <el-form :model="outForm" label-position="top">
      <div class="h-1.5 w-full bg-slate-800"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-slate-700">outbox</span>
            扫码出库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">请确认条码和本次出库米数。</p>
        </div>
        <el-button circle @click="outVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-6">
        <el-form-item label="布匹条码" required>
          <el-input v-model.trim="outForm.barcode" class="font-mono" placeholder="请将光标放在此处扫码" />
        </el-form-item>

        <div v-if="previewMatchesBarcode" class="relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4">
          <div class="absolute right-0 top-0 h-full w-1 bg-blue-500"></div>
          <div class="flex flex-col gap-2">
            <p class="flex justify-between text-sm">
              <span class="text-slate-500">{{ fieldLabel('modelCode', '型号') }}</span>
              <span class="font-bold text-slate-800">{{ outPreview.modelCode }}</span>
            </p>
            <p class="flex justify-between text-sm">
              <span class="text-slate-500">{{ fieldLabel('remainingMeters', '当前可出') }}</span>
              <span class="font-bold text-blue-600">{{ meter(outPreview.remainingMeters) }} 米</span>
            </p>
          </div>
        </div>

        <el-form-item :label="fieldLabel('remainingMeters', '出库米数')" required>
          <el-input-number v-model="outForm.meters" :min="0" :step="0.01" :precision="2" controls-position="right" class="w-full" />
        </el-form-item>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <el-button @click="outVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canOutInventory || !previewMatchesBarcode" @click="submitOut">确认出库</el-button>
      </div>
      </el-form>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="单匹布详情" size="min(92vw, 560px)" destroy-on-close @closed="closeClothDetail">
      <div class="h-1.5 w-full bg-slate-800"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-slate-700">receipt_long</span>
            单匹布详情
          </h3>
          <p class="mt-1.5 break-all text-xs text-slate-500">{{ currentCloth.barcode || '正在加载布匹条码...' }}</p>
        </div>
        <el-button circle @click="closeClothDetail" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>

      <div class="flex-1 overflow-y-auto p-6">
        <div v-if="detailLoading" v-loading="true" class="min-h-[320px]" element-loading-text="正在加载单匹详情" />
        <el-empty v-else-if="detailLoadError" :description="detailLoadError.message"><el-button type="primary" @click="retryClothDetail">重试</el-button></el-empty>
        <template v-else-if="clothDetail">
          <section class="inventory-detail-card">
            <div class="inventory-detail-card-head">
              <div>
                <p class="inventory-field-label mb-1">条码</p>
                <h4 class="break-all font-mono text-lg font-black text-slate-900">{{ currentCloth.barcode || '--' }}</h4>
              </div>
              <el-tag :type="statusTagType(currentCloth.status)" size="small">
                {{ currentCloth.statusName || statusLabel(currentCloth.status) }}
              </el-tag>
            </div>
            <div class="inventory-detail-grid">
              <p><span>型号</span><b>{{ currentCloth.modelCode || '--' }}</b></p>
              <p><span>规格</span><b>{{ meter(currentCloth.spec) }}</b></p>
              <p><span>总米数</span><b>{{ meter(currentCloth.totalMeters) }}</b></p>
              <p><span>剩余米数</span><b class="text-blue-600">{{ meter(currentCloth.remainingMeters) }}</b></p>
              <p><span>入库方式</span><b>{{ currentCloth.inType || '--' }}</b></p>
              <p><span>入库时间</span><b>{{ formatDateTime(currentCloth.inTime) }}</b></p>
              <p><span>最近更新</span><b>{{ formatDateTime(currentCloth.updateTime) }}</b></p>
              <p><span>出库时间</span><b>{{ formatDateTime(currentCloth.outTime) }}</b></p>
            </div>
            <div v-if="customInventoryFields.length" class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="field in customInventoryFields"
                :key="`detail-${field.key}`"
                class="rounded-lg bg-slate-50 px-2.5 py-1 text-[11px] font-bold text-slate-500"
              >
                {{ field.label }}：{{ customFieldValue(currentCloth, field) }}
              </span>
            </div>
          </section>

          <section class="mt-5">
            <div class="mb-3 flex items-center justify-between">
              <h4 class="text-base font-black text-slate-900">出入库流水</h4>
              <span class="rounded-lg bg-slate-50 px-2.5 py-1 text-xs font-bold text-slate-500">
                {{ currentRecords.length }} 条
              </span>
            </div>
            <div v-if="currentRecords.length" class="inventory-record-timeline">
              <article
                v-for="record in currentRecords"
                :key="record.id"
                class="inventory-record-item"
                :class="recordOperateClass(record.operateType)"
              >
                <div class="inventory-record-icon">
                  <span class="material-symbols-outlined text-[18px]">{{ recordOperateIcon(record.operateType) }}</span>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex flex-wrap items-center justify-between gap-2">
                    <strong>{{ record.operateTypeName || recordOperateLabel(record.operateType) }}</strong>
                    <span class="text-xs text-slate-400">{{ formatDateTime(record.createTime) }}</span>
                  </div>
                  <p class="mt-1 text-sm text-slate-600">
                    本次 {{ meter(record.operateMeters) }} 米，操作后剩余 {{ meter(record.remainingMeters) }} 米
                  </p>
                  <p class="mt-1 text-xs text-slate-400">操作人：{{ record.operatorName || '系统' }}</p>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无出入库流水" />
          </section>
        </template>
        <el-empty v-else description="暂无单匹详情" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElOption, ElSelect, ElTag, ElTooltip } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import { customTenantFields, defaultTenantFieldConfig, mergeTenantFieldConfig } from '@/utils/tenantFieldConfig'
import { getInventoryClothDetail, getInventoryModelDetail, outCloth } from './api/inventory.js'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const modelCode = computed(() => String(route.query.modelCode || '').trim())
const spec = computed(() => route.query.spec ?? '')
const status = computed(() => route.query.status === undefined || route.query.status === '' ? undefined : Number(route.query.status))
const timeOrder = ref(route.query.timeOrder === 'lifo' ? 'lifo' : 'fifo')
const detailRows = ref([])
const loading = ref(false)
const listLoadError = ref(null)
const listLoaded = ref(false)
let listRequestId = 0
const outVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailLoadError = ref(null)
let clothDetailRequestId = 0
const selectedCloth = ref(null)
const outPreview = ref(null)
const outPreviewBarcode = ref('')
const clothDetail = ref(null)
const outForm = reactive({ barcode: '', meters: '' })
const inventoryFieldConfig = ref(defaultTenantFieldConfig('inventory'))
const customInventoryFields = computed(() => customTenantFields(inventoryFieldConfig.value))
const defaultInventoryDetailTableColumns = computed(() => {
  const columns = [
    { key: 'barcode', label: '条码' },
    { key: 'totalMeters', label: fieldLabel('totalMeters', '总米数'), align: 'right' },
    { key: 'remainingMeters', label: fieldLabel('remainingMeters', '剩余米数'), align: 'right' },
    { key: 'status', label: '状态' },
    { key: 'inTime', label: '入库时间' },
    { key: 'updateTime', label: '更新时间' }
  ]
  if (customInventoryFields.value.length) {
    columns.push({ key: 'customFields', label: '自定义信息' })
  }
  return columns
})
const {
  orderedColumns: inventoryDetailTableColumns,
  moveColumn: moveInventoryDetailTableColumn,
  resetColumns: resetInventoryDetailTableColumns
} = useLocalTableColumns('inventory.model.detail', defaultInventoryDetailTableColumns)
const inventoryDetailTableColumnCount = computed(() => inventoryDetailTableColumns.value.length + 1)
const summaryRemainingMeters = computed(() => detailRows.value.reduce((sum, item) => sum + Number(item.remainingMeters || 0), 0))
const statusFilterLabel = computed(() => status.value === undefined ? '全部状态' : statusLabel(status.value))
const timeOrderLabel = computed(() => timeOrder.value === 'lifo' ? '先进后出' : '先进先出')
const currentCloth = computed(() => clothDetail.value?.cloth || {})
const currentRecords = computed(() => Array.isArray(clothDetail.value?.records) ? clothDetail.value.records : [])
const canReadInventory = computed(() => userStore.hasPermission('inventory:warning:list'))
const canOutInventory = computed(() => userStore.hasPermission('inventory:cloth:out'))
const previewMatchesBarcode = computed(() => Boolean(outPreview.value && outPreviewBarcode.value && outPreviewBarcode.value === outForm.barcode))

watch(() => outForm.barcode, (barcode) => {
  if (barcode !== outPreviewBarcode.value) outPreview.value = null
})

fetchFieldConfig()
fetchDetail()

async function fetchFieldConfig() {
  try {
    const rows = await getCurrentTenantFieldConfig('inventory')
    inventoryFieldConfig.value = mergeTenantFieldConfig('inventory', Array.isArray(rows) ? rows : [])
  } catch (error) {
    inventoryFieldConfig.value = defaultTenantFieldConfig('inventory')
  }
}

async function fetchDetail() {
  if (!modelCode.value) {
    ElMessage.warning('缺少型号参数')
    return
  }
  if (!canReadInventory.value) {
    detailRows.value = []
    listLoaded.value = false
    return
  }
  const requestId = ++listRequestId
  detailRows.value = []
  listLoadError.value = null
  listLoaded.value = false
  loading.value = true
  try {
    const result = await getInventoryModelDetail({
      modelCode: modelCode.value,
      spec: spec.value,
      status: status.value,
      timeOrder: timeOrder.value
    })
    if (requestId !== listRequestId) return
    detailRows.value = Array.isArray(result) ? result : []
    listLoaded.value = true
  } catch (error) {
    if (requestId !== listRequestId) return
    listLoadError.value = resolveLoadFailure(error, '库存明细')
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

function openOutDrawer(record) {
  if (!requireUiPermission('inventory:cloth:out')) return
  Object.assign(outForm, { barcode: record?.barcode || '', meters: record?.remainingMeters ? Number(record.remainingMeters) : null })
  outPreview.value = record || null
  outPreviewBarcode.value = record?.barcode || ''
  outVisible.value = true
}

async function openClothDetail(record) {
  if (!requireUiPermission('inventory:warning:list')) return
  if (!record?.id && !record?.barcode) {
    ElMessage.warning('缺少单匹布标识，无法查看详情')
    return
  }
  const requestId = ++clothDetailRequestId
  selectedCloth.value = record
  clothDetail.value = null
  detailLoadError.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const result = await getInventoryClothDetail({ id: record.id, barcode: record.barcode })
    if (requestId !== clothDetailRequestId) return
    clothDetail.value = result || null
  } catch (error) {
    if (requestId !== clothDetailRequestId) return
    detailLoadError.value = resolveLoadFailure(error, '单匹布详情')
  } finally {
    if (requestId === clothDetailRequestId) detailLoading.value = false
  }
}

function closeClothDetail() {
  clothDetailRequestId += 1
  detailVisible.value = false
  detailLoading.value = false
  detailLoadError.value = null
  clothDetail.value = null
  selectedCloth.value = null
}

function retryClothDetail() {
  if (selectedCloth.value) openClothDetail(selectedCloth.value)
}

async function submitOut() {
  if (!requireUiPermission('inventory:cloth:out')) return
  if (!outForm.barcode || Number(outForm.meters) <= 0) {
    ElMessage.warning('请填写条码和有效出库米数')
    return
  }
  if (!previewMatchesBarcode.value) {
    ElMessage.warning('条码已变化，请重新从列表选择并核对目标布匹')
    return
  }
  await outCloth({ barcode: outForm.barcode, meters: Number(outForm.meters) })
  ElMessage.success('出库成功')
  outVisible.value = false
  await fetchDetail()
}

function requireUiPermission(permission) {
  if (userStore.hasPermission(permission)) return true
  ElMessage.warning('当前账号暂无权限')
  return false
}

function resolveLoadFailure(error, label) {
  const statusCode = Number(error?.response?.status || error?.status || 0)
  if (statusCode === 401) return { kind: 'authentication', message: `${label}加载失败：登录状态已失效，请重新登录。` }
  if (statusCode === 403) return { kind: 'permission', message: `${label}加载失败：当前账号暂无权限。` }
  if (statusCode >= 500) return { kind: 'server', message: `${label}加载失败：服务暂时不可用，请稍后重试。` }
  return { kind: 'network', message: `${label}加载失败：网络异常，请检查连接后重试。` }
}

function statusTagType(value) {
  if (Number(value) === 0) return 'success'
  if (Number(value) === 2) return 'warning'
  return 'info'
}

function goBack() {
  router.push({ name: 'Inventory' })
}

function fieldLabel(key, fallback) {
  return inventoryFieldConfig.value[key]?.label || fallback || key
}

function inventoryDetailCellClass(key) {
  if (key === 'totalMeters') return 'text-right text-sm font-bold text-slate-700'
  if (key === 'remainingMeters') return 'text-right text-sm font-black text-blue-600'
  if (key === 'inTime' || key === 'updateTime') return 'text-xs text-slate-500'
  return ''
}

function customFieldValue(row, field) {
  const value = row?.customFields?.[field?.key]
  return value == null || value === '' ? '--' : value
}

function statusLabel(value) {
  if (Number(value) === 1) return '已出库'
  if (Number(value) === 2) return '部分出库'
  return '在库'
}

function statusClass(value) {
  if (Number(value) === 1) return 'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-200/60'
  if (Number(value) === 2) return 'bg-amber-50 text-amber-600 ring-1 ring-inset ring-amber-200/60'
  return 'bg-emerald-50 text-emerald-600 ring-1 ring-inset ring-emerald-200/60'
}

function recordOperateLabel(value) {
  if (Number(value) === 0) return '入库'
  if (Number(value) === 1) return '出库'
  if (Number(value) === 2) return '外部导入'
  return '库存操作'
}

function recordOperateIcon(value) {
  if (Number(value) === 1) return 'move_to_inbox'
  if (Number(value) === 2) return 'upload_file'
  return 'inventory'
}

function recordOperateClass(value) {
  if (Number(value) === 1) return 'is-out'
  if (Number(value) === 2) return 'is-import'
  return 'is-in'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--'
}

function meter(value) {
  return Number(value || 0).toFixed(2)
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

.inventory-back-btn {
  display: inline-flex;
  align-items: center;
  gap: .4rem;
  border-radius: .75rem;
  border: 1px solid rgb(226 232 240);
  background: #fff;
  padding: .55rem .9rem;
  font-size: .875rem;
  font-weight: 800;
  color: rgb(51 65 85);
  transition: background-color .2s ease, transform .15s ease;
}

.inventory-back-btn:hover {
  background: rgb(248 250 252);
  transform: translateY(-1px);
}

.inventory-detail-stat {
  border-radius: 1rem;
  border: 1px solid rgb(241 245 249);
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
}

.inventory-detail-stat-label {
  font-size: .75rem;
  font-weight: 800;
  letter-spacing: .08em;
  text-transform: uppercase;
  color: rgb(100 116 139);
}

.inventory-detail-stat-value {
  margin-top: .75rem;
  font-size: 2rem;
  font-weight: 900;
  color: rgb(15 23 42);
}

.inventory-th {
  padding: 1rem 1.5rem;
  font-size: .75rem;
  font-weight: 700;
  letter-spacing: .05em;
  text-transform: uppercase;
  color: rgb(100 116 139);
}

.inventory-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 100;
  display: flex;
  height: 100%;
  width: 100%;
  flex-direction: column;
  background: #fff;
  box-shadow: 0 25px 50px -12px rgb(15 23 42 / 0.25);
  transition: transform 0.3s ease-in-out;
}

@media (min-width: 640px) {
  .inventory-drawer {
    width: 420px;
  }

  .inventory-detail-drawer {
    width: min(560px, 92vw);
  }
}

.inventory-close-btn {
  border-radius: 9999px;
  padding: .375rem;
  color: rgb(148 163 184);
  transition: background-color .2s ease, color .2s ease;
}

.inventory-close-btn:hover {
  background: rgb(241 245 249);
  color: rgb(71 85 105);
}

.inventory-field-label {
  margin-bottom: .5rem;
  display: flex;
  align-items: center;
  gap: .25rem;
  font-size: .75rem;
  font-weight: 700;
  letter-spacing: .05em;
  text-transform: uppercase;
  color: rgb(51 65 85);
}

.inventory-input {
  width: 100%;
  border-radius: .75rem;
  border: 1px solid rgb(226 232 240);
  background: rgb(248 250 252 / 0.5);
  padding: .75rem 1rem;
  font-size: .875rem;
  outline: none;
  transition: border-color .2s ease, box-shadow .2s ease, background-color .2s ease;
}

.inventory-input:focus {
  border-color: rgb(16 185 129);
  background: #fff;
  box-shadow: 0 0 0 4px rgb(16 185 129 / 0.1);
}

.inventory-cancel-btn,
.inventory-confirm-btn {
  flex: 1;
  border-radius: .75rem;
  padding: .75rem 1rem;
  font-size: .875rem;
  font-weight: 700;
  transition: transform .15s ease, background-color .2s ease;
}

.inventory-cancel-btn {
  border: 1px solid rgb(226 232 240);
  background: #fff;
  color: rgb(51 65 85);
}

.inventory-cancel-btn:hover {
  background: rgb(241 245 249);
}

.inventory-confirm-btn {
  box-shadow: 0 10px 18px rgb(15 23 42 / 0.12);
}

.inventory-confirm-btn:active {
  transform: scale(0.95);
}

.inventory-detail-card {
  border-radius: 1rem;
  border: 1px solid rgb(226 232 240);
  background: linear-gradient(135deg, rgb(248 250 252), #fff);
  padding: 1rem;
}

.inventory-detail-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgb(226 232 240 / 0.7);
  padding-bottom: 1rem;
}

.inventory-detail-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: .75rem;
}

.inventory-detail-grid p {
  border-radius: .85rem;
  background: rgb(255 255 255 / .86);
  padding: .75rem;
}

.inventory-detail-grid span {
  display: block;
  font-size: .72rem;
  font-weight: 800;
  color: rgb(100 116 139);
}

.inventory-detail-grid b {
  margin-top: .25rem;
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgb(15 23 42);
}

.inventory-record-timeline {
  display: grid;
  gap: .75rem;
}

.inventory-record-item {
  display: flex;
  gap: .85rem;
  border-radius: 1rem;
  border: 1px solid rgb(226 232 240);
  background: #fff;
  padding: .9rem;
}

.inventory-record-icon {
  display: inline-flex;
  height: 2.4rem;
  width: 2.4rem;
  flex: 0 0 2.4rem;
  align-items: center;
  justify-content: center;
  border-radius: .85rem;
  background: rgb(239 246 255);
  color: rgb(37 99 235);
}

.inventory-record-item.is-out .inventory-record-icon {
  background: rgb(236 253 245);
  color: rgb(5 150 105);
}

.inventory-record-item.is-import .inventory-record-icon {
  background: rgb(245 243 255);
  color: rgb(109 40 217);
}
</style>
