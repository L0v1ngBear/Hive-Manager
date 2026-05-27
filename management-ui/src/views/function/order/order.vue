<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
    <header class="function-page-header">
      <div>
        <div class="function-page-eyebrow">
          <span class="material-symbols-outlined">list_alt</span>
          订单协同中心
        </div>
        <h1 class="function-page-title">订单全流程管理</h1>
        <p class="function-page-desc">
          统一管理订单创建、编辑、详情查看和状态流转追踪。</p>
      </div>
      <div class="flex flex-col gap-3 xl:flex-row xl:items-stretch">
        <div class="order-header-card-stack">
          <div class="order-summary-grid">
            <button
                v-for="card in summaryCards"
                :key="card.key"
                type="button"
                class="stat-card text-left"
                :class="[isSummaryCardActive(card) ? 'stat-card-active' : '', summaryCardClass(card)]"
                @click="selectSummaryCard(card)"
            >
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.count }}</div>
              <div class="stat-hint">{{ card.hint }}</div>
            </button>
          </div>
          <div class="order-category-summary-grid" aria-label="订单小项数量统计">
            <button
                v-for="card in categorySummaryCards"
                :key="card.key"
                type="button"
                class="category-stat-card"
                :class="{ 'category-stat-card-active': filters.orderCategory === card.value }"
                @click="selectCategoryCard(card.value)"
            >
              <span>{{ card.label }}</span>
              <strong>{{ card.count }}</strong>
              <small>{{ card.hint }}</small>
            </button>
          </div>
        </div>
        <button
            v-permission="'order:warning:setting'"
            class="order-warning-setting-btn"
            @click="openOrderWarningSetting"
        >
          <span class="material-symbols-outlined text-[20px]">notification_important</span>
          预警设置
        </button>
        <button
            class="order-warning-refresh-btn"
            :disabled="warningRefreshing"
            @click="refreshOrderWarnings(true)"
        >
          <span class="material-symbols-outlined text-[20px]" :class="warningRefreshing ? 'animate-spin' : ''">sync</span>
          重新更新预警
        </button>
        <button class="function-action-primary px-6 py-4"
                @click="openCreate">
          新建订单
        </button>
      </div>
    </header>

    <section class="overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
      <div class="status-chip-row order-primary-status-tabs border-b border-outline-variant/10">
        <button
            v-for="status in currentStatusTabs"
            :key="status.value || 'all'"
            type="button"
            class="status-chip"
            :class="[!filters.staleOnly && filters.status === status.value ? 'status-chip-active' : '', statusChipClass(status.value)]"
            @click="selectStatus(status.value)"
        >
          <span>{{ status.label }}</span>
          <strong>{{ status.count }}</strong>
        </button>
      </div>
      <div
          class="order-filter-grid grid grid-cols-1 gap-4 border-b border-outline-variant/10 bg-surface-container-low/30 px-6 py-5 md:grid-cols-2 xl:grid-cols-12">
        <input
            v-model.trim="filters.keyword"
            class="box-input xl:col-span-2"
            placeholder="搜索订单号、客户、项目、品牌或商品描述"
            @keyup.enter="refreshCurrentTab"
        />
        <input
            v-model.trim="filters.customerName"
            class="box-input xl:col-span-2"
            placeholder="客户名称"
            @keyup.enter="refreshCurrentTab"
        />
        <input
            v-model.trim="filters.brandName"
            class="box-input xl:col-span-2"
            placeholder="品牌名称"
            @keyup.enter="refreshCurrentTab"
        />
        <select v-model="filters.orderCategory" class="box-input xl:col-span-2">
          <option value="">全部小项</option>
          <option v-for="option in orderCategoryOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <select v-model="filters.invoiceStatus" class="box-input xl:col-span-2">
          <option value="">全部开票</option>
          <option value="1">已开票</option>
          <option value="0">未开票</option>
        </select>
        <div class="order-filter-actions flex flex-wrap items-center gap-2 md:col-span-2 xl:col-span-12">
          <div class="order-query-actions">
            <button class="order-filter-action-btn rounded-lg bg-primary px-5 py-2.5 text-sm font-bold text-on-primary" @click="refreshCurrentTab">
              查询
            </button>
            <button class="order-filter-action-btn rounded-lg border border-outline-variant/30 bg-white px-5 py-2.5 text-sm font-bold text-on-surface" @click="resetFilters">
              重置
            </button>
          </div>
          <TableColumnSettings
              class="order-table-toolbox"
              :columns="orderTableColumns"
              export-file-name="订单列表"
              export-sheet-name="订单列表"
              export-module="order"
              :export-allable="true"
              @move="moveOrderTableColumn"
              @reset="resetOrderTableColumns"
              @export-all="exportAllOrders"
          />
        </div>
        <div class="grid grid-cols-1 gap-3 md:col-span-2 md:grid-cols-2 xl:col-span-12">
          <label class="flex items-center gap-2 text-xs font-bold text-on-surface-variant">
            <span>创建时间</span>
            <DateFilterInput v-model="filters.createStart" placeholder="开始日期" class="box-input min-w-0 flex-1" />
            <span>至</span>
            <DateFilterInput v-model="filters.createEnd" placeholder="结束日期" class="box-input min-w-0 flex-1" />
          </label>
          <label class="flex items-center gap-2 text-xs font-bold text-on-surface-variant">
            <span>交付时间</span>
            <DateFilterInput v-model="filters.deliveryStart" placeholder="交付开始" class="box-input min-w-0 flex-1" />
            <span>至</span>
            <DateFilterInput v-model="filters.deliveryEnd" placeholder="交付结束" class="box-input min-w-0 flex-1" />
          </label>
        </div>
      </div>

      <div class="responsive-table-wrap">
        <table class="order-list-table responsive-data-table w-full text-left">
          <thead class="bg-surface-container-low/50">
          <tr>
            <th
                v-for="column in orderTableColumns"
                :key="column.key"
                class="th-cell"
                :class="orderColumnClass(column.key)"
            >
              {{ column.label }}
            </th>
            <th class="th-cell text-right">操作</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
          <tr
              v-for="row in currentState.rows"
              :key="row.orderId"
              class="order-table-row group cursor-pointer"
              :class="[orderRowClass(row.status), row.staleWarning ? 'order-row-stale-warning' : '']"
              @click="openDetail(row.orderId)"
          >
            <td
                v-for="column in orderTableColumns"
                :key="column.key"
                :data-label="column.label"
                class="td-cell"
                :class="[orderColumnClass(column.key), column.key === 'time' ? 'text-sm text-on-surface-variant' : '']"
            >
              <template v-if="column.key === 'orderNo'">
                <div class="font-bold text-primary">{{ row.orderId }}</div>
                <div class="mt-1 text-xs text-on-surface-variant">
                  {{ currentTab === 'sales' ? `明细 ${row.detailCount || 0} 项` : `数量 ${row.quantity || 0}` }}
                </div>
                <div class="mt-2">
                  <span class="order-category-pill">{{ orderCategoryLabel(row.orderCategory) }}</span>
                </div>
                <div v-if="row.staleWarning" class="order-stale-actions">
                  <div class="order-stale-tag">
                    <span class="material-symbols-outlined text-[14px]">warning</span>
                    {{ row.staleDays || row.staleWarningDays || 0 }} 天未更新
                  </div>
                  <button
                      type="button"
                      class="order-stale-refresh-btn"
                      :class="{ 'is-refreshing': isRowWarningRefreshing(row) }"
                      :disabled="warningRefreshing || Boolean(rowWarningRefreshingKey)"
                      @click.stop="refreshSingleOrderWarning(row)"
                  >
                    <span class="material-symbols-outlined text-[14px]">sync</span>
                    重新预警
                  </button>
                </div>
              </template>
              <template v-else-if="column.key === 'category'">
                <span class="order-category-pill">{{ orderCategoryLabel(row.orderCategory) }}</span>
              </template>
              <template v-else-if="column.key === 'customer'">
                <div class="font-bold text-primary">{{ row.customerName || '未填写客户' }}</div>
                <div class="mt-1 text-xs text-on-surface-variant">{{ row.projectName || '未填写项目' }}</div>
                <div class="mt-1 text-xs text-on-surface-variant">品牌：{{ row.brandName || '未填写' }}</div>
              </template>
              <template v-else-if="column.key === 'brand'">
                <div class="font-bold text-primary">{{ row.brandName || '未填写品牌' }}</div>
                <div class="mt-1 text-xs text-on-surface-variant">订单品牌</div>
              </template>
              <template v-else-if="column.key === 'core'">
                <template v-if="currentTab === 'sales'">
                  <div class="max-w-[320px] truncate font-bold text-primary">{{ row.goodsDesc || '未填写商品信息' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">总数量 {{ row.totalQuantity || 0 }} / 金额
                    ￥{{ formatAmount(row.totalAmount) }}
                  </div>
                </template>
                <template v-else>
                  <div class="font-bold text-primary">{{ row.modelCode || '未填写型号' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ formatNumber(row.weight) }} 克 /
                    {{ formatNumber(row.width) }} 规格
                  </div>
                </template>
              </template>
              <template v-else-if="column.key === 'delivery'">
                <template v-if="currentTab === 'sales'">
                  <div class="text-sm text-on-surface-variant">{{ row.deliveryDate || '未填写交付日期' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">
                    {{ row.expressCompany || '未发货' }}
                    <template v-if="row.expressNo"> / {{ row.expressNo }}</template>
                  </div>
                </template>
                <template v-else>
                  <div class="text-sm text-on-surface-variant">{{ row.salesOrderId || '未关联销售单' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{
                      formatDateTime(row.deliveryDate || row.createTime)
                    }}
                  </div>
                </template>
              </template>
              <template v-else-if="column.key === 'invoice'">
                <template v-if="currentTab === 'sales'">
                    <span :class="invoiceClass(row.isInvoice)"
                          class="order-status-pill">
                      {{ invoiceLabel(row.isInvoice) }}
                    </span>
                </template>
                <template v-else>
                  <span class="text-xs text-on-surface-variant">--</span>
                </template>
              </template>
              <template v-else-if="column.key === 'status'">
                <div class="flex flex-col items-start gap-2">
                  <span
                      class="order-status-pill"
                      :class="currentTab === 'sales' ? salesStatusClass(row.status) : productionStatusClass(row.status)"
                  >
                    {{ currentTab === 'sales' ? salesStatusLabel(row.status) : productionStatusLabel(row.status) }}
                  </span>
                  <span
                      v-if="currentTab === 'sales'"
                      :class="invoiceClass(row.isInvoice)"
                      class="order-status-pill order-status-pill-sm"
                  >
                    {{ invoiceLabel(row.isInvoice) }}
                  </span>
                </div>
              </template>
              <template v-else-if="column.key === 'progress'">
                <div class="order-progress-cell">
                  <div class="order-progress-meta">
                    <span>{{ orderProgress(row).label }}</span>
                    <strong>{{ orderProgress(row).percent }}%</strong>
                  </div>
                  <div class="order-progress-track">
                    <div
                        class="order-progress-bar"
                        :style="{ width: `${orderProgress(row).percent}%` }"
                    ></div>
                  </div>
                  <div v-if="currentTab === 'production' && row.status === 'producing'" class="production-process-mini">
                    <div class="production-process-current">
                      当前工序：{{ productionProcessText(row) }}
                    </div>
                    <div class="production-process-steps" aria-label="生产工序进度">
                      <span
                          v-for="step in productionProcessSteps(row)"
                          :key="step.code"
                          class="production-process-step"
                          :class="{ done: step.done, current: step.current }"
                      >
                        {{ step.name }}
                      </span>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'time'">
                {{ formatDateTime(row.createTime) }}
              </template>
            </td>
            <td class="td-cell" data-label="操作">
              <div class="order-row-actions">
                <button class="icon-btn text-secondary" @click.stop="openDetail(row.orderId)">
                  <span class="material-symbols-outlined text-[18px]">visibility</span>
                </button>
                <button class="icon-btn text-primary" title="补打流转码" @click.stop="openFlowCode(row)">
                  <span class="material-symbols-outlined text-[18px]">qr_code_2</span>
                </button>
                <button class="icon-btn text-primary" @click.stop="openEdit(row.orderId)">
                  <span class="material-symbols-outlined text-[18px]">edit</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!currentState.loading && !currentState.rows.length">
            <td :colspan="orderTableColumnCount" class="px-6 py-14 text-center text-sm text-on-surface-variant">暂无订单数据</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div
          class="flex items-center justify-between border-t border-outline-variant/10 bg-surface-container-low/30 px-6 py-4 text-sm">
        <span class="text-on-surface-variant">共 {{ currentState.total }} 条</span>
        <div class="flex items-center gap-2">
          <button class="page-btn" :disabled="currentState.page <= 1 || currentState.loading"
                  @click="changePage(currentState.page - 1)">上一页
          </button>
          <span>{{ currentState.page }} / {{ Math.max(currentState.pages, 1) }}</span>
          <button class="page-btn"
                  :disabled="currentState.page >= Math.max(currentState.pages, 1) || currentState.loading"
                  @click="changePage(currentState.page + 1)">下一页
          </button>
        </div>
      </div>
    </section>

    <Teleport defer to="body">
      <transition name="fade">
        <div v-if="detailVisible" class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm"
             @click="closeDetail"></div>
      </transition>
      <transition name="slide">
        <aside v-if="detailVisible" class="drawer-large">
          <div class="drawer-head">
            <div>
              <h2 class="text-xl font-bold tracking-tight text-primary">订单详情</h2>
              <p class="mt-1 text-xs text-on-surface-variant">查看订单主体信息、明细内容和状态流转记录。</p>
            </div>
            <button class="close-btn" @click="closeDetail">
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>
          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="detailLoading" class="py-12 text-center text-on-surface-variant">加载中...</div>

            <template v-else-if="currentTab === 'sales' && salesDetail">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="info-card">
                  <div class="info-label">订单号</div>
                  <div class="info-value">{{ salesDetail.orderId }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">状态</div>
                  <div class="info-value order-status-pill detail-status-pill" :class="salesStatusClass(salesDetail.status)">
                    {{ salesStatusLabel(salesDetail.status) }}
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">订单小项</div>
                  <div class="info-value">{{ orderCategoryLabel(salesDetail.orderCategory) }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">客户 / 项目</div>
                  <div class="info-value">{{ salesDetail.customerName || '未填写客户' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ salesDetail.projectName || '未填写项目' }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">品牌</div>
                  <div class="info-value">{{ salesDetail.brandName || '未填写品牌' }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">交付信息</div>
                  <div class="info-value">{{ salesDetail.deliveryDate || '未填写交付日期' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">
                    {{ salesDetail.expressCompany || '未发货' }}
                    <template v-if="salesDetail.expressNo"> / {{ salesDetail.expressNo }}</template>
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">开票状态</div>
                  <div class="info-value">{{ invoiceLabel(salesDetail.isInvoice) }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">用于财务开票跟进和业务统计。</div>
                </div>
              </div>
              <div class="mt-4">
                <div class="info-card">
                  <div class="info-label">订单附件</div>
                  <template v-if="salesDetail.attachmentUrl">
                    <button class="mt-2 text-left font-bold text-primary hover:underline" @click="openAttachmentUrl(salesDetail.attachmentUrl, salesDetail.attachmentName)">
                      {{ salesDetail.attachmentName || '查看附件' }}
                    </button>
                    <div class="mt-1 text-xs text-on-surface-variant">{{ formatFileSize(salesDetail.attachmentSize) }}</div>
                  </template>
                  <div v-else class="mt-2 text-sm text-on-surface-variant">暂无附件</div>
                </div>
              </div>
              <div class="mt-6">
                <div class="section-title">生产工序进度</div>
                <div class="production-process-detail">
                  <div class="production-process-detail-head">
                    <span>当前工序</span>
                    <strong>{{ productionProcessText(productionDetail) || '待进入生产' }}</strong>
                    <em>{{ productionProcessPercent(productionDetail) }}%</em>
                  </div>
                  <div class="order-progress-track">
                    <div
                        class="order-progress-bar"
                        :style="{ width: `${productionProcessPercent(productionDetail)}%` }"
                    ></div>
                  </div>
                  <div class="production-process-steps production-process-steps-detail">
                    <span
                        v-for="step in productionProcessSteps(productionDetail)"
                        :key="step.code"
                        class="production-process-step"
                        :class="{ done: step.done, current: step.current }"
                    >
                      {{ step.name }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="mt-6">
                <div class="section-title">订单流转码</div>
                <div class="order-flow-code-card">
                  <div class="flow-code-summary">
                    <div class="flow-code-icon">码</div>
                    <div>
                      <div class="flow-code-title">{{ currentOrderFlowCode.taskNo ? '待打印流转标签' : '审批通过后自动生成' }}</div>
                      <div class="flow-code-desc">审批通过后进入小程序待打印队列，打印后贴到订单资料上用于扫码流转。</div>
                    </div>
                  </div>
                  <div class="flow-code-meta">
                    <span>{{ currentOrderFlowCode.orderTypeLabel }}</span>
                    <span>{{ currentOrderFlowCode.currentStatusLabel }}</span>
                    <span>{{ currentOrderFlowCode.orderCategoryLabel }}</span>
                  </div>
                  <div class="flow-code-hint">系统会在订单审批通过后自动生成待打印任务；列表中的流转码按钮仅用于补打。</div>
                </div>
              </div>
              <div class="mt-6">
                <div class="section-title">订单明细</div>
                <div v-for="item in salesDetail.items || []" :key="item.id" class="detail-item">
                  <div class="font-bold text-primary">{{ item.modelCode || '未填写型号' }}</div>
                  <div class="mt-1 text-sm text-on-surface-variant">
                    克重：{{ formatNumber(item.weight) || '未填写' }} / 规格：{{ item.spec || '未填写' }} /
                    数量：{{ item.quantity || 0 }}
                  </div>
                </div>
                <div v-if="!(salesDetail.items || []).length" class="mt-3 text-sm text-on-surface-variant">暂无明细项
                </div>
              </div>
              <div class="mt-6">
                <div class="section-title">状态流转记录</div>
                <div v-if="(salesDetail.logs || []).length" class="status-timeline">
                  <div
                      v-for="(log, index) in salesDetail.logs || []"
                      :key="log.id || index"
                      class="status-log-item"
                  >
                    <div class="status-log-rail">
                      <div class="status-log-dot" :class="{ current: index === 0 }"></div>
                    </div>
                    <div class="status-log-content">
                      <div class="flex flex-wrap items-center justify-between gap-2">
                        <div class="status-log-title">{{ salesLogTitle(log) }}</div>
                        <span v-if="index === 0" class="status-log-current">最新</span>
                      </div>
                      <div class="status-log-time">{{ formatDateTime(log.createTime) }}</div>
                      <div v-if="log.operatorName || log.operator" class="status-log-meta">
                        操作人：{{ log.operatorName || log.operator }}
                      </div>
                      <div v-if="log.remark" class="status-log-remark">备注：{{ log.remark }}</div>
                    </div>
                  </div>
                </div>
                <div v-else class="status-log-empty">暂无状态流转记录</div>
              </div>
            </template>

            <template v-else-if="productionDetail">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="info-card">
                  <div class="info-label">生产单号</div>
                  <div class="info-value">{{ productionDetail.orderId }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">状态</div>
                  <div class="info-value order-status-pill detail-status-pill" :class="productionStatusClass(productionDetail.status)">
                    {{ productionStatusLabel(productionDetail.status) }}
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">订单小项</div>
                  <div class="info-value">{{ orderCategoryLabel(productionDetail.orderCategory) }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">客户 / 项目</div>
                  <div class="info-value">{{ productionDetail.customerName || '未填写客户' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{
                      productionDetail.projectName || '未填写项目'
                    }}
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">品牌</div>
                  <div class="info-value">{{ productionDetail.brandName || '未填写品牌' }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">生产信息</div>
                  <div class="info-value">{{ productionDetail.modelCode || '未填写型号' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">
                    {{ formatNumber(productionDetail.weight) }} 克 / {{ formatNumber(productionDetail.width) }} 规格 /
                    数量 {{ productionDetail.quantity || 0 }}
                  </div>
                </div>
              </div>

              <div class="mt-6">
                <div class="section-title">订单流转码</div>
                <div class="order-flow-code-card">
                  <div class="flow-code-summary">
                    <div class="flow-code-icon">码</div>
                    <div>
                      <div class="flow-code-title">{{ currentOrderFlowCode.taskNo ? '待打印流转标签' : '审批通过后自动生成' }}</div>
                      <div class="flow-code-desc">审批通过后进入小程序待打印队列，打印后贴到订单资料上用于扫码流转。</div>
                    </div>
                  </div>
                  <div class="flow-code-meta">
                    <span>{{ currentOrderFlowCode.orderTypeLabel }}</span>
                    <span>{{ currentOrderFlowCode.currentStatusLabel }}</span>
                    <span>{{ currentOrderFlowCode.orderCategoryLabel }}</span>
                  </div>
                  <div class="flow-code-hint">系统会在订单审批通过后自动生成待打印任务；列表中的流转码按钮仅用于补打。</div>
                </div>
              </div>

              <div class="mt-6">
                <div class="section-title">状态流转记录</div>
                <div v-if="(productionDetail.logs || []).length" class="status-timeline">
                  <div
                      v-for="(log, index) in productionDetail.logs || []"
                      :key="log.id || index"
                      class="status-log-item"
                  >
                    <div class="status-log-rail">
                      <div class="status-log-dot" :class="{ current: index === 0 }"></div>
                    </div>
                    <div class="status-log-content">
                      <div class="flex flex-wrap items-center justify-between gap-2">
                        <div class="status-log-title">{{ logStatusTitle(log) }}</div>
                        <span v-if="index === 0" class="status-log-current">最新</span>
                      </div>
                      <div class="status-log-time">{{ formatDateTime(log.createTime) }}</div>
                      <div v-if="log.operatorName || log.operator" class="status-log-meta">
                        操作人：{{ log.operatorName || log.operator }}
                      </div>
                      <div v-if="log.remark" class="status-log-remark">备注：{{ log.remark }}</div>
                    </div>
                  </div>
                </div>
                <div v-else class="status-log-empty">暂无状态流转记录</div>
              </div>
            </template>
          </div>
        </aside>
      </transition>
    </Teleport>

    <Teleport defer to="body">
      <transition name="fade">
        <div v-if="formVisible" class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm" @click="closeForm"></div>
      </transition>
      <transition name="slide">
        <aside v-if="formVisible" class="drawer-small">
          <div class="drawer-head">
            <div>
              <h2 class="text-xl font-bold tracking-tight text-primary">
                {{ formMode === 'create' ? '新建订单' : '编辑订单' }}
              </h2>
              <p class="mt-1 text-xs text-on-surface-variant">
                录入订单基础信息、明细、交付与状态信息。
              </p>
            </div>
            <div class="drawer-head-actions">
              <button class="close-btn" @click="closeForm">
                <span class="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>
          </div>

          <div class="flex-1 space-y-6 overflow-y-auto p-6">
            <BusinessTimeCorrectionPanel
              v-model="currentFormCreateTime"
              :active="timeCorrectionMode"
              data-field="order.createTime"
              title="业务时间修正"
              label="业务时间"
              description="用于修正当前订单的业务时间。"
            />
            <template v-if="currentTab === 'sales'">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="relative">
                  <label class="field-label">客户名称 *</label>
                  <input v-model.trim="salesForm.customerName" data-field="sales.customerName" class="box-input pr-10" type="text"
                         placeholder="输入或选择客户"
                         autocomplete="off"
                         @focus="handleSalesCustomerFocus"
                         @input="handleSalesCustomerInput"
                         @blur="handleSalesCustomerBlur"/>
                  <span class="material-symbols-outlined combo-arrow">expand_more</span>
                  <div v-if="showCustomerOptions" class="combo-panel">
                    <button v-for="option in customerOptions" :key="option.id" type="button" class="combo-option"
                            @mousedown.prevent="chooseCustomer(option)">
                      <span class="font-bold text-on-surface">{{ option.customerName }}</span>
                      <span class="text-xs text-on-surface-variant">{{ option.contactPhone || '未维护电话' }}</span>
                      <span v-if="option.projectNames?.length" class="text-[11px] text-primary">
                        项目：{{ option.projectNames.slice(0, 2).join('、') }}
                      </span>
                    </button>
                  </div>
                  <p v-if="customerCreateHintVisible" class="mt-1 text-[11px] text-on-surface-variant">
                    未匹配到客户，保存后会自动归档到客户管理。
                  </p>
                </div>
                <div>
                  <label class="field-label">联系电话</label>
                  <input v-model.trim="salesForm.customerPhone" class="box-input" type="text"/>
                </div>
                <div class="relative">
                  <label class="field-label">项目名称 *</label>
                  <input v-model.trim="salesForm.projectName" data-field="sales.projectName" class="box-input pr-10" type="text"
                         placeholder="选择客户后自动带出，也可输入新项目"
                         autocomplete="off"
                         @focus="handleProjectFocus"
                         @input="handleProjectInput"
                         @blur="handleProjectBlur"/>
                  <span class="material-symbols-outlined combo-arrow">expand_more</span>
                  <div v-if="showProjectOptions" class="combo-panel">
                    <button v-for="projectName in selectedCustomerProjects" :key="projectName" type="button"
                            class="combo-option" @mousedown.prevent="chooseProject(projectName)">
                      <span class="font-bold text-on-surface">{{ projectName }}</span>
                    </button>
                  </div>
                  <p v-if="projectCreateHintVisible" class="mt-1 text-[11px] text-on-surface-variant">
                    新项目保存后会归档到该客户。
                  </p>
                </div>
                <div>
                  <label class="field-label">交付日期 *</label>
                  <input v-model="salesForm.deliveryDate" data-field="sales.deliveryDate" class="box-input" type="date"/>
                </div>
                <div>
                  <label class="field-label">品牌</label>
                  <input v-model.trim="salesForm.brandName" class="box-input" type="text" placeholder="请输入订单品牌"/>
                </div>
                <div>
                  <label class="field-label">订单小项</label>
                  <select v-model="salesForm.orderCategory" class="box-input">
                    <option v-for="option in productionOrderCategoryOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </div>
              </div>

              <div>
                <div class="flex items-end justify-between">
                  <div class="section-title">订单明细</div>
                  <button class="text-xs font-bold text-primary" @click="addSalesItem">添加商品</button>
                </div>
                <div v-for="(item, index) in salesForm.items" :key="index" class="detail-item">
                  <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <input v-model.trim="item.modelCode" :data-field="`sales.items.${index}.modelCode`" class="box-input" placeholder="型号" type="text"/>
                    <input v-model.number="item.quantity" :data-field="`sales.items.${index}.quantity`" class="box-input" placeholder="数量" type="number" min="0.01"
                           step="0.01"/>
                    <input v-model.number="item.weight" :data-field="`sales.items.${index}.weight`" class="box-input" placeholder="克重" type="number" min="0.01"
                           step="0.01"/>
                    <input v-model.number="item.spec" :data-field="`sales.items.${index}.spec`" class="box-input" placeholder="规格" type="number" min="0.01"
                           step="0.01"/>
                  </div>
                  <div class="mt-2 flex justify-end">
                    <button class="text-xs font-bold text-error" @click="removeSalesItem(index)">删除</button>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label class="field-label">订单状态</label>
                  <select v-model="salesForm.status" class="box-input">
                    <option v-for="status in salesStatuses" :key="status.value" :value="status.value">{{
                        status.label
                      }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="field-label">是否开票</label>
                  <select v-model.number="salesForm.isInvoice" class="box-input">
                    <option :value="0">未开票</option>
                    <option :value="1">已开票</option>
                  </select>
                </div>
                <div v-if="salesForm.status === 'shipped'">
                  <label class="field-label">物流公司</label>
                  <input v-model.trim="salesForm.expressCompany" data-field="sales.expressCompany" class="box-input" type="text"/>
                </div>
                <div v-if="salesForm.status === 'shipped'">
                  <label class="field-label">物流单号</label>
                  <input v-model.trim="salesForm.expressNo" data-field="sales.expressNo" class="box-input" type="text"/>
                </div>
              </div>
              <div>
                <label class="field-label">备注</label>
                <textarea v-model.trim="salesForm.remark" class="box-input min-h-[92px] resize-none"></textarea>
              </div>
              <div>
                <label class="field-label">订单附件</label>
                <input
                    ref="salesAttachmentInputRef"
                    type="file"
                    class="hidden"
                    accept=".pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar"
                    @change="handleSalesAttachmentChange"
                />
                <div class="attachment-uploader" @click="triggerSalesAttachmentUpload">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-2xl text-primary">
                      {{ salesAttachmentUploading ? 'progress_activity' : 'upload_file' }}
                    </span>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-sm font-bold text-primary">
                        {{ salesForm.attachmentName || '上传合同、客户需求或沟通截图' }}
                      </p>
                      <p class="mt-1 text-xs text-on-surface-variant">
                        支持 PDF、图片、Word、Excel、文本或压缩包，单个不超过 10MB
                        <template v-if="salesForm.attachmentSize"> · {{ formatFileSize(salesForm.attachmentSize) }}</template>
                      </p>
                    </div>
                  </div>
                  <div v-if="salesForm.attachmentUrl" class="mt-3 flex gap-2">
                    <span class="attachment-action text-primary">已上传</span>
                    <button type="button" class="attachment-action text-error" @click.stop="removeSalesAttachment">移除附件</button>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label class="field-label">关联销售单号</label>
                  <input v-model.trim="productionForm.salesOrderId" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">交付日期 *</label>
                  <input v-model="productionForm.deliveryDate" data-field="production.deliveryDate" class="box-input" type="date"/>
                </div>
                <div class="relative">
                  <label class="field-label">客户名称</label>
                  <input v-model.trim="productionForm.customerName" class="box-input pr-10" type="text"
                         placeholder="输入或选择客户"
                         autocomplete="off"
                         @focus="handleProductionCustomerFocus"
                         @input="handleProductionCustomerInput"
                         @blur="handleProductionCustomerBlur"/>
                  <span class="material-symbols-outlined combo-arrow">expand_more</span>
                  <div v-if="showProductionCustomerOptions" class="combo-panel">
                    <button v-for="option in customerOptions" :key="option.id" type="button" class="combo-option"
                            @mousedown.prevent="chooseProductionCustomer(option)">
                      <span class="font-bold text-on-surface">{{ option.customerName }}</span>
                      <span class="text-xs text-on-surface-variant">{{ option.contactPhone || '未维护电话' }}</span>
                      <span v-if="option.projectNames?.length" class="text-[11px] text-primary">
                        项目：{{ option.projectNames.slice(0, 2).join('、') }}
                      </span>
                    </button>
                  </div>
                  <p v-if="productionCustomerCreateHintVisible" class="mt-1 text-[11px] text-on-surface-variant">
                    未匹配到客户，保存后会自动归档到客户管理。
                  </p>
                </div>
                <div class="relative">
                  <label class="field-label">项目名称</label>
                  <input v-model.trim="productionForm.projectName" class="box-input pr-10" type="text"
                         placeholder="选择客户后自动带出，也可输入新项目"
                         autocomplete="off"
                         @focus="handleProductionProjectFocus"
                         @input="handleProductionProjectInput"
                         @blur="handleProductionProjectBlur"/>
                  <span class="material-symbols-outlined combo-arrow">expand_more</span>
                  <div v-if="showProductionProjectOptions" class="combo-panel">
                    <button v-for="projectName in selectedProductionCustomerProjects" :key="projectName" type="button"
                            class="combo-option" @mousedown.prevent="chooseProductionProject(projectName)">
                      <span class="font-bold text-on-surface">{{ projectName }}</span>
                    </button>
                  </div>
                  <p v-if="productionProjectCreateHintVisible" class="mt-1 text-[11px] text-on-surface-variant">
                    新项目保存后会归档到该客户。
                  </p>
                </div>
                <div>
                  <label class="field-label">联系电话</label>
                  <input v-model.trim="productionForm.contactPhone" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">品牌</label>
                  <input v-model.trim="productionForm.brandName" class="box-input" type="text" placeholder="请输入订单品牌"/>
                </div>
                <div>
                  <label class="field-label">订单小项</label>
                  <select v-model="productionForm.orderCategory" class="box-input">
                    <option v-for="option in orderCategoryOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="field-label">订单状态</label>
                  <select v-model="productionForm.status" class="box-input">
                    <option v-for="status in productionStatuses" :key="status.value" :value="status.value">
                      {{ status.label }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="field-label">面料型号 *</label>
                  <input v-model.trim="productionForm.modelCode" data-field="production.modelCode" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">数量 *</label>
                  <input v-model.number="productionForm.quantity" data-field="production.quantity" class="box-input" type="number" min="1" step="1"/>
                </div>
                <div>
                  <label class="field-label">克重 *</label>
                  <input v-model.number="productionForm.weight" data-field="production.weight" class="box-input" type="number" min="0.01" step="0.01"/>
                </div>
                <div>
                  <label class="field-label">规格 *</label>
                  <input v-model.number="productionForm.spec" data-field="production.spec" class="box-input" type="number" min="0.01" step="0.01"/>
                </div>
                <div>
                  <label class="field-label">面料材质</label>
                  <input v-model.trim="productionForm.fabric" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">颜色</label>
                  <input v-model.trim="productionForm.color" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">单价</label>
                  <input v-model.number="productionForm.price" class="box-input" type="number" min="0" step="0.01"/>
                </div>
                <div v-if="productionForm.status === 'producing'">
                  <label class="field-label">履约工序</label>
                  <select v-model="productionForm.process" class="box-input">
                    <option :value="null">未设置</option>
                    <option v-for="process in processOptions" :key="process.value" :value="process.value">
                      {{ process.label }}
                    </option>
                  </select>
                </div>
              </div>
              <div>
                <label class="field-label">备注</label>
                <textarea v-model.trim="productionForm.remark" class="box-input min-h-[92px] resize-none"></textarea>
              </div>
            </template>
          </div>
          <div
              class="flex shrink-0 items-center justify-end gap-3 border-t border-outline-variant/20 bg-surface-container-lowest p-6">
            <button class="rounded-lg px-5 py-2.5 text-sm font-bold text-secondary hover:bg-surface-container-high"
                    @click="closeForm">取消
            </button>
            <button :disabled="submitting"
                    class="rounded-lg bg-primary px-6 py-2.5 text-sm font-bold text-on-primary disabled:opacity-50"
                    @click="submitForm">
              {{ formMode === 'create' ? '提交创建' : '保存修改' }}
            </button>
          </div>
        </aside>
      </transition>
    </Teleport>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useRoute} from 'vue-router'
import {getCustomerOptions} from '../customer/api/customer'
import {warnAndFocusField} from '@/utils/formFocus'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import DateFilterInput from '@/components/DateFilterInput.vue'
import BusinessTimeCorrectionPanel from '@/components/BusinessTimeCorrectionPanel.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useTimeCorrectionMode } from '@/composables/useTimeCorrectionMode'
import { exportRowsToExcel } from '@/utils/tableExport'
import {
  createProductionOrder,
  createProductionOrderFlowPrintTask,
  createSalesOrder,
  createSalesOrderFlowPrintTask,
  getProductionOrderDetail,
  getProductionOrderPage,
  getProductionOrderStatusSummary,
  getOrderWarningSetting,
  getOrderWarningSummary,
  refreshOrderWarningSummary,
  getSalesOrderDetail,
  getSalesOrderPage,
  getSalesOrderStatusSummary,
  downloadSalesOrderAttachment,
  saveProductionOrder,
  saveSalesOrder,
  updateOrderWarningSetting,
  uploadSalesOrderAttachment
} from './api/order'

const route = useRoute()
const defaultOrderTableColumns = [
  {key: 'orderNo', label: '编号'},
  {key: 'customer', label: '客户 / 项目'},
  {key: 'core', label: '核心信息'},
  {key: 'status', label: '状态'},
  {key: 'progress', label: '进度'},
  {key: 'time', label: '时间'}
]
const {
  orderedColumns: orderTableColumns,
  moveColumn: moveOrderTableColumn,
  resetColumns: resetOrderTableColumns
} = useLocalTableColumns('order.list.commercial.v2', defaultOrderTableColumns)
const MAX_ORDER_EXPORT_ROWS = 2000
const orderTableColumnCount = computed(() => orderTableColumns.value.length + 1)
const orderColumnClass = (key) => `order-column-${key}`
const salesStatuses = [
  {value: 'budgeting', label: '预算中'},
  {value: 'budget_completed', label: '预算完成'},
  {value: 'pending_confirm', label: '待确认'},
  {value: 'pending_pay', label: '待收款'},
  {value: 'pending_material', label: '备料中'},
  {value: 'producing', label: '生产中'},
  {value: 'pending_ship', label: '待发货'},
  {value: 'shipped', label: '已发货'},
  {value: 'completed', label: '已完成'},
  {value: 'cancelled', label: '已取消'}
]
const productionStatuses = [
  {value: 'pending_confirm', label: '待确认'},
  {value: 'pending_material', label: '备料中'},
  {value: 'producing', label: '生产中'},
  {value: 'pending_ship', label: '待发货'},
  {value: 'shipped', label: '已发货'},
  {value: 'completed', label: '已完成'}
]
const processOptions = [
  {value: 0, label: '原料入库'},
  {value: 1, label: '原料检验'},
  {value: 2, label: '尺寸裁剪'},
  {value: 3, label: '窗帘缝制'},
  {value: 4, label: '窗帘熨烫'},
  {value: 5, label: '成品检验'},
  {value: 6, label: '高温定型'},
  {value: 7, label: '打包装箱'},
  {value: 8, label: '成品入库'},
  {value: 9, label: '成品发货'}
]
const orderCategoryOptions = [
  {value: 'sample_room', label: '样板间'},
  {value: 'bulk', label: '大货'},
  {value: 'replenishment', label: '补单'},
  {value: 'drawing_budget', label: '图纸预算'}
]
const productionOrderCategoryOptions = orderCategoryOptions.filter(option => option.value !== 'drawing_budget')

const filters = reactive({
  keyword: '',
  status: '',
  customerName: '',
  brandName: '',
  orderCategory: '',
  invoiceStatus: '',
  process: '',
  deliveryStart: '',
  deliveryEnd: '',
  createStart: '',
  createEnd: '',
  staleOnly: false
})
const currentTab = ref('sales')
const salesState = reactive({rows: [], page: 1, size: 10, total: 0, pages: 1, loading: false})
const productionState = reactive({rows: [], page: 1, size: 10, total: 0, pages: 1, loading: false})
const salesSummary = reactive({total: 0})
const productionSummary = reactive({total: 0})
const orderWarningSetting = reactive({
  staleWarningDays: 3,
  sampleRoomStaleWarningDays: 3,
  bulkStaleWarningDays: 3,
  replenishmentStaleWarningDays: 3,
  drawingBudgetStaleWarningDays: 3
})
const orderWarningSummary = reactive({
  staleWarningDays: 3,
  sampleRoomStaleWarningDays: 3,
  bulkStaleWarningDays: 3,
  replenishmentStaleWarningDays: 3,
  drawingBudgetStaleWarningDays: 3,
  salesCount: 0,
  productionCount: 0,
  totalCount: 0,
  sampleRoomCount: 0,
  bulkCount: 0,
  replenishmentCount: 0,
  drawingBudgetCount: 0
})
const warningRefreshing = ref(false)
const rowWarningRefreshingKey = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const salesDetail = ref(null)
const productionDetail = ref(null)
const customerOptions = ref([])
const customerDropdownVisible = ref(false)
const projectDropdownVisible = ref(false)
const productionCustomerDropdownVisible = ref(false)
const productionProjectDropdownVisible = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const editingOrderId = ref('')
const submitting = ref(false)
const {
  timeCorrectionMode,
  closeTimeCorrectionMode
} = useTimeCorrectionMode({
  isAvailable: () => formVisible.value
})
const salesForm = reactive(defaultSalesForm())
const productionForm = reactive(defaultProductionForm())
const salesAttachmentInputRef = ref(null)
const salesAttachmentUploading = ref(false)
const latestOrderFlowPrintTask = ref(null)

const currentStatuses = computed(() => salesStatuses)
const currentState = computed(() => salesState)
const currentSummary = computed(() => salesSummary)
const currentStatusTabs = computed(() => [
  {value: '', label: '全部订单', count: currentSummary.value.total || 0},
  ...currentStatuses.value.map(status => ({
    ...status,
    label: `${status.label}订单`,
    count: currentSummary.value[status.value] || 0
  }))
])
const orderWarningHint = computed(() => {
  const sampleDays = orderWarningSummary.sampleRoomStaleWarningDays || orderWarningSetting.sampleRoomStaleWarningDays || 3
  const bulkDays = orderWarningSummary.bulkStaleWarningDays || orderWarningSetting.bulkStaleWarningDays || 3
  const replenishmentDays = orderWarningSummary.replenishmentStaleWarningDays || orderWarningSetting.replenishmentStaleWarningDays || 3
  const drawingDays = orderWarningSummary.drawingBudgetStaleWarningDays || orderWarningSetting.drawingBudgetStaleWarningDays || 3
  return `样板间${sampleDays}天 / 大货${bulkDays}天 / 补单${replenishmentDays}天 / 图纸预算${drawingDays}天`
})
const summaryCards = computed(() => {
  return [
    {key: 'sales-total', tab: 'sales', label: '订单总量', status: '', count: salesSummary.total || salesState.total || 0, hint: '全部订单'},
    {key: 'sales-budgeting', tab: 'sales', label: '预算中订单', status: 'budgeting', count: salesSummary.budgeting || 0, hint: '图纸预算测算中'},
    {key: 'sales-budget-done', tab: 'sales', label: '预算完成', status: 'budget_completed', count: salesSummary.budget_completed || 0, hint: '预算可交付客户确认'},
    {key: 'sales-confirm', tab: 'sales', label: '待确认订单', status: 'pending_confirm', count: salesSummary.pending_confirm || 0, hint: '待客户/业务确认'},
    {key: 'sales-pay', tab: 'sales', label: '待收款订单', status: 'pending_pay', count: salesSummary.pending_pay || 0, hint: '待收款跟进'},
    {key: 'sales-ship', tab: 'sales', label: '待发货订单', status: 'pending_ship', count: salesSummary.pending_ship || 0, hint: '待安排发货'},
    {key: 'sales-stale', tab: 'sales', label: '未更新预警', staleOnly: true, count: orderWarningSummary.salesCount || 0, hint: orderWarningHint.value}
  ]
})
const categorySummaryCards = computed(() => orderCategoryOptions.map(option => ({
  key: `category-${option.value}`,
  value: option.value,
  label: option.label,
  count: salesSummary[`category_${option.value}`] || 0,
  hint: option.value === 'sample_room'
      ? '样板订单'
      : option.value === 'replenishment'
          ? '补充订单'
          : option.value === 'drawing_budget'
              ? '图纸预算订单'
              : '大货订单'
})))
const selectedCustomerOption = computed(() => {
  const customerName = normalizeText(salesForm.customerName)
  if (!customerName) return null
  return customerOptions.value.find(item => normalizeText(item.customerName) === customerName) || null
})
const selectedProductionCustomerOption = computed(() => {
  const customerName = normalizeText(productionForm.customerName)
  if (!customerName) return null
  return customerOptions.value.find(item => normalizeText(item.customerName) === customerName) || null
})
const selectedCustomerProjects = computed(() => selectedCustomerOption.value?.projectNames || [])
const selectedProductionCustomerProjects = computed(() => selectedProductionCustomerOption.value?.projectNames || [])
const showCustomerOptions = computed(() => customerDropdownVisible.value && customerOptions.value.length > 0)
const showProjectOptions = computed(() => projectDropdownVisible.value && selectedCustomerProjects.value.length > 0)
const showProductionCustomerOptions = computed(() => productionCustomerDropdownVisible.value && customerOptions.value.length > 0)
const showProductionProjectOptions = computed(() => productionProjectDropdownVisible.value && selectedProductionCustomerProjects.value.length > 0)
const customerCreateHintVisible = computed(() => Boolean(normalizeText(salesForm.customerName)) && !selectedCustomerOption.value)
const projectCreateHintVisible = computed(() => {
  const projectName = normalizeText(salesForm.projectName)
  return Boolean(selectedCustomerOption.value && projectName && !selectedCustomerProjects.value.includes(projectName))
})
const productionCustomerCreateHintVisible = computed(() => Boolean(normalizeText(productionForm.customerName)) && !selectedProductionCustomerOption.value)
const productionProjectCreateHintVisible = computed(() => {
  const projectName = normalizeText(productionForm.projectName)
  return Boolean(selectedProductionCustomerOption.value && projectName && !selectedProductionCustomerProjects.value.includes(projectName))
})
const currentFormCreateTime = computed({
  get() {
    return salesForm.createTime
  },
  set(value) {
    salesForm.createTime = value
  }
})
const currentOrderFlowCode = computed(() => {
  const detail = salesDetail.value
  const task = latestOrderFlowPrintTask.value
  if (task?.orderId && detail?.orderId === task.orderId && task.orderType === 'sales' && task.printPayload) {
    return buildOrderFlowCodeFromTask(task)
  }
  return buildOrderFlowCode(detail, 'sales')
})

onMounted(async () => {
  applyRouteSearch()
  await Promise.all([loadSalesOrders(), loadOrderSummaries(), loadCustomerOptions()])
})

watch(
  () => [route.query.keyword, route.query.q, route.query.tab],
  async () => {
    applyRouteSearch()
    await refreshCurrentTab()
  }
)

watch(
  () => salesForm.orderCategory,
  (category) => {
    if (category === 'drawing_budget') {
      if (!['budgeting', 'budget_completed'].includes(salesForm.status)) {
        salesForm.status = 'budgeting'
      }
      salesForm.createProductionOrder = 0
      return
    }
    if (['budgeting', 'budget_completed'].includes(salesForm.status)) {
      salesForm.status = 'pending_confirm'
    }
  }
)

function applyRouteSearch() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== filters.keyword) {
    filters.keyword = routeKeyword
    salesState.page = 1
  }
}

function defaultSalesItem() {
  return {modelCode: '', quantity: 1, weight: '', spec: ''}
}

function defaultSalesForm() {
  return {
    customerName: '',
    customerPhone: '',
    projectName: '',
    brandName: '',
    orderCategory: 'bulk',
    deliveryDate: '',
    createTime: '',
    expressCompany: '',
    expressNo: '',
    isInvoice: 0,
    remark: '',
    attachmentName: '',
    attachmentUrl: '',
    attachmentSize: null,
    status: 'pending_confirm',
    createProductionOrder: 1,
    items: [defaultSalesItem()]
  }
}

function defaultProductionForm() {
  return {
    salesOrderId: '',
    customerName: '',
    projectName: '',
    brandName: '',
    orderCategory: 'bulk',
    contactPhone: '',
    modelCode: '',
    fabric: '',
    weight: '',
    spec: '',
    color: '',
    quantity: 1,
    price: '',
    deliveryDate: '',
    createTime: '',
    status: 'pending_confirm',
    process: null,
    remark: ''
  }
}

function resetSalesForm() {
  Object.assign(salesForm, defaultSalesForm())
  customerDropdownVisible.value = false
  projectDropdownVisible.value = false
}

function resetProductionForm() {
  Object.assign(productionForm, defaultProductionForm())
  productionCustomerDropdownVisible.value = false
  productionProjectDropdownVisible.value = false
}

async function loadCustomerOptions(keyword) {
  const result = await getCustomerOptions(keyword ? {keyword} : undefined)
  customerOptions.value = Array.isArray(result) ? result : []
}

async function handleSalesCustomerInput() {
  customerDropdownVisible.value = true
  const keyword = normalizeText(salesForm.customerName)
  await loadCustomerOptions(keyword || undefined)
  if (!keyword) {
    salesForm.projectName = ''
    return
  }
  applyMatchedCustomer(false)
}

async function handleSalesCustomerFocus() {
  customerDropdownVisible.value = true
  await loadCustomerOptions(normalizeText(salesForm.customerName) || undefined)
}

function handleSalesCustomerBlur() {
  setTimeout(() => {
    customerDropdownVisible.value = false
    applyMatchedCustomer(true)
  }, 160)
}

function chooseCustomer(option) {
  if (!option) {
    return
  }
  salesForm.customerName = option.customerName || ''
  if (option.contactPhone) {
    salesForm.customerPhone = option.contactPhone
  }
  applyCustomerProject(option, true)
  customerDropdownVisible.value = false
  projectDropdownVisible.value = Boolean(option.projectNames?.length > 1)
}

function applyMatchedCustomer(autoFillProject) {
  const option = selectedCustomerOption.value
  if (!option) {
    return
  }
  if (option.contactPhone && !salesForm.customerPhone) {
    salesForm.customerPhone = option.contactPhone
  }
  applyCustomerProject(option, autoFillProject)
}

function applyCustomerProject(option, forceFill) {
  const projectNames = Array.isArray(option?.projectNames) ? option.projectNames : []
  if (!projectNames.length) {
    return
  }
  if (forceFill || !salesForm.projectName || !projectNames.includes(salesForm.projectName)) {
    salesForm.projectName = projectNames[0]
  }
}

function handleProjectFocus() {
  projectDropdownVisible.value = true
}

function handleProjectInput() {
  projectDropdownVisible.value = true
}

function handleProjectBlur() {
  setTimeout(() => {
    projectDropdownVisible.value = false
  }, 160)
}

function chooseProject(projectName) {
  salesForm.projectName = projectName
  projectDropdownVisible.value = false
}

async function handleProductionCustomerInput() {
  productionCustomerDropdownVisible.value = true
  const keyword = normalizeText(productionForm.customerName)
  await loadCustomerOptions(keyword || undefined)
  if (!keyword) {
    productionForm.projectName = ''
    return
  }
  applyMatchedProductionCustomer(false)
}

async function handleProductionCustomerFocus() {
  productionCustomerDropdownVisible.value = true
  await loadCustomerOptions(normalizeText(productionForm.customerName) || undefined)
}

function handleProductionCustomerBlur() {
  setTimeout(() => {
    productionCustomerDropdownVisible.value = false
    applyMatchedProductionCustomer(true)
  }, 160)
}

function chooseProductionCustomer(option) {
  if (!option) {
    return
  }
  productionForm.customerName = option.customerName || ''
  if (option.contactPhone) {
    productionForm.contactPhone = option.contactPhone
  }
  applyProductionCustomerProject(option, true)
  productionCustomerDropdownVisible.value = false
  productionProjectDropdownVisible.value = Boolean(option.projectNames?.length > 1)
}

function applyMatchedProductionCustomer(autoFillProject) {
  const option = selectedProductionCustomerOption.value
  if (!option) {
    return
  }
  if (option.contactPhone && !productionForm.contactPhone) {
    productionForm.contactPhone = option.contactPhone
  }
  applyProductionCustomerProject(option, autoFillProject)
}

function applyProductionCustomerProject(option, forceFill) {
  const projectNames = Array.isArray(option?.projectNames) ? option.projectNames : []
  if (!projectNames.length) {
    return
  }
  if (forceFill || !productionForm.projectName || !projectNames.includes(productionForm.projectName)) {
    productionForm.projectName = projectNames[0]
  }
}

function handleProductionProjectFocus() {
  productionProjectDropdownVisible.value = true
}

function handleProductionProjectInput() {
  productionProjectDropdownVisible.value = true
}

function handleProductionProjectBlur() {
  setTimeout(() => {
    productionProjectDropdownVisible.value = false
  }, 160)
}

function chooseProductionProject(projectName) {
  productionForm.projectName = projectName
  productionProjectDropdownVisible.value = false
}

function switchTab(tabId) {
  currentTab.value = 'sales'
  if (tabId !== 'sales') {
    resetFilters(false)
  }
}

async function selectSummaryCard(card) {
  if (!card) return
  if (card.staleOnly) {
    filters.status = ''
    filters.staleOnly = true
    await refreshCurrentTab()
    return
  }
  filters.staleOnly = false
  await selectStatus(card.status || '')
}

async function selectStatus(status) {
  const nextStatus = status || ''
  if (filters.status === nextStatus && !filters.staleOnly) {
    return
  }
  filters.staleOnly = false
  filters.status = nextStatus
  await refreshCurrentTab()
}

async function selectCategoryCard(category) {
  const nextCategory = filters.orderCategory === category ? '' : category
  filters.staleOnly = false
  filters.orderCategory = nextCategory
  await refreshCurrentTab()
}

function isSummaryCardActive(card) {
  if (!card) {
    return false
  }
  if (card.staleOnly) {
    return Boolean(filters.staleOnly)
  }
  return !filters.staleOnly && card.status === filters.status
}

async function resetFilters(refresh = true) {
  filters.keyword = ''
  filters.status = ''
  filters.customerName = ''
  filters.brandName = ''
  filters.orderCategory = ''
  filters.invoiceStatus = ''
  filters.process = ''
  filters.deliveryStart = ''
  filters.deliveryEnd = ''
  filters.createStart = ''
  filters.createEnd = ''
  filters.staleOnly = false
  salesState.page = 1
  if (refresh) {
    await refreshCurrentTab()
  }
}

async function refreshCurrentTab() {
  salesState.page = 1
  await loadSalesOrders()
}

async function loadOrderSummaries() {
  await Promise.all([loadSalesSummary(), loadOrderWarningSummary()])
}

async function loadSalesSummary() {
  try {
    assignSummary(salesSummary, await getSalesOrderStatusSummary())
  } catch (error) {
    console.warn('加载订单统计失败', error)
  }
}

async function loadProductionSummary() {
  try {
    assignSummary(productionSummary, await getProductionOrderStatusSummary())
  } catch (error) {
    console.warn('加载履约统计失败', error)
  }
}

async function loadOrderWarningSummary() {
  try {
    const [setting, summary] = await Promise.all([getOrderWarningSetting(), getOrderWarningSummary()])
    assignWarningSetting(orderWarningSetting, setting)
    applyOrderWarningSummary(summary)
  } catch (error) {
    console.warn('加载订单预警统计失败', error)
  }
}

function applyOrderWarningSummary(summary = {}) {
  assignWarningSetting(orderWarningSummary, summary, orderWarningSetting)
  orderWarningSummary.salesCount = Number(summary?.salesCount || 0)
  orderWarningSummary.productionCount = Number(summary?.productionCount || 0)
  orderWarningSummary.totalCount = Number(summary?.totalCount || 0)
  orderWarningSummary.sampleRoomCount = Number(summary?.sampleRoomCount || 0)
  orderWarningSummary.bulkCount = Number(summary?.bulkCount || 0)
  orderWarningSummary.replenishmentCount = Number(summary?.replenishmentCount || 0)
  orderWarningSummary.drawingBudgetCount = Number(summary?.drawingBudgetCount || 0)
}

function assignWarningSetting(target, source = {}, fallback = {}) {
  target.staleWarningDays = normalizeWarningDayValue(source?.staleWarningDays, fallback.staleWarningDays || 3)
  target.sampleRoomStaleWarningDays = normalizeWarningDayValue(
      source?.sampleRoomStaleWarningDays,
      fallback.sampleRoomStaleWarningDays || target.staleWarningDays
  )
  target.bulkStaleWarningDays = normalizeWarningDayValue(
      source?.bulkStaleWarningDays,
      fallback.bulkStaleWarningDays || target.staleWarningDays
  )
  target.replenishmentStaleWarningDays = normalizeWarningDayValue(
      source?.replenishmentStaleWarningDays,
      fallback.replenishmentStaleWarningDays || target.staleWarningDays
  )
  target.drawingBudgetStaleWarningDays = normalizeWarningDayValue(
      source?.drawingBudgetStaleWarningDays,
      fallback.drawingBudgetStaleWarningDays || target.staleWarningDays
  )
}

function normalizeWarningDayValue(value, fallback = 3) {
  const numeric = Number(value)
  if (Number.isInteger(numeric) && numeric >= 1 && numeric <= 365) {
    return numeric
  }
  const fallbackNumber = Number(fallback)
  return Number.isInteger(fallbackNumber) && fallbackNumber >= 1 && fallbackNumber <= 365 ? fallbackNumber : 3
}

async function openOrderWarningSetting() {
  let nextSetting = null
  try {
    await ElMessageBox({
        title: '订单未更新预警设置',
        dangerouslyUseHTMLString: true,
        showCancelButton: true,
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        message: buildOrderWarningSettingHtml(),
        beforeClose: (action, instance, done) => {
          if (action !== 'confirm') {
            done()
            return
          }
          try {
            nextSetting = readOrderWarningSettingFromDialog()
            done()
          } catch (error) {
            ElMessage.warning(error.message || '请输入 1-365 之间的整数')
          }
        }
    })
    if (!nextSetting) {
      return
    }
    const setting = await updateOrderWarningSetting(nextSetting)
    assignWarningSetting(orderWarningSetting, setting, nextSetting)
    ElMessage.success('订单预警设置已保存')
    await Promise.all([loadOrderSummaries(), refreshCurrentTab()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

function buildOrderWarningSettingHtml() {
  return `
    <div class="order-warning-dialog" data-order-warning-form>
      <p class="order-warning-dialog__desc">不同订单小项可以设置不同未更新预警天数。系统采用懒查询策略，只在打开订单页、筛选预警或手动重新更新时计算，不做后台轮询。</p>
      ${buildOrderWarningInputHtml('sampleRoomStaleWarningDays', '样板间', orderWarningSetting.sampleRoomStaleWarningDays)}
      ${buildOrderWarningInputHtml('bulkStaleWarningDays', '大货', orderWarningSetting.bulkStaleWarningDays)}
      ${buildOrderWarningInputHtml('replenishmentStaleWarningDays', '补单', orderWarningSetting.replenishmentStaleWarningDays)}
      ${buildOrderWarningInputHtml('drawingBudgetStaleWarningDays', '图纸预算', orderWarningSetting.drawingBudgetStaleWarningDays)}
    </div>
  `
}

function buildOrderWarningInputHtml(name, label, value) {
  return `
    <label class="order-warning-dialog__row">
      <span>${label}</span>
      <input data-field="${name}" value="${normalizeWarningDayValue(value)}" inputmode="numeric" />
      <em>天未更新后预警</em>
    </label>
  `
}

function readOrderWarningSettingFromDialog() {
  const root = document.querySelector('[data-order-warning-form]')
  if (!root) {
    throw new Error('预警设置窗口已关闭，请重新打开')
  }
  const read = (field, label) => {
    const input = root.querySelector(`[data-field="${field}"]`)
    const value = Number(input?.value)
    if (!Number.isInteger(value) || value < 1 || value > 365) {
      throw new Error(`${label}预警天数请输入 1-365 之间的整数`)
    }
    return value
  }
  const sampleRoomStaleWarningDays = read('sampleRoomStaleWarningDays', '样板间')
  const bulkStaleWarningDays = read('bulkStaleWarningDays', '大货')
  const replenishmentStaleWarningDays = read('replenishmentStaleWarningDays', '补单')
  const drawingBudgetStaleWarningDays = read('drawingBudgetStaleWarningDays', '图纸预算')
  return {
    staleWarningDays: bulkStaleWarningDays,
    sampleRoomStaleWarningDays,
    bulkStaleWarningDays,
    replenishmentStaleWarningDays,
    drawingBudgetStaleWarningDays
  }
}

async function refreshOrderWarnings(showToast = false) {
  if (warningRefreshing.value) {
    return
  }
  warningRefreshing.value = true
  try {
    const summary = await refreshOrderWarningSummary()
    applyOrderWarningSummary(summary)
    await refreshCurrentTab()
    if (showToast) {
      ElMessage.success('订单预警已重新更新')
    }
  } finally {
    warningRefreshing.value = false
  }
}

function warningRowKey(row = {}) {
  return row?.orderId ? String(row.orderId) : ''
}

function isRowWarningRefreshing(row = {}) {
  const key = warningRowKey(row)
  return Boolean(key && rowWarningRefreshingKey.value === key)
}

async function refreshSingleOrderWarning(row = {}) {
  const key = warningRowKey(row)
  if (!key || warningRefreshing.value || rowWarningRefreshingKey.value) {
    return
  }
  rowWarningRefreshingKey.value = key
  try {
    const summary = await refreshOrderWarningSummary()
    applyOrderWarningSummary(summary)
    await loadSalesOrders()
    ElMessage.success('该订单预警已重新计算')
  } finally {
    rowWarningRefreshingKey.value = ''
  }
}

function assignSummary(target, source = {}) {
  Object.keys(target).forEach(key => delete target[key])
  Object.entries(source || {}).forEach(([key, value]) => {
    target[key] = Number(value || 0)
  })
}

async function changePage(page) {
  salesState.page = page
  await loadSalesOrders()
}

function buildSalesOrderQuery(pageNum = salesState.page, pageSize = salesState.size) {
  return {
    pageNum,
    pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    customerName: filters.customerName || undefined,
    brandName: filters.brandName || undefined,
    orderCategory: filters.orderCategory || undefined,
    isInvoice: filters.invoiceStatus === '' ? undefined : Number(filters.invoiceStatus),
    deliveryStart: filters.deliveryStart || undefined,
    deliveryEnd: filters.deliveryEnd || undefined,
    createStart: filters.createStart || undefined,
    createEnd: filters.createEnd || undefined,
    staleOnly: filters.staleOnly || undefined
  }
}

async function loadSalesOrders() {
  salesState.loading = true
  try {
    const res = await getSalesOrderPage(buildSalesOrderQuery())
    salesState.rows = res.data || []
    salesState.total = res.total || 0
    salesState.pages = res.pages || 1
  } finally {
    salesState.loading = false
  }
}

async function exportAllOrders() {
  const total = Number(salesState.total || 0)
  if (total <= 0) {
    ElMessage.warning('暂无可导出的订单数据')
    return
  }
  if (total > MAX_ORDER_EXPORT_ROWS) {
    ElMessage.warning(`当前筛选结果 ${total} 条，单次最多导出 ${MAX_ORDER_EXPORT_ROWS} 条，请缩小筛选范围`)
    return
  }
  const res = await getSalesOrderPage(buildSalesOrderQuery(1, Math.max(1, total)))
  const rows = Array.isArray(res.data) ? res.data : []
  await exportRowsToExcel({
    title: '订单列表',
    fileName: '订单列表',
    sheetName: '订单列表',
    sourceModule: 'order',
    headers: orderTableColumns.value.map((column) => column.label),
    rows: rows.map((row) => orderTableColumns.value.map((column) => formatOrderExportCell(row, column.key)))
  })
  ElMessage.success('订单列表已导出')
}

function formatOrderExportCell(row, key) {
  if (!row) return ''
  if (key === 'orderNo') return row.orderId || ''
  if (key === 'category') return orderCategoryLabel(row.orderCategory)
  if (key === 'customer') return [row.customerName, row.projectName].filter(Boolean).join(' / ')
  if (key === 'brand') return row.brandName || ''
  if (key === 'core') return `${row.goodsDesc || ''} 数量${row.totalQuantity || 0} 金额${formatAmount(row.totalAmount)}`
  if (key === 'delivery') return [row.deliveryDate, row.expressCompany, row.expressNo].filter(Boolean).join(' / ')
  if (key === 'invoice') return invoiceLabel(row.isInvoice)
  if (key === 'status') return salesStatusLabel(row.status)
  if (key === 'progress') {
    const progress = orderProgress(row)
    return `${progress.label} ${progress.percent}%`
  }
  if (key === 'time') return formatDateTime(row.createTime)
  return ''
}

async function loadProductionOrders() {
  productionState.loading = true
  try {
    const res = await getProductionOrderPage({
      pageNum: productionState.page,
      pageSize: productionState.size,
      keyword: filters.keyword || undefined,
      status: currentTab.value === 'production' ? filters.status || undefined : undefined,
      customerName: filters.customerName || undefined,
      brandName: filters.brandName || undefined,
      orderCategory: filters.orderCategory || undefined,
      process: filters.process === '' ? undefined : Number(filters.process),
      deliveryStart: filters.deliveryStart || undefined,
      deliveryEnd: filters.deliveryEnd || undefined,
      createStart: filters.createStart || undefined,
      createEnd: filters.createEnd || undefined,
      staleOnly: filters.staleOnly || undefined
    })
    productionState.rows = res.data || []
    productionState.total = res.total || 0
    productionState.pages = res.pages || 1
  } finally {
    productionState.loading = false
  }
}

async function openDetail(orderId) {
  detailVisible.value = true
  detailLoading.value = true
  salesDetail.value = null
  productionDetail.value = null
  try {
    salesDetail.value = await getSalesOrderDetail(orderId)
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailVisible.value = false
}

async function openFlowCode(row) {
  if (!row?.orderId) {
    ElMessage.warning('订单信息异常，无法补打流转码')
    return
  }
  const task = await createSalesOrderFlowPrintTask({orderId: row.orderId})
  latestOrderFlowPrintTask.value = task
  await openDetail(row.orderId)
  ElMessage.success('已创建补打任务，请到小程序待打印队列处理')
}

function openCreate() {
  formMode.value = 'create'
  editingOrderId.value = ''
  resetSalesForm()
  formVisible.value = true
}

async function openEdit(orderId) {
  formMode.value = 'edit'
  editingOrderId.value = orderId
  formVisible.value = true
  resetSalesForm()
  const detail = await getSalesOrderDetail(orderId)
  salesForm.customerName = detail.customerName || ''
  salesForm.customerPhone = detail.customerPhone || ''
  salesForm.projectName = detail.projectName || ''
  salesForm.brandName = detail.brandName || ''
  salesForm.orderCategory = normalizeOrderCategory(detail.orderCategory)
  salesForm.deliveryDate = toDateInput(detail.deliveryDate)
  salesForm.createTime = toDateTimeLocal(detail.createTime)
  salesForm.expressCompany = detail.expressCompany || ''
  salesForm.expressNo = detail.expressNo || ''
  salesForm.isInvoice = Number(detail.isInvoice || 0)
  salesForm.remark = detail.remark || ''
  salesForm.attachmentName = detail.attachmentName || ''
  salesForm.attachmentUrl = detail.attachmentUrl || ''
  salesForm.attachmentSize = detail.attachmentSize || null
  salesForm.status = detail.status || 'pending_confirm'
  salesForm.createProductionOrder = 0
  salesForm.items = (detail.items || []).length
      ? detail.items.map(item => ({
        modelCode: item.modelCode || '',
        quantity: num(item.quantity),
        weight: num(item.weight),
        spec: num(item.spec)
      }))
      : [defaultSalesItem()]
}

function closeForm() {
  formVisible.value = false
  closeTimeCorrectionMode()
  customerDropdownVisible.value = false
  projectDropdownVisible.value = false
  productionCustomerDropdownVisible.value = false
  productionProjectDropdownVisible.value = false
}

function addSalesItem() {
  salesForm.items.push(defaultSalesItem())
}

function removeSalesItem(index) {
  if (salesForm.items.length === 1) {
    ElMessage.warning('至少保留一个商品明细')
    return
  }
  salesForm.items.splice(index, 1)
}

function triggerSalesAttachmentUpload() {
  salesAttachmentInputRef.value?.click()
}

async function handleSalesAttachmentChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('订单附件不能超过 10MB')
    event.target.value = ''
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  salesAttachmentUploading.value = true
  try {
    const result = await uploadSalesOrderAttachment(formData)
    salesForm.attachmentName = result.fileName || file.name
    salesForm.attachmentUrl = result.fileUrl || ''
    salesForm.attachmentSize = result.fileSize || file.size
    ElMessage.success('订单附件上传成功')
  } finally {
    salesAttachmentUploading.value = false
    event.target.value = ''
  }
}

function removeSalesAttachment() {
  salesForm.attachmentName = ''
  salesForm.attachmentUrl = ''
  salesForm.attachmentSize = null
}

function openSalesAttachment() {
  openAttachmentUrl(salesForm.attachmentUrl, salesForm.attachmentName)
}

async function openAttachmentUrl(url, name) {
  if (!url) {
    return
  }
  const blob = await downloadSalesOrderAttachment({url, name})
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name || 'order-attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

async function submitForm() {
  submitting.value = true
  try {
    validateSalesForm()
    const payload = buildSalesPayload()
    if (formMode.value === 'create') {
      await createSalesOrder(payload)
    } else {
      await saveSalesOrder(editingOrderId.value, payload)
    }
    ElMessage.success(formMode.value === 'create' ? '订单创建成功' : '订单保存成功')
    closeForm()
    await loadSalesOrders()
    await loadOrderSummaries()
  } finally {
    submitting.value = false
  }
}

function validateSalesForm() {
  if (!salesForm.customerName.trim()) fail('请输入客户名称', 'sales.customerName')
  if (!salesForm.projectName.trim()) fail('请输入项目名称', 'sales.projectName')
  if (!salesForm.deliveryDate) fail('请选择交付日期', 'sales.deliveryDate')
  validateCreateTimeInput(salesForm.createTime)
  if (!salesForm.items.length) fail('请至少添加一个商品明细', 'sales.items')
  if (salesForm.status === 'shipped' && !String(salesForm.expressCompany || '').trim()) {
    fail('订单变更为已发货时必须填写物流公司', 'sales.expressCompany')
  }
  if (salesForm.status === 'shipped' && !String(salesForm.expressNo || '').trim()) {
    fail('订单变更为已发货时必须填写物流单号', 'sales.expressNo')
  }
  salesForm.items.forEach((item, index) => {
    const label = `第 ${index + 1} 项`
    if (!String(item.modelCode || '').trim()) fail(`${label}型号不能为空`, `sales.items.${index}.modelCode`)
    if (!Number(item.quantity) || Number(item.quantity) <= 0) fail(`${label}数量必须大于 0`, `sales.items.${index}.quantity`)
    if (!Number(item.weight) || Number(item.weight) <= 0) fail(`${label}克重必须大于 0`, `sales.items.${index}.weight`)
    if (!Number(item.spec) || Number(item.spec) <= 0) fail(`${label}规格必须大于 0`, `sales.items.${index}.spec`)
  })
}

function validateProductionForm() {
  if (!productionForm.modelCode.trim()) fail('请输入面料型号', 'production.modelCode')
  if (!productionForm.deliveryDate) fail('请选择交付日期', 'production.deliveryDate')
  validateCreateTimeInput(productionForm.createTime)
  if (!Number(productionForm.weight) || Number(productionForm.weight) <= 0) fail('克重必须大于 0', 'production.weight')
  if (!Number(productionForm.spec) || Number(productionForm.spec) <= 0) fail('规格必须大于 0', 'production.spec')
  if (!Number(productionForm.quantity) || Number(productionForm.quantity) < 1) fail('数量至少为 1', 'production.quantity')
}

function buildSalesPayload() {
  const orderCategory = normalizeOrderCategory(salesForm.orderCategory)
  return {
    customerName: salesForm.customerName.trim(),
    customerPhone: blank(salesForm.customerPhone),
    projectName: salesForm.projectName.trim(),
    brandName: blank(salesForm.brandName),
    orderCategory,
    deliveryDate: blank(salesForm.deliveryDate),
    createTime: blank(formatCreateTimePayload(salesForm.createTime)),
    expressCompany: blank(salesForm.expressCompany),
    expressNo: blank(salesForm.expressNo),
    isInvoice: Number(salesForm.isInvoice || 0),
    remark: blank(salesForm.remark),
    attachmentName: blank(salesForm.attachmentName),
    attachmentUrl: blank(salesForm.attachmentUrl),
    attachmentSize: salesForm.attachmentSize || null,
    status: salesForm.status,
    createProductionOrder: formMode.value === 'create' && orderCategory !== 'drawing_budget'
        ? Number(salesForm.createProductionOrder || 0)
        : 0,
    items: salesForm.items.map(item => ({
      modelCode: item.modelCode.trim(),
      quantity: Number(item.quantity),
      weight: Number(item.weight),
      spec: Number(item.spec)
    }))
  }
}

function buildProductionPayload() {
  return {
    salesOrderId: blank(productionForm.salesOrderId),
    customerName: blank(productionForm.customerName),
    projectName: blank(productionForm.projectName),
    brandName: blank(productionForm.brandName),
    orderCategory: normalizeOrderCategory(productionForm.orderCategory),
    contactPhone: blank(productionForm.contactPhone),
    modelCode: productionForm.modelCode.trim(),
    fabric: blank(productionForm.fabric),
    weight: Number(productionForm.weight),
    spec: Number(productionForm.spec),
    color: blank(productionForm.color),
    quantity: Number(productionForm.quantity),
    price: productionForm.price === '' || productionForm.price === null ? null : Number(productionForm.price),
    deliveryDate: `${productionForm.deliveryDate} 00:00:00`,
    createTime: blank(formatCreateTimePayload(productionForm.createTime)),
    status: productionForm.status,
    process: productionForm.status === 'producing' && productionForm.process !== null && productionForm.process !== '' ? Number(productionForm.process) : null,
    remark: blank(productionForm.remark)
  }
}

function logStatusTitle(log) {
  const oldStatus = log?.oldStatus ? productionStatusLabel(log.oldStatus) : ''
  const newStatus = log?.newStatus ? productionStatusLabel(log.newStatus) : '状态更新'
  return oldStatus ? `${oldStatus} → ${newStatus}` : newStatus
}

function salesLogTitle(log) {
  const oldStatus = log?.oldStatus ? salesStatusLabel(log.oldStatus) : ''
  const newStatus = log?.newStatus ? salesStatusLabel(log.newStatus) : '状态更新'
  return oldStatus ? `${oldStatus} → ${newStatus}` : newStatus
}

function fail(message, field) {
  warnAndFocusField(message, field)
  throw new Error(message)
}

function blank(value) {
  const text = String(value || '').trim()
  return text ? text : null
}

function validateCreateTimeInput(value) {
  const text = String(value || '').trim()
  if (!text) {
    return
  }
  const normalized = text.replace(' ', 'T')
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(normalized)) {
    fail('创建时间格式不正确，请选择完整日期和时间', 'order.createTime')
  }
  const timestamp = new Date(normalized).getTime()
  if (!Number.isFinite(timestamp)) {
    fail('创建时间无效，请重新选择', 'order.createTime')
  }
  if (timestamp > Date.now() + 60 * 1000) {
    fail('创建时间不能晚于当前时间', 'order.createTime')
  }
}

function formatCreateTimePayload(value) {
  const text = String(value || '').trim()
  if (!text) {
    return ''
  }
  const normalized = text.replace('T', ' ')
  return normalized.length === 16 ? `${normalized}:00` : normalized.slice(0, 19)
}

function normalizeText(value) {
  return String(value || '').trim()
}

function toDateInput(value) {
  return value ? String(value).slice(0, 10) : ''
}

function toDateTimeLocal(value) {
  if (!value) {
    return ''
  }
  const normalized = String(value).replace(' ', 'T')
  return normalized.length >= 19 ? normalized.slice(0, 19) : normalized.slice(0, 16)
}

function num(value) {
  return value === null || value === undefined || value === '' ? '' : Number(value)
}

function formatAmount(value) {
  return value === null || value === undefined || value === '' ? '0.00' : Number(value).toFixed(2)
}

function formatFileSize(value) {
  const size = Number(value || 0)
  if (!size) {
    return ''
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatNumber(value) {
  return value === null || value === undefined || value === '' ? '' : String(Number(value))
}

function formatDateTime(value) {
  if (!value) return '未记录'
  return String(value).replace('T', ' ').slice(0, 19)
}

function buildOrderFlowCode(order, orderType = 'sales') {
  if (!order?.orderId) {
    return {
      taskNo: '',
      orderId: '',
      barcode: '',
      qrText: '',
      orderTypeLabel: '订单流转',
      currentStatusLabel: '未设置',
      orderCategoryLabel: '大货'
    }
  }
  const flowScanCode = String(order.flowScanCode || order.flowBarcode || order.flowCode || '').trim()
  return {
    taskNo: '',
    orderId: String(order.orderId || '').trim(),
    barcode: flowScanCode,
    qrText: flowScanCode,
    orderTypeLabel: '订单流转',
    currentStatusLabel: orderType === 'production' ? productionStatusLabel(order.status) : salesStatusLabel(order.status),
    orderCategoryLabel: orderCategoryLabel(order.orderCategory)
  }
}

function buildOrderFlowCodeFromTask(task = {}) {
  const payload = task.printPayload || {}
  const orderType = payload.orderType || task.orderType || 'sales'
  return {
    taskNo: task.taskNo || payload.printTaskNo || '',
    orderId: payload.orderId || task.orderId || '',
    barcode: payload.flowBarcode || payload.flowScanCode || payload.flowCode || '',
    qrText: payload.flowQrPayload || payload.flowScanCode || payload.flowBarcode || payload.flowCode || '',
    orderTypeLabel: payload.orderTypeLabel || '订单流转',
    currentStatusLabel: payload.currentStatusText || (orderType === 'production' ? productionStatusLabel(payload.currentStatus) : salesStatusLabel(payload.currentStatus)),
    orderCategoryLabel: payload.orderCategoryLabel || orderCategoryLabel(payload.orderCategory)
  }
}

function salesStatusLabel(status) {
  return salesStatuses.find(item => item.value === status)?.label || status || '未设置'
}

function productionStatusLabel(status) {
  return productionStatuses.find(item => item.value === status)?.label || status || '未设置'
}

function normalizeOrderCategory(value) {
  const normalized = String(value || '').trim()
  return orderCategoryOptions.some(item => item.value === normalized) ? normalized : 'bulk'
}

function orderCategoryLabel(value) {
  return orderCategoryOptions.find(item => item.value === normalizeOrderCategory(value))?.label || '大货'
}

function invoiceLabel(value) {
  return Number(value || 0) === 1 ? '已开票' : '未开票'
}

function invoiceClass(value) {
  return Number(value || 0) === 1 ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
}

function statusToken(status) {
  return String(status || 'unset').replace(/[^a-zA-Z0-9_-]/g, '') || 'unset'
}

function statusChipClass(status) {
  return `status-chip-status-${statusToken(status)}`
}

function summaryCardClass(card = {}) {
  if (card.staleOnly) return 'stat-card-warning'
  return card.status ? `stat-card-status-${statusToken(card.status)}` : 'stat-card-status-all'
}

function salesStatusClass(status) {
  return `order-status-${statusToken(status)}`
}

function productionStatusClass(status) {
  return `order-status-${statusToken(status)}`
}

function orderRowClass(status) {
  return `order-row-status-${statusToken(status)}`
}

function orderProgress(row = {}) {
  const status = row?.status || ''
  const isProduction = currentTab.value === 'production'
  const baseMap = isProduction
    ? {
      pending_confirm: 12,
      pending_material: 28,
      producing: productionProcessProgress(row),
      pending_ship: 78,
      shipped: 90,
      completed: 100,
      cancelled: 0
    }
    : {
      pending_confirm: 12,
      budgeting: 38,
      budget_completed: 100,
      pending_pay: 28,
      pending_material: 36,
      producing: 56,
      pending_ship: 72,
      shipped: 88,
      completed: 100,
      cancelled: 0
    }
  const percent = Math.max(0, Math.min(100, Number(baseMap[status] ?? 8)))
  const label = isProduction && status === 'producing'
    ? (productionProcessText(row) || productionStatusLabel(status))
    : (isProduction ? productionStatusLabel(status) : salesStatusLabel(status))
  return {percent, label}
}

function productionProcessProgress(row = {}) {
  if (Number.isFinite(Number(row.processProgressPercent))) {
    return Number(row.processProgressPercent)
  }
  const processValue = Number(row.process ?? 0)
  const maxIndex = Math.max(processOptions.length - 1, 0)
  const processIndex = Number.isFinite(processValue) ? Math.max(0, Math.min(maxIndex, processValue)) : 0
  return Math.round(40 + ((processIndex + 1) / Math.max(processOptions.length, 1)) * 38)
}

function productionProcessPercent(row = {}) {
  if (Number.isFinite(Number(row.processProgressPercent))) {
    return Math.max(0, Math.min(100, Number(row.processProgressPercent)))
  }
  const status = row?.status || ''
  if (['pending_ship', 'shipped', 'completed'].includes(status)) {
    return 100
  }
  if (status !== 'producing') {
    return 0
  }
  const processValue = Number(row.process)
  const completedIndex = Number.isFinite(processValue) ? Math.max(-1, Math.min(processOptions.length - 1, processValue)) : -1
  return Math.max(0, Math.min(100, Math.round(((completedIndex + 1) * 100) / Math.max(processOptions.length, 1))))
}

function productionProcessSteps(row = {}) {
  if (Array.isArray(row.processSteps) && row.processSteps.length) {
    return row.processSteps.map((step, index) => ({
      code: step.code ?? index,
      name: step.name || processOptions[index]?.label || `工序${index + 1}`,
      done: Boolean(step.done),
      current: Boolean(step.current)
    }))
  }
  const status = row?.status || ''
  const total = processOptions.length
  const processValue = Number(row.process)
  const completedIndex = ['pending_ship', 'shipped', 'completed'].includes(status)
    ? total - 1
    : (status === 'producing' && Number.isFinite(processValue)
      ? Math.max(-1, Math.min(total - 1, processValue))
      : -1)
  const currentIndex = status === 'producing' && completedIndex + 1 < total ? completedIndex + 1 : -1
  return processOptions.map((option, index) => ({
    code: option.value,
    name: option.label,
    done: completedIndex >= index,
    current: currentIndex === index
  }))
}

function productionProcessText(row = {}) {
  return row.currentProcessText || row.processText || row.completedProcessText || ''
}
</script>

<style scoped>
.box-input {
  width: 100%;
  border-radius: .75rem;
  background: rgb(var(--surface-container-low));
  padding: .625rem 1rem;
  font-size: .875rem;
  color: rgb(var(--on-surface));
  outline: none
}

.box-input:focus {
  box-shadow: 0 0 0 2px rgba(0, 82, 204, .2)
}

.combo-arrow {
  pointer-events: none;
  position: absolute;
  right: .75rem;
  top: 2.15rem;
  font-size: 1.1rem;
  color: rgb(var(--on-surface-variant))
}

.combo-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + .35rem);
  z-index: 90;
  max-height: 15rem;
  overflow-y: auto;
  border-radius: .875rem;
  border: 1px solid rgba(31, 111, 255, .14);
  background: rgba(255, 255, 255, .98);
  box-shadow: 0 18px 40px rgba(15, 23, 42, .14);
  padding: .35rem
}

.combo-option {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: .2rem;
  border-radius: .65rem;
  padding: .65rem .75rem;
  text-align: left;
  transition: background .16s ease, transform .16s ease
}

.combo-option:hover {
  background: rgba(31, 111, 255, .08);
  transform: translateY(-1px)
}

.order-status-pill {
  display: inline-flex;
  min-width: 4.75rem;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  border-radius: 999px;
  padding: .5rem .9rem;
  font-size: .75rem;
  font-weight: 800;
  line-height: 1.1;
  text-align: center
}

.detail-status-pill {
  width: fit-content;
  min-width: 5.25rem;
}

.order-status-pending_confirm {
  border: 1px solid rgba(180, 83, 9, .18);
  background: rgba(255, 251, 235, .96);
  color: #b45309;
}

.order-status-pending_pay {
  border: 1px solid rgba(2, 132, 199, .18);
  background: rgba(224, 242, 254, .96);
  color: #0284c7;
}

.order-status-pending_material {
  border: 1px solid rgba(124, 58, 237, .18);
  background: rgba(245, 243, 255, .96);
  color: #7c3aed;
}

.order-status-budgeting {
  border: 1px solid rgba(147, 51, 234, .18);
  background: rgba(250, 245, 255, .96);
  color: #9333ea;
}

.order-status-budget_completed {
  border: 1px solid rgba(22, 163, 74, .18);
  background: rgba(240, 253, 244, .96);
  color: #15803d;
}

.order-status-producing {
  border: 1px solid rgba(37, 99, 235, .18);
  background: rgba(239, 246, 255, .96);
  color: #2563eb;
}

.order-status-pending_ship {
  border: 1px solid rgba(15, 118, 110, .18);
  background: rgba(240, 253, 250, .96);
  color: #0f766e;
}

.order-status-shipped {
  border: 1px solid rgba(22, 163, 74, .18);
  background: rgba(240, 253, 244, .96);
  color: #16a34a;
}

.order-status-completed {
  border: 1px solid rgba(71, 85, 105, .18);
  background: rgba(248, 250, 252, .96);
  color: #475569;
}

.order-status-cancelled {
  border: 1px solid rgba(220, 38, 38, .18);
  background: rgba(254, 242, 242, .96);
  color: #dc2626;
}

.order-category-pill {
  display: inline-flex;
  min-width: 4.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(31, 111, 255, .18);
  background: rgba(31, 111, 255, .08);
  color: #0b2a6f;
  padding: .45rem .8rem;
  font-size: .75rem;
  font-weight: 800;
  white-space: nowrap;
}

.order-progress-cell {
  min-width: 9rem;
}

.order-progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  font-size: .72rem;
  font-weight: 800;
  color: var(--order-row-text, rgb(var(--on-surface-variant)));
}

.order-progress-meta strong {
  color: var(--order-row-text, #16a34a);
}

.order-progress-track {
  margin-top: .45rem;
  height: .48rem;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--order-row-text, #16a34a) 14%, white);
}

.order-progress-bar {
  height: 100%;
  min-width: .5rem;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--order-row-text, #16a34a), color-mix(in srgb, var(--order-row-text, #16a34a) 56%, white));
  box-shadow: 0 4px 12px color-mix(in srgb, var(--order-row-text, #16a34a) 28%, transparent);
  transition: width .25s ease;
}

.production-process-mini {
  margin-top: .55rem;
  max-width: 19rem;
}

.production-process-current {
  margin-bottom: .42rem;
  font-size: .7rem;
  font-weight: 800;
  color: var(--order-row-text, rgb(var(--primary)));
}

.production-process-steps {
  display: flex;
  flex-wrap: wrap;
  gap: .35rem;
}

.production-process-step {
  border-radius: 999px;
  border: 1px solid rgba(100, 116, 139, .18);
  background: rgba(148, 163, 184, .12);
  color: rgb(var(--on-surface-variant));
  padding: .16rem .42rem;
  font-size: .66rem;
  font-weight: 800;
  line-height: 1.25;
  white-space: nowrap;
}

.production-process-step.done {
  border-color: color-mix(in srgb, var(--order-row-text, #16a34a) 42%, white);
  background: color-mix(in srgb, var(--order-row-text, #16a34a) 14%, white);
  color: var(--order-row-text, #16a34a);
}

.production-process-step.current {
  border-color: var(--order-row-text, rgb(var(--primary)));
  background: var(--order-row-text, rgb(var(--primary)));
  color: white;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--order-row-text, rgb(var(--primary))) 22%, transparent);
}

.production-process-detail {
  border-radius: 1rem;
  border: 1px solid rgba(31, 63, 95, .16);
  background: linear-gradient(135deg, rgba(248, 251, 255, .96), rgba(255, 255, 255, .98));
  padding: 1rem;
}

.production-process-detail-head {
  display: flex;
  align-items: center;
  gap: .6rem;
  margin-bottom: .65rem;
  color: rgb(var(--on-surface-variant));
  font-size: .78rem;
}

.production-process-detail-head strong {
  color: rgb(var(--on-surface));
  font-size: .95rem;
}

.production-process-detail-head em {
  margin-left: auto;
  color: rgb(var(--primary));
  font-style: normal;
  font-weight: 900;
}

.production-process-steps-detail {
  margin-top: .7rem;
}

.order-flow-code-card {
  border-radius: 1rem;
  border: 1px solid rgba(31, 63, 95, .18);
  background: linear-gradient(135deg, rgba(238, 244, 251, .92), rgba(255, 255, 255, .98));
  padding: 1rem;
  box-shadow: 0 12px 28px rgba(15, 23, 42, .06);
}

.flow-code-summary {
  display: flex;
  align-items: center;
  gap: .75rem;
}

.flow-code-icon {
  display: grid;
  width: 2.8rem;
  height: 2.8rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: .95rem;
  background: rgb(var(--primary));
  color: rgb(var(--on-primary));
  font-weight: 1000;
  box-shadow: 0 10px 24px rgba(31, 63, 95, .18);
}

.flow-code-title {
  font-size: .95rem;
  font-weight: 1000;
  color: rgb(var(--primary));
}

.flow-code-desc,
.flow-code-hint {
  margin-top: .2rem;
  font-size: .78rem;
  line-height: 1.6;
  color: rgb(var(--on-surface-variant));
}

.flow-code-meta {
  display: flex;
  flex-wrap: wrap;
  gap: .5rem;
  margin: .75rem 0;
}

.flow-code-meta span {
  border-radius: 999px;
  background: rgba(31, 63, 95, .09);
  padding: .28rem .6rem;
  font-size: .72rem;
  font-weight: 900;
  color: rgb(var(--primary));
}

.order-table-row {
  --order-row-bg: rgba(31, 111, 255, .035);
  --order-row-accent: rgba(31, 111, 255, .32);
  --order-row-text: #1f6fff;
  background: linear-gradient(90deg, var(--order-row-bg), rgba(255, 255, 255, .9));
  color: var(--order-row-text);
  transition: background .18s ease, box-shadow .18s ease, transform .18s ease;
}

.order-table-row .text-on-surface-variant,
.order-table-row .text-primary,
.order-table-row .text-secondary,
.order-table-row .text-tertiary,
.order-table-row .text-error,
.order-table-row .text-green-700,
.order-table-row .text-amber-700,
.order-table-row .td-cell,
.order-table-row .font-bold,
.order-table-row .order-category-pill,
.order-table-row .order-status-pill,
.order-table-row .order-stale-tag,
.order-table-row .icon-btn,
.order-table-row .material-symbols-outlined {
  color: var(--order-row-text) !important;
}

.order-table-row .order-category-pill,
.order-table-row .order-status-pill {
  border-color: color-mix(in srgb, var(--order-row-text) 24%, transparent);
  background: color-mix(in srgb, var(--order-row-text) 10%, white);
}

.order-table-row .order-stale-tag,
.order-table-row .order-stale-tag .material-symbols-outlined {
  color: #dc2626 !important;
}

.order-table-row .order-stale-tag {
  border: 1px solid rgba(220, 38, 38, .16);
  background: rgba(254, 226, 226, .92);
}

.order-table-row:hover {
  background: linear-gradient(90deg, var(--order-row-bg), rgba(255, 255, 255, .96));
  box-shadow: inset 0 0 0 9999px rgba(255, 255, 255, .08);
}

.order-table-row > .td-cell:first-child {
  box-shadow: inset 4px 0 0 var(--order-row-accent);
}

.order-row-status-pending_confirm {
  --order-row-bg: rgba(245, 158, 11, .10);
  --order-row-accent: rgba(180, 83, 9, .50);
  --order-row-text: #b45309;
}

.order-row-status-pending_pay {
  --order-row-bg: rgba(14, 165, 233, .10);
  --order-row-accent: rgba(2, 132, 199, .54);
  --order-row-text: #0284c7;
}

.order-row-status-pending_material {
  --order-row-bg: rgba(124, 58, 237, .10);
  --order-row-accent: rgba(124, 58, 237, .58);
  --order-row-text: #7c3aed;
}

.order-row-status-budgeting {
  --order-row-bg: rgba(147, 51, 234, .10);
  --order-row-accent: rgba(147, 51, 234, .56);
  --order-row-text: #7e22ce;
}

.order-row-status-budget_completed {
  --order-row-bg: rgba(34, 197, 94, .10);
  --order-row-accent: rgba(22, 163, 74, .56);
  --order-row-text: #15803d;
}

.order-row-status-producing {
  --order-row-bg: rgba(37, 99, 235, .10);
  --order-row-accent: rgba(37, 99, 235, .58);
  --order-row-text: #2563eb;
}

.order-row-status-pending_ship {
  --order-row-bg: rgba(20, 184, 166, .10);
  --order-row-accent: rgba(15, 118, 110, .58);
  --order-row-text: #0f766e;
}

.order-row-status-shipped {
  --order-row-bg: rgba(34, 197, 94, .10);
  --order-row-accent: rgba(22, 163, 74, .58);
  --order-row-text: #16a34a;
}

.order-row-status-completed {
  --order-row-bg: rgba(71, 85, 105, .08);
  --order-row-accent: rgba(71, 85, 105, .48);
  --order-row-text: #475569;
}

.order-row-status-cancelled {
  --order-row-bg: rgba(239, 68, 68, .10);
  --order-row-accent: rgba(220, 38, 38, .58);
  --order-row-text: #b91c1c;
}

.attachment-uploader {
  margin-top: .5rem;
  cursor: pointer;
  border-radius: 1rem;
  border: 1px dashed rgba(31, 111, 255, .35);
  background: linear-gradient(135deg, rgba(31, 63, 95, .07), rgba(143, 110, 61, .06));
  padding: 1rem;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease
}

.attachment-uploader:hover {
  border-color: rgb(var(--primary));
  box-shadow: 0 12px 28px rgba(31, 111, 255, .12);
  transform: translateY(-1px)
}

.attachment-action {
  border-radius: .625rem;
  background: rgba(255, 255, 255, .8);
  padding: .375rem .75rem;
  font-size: .75rem;
  font-weight: 800
}

.order-header-card-stack {
  display: flex;
  min-width: min(100%, 720px);
  flex-direction: column;
  gap: .75rem;
}

.order-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(132px, 1fr));
  gap: .75rem;
}

.order-category-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(118px, 1fr));
  gap: .65rem;
}

.category-stat-card {
  border-radius: 1rem;
  border: 1px solid rgba(31, 63, 95, .14);
  background: rgba(255, 255, 255, .9);
  padding: .78rem .95rem;
  text-align: left;
  box-shadow: 0 10px 24px rgba(15, 23, 42, .06);
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease, background .18s ease;
}

.category-stat-card span {
  display: block;
  font-size: .78rem;
  font-weight: 900;
  color: rgb(var(--on-surface));
}

.category-stat-card strong {
  display: block;
  margin-top: .22rem;
  font-size: 1.35rem;
  line-height: 1.55rem;
  font-weight: 950;
  color: rgb(var(--primary));
}

.category-stat-card small {
  display: block;
  margin-top: .1rem;
  font-size: .72rem;
  color: rgb(var(--on-surface-variant));
}

.category-stat-card:hover,
.category-stat-card-active {
  transform: translateY(-1px);
  border-color: rgba(31, 63, 95, .42);
  background: linear-gradient(135deg, rgba(31, 63, 95, .10), rgba(255, 255, 255, .96));
  box-shadow: 0 16px 34px rgba(31, 63, 95, .12);
}

@media (min-width: 1280px) {
  .order-summary-grid {
    grid-template-columns: repeat(5, minmax(132px, 1fr));
  }
}

@media (max-width: 720px) {
  .order-category-summary-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .drawer-head {
    align-items: flex-start;
    flex-direction: column
  }

  .drawer-head-actions {
    width: 100%;
    justify-content: space-between
  }

  .time-correction-toggle {
    max-width: calc(100% - 3rem)
  }
}

.order-warning-setting-btn,
.order-warning-refresh-btn {
  display: inline-flex;
  min-height: 4.5rem;
  align-items: center;
  justify-content: center;
  gap: .5rem;
  border-radius: 1rem;
  border: 1px solid rgba(31, 63, 95, .24);
  background: rgba(255, 255, 255, .88);
  padding: 1rem 1.25rem;
  font-size: .875rem;
  font-weight: 900;
  color: rgb(var(--primary));
  box-shadow: 0 10px 26px rgba(15, 23, 42, .08);
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}

.order-warning-refresh-btn {
  color: #047857;
  border-color: rgba(4, 120, 87, .24);
}

.order-warning-setting-btn:hover,
.order-warning-refresh-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(31, 63, 95, .58);
  box-shadow: 0 16px 36px rgba(15, 23, 42, .12);
}

.order-warning-refresh-btn:disabled {
  cursor: wait;
  opacity: .72;
}

:global(.order-warning-dialog) {
  display: grid;
  gap: .85rem;
  color: rgb(var(--on-surface));
}

:global(.order-warning-dialog__desc) {
  margin: 0 0 .25rem;
  color: rgb(var(--on-surface-variant));
  font-size: .86rem;
  line-height: 1.55;
}

:global(.order-warning-dialog__row) {
  display: grid;
  grid-template-columns: 5rem minmax(0, 1fr) 7rem;
  align-items: center;
  gap: .75rem;
  border: 1px solid rgba(31, 63, 95, .12);
  border-radius: .9rem;
  background: rgba(248, 250, 252, .92);
  padding: .75rem .85rem;
}

:global(.order-warning-dialog__row span) {
  font-weight: 900;
}

:global(.order-warning-dialog__row input) {
  width: 100%;
  border: 1px solid rgba(31, 63, 95, .2);
  border-radius: .7rem;
  background: #fff;
  padding: .6rem .75rem;
  font-weight: 900;
  outline: none;
}

:global(.order-warning-dialog__row em) {
  color: rgb(var(--on-surface-variant));
  font-size: .8rem;
  font-style: normal;
}

.stat-card {
  --order-card-color: rgb(var(--primary));
  --order-card-soft: rgba(31, 63, 95, .10);
  min-width: 132px;
  border-radius: 1rem;
  border: 1px solid rgba(0, 82, 204, .1);
  background: rgb(var(--surface-container-high));
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .05);
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease
}

.stat-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--order-card-color) 42%, transparent);
  box-shadow: 0 14px 34px color-mix(in srgb, var(--order-card-color) 12%, transparent)
}

.stat-card-active {
  border-color: color-mix(in srgb, var(--order-card-color) 72%, transparent);
  background: linear-gradient(135deg, var(--order-card-soft), rgba(255, 255, 255, .98))
}

.order-row-stale-warning {
  background: linear-gradient(90deg, rgba(238, 244, 251, .88), rgba(255, 255, 255, .96));
}

.order-stale-tag {
  margin-top: .5rem;
  display: inline-flex;
  align-items: center;
  gap: .25rem;
  border-radius: 999px;
  background: rgba(232, 238, 246, .95);
  padding: .25rem .55rem;
  font-size: .72rem;
  font-weight: 900;
  color: rgb(180, 83, 9);
}

.order-stale-actions {
  margin-top: .55rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
}

.order-stale-actions .order-stale-tag {
  margin-top: 0;
}

.order-stale-refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: .25rem;
  min-height: 1.85rem;
  border-radius: 999px;
  border: 1px solid rgba(220, 38, 38, .22);
  background: rgba(255, 255, 255, .92);
  padding: .25rem .58rem;
  color: #dc2626;
  font-size: .72rem;
  font-weight: 900;
  line-height: 1;
  white-space: nowrap;
  transition: transform .18s ease, border-color .18s ease, background .18s ease;
}

.order-table-row .order-stale-refresh-btn,
.order-table-row .order-stale-refresh-btn .material-symbols-outlined {
  color: #dc2626 !important;
}

.order-stale-refresh-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(220, 38, 38, .42);
  background: rgba(254, 242, 242, .98);
}

.order-stale-refresh-btn:disabled {
  cursor: wait;
  opacity: .62;
}

.order-stale-refresh-btn.is-refreshing .material-symbols-outlined {
  animation: order-row-warning-spin .85s linear infinite;
}

@keyframes order-row-warning-spin {
  to {
    transform: rotate(360deg);
  }
}

.stat-label {
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: .18em;
  color: rgb(var(--on-surface-variant))
}

.stat-value {
  margin-top: .5rem;
  font-size: 1.875rem;
  line-height: 2.25rem;
  font-weight: 900;
  color: var(--order-card-color)
}

.stat-card-status-pending_confirm {
  --order-card-color: #b45309;
  --order-card-soft: rgba(245, 158, 11, .13);
}

.stat-card-status-pending_pay {
  --order-card-color: #0284c7;
  --order-card-soft: rgba(14, 165, 233, .13);
}

.stat-card-status-pending_material {
  --order-card-color: #7c3aed;
  --order-card-soft: rgba(124, 58, 237, .13);
}

.stat-card-status-budgeting {
  --order-card-color: #9333ea;
  --order-card-soft: rgba(147, 51, 234, .13);
}

.stat-card-status-budget_completed {
  --order-card-color: #15803d;
  --order-card-soft: rgba(34, 197, 94, .13);
}

.stat-card-status-producing {
  --order-card-color: #2563eb;
  --order-card-soft: rgba(37, 99, 235, .13);
}

.stat-card-status-pending_ship {
  --order-card-color: #0f766e;
  --order-card-soft: rgba(20, 184, 166, .13);
}

.stat-card-status-shipped {
  --order-card-color: #16a34a;
  --order-card-soft: rgba(34, 197, 94, .13);
}

.stat-card-status-completed {
  --order-card-color: #475569;
  --order-card-soft: rgba(71, 85, 105, .10);
}

.stat-card-status-cancelled,
.stat-card-warning {
  --order-card-color: #dc2626;
  --order-card-soft: rgba(220, 38, 38, .11);
}

.stat-hint {
  margin-top: .25rem;
  font-size: .75rem;
  color: rgb(var(--on-surface-variant))
}

.status-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: .65rem
}

.order-primary-status-tabs {
  padding: 1rem 1.5rem;
  background: rgba(255, 255, 255, .84);
}

.status-chip {
  --status-chip-color: rgb(var(--primary));
  --status-chip-soft: rgba(31, 63, 95, .10);
  display: inline-flex;
  align-items: center;
  gap: .45rem;
  border-radius: 999px;
  border: 1px solid rgba(0, 82, 204, .1);
  background: rgba(255, 255, 255, .82);
  padding: .55rem .85rem;
  font-size: .84rem;
  font-weight: 800;
  color: var(--status-chip-color);
  transition: border-color .18s ease, background .18s ease, color .18s ease
}

.status-chip strong {
  min-width: 1.65rem;
  border-radius: 999px;
  background: var(--status-chip-soft);
  padding: .1rem .45rem;
  color: var(--status-chip-color);
  text-align: center
}

.status-chip-active {
  border-color: color-mix(in srgb, var(--status-chip-color) 74%, transparent);
  background: var(--status-chip-color);
  color: rgb(var(--on-primary))
}

.status-chip-active strong {
  background: rgba(255, 255, 255, .24);
  color: rgb(var(--on-primary))
}

.status-chip-status-pending_confirm {
  --status-chip-color: #b45309;
  --status-chip-soft: rgba(245, 158, 11, .13);
}

.status-chip-status-pending_pay {
  --status-chip-color: #0284c7;
  --status-chip-soft: rgba(14, 165, 233, .13);
}

.status-chip-status-pending_material {
  --status-chip-color: #7c3aed;
  --status-chip-soft: rgba(124, 58, 237, .13);
}

.status-chip-status-budgeting {
  --status-chip-color: #9333ea;
  --status-chip-soft: rgba(147, 51, 234, .13);
}

.status-chip-status-budget_completed {
  --status-chip-color: #15803d;
  --status-chip-soft: rgba(34, 197, 94, .13);
}

.status-chip-status-producing {
  --status-chip-color: #2563eb;
  --status-chip-soft: rgba(37, 99, 235, .13);
}

.status-chip-status-pending_ship {
  --status-chip-color: #0f766e;
  --status-chip-soft: rgba(20, 184, 166, .13);
}

.status-chip-status-shipped {
  --status-chip-color: #16a34a;
  --status-chip-soft: rgba(34, 197, 94, .13);
}

.status-chip-status-completed {
  --status-chip-color: #475569;
  --status-chip-soft: rgba(71, 85, 105, .10);
}

.status-chip-status-cancelled {
  --status-chip-color: #dc2626;
  --status-chip-soft: rgba(220, 38, 38, .11);
}

.order-filter-grid {
  align-items: stretch;
}

.order-filter-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: .9rem;
  row-gap: .9rem;
}

.order-query-actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: .65rem;
}

.order-filter-action-btn {
  display: inline-flex;
  min-width: 6.75rem;
  min-height: 3rem;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  line-height: 1.1;
  flex: 0 0 auto;
}

.order-filter-actions :deep(.table-column-settings) {
  display: inline-flex;
  flex: 0 1 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: .65rem;
  min-width: 0;
}

.order-filter-actions :deep(.column-settings-trigger) {
  min-width: 7.8rem;
  height: 3rem;
  padding-inline: .9rem;
  white-space: nowrap;
}

.function-page-shell .order-list-table.responsive-data-table {
  table-layout: fixed;
}

.function-page-shell .order-list-table.responsive-data-table th:last-child,
.function-page-shell .order-list-table.responsive-data-table td:last-child {
  width: clamp(7.5rem, 8vw, 9.5rem);
}

.th-cell {
  padding: 1rem 1.5rem;
  font-size: .75rem;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: .08em;
  color: rgb(var(--on-surface-variant))
}

.td-cell {
  padding: 1rem 1.5rem
}

.order-column-orderNo {
  width: 16%;
  min-width: 11rem;
}

.order-column-customer {
  width: 22%;
  min-width: 13rem;
}

.order-column-core {
  width: 24%;
  min-width: 14rem;
}

.order-column-status {
  width: 11%;
  min-width: 8rem;
}

.order-column-progress {
  width: 15%;
  min-width: 10rem;
}

.order-column-time {
  width: 10%;
  min-width: 8rem;
}

.order-column-orderNo,
.order-column-customer,
.order-column-core {
  word-break: keep-all;
}

.order-column-orderNo > *,
.order-column-customer > *,
.order-column-core > * {
  min-width: 0;
}

.order-column-orderNo .font-bold,
.order-column-customer .font-bold,
.order-column-core .font-bold {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-column-time {
  white-space: nowrap;
}

.order-status-pill-sm {
  min-width: 0;
  padding: .3rem .7rem;
  font-size: .7rem;
}

.icon-btn {
  border-radius: .375rem;
  padding: .375rem
}

.order-row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: .4rem;
}

.order-row-actions .icon-btn {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: .75rem;
  background: rgba(255, 255, 255, .58);
  transition: transform .18s ease, background .18s ease;
}

.order-row-actions .icon-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, .9);
}

.page-btn {
  border-radius: .375rem;
  border: 1px solid rgba(148, 163, 184, .35);
  padding: .375rem .75rem
}

.page-btn:disabled {
  opacity: .5
}

.drawer-large, .drawer-small {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 70;
  display: flex;
  height: 100%;
  width: 100%;
  flex-direction: column;
  border-left: 4px solid rgb(var(--primary));
  background: rgba(255, 255, 255, .95);
  box-shadow: -20px 0 40px rgba(31, 111, 255, .1);
  backdrop-filter: blur(24px)
}

.drawer-large {
  max-width: 780px
}

.drawer-small {
  max-width: 620px
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(148, 163, 184, .2);
  background: #fff;
  padding: 1.5rem
}

.drawer-head-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: .75rem
}

.time-correction-toggle {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  border-radius: 999px;
  border: 1px solid rgba(31, 63, 95, .24);
  background: rgba(238, 244, 251, .86);
  padding: .5rem .75rem;
  color: rgb(var(--primary));
  font-size: .75rem;
  font-weight: 900;
  white-space: nowrap;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease
}

.time-correction-toggle small {
  color: rgb(var(--on-surface-variant));
  font-size: .68rem;
  font-weight: 800
}

.time-correction-toggle:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 63, 95, .58);
  box-shadow: 0 10px 24px rgba(15, 23, 42, .10)
}

.time-correction-toggle.active {
  background: linear-gradient(135deg, rgb(var(--primary)), #4b7395);
  color: rgb(var(--on-primary));
  box-shadow: 0 14px 30px rgba(15, 23, 42, .16)
}

.time-correction-toggle.active small {
  color: rgba(255, 255, 255, .82)
}

.close-btn {
  border-radius: 999px;
  padding: .5rem;
  color: rgb(var(--on-surface-variant))
}

.info-card, .detail-item {
  margin-top: .75rem;
  border-radius: .75rem;
  border: 1px solid rgba(148, 163, 184, .18);
  background: rgb(var(--surface-container-low));
  padding: 1rem
}

.info-card {
  margin-top: 0
}

.info-label {
  font-size: .75rem;
  color: rgb(var(--on-surface-variant))
}

.info-value {
  margin-top: .5rem;
  font-weight: 700;
  color: rgb(var(--primary))
}

.section-title, .field-label {
  margin-bottom: .5rem;
  font-size: .75rem;
  font-weight: 700;
  color: rgb(var(--primary))
}

.status-timeline {
  overflow: hidden;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, .18);
  background: rgb(var(--surface-container-low));
  padding: 1rem
}

.status-log-item {
  position: relative;
  display: grid;
  grid-template-columns:28px minmax(0, 1fr);
  gap: .75rem;
  padding-bottom: 1rem
}

.status-log-item:last-child {
  padding-bottom: 0
}

.status-log-rail {
  position: relative;
  display: flex;
  justify-content: center
}

.status-log-rail::after {
  position: absolute;
  top: 20px;
  bottom: -1rem;
  width: 1px;
  background: rgba(0, 82, 204, .18);
  content: ""
}

.status-log-item:last-child .status-log-rail::after {
  display: none
}

.status-log-dot {
  position: relative;
  z-index: 1;
  margin-top: .25rem;
  height: 12px;
  width: 12px;
  border-radius: 999px;
  background: rgb(var(--surface-container-high));
  box-shadow: 0 0 0 3px rgba(0, 82, 204, .12)
}

.status-log-dot.current {
  background: rgb(var(--primary));
  box-shadow: 0 0 0 5px rgba(0, 82, 204, .16)
}

.status-log-content {
  border-radius: .875rem;
  background: #fff;
  padding: .875rem 1rem;
  box-shadow: 0 8px 24px rgba(15, 23, 42, .05)
}

.status-log-title {
  font-size: .9rem;
  font-weight: 800;
  color: rgb(var(--primary))
}

.status-log-current {
  border-radius: 999px;
  background: rgba(0, 82, 204, .1);
  padding: .125rem .5rem;
  font-size: .7rem;
  font-weight: 800;
  color: rgb(var(--primary))
}

.status-log-time, .status-log-meta {
  margin-top: .35rem;
  font-size: .78rem;
  color: rgb(var(--on-surface-variant))
}

.status-log-remark {
  margin-top: .45rem;
  border-radius: .625rem;
  background: rgba(0, 82, 204, .06);
  padding: .5rem .625rem;
  font-size: .8rem;
  color: rgb(var(--on-surface-variant))
}

.status-log-empty {
  border-radius: 1rem;
  border: 1px dashed rgba(148, 163, 184, .35);
  background: rgb(var(--surface-container-low));
  padding: 1.5rem;
  text-align: center;
  font-size: .875rem;
  color: rgb(var(--on-surface-variant))
}

.fade-enter-active, .fade-leave-active {
  transition: opacity .24s ease
}

.fade-enter-from, .fade-leave-to {
  opacity: 0
}

.slide-enter-active, .slide-leave-active {
  transition: transform .28s ease, opacity .28s ease
}

.slide-enter-from, .slide-leave-to {
  opacity: 0;
  transform: translateX(100%)
}
</style>
