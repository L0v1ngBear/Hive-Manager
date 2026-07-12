<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">rule_folder</span>
            流程审批中心
          </div>
          <h1 class="function-page-title">审批中心</h1>
          <p class="function-page-desc">
              这里只展示和您本人相关的审批请求，包括您发起的申请，以及当前流转到您这里的待审批事项。
            </p>
        </div>
        <div class="flex items-center gap-3">
          <el-button
            v-permission="'approval:finance:audit'"
            @click="openDefaultAuditorDialog"
            plain
          >
            <span class="material-symbols-outlined text-[18px]">manage_accounts</span>审批负责人
          </el-button>
          <el-button
            @click="refreshAll"
            plain
          >
            <span class="material-symbols-outlined text-[18px]">refresh</span>刷新
          </el-button>
          <el-button
            v-if="activeTab === 'finance'"
            v-permission="'approval:finance:submit'"
            @click="openFinanceDialog"
            type="primary"
          >
            <span class="material-symbols-outlined text-[18px]">add_circle</span>新建财务审批
          </el-button>
          <el-button
            v-if="activeTab === 'resignation'"
            v-permission="'approval:resignation:submit'"
            @click="openResignationDialog"
            type="primary"
          >
            <span class="material-symbols-outlined text-[18px]">person_remove</span>新建离职申请
          </el-button>
        </div>
      </header>

      <section class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <article class="bg-surface-container-lowest p-5 rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col justify-between">
          <div class="flex justify-between items-start mb-2">
            <p class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest">待我处理</p>
            <div class="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <span class="material-symbols-outlined text-[18px]">hourglass_empty</span>
            </div>
          </div>
          <div>
            <h3 class="text-3xl font-black text-on-surface">{{ approvalSummary.totalPending }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">当前需要处理的全部事项</p>
          </div>
        </article>

        <article class="bg-surface-container-lowest p-5 rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col justify-between">
          <div class="flex justify-between items-start mb-2">
            <p class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest">我发起的</p>
            <div class="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <span class="material-symbols-outlined text-[18px]">outgoing_mail</span>
            </div>
          </div>
          <div>
            <h3 class="text-3xl font-black text-on-surface">{{ stats.mine }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">由您提交的审批申请总数</p>
          </div>
        </article>

        <article class="bg-surface-container-lowest p-5 rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col justify-between">
          <div class="flex justify-between items-start mb-2">
            <p class="text-[11px] font-bold text-on-surface-variant uppercase tracking-widest">已通过</p>
            <div class="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <span class="material-symbols-outlined text-[18px]">check_circle</span>
            </div>
          </div>
          <div>
            <h3 class="text-3xl font-black text-on-surface">{{ stats.approved }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">和您相关且已通过的记录</p>
          </div>
        </article>

        <article class="bg-surface-container-lowest p-5 rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col justify-between relative overflow-hidden">
          <div class="absolute -right-2 -bottom-2 text-primary/5">
            <span class="material-symbols-outlined text-[80px]">dataset</span>
          </div>
          <div class="flex justify-between items-start mb-2 relative z-10">
            <p class="text-[11px] font-bold text-primary uppercase tracking-widest">当前列表</p>
          </div>
          <div class="relative z-10">
            <h3 class="text-3xl font-black text-primary">{{ filteredRows.length }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">与您本人相关的审批请求</p>
          </div>
        </article>
      </section>

      <section class="bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col">
        <div class="p-4 md:p-5 border-b border-surface-variant/40 flex items-center justify-between gap-4">
          <el-tabs v-model="activeTab" class="approval-tabs" @tab-change="changeTab">
            <el-tab-pane
              v-for="tab in tabs"
              :key="tab.value"
              v-permission="tab.permissions"
              :name="tab.value"
              :disabled="!canAccessTab(tab)"
            >
              <template #label>
                <el-badge :value="tabPendingCount(tab.value)" :hidden="tabPendingCount(tab.value) === 0" :max="99">
                  <span class="px-2">{{ tab.label }}</span>
                </el-badge>
              </template>
            </el-tab-pane>
          </el-tabs>
          <div class="flex flex-wrap items-center justify-end gap-3">
            <el-input
              v-model.trim="filters.keyword"
              clearable
              class="w-56"
              placeholder="搜索单号、申请人、摘要"
            />
            <el-select
              v-model="filters.status"
              class="w-32"
              placeholder="全部状态"
            >
              <el-option label="全部状态" value="" />
              <el-option label="待审批" value="1" />
              <el-option label="已通过" value="2" />
              <el-option label="已拒绝" value="3" />
            </el-select>
            <p class="text-xs font-bold text-on-surface-variant">列表已按“与我相关”自动过滤</p>
            <TableColumnSettings
              :columns="approvalTableColumns"
              :export-module="approvalExportModule"
              @move="moveApprovalTableColumn"
              @reset="resetApprovalTableColumns"
            />
          </div>
        </div>

        <div class="responsive-table-wrap relative min-h-[300px]">
          <el-table
            v-loading="loading"
            :data="activeTabCanViewList ? pagedRows : []"
            row-key="code"
            class="w-full"
            @row-click="openDetail"
          >
            <el-table-column
              v-for="column in approvalTableColumns"
              :key="column.key"
              :label="column.label"
              :min-width="column.key === 'summary' ? 220 : 120"
            >
              <template #default="{ row: item }">
                  <template v-if="column.key === 'code'">
                    <div class="text-sm font-black text-on-surface group-hover:text-primary transition-colors">{{ item.code }}</div>
                    <div class="text-[10px] font-medium text-on-surface-variant mt-0.5">{{ item.typeLabel }}</div>
                  </template>
                  <template v-else-if="column.key === 'applicant'">
                    <div class="text-sm font-bold text-on-surface">{{ item.applicantName }}</div>
                    <div class="text-[10px] text-on-surface-variant mt-0.5">{{ item.departmentName || '未配置部门' }}</div>
                  </template>
                  <template v-else-if="column.key === 'category'">
                    <el-tag size="small" effect="plain" :class="typeTagClass(item.type)">
                      {{ item.category }}
                    </el-tag>
                  </template>
                  <template v-else-if="column.key === 'summary'">
                    <div class="text-xs font-medium text-on-surface max-w-[320px] truncate" :title="item.summary">{{ item.summary }}</div>
                  </template>
                  <template v-else-if="column.key === 'auditor'">
                    <div class="flex items-center gap-1.5 text-xs font-medium text-on-surface-variant">
                      <span class="material-symbols-outlined text-[14px] opacity-50">person</span>
                      {{ item.auditorName || '待分配' }}
                    </div>
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <el-tag size="small" effect="plain" :class="statusClass(item.status)">
                      {{ item.statusText }}
                    </el-tag>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatTime(item.createTime) }}</template>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="210" align="right">
              <template #default="{ row: item }">
                  <el-button link @click.stop="openDetail(item)">详情</el-button>
                  <template v-if="item.status === 1">
                    <el-button
                      :disabled="!canAuditAction(item)"
                      :title="canAuditAction(item) ? '通过' : '当前账号暂无审批该记录权限'"
                      @click.stop="quickAudit(item, 1)"
                      type="primary"
                      size="small"
                    >通过</el-button>
                    <el-button
                      v-if="item.type !== 'order'"
                      :disabled="!canAuditAction(item)"
                      :title="canAuditAction(item) ? '拒绝' : '当前账号暂无审批该记录权限'"
                      @click.stop="quickAudit(item, 2)"
                      type="danger"
                      plain
                      size="small"
                    >拒绝</el-button>
                  </template>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="activeTabCanViewList ? '当前没有和您相关的审批记录' : '当前账号暂无权限查看该审批列表'" />
            </template>
          </el-table>
          <div v-if="activeTabCanViewList && filteredRows.length" class="flex justify-end p-4">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="filteredRows.length"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px" class="atelier-dialog" destroy-on-close>
      <div v-if="detailData" class="space-y-4">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ detailData.code }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag size="small" effect="plain" :class="statusClass(detailData.status)">{{ detailData.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前审批人">{{ detailData.auditorName || '待分配' }}</el-descriptions-item>
        </el-descriptions>

        <div class="bg-surface-container-lowest rounded-xl p-5 ring-1 ring-outline-variant/20 shadow-sm space-y-4">
          <div class="flex items-start gap-3 border-b border-surface-variant/30 pb-4">
            <span class="material-symbols-outlined text-primary mt-0.5">description</span>
            <div class="flex-1 space-y-2 text-sm">
              <div v-if="detailData.type === 'leave'" class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">请假类型</span><span class="font-bold">{{ detailData.category }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">起止时间</span><span class="font-medium">{{ formatTime(detailData.startTime) }} <span class="mx-1 text-on-surface-variant">至</span> {{ formatTime(detailData.endTime) }}</span></p>
              </div>
              <div v-else-if="detailData.type === 'finance'" class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">财务类别</span><span class="font-bold">{{ detailData.category }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">申请金额</span><span class="font-bold text-rose-600 text-base">￥{{ detailData.amount }}</span></p>
              </div>
              <div v-else-if="detailData.type === 'resignation'" class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">预计离职日期</span><span class="font-bold">{{ detailData.expectedLeaveDate || '--' }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">交接说明</span><span class="font-medium">{{ detailData.handoverNote || '无' }}</span></p>
              </div>
              <div v-else-if="detailData.type === 'quality'" class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">质量类型</span><span class="font-bold">{{ detailData.category }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">关联订单</span><span class="font-medium">{{ detailData.orderId || '未关联' }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">异常数量</span><span class="font-bold">{{ detailData.quantity || '--' }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">损失金额</span><span class="font-bold text-rose-600">￥{{ detailData.lossAmount || 0 }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">负责人员</span><span class="font-medium">{{ detailData.responsiblePerson || '--' }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">处理方式</span><span class="font-medium">{{ detailData.processMethod || '--' }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">处理措施</span><span class="font-medium">{{ detailData.processMeasure || '--' }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">改进方案</span><span class="font-medium">{{ detailData.improvementPlan || '--' }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">处理备注</span><span class="font-medium">{{ detailData.processRemark || '无' }}</span></p>
              </div>
              <div v-else class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">订单类型</span><span class="font-bold">{{ detailData.orderTypeText }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">当前状态</span><span class="font-bold">{{ detailData.statusText }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">客户 / 项目</span><span class="font-medium">{{ detailData.applicantName }} / {{ detailData.category }}</span></p>
              </div>
            </div>
          </div>

          <div class="space-y-1">
            <p class="text-xs font-bold text-on-surface-variant">申请理由</p>
            <p class="text-sm leading-relaxed text-on-surface bg-surface-container-low/50 p-3 rounded-lg">{{ detailData.reason }}</p>
          </div>

          <div v-if="detailData.type === 'finance'" class="space-y-1">
            <p class="text-xs font-bold text-on-surface-variant">附件凭证</p>
            <el-button
              v-if="detailData.attachmentUrl"
              native-type="button"
              link
              @click="openFinanceAttachment(detailData.attachmentUrl, detailData.attachmentName)"
            >
              <span class="material-symbols-outlined text-[18px]">attach_file</span>
              {{ detailData.attachmentName || '下载财务附件' }}
            </el-button>
            <p v-else class="text-sm text-on-surface-variant">暂无附件凭证</p>
          </div>

          <div v-if="detailData.status !== 1" class="space-y-1">
            <p class="text-xs font-bold text-on-surface-variant">历史审批意见</p>
            <p class="text-sm text-on-surface">{{ detailData.auditComment || '无附加意见' }}</p>
          </div>
        </div>

        <el-form v-if="detailData.status === 1" label-position="top" class="pt-2">
          <el-form-item label="您的处理意见（订单审批可选填，拒绝审批时建议填写）">
            <el-input v-model.trim="auditComment" type="textarea" :rows="4" :disabled="!canAuditDetail" placeholder="请输入审批意见..." />
          </el-form-item>
          <div class="flex justify-end gap-3 pt-2">
            <el-button
              v-if="detailData.type !== 'order'"
              :disabled="!canAuditDetail"
              :title="canAuditDetail ? '驳回申请' : '当前账号暂无审批该记录权限'"
              @click="submitAudit(2)"
              type="danger"
              plain
            >驳回申请</el-button>
            <el-button
              :disabled="!canAuditDetail"
              :title="canAuditDetail ? '' : '当前账号暂无审批该记录权限'"
              @click="submitAudit(1)"
              type="primary"
            >{{ detailData.type === 'order' ? orderAuditActionText(detailData) : (detailData.type === 'quality' ? '通过质量审核' : '同意并流转') }}</el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>

    <el-dialog v-model="defaultAuditorDialogVisible" title="审批负责人设置" width="720px" class="atelier-dialog" destroy-on-close>
      <div class="space-y-4 py-2">
        <div class="rounded-2xl bg-primary/5 p-4 text-sm leading-relaxed text-on-surface-variant">
          各审批类型可以设置多名默认负责人。员工提交审批时如未手动指定审批人，系统会自动流转给全部默认负责人；手动指定时优先按指定人流转。
        </div>
        <div v-if="defaultAuditorLoading" class="flex items-center justify-center py-10 text-sm font-bold text-primary">
          <span class="material-symbols-outlined mr-2 animate-spin">progress_activity</span>
          正在读取负责人配置...
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="row in defaultAuditorRows"
            :key="row.approvalType"
            class="grid grid-cols-[150px_minmax(0,1fr)_120px] items-center gap-3 rounded-2xl bg-surface-container-lowest p-4 ring-1 ring-outline-variant/20"
          >
            <div>
              <p class="text-sm font-black text-on-surface">{{ row.approvalTypeText }}</p>
              <p class="mt-1 text-[11px] text-on-surface-variant">{{ row.configured ? `当前：${row.auditorName || '未命名'}` : '暂未配置' }}</p>
            </div>
            <el-select
              v-model="row.auditorIds"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择审批负责人"
              class="w-full"
            >
              <el-option
                v-for="item in defaultAuditorOptions[row.approvalType] || []"
                :key="item.id"
                :label="formatAuditorOption(item)"
                :value="String(item.id)"
              />
            </el-select>
            <el-button
              v-permission="'approval:finance:audit'"
              native-type="button"
              :disabled="defaultAuditorSaving[row.approvalType]"
              :loading="defaultAuditorSaving[row.approvalType]"
              type="primary"
              @click="saveDefaultAuditorRow(row)"
            >
              {{ defaultAuditorSaving[row.approvalType] ? '保存中' : '保存' }}
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="financeDialogVisible" title="新建财务审批" width="560px" class="atelier-dialog" destroy-on-close>
      <el-form :model="financeForm" label-position="top" class="py-2">
        <el-form-item label="财务类别">
          <el-input v-model.trim="financeForm.category" placeholder="如：差旅报销 / 采购付款 / 备用金申请" />
        </el-form-item>
        <el-form-item label="申请金额（元）">
          <el-input-number v-model="financeForm.amount" :min="0" :precision="2" :step="100" class="w-full" />
        </el-form-item>
        <el-form-item label="审批负责人（可手动指定）">
          <el-select
            v-model="financeForm.auditorIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            class="w-full"
            :disabled="auditorLoading.finance"
            :loading="auditorLoading.finance"
            :placeholder="auditorLoading.finance ? '审批人加载中...' : '请选择审批人'"
          >
            <el-option-group
              v-for="group in groupedAuditorOptions.finance"
              :key="group.name"
              :label="group.name"
            >
              <el-option
                v-for="item in group.items"
                :key="item.id"
                :label="formatAuditorOption(item)"
                :value="String(item.id)"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="详细事由">
          <el-input v-model.trim="financeForm.reason" type="textarea" :rows="5" placeholder="请详细说明资金用途、收款方及相关背景..." />
        </el-form-item>
        <el-form-item label="附件凭证">
          <DragAttachmentUpload
            title="上传发票、收据、合同或付款截图"
            helper-text="支持拖拽上传，单个文件不超过 10MB"
            :uploading="financeAttachmentUploading"
            :file-name="financeForm.attachmentName"
            :file-url="financeForm.attachmentUrl"
            :file-size="financeForm.attachmentSize"
            @select="handleFinanceAttachmentFile"
            @download="openFinanceAttachment(financeForm.attachmentUrl, financeForm.attachmentName)"
            @remove="removeFinanceAttachment"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3 pb-1">
          <el-button @click="financeDialogVisible = false">取消</el-button>
          <el-button v-permission="'approval:finance:submit'" type="primary" @click="submitFinance">
            <span class="material-symbols-outlined text-[18px]">send</span>提交申请
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="resignationDialogVisible" title="新建离职申请" width="560px" class="atelier-dialog" destroy-on-close>
      <el-form :model="resignationForm" label-position="top" class="py-2">
        <el-form-item label="预计离职日期">
          <el-date-picker v-model="resignationForm.expectedLeaveDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item label="审批负责人（可手动指定）">
          <el-select
            v-model="resignationForm.auditorIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            class="w-full"
            :disabled="auditorLoading.resignation"
            :loading="auditorLoading.resignation"
            :placeholder="auditorLoading.resignation ? '审批人加载中...' : '请选择审批人'"
          >
            <el-option-group
              v-for="group in groupedAuditorOptions.resignation"
              :key="group.name"
              :label="group.name"
            >
              <el-option
                v-for="item in group.items"
                :key="item.id"
                :label="formatAuditorOption(item)"
                :value="String(item.id)"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="离职原因">
          <el-input v-model.trim="resignationForm.reason" type="textarea" :rows="4" placeholder="请说明离职原因..." />
        </el-form-item>
        <el-form-item label="交接说明">
          <el-input v-model.trim="resignationForm.handoverNote" type="textarea" :rows="4" placeholder="可填写交接事项、资料位置、未完成工作..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3 pb-1">
          <el-button @click="resignationDialogVisible = false">取消</el-button>
          <el-button v-permission="'approval:resignation:submit'" type="primary" @click="submitResignation">
            <span class="material-symbols-outlined text-[18px]">send</span>提交申请
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  ElBadge,
  ElButton,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElOptionGroup,
  ElPagination,
  ElSelect,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag
} from 'element-plus'
import { useUserStore } from '@/stores/user'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import {
  auditFinanceApproval,
  auditOrderApproval,
  auditResignationApproval,
  auditLeaveApproval,
  auditQualityApproval,
  downloadFinanceApprovalAttachment,
  getApprovalSummary,
  getFinanceApprovalDetail,
  getLeaveApprovalDetail,
  getOrderApprovalDetail,
  getQualityApprovalDetail,
  getResignationApprovalDetail,
  listApprovalAuditors,
  listApprovalDefaultAuditors,
  listOrderApprovals,
  listFinanceApprovals,
  listLeaveApprovals,
  listQualityApprovals,
  listResignationApprovals,
  saveApprovalDefaultAuditor,
  submitResignationApproval,
  submitFinanceApproval,
  uploadFinanceApprovalAttachment
} from './api/approval'

const userStore = useUserStore()

const tabs = [
  { label: '订单审批', value: 'order', permissions: ['order:list'], listPermission: 'order:list', auditPermission: 'approval:order:audit' },
  { label: '质量审核', value: 'quality', permissions: ['badproduct:process'], listPermission: 'badproduct:process', auditPermission: 'badproduct:process' },
  { label: '财务审批', value: 'finance', permissions: ['approval:finance', 'approval:finance:submit', 'approval:finance:audit'], listPermission: 'approval:finance', auditPermission: 'approval:finance:audit' },
  { label: '请假审批', value: 'leave', permissions: ['approval:leave'], listPermission: 'approval:leave', auditPermission: 'approval:leave:audit' },
  { label: '离职审批', value: 'resignation', permissions: ['approval:resignation', 'approval:resignation:submit', 'approval:resignation:audit'], listPermission: 'approval:resignation', auditPermission: 'approval:resignation:audit' }
]
const defaultApprovalTableColumns = [
  { key: 'code', label: '单号' },
  { key: 'applicant', label: '申请人' },
  { key: 'category', label: '分类' },
  { key: 'summary', label: '摘要' },
  { key: 'auditor', label: '当前审批人' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '提交时间' }
]
const {
  orderedColumns: approvalTableColumns,
  moveColumn: moveApprovalTableColumn,
  resetColumns: resetApprovalTableColumns
} = useLocalTableColumns('approval.center.list', defaultApprovalTableColumns)
const activeTab = ref('order')
const currentPage = ref(1)
const pageSize = ref(10)
const approvalExportModule = computed(() => `approval-${activeTab.value}`)
const loading = ref(false)
const rows = ref([])
const filters = reactive({ keyword: '', status: '' })
const detailVisible = ref(false)
const detailData = ref(null)
const detailTitle = ref('审批详情')
const auditComment = ref('')
const financeDialogVisible = ref(false)
const resignationDialogVisible = ref(false)
const financeAttachmentUploading = ref(false)
const financeForm = reactive({
  category: '',
  amount: null,
  reason: '',
  auditorId: '',
  auditorIds: [],
  attachmentName: '',
  attachmentUrl: '',
  attachmentSize: null
})
const resignationForm = reactive({
  expectedLeaveDate: '',
  reason: '',
  handoverNote: '',
  auditorId: '',
  auditorIds: []
})
const auditorOptions = reactive({
  finance: [],
  resignation: []
})
const auditorLoading = reactive({
  finance: false,
  resignation: false
})
const defaultAuditorDialogVisible = ref(false)
const defaultAuditorLoading = ref(false)
const defaultAuditorRows = ref([])
const defaultAuditorOptions = reactive({})
const defaultAuditorSaving = reactive({})
const approvalSummary = ref({
  leavePending: 0,
  financePending: 0,
  resignationPending: 0,
  orderPending: 0,
  qualityPending: 0,
  totalPending: 0
})

const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))

const canAccessPermissionSet = (permissions = []) => !permissions?.length || userStore.hasAnyPermission(permissions)
const canAccessTab = (tab) => canAccessPermissionSet(tab?.permissions || [])
const accessibleTabs = computed(() => tabs.filter((tab) => canAccessTab(tab)))
const activeTabMeta = computed(() => tabs.find((tab) => tab.value === activeTab.value) || accessibleTabs.value[0] || tabs[0])
const activeTabCanViewList = computed(() => {
  const permission = activeTabMeta.value?.listPermission
  return !permission || userStore.hasPermission(permission)
})

function ensureActiveTabAccess() {
  if (canAccessTab(activeTabMeta.value)) {
    return true
  }
  const first = accessibleTabs.value[0]
  if (!first) {
    rows.value = []
    return false
  }
  activeTab.value = first.value
  return true
}

function requireUiPermission(permission) {
  if (!permission || userStore.hasPermission(permission)) {
    return true
  }
  ElMessage.warning('当前账号暂无权限')
  return false
}

function tabMetaByValue(value) {
  return tabs.find((tab) => tab.value === value)
}

function auditPermissionForType(type) {
  return tabMetaByValue(type)?.auditPermission || ''
}

function canAuditAction(item) {
  if (!canAuditApproval(item)) {
    return false
  }
  const permission = auditPermissionForType(item?.type)
  return !permission || userStore.hasPermission(permission)
}

const canAuditDetail = computed(() => canAuditAction(detailData.value))

const parseAuditorIds = (value) => (Array.isArray(value) ? value : String(value || '').split(','))
    .map((item) => Number(String(item).trim()))
    .filter((id) => Number.isFinite(id) && id > 0)

const canAuditApproval = (item) => {
  const userId = currentUserId.value
  if (!userId || Number(item?.status) !== 1) return false
  if (typeof item?.canAudit === 'boolean') return item.canAudit
  if (Number(item?.auditorId) === userId) return true
  return parseAuditorIds(item?.auditorIds).includes(userId)
}

const auditButtonDisabledClass = (disabled) => disabled ? 'cursor-not-allowed opacity-50 grayscale' : ''

const groupAuditorOptions = (options = []) => {
  const groups = new Map()
  for (const item of options || []) {
    const name = item.departmentName || '未分配部门'
    if (!groups.has(name)) {
      groups.set(name, [])
    }
    groups.get(name).push(item)
  }
  return Array.from(groups.entries()).map(([name, items]) => ({ name, items }))
}

const groupedAuditorOptions = computed(() => ({
  finance: groupAuditorOptions(auditorOptions.finance),
  resignation: groupAuditorOptions(auditorOptions.resignation)
}))

const formatAuditorOption = (item = {}) => {
  const profile = [item.departmentName, item.positionName].filter(Boolean).join(' / ')
  const suffix = item.defaultAuditor ? ' · 默认' : ''
  return profile ? `${item.name || '未命名'}（${profile}）${suffix}` : `${item.name || '未命名'}${suffix}`
}

const defaultTypeToApiType = (type) => {
  if (type === 'ORDER') return 'order'
  if (type === 'QUALITY') return 'quality'
  if (type === 'FINANCE') return 'finance'
  if (type === 'LEAVE') return 'leave'
  if (type === 'RESIGNATION') return 'resignation'
  return String(type || '').toLowerCase()
}

const loadDefaultAuditorOptions = async (approvalType) => {
  const data = await listApprovalAuditors({ type: defaultTypeToApiType(approvalType), limit: 80 })
  defaultAuditorOptions[approvalType] = Array.isArray(data) ? data : []
}

const loadDefaultAuditors = async () => {
  defaultAuditorLoading.value = true
  try {
    const rows = await listApprovalDefaultAuditors()
    defaultAuditorRows.value = (Array.isArray(rows) ? rows : []).map((row) => ({
      ...row,
      auditorId: row.auditorId ? String(row.auditorId) : '',
      auditorIds: parseAuditorIds(row.auditorIds).length
          ? parseAuditorIds(row.auditorIds).map((id) => String(id))
          : (row.auditorId ? [String(row.auditorId)] : [])
    }))
    await Promise.all(defaultAuditorRows.value.map((row) => loadDefaultAuditorOptions(row.approvalType)))
  } finally {
    defaultAuditorLoading.value = false
  }
}

const openDefaultAuditorDialog = async () => {
  if (!requireUiPermission('approval:finance:audit')) {
    return
  }
  defaultAuditorDialogVisible.value = true
  await loadDefaultAuditors()
}

const saveDefaultAuditorRow = async (row) => {
  if (!requireUiPermission('approval:finance:audit')) {
    return
  }
  const auditorIds = parseAuditorIds(row?.auditorIds)
  if (!row?.approvalType || !auditorIds.length) {
    ElMessage.warning('请选择审批负责人')
    return
  }
  defaultAuditorSaving[row.approvalType] = true
  try {
    await saveApprovalDefaultAuditor({
      approvalType: row.approvalType,
      auditorId: auditorIds[0],
      auditorIds
    })
    ElMessage.success('审批负责人已更新')
    await loadDefaultAuditors()
  } finally {
    defaultAuditorSaving[row.approvalType] = false
  }
}

const loadAuditorOptions = async (type) => {
  if (!['finance', 'resignation'].includes(type)) return
  auditorLoading[type] = true
  try {
    const data = await listApprovalAuditors({ type, limit: 30 })
    auditorOptions[type] = Array.isArray(data) ? data : []
  } catch (error) {
    auditorOptions[type] = []
  } finally {
    auditorLoading[type] = false
  }
}

const stats = computed(() => ({
  pendingForMe: approvalSummary.value.totalPending || rows.value.filter((item) => item.status === 1 && item.canAudit).length,
  mine: rows.value.filter((item) => item.isMine).length,
  approved: rows.value.filter((item) => item.status === 2).length
}))

const filteredRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  const status = filters.status === '' ? null : Number(filters.status)
  return rows.value.filter((item) => {
    const statusMatched = status == null || Number(item.status) === status
    const keywordMatched = !keyword
        || String(item.code || '').toLowerCase().includes(keyword)
        || String(item.applicantName || '').toLowerCase().includes(keyword)
        || String(item.category || '').toLowerCase().includes(keyword)
        || String(item.summary || '').toLowerCase().includes(keyword)
    return statusMatched && keywordMatched
  })
})

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const tabPendingCount = (tab) => {
  if (tab === 'leave') return approvalSummary.value.leavePending || 0
  if (tab === 'finance') return approvalSummary.value.financePending || 0
  if (tab === 'resignation') return approvalSummary.value.resignationPending || 0
  if (tab === 'order') return approvalSummary.value.orderPending || 0
  if (tab === 'quality') return approvalSummary.value.qualityPending || 0
  return 0
}

const fetchSummary = async () => {
  try {
    const data = await getApprovalSummary()
    approvalSummary.value = {
      leavePending: 0,
      financePending: 0,
      resignationPending: 0,
      orderPending: 0,
      qualityPending: 0,
      totalPending: 0,
      ...data
    }
  } catch (error) {
    approvalSummary.value = {
      leavePending: 0,
      financePending: 0,
      resignationPending: 0,
      orderPending: 0,
      qualityPending: 0,
      totalPending: 0
    }
  }
}

const refreshAll = async () => {
  await Promise.all([fetchSummary(), fetchList()])
}

const fetchList = async () => {
  if (!ensureActiveTabAccess()) {
    return
  }
  if (!activeTabCanViewList.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'leave') {
      const data = await listLeaveApprovals()
      rows.value = (data || []).map((item) => ({
        type: 'leave',
        typeLabel: '请假审批',
        code: item.leaveCode,
        applicantName: item.applyUserName,
        departmentName: item.applyDepartmentName,
        category: item.leaveTypeText,
        summary: `${formatTime(item.startTime)} 至 ${formatTime(item.endTime)}`,
        auditorName: item.auditorName,
        auditorId: item.auditorId,
        auditorIds: item.auditorIds,
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: Number(item.applyUserId) === currentUserId.value,
        canAudit: canAuditApproval(item),
        raw: item
      }))
    } else if (activeTab.value === 'finance') {
      const data = await listFinanceApprovals()
      rows.value = (data || []).map((item) => ({
        type: 'finance',
        typeLabel: '财务审批',
        code: item.approvalCode,
        applicantName: item.applyUserName,
        departmentName: item.applyDepartmentName,
        category: item.category,
        summary: `金额 ￥${item.amount} / ${item.reason}`,
        auditorName: item.auditorName,
        auditorId: item.auditorId,
        auditorIds: item.auditorIds,
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: Number(item.applyUserId) === currentUserId.value,
        canAudit: canAuditApproval(item),
        raw: item
      }))
    } else if (activeTab.value === 'resignation') {
      const data = await listResignationApprovals()
      rows.value = (data || []).map((item) => ({
        type: 'resignation',
        typeLabel: '离职审批',
        code: item.resignationCode,
        applicantName: item.applyUserName,
        departmentName: item.applyDepartmentName,
        category: '离职申请',
        summary: `预计离职 ${item.expectedLeaveDate || '--'} / ${item.reason || '无说明'}`,
        auditorName: item.auditorName,
        auditorId: item.auditorId,
        auditorIds: item.auditorIds,
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: Number(item.applyUserId) === currentUserId.value,
        canAudit: canAuditApproval(item),
        raw: item
      }))
    } else if (activeTab.value === 'quality') {
      const data = await listQualityApprovals()
      rows.value = (data || []).map((item) => ({
        type: 'quality',
        typeLabel: '质量审核',
        code: item.defectiveId,
        applicantName: item.applicantName || '质量处理',
        departmentName: item.orderId || '未关联订单',
        category: item.typeText || '质量记录',
        summary: item.summary || item.description || '质量处理审核',
        auditorName: item.auditorName || '质量审核人',
        auditorId: item.auditorId,
        auditorIds: item.auditorIds,
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: false,
        canAudit: item.canAudit !== false,
        raw: item
      }))
    } else {
      const data = await listOrderApprovals()
      rows.value = (data || []).map((item) => ({
        type: 'order',
        typeLabel: item.orderTypeText || '订单审批',
        orderType: item.orderType,
        orderTypeText: item.orderTypeText,
        code: item.orderId,
        applicantName: item.customerName || '未填写客户',
        departmentName: item.projectName || '未填写项目',
        category: item.orderTypeText || '订单确认',
        summary: item.summary || '待确认订单',
        auditorName: item.auditorName || '订单负责人',
        auditorId: item.auditorId,
        status: 1,
        statusText: item.statusText || '待确认',
        createTime: item.createTime,
        isMine: false,
        canAudit: item.canAudit !== false,
        raw: item
      }))
    }
  } finally {
    loading.value = false
  }
}

const changeTab = (tab) => {
  const meta = tabMetaByValue(tab)
  if (!canAccessTab(meta)) {
    ElMessage.warning('当前账号暂无权限')
    return
  }
  activeTab.value = tab
  currentPage.value = 1
  filters.keyword = ''
  filters.status = ''
}

watch(activeTab, fetchList, { immediate: true })
watch([() => filters.keyword, () => filters.status, pageSize], () => {
  currentPage.value = 1
})
fetchSummary()

const openDetail = async (item) => {
  auditComment.value = ''
  if (item.type === 'leave') {
    const detail = await getLeaveApprovalDetail(item.code)
    detailData.value = {
      type: 'leave',
      code: detail.leaveCode,
      applicantName: detail.applyUserName,
      category: leaveTypeText(detail.leaveType),
      startTime: detail.startTime,
      endTime: detail.endTime,
      reason: detail.reason,
      status: detail.status,
      statusText: statusText(detail.status),
      auditorName: detail.auditorName,
      auditorId: detail.auditorId,
      auditorIds: detail.auditorIds,
      auditComment: detail.auditComment,
      canAudit: canAuditApproval(detail)
    }
    detailTitle.value = '请假审批详情'
  } else if (item.type === 'finance') {
    const detail = await getFinanceApprovalDetail(item.code)
    detailData.value = {
      type: 'finance',
      code: detail.approvalCode,
      applicantName: detail.applyUserName,
      category: detail.category,
      amount: detail.amount,
      reason: detail.reason,
      status: detail.status,
      statusText: detail.statusText,
      auditorName: detail.auditorName,
      auditorId: detail.auditorId,
      auditorIds: detail.auditorIds,
      auditComment: detail.auditComment,
      attachmentName: detail.attachmentName,
      attachmentUrl: detail.attachmentUrl,
      attachmentSize: detail.attachmentSize,
      canAudit: canAuditApproval(detail)
    }
    detailTitle.value = '财务审批详情'
  } else if (item.type === 'resignation') {
    const detail = await getResignationApprovalDetail(item.code)
    detailData.value = {
      type: 'resignation',
      code: detail.resignationCode,
      applicantName: detail.applyUserName,
      category: '离职申请',
      expectedLeaveDate: detail.expectedLeaveDate,
      handoverNote: detail.handoverNote,
      reason: detail.reason,
      status: detail.status,
      statusText: detail.statusText,
      auditorName: detail.auditorName,
      auditorId: detail.auditorId,
      auditorIds: detail.auditorIds,
      auditComment: detail.auditComment,
      canAudit: canAuditApproval(detail)
    }
    detailTitle.value = '离职审批详情'
  } else if (item.type === 'quality') {
    const detail = await getQualityApprovalDetail(item.code)
    detailData.value = {
      type: 'quality',
      code: detail.defectiveId,
      applicantName: detail.applicantName || '质量处理',
      category: detail.typeText || '质量记录',
      orderId: detail.orderId,
      quantity: detail.quantity,
      lossAmount: detail.lossAmount,
      reason: detail.description || detail.summary || '质量处理审核',
      responsiblePerson: detail.responsiblePerson,
      processMethod: detail.processMethod,
      processMeasure: detail.processMeasure,
      improvementPlan: detail.improvementPlan,
      processRemark: detail.processRemark,
      status: detail.status,
      statusText: detail.statusText,
      auditorName: detail.auditorName,
      auditorId: detail.auditorId,
      auditorIds: detail.auditorIds,
      canAudit: detail.canAudit !== false
    }
    detailTitle.value = '质量审核详情'
  } else {
    const detail = await getOrderApprovalDetail(item.orderType || item.raw?.orderType, item.code)
    detailData.value = {
      type: 'order',
      code: detail.orderId,
      orderType: detail.orderType,
      orderTypeText: detail.orderTypeText || '订单',
      applicantName: detail.customerName || '未填写客户',
      category: detail.projectName || '未填写项目',
      reason: detail.summary || '待确认订单',
      status: 1,
      statusText: detail.statusText || '待确认',
      auditorName: detail.auditorName || '订单负责人',
      auditorId: detail.auditorId,
      canAudit: detail.canAudit !== false
    }
    detailTitle.value = '订单审批详情'
  }
  detailVisible.value = true
}

const quickAudit = async (item, action) => {
  if (!canAuditAction(item)) {
    ElMessage.warning('当前账号暂无审批该记录权限')
    return
  }
  if (item.type === 'leave') {
    await auditLeaveApproval({
      leaveCode: item.code,
      action,
      comment: action === 2 ? '审批中心快捷处理' : ''
    })
  } else if (item.type === 'finance') {
    await auditFinanceApproval({
      approvalCode: item.code,
      action,
      comment: action === 2 ? '审批中心快捷处理' : ''
    })
  } else if (item.type === 'resignation') {
    await auditResignationApproval({
      resignationCode: item.code,
      action,
      comment: action === 2 ? '审批中心快捷处理' : ''
    })
  } else if (item.type === 'quality') {
    await auditQualityApproval({
      defectiveId: item.code,
      action,
      comment: action === 2 ? '审批中心快捷驳回' : ''
    })
  } else {
    await auditOrderApproval({
      orderType: item.orderType || item.raw?.orderType,
      orderId: item.code,
      action,
      comment: action === 1 ? orderAuditActionText(item) : ''
    })
  }
  ElMessage.success(item.type === 'order' ? orderAuditSuccessText(item) : (item.type === 'quality' ? (action === 1 ? '质量审核已通过' : '质量审核已驳回') : (action === 1 ? '审批已通过' : '审批已拒绝')))
  refreshAll()
}

const submitAudit = async (action) => {
  if (!detailData.value) return
  if (!canAuditDetail.value) {
    ElMessage.warning('当前账号暂无审批该记录权限')
    return
  }
  if (detailData.value.type === 'leave') {
    await auditLeaveApproval({
      leaveCode: detailData.value.code,
      action,
      comment: auditComment.value
    })
  } else if (detailData.value.type === 'finance') {
    await auditFinanceApproval({
      approvalCode: detailData.value.code,
      action,
      comment: auditComment.value
    })
  } else if (detailData.value.type === 'resignation') {
    await auditResignationApproval({
      resignationCode: detailData.value.code,
      action,
      comment: auditComment.value
    })
  } else if (detailData.value.type === 'quality') {
    await auditQualityApproval({
      defectiveId: detailData.value.code,
      action,
      comment: auditComment.value
    })
  } else {
    await auditOrderApproval({
      orderType: detailData.value.orderType,
      orderId: detailData.value.code,
      action,
      comment: auditComment.value
    })
  }
  ElMessage.success(detailData.value.type === 'order' ? orderAuditSuccessText(detailData.value) : (detailData.value.type === 'quality' ? (action === 1 ? '质量审核已通过' : '质量审核已驳回') : (action === 1 ? '审批已提交' : '已驳回申请')))
  detailVisible.value = false
  refreshAll()
}

const orderAuditActionText = (item) => {
  const text = item?.statusText || item?.raw?.statusText || ''
  return text.includes('生产') ? '同意转生产中' : '确认订单'
}

const orderAuditSuccessText = (item) => {
  const text = item?.statusText || item?.raw?.statusText || ''
  return text.includes('生产') || text.includes('履约') ? '已通过流转审批' : '订单已确认'
}

const resetFinanceForm = () => {
  financeForm.category = ''
  financeForm.amount = null
  financeForm.reason = ''
  financeForm.auditorId = ''
  financeForm.auditorIds = []
  financeForm.attachmentName = ''
  financeForm.attachmentUrl = ''
  financeForm.attachmentSize = null
}

const resetResignationForm = () => {
  resignationForm.expectedLeaveDate = ''
  resignationForm.reason = ''
  resignationForm.handoverNote = ''
  resignationForm.auditorId = ''
  resignationForm.auditorIds = []
}

const openFinanceDialog = async () => {
  if (!requireUiPermission('approval:finance:submit')) {
    return
  }
  resetFinanceForm()
  financeDialogVisible.value = true
  await loadAuditorOptions('finance')
}

const openResignationDialog = async () => {
  if (!requireUiPermission('approval:resignation:submit')) {
    return
  }
  resetResignationForm()
  resignationDialogVisible.value = true
  await loadAuditorOptions('resignation')
}

async function handleFinanceAttachmentFile(file) {
  if (!requireUiPermission('approval:finance:submit')) {
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
  financeAttachmentUploading.value = true
  try {
    const result = await uploadFinanceApprovalAttachment(formData)
    financeForm.attachmentName = result.fileName || file.name
    financeForm.attachmentUrl = result.fileUrl || ''
    financeForm.attachmentSize = result.fileSize || file.size
    ElMessage.success('附件上传成功')
  } finally {
    financeAttachmentUploading.value = false
  }
}

function removeFinanceAttachment() {
  financeForm.attachmentName = ''
  financeForm.attachmentUrl = ''
  financeForm.attachmentSize = null
}

async function openFinanceAttachment(url, name) {
  if (!url) {
    return
  }
  const blob = await downloadFinanceApprovalAttachment({ url, name })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name || 'finance-attachment'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

const submitFinance = async () => {
  if (!requireUiPermission('approval:finance:submit')) {
    return
  }
  if (!financeForm.category || !financeForm.amount || !financeForm.reason) {
    ElMessage.warning('请完整填写财务类别、金额和申请理由')
    return
  }
  await submitFinanceApproval({
    category: financeForm.category,
    amount: Number(financeForm.amount),
    reason: financeForm.reason,
    auditorId: financeForm.auditorIds?.length ? Number(financeForm.auditorIds[0]) : undefined,
    auditorIds: (financeForm.auditorIds || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0),
    attachmentName: financeForm.attachmentName || undefined,
    attachmentUrl: financeForm.attachmentUrl || undefined,
    attachmentSize: financeForm.attachmentSize || undefined
  })
  ElMessage.success('财务审批申请已提交')
  resetFinanceForm()
  financeDialogVisible.value = false
  activeTab.value = 'finance'
  fetchList()
}

const submitResignation = async () => {
  if (!requireUiPermission('approval:resignation:submit')) {
    return
  }
  if (!resignationForm.expectedLeaveDate || !resignationForm.reason) {
    ElMessage.warning('请填写预计离职日期和离职原因')
    return
  }
  await submitResignationApproval({
    expectedLeaveDate: resignationForm.expectedLeaveDate,
    reason: resignationForm.reason,
    handoverNote: resignationForm.handoverNote,
    auditorId: resignationForm.auditorIds?.length ? Number(resignationForm.auditorIds[0]) : undefined,
    auditorIds: (resignationForm.auditorIds || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0)
  })
  ElMessage.success('离职申请已提交')
  resetResignationForm()
  resignationDialogVisible.value = false
  activeTab.value = 'resignation'
  fetchList()
}

const statusClass = (status) => {
  if (status === 1) return 'bg-amber-50 text-amber-700 border-amber-100'
  if (status === 2) return 'bg-emerald-50 text-emerald-700 border-emerald-100'
  if (status === 3) return 'bg-rose-50 text-rose-700 border-rose-100'
  return 'bg-surface-container-low text-on-surface-variant border-outline-variant/20'
}

const typeTagClass = (type) => {
  if (type === 'leave') return 'bg-indigo-50 text-indigo-700 border-indigo-100'
  if (type === 'finance') return 'bg-sky-50 text-sky-700 border-sky-100'
  if (type === 'order') return 'bg-amber-50 text-amber-700 border-amber-100'
  if (type === 'quality') return 'bg-emerald-50 text-emerald-700 border-emerald-100'
  return 'bg-rose-50 text-rose-700 border-rose-100'
}

const formatTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

const formatFileSize = (value) => {
  const size = Number(value || 0)
  if (!Number.isFinite(size) || size <= 0) return '--'
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

const leaveTypeText = (leaveType) => {
  if (leaveType === 1) return '事假'
  if (leaveType === 2) return '病假'
  if (leaveType === 3) return '年假'
  if (leaveType === 4) return '调休'
  return '其他'
}

const statusText = (status) => {
  if (status === 1) return '待审批'
  if (status === 2) return '已通过'
  if (status === 3) return '已拒绝'
  return '未知'
}
</script>
