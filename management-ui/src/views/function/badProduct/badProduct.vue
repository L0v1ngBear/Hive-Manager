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
              <option value="processed">已处理</option>
            </select>
            <select v-model="query.type" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部类型</option>
              <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
            <input
              v-model="query.date"
              type="date"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
            />
            <input
              v-model="query.startDate"
              type="date"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
              title="开始日期"
            />
            <input
              v-model="query.endDate"
              type="date"
              class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none"
              title="结束日期"
            />
            <button @click="handleFilter" class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold">查询</button>
            <button @click="resetFilter" class="px-4 py-2 bg-surface-container-highest text-on-surface rounded-lg text-sm font-bold">
              重置
            </button>
            <TableColumnSettings
              :columns="badProductTableColumns"
              @move="moveBadProductTableColumn"
              @reset="resetBadProductTableColumns"
            />
          </div>
          <span class="text-xs text-on-surface-variant">共 {{ pagination.total }} {{ scopeMeta.countText }}</span>
        </div>

        <div class="overflow-x-auto relative min-h-[260px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
          </div>
          <table class="w-full text-left border-collapse min-w-[1120px]">
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
                  <template v-else-if="column.key === 'lossAmount'">¥{{ money(item.lossAmount) }}</template>
                  <template v-else-if="column.key === 'creator'">{{ item.creator || '--' }}</template>
                  <template v-else-if="column.key === 'status'">
                    <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">
                      {{ statusLabel(item.status) }}
                    </span>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatDateTime(item.createTime) }}</template>
                </td>
                <td class="px-6 py-4 text-right space-x-2">
                  <button @click.stop="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">
                    详情
                  </button>
                  <button @click.stop="openEdit(item)" class="text-secondary hover:bg-surface-container-high px-3 py-1.5 rounded-lg text-xs font-bold">
                    编辑
                  </button>
                  <button
                    v-if="item.status !== 'processed'"
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
            <p class="text-xl font-black text-primary">¥{{ money(detailRecord.lossAmount) }}</p>
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
      <div class="p-6 border-b flex justify-between items-start">
        <div>
          <h3 class="font-black text-primary text-lg">{{ editingRecord ? scopeMeta.editTitle : scopeMeta.createTitle }}</h3>
          <p class="text-xs text-on-surface-variant mt-1">{{ scopeMeta.formSubtitle }}</p>
        </div>
        <button @click="closeForm" class="p-1 hover:bg-surface-container-high rounded-full">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 p-6 space-y-5 overflow-y-auto">
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
          <input v-model.trim="form.lossAmount" data-field="badProduct.lossAmount" type="number" min="0" step="0.01" class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary" placeholder="请输入损失金额" />
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
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">负责人员</span>
          <input
            v-model.trim="form.responsiblePerson"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="请输入负责人员"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理措施</span>
          <textarea
            v-model.trim="form.processMeasure"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入处理措施"
          />
        </label>
        <label class="block">
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">改进方案</span>
          <textarea
            v-model.trim="form.improvementPlan"
            rows="3"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary resize-none"
            placeholder="请输入改进方案"
          />
        </label>
        <div class="rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">附件凭证</p>
              <p class="mt-1 text-xs text-on-surface-variant">可上传图片、PDF、Word、Excel、文本或压缩包，单个文件不超过 10MB。</p>
            </div>
            <button
              type="button"
              class="rounded-lg bg-primary px-3 py-2 text-xs font-bold text-white disabled:opacity-60"
              :disabled="attachmentUploading"
              @click="triggerAttachmentUpload"
            >
              {{ attachmentUploading ? '上传中...' : '上传附件' }}
            </button>
          </div>
          <input ref="attachmentInputRef" type="file" class="hidden" @change="handleAttachmentChange" />
          <div v-if="form.attachmentUrl" class="mt-3 flex items-center justify-between gap-3 rounded-lg bg-primary/5 px-3 py-2">
            <button type="button" class="truncate text-left text-sm font-bold text-primary hover:underline" @click="openAttachment(form.attachmentUrl, form.attachmentName)">
              {{ form.attachmentName || '查看附件' }}
            </button>
            <div class="flex items-center gap-2 shrink-0">
              <span class="text-xs text-on-surface-variant">{{ formatFileSize(form.attachmentSize) }}</span>
              <button type="button" class="text-xs font-bold text-rose-600" @click="removeAttachment">移除</button>
            </div>
          </div>
        </div>
      </div>
      <div class="p-6 border-t border-outline-variant/30 flex gap-3">
        <button @click="closeForm" class="flex-1 px-4 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm">取消</button>
        <button @click="submitForm" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">保存</button>
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
          <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">处理方式</span>
          <input
            v-model.trim="processForm.method"
            data-field="badProduct.processMethod"
            class="mt-2 w-full rounded-xl border border-outline-variant/40 px-4 py-3 text-sm outline-none focus:border-primary"
            placeholder="例如报废、返工、让步接收"
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
        <button @click="submitProcess" class="flex-1 px-4 py-3 rounded-xl bg-primary text-white font-bold text-sm shadow-md">确认处理</button>
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
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'

const qualityTypeOptions = [
  { value: 'quality', label: '质量问题' },
  { value: 'damage', label: '运输破损' },
  { value: 'wrong', label: '生产错误' },
  { value: 'other', label: '其他原因' }
]
const afterSalesTypeOptions = [
  { value: 'after_sales', label: '售后问题' },
  { value: 'return_exchange', label: '退换货' },
  { value: 'compensation', label: '赔付协商' },
  { value: 'customer_complaint', label: '客户投诉' }
]
const allTypeOptions = [...qualityTypeOptions, ...afterSalesTypeOptions]
const scopeOptions = [
  {
    value: 'quality',
    tabTitle: '质量记录',
    tabDesc: '登记生产、运输和内部质量异常，沉淀责任与改进闭环。',
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
    formSubtitle: '保存后会写入真实质量台账。',
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
    formSubtitle: '保存后会写入真实售后台账。',
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
const attachmentInputRef = ref(null)
const attachmentUploading = ref(false)
const form = reactive(createEmptyForm())
const processForm = reactive({ method: '', remark: '' })

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

function openCreate() {
  resetForm()
  editingRecord.value = null
  formVisible.value = true
}

function openEdit(record) {
  editingRecord.value = record
  form.defectiveId = record.defectiveId
  form.orderId = record.orderId || ''
  form.type = record.type || 'quality'
  form.quantity = record.quantity == null ? '' : String(record.quantity)
  form.lossAmount = record.lossAmount == null ? '' : String(record.lossAmount)
  form.description = record.description || ''
  form.responsiblePerson = record.responsiblePerson || ''
  form.processMeasure = record.processMeasure || ''
  form.improvementPlan = record.improvementPlan || ''
  form.attachmentName = record.attachmentName || ''
  form.attachmentUrl = record.attachmentUrl || ''
  form.attachmentSize = record.attachmentSize || null
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  editingRecord.value = null
  resetForm()
}

function openProcess(record) {
  processingRecord.value = record
  processForm.method = ''
  processForm.remark = ''
  processVisible.value = true
}

function closeProcess() {
  processVisible.value = false
  processingRecord.value = null
  processForm.method = ''
  processForm.remark = ''
}

function closePanels() {
  detailVisible.value = false
  closeForm()
  closeProcess()
}

function triggerAttachmentUpload() {
  attachmentInputRef.value?.click()
}

async function handleAttachmentChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('附件不能超过 10MB')
    event.target.value = ''
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
    event.target.value = ''
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
  if (!form.quantity || Number(form.quantity) <= 0) {
    return warnAndFocusField('请填写有效的异常数量', 'badProduct.quantity')
  }
  if (!form.lossAmount || Number(form.lossAmount) <= 0) {
    return warnAndFocusField('请填写有效的损失金额', 'badProduct.lossAmount')
  }

  await saveBadProduct({
    defectiveId: form.defectiveId || undefined,
    orderId: form.orderId || undefined,
    type: form.type,
    quantity: Number(form.quantity),
    lossAmount: Number(form.lossAmount),
    description: form.description || undefined,
    responsiblePerson: form.responsiblePerson || undefined,
    processMeasure: form.processMeasure || undefined,
    improvementPlan: form.improvementPlan || undefined,
    attachmentName: form.attachmentName || undefined,
    attachmentUrl: form.attachmentUrl || undefined,
    attachmentSize: form.attachmentSize || undefined
  })
  ElMessage.success(editingRecord.value ? '质量记录已更新' : '质量记录已新增')
  closeForm()
  await fetchData()
}

async function submitProcess() {
  if (!processForm.method) {
    return warnAndFocusField('请填写处理方式', 'badProduct.processMethod')
  }

  await processBadProduct({
    defectiveId: processingRecord.value?.defectiveId,
    method: processForm.method,
    remark: processForm.remark || undefined
  })
  ElMessage.success('质量记录已处理')
  closeProcess()
  await fetchData()
}

function typeLabel(value) {
  return allTypeOptions.find((item) => item.value === value)?.label || '其他原因'
}

function statusLabel(value) {
  return value === 'processed' ? '已处理' : '待处理'
}

function statusClass(value) {
  return value === 'processed' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 19)
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

function createEmptyForm() {
  return {
    defectiveId: '',
    orderId: '',
    type: activeScope.value === 'afterSales' ? 'after_sales' : 'quality',
    quantity: '',
    lossAmount: '',
    description: '',
    responsiblePerson: '',
    processMeasure: '',
    improvementPlan: '',
    attachmentName: '',
    attachmentUrl: '',
    attachmentSize: null
  }
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
</style>
