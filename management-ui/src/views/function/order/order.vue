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
                <div>
                  <label class="field-label">客户名称 *</label>
                  <input v-model.trim="salesForm.customerName" list="sales-customer-options" class="box-input"
                         type="text" @input="handleSalesCustomerChange" @change="handleSalesCustomerChange"/>
                  <datalist id="sales-customer-options">
                    <option v-for="option in customerOptions" :key="option.id" :value="option.customerName"></option>
                  </datalist>
                </div>
                <div>
                  <label class="field-label">联系电话</label>
                  <input v-model.trim="salesForm.customerPhone" class="box-input" type="text"/>
                </div>
                <div>
                  <label class="field-label">项目名称 *</label>
                  <input v-model.trim="salesForm.projectName" list="sales-project-options" class="box-input"
                         type="text"/>
                  <datalist id="sales-project-options">
                    <option v-for="projectName in selectedCustomerProjects" :key="projectName"
                            :value="projectName"></option>
                  </datalist>
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
  saveProductionOrder,
  saveSalesOrder
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
const formVisible = ref(false)
const formMode = ref('create')
const editingOrderId = ref('')
const submitting = ref(false)
const salesForm = reactive(defaultSalesForm())
const productionForm = reactive(defaultProductionForm())

const currentStatuses = computed(() => currentTab.value === 'sales' ? salesStatuses : productionStatuses)
const currentState = computed(() => currentTab.value === 'sales' ? salesState : productionState)
const selectedCustomerProjects = computed(() => {
  const customerName = salesForm.customerName.trim()
  if (!customerName) return []
  const option = customerOptions.value.find(item => item.customerName === customerName)
  return option?.projectNames || []
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
}

function resetProductionForm() {
  Object.assign(productionForm, defaultProductionForm())
}

async function loadCustomerOptions(keyword) {
  customerOptions.value = await getCustomerOptions(keyword ? {keyword} : undefined)
}

async function handleSalesCustomerChange() {
  const keyword = salesForm.customerName.trim()
  await loadCustomerOptions(keyword || undefined)
  if (!keyword) {
    salesForm.projectName = ''
    return
  }
  if (selectedCustomerProjects.value.length && !selectedCustomerProjects.value.includes(salesForm.projectName)) {
    salesForm.projectName = ''
  }
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
  currentTab.value === 'sales' ? await loadSalesOrders() : await loadProductionOrders()
}

async function changePage(page) {
  currentState.value.page = page
  currentTab.value === 'sales' ? await loadSalesOrders() : await loadProductionOrders()
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
  currentTab.value === 'sales' ? resetSalesForm() : resetProductionForm()
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

async function submitForm() {
  submitting.value = true
  try {
    if (currentTab.value === 'sales') {
      validateSalesForm()
      const payload = buildSalesPayload()
      formMode.value === 'create' ? await createSalesOrder(payload) : await saveSalesOrder(editingOrderId.value, payload)
      ElMessage.success(formMode.value === 'create' ? '销售订单创建成功' : '销售订单保存成功')
      closeForm()
      await loadSalesOrders()
      if (payload.createProductionOrder === 1) await loadProductionOrders()
      return
    }

    validateProductionForm()
    const payload = buildProductionPayload()
    formMode.value === 'create' ? await createProductionOrder(payload) : await saveProductionOrder(editingOrderId.value, payload)
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

function toDateInput(value) {
  return value ? String(value).slice(0, 10) : ''
}

function num(value) {
  return value === null || value === undefined || value === '' ? '' : Number(value)
}

function formatAmount(value) {
  return value === null || value === undefined || value === '' ? '0.00' : Number(value).toFixed(2)
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
