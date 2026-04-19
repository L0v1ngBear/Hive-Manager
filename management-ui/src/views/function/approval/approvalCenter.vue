<template>
  <div class="h-full min-h-0 bg-surface text-on-surface overflow-x-hidden font-body">
    <div class="max-w-7xl mx-auto space-y-6">
      <header class="flex flex-col lg:flex-row lg:items-end justify-between gap-6">
        <div class="flex items-start gap-4">
          <div class="w-12 h-12 rounded-2xl bg-primary flex items-center justify-center text-white shadow-lg shadow-primary/20 shrink-0">
            <span class="material-symbols-outlined text-2xl">rule_folder</span>
          </div>
          <div>
            <h1 class="text-2xl md:text-3xl font-black tracking-tight text-on-surface">审批中心</h1>
            <p class="text-sm text-on-surface-variant mt-1 max-w-2xl">
              这里只展示和您本人相关的审批请求，包括您发起的申请，以及当前流转到您这里的待审批事项。
            </p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button
            @click="fetchList"
            class="px-4 py-2.5 bg-surface-container-lowest text-on-surface-variant font-bold rounded-xl hover:text-primary hover:bg-surface-container-low transition-all text-sm ring-1 ring-outline-variant/20 shadow-sm flex items-center gap-1.5 active:scale-95"
          >
            <span class="material-symbols-outlined text-[18px]">refresh</span>刷新
          </button>
          <button
            v-if="activeTab === 'finance'"
            @click="financeDialogVisible = true"
            class="px-5 py-2.5 bg-primary text-white font-bold rounded-xl hover:bg-primary/90 transition-all text-sm shadow-md shadow-primary/20 flex items-center gap-1.5 active:scale-95"
          >
            <span class="material-symbols-outlined text-[18px]">add_circle</span>新建财务审批
          </button>
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
            <h3 class="text-3xl font-black text-on-surface">{{ stats.pendingForMe }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">当前流转到您这里的审批单</p>
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
            <h3 class="text-3xl font-black text-primary">{{ rows.length }}</h3>
            <p class="text-[10px] text-on-surface-variant mt-1">与您本人相关的审批请求</p>
          </div>
        </article>
      </section>

      <section class="bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/15 flex flex-col">
        <div class="p-4 md:p-5 border-b border-surface-variant/40 flex items-center justify-between gap-4">
          <div class="inline-flex p-1 bg-surface-container-low rounded-xl ring-1 ring-outline-variant/20 self-start">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              @click="changeTab(tab.value)"
              class="px-5 py-2 rounded-lg text-sm font-bold transition-all"
              :class="activeTab === tab.value ? 'bg-white text-primary shadow-sm ring-1 ring-outline-variant/10' : 'text-on-surface-variant hover:text-on-surface'"
            >
              {{ tab.label }}
            </button>
          </div>
          <p class="text-xs font-bold text-on-surface-variant">列表已按“与我相关”自动过滤</p>
        </div>

        <div class="overflow-x-auto relative min-h-[300px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex flex-col items-center justify-center">
            <span class="material-symbols-outlined text-primary text-4xl animate-spin">progress_activity</span>
            <span class="text-xs font-bold text-primary mt-2">加载数据中...</span>
          </div>

          <table class="w-full text-left border-collapse min-w-[1080px]">
            <thead class="bg-surface-container-low/30 border-b border-surface-variant/40">
              <tr>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">单号</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">申请人</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">分类</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest min-w-[220px]">摘要</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">当前审批人</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">状态</th>
                <th class="px-5 py-4 text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">提交时间</th>
                <th class="px-5 py-4 text-right text-[11px] font-bold text-on-surface-variant uppercase tracking-widest whitespace-nowrap">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/20">
              <tr v-for="item in rows" :key="item.code" class="cursor-pointer hover:bg-surface-container-low/50 transition-colors group" @click="openDetail(item)">
                <td class="px-5 py-3.5">
                  <div class="text-sm font-black text-on-surface group-hover:text-primary transition-colors">{{ item.code }}</div>
                  <div class="text-[10px] font-medium text-on-surface-variant mt-0.5">{{ item.typeLabel }}</div>
                </td>
                <td class="px-5 py-3.5">
                  <div class="text-sm font-bold text-on-surface">{{ item.applicantName }}</div>
                  <div class="text-[10px] text-on-surface-variant mt-0.5">{{ item.departmentName || '未配置部门' }}</div>
                </td>
                <td class="px-5 py-3.5">
                  <span
                    class="inline-flex px-2 py-0.5 rounded text-[11px] font-bold border"
                    :class="item.type === 'leave' ? 'bg-indigo-50 text-indigo-700 border-indigo-100' : 'bg-sky-50 text-sky-700 border-sky-100'"
                  >
                    {{ item.category }}
                  </span>
                </td>
                <td class="px-5 py-3.5">
                  <div class="text-xs font-medium text-on-surface max-w-[320px] truncate" :title="item.summary">{{ item.summary }}</div>
                </td>
                <td class="px-5 py-3.5">
                  <div class="flex items-center gap-1.5 text-xs font-medium text-on-surface-variant">
                    <span class="material-symbols-outlined text-[14px] opacity-50">person</span>
                    {{ item.auditorName || '待分配' }}
                  </div>
                </td>
                <td class="px-5 py-3.5">
                  <span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[11px] font-bold border">
                    {{ item.statusText }}
                  </span>
                </td>
                <td class="px-5 py-3.5 text-xs font-medium text-on-surface-variant">{{ formatTime(item.createTime) }}</td>
                <td class="px-5 py-3.5 text-right space-x-2">
                  <button @click.stop="openDetail(item)" class="text-on-surface-variant hover:text-primary hover:bg-primary/10 px-2.5 py-1.5 rounded-lg text-xs font-bold transition-colors">详情</button>
                  <template v-if="item.status === 1 && item.canAudit">
                    <button @click.stop="quickAudit(item, 1)" class="text-white bg-primary hover:bg-primary/90 shadow-sm shadow-primary/20 px-3 py-1.5 rounded-lg text-xs font-bold transition-all active:scale-95">通过</button>
                    <button @click.stop="quickAudit(item, 2)" class="text-rose-600 bg-rose-50 hover:bg-rose-100 border border-rose-100 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors active:scale-95">拒绝</button>
                  </template>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td colspan="8" class="px-6 py-16 text-center">
                  <div class="flex flex-col items-center justify-center opacity-50">
                    <span class="material-symbols-outlined text-5xl mb-3">inbox</span>
                    <p class="text-sm font-bold text-on-surface-variant">当前没有和您相关的审批记录</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px" class="atelier-dialog" destroy-on-close>
      <div v-if="detailData" class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div class="bg-surface-container-low rounded-xl p-4 ring-1 ring-outline-variant/10">
            <p class="text-[10px] font-bold tracking-widest uppercase text-on-surface-variant mb-1">申请单号</p>
            <p class="font-black text-sm text-primary">{{ detailData.code }}</p>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4 ring-1 ring-outline-variant/10">
            <p class="text-[10px] font-bold tracking-widest uppercase text-on-surface-variant mb-1">申请人</p>
            <p class="font-bold text-sm text-on-surface">{{ detailData.applicantName }}</p>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4 ring-1 ring-outline-variant/10">
            <p class="text-[10px] font-bold tracking-widest uppercase text-on-surface-variant mb-1">当前状态</p>
            <span :class="statusClass(detailData.status)" class="inline-flex px-2 py-0.5 rounded text-[11px] font-bold border mt-0.5">
              {{ detailData.statusText }}
            </span>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4 ring-1 ring-outline-variant/10">
            <p class="text-[10px] font-bold tracking-widest uppercase text-on-surface-variant mb-1">当前审批人</p>
            <div class="flex items-center gap-1.5 text-sm font-bold text-on-surface">
              <span class="material-symbols-outlined text-[16px] text-on-surface-variant">account_circle</span>
              {{ detailData.auditorName || '待分配' }}
            </div>
          </div>
        </div>

        <div class="bg-surface-container-lowest rounded-xl p-5 ring-1 ring-outline-variant/20 shadow-sm space-y-4">
          <div class="flex items-start gap-3 border-b border-surface-variant/30 pb-4">
            <span class="material-symbols-outlined text-primary mt-0.5">description</span>
            <div class="flex-1 space-y-2 text-sm">
              <div v-if="detailData.type === 'leave'" class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">请假类型</span><span class="font-bold">{{ detailData.category }}</span></p>
                <p class="col-span-2"><span class="text-on-surface-variant text-xs mr-2">起止时间</span><span class="font-medium">{{ formatTime(detailData.startTime) }} <span class="mx-1 text-on-surface-variant">至</span> {{ formatTime(detailData.endTime) }}</span></p>
              </div>
              <div v-else class="grid grid-cols-2 gap-4">
                <p><span class="text-on-surface-variant text-xs mr-2">财务类别</span><span class="font-bold">{{ detailData.category }}</span></p>
                <p><span class="text-on-surface-variant text-xs mr-2">申请金额</span><span class="font-bold text-rose-600 text-base">￥{{ detailData.amount }}</span></p>
              </div>
            </div>
          </div>

          <div class="space-y-1">
            <p class="text-xs font-bold text-on-surface-variant">申请理由</p>
            <p class="text-sm leading-relaxed text-on-surface bg-surface-container-low/50 p-3 rounded-lg">{{ detailData.reason }}</p>
          </div>

          <div v-if="detailData.status !== 1" class="space-y-1">
            <p class="text-xs font-bold text-on-surface-variant">历史审批意见</p>
            <p class="text-sm text-on-surface">{{ detailData.auditComment || '无附加意见' }}</p>
          </div>
        </div>

        <div v-if="detailData.status === 1 && detailData.canAudit" class="pt-2 space-y-3">
          <p class="text-xs font-bold text-on-surface">您的审批意见 <span class="font-normal text-on-surface-variant ml-1">(拒绝时建议填写)</span></p>
          <textarea v-model.trim="auditComment" class="w-full min-h-[100px] bg-surface-container-lowest rounded-xl ring-1 ring-outline-variant/30 p-3 text-sm outline-none focus:ring-2 focus:ring-primary transition-shadow" placeholder="请输入审批意见..."></textarea>
          <div class="flex justify-end gap-3 pt-2">
            <button @click="submitAudit(2)" class="px-5 py-2.5 rounded-xl text-sm font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 transition-colors">驳回申请</button>
            <button @click="submitAudit(1)" class="px-6 py-2.5 rounded-xl text-sm font-bold bg-primary text-white shadow-md shadow-primary/20 hover:bg-primary/90 transition-all active:scale-95">同意并流转</button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="financeDialogVisible" title="新建财务审批" width="560px" class="atelier-dialog" destroy-on-close>
      <div class="space-y-4 py-2">
        <div class="space-y-1.5">
          <label class="text-xs font-bold text-on-surface-variant pl-1">财务类别</label>
          <input v-model.trim="financeForm.category" class="w-full h-11 px-4 bg-surface-container-lowest rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary transition-shadow text-sm" placeholder="如：差旅报销 / 采购付款 / 备用金申请" />
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-bold text-on-surface-variant pl-1">申请金额（元）</label>
          <input v-model.trim="financeForm.amount" type="number" class="w-full h-11 px-4 bg-surface-container-lowest rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary transition-shadow text-sm font-mono" placeholder="0.00" />
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-bold text-on-surface-variant pl-1">详细事由</label>
          <textarea v-model.trim="financeForm.reason" class="w-full min-h-[120px] p-4 bg-surface-container-lowest rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary transition-shadow text-sm leading-relaxed" placeholder="请详细说明资金用途、收款方及相关背景..." />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 pb-1">
          <button @click="financeDialogVisible = false" class="px-5 py-2.5 rounded-xl text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-colors">取消</button>
          <button @click="submitFinance" class="px-6 py-2.5 rounded-xl text-sm font-bold bg-primary text-white shadow-md shadow-primary/20 hover:bg-primary/90 transition-all active:scale-95 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[18px]">send</span>提交申请
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElDialog } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  auditFinanceApproval,
  auditLeaveApproval,
  getFinanceApprovalDetail,
  getLeaveApprovalDetail,
  listFinanceApprovals,
  listLeaveApprovals,
  submitFinanceApproval
} from './api/approval'

const userStore = useUserStore()

const tabs = [
  { label: '请假审批', value: 'leave' },
  { label: '财务审批', value: 'finance' }
]

const activeTab = ref('leave')
const loading = ref(false)
const rows = ref([])
const detailVisible = ref(false)
const detailData = ref(null)
const detailTitle = ref('审批详情')
const auditComment = ref('')
const financeDialogVisible = ref(false)
const financeForm = reactive({
  category: '',
  amount: '',
  reason: ''
})

const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))

const stats = computed(() => ({
  pendingForMe: rows.value.filter((item) => item.status === 1 && item.canAudit).length,
  mine: rows.value.filter((item) => item.isMine).length,
  approved: rows.value.filter((item) => item.status === 2).length
}))

const fetchList = async () => {
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
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: Number(item.applyUserId) === currentUserId.value,
        canAudit: Number(item.auditorId) === currentUserId.value,
        raw: item
      }))
    } else {
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
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        isMine: Number(item.applyUserId) === currentUserId.value,
        canAudit: Number(item.auditorId) === currentUserId.value,
        raw: item
      }))
    }
  } finally {
    loading.value = false
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
}

watch(activeTab, fetchList, { immediate: true })

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
      auditComment: detail.auditComment,
      canAudit: Number(detail.auditorId) === currentUserId.value
    }
    detailTitle.value = '请假审批详情'
  } else {
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
      auditComment: detail.auditComment,
      canAudit: Number(detail.auditorId) === currentUserId.value
    }
    detailTitle.value = '财务审批详情'
  }
  detailVisible.value = true
}

const quickAudit = async (item, action) => {
  if (item.type === 'leave') {
    await auditLeaveApproval({
      leaveCode: item.code,
      action,
      comment: action === 2 ? '审批中心快捷处理' : ''
    })
  } else {
    await auditFinanceApproval({
      approvalCode: item.code,
      action,
      comment: action === 2 ? '审批中心快捷处理' : ''
    })
  }
  ElMessage.success(action === 1 ? '审批已通过' : '审批已拒绝')
  fetchList()
}

const submitAudit = async (action) => {
  if (!detailData.value) return
  if (detailData.value.type === 'leave') {
    await auditLeaveApproval({
      leaveCode: detailData.value.code,
      action,
      comment: auditComment.value
    })
  } else {
    await auditFinanceApproval({
      approvalCode: detailData.value.code,
      action,
      comment: auditComment.value
    })
  }
  ElMessage.success(action === 1 ? '审批已提交' : '已驳回申请')
  detailVisible.value = false
  fetchList()
}

const submitFinance = async () => {
  if (!financeForm.category || !financeForm.amount || !financeForm.reason) {
    ElMessage.warning('请完整填写财务类别、金额和申请理由')
    return
  }
  await submitFinanceApproval({
    category: financeForm.category,
    amount: Number(financeForm.amount),
    reason: financeForm.reason
  })
  ElMessage.success('财务审批申请已提交')
  financeForm.category = ''
  financeForm.amount = ''
  financeForm.reason = ''
  financeDialogVisible.value = false
  activeTab.value = 'finance'
  fetchList()
}

const statusClass = (status) => {
  if (status === 1) return 'bg-amber-50 text-amber-700 border-amber-100'
  if (status === 2) return 'bg-emerald-50 text-emerald-700 border-emerald-100'
  if (status === 3) return 'bg-rose-50 text-rose-700 border-rose-100'
  return 'bg-surface-container-low text-on-surface-variant border-outline-variant/20'
}

const formatTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
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
