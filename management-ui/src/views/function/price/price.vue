<template>
  <div class="function-page-shell function-page-shell--compact h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div><div class="function-page-eyebrow"><span class="material-symbols-outlined">sell</span>价格策略中心</div><h1 class="function-page-title">价格管理</h1><p class="function-page-desc">维护面料 SKU 基准价、客户等级价和指定客户特价。</p></div>
        <div class="flex flex-wrap items-center gap-3">
          <el-button :loading="downloadingTemplate" :disabled="downloadingTemplate" @click="downloadTemplate">导入模板</el-button>
          <el-tooltip :disabled="canPublish" content="缺少价格发布权限"><span><el-upload action="#" accept=".xlsx" :auto-upload="false" :show-file-list="false" :disabled="!canPublish || importing" :on-change="handleImportUpload"><el-button :loading="importing" :disabled="!canPublish || importing">导入价格</el-button></el-upload></span></el-tooltip>
          <el-button :loading="exporting" :disabled="exporting" @click="exportExcel">导出 Excel</el-button>
          <el-tooltip :disabled="canPublish" content="缺少价格发布权限"><span><el-button type="primary" :disabled="!canPublish" @click="openCreate()">新增价格</el-button></span></el-tooltip>
        </div>
      </header>
      <el-result v-if="statsError" :icon="statsError.icon" :title="statsError.title" :sub-title="statsError.message"><template #extra><el-button @click="fetchStats">重试统计</el-button></template></el-result>
      <section v-else v-loading="statsLoading" class="function-stats-grid">
        <el-statistic class="function-stat-card" title="SKU 数量" :value="stats.skuCount" />
        <el-statistic class="function-stat-card" title="平均基准价" :precision="2" :value="Number(stats.averagePrice ?? 0)" />
        <el-statistic class="function-stat-card" title="计划中价格" :value="stats.pendingCount" />
        <el-statistic class="function-stat-card" title="客户特价" :value="stats.overrideCount" />
      </section>
      <section class="function-list-panel">
        <el-form v-filter-collapse :model="query" inline class="function-filter-form p-4">
          <div class="price-filter-group"><el-form-item><el-input v-model.trim="query.keyword" aria-label="价格关键词" placeholder="搜索型号、批号、规格" @keyup.enter="handleFilter" /></el-form-item><el-form-item><el-select v-model="query.status" aria-label="价格状态" placeholder="全部状态" clearable><el-option :value="1" label="生效中" /><el-option :value="2" label="计划中" /><el-option :value="0" label="已过期" /></el-select></el-form-item><el-form-item><el-input v-model.trim="query.batchNo" aria-label="价格批号" placeholder="批号" /></el-form-item><el-form-item><el-input v-model.trim="query.spec" aria-label="价格规格" placeholder="规格" /></el-form-item><el-form-item><el-select v-model="query.currency" aria-label="价格币种" placeholder="全部币种" clearable><el-option value="CNY" label="CNY" /><el-option value="USD" label="USD" /></el-select></el-form-item></div>
          <div class="price-filter-group"><el-form-item><el-input-number v-model="query.priceMin" aria-label="最低价格" :min="0" :precision="2" placeholder="最低价" /></el-form-item><el-form-item><el-input-number v-model="query.priceMax" aria-label="最高价格" :min="0" :precision="2" placeholder="最高价" /></el-form-item></div>
          <div class="price-filter-group"><el-form-item><el-date-picker v-model="query.effectiveStart" aria-label="生效开始日期" type="date" value-format="YYYY-MM-DD" placeholder="生效开始" /></el-form-item><el-form-item><el-date-picker v-model="query.effectiveEnd" aria-label="生效结束日期" type="date" value-format="YYYY-MM-DD" placeholder="生效结束" /></el-form-item></div>
          <div class="function-filter-actions"><el-button type="primary" @click="handleFilter">查询</el-button><el-button @click="resetFilter">重置</el-button><TableColumnSettings :columns="priceTableColumns" :exportable="false" @move="movePriceTableColumn" @reset="resetPriceTableColumns" /></div>
        </el-form>
        <el-result v-if="requestError" :icon="requestError.icon" :title="requestError.title" :sub-title="requestError.message"><template #extra><el-button type="primary" @click="retry">重试</el-button></template></el-result>
        <template v-else>
          <div class="function-table-scroll">
            <el-table v-loading="loading" :data="rows" row-key="id" @row-click="openDetail">
              <el-table-column v-for="column in priceTableColumns" :key="column.key" :label="column.label" :align="column.align" min-width="130"><template #default="{ row }"><template v-if="column.key === 'basePrice'">¥{{ money(row.basePrice) }}</template><el-tag v-else-if="column.key === 'status'" :type="Number(row.status) === 1 ? 'success' : Number(row.status) === 2 ? 'warning' : 'info'">{{ row.statusLabel }}</el-tag><template v-else>{{ row[column.key] || '--' }}</template></template></el-table-column>
              <el-table-column label="操作" fixed="right" width="200"><template #default="{ row }"><el-tooltip :disabled="canViewDetail" content="缺少价格详情权限"><span><el-button link type="primary" :disabled="!canViewDetail" @click.stop="openDetail(row)">详情</el-button></span></el-tooltip><el-tooltip :disabled="canAdjust" content="调整价格需要 price:publish 和 price:detail 权限"><span><el-button link type="primary" :disabled="!canAdjust" @click.stop="openCreate(row)">调整</el-button></span></el-tooltip><el-tooltip :disabled="canDelete" content="缺少价格删除权限"><span><el-button link type="danger" :loading="deletingId === row.id" :disabled="!canDelete || deletingId !== null" @click.stop="remove(row)">删除</el-button></span></el-tooltip></template></el-table-column>
              <template #empty><el-empty description="暂无价格记录" /></template>
            </el-table>
          </div>
        </template>
        <div v-if="!requestError" class="flex justify-end p-4"><el-pagination v-model:current-page="query.page" :page-size="query.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="changePage" /></div>
      </section>
    </div>
    <PriceCreateDrawer :is-visible="createVisible" :sku-data="editingSku" :can-publish="canPublish" @close="closeCreate" @success="handleSaved" />
    <el-drawer v-model="detailVisible" title="价格详情" size="440px" @closed="closeDetail">
      <div v-loading="detailLoading"><el-result v-if="detailError" :icon="detailError.icon" :title="detailError.title" :sub-title="detailError.message"><template #extra><el-button @click="retryDetail">重试</el-button></template></el-result><template v-else-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="型号">{{ detail.modelCode }}</el-descriptions-item><el-descriptions-item label="规格">{{ detail.spec || '--' }}</el-descriptions-item><el-descriptions-item label="基准价">¥{{ money(detail.basePrice) }}</el-descriptions-item><el-descriptions-item label="生效日期">{{ detail.effectiveDate }}</el-descriptions-item></el-descriptions><h3>客户等级价格</h3><el-table :data="detail?.tierPrices || []" size="small"><el-table-column prop="tierName" label="等级" /><el-table-column label="价格"><template #default="{ row }">¥{{ money(row.finalPrice ?? row.fixedPrice) }}</template></el-table-column></el-table><h3>客户特价</h3><el-table :data="detail?.overrides || []" size="small"><el-table-column prop="customerName" label="客户" /><el-table-column label="价格"><template #default="{ row }">¥{{ money(row.price) }}</template></el-table-column></el-table><h3>调整日志</h3><el-table :data="detail?.logs || []" size="small"><el-table-column prop="createTime" label="时间" /><el-table-column prop="remark" label="备注" /><el-table-column label="原价/新价"><template #default="{ row }">¥{{ money(row.oldPrice) }} → ¥{{ money(row.newPrice) }}</template></el-table-column></el-table></template><el-empty v-else-if="!detailLoading" description="暂无详情" /></div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElDatePicker, ElDescriptions, ElDescriptionsItem, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElPagination, ElResult, ElSelect, ElStatistic, ElTable, ElTableColumn, ElTag, ElTooltip, ElUpload } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import PriceCreateDrawer from './priceCreate.vue'
import { createLatestRequestGate, normalizeOptionalNumber } from './priceBehavior.js'
import { deletePrice, downloadPriceImportTemplate, exportPriceExcel, getPriceDetail, getPricePage, getPriceStats, importPrices } from './api/price.js'

const route = useRoute()
const userStore = useUserStore()
const canViewDetail = computed(() => userStore.hasPermission('price:detail'))
const canPublish = computed(() => userStore.hasPermission('price:publish'))
const canAdjust = computed(() => canPublish.value && canViewDetail.value)
const canDelete = computed(() => userStore.hasPermission('price:delete'))
const defaultPriceTableColumns = [{ key: 'modelCode', label: '面料型号' }, { key: 'spec', label: '规格说明' }, { key: 'basePrice', label: '基准价', align: 'right' }, { key: 'currency', label: '币种' }, { key: 'effectiveDate', label: '生效日期' }, { key: 'status', label: '状态' }]
const { orderedColumns: priceTableColumns, moveColumn: movePriceTableColumn, resetColumns: resetPriceTableColumns } = useLocalTableColumns('price.list', defaultPriceTableColumns)
const loading = ref(false), rows = ref([]), requestError = ref(null)
const statsLoading = ref(false), statsError = ref(null)
const stats = reactive({ skuCount: 0, averagePrice: 0, pendingCount: 0, overrideCount: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ page: 1, size: 10, keyword: '', status: '', batchNo: '', spec: '', currency: '', priceMin: undefined, priceMax: undefined, effectiveStart: '', effectiveEnd: '' })
const createVisible = ref(false), editingSku = ref(null), detailVisible = ref(false), detail = ref(null), detailLoading = ref(false), detailError = ref(null), detailItem = ref(null)
const deletingId = ref(null), importing = ref(false), exporting = ref(false), downloadingTemplate = ref(false)
let detailRequestId = 0
const listGate = createLatestRequestGate(), statsGate = createLatestRequestGate()
const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))

function errorState(error) { const status = Number(error?.response?.status); if (status === 401) return { icon: 'warning', title: '登录已失效', message: '请重新登录后重试。' }; if (status === 403) return { icon: 'warning', title: '无权访问', message: '当前账号缺少此操作权限。' }; if (status >= 500) return { icon: 'error', title: '服务暂时不可用', message: '服务器处理失败，请稍后重试。' }; return { icon: 'error', title: '加载失败', message: '网络异常，请检查连接后重试。' } }
async function fetchData() { const requestId = listGate.begin(); loading.value = true; requestError.value = null; rows.value = []; try { const data = await getPricePage(normalizedQuery()); if (!listGate.isCurrent(requestId)) return; rows.value = data.data || []; pagination.total = Number(data.total || 0); pagination.pages = Number(data.pages || 0) } catch (error) { if (!listGate.isCurrent(requestId)) return; pagination.total = 0; pagination.pages = 0; requestError.value = errorState(error) } finally { if (listGate.isCurrent(requestId)) loading.value = false } }
async function fetchStats() { const requestId = statsGate.begin(); statsLoading.value = true; statsError.value = null; try { const data = await getPriceStats(); if (statsGate.isCurrent(requestId)) Object.assign(stats, data) } catch (error) { if (statsGate.isCurrent(requestId)) statsError.value = errorState(error) } finally { if (statsGate.isCurrent(requestId)) statsLoading.value = false } }
function retry() { return Promise.all([fetchData(), fetchStats()]) }
function handleFilter() { query.page = 1; fetchData() }
function resetFilter() { Object.assign(query, { page: 1, keyword: '', status: '', batchNo: '', spec: '', currency: '', priceMin: undefined, priceMax: undefined, effectiveStart: '', effectiveEnd: '' }); fetchData() }
function normalizedQuery() { return { ...query, status: normalizeOptionalNumber(query.status), batchNo: query.batchNo || undefined, spec: query.spec || undefined, currency: query.currency || undefined, priceMin: normalizeOptionalNumber(query.priceMin), priceMax: normalizeOptionalNumber(query.priceMax), effectiveStart: query.effectiveStart || undefined, effectiveEnd: query.effectiveEnd || undefined } }
function changePage(page) { if (page < 1 || page > totalPages.value) return; query.page = page; fetchData() }
function openCreate(item) { if (item && !canAdjust.value) return; if (!item && !canPublish.value) return; editingSku.value = item || null; createVisible.value = true }
function closeCreate() { createVisible.value = false; editingSku.value = null }
async function handleSaved() { closeCreate(); await Promise.all([fetchData(), fetchStats()]); ElMessage.success('价格已保存。') }
async function openDetail(item) { if (!canViewDetail.value) return; detailItem.value = item; detailVisible.value = true; detail.value = null; detailError.value = null; detailLoading.value = true; const requestId = ++detailRequestId; try { const result = await getPriceDetail(item.id); if (requestId === detailRequestId) detail.value = result } catch (error) { if (requestId === detailRequestId) detailError.value = errorState(error) } finally { if (requestId === detailRequestId) detailLoading.value = false } }
function retryDetail() { if (detailItem.value) openDetail(detailItem.value) }
function closeDetail() { detailRequestId += 1; detail.value = null; detailError.value = null; detailItem.value = null }
async function remove(item) { if (!canDelete.value || deletingId.value !== null) return; try { await ElMessageBox.confirm(`确认删除 ${item.modelCode} 的价格记录吗？`, '删除确认', { type: 'warning' }); deletingId.value = item.id; await deletePrice(item.id); ElMessage.success('价格记录已删除。'); await Promise.all([fetchData(), fetchStats()]) } catch (error) { if (error !== 'cancel' && error !== 'close') throw error } finally { deletingId.value = null } }
async function exportExcel() { if (exporting.value) return; exporting.value = true; try { downloadBlob(await exportPriceExcel(normalizedQuery()), `价格表-${Date.now()}.xlsx`) } finally { exporting.value = false } }
async function downloadTemplate() { if (downloadingTemplate.value) return; downloadingTemplate.value = true; try { downloadBlob(await downloadPriceImportTemplate(), '价格导入模板.xlsx') } finally { downloadingTemplate.value = false } }
async function handleImportUpload(uploadFile) { if (!canPublish.value || importing.value || !uploadFile.raw) return; importing.value = true; try { const result = await importPrices(uploadFile.raw); const failText = (result.failMessages || []).slice(0, 5).join('\n'); await ElMessageBox.alert(`总行数：${result.totalCount}\n成功：${result.successCount}\n失败：${result.failCount}${failText ? `\n\n失败明细：\n${failText}` : ''}`, '价格导入结果'); await Promise.all([fetchData(), fetchStats()]) } finally { importing.value = false } }
function downloadBlob(blob, fileName) { const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = fileName; link.click(); URL.revokeObjectURL(url) }
function money(value) { return Number(value ?? 0).toFixed(2) }
function applyRouteKeyword() { const value = String(route.query.keyword || route.query.q || '').trim(); if (value !== query.keyword) { query.keyword = value; query.page = 1 } }
onMounted(async () => { applyRouteKeyword(); await Promise.all([fetchData(), fetchStats()]) })
watch(() => [route.query.keyword, route.query.q], async () => { applyRouteKeyword(); await fetchData() })
</script>

<style scoped>
:deep(.el-form--inline.el-form) { grid-template-columns: minmax(0, 1fr); }
:deep(.el-form--inline .el-form-item) { margin-bottom: 0; }
.price-filter-group { display: grid; grid-template-columns: repeat(auto-fit, minmax(11rem, 15rem)); justify-content: start; gap: .75rem; min-width: 0; }
.price-filter-group:first-child { grid-template-columns: repeat(auto-fit, minmax(11rem, 15rem)); }
@media (max-width: 640px) { .price-filter-group, .price-filter-group:first-child { grid-template-columns: minmax(0, 1fr); } }
h3 { margin: 20px 0 8px; font-weight: 800; }
</style>
