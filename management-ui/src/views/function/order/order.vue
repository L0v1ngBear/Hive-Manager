<template>
  <div class="space-y-6">
    <header class="function-page-header">
      <div>
        <div class="function-page-eyebrow">
          <span class="material-symbols-outlined">list_alt</span>
          订单协同中心
        </div>
        <h1 class="function-page-title">订单全流程管理</h1>
        <p class="function-page-desc">
          统一管理销售订单和生产订单，支持抽屉式新建、编辑、详情查看和状态流转追踪。</p>
      </div>
      <div class="flex flex-col gap-3 sm:flex-row sm:items-stretch">
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div class="stat-card">
            <div class="stat-label">销售订单数量</div>
            <div class="stat-value">{{ salesState.total }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">生产订单数量</div>
            <div class="stat-value">{{ productionState.total }}</div>
          </div>
        </div>
        <button class="function-action-primary px-6 py-4"
                @click="openCreate">
          {{ currentTab === 'sales' ? '新建销售订单' : '新建生产订单' }}
        </button>
      </div>
    </header>

    <section class="overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
      <div class="flex border-b border-outline-variant/10">
        <button
            v-for="tab in tabs"
            :key="tab.id"
            class="relative px-6 py-4 text-sm font-bold"
            :class="currentTab === tab.id ? 'text-primary' : 'text-on-surface-variant'"
            @click="switchTab(tab.id)"
        >
          {{ tab.label }}
          <span v-if="currentTab === tab.id" class="absolute inset-x-0 bottom-0 h-[3px] bg-primary"></span>
        </button>
      </div>
      <div
          class="grid grid-cols-1 gap-4 border-b border-outline-variant/10 bg-surface-container-low/30 px-6 py-5 md:grid-cols-[minmax(0,1fr)_220px_auto]">
        <input
            v-model.trim="filters.keyword"
            class="box-input"
            :placeholder="currentTab === 'sales' ? '搜索订单号、客户、项目或商品描述' : '搜索生产单号、销售单号、客户、项目或型号'"
            @keyup.enter="refreshCurrentTab"
        />
        <select v-model="filters.status" class="box-input" @change="refreshCurrentTab">
          <option value="">全部状态</option>
          <option v-for="status in currentStatuses" :key="status.value" :value="status.value">{{
              status.label
            }}
          </option>
        </select>
        <button class="rounded-lg bg-primary px-5 py-2.5 text-sm font-bold text-on-primary" @click="refreshCurrentTab">
          查询
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1220px] w-full text-left">
          <thead class="bg-surface-container-low/50">
          <tr>
            <th class="th-cell">编号</th>
            <th class="th-cell">客户 / 项目</th>
            <th class="th-cell">核心信息</th>
            <th class="th-cell">交付信息</th>
            <th class="th-cell">开票</th>
            <th class="th-cell">状态</th>
            <th class="th-cell">时间</th>
            <th class="th-cell text-right">操作</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
          <tr
              v-for="row in currentState.rows"
              :key="row.orderId"
              class="group cursor-pointer hover:bg-surface-container-high/30"
              @click="openDetail(row.orderId)"
          >
            <td class="td-cell">
              <div class="font-bold text-primary">{{ row.orderId }}</div>
              <div class="mt-1 text-xs text-on-surface-variant">
                {{ currentTab === 'sales' ? `明细 ${row.detailCount || 0} 项` : `数量 ${row.quantity || 0}` }}
              </div>
            </td>
            <td class="td-cell">
              <div class="font-bold text-primary">{{ row.customerName || '未填写客户' }}</div>
              <div class="mt-1 text-xs text-on-surface-variant">{{ row.projectName || '未填写项目' }}</div>
            </td>
            <td class="td-cell">
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
            </td>
            <td class="td-cell">
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
            </td>
            <td class="td-cell">
              <template v-if="currentTab === 'sales'">
                  <span :class="invoiceClass(row.isInvoice)"
                        class="inline-flex rounded-full px-3 py-1 text-xs font-bold">
                    {{ invoiceLabel(row.isInvoice) }}
                  </span>
              </template>
              <template v-else>
                <span class="text-xs text-on-surface-variant">--</span>
              </template>
            </td>
            <td class="td-cell">
                <span
                    class="inline-flex rounded-full px-3 py-1 text-xs font-bold"
                    :class="currentTab === 'sales' ? salesStatusClass(row.status) : productionStatusClass(row.status)"
                >
                  {{ currentTab === 'sales' ? salesStatusLabel(row.status) : productionStatusLabel(row.status) }}
                </span>
            </td>
            <td class="td-cell text-sm text-on-surface-variant">{{ formatDateTime(row.createTime) }}</td>
            <td class="td-cell">
              <div class="flex justify-end gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                <button class="icon-btn text-secondary" @click.stop="openDetail(row.orderId)">
                  <span class="material-symbols-outlined text-[18px]">visibility</span>
                </button>
                <button class="icon-btn text-primary" @click.stop="openEdit(row.orderId)">
                  <span class="material-symbols-outlined text-[18px]">edit</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!currentState.loading && !currentState.rows.length">
            <td colspan="8" class="px-6 py-14 text-center text-sm text-on-surface-variant">暂无订单数据</td>
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
              <h2 class="text-xl font-bold tracking-tight text-primary">
                {{ currentTab === 'sales' ? '销售订单详情' : '生产订单详情' }}</h2>
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
                  <div class="info-value">{{ salesStatusLabel(salesDetail.status) }}</div>
                </div>
                <div class="info-card">
                  <div class="info-label">客户 / 项目</div>
                  <div class="info-value">{{ salesDetail.customerName || '未填写客户' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ salesDetail.projectName || '未填写项目' }}</div>
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
                  <div class="mt-1 text-xs text-on-surface-variant">用于财务开票跟进和后续统计。</div>
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
                  <div class="info-value">{{ productionStatusLabel(productionDetail.status) }}</div>
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
                  <div class="info-label">生产信息</div>
                  <div class="info-value">{{ productionDetail.modelCode || '未填写型号' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">
                    {{ formatNumber(productionDetail.weight) }} 克 / {{ formatNumber(productionDetail.width) }} 规格 /
                    数量 {{ productionDetail.quantity || 0 }}
                  </div>
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
                {{
                  currentTab === 'sales' ? (formMode === 'create' ? '新建销售订单' : '编辑销售订单') : (formMode === 'create' ? '新建生产订单' : '编辑生产订单')
                }}
              </h2>
              <p class="mt-1 text-xs text-on-surface-variant">
                {{ currentTab === 'sales' ? '参照小程序下单页录入销售订单信息。' : '参照小程序下单页录入生产订单信息。' }}
              </p>
            </div>
            <button class="close-btn" @click="closeForm">
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div class="flex-1 space-y-6 overflow-y-auto p-6">
            <template v-if="currentTab === 'sales'">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="relative">
                  <label class="field-label">客户名称 *</label>
                  <input v-model.trim="salesForm.customerName" class="box-input pr-10" type="text"
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
                  <input v-model.trim="salesForm.projectName" class="box-input pr-10" type="text"
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
                  <input v-model="salesForm.deliveryDate" class="box-input" type="date"/>
                </div>
              </div>

              <div>
                <div class="flex items-end justify-between">
                  <div class="section-title">订单明细</div>
                  <button class="text-xs font-bold text-primary" @click="addSalesItem">添加商品</button>
                </div>
                <div v-for="(item, index) in salesForm.items" :key="index" class="detail-item">
                  <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <input v-model.trim="item.modelCode" class="box-input" placeholder="型号" type="text"/>
                    <input v-model.number="item.quantity" class="box-input" placeholder="数量" type="number" min="0.01"
                           step="0.01"/>
                    <input v-model.number="item.weight" class="box-input" placeholder="克重" type="number" min="0.01"
                           step="0.01"/>
                    <input v-model.number="item.spec" class="box-input" placeholder="规格" type="number" min="0.01"
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
                <div class="flex items-end">
                  <label
                      class="flex items-center gap-2 rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 text-sm">
                    <input v-model="salesForm.createProductionOrder" :true-value="1" :false-value="0" type="checkbox"/>
                    同步创建生产单
                  </label>
                </div>
                <div v-if="salesForm.status === 'shipped'">
                  <label class="field-label">物流公司</label>
                  <input v-model.trim="salesForm.expressCompany" class="box-input" type="text"/>
                </div>
                <div v-if="salesForm.status === 'shipped'">
                  <label class="field-label">物流单号</label>
                  <input v-model.trim="salesForm.expressNo" class="box-input" type="text"/>
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
                  <input v-model="productionForm.deliveryDate" class="box-input" type="date"/>
                </div>
                <div>
                  <label class="field-label">客户名称</label>
                  <input v-model.trim="productionForm.customerName" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">项目名称</label>
                  <input v-model.trim="productionForm.projectName" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">联系电话</label>
                  <input v-model.trim="productionForm.contactPhone" class="box-input" type="text"/>
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
                  <input v-model.trim="productionForm.modelCode" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">数量 *</label>
                  <input v-model.number="productionForm.quantity" class="box-input" type="number" min="1" step="1"/>
                </div>
                <div>
                  <label class="field-label">克重 *</label>
                  <input v-model.number="productionForm.weight" class="box-input" type="number" min="0.01" step="0.01"/>
                </div>
                <div>
                  <label class="field-label">规格 *</label>
                  <input v-model.number="productionForm.spec" class="box-input" type="number" min="0.01" step="0.01"/>
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
                  <label class="field-label">生产工序</label>
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
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {useRoute} from 'vue-router'
import {getCustomerOptions} from '../customer/api/customer'
import {
  createProductionOrder,
  createSalesOrder,
  getProductionOrderDetail,
  getProductionOrderPage,
  getSalesOrderDetail,
  getSalesOrderPage,
  downloadSalesOrderAttachment,
  saveProductionOrder,
  saveSalesOrder,
  uploadSalesOrderAttachment
} from './api/order'

const route = useRoute()
const tabs = [{id: 'sales', label: '销售订单'}, {id: 'production', label: '生产订单'}]
const salesStatuses = [
  {value: 'pending_confirm', label: '待确认'},
  {value: 'pending_pay', label: '待收款'},
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
  {value: 0, label: '整经'},
  {value: 1, label: '浆纱'},
  {value: 2, label: '织造'},
  {value: 3, label: '验布'},
  {value: 4, label: '卷布'}
]

const filters = reactive({keyword: '', status: ''})
const currentTab = ref('sales')
const salesState = reactive({rows: [], page: 1, size: 10, total: 0, pages: 1, loading: false})
const productionState = reactive({rows: [], page: 1, size: 10, total: 0, pages: 1, loading: false})
const detailVisible = ref(false)
const detailLoading = ref(false)
const salesDetail = ref(null)
const productionDetail = ref(null)
const customerOptions = ref([])
const customerDropdownVisible = ref(false)
const projectDropdownVisible = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const editingOrderId = ref('')
const submitting = ref(false)
const salesForm = reactive(defaultSalesForm())
const productionForm = reactive(defaultProductionForm())
const salesAttachmentInputRef = ref(null)
const salesAttachmentUploading = ref(false)

const currentStatuses = computed(() => currentTab.value === 'sales' ? salesStatuses : productionStatuses)
const currentState = computed(() => currentTab.value === 'sales' ? salesState : productionState)
const selectedCustomerOption = computed(() => {
  const customerName = normalizeText(salesForm.customerName)
  if (!customerName) return null
  return customerOptions.value.find(item => normalizeText(item.customerName) === customerName) || null
})
const selectedCustomerProjects = computed(() => selectedCustomerOption.value?.projectNames || [])
const showCustomerOptions = computed(() => customerDropdownVisible.value && customerOptions.value.length > 0)
const showProjectOptions = computed(() => projectDropdownVisible.value && selectedCustomerProjects.value.length > 0)
const customerCreateHintVisible = computed(() => Boolean(normalizeText(salesForm.customerName)) && !selectedCustomerOption.value)
const projectCreateHintVisible = computed(() => {
  const projectName = normalizeText(salesForm.projectName)
  return Boolean(selectedCustomerOption.value && projectName && !selectedCustomerProjects.value.includes(projectName))
})

onMounted(async () => {
  applyRouteSearch()
  await Promise.all([loadSalesOrders(), loadProductionOrders(), loadCustomerOptions()])
})

watch(
  () => [route.query.keyword, route.query.q, route.query.tab],
  async () => {
    applyRouteSearch()
    await refreshCurrentTab()
  }
)

function applyRouteSearch() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  const routeTab = String(route.query.tab || '').trim()
  if (routeTab === 'sales' || routeTab === 'production') {
    currentTab.value = routeTab
  }
  if (routeKeyword !== filters.keyword) {
    filters.keyword = routeKeyword
    salesState.page = 1
    productionState.page = 1
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
    deliveryDate: '',
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
    contactPhone: '',
    modelCode: '',
    fabric: '',
    weight: '',
    spec: '',
    color: '',
    quantity: 1,
    price: '',
    deliveryDate: '',
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
  if (!customerOptions.value.length) {
    await loadCustomerOptions()
  }
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

function switchTab(tabId) {
  if (currentTab.value !== tabId) {
    currentTab.value = tabId
    filters.keyword = ''
    filters.status = ''
  }
}

async function refreshCurrentTab() {
  currentState.value.page = 1
  if (currentTab.value === 'sales') {
    await loadSalesOrders()
  } else {
    await loadProductionOrders()
  }
}

async function changePage(page) {
  currentState.value.page = page
  if (currentTab.value === 'sales') {
    await loadSalesOrders()
  } else {
    await loadProductionOrders()
  }
}

async function loadSalesOrders() {
  salesState.loading = true
  try {
    const res = await getSalesOrderPage({
      pageNum: salesState.page,
      pageSize: salesState.size,
      keyword: filters.keyword || undefined,
      status: currentTab.value === 'sales' ? filters.status || undefined : undefined
    })
    salesState.rows = res.data || []
    salesState.total = res.total || 0
    salesState.pages = res.pages || 1
  } finally {
    salesState.loading = false
  }
}

async function loadProductionOrders() {
  productionState.loading = true
  try {
    const res = await getProductionOrderPage({
      pageNum: productionState.page,
      pageSize: productionState.size,
      keyword: filters.keyword || undefined,
      status: currentTab.value === 'production' ? filters.status || undefined : undefined
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
    if (currentTab.value === 'sales') {
      salesDetail.value = await getSalesOrderDetail(orderId)
    } else {
      productionDetail.value = await getProductionOrderDetail(orderId)
    }
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailVisible.value = false
}

function openCreate() {
  formMode.value = 'create'
  editingOrderId.value = ''
  if (currentTab.value === 'sales') {
    resetSalesForm()
  } else {
    resetProductionForm()
  }
  formVisible.value = true
}

async function openEdit(orderId) {
  formMode.value = 'edit'
  editingOrderId.value = orderId
  formVisible.value = true
  if (currentTab.value === 'sales') {
    resetSalesForm()
    const detail = await getSalesOrderDetail(orderId)
    salesForm.customerName = detail.customerName || ''
    salesForm.customerPhone = detail.customerPhone || ''
    salesForm.projectName = detail.projectName || ''
    salesForm.deliveryDate = toDateInput(detail.deliveryDate)
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
    return
  }

  resetProductionForm()
  const detail = await getProductionOrderDetail(orderId)
  productionForm.salesOrderId = detail.salesOrderId || ''
  productionForm.customerName = detail.customerName || ''
  productionForm.projectName = detail.projectName || ''
  productionForm.contactPhone = detail.contactPhone || ''
  productionForm.modelCode = detail.modelCode || ''
  productionForm.fabric = detail.fabric || ''
  productionForm.weight = num(detail.weight)
  productionForm.spec = num(detail.width)
  productionForm.color = detail.color || ''
  productionForm.quantity = num(detail.quantity) || 1
  productionForm.price = num(detail.price)
  productionForm.deliveryDate = toDateInput(detail.deliveryDate)
  productionForm.status = detail.status || 'pending_confirm'
  productionForm.process = detail.process ?? null
  productionForm.remark = ''
}

function closeForm() {
  formVisible.value = false
  customerDropdownVisible.value = false
  projectDropdownVisible.value = false
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
    if (currentTab.value === 'sales') {
      validateSalesForm()
      const payload = buildSalesPayload()
      if (formMode.value === 'create') {
        await createSalesOrder(payload)
      } else {
        await saveSalesOrder(editingOrderId.value, payload)
      }
      ElMessage.success(formMode.value === 'create' ? '销售订单创建成功' : '销售订单保存成功')
      closeForm()
      await loadSalesOrders()
      if (payload.createProductionOrder === 1) await loadProductionOrders()
      return
    }

    validateProductionForm()
    const payload = buildProductionPayload()
    if (formMode.value === 'create') {
      await createProductionOrder(payload)
    } else {
      await saveProductionOrder(editingOrderId.value, payload)
    }
    ElMessage.success(formMode.value === 'create' ? '生产订单创建成功' : '生产订单保存成功')
    closeForm()
    await loadProductionOrders()
  } finally {
    submitting.value = false
  }
}

function validateSalesForm() {
  if (!salesForm.customerName.trim()) fail('请输入客户名称')
  if (!salesForm.projectName.trim()) fail('请输入项目名称')
  if (!salesForm.deliveryDate) fail('请选择交付日期')
  if (!salesForm.items.length) fail('请至少添加一个商品明细')
  if (salesForm.status === 'shipped' && (!String(salesForm.expressCompany || '').trim() || !String(salesForm.expressNo || '').trim())) {
    fail('订单变更为已发货时必须填写物流公司和物流单号')
  }
  salesForm.items.forEach((item, index) => {
    const label = `第 ${index + 1} 项`
    if (!String(item.modelCode || '').trim()) fail(`${label}型号不能为空`)
    if (!Number(item.quantity) || Number(item.quantity) <= 0) fail(`${label}数量必须大于 0`)
    if (!Number(item.weight) || Number(item.weight) <= 0) fail(`${label}克重必须大于 0`)
    if (!Number(item.spec) || Number(item.spec) <= 0) fail(`${label}规格必须大于 0`)
  })
}

function validateProductionForm() {
  if (!productionForm.modelCode.trim()) fail('请输入面料型号')
  if (!productionForm.deliveryDate) fail('请选择交付日期')
  if (!Number(productionForm.weight) || Number(productionForm.weight) <= 0) fail('克重必须大于 0')
  if (!Number(productionForm.spec) || Number(productionForm.spec) <= 0) fail('规格必须大于 0')
  if (!Number(productionForm.quantity) || Number(productionForm.quantity) < 1) fail('数量至少为 1')
}

function buildSalesPayload() {
  return {
    customerName: salesForm.customerName.trim(),
    customerPhone: blank(salesForm.customerPhone),
    projectName: salesForm.projectName.trim(),
    deliveryDate: blank(salesForm.deliveryDate),
    expressCompany: blank(salesForm.expressCompany),
    expressNo: blank(salesForm.expressNo),
    isInvoice: Number(salesForm.isInvoice || 0),
    remark: blank(salesForm.remark),
    attachmentName: blank(salesForm.attachmentName),
    attachmentUrl: blank(salesForm.attachmentUrl),
    attachmentSize: salesForm.attachmentSize || null,
    status: salesForm.status,
    createProductionOrder: formMode.value === 'create' ? Number(salesForm.createProductionOrder || 0) : 0,
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
    contactPhone: blank(productionForm.contactPhone),
    modelCode: productionForm.modelCode.trim(),
    fabric: blank(productionForm.fabric),
    weight: Number(productionForm.weight),
    spec: Number(productionForm.spec),
    color: blank(productionForm.color),
    quantity: Number(productionForm.quantity),
    price: productionForm.price === '' || productionForm.price === null ? null : Number(productionForm.price),
    deliveryDate: `${productionForm.deliveryDate} 00:00:00`,
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

function fail(message) {
  ElMessage.warning(message)
  throw new Error(message)
}

function blank(value) {
  const text = String(value || '').trim()
  return text ? text : null
}

function normalizeText(value) {
  return String(value || '').trim()
}

function toDateInput(value) {
  return value ? String(value).slice(0, 10) : ''
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

function salesStatusLabel(status) {
  return salesStatuses.find(item => item.value === status)?.label || status || '未设置'
}

function productionStatusLabel(status) {
  return productionStatuses.find(item => item.value === status)?.label || status || '未设置'
}

function invoiceLabel(value) {
  return Number(value || 0) === 1 ? '已开票' : '未开票'
}

function invoiceClass(value) {
  return Number(value || 0) === 1 ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
}

function salesStatusClass(status) {
  return {
    'bg-secondary-fixed/20 text-secondary': status === 'pending_confirm' || status === 'pending_pay',
    'bg-primary/10 text-primary': status === 'pending_ship',
    'bg-tertiary-fixed/20 text-tertiary': status === 'shipped',
    'bg-green-100 text-green-700': status === 'completed',
    'bg-error-container text-error': status === 'cancelled'
  }
}

function productionStatusClass(status) {
  return {
    'bg-secondary-fixed/20 text-secondary': status === 'pending_confirm' || status === 'pending_material',
    'bg-primary/10 text-primary': status === 'producing' || status === 'pending_ship',
    'bg-tertiary-fixed/20 text-tertiary': status === 'shipped',
    'bg-green-100 text-green-700': status === 'completed'
  }
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

.attachment-uploader {
  margin-top: .5rem;
  cursor: pointer;
  border-radius: 1rem;
  border: 1px dashed rgba(31, 111, 255, .35);
  background: linear-gradient(135deg, rgba(31, 111, 255, .06), rgba(255, 176, 0, .08));
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

.stat-card {
  min-width: 132px;
  border-radius: 1rem;
  border: 1px solid rgba(0, 82, 204, .1);
  background: rgb(var(--surface-container-high));
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .05)
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
  color: rgb(var(--primary))
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

.icon-btn {
  border-radius: .375rem;
  padding: .375rem
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
  border-bottom: 1px solid rgba(148, 163, 184, .2);
  background: #fff;
  padding: 1.5rem
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
