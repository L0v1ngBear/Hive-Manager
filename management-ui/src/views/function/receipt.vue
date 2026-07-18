<template>
  <div class="receipt-page-shell function-page-shell font-body">
    <div class="function-page-container receipt-page-container space-y-6">
      <header class="receipt-hero function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">print</span>
            出库打印中心
          </div>
          <h1 class="function-page-title">出库单打印与模板</h1>
          <p class="function-page-desc">
            选择待打印出库单并套用模板，支持浏览器连续纸打印和模板内容自定义排版。
          </p>
        </div>

        <el-tabs v-model="activeMode" class="receipt-tabs" aria-label="出库单功能切换" @tab-change="handleModeChange">
          <el-tab-pane label="出库单打印" name="print" />
          <el-tab-pane label="模板设置" name="template" />
        </el-tabs>
      </header>

      <el-form v-if="activeMode === 'print'" class="receipt-print-profile-panel" label-position="top">
        <div class="profile-intro">
          <strong>浏览器打印适配</strong>
          <span>用于适配 241-1 连续纸、A4、热敏纸和不同打印驱动的边距偏移。</span>
        </div>
        <div class="receipt-print-profile-controls">
          <el-form-item label="纸宽(mm)"><el-input-number v-model="receiptPrintProfile.paperWidthMm" :min="20" :max="500" :step="0.1" @change="persistReceiptPrintProfile" /></el-form-item>
          <el-form-item label="纸高(mm)"><el-input-number v-model="receiptPrintProfile.paperHeightMm" :min="10" :max="500" :step="0.1" @change="persistReceiptPrintProfile" /></el-form-item>
          <el-form-item label="边距(mm)"><el-input-number v-model="receiptPrintProfile.pageMarginMm" :min="0" :max="30" :step="0.1" @change="persistReceiptPrintProfile" /></el-form-item>
          <el-form-item label="左右偏移(mm)"><el-input-number v-model="receiptPrintProfile.offsetXmm" :min="-50" :max="50" :step="0.1" @change="persistReceiptPrintProfile" /></el-form-item>
          <el-form-item label="上下偏移(mm)"><el-input-number v-model="receiptPrintProfile.offsetYmm" :min="-50" :max="50" :step="0.1" @change="persistReceiptPrintProfile" /></el-form-item>
          <el-form-item label="缩放"><el-input-number v-model="receiptPrintProfile.scale" :min="0.5" :max="1.5" :step="0.01" @change="persistReceiptPrintProfile" /></el-form-item>
        </div>
        <div class="receipt-print-profile-actions">
          <el-button @click="syncReceiptProfileWithTemplate">使用模板尺寸</el-button>
          <el-button @click="printReceiptCalibrationPage">校准页</el-button>
          <el-button @click="resetReceiptPrintProfile">恢复默认</el-button>
        </div>
      </el-form>

  <div v-if="activeMode === 'print'" class="receipt-workspace">
    <section class="queue-panel">
      <header class="queue-head">
        <div>
          <h2>待打印出库单</h2>
          <p>选择单据后预览，浏览器打印按 241-1 连续纸尺寸分页。</p>
        </div>
        <el-button circle :loading="isFetchingList" aria-label="刷新待打印出库单" @click="fetchPendingList">
          <span class="material-symbols-outlined">refresh</span>
        </el-button>
      </header>

      <div v-loading="isFetchingList" class="queue-list">
        <el-result v-if="listLoadError" icon="error" title="待打印出库单加载失败" :sub-title="listLoadError"><template #extra><el-button type="primary" @click="retryPendingList">重试</el-button></template></el-result>
        <el-empty v-else-if="pendingOrders.length === 0 && !isFetchingList" description="当前暂无待打印出库单" />

        <el-tooltip v-for="item in pendingOrders" :key="item.orderNo" :disabled="canViewDetail" content="暂无 print:receipt:detail 权限"><span><el-button
          v-if="!listLoadError"
          class="queue-card"
          :class="{ active: selectedOrder?.orderNo === item.orderNo }"
          :disabled="!canViewDetail"
          @click="selectOrder(item)"
        >
          <div class="queue-row">
            <strong>{{ item.orderNo }}</strong>
            <span>{{ formatDate(item.createTime) }}</span>
          </div>
          <p>客户：{{ item.customerName || '--' }}</p>
          <small>共 {{ item.itemCount || 0 }} 条，合计 {{ formatNumber(item.totalMeters) }} 米</small>
        </el-button></span></el-tooltip>
      </div>
    </section>

    <section class="preview-panel">
      <header class="preview-head">
        <div>
          <h2>出库单打印预览</h2>
          <p v-if="selectedOrder">共 {{ printPages.length }} 页，每页都是完整出库单格式。</p>
          <p v-else>请选择左侧待打印出库单。</p>
        </div>

        <div class="receipt-actions">
          <el-select v-if="selectedOrder" v-model="selectedTemplateId" class="template-select" @change="handleTemplateChange"><el-option v-for="template in receiptTemplates" :key="template.id" :value="template.id" :label="`${template.name}${template.isDefault === 1 ? '（默认）' : ''}`" /></el-select>
          <el-tooltip v-if="selectedOrder" :disabled="canExecute" content="暂无 print:receipt:execute 权限"><span><el-button type="primary" :disabled="!canExecute || isPrinting" :loading="isPrinting" @click="openBrowserPrint">
            <span class="material-symbols-outlined">print</span>
            浏览器打印
          </el-button></span></el-tooltip>
          <el-tooltip v-if="selectedOrder" :disabled="canCancel" content="暂无 print:receipt:cancel 权限"><span><el-button type="danger" plain :disabled="!canCancel || isSubmitting" @click="handleCancelPrint">
            <span class="material-symbols-outlined">block</span>
            作废/跳过
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canExecute" content="暂无 print:receipt:execute 权限"><span><el-button type="success" :disabled="!canExecute || !selectedOrder || isSubmitting" @click="confirmPrinted">
            <span class="material-symbols-outlined">task_alt</span>
            确认已打印
          </el-button></span></el-tooltip>
        </div>
      </header>

      <div v-loading="isLoadingDetail" class="preview-scroll" element-loading-text="正在生成打印预览...">
        <el-result v-if="detailLoadError" icon="error" title="出库单详情加载失败" :sub-title="detailLoadError"><template #extra><el-button type="primary" @click="retrySelectedOrder">重试</el-button></template></el-result>
        <el-empty v-else-if="!selectedOrder && !isLoadingDetail" description="请在左侧选择一张出库单" />

        <div v-else-if="selectedOrder && !isLoadingDetail" class="receipt-preview-layout">
          <div class="paper-preview-viewport">
            <div id="print-paper-area" class="paper-stack">
              <article v-for="page in printPages" :key="page.pageNo" class="receipt-page" :style="paperStyle">
              <header class="receipt-top">
                <div class="receipt-title-block">
                  <h1>{{ templateConfig.title }}</h1>
                  <p>{{ templateConfig.subtitle }}</p>
                </div>
              </header>

              <section class="receipt-info">
                <div class="customer-line">
                  <div>客户名称：<span>{{ printableText(printDraft.customerName) }}</span></div>
                  <div>单据编号：<span>{{ printDraft.orderNo || '--' }}</span></div>
                </div>
                <div class="order-line">
                  <span>项目名称：<b>{{ printableText(printDraft.projectName) }}</b></span>
                  <span>录单日期：<b>{{ formatDateOnly(printDraft.printDate || printDraft.createTime) }}</b></span>
                  <span>制单人：<b>{{ printDraft.operator || '--' }}</b></span>
                  <span>第 {{ page.pageNo }} 页 / 共 {{ page.totalPages }} 页</span>
                </div>
              </section>

              <table class="receipt-print-table">
                <colgroup>
                  <col
                    v-for="column in visibleColumns"
                    :key="`col-${column.key}`"
                    :style="{ width: columnWidth(column) }"
                  />
                </colgroup>
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
                  <b>{{ printableText(printDraft.logisticsCompany) }}</b>
                  <span>物流单号：</span>
                  <b>{{ printableText(printDraft.logisticsNo) }}</b>
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

          <aside class="print-editor">
            <div class="print-editor-head">
              <div>
                <strong>打印内容修正</strong>
                <span>保存后会回写出库单，并记录修改前后快照</span>
              </div>
              <div class="print-editor-actions">
                <el-tooltip :disabled="canUpdatePrint" content="暂无 print:receipt:update 权限"><span><el-button
                  v-if="timeCorrectionMode"
                  type="button"
                  class="editor-save-btn"
                  :disabled="!canUpdatePrint || isSubmitting"
                  @click="savePrintRevision({ timeCorrectionOnly: true })"
                >
                  保存时间
                </el-button></span></el-tooltip>
                <el-tooltip :disabled="canUpdatePrint" content="暂无 print:receipt:update 权限"><span><el-button type="primary" :disabled="!canUpdatePrint || isSubmitting" @click="savePrintRevision()">保存修正</el-button></span></el-tooltip>
                <el-button @click="resetPrintDraft">恢复系统内容</el-button>
              </div>
            </div>

            <BusinessTimeCorrectionPanel
              v-model="printDraft.printDate"
              :active="timeCorrectionMode"
              data-field="receipt.printDate"
              input-type="date"
              title="业务时间修正"
              label="业务日期"
              description="用于修正当前出库单的业务日期。"
            />

            <div class="print-editor-grid">
              <label>
                <span>客户名称</span>
                <el-input v-model.trim="printDraft.customerName" maxlength="80" placeholder="客户名称" />
              </label>
              <label>
                <span>打印单号</span>
                <el-input v-model.trim="printDraft.orderNo" maxlength="60" placeholder="打印显示单号" />
              </label>
              <label>
                <span>项目名称</span>
                <el-input v-model.trim="printDraft.projectName" maxlength="80" placeholder="项目名称，可留空" />
              </label>
              <label>
                <span>制单人</span>
                <el-input v-model.trim="printDraft.operator" maxlength="30" placeholder="制单人" />
              </label>
              <label>
                <span>收货仓库</span>
                <el-input v-model.trim="templateConfig.warehouse" maxlength="20" placeholder="收货仓库" />
              </label>
              <label>
                <span>物流公司</span>
                <el-input v-model.trim="printDraft.logisticsCompany" maxlength="40" placeholder="物流公司，可留空" />
              </label>
              <label>
                <span>物流单号</span>
                <el-input v-model.trim="printDraft.logisticsNo" maxlength="60" placeholder="物流单号，可留空" />
              </label>
            </div>

            <div class="print-editor-table-wrap">
              <div class="print-editor-table-head">
                <strong>明细行修正</strong>
                <el-tooltip :disabled="canUpdatePrint" content="暂无 print:receipt:update 权限"><span><el-button :disabled="!canUpdatePrint" @click="addPrintRow">新增打印行</el-button></span></el-tooltip>
              </div>
              <div class="print-editor-table">
                <div class="print-editor-row print-editor-row-head">
                  <span>货物名称</span>
                  <span>规格</span>
                  <span>米数</span>
                  <span>单价</span>
                  <span>金额</span>
                  <span>备注</span>
                  <span>操作</span>
                </div>
                <div v-for="(row, index) in printableRows" :key="getRowKey(row)" class="print-editor-row">
                  <el-input
                    :value="row.modelCode || row.barcode || ''"
                    maxlength="80"
                    placeholder="货物名称"
                    @input="updatePrintRow(index, 'modelCode', $event)"
                  />
                  <el-input
                    :value="row.spec || ''"
                    maxlength="30"
                    placeholder="规格"
                    @input="updatePrintRow(index, 'spec', $event)"
                  />
                  <el-input
                    :value="row.meters ?? ''"
                    inputmode="decimal"
                    maxlength="12"
                    placeholder="米数"
                    @input="updatePrintRow(index, 'meters', $event)"
                  />
                  <el-input
                    :value="row.price ?? ''"
                    inputmode="decimal"
                    maxlength="12"
                    placeholder="单价"
                    @input="updatePrintRow(index, 'price', $event)"
                  />
                  <el-input
                    :value="row.totalAmount ?? ''"
                    inputmode="decimal"
                    maxlength="14"
                    placeholder="金额"
                    @input="updatePrintRow(index, 'totalAmount', $event)"
                  />
                  <el-input
                    :value="row.remark || ''"
                    maxlength="30"
                    placeholder="备注"
                    @input="updatePrintRow(index, 'remark', $event)"
                  />
                  <el-tooltip :disabled="canUpdatePrint" content="暂无 print:receipt:update 权限"><span><el-button type="danger" link :disabled="!canUpdatePrint" @click="removePrintRow(index)">移除</el-button></span></el-tooltip>
                </div>
              </div>
            </div>
            <div v-if="!printableRows.length" class="print-editor-empty">
              当前没有可打印明细，可点击“新增打印行”补充本次打印内容。
            </div>
          </aside>
        </div>
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
            <el-select v-model="selectedTemplateId" class="template-select" @change="handleTemplateChange"><el-option v-for="template in receiptTemplates" :key="template.id" :value="template.id" :label="`${template.name}${template.isDefault === 1 ? '（默认）' : ''}`" /></el-select>
            <el-button @click="createNewReceiptTemplate">
              <span class="material-symbols-outlined">add</span>
              新建模板
            </el-button>
            <el-tooltip :disabled="canUpdatePrint" content="暂无 print:receipt:update 权限"><span><el-button type="primary" :disabled="!canUpdatePrint || isTemplateSaving" :loading="isTemplateSaving" @click="saveCurrentReceiptTemplate">
              <span class="material-symbols-outlined">save</span>
              保存为默认
            </el-button></span></el-tooltip>
          </div>
        </header>

        <div class="template-designer-body">
          <div class="template-editor standalone">
            <el-form class="template-grid" label-position="top">
              <label>
                <span>模板名称</span>
                <el-input v-model.trim="templateDraftName" maxlength="30" placeholder="请输入模板名称" />
              </label>
              <label>
                <span>主标题</span>
                <el-input v-model.trim="templateConfig.title" maxlength="20" />
              </label>
              <label>
                <span>副标题</span>
                <el-input v-model.trim="templateConfig.subtitle" maxlength="20" />
              </label>
              <label>
                <span>每页行数</span>
                <el-input-number v-model="templateConfig.rowsPerPage" :min="4" :max="10" />
              </label>
              <label>
                <span>收货仓库</span>
                <el-input v-model.trim="templateConfig.warehouse" maxlength="20" />
              </label>
              <label class="template-check">
                <el-checkbox v-model="templateConfig.showLogistics" />
                <span>显示物流信息</span>
              </label>
              <label class="template-check">
                <el-checkbox v-model="templateConfig.showSignature" />
                <span>显示签字区</span>
              </label>
              <label class="template-wide">
                <span>底部提示语</span>
                <el-input v-model.trim="templateConfig.notice" maxlength="80" />
              </label>
              <el-form-item label="标准控件说明" class="sr-only"><span>模板字段保持原有类型</span></el-form-item>
            </el-form>

            <div v-if="receiptVariables.length" class="variable-strip">
              <span v-for="item in receiptVariables" :key="item.field">{{ item.label }}</span>
            </div>

            <div class="column-editor">
              <div class="column-editor-head">
                <strong>列名与排版</strong>
                <span>明细项固定，支持改列名、显隐和调整顺序</span>
              </div>
              <div class="column-editor-list">
                <div v-for="(column, index) in templateConfig.columns" :key="column.key" class="column-editor-row">
                  <label class="column-visible">
                    <el-checkbox v-model="column.visible" />
                  </label>
                  <span class="column-field">{{ getColumnFieldName(column.key) }}</span>
                  <el-input v-model.trim="column.label" maxlength="12" />
                  <label class="column-width-input">
                    <span>列宽mm</span>
                    <el-input-number
                      v-model="column.widthMm"
                      :min="10"
                      :max="80"
                      :step="1"
                      @change="normalizeColumnWidth(column)"
                    />
                  </label>
                  <div class="column-move-actions">
                    <el-button :disabled="index === 0" @click="moveColumn(index, -1)">上移</el-button>
                    <el-button :disabled="index === templateConfig.columns.length - 1" @click="moveColumn(index, 1)">下移</el-button>
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
                    <div>客户名称：<span>客户名称</span></div>
                    <div>单据编号：<span>CK20260414001</span></div>
                  </div>
                  <div class="order-line">
                    <span>项目名称：<b>春季面料项目</b></span>
                    <span>录单日期：<b>2026-04-14</b></span>
                    <span>制单人：<b>制单人</b></span>
                    <span>第 1 页 / 共 1 页</span>
                  </div>
                </section>

                <table class="receipt-print-table">
                  <colgroup>
                    <col
                      v-for="column in visibleColumns"
                      :key="`preview-col-${column.key}`"
                      :style="{ width: columnWidth(column) }"
                    />
                  </colgroup>
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
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  ElButton,
  ElCheckbox,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElResult,
  ElSelect,
  ElTabPane,
  ElTabs,
  ElTooltip
} from 'element-plus'
import { useUserStore } from '@/stores/user'
import BusinessTimeCorrectionPanel from '@/components/BusinessTimeCorrectionPanel.vue'
import { useTimeCorrectionMode } from '@/composables/useTimeCorrectionMode'
import {
  PRINT_PROFILE_KEYS,
  buildPrintTransformCss,
  loadPrintProfile,
  normalizePrintProfile,
  openCalibrationPrint,
  resetPrintProfile,
  savePrintProfile
} from '@/utils/printProfile'
import {
  cancelPrint,
  getPendingPrintOrders,
  getPrintDetail,
  listReceiptTemplateVariables,
  listReceiptTemplates,
  markPrinted,
  saveReceiptTemplate,
  setDefaultReceiptTemplate,
  updatePrintDetail
} from './receipt/api/receipt.js'
import { createReceiptDetailRequestController } from './receipt/receiptDetailRequest.js'

const isFetchingList = ref(false)
const isLoadingDetail = ref(false)
const isPrinting = ref(false)
const isSubmitting = ref(false)
const isTemplateSaving = ref(false)
const listLoadError = ref('')
const detailLoadError = ref('')
let listRequestId = 0
const lastSelectedOrder = ref(null)
const userStore = useUserStore()
const canViewDetail = computed(() => userStore.hasPermission('print:receipt:detail'))
const canExecute = computed(() => userStore.hasPermission('print:receipt:execute'))
const canUpdatePrint = computed(() => userStore.hasPermission('print:receipt:update'))
const canCancel = computed(() => userStore.hasPermission('print:receipt:cancel'))
const activeMode = ref('print')
const pendingOrders = ref([])
const selectedOrder = ref(null)
const tableData = ref([])
const printDraft = ref(createEmptyPrintDraft())
const receiptDetailRequestController = createReceiptDetailRequestController({
  setLoading: (value) => { isLoadingDetail.value = value },
  clearDetail: () => {
    detailLoadError.value = ''
    selectedOrder.value = null
    tableData.value = []
    printDraft.value = createEmptyPrintDraft()
    closeTimeCorrectionMode()
  }
})
const receiptTemplates = ref([])
const receiptVariables = ref([])
const selectedTemplateId = ref(null)
const templateDraftName = ref('系统默认出库单')
const templateConfig = ref(defaultTemplateConfig())
const receiptPrintProfile = ref(loadPrintProfile(PRINT_PROFILE_KEYS.RECEIPT, {
  paperWidthMm: Number(templateConfig.value.paperWidthMm || 215.9),
  paperHeightMm: Number(templateConfig.value.paperHeightMm || 139.7)
}))
const effectiveReceiptPrintProfile = computed(() => normalizePrintProfile({
  ...receiptPrintProfile.value,
  paperWidthMm: receiptPrintProfile.value.paperWidthMm || Number(templateConfig.value.paperWidthMm || 215.9),
  paperHeightMm: receiptPrintProfile.value.paperHeightMm || Number(templateConfig.value.paperHeightMm || 139.7)
}))
const {
  timeCorrectionMode,
  closeTimeCorrectionMode
} = useTimeCorrectionMode({
  isAvailable: () => activeMode.value === 'print' && !!selectedOrder.value
})

onMounted(async () => {
  await Promise.all([fetchPendingList(), fetchReceiptTemplates(), fetchReceiptVariables()])
})

async function fetchPendingList() {
  const requestId = ++listRequestId
  receiptDetailRequestController.invalidate()
  isFetchingList.value = true
  listLoadError.value = ''
  pendingOrders.value = []
  try {
    const rows = await getPendingPrintOrders()
    if (requestId !== listRequestId) return
    pendingOrders.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    if (requestId !== listRequestId) return
    listLoadError.value = resolveLoadError(error, '待打印出库单')
  } finally {
    if (requestId === listRequestId) isFetchingList.value = false
  }
}

function retryPendingList() { return fetchPendingList() }

async function selectOrder(order) {
  if (!canViewDetail.value) return
  if (selectedOrder.value?.orderNo === order.orderNo) return
  const requestId = receiptDetailRequestController.begin()
  lastSelectedOrder.value = order
  try {
    const detail = await getPrintDetail({ orderNo: order.orderNo })
    if (!receiptDetailRequestController.isCurrent(requestId)) return
    selectedOrder.value = detail
    tableData.value = normalizeEditableRows(detail.items || [])
    printDraft.value = createPrintDraft(detail)
  } catch (error) {
    if (!receiptDetailRequestController.isCurrent(requestId)) return
    detailLoadError.value = resolveLoadError(error, '出库单详情')
  } finally {
    receiptDetailRequestController.finish(requestId)
  }
}

function handleModeChange(mode) {
  if (mode === 'print') return
  lastSelectedOrder.value = null
  receiptDetailRequestController.invalidate()
}

function retrySelectedOrder() { if (lastSelectedOrder.value) return selectOrder(lastSelectedOrder.value) }

function resolveLoadError(error, label) {
  const status = Number(error?.response?.status || error?.status || error?.code || 0)
  if (status === 401) return `登录状态已失效，无法加载${label}。`
  if (status === 403) return `当前账号没有加载${label}的权限。`
  if (!status && !error?.response) return `网络连接失败，无法加载${label}。`
  if (status >= 500) return `服务暂时不可用，无法加载${label}。`
  return error?.msg || error?.message || `${label}加载失败，请重试。`
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
  if (!canUpdatePrint.value) return
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

async function savePrintRevision(options = {}) {
  if (!canUpdatePrint.value) return null
  if (!selectedOrder.value) return null
  const payload = buildPrintRevisionPayload()
  payload.timeCorrectionOnly = options.timeCorrectionOnly === true
  if (!payload.items.length) {
    ElMessage.warning('出库单至少需要一条明细')
    throw new Error('empty receipt items')
  }
  const saved = await updatePrintDetail(payload)
  selectedOrder.value = saved
  tableData.value = normalizeEditableRows(saved.items || [])
  printDraft.value = createPrintDraft(saved)
  if (!options.silent) {
    ElMessage.success(options.timeCorrectionOnly ? '录单日期已修正' : '打印内容已保存，可追溯')
  }
  if (options.timeCorrectionOnly) {
    closeTimeCorrectionMode()
  }
  return saved
}

function buildPrintRevisionPayload() {
  const rows = printableRows.value.map((row) => ({
    id: row.id || undefined,
    barcode: trimOrNull(row.barcode),
    modelCode: trimOrNull(row.modelCode || row.barcode),
    spec: decimalOrNull(row.spec),
    meters: decimalOrNull(row.meters),
    price: decimalOrNull(row.price),
    totalAmount: decimalOrNull(row.totalAmount),
    remark: trimOrNull(row.remark)
  }))
  return {
    id: selectedOrder.value.id,
    orderNo: trimOrNull(printDraft.value.orderNo),
    customerName: trimOrNull(printDraft.value.customerName),
    projectName: trimOrNull(printDraft.value.projectName),
    printDate: normalizeDateText(printDraft.value.printDate || printDraft.value.createTime),
    operator: trimOrNull(printDraft.value.operator),
    logisticsCompany: trimOrNull(printDraft.value.logisticsCompany),
    logisticsNo: trimOrNull(printDraft.value.logisticsNo),
    editReason: '出库单打印前人工修正',
    items: rows
  }
}

function createEmptyPrintDraft() {
  return {
    orderNo: '',
    customerName: '',
    projectName: '',
    createTime: '',
    printDate: '',
    operator: '',
    logisticsCompany: '',
    logisticsNo: ''
  }
}

function createPrintDraft(detail) {
  return {
    orderNo: detail?.orderNo || '',
    customerName: detail?.customerName || '',
    projectName: detail?.projectName || '',
    createTime: formatDateOnly(detail?.createTime),
    printDate: detail?.printDate || formatDateOnly(detail?.createTime),
    operator: detail?.operator || '',
    logisticsCompany: detail?.logisticsCompany || '',
    logisticsNo: detail?.logisticsNo || ''
  }
}

function resetPrintDraft() {
  if (!selectedOrder.value) return
  printDraft.value = createPrintDraft(selectedOrder.value)
  tableData.value = normalizeEditableRows(selectedOrder.value.items || [])
}

function normalizeEditableRows(rows) {
  return (Array.isArray(rows) ? rows : []).map((row, index) => ({
    ...row,
    _key: row.id ? `id-${row.id}` : `manual-${Date.now()}-${index}`,
    spec: row.spec ?? '',
    meters: row.meters ?? '',
    price: row.price ?? '',
    totalAmount: row.totalAmount ?? '',
    remark: row.remark || ''
  }))
}

function addPrintRow() {
  tableData.value = [
    ...tableData.value,
    {
      _key: `manual-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      id: null,
      barcode: '',
      modelCode: '',
      spec: '',
      meters: '',
      price: '',
      totalAmount: '',
      remark: ''
    }
  ]
}

function removePrintRow(index) {
  tableData.value = tableData.value.filter((_, rowIndex) => rowIndex !== index)
}

function updatePrintRow(index, field, value) {
  const rows = [...tableData.value]
  const row = rows[index] ? { ...rows[index] } : {}
  row[field] = ['meters', 'price', 'totalAmount', 'spec'].includes(field) ? sanitizeDecimalText(value) : value
  if (field === 'totalAmount') {
    row._amountManual = true
  }
  if (['meters', 'price'].includes(field) && !row._amountManual) {
    row.totalAmount = calcRowAmount(row.meters, row.price)
  }
  rows[index] = row
  tableData.value = rows
}

function isPrintableRow(row) {
  if (!row) return false
  return Boolean(
    row.id ||
    trimOrNull(row.modelCode) ||
    trimOrNull(row.barcode) ||
    decimalOrNull(row.meters) ||
    decimalOrNull(row.price) ||
    decimalOrNull(row.totalAmount) ||
    trimOrNull(row.remark)
  )
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

const printableRows = computed(() => tableData.value.filter((row) => isPrintableRow(row)))
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

function persistReceiptPrintProfile() {
  receiptPrintProfile.value = savePrintProfile(PRINT_PROFILE_KEYS.RECEIPT, effectiveReceiptPrintProfile.value)
}

function syncReceiptProfileWithTemplate() {
  receiptPrintProfile.value = savePrintProfile(PRINT_PROFILE_KEYS.RECEIPT, {
    ...effectiveReceiptPrintProfile.value,
    paperWidthMm: Number(templateConfig.value.paperWidthMm || 215.9),
    paperHeightMm: Number(templateConfig.value.paperHeightMm || 139.7)
  })
}

function resetReceiptPrintProfile() {
  receiptPrintProfile.value = resetPrintProfile(PRINT_PROFILE_KEYS.RECEIPT, {
    paperWidthMm: Number(templateConfig.value.paperWidthMm || 215.9),
    paperHeightMm: Number(templateConfig.value.paperHeightMm || 139.7)
  })
}

function printReceiptCalibrationPage() {
  persistReceiptPrintProfile()
  if (!openCalibrationPrint(effectiveReceiptPrintProfile.value, '出库单打印校准页')) {
    ElMessage.error('浏览器拦截了打印窗口，请允许弹窗后重试')
  }
}

async function openBrowserPrint() {
  if (!canExecute.value) return
  if (!selectedOrder.value) return
  isPrinting.value = true
  try {
    persistReceiptPrintProfile()
    await savePrintRevision({ silent: true })
    await nextTick()
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
  if (!canExecute.value) return
  if (!selectedOrder.value) return
  await ElMessageBox.confirm('确认这张出库单已经打印成功吗？确认后该单据会移出待打印队列。', '打印确认', {
    confirmButtonText: '确认已打印',
    cancelButtonText: '取消',
    type: 'warning'
  })
  isSubmitting.value = true
  try {
    await savePrintRevision({ silent: true })
    await markPrinted({ orderNo: selectedOrder.value.orderNo })
    ElMessage.success('已标记为打印完成')
    await fetchPendingList()
  } finally {
    isSubmitting.value = false
  }
}

async function handleCancelPrint() {
  if (!canCancel.value) return
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
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data: https:; font-src data:; base-uri 'none'; form-action 'none'; script-src 'none'" />
  <title>出库单打印</title>
  <style>${printCss()}</style>
</head>
<body>${content}</body>
</html>`
}

function printCss() {
  const contentWidth = Number(templateConfig.value.paperWidthMm || 215.9)
  const contentHeight = Number(templateConfig.value.paperHeightMm || 139.7)
  const profile = effectiveReceiptPrintProfile.value
  return `
    @page { size: ${profile.paperWidthMm}mm ${profile.paperHeightMm}mm; margin: ${profile.pageMarginMm}mm; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; background: #fff; color: #000; font-family: SimSun, "宋体", NSimSun, serif; font-weight: 600; text-rendering: geometricPrecision; -webkit-font-smoothing: none; }
    .paper-stack { display: block; }
    .receipt-page { width: ${contentWidth}mm; height: ${contentHeight}mm; padding: 6mm 8mm 5mm; page-break-after: always; break-after: page; overflow: hidden; position: relative; background: white; transform: ${buildPrintTransformCss(profile)}; transform-origin: top left; }
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
    .logistics-row b { width: 31mm; border-bottom: 1.2px solid #000; min-height: 4mm; display: inline-block; text-align: center; }
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
    { key: 'modelCode', label: '货物名称', visible: true, widthMm: 30 },
    { key: 'spec', label: '规格', visible: true, widthMm: 18 },
    { key: 'meters', label: '数量/米', visible: true, widthMm: 18 },
    { key: 'blank1', label: '数量/米', visible: true, widthMm: 16 },
    { key: 'blank2', label: '数量/米', visible: true, widthMm: 16 },
    { key: 'blank3', label: '数量/米', visible: true, widthMm: 16 },
    { key: 'totalMeters', label: '总米数', visible: true, widthMm: 18 },
    { key: 'price', label: '单价', visible: true, widthMm: 18 },
    { key: 'amount', label: '金额', visible: true, widthMm: 20 },
    { key: 'remark', label: '备注', visible: true, widthMm: 36 }
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
      visible: source.visible !== false,
      widthMm: normalizeWidthValue(source.widthMm, fallback.widthMm)
    }
  })
}

function normalizeWidthValue(value, fallback = 18) {
  const numeric = Number(value)
  const safeFallback = Number.isFinite(Number(fallback)) ? Number(fallback) : 18
  if (!Number.isFinite(numeric)) return safeFallback
  return Math.max(10, Math.min(80, Math.round(numeric)))
}

function normalizeColumnWidth(column) {
  if (!column) return
  const fallback = defaultReceiptColumns().find((item) => item.key === column.key)?.widthMm || 18
  column.widthMm = normalizeWidthValue(column.widthMm, fallback)
}

function columnWidth(column) {
  const fallback = defaultReceiptColumns().find((item) => item.key === column?.key)?.widthMm || 18
  return `${normalizeWidthValue(column?.widthMm, fallback)}mm`
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
    modelCode: '货物名称项',
    spec: '规格项',
    meters: '米数项',
    blank1: '空白数量列',
    blank2: '空白数量列',
    blank3: '空白数量列',
    totalMeters: '总米数项',
    price: '单价项',
    amount: '金额项',
    remark: '行备注项'
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
    remark: row.remark || ''
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
  return String(row._key || row.id || row.barcode || row.modelCode || '')
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

function trimOrNull(value) {
  const text = String(value ?? '').trim()
  return text ? text : null
}

function sanitizeDecimalText(value) {
  return String(value ?? '')
    .replace(/[^\d.]/g, '')
    .replace(/^(\d*\.?\d{0,2}).*$/, '$1')
}

function decimalOrNull(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? Number(number.toFixed(2)) : null
}

function calcRowAmount(meters, price) {
  const meterNumber = decimalOrNull(meters)
  const priceNumber = decimalOrNull(price)
  if (meterNumber === null || priceNumber === null) return ''
  return (meterNumber * priceNumber).toFixed(2)
}

function normalizeDateText(value) {
  const text = formatDateOnly(value)
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return text
  }
  ElMessage.warning('录单日期格式必须为 yyyy-MM-dd')
  throw new Error('invalid print date')
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
    '明细内容：${modelCode} / ${spec} / ${meters} / ${price} / ${amount} / ${remark}',
    '本页：${pageNo}/${totalPages}，合计米数：${pageMeters}，小计：${pageAmount}，总金额：${totalAmount}'
  ].join('\n')
}
</script>

<style scoped>
.receipt-page-shell {
  min-height: auto;
  overflow-x: hidden;
}

.receipt-page-container {
  padding-bottom: .75rem;
}

.receipt-hero {
  gap: var(--function-card-gap);
}

.receipt-tabs {
  display: flex;
  gap: .75rem;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.receipt-tabs button {
  border: 1px solid #c8d3df;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #475569;
  padding: .7rem 1rem;
  display: inline-flex;
  align-items: center;
  gap: .4rem;
  font-weight: 900;
  cursor: pointer;
}

.receipt-tabs button.active {
  background: linear-gradient(135deg, var(--ys-primary-dark), var(--ys-primary));
  border-color: var(--ys-primary);
  color: #fff;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18);
}

.receipt-print-profile-panel {
  display: grid;
  grid-template-columns: minmax(12rem, 1.25fr) minmax(0, 3fr) auto;
  gap: .75rem;
  align-items: end;
  margin-bottom: .75rem;
  padding: .75rem;
  border: 1px solid rgb(var(--ys-primary-rgb) / 0.14);
  border-radius: .75rem;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.receipt-print-profile-panel .profile-intro {
  display: flex;
  flex-direction: column;
  gap: .25rem;
  color: var(--ys-primary-dark);
}

.receipt-print-profile-panel .profile-intro strong {
  font-size: .95rem;
  font-weight: 1000;
}

.receipt-print-profile-panel .profile-intro span,
.receipt-print-profile-panel label span {
  color: #64748b;
  font-size: .75rem;
  font-weight: 800;
}

.receipt-print-profile-panel label {
  display: flex;
  flex-direction: column;
  gap: .4rem;
}

.receipt-print-profile-controls {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(6, minmax(6.25rem, 1fr));
  gap: .5rem;
}

.receipt-print-profile-controls :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.receipt-print-profile-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: .5rem;
}

.receipt-print-profile-panel input {
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: .75rem;
  padding: 0 .65rem;
  color: #0f172a;
  font-weight: 800;
  background: #fff;
}

.receipt-workspace {
  min-height: 0;
  width: 100%;
  padding: 0;
  display: flex;
  align-items: stretch;
  gap: 1.25rem;
  color: #1f2937;
}

.template-workspace {
  width: 100%;
  padding: 0;
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
  border-color: var(--ys-primary);
  box-shadow: 0 8px 24px rgb(var(--ys-primary-rgb) / 12%);
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
  min-height: clamp(480px, calc(100vh - 18rem), 760px);
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
  border-color: var(--ys-primary);
  box-shadow: 0 0 0 3px rgb(var(--ys-primary-rgb) / 12%);
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
  cursor: not-allowed;
}

.btn-print {
  background: var(--ys-primary);
  color: white;
}

.btn-template {
  background: #f8fafc;
  color: var(--ys-primary);
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
  min-width: 0;
  overflow: auto;
  background: #eef2f7;
  padding: 1.5rem;
  position: relative;
}

.receipt-preview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  align-items: flex-start;
  gap: 1.25rem;
  width: 100%;
  min-width: 0;
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
  color: var(--ys-primary);
  font-weight: 900;
}

.loading-mask .material-symbols-outlined {
  font-size: 2.4rem;
}

.paper-stack {
  flex: 0 0 auto;
  width: max-content;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.paper-preview-viewport {
  min-width: 0;
  overflow: auto;
  padding: .25rem .25rem .75rem;
  display: flex;
  align-items: flex-start;
  justify-content: center;
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

.print-editor {
  width: 100%;
  min-width: 340px;
  max-width: 420px;
  margin: 0;
  padding: 1rem;
  border: 1px solid #dbe3ef;
  border-radius: 1rem;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 10px 28px rgb(15 23 42 / 10%);
  position: sticky;
  top: 0;
  align-self: flex-start;
  max-height: calc(100vh - 12rem);
  overflow: auto;
}

.print-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.print-editor-head strong,
.print-editor-table-head strong {
  color: #1f2937;
  font-size: .95rem;
  font-weight: 900;
}

.print-editor-head span {
  display: block;
  margin-top: .2rem;
  color: #64748b;
  font-size: .75rem;
}

.print-editor-actions {
  display: flex;
  gap: .5rem;
  flex-wrap: wrap;
}

.editor-time-btn,
.editor-save-btn,
.editor-reset-btn,
.editor-add-btn,
.editor-remove-btn {
  border: 1px solid #dbe3ef;
  border-radius: .65rem;
  padding: .45rem .75rem;
  font-size: .75rem;
  font-weight: 900;
  cursor: pointer;
}

.editor-time-btn {
  display: inline-flex;
  align-items: center;
  gap: .3rem;
  background: var(--ys-primary-container);
  border-color: rgb(var(--ys-primary-rgb) / .24);
  color: var(--ys-primary);
}

.editor-time-btn.active {
  background: var(--ys-primary);
  border-color: var(--ys-primary);
  color: #fff;
}

.editor-time-btn .material-symbols-outlined {
  font-size: 1rem;
}

.editor-save-btn {
  background: #16a34a;
  border-color: #16a34a;
  color: #fff;
}

.editor-save-btn:disabled {
  cursor: not-allowed;
  color: var(--ys-disabled-text);
  border-color: var(--ys-disabled-bg);
  background: var(--ys-disabled-bg);
  box-shadow: none;
  opacity: 1;
}

.editor-reset-btn,
.editor-add-btn {
  background: #fff7e6;
  color: var(--ys-primary);
}

.editor-remove-btn {
  background: #fff1f2;
  color: #be123c;
}

.print-editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: .75rem;
}

.print-editor-grid label {
  display: flex;
  flex-direction: column;
  gap: .35rem;
}

.print-editor-grid span {
  color: #64748b;
  font-size: .72rem;
  font-weight: 900;
}

.print-editor :deep(.el-input) {
  width: 100%;
}

.print-editor :deep(.el-input__wrapper) {
  min-height: 2.25rem;
  border-radius: .65rem;
}

.print-editor-table-wrap {
  margin-top: 1rem;
  border-top: 1px dashed #dbe3ef;
  padding-top: 1rem;
}

.print-editor-table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: .75rem;
}

.print-editor-table {
  display: flex;
  flex-direction: column;
  gap: .45rem;
  width: 100%;
  overflow-x: auto;
  scrollbar-gutter: stable;
}

.print-editor-row {
  min-width: 680px;
  display: grid;
  grid-template-columns: 1.25fr .65fr .65fr .65fr .75fr .85fr 64px;
  gap: .45rem;
  align-items: center;
}

.print-editor-row-head {
  color: #64748b;
  font-size: .72rem;
  font-weight: 900;
  padding: 0 .25rem;
}

.print-editor-empty {
  margin-top: .85rem;
  color: #94a3b8;
  font-size: .8rem;
  font-weight: 800;
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

.template-grid :deep(.el-input),
.template-grid :deep(.el-input-number) {
  width: 100%;
}

.template-grid :deep(.el-input__wrapper) {
  min-height: 2.3rem;
  border-radius: .7rem;
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
  color: var(--ys-primary);
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
  grid-template-columns: 28px minmax(90px, 1fr) minmax(120px, 1.2fr) 96px auto;
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

.column-editor-row > :deep(.el-input) {
  width: 100%;
}

.column-width-input {
  display: flex;
  align-items: center;
  gap: .35rem;
  color: #64748b;
  font-size: .72rem;
  font-weight: 800;
}

.column-width-input :deep(.el-input-number) {
  width: 96px;
}

.column-move-actions {
  display: flex;
  gap: .35rem;
}

.column-move-actions button {
  border: 1px solid #dbe3ef;
  border-radius: .55rem;
  background: #f8fafc;
  color: var(--ys-primary);
  padding: .35rem .5rem;
  font-size: .72rem;
  font-weight: 900;
  cursor: pointer;
}

.column-move-actions button:disabled {
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
  border-color: var(--ys-primary);
  box-shadow: 0 0 0 3px rgb(var(--ys-primary-rgb) / 12%);
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
  min-height: 4mm;
  display: inline-block;
  text-align: center;
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

@media (max-width: 1280px) {
  .receipt-print-profile-panel {
    grid-template-columns: 1fr;
  }

  .receipt-print-profile-controls {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .receipt-preview-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .print-editor {
    width: 100%;
    min-width: 0;
    max-width: none;
    position: static;
    max-height: none;
  }
}

@media (max-width: 1100px) {
  .receipt-workspace {
    min-height: auto;
    flex-direction: column;
  }

  .queue-panel {
    width: 100%;
    max-height: 420px;
  }

  .remark-editor-list {
    grid-template-columns: 1fr;
  }

  .print-editor-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

@media (max-width: 720px) {
  .receipt-print-profile-panel {
    grid-template-columns: 1fr;
  }

  .receipt-print-profile-controls {
    grid-template-columns: 1fr;
  }

  .receipt-workspace,
  .template-workspace {
    padding-left: 0;
    padding-right: 0;
  }

  .receipt-tabs {
    width: 100%;
  }

  .preview-scroll {
    padding: .85rem;
  }

  .preview-head {
    align-items: flex-start;
  }

  .receipt-actions,
  .receipt-actions .btn,
  .template-select {
    width: 100%;
  }

  .receipt-actions .btn,
  .template-select {
    justify-content: center;
  }

  .paper-preview-viewport {
    justify-content: flex-start;
  }

  .print-editor-head,
  .print-editor-table-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .print-editor-grid {
    grid-template-columns: 1fr;
  }

  .print-editor-row {
    min-width: 640px;
  }
}
</style>
