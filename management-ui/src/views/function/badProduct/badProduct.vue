<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">report_problem</span>
            {{ scopeMeta.eyebrow }}
          </div>
          <h1 class="function-page-title">{{ scopeMeta.title }}</h1>
          <p class="function-page-desc">
            {{ scopeMeta.desc }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
            v-permission="'badproduct:save'"
            @click="openCreate"
            class="function-action-primary"
          >
            <span class="material-symbols-outlined text-lg align-middle mr-1">add_circle</span>{{ scopeMeta.createText }}
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          v-for="scope in scopeOptions"
          :key="scope.value"
          type="button"
          class="text-left rounded-2xl border px-5 py-4 transition-all"
          :class="activeScope === scope.value ? 'border-primary bg-primary/10 shadow-sm' : 'border-outline-variant/30 bg-white hover:border-primary/50'"
          @click="handleScopeChange(scope.value)"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-base font-black text-primary">{{ scope.tabTitle }}</p>
              <p class="mt-1 text-xs text-on-surface-variant leading-5">{{ scope.tabDesc }}</p>
            </div>
            <span class="material-symbols-outlined text-primary">{{ scope.icon }}</span>
          </div>
        </button>
      </section>

      <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">总记录数</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ pagination.total }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前页待处理</p>
          <h3 class="text-4xl font-black text-amber-600 mt-2">{{ stats.pending }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前页已处理</p>
          <h3 class="text-4xl font-black text-emerald-600 mt-2">{{ stats.processed }}</h3>
        </div>
        <div class="bg-[#1a365d] text-white p-6 rounded-xl shadow-md">
          <p class="text-xs font-bold uppercase tracking-widest opacity-80">当前页损失金额</p>
          <h3 class="text-4xl font-black mt-2">¥{{ money(stats.lossAmount) }}</h3>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden ring-1 ring-outline-variant/20">
        <div class="px-6 py-4 border-b border-surface-variant/50 flex flex-wrap items-center justify-between gap-4">
          <div class="flex flex-wrap items-center gap-3">
            <input
              v-model.trim="query.keyword"
              @keyup.enter="handleFilter"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
              placeholder="搜索编号、订单、描述或负责人"
            />
            <select v-model="query.status" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部状态</option>
              <option value="pending">待处理</option>
              <option value="pending_audit">审核中</option>
              <option value="processed">已处理</option>
            </select>
            <select v-model="query.type" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部类型</option>
              <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
            <DateFilterInput
              v-model="query.date"
              placeholder="发生日期"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
            />
            <DateFilterInput
              v-model="query.startDate"
              placeholder="开始日期"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
            />
            <DateFilterInput
              v-model="query.endDate"
              placeholder="结束日期"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
            />
            <button @click="handleFilter" class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold">查询</button>
            <button @click="resetFilter" class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold">
              重置
            </button>
            <TableColumnSettings
              :columns="badProductTableColumns"
              export-module="badproduct"
              @move="moveBadProductTableColumn"
              @reset="resetBadProductTableColumns"
            />
          </div>
          <span class="text-xs text-on-surface-variant">共 {{ pagination.total }} {{ scopeMeta.countText }}</span>
        </div>

        <div class="responsive-table-wrap relative min-h-[260px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
          </div>
          <table class="responsive-data-table w-full text-left border-collapse">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th
                  v-for="column in badProductTableColumns"
                  :key="column.key"
                  class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider"
                  :class="column.align === 'right' ? 'text-right' : ''"
                >
                  {{ column.label }}
                </th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/30">
              <tr
                v-for="item in rows"
                :key="item.defectiveId"
                class="cursor-pointer hover:bg-surface-container-high/40 transition-colors"
                @click="openDetail(item)"
              >
                <td
                  v-for="column in badProductTableColumns"
                  :key="column.key"
                  :data-label="column.label"
                  class="px-6 py-4"
                  :class="badProductCellClass(column.key)"
                >
                  <template v-if="column.key === 'defectiveId'">
                    <p class="text-sm font-bold text-primary">{{ item.defectiveId }}</p>
                    <p class="text-[10px] text-on-surface-variant line-clamp-1">{{ item.description || '未填写问题描述' }}</p>
                  </template>
                  <template v-else-if="column.key === 'orderId'">{{ item.orderId || '未关联' }}</template>
                  <template v-else-if="column.key === 'type'">{{ typeLabel(item.type) }}</template>
                  <template v-else-if="column.key === 'quantity'">{{ money(item.quantity) }}</template>
                  <template v-else-if="column.key === 'lossAmount'">{{ lossAmountLabel(item.lossAmount) }}</template>
                  <template v-else-if="column.key === 'creator'">{{ item.creator || '--' }}</template>
                  <template v-else-if="column.key === 'status'">
                    <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">
                      {{ statusLabel(item.status) }}
                    </span>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatDateTime(item.createTime) }}</template>
                </td>
                <td class="px-6 py-4 text-right space-x-2" data-label="操作">
                  <button @click.stop="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">
                    详情
                  </button>
                  <button v-permission="'badproduct:save'" @click.stop="openEdit(item)" class="text-secondary hover:bg-surface-container-high px-3 py-1.5 rounded-lg text-xs font-bold">
                    编辑
                  </button>
                  <button
                    v-if="item.status === 'pending'"
                    v-permission="'badproduct:process'"
                    @click.stop="openProcess(item)"
                    class="text-emerald-700 hover:bg-emerald-50 px-3 py-1.5 rounded-lg text-xs font-bold"
                  >
                    处理
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td :colspan="badProductTableColumnCount" class="px-6 py-12 text-center text-sm text-on-surface-variant">{{ scopeMeta.emptyText }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex items-center justify-between text-sm text-on-surface-variant border-t border-surface-variant/50">
          <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-3 py-1.5 rounded bg-white border disabled:opacity-50">
              上一页
            </button>
            <button
              @click="changePage(query.pageNum + 1)"
              :disabled="query.pageNum >= totalPages"
              class="px-3 py-1.5 rounded bg-white border disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </div>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || formVisible || processVisible" class="fixed inset-0 bg-black/20 backdrop-blur-[2px] z-40" @click="closePanels"></div>
    </transition>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="detailVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">{{ scopeMeta.detailTitle }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ detailRecord?.defectiveId || '--' }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto" v-if="detailRecord">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">异常数量</span>
            <p class="text-xl font-black text-primary">{{ money(detailRecord.quantity) }}</p>
          </div>
          <div class="bg-surface-container-low p-4 rounded-xl">
            <span class="text-[10px] text-on-surface-variant font-bold">损失金额</span>
            <p class="text-xl font-black text-primary">{{ lossAmountLabel(detailRecord.lossAmount) }}</p>
          </div>
        </div>

        <section class="space-y-3">
          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">闭环信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">负责人员：</span>{{ detailRecord.responsiblePerson || '未填写' }}</p>
              <p><span class="text-on-surface-variant">处理措施：</span>{{ detailRecord.processMeasure || '未填写' }}</p>
              <p><span class="text-on-surface-variant">改进方案：</span>{{ detailRecord.improvementPlan || '未填写' }}</p>
            </div>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">基础信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">关联订单：</span>{{ detailRecord.orderId || '未关联' }}</p>
              <p><span class="text-on-surface-variant">质量类型：</span>{{ typeLabel(detailRecord.type) }}</p>
              <p><span class="text-on-surface-variant">登记人：</span>{{ detailRecord.creator || '--' }}</p>
              <p><span class="text-on-surface-variant">状态：</span>{{ statusLabel(detailRecord.status) }}</p>
              <p><span class="text-on-surface-variant">登记时间：</span>{{ formatDateTime(detailRecord.createTime) }}</p>
            </div>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">问题描述</p>
            <p class="text-sm leading-6">{{ detailRecord.description || '未填写问题描述。' }}</p>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">附件凭证</p>
            <button
              v-if="detailRecord.attachmentUrl"
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-white px-3 py-2 text-sm font-bold text-primary ring-1 ring-outline-variant/30 hover:bg-primary/5"
              @click="openAttachment(detailRecord.attachmentUrl, detailRecord.attachmentName)"
            >
              <span class="material-symbols-outlined text-[18px]">attach_file</span>
              {{ detailRecord.attachmentName || '下载附件' }}
            </button>
            <p v-else class="text-sm text-on-surface-variant">暂无附件凭证</p>
          </div>

          <div class="rounded-xl bg-surface-container-low p-4">
            <p class="text-[10px] text-on-surface-variant font-bold mb-2">处理信息</p>
            <div class="space-y-2 text-sm">
              <p><span class="text-on-surface-variant">处理方式：</span>{{ detailRecord.processMethod || '未处理' }}</p>
              <p><span class="text-on-surface-variant">处理备注：</span>{{ detailRecord.processRemark || '未填写处理备注。' }}</p>
            </div>
          </div>
        </section>
      </div>
    </aside>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="formVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start gap-3">
        <div>
          <h3 class="font-black text-primary text-lg">{{ editingRecord ? scopeMeta.editTitle : scopeMeta.createTitle }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ scopeMeta.formSubtitle }}</p>
        </div>
        <div class="drawer-head-actions">
          <button @click="closeForm" class="p-1 hover:bg-surface-container-high rounded-full">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <BusinessTimeCorrectionPanel
          v-model="form.createTime"
          :active="timeCorrectionMode"
          data-field="badProduct.createTime"
          title="业务时间修正"
          label="业务时间"
          description="用于修正当前记录的业务时间。"
        />
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">关联订单</span>
          <input v-model.trim="form.orderId" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入订单号" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">质量类型</span>
          <select v-model="form.type" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary">
            <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">异常数量</span>
          <input v-model.trim="form.quantity" data-field="badProduct.quantity" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入异常数量" />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">损失金额</span>
          <select
            v-model="form.lossAmount"
            data-field="badProduct.lossAmount"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
          >
            <option value="">请选择损失金额档位</option>
            <option v-for="item in lossAmountOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">问题描述</span>
          <textarea
            v-model.trim="form.description"
            rows="5"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入质量问题说明"
          />
        </label>
        <div>
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">附件凭证</span>
          <DragAttachmentUpload
            class="mt-2"
            title="上传图片、PDF、Word、Excel、文本或压缩包"
            helper-text="支持拖拽上传，单个文件不超过 10MB"
            :uploading="attachmentUploading"
            :file-name="form.attachmentName"
            :file-url="form.attachmentUrl"
            :file-size="form.attachmentSize"
            @select="handleAttachmentFile"
            @download="openAttachment(form.attachmentUrl, form.attachmentName)"
            @remove="removeAttachment"
          />
        </div>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="closeForm" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button v-permission="'badproduct:save'" @click="submitForm" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">保存</button>
      </div>
    </aside>

    <aside
      class="fixed top-0 right-0 h-full w-full sm:w-[460px] bg-white/95 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300"
      :class="processVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">{{ scopeMeta.processTitle }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ processingRecord?.defectiveId || '--' }}</p>
        </div>
        <button @click="closeProcess" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
        <div class="rounded-xl bg-surface-container-low p-4 text-sm space-y-2" v-if="processingRecord">
          <p><span class="text-on-surface-variant">关联订单：</span>{{ processingRecord.orderId || '未关联' }}</p>
          <p><span class="text-on-surface-variant">当前状态：</span>{{ statusLabel(processingRecord.status) }}</p>
        </div>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">负责人员</span>
          <input
            v-model.trim="processForm.responsiblePerson"
            data-field="badProduct.responsiblePerson"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="请输入负责人员"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理方式</span>
          <input
            v-model.trim="processForm.method"
            data-field="badProduct.processMethod"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="例如报废、返工、让步接收"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理措施</span>
          <textarea
            v-model.trim="processForm.processMeasure"
            data-field="badProduct.processMeasure"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入处理措施"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">改进方案</span>
          <textarea
            v-model.trim="processForm.improvementPlan"
            data-field="badProduct.improvementPlan"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入改进方案"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理备注</span>
          <textarea
            v-model.trim="processForm.remark"
            rows="5"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入处理说明"
          />
        </label>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="closeProcess" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button v-permission="'badproduct:process'" @click="submitProcess" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">提交审核</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  downloadBadProductAttachment,
  getBadProductPage,
  processBadProduct,
  saveBadProduct,
  uploadBadProductAttachment
} from './api/badProduct.js'
import { warnAndFocusField } from '@/utils/formFocus'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import DateFilterInput from '@/components/DateFilterInput.vue'
import BusinessTimeCorrectionPanel from '@/components/BusinessTimeCorrectionPanel.vue'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useTimeCorrectionMode } from '@/composables/useTimeCorrectionMode'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const qualityTypeOptions = [
  { value: 'raw_material', label: '原材料' },
  { value: 'process_standard', label: '工艺标准' },
  { value: 'process_flow', label: '工艺流程' },
  { value: 'other', label: '其他' }
]
const afterSalesTypeOptions = [
  { value: 'motor', label: '电机' },
  { value: 'manual_track', label: '手动轨道' },
  { value: 'electric_track', label: '电动轨道' },
  { value: 'fabric', label: '面料' },
  { value: 'electric_roller_blind', label: '电动卷帘' },
  { value: 'manual_roller_blind', label: '手动卷帘' },
  { value: 'wear_part', label: '易损件' },
  { value: 'craft', label: '工艺' },
  { value: 'installation', label: '安装' },
  { value: 'measurement', label: '测量' },
  { value: 'after_sales_other', label: '其他' }
]
const allTypeOptions = [...qualityTypeOptions, ...afterSalesTypeOptions]
const lossAmountOptions = [
  { value: '25', label: '0-50' },
  { value: '100', label: '50-200' },
  { value: '350', label: '200-500' },
  { value: '1250', label: '500-2000' },
  { value: '3500', label: '2000-5000' },
  { value: '5001', label: '5000以上' }
]
const scopeOptions = [
  {
    value: 'quality',
    tabTitle: '质量记录',
    tabDesc: '登记生产、运输和质量异常，形成责任与改进闭环。',
    eyebrow: '质量追踪中心',
    title: '质量管理',
    desc: '统一登记质量问题、运输破损和其他质量异常记录，支持处理闭环和损失跟踪。',
    createText: '新增质量记录',
    countText: '条质量记录',
    emptyText: '暂无质量记录。',
    detailTitle: '质量记录详情',
    createTitle: '新增质量记录',
    editTitle: '编辑质量记录',
    processTitle: '处理质量记录',
    formSubtitle: '保存后会形成质量记录。',
    icon: 'fact_check'
  },
  {
    value: 'afterSales',
    tabTitle: '售后管理',
    tabDesc: '记录客户售后、退换货、投诉和赔付协商，便于追踪回访。',
    eyebrow: '客户售后中心',
    title: '售后管理',
    desc: '统一管理客户售后问题、退换货、赔付协商和回访记录，确保每个售后都有处理闭环。',
    createText: '新增售后记录',
    countText: '条售后记录',
    emptyText: '暂无售后记录。',
    detailTitle: '售后记录详情',
    createTitle: '新增售后记录',
    editTitle: '编辑售后记录',
    processTitle: '处理售后记录',
    formSubtitle: '保存后会形成售后记录。',
    icon: 'support_agent'
  }
]
const defaultBadProductTableColumns = [
  { key: 'defectiveId', label: '记录编号' },
  { key: 'orderId', label: '关联订单' },
  { key: 'type', label: '记录类型' },
  { key: 'quantity', label: '数量', align: 'right' },
  { key: 'lossAmount', label: '损失金额', align: 'right' },
  { key: 'creator', label: '登记人' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '登记时间' }
]
const {
  orderedColumns: badProductTableColumns,
  moveColumn: moveBadProductTableColumn,
  resetColumns: resetBadProductTableColumns
} = useLocalTableColumns('bad-product.list', defaultBadProductTableColumns)
const badProductTableColumnCount = computed(() => badProductTableColumns.value.length + 1)

const rows = ref([])
const loading = ref(false)
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: '', type: '', date: '', keyword: '', startDate: '', endDate: '' })
const activeScope = ref('quality')
const detailVisible = ref(false)
const detailRecord = ref(null)
const formVisible = ref(false)
const editingRecord = ref(null)
const processVisible = ref(false)
const processingRecord = ref(null)
const attachmentUploading = ref(false)
const form = reactive(createEmptyForm())
const processForm = reactive({
  responsiblePerson: '',
  method: '',
  processMeasure: '',
  improvementPlan: '',
  remark: ''
})
const {
  timeCorrectionMode,
  closeTimeCorrectionMode
} = useTimeCorrectionMode({
  isAvailable: () => formVisible.value
})

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const scopeMeta = computed(() => scopeOptions.find((item) => item.value === activeScope.value) || scopeOptions[0])
const typeOptions = computed(() => activeScope.value === 'afterSales' ? afterSalesTypeOptions : qualityTypeOptions)
const stats = computed(() => {
  const pending = rows.value.filter((item) => item.status === 'pending').length
  const processed = rows.value.filter((item) => item.status === 'processed').length
  const lossAmount = rows.value.reduce((total, item) => total + Number(item.lossAmount || 0), 0)
  return { pending, processed, lossAmount }
})

function badProductCellClass(key) {
  if (key === 'quantity') return 'text-right text-sm font-bold'
  if (key === 'lossAmount') return 'text-right text-sm font-black text-primary'
  if (key === 'createTime') return 'text-xs text-on-surface-variant'
  if (key !== 'defectiveId' && key !== 'status') return 'text-sm'
  return ''
}

fetchData()

function handleScopeChange(scope) {
  if (activeScope.value === scope) {
    return
  }
  activeScope.value = scope
  query.type = ''
  query.status = ''
  query.pageNum = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getBadProductPage({
      ...query,
      businessScope: activeScope.value,
      status: query.status || undefined,
      type: query.type || undefined,
      date: query.date || undefined,
      keyword: query.keyword || undefined,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  query.pageNum = 1
  fetchData()
}

function resetFilter() {
  query.status = ''
  query.type = ''
  query.date = ''
  query.keyword = ''
  query.startDate = ''
  query.endDate = ''
  query.pageNum = 1
  fetchData()
}

function changePage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) {
    return
  }
  query.pageNum = pageNum
  fetchData()
}

function openDetail(record) {
  detailRecord.value = record
  detailVisible.value = true
}

function ensurePermission(permission) {
  if (userStore.hasPermission(permission)) {
    return true
  }
  ElMessage.warning('当前账号暂无权限')
  return false
}

function openCreate() {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  resetForm()
  editingRecord.value = null
  formVisible.value = true
}

function openEdit(record) {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  editingRecord.value = record
  form.defectiveId = record.defectiveId
  form.orderId = record.orderId || ''
  form.type = normalizeTypeForScope(record.type)
  form.quantity = record.quantity == null ? '' : String(record.quantity)
  form.lossAmount = normalizeLossAmountBucketValue(record.lossAmount)
  form.description = record.description || ''
  form.attachmentName = record.attachmentName || ''
  form.attachmentUrl = record.attachmentUrl || ''
  form.attachmentSize = record.attachmentSize || null
  form.createTime = toDateTimeLocal(record.createTime)
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  closeTimeCorrectionMode()
  editingRecord.value = null
  resetForm()
}

function openProcess(record) {
  if (!ensurePermission('badproduct:process')) {
    return
  }
  processingRecord.value = record
  resetProcessForm(record)
  processVisible.value = true
}

function closeProcess() {
  processVisible.value = false
  processingRecord.value = null
  resetProcessForm()
}

function closePanels() {
  detailVisible.value = false
  closeForm()
  closeProcess()
}

async function handleAttachmentFile(file) {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  if (!file) {
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('附件不能超过 10MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  attachmentUploading.value = true
  try {
    const result = await uploadBadProductAttachment(formData)
    form.attachmentName = result.fileName || file.name
    form.attachmentUrl = result.fileUrl || ''
    form.attachmentSize = result.fileSize || file.size
    ElMessage.success('附件上传成功')
  } finally {
    attachmentUploading.value = false
  }
}

function removeAttachment() {
  form.attachmentName = ''
  form.attachmentUrl = ''
  form.attachmentSize = null
}

async function openAttachment(url, name) {
  if (!url) {
    return
  }
  const blob = await downloadBadProductAttachment({ url, name })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name || 'quality-attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

async function submitForm() {
  if (!ensurePermission('badproduct:save')) {
    return
  }
  if (!form.quantity || Number(form.quantity) <= 0) {
    return warnAndFocusField('请填写有效的异常数量', 'badProduct.quantity')
  }
  if (!form.lossAmount || Number(form.lossAmount) <= 0) {
    return warnAndFocusField('请填写有效的损失金额', 'badProduct.lossAmount')
  }
  if (!validateCreateTimeInput(form.createTime)) {
    return
  }

  await saveBadProduct({
    defectiveId: form.defectiveId || undefined,
    orderId: form.orderId || undefined,
    type: form.type,
    quantity: Number(form.quantity),
    lossAmount: Number(form.lossAmount),
    description: form.description || undefined,
    attachmentName: form.attachmentName || undefined,
    attachmentUrl: form.attachmentUrl || undefined,
    attachmentSize: form.attachmentSize || undefined,
    createTime: formatCreateTimePayload(form.createTime) || undefined
  })
  ElMessage.success(editingRecord.value ? '质量记录已更新' : '质量记录已新增')
  closeForm()
  await fetchData()
}

async function submitProcess() {
  if (!ensurePermission('badproduct:process')) {
    return
  }
  if (!processForm.responsiblePerson) {
    return warnAndFocusField('请填写负责人员', 'badProduct.responsiblePerson')
  }
  if (!processForm.method) {
    return warnAndFocusField('请填写处理方式', 'badProduct.processMethod')
  }
  if (!processForm.processMeasure) {
    return warnAndFocusField('请填写处理措施', 'badProduct.processMeasure')
  }
  if (!processForm.improvementPlan) {
    return warnAndFocusField('请填写改进方案', 'badProduct.improvementPlan')
  }

  await processBadProduct({
    defectiveId: processingRecord.value?.defectiveId,
    method: processForm.method,
    responsiblePerson: processForm.responsiblePerson,
    processMeasure: processForm.processMeasure,
    improvementPlan: processForm.improvementPlan,
    remark: processForm.remark || undefined
  })
  ElMessage.success('质量处理已提交审核')
  closeProcess()
  await fetchData()
}

function typeLabel(value) {
  return allTypeOptions.find((item) => item.value === value)?.label || '其他'
}

function normalizeLossAmountBucketValue(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number) || number <= 0) {
    return ''
  }
  if (number <= 50) return '25'
  if (number <= 200) return '100'
  if (number <= 500) return '350'
  if (number <= 2000) return '1250'
  if (number <= 5000) return '3500'
  return '5001'
}

function lossAmountLabel(value) {
  const bucketValue = normalizeLossAmountBucketValue(value)
  return lossAmountOptions.find((item) => item.value === bucketValue)?.label || '--'
}

function statusLabel(value) {
  if (value === 'processed') return '已处理'
  if (value === 'pending_audit') return '审核中'
  return '待处理'
}

function statusClass(value) {
  if (value === 'processed') return 'bg-emerald-100 text-emerald-700'
  if (value === 'pending_audit') return 'bg-sky-100 text-sky-700'
  return 'bg-amber-100 text-amber-700'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function toDateTimeLocal(value) {
  if (!value) {
    return ''
  }
  const normalized = String(value).replace(' ', 'T')
  return normalized.length >= 16 ? normalized.slice(0, 19) : normalized
}

function formatCreateTimePayload(value) {
  if (!value) {
    return ''
  }
  const text = String(value).trim()
  return text.length === 16 ? `${text}:00` : text
}

function validateCreateTimeInput(value) {
  if (!value) {
    return true
  }
  const payload = formatCreateTimePayload(value)
  const date = new Date(payload)
  if (!Number.isFinite(date.getTime())) {
    warnAndFocusField('登记时间格式不正确，请选择完整日期和时间', 'badProduct.createTime')
    return false
  }
  if (date.getTime() > Date.now()) {
    warnAndFocusField('登记时间不能晚于当前时间', 'badProduct.createTime')
    return false
  }
  return true
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatFileSize(value) {
  const size = Number(value || 0)
  if (!Number.isFinite(size) || size <= 0) {
    return '--'
  }
  if (size < 1024) {
    return `${size}B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function resetProcessForm(record = null) {
  processForm.responsiblePerson = record?.responsiblePerson || ''
  processForm.method = record?.processMethod || ''
  processForm.processMeasure = record?.processMeasure || ''
  processForm.improvementPlan = record?.improvementPlan || ''
  processForm.remark = record?.processRemark || ''
}

function createEmptyForm() {
  return {
    defectiveId: '',
    orderId: '',
    type: defaultTypeForScope(),
    quantity: '',
    lossAmount: '',
    description: '',
    attachmentName: '',
    attachmentUrl: '',
    attachmentSize: null,
    createTime: ''
  }
}

function defaultTypeForScope() {
  return activeScope.value === 'afterSales' ? 'motor' : 'raw_material'
}

function normalizeTypeForScope(value) {
  const options = activeScope.value === 'afterSales' ? afterSalesTypeOptions : qualityTypeOptions
  return options.some((item) => item.value === value) ? value : defaultTypeForScope()
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

.drawer-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.time-correction-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  border: 1px solid rgba(31, 63, 95, 0.24);
  background: rgba(238, 244, 251, 0.86);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 900;
  color: #1f3f5f;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.time-correction-toggle small {
  color: rgba(71, 85, 105, 0.72);
  font-size: 10px;
  font-weight: 800;
}

.time-correction-toggle.active {
  border-color: rgba(31, 63, 95, 0.86);
  background: #1f3f5f;
  color: #fff;
}

.time-correction-toggle.active small {
  color: rgba(255, 255, 255, 0.78);
}
</style>
