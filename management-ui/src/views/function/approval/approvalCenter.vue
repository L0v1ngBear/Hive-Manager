<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden font-body">
    <div class="max-w-7xl mx-auto space-y-8">
      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">审批中心</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-2xl">统一处理请假审批和财务审批，支持查看详情、审批流转和快速发起财务申请。</p>
        </div>
        <div class="flex items-center gap-3">
          <button v-if="activeTab === 'finance'" @click="financeDialogVisible = true" class="px-5 py-2 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 transition-colors text-sm shadow-md active:scale-95">
            <span class="material-symbols-outlined text-lg align-middle mr-1">add_circle</span>新建财务申请
          </button>
          <button @click="fetchList" class="px-4 py-2 bg-surface-container-highest text-primary font-bold rounded-lg hover:bg-surface-container-high transition-colors text-sm">
            <span class="material-symbols-outlined text-lg align-middle mr-1">refresh</span>刷新
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前待处理</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.pending }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">本页总数</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ rows.length }}</h3>
        </div>
        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-xs font-bold text-on-surface-variant uppercase tracking-widest">已通过</p>
          <h3 class="text-4xl font-black text-primary mt-2">{{ stats.approved }}</h3>
        </div>
        <div class="bg-[#1a365d] text-white p-6 rounded-xl shadow-md">
          <p class="text-xs font-bold uppercase tracking-widest opacity-80">已拒绝</p>
          <h3 class="text-4xl font-black mt-2">{{ stats.rejected }}</h3>
          <p class="text-xs opacity-70 mt-3">可通过筛选查看历史处理记录。</p>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden ring-1 ring-outline-variant/20">
        <div class="px-6 py-4 border-b border-surface-variant/50 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div class="flex flex-wrap items-center gap-3">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              @click="changeTab(tab.value)"
              class="px-4 py-2 rounded-lg text-sm font-bold transition-colors"
              :class="activeTab === tab.value ? 'bg-primary text-white' : 'bg-surface-container-high text-on-surface-variant hover:text-primary'"
            >
              {{ tab.label }}
            </button>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <select v-model="scope" @change="fetchList" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="pending">全部待我审批</option>
              <option value="others_pending">别人提交给我</option>
              <option value="self_pending">我自己发起且待我审批</option>
              <option value="mine">我发起的</option>
              <option value="all">全部记录</option>
            </select>
            <select v-model="statusFilter" @change="fetchList" class="px-3 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 text-sm outline-none">
              <option value="">全部状态</option>
              <option value="1">待审批</option>
              <option value="2">已通过</option>
              <option value="3">已拒绝</option>
            </select>
            <div class="relative min-w-[260px]">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
              <input v-model.trim="keyword" class="w-full pl-10 pr-4 py-2 bg-white rounded-lg ring-1 ring-outline-variant/30 focus:ring-2 focus:ring-primary text-sm outline-none" placeholder="搜索申请人、单号、事由" />
            </div>
          </div>
        </div>

        <div class="overflow-x-auto relative min-h-[260px]">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
          </div>
          <table class="w-full text-left border-collapse min-w-[1080px]">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">单号</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">申请人</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">审批类型</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">内容摘要</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">当前审批人</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">状态</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">提交时间</th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/30">
              <tr v-for="item in filteredRows" :key="item.code" class="hover:bg-surface-container-high/40 transition-colors">
                <td class="px-6 py-4">
                  <div class="text-sm font-bold text-primary">{{ item.code }}</div>
                  <div class="text-[10px] text-on-surface-variant">{{ item.typeLabel }}</div>
                </td>
                <td class="px-6 py-4">
                  <div class="text-sm font-bold">{{ item.applicantName }}</div>
                  <div class="text-xs text-on-surface-variant">{{ item.departmentName || '未配置部门' }}</div>
                </td>
                <td class="px-6 py-4">
                  <span class="inline-flex px-2 py-1 rounded-full text-[10px] font-bold" :class="item.type === 'leave' ? 'bg-primary/10 text-primary' : 'bg-secondary/10 text-secondary'">{{ item.category }}</span>
                </td>
                <td class="px-6 py-4">
                  <div class="text-sm font-medium text-on-surface max-w-[260px] truncate">{{ item.summary }}</div>
                </td>
                <td class="px-6 py-4 text-sm text-on-surface-variant">{{ item.auditorName || '待分配' }}</td>
                <td class="px-6 py-4"><span :class="statusClass(item.status)" class="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold">{{ item.statusText }}</span></td>
                <td class="px-6 py-4 text-xs text-on-surface-variant">{{ formatTime(item.createTime) }}</td>
                <td class="px-6 py-4 text-right space-x-2">
                  <button @click="openDetail(item)" class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-xs font-bold">详情</button>
                  <button v-if="item.status === 1 && scope !== 'mine'" @click="quickAudit(item, 1)" class="text-white bg-primary hover:bg-primary/90 px-3 py-1.5 rounded-lg text-xs font-bold">通过</button>
                  <button v-if="item.status === 1 && scope !== 'mine'" @click="quickAudit(item, 2)" class="text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-lg text-xs font-bold">拒绝</button>
                </td>
              </tr>
              <tr v-if="!loading && filteredRows.length === 0">
                <td colspan="8" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无审批记录。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="680px" destroy-on-close>
      <div v-if="detailData" class="space-y-5">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div class="bg-surface-container-low rounded-xl p-4">
            <p class="text-xs text-on-surface-variant mb-2">申请单号</p>
            <p class="font-bold text-primary">{{ detailData.code }}</p>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4">
            <p class="text-xs text-on-surface-variant mb-2">申请人</p>
            <p class="font-bold">{{ detailData.applicantName }}</p>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4">
            <p class="text-xs text-on-surface-variant mb-2">当前状态</p>
            <p class="font-bold">{{ detailData.statusText }}</p>
          </div>
          <div class="bg-surface-container-low rounded-xl p-4">
            <p class="text-xs text-on-surface-variant mb-2">当前审批人</p>
            <p class="font-bold">{{ detailData.auditorName || '待分配' }}</p>
          </div>
        </div>

        <div class="bg-surface-container-low rounded-xl p-5 text-sm space-y-3">
          <div v-if="detailData.type === 'leave'">
            <p><span class="text-on-surface-variant">请假类型：</span>{{ detailData.category }}</p>
            <p><span class="text-on-surface-variant">开始时间：</span>{{ formatTime(detailData.startTime) }}</p>
            <p><span class="text-on-surface-variant">结束时间：</span>{{ formatTime(detailData.endTime) }}</p>
          </div>
          <div v-else>
            <p><span class="text-on-surface-variant">财务类别：</span>{{ detailData.category }}</p>
            <p><span class="text-on-surface-variant">申请金额：</span>¥{{ detailData.amount }}</p>
          </div>
          <p><span class="text-on-surface-variant">申请内容：</span>{{ detailData.reason }}</p>
          <p><span class="text-on-surface-variant">审批意见：</span>{{ detailData.auditComment || '暂无' }}</p>
        </div>

        <div v-if="detailData.status === 1 && scope !== 'mine'" class="space-y-3">
          <textarea v-model.trim="auditComment" class="w-full min-h-[100px] bg-white rounded-xl ring-1 ring-outline-variant/30 p-4 text-sm outline-none focus:ring-2 focus:ring-primary" placeholder="填写审批意见（可选，拒绝时建议填写）"></textarea>
          <div class="flex justify-end gap-3">
            <button @click="submitAudit(2)" class="px-4 py-2 rounded-lg text-sm font-bold text-red-600 hover:bg-red-50">拒绝</button>
            <button @click="submitAudit(1)" class="px-5 py-2 rounded-lg text-sm font-bold bg-primary text-white hover:bg-primary/90">审批通过</button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="financeDialogVisible" title="新建财务申请" width="560px" destroy-on-close>
      <div class="space-y-4">
        <input v-model.trim="financeForm.category" class="w-full px-4 py-3 bg-white rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="财务类别，例如：差旅报销 / 采购付款" />
        <input v-model.trim="financeForm.amount" class="w-full px-4 py-3 bg-white rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="申请金额" />
        <textarea v-model.trim="financeForm.reason" class="w-full min-h-[120px] px-4 py-3 bg-white rounded-xl ring-1 ring-outline-variant/30 outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="填写申请事由"></textarea>
      </div>
      <template #footer>
        <button @click="financeDialogVisible = false" class="px-4 py-2 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high">取消</button>
        <button @click="submitFinance" class="px-5 py-2 rounded-lg text-sm font-bold bg-primary text-white hover:bg-primary/90">提交申请</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  auditFinanceApproval,
  auditLeaveApproval,
  getFinanceApprovalDetail,
  getLeaveApprovalDetail,
  listFinanceApprovals,
  listLeaveApprovals,
  submitFinanceApproval,
} from './api/approval'

const tabs = [
  { label: '请假审批', value: 'leave' },
  { label: '财务审批', value: 'finance' },
]

const activeTab = ref('leave')
const scope = ref('pending')
const statusFilter = ref('')
const keyword = ref('')
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
  reason: '',
})

const filteredRows = computed(() => {
  if (!keyword.value) return rows.value
  const matcher = keyword.value.toLowerCase()
  return rows.value.filter((item) =>
    [item.code, item.applicantName, item.summary, item.category].filter(Boolean).some((field) => String(field).toLowerCase().includes(matcher))
  )
})

const stats = computed(() => ({
  pending: rows.value.filter((item) => item.status === 1).length,
  approved: rows.value.filter((item) => item.status === 2).length,
  rejected: rows.value.filter((item) => item.status === 3).length,
}))

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      scope: scope.value,
      ...(statusFilter.value ? { status: Number(statusFilter.value) } : {}),
    }
    if (activeTab.value === 'leave') {
      const data = await listLeaveApprovals(params)
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
        raw: item,
      }))
    } else {
      const data = await listFinanceApprovals(params)
      rows.value = (data || []).map((item) => ({
        type: 'finance',
        typeLabel: '财务审批',
        code: item.approvalCode,
        applicantName: item.applyUserName,
        departmentName: item.applyDepartmentName,
        category: item.category,
        summary: `金额 ¥${item.amount} / ${item.reason}`,
        auditorName: item.auditorName,
        status: item.status,
        statusText: item.statusText,
        createTime: item.createTime,
        raw: item,
      }))
    }
  } finally {
    loading.value = false
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
  statusFilter.value = ''
  keyword.value = ''
}

watch([activeTab, scope], fetchList)

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
    }
    detailTitle.value = '财务审批详情'
  }
  detailVisible.value = true
}

const quickAudit = async (item, action) => {
  await doAudit(item, action, '')
}

const submitAudit = async (action) => {
  if (!detailData.value) return
  await doAudit({ type: detailData.value.type, code: detailData.value.code }, action, auditComment.value)
  detailVisible.value = false
}

const doAudit = async (item, action, comment) => {
  if (item.type === 'leave') {
    await auditLeaveApproval({ leaveCode: item.code, action, comment })
  } else {
    await auditFinanceApproval({ approvalCode: item.code, action, comment })
  }
  ElMessage.success(action === 1 ? '审批已通过' : '审批已拒绝')
  await fetchList()
}

const submitFinance = async () => {
  if (!financeForm.category || !financeForm.amount || !financeForm.reason) {
    ElMessage.warning('请完整填写财务申请信息')
    return
  }
  await submitFinanceApproval({
    category: financeForm.category,
    amount: Number(financeForm.amount),
    reason: financeForm.reason,
  })
  ElMessage.success('财务申请已提交')
  financeDialogVisible.value = false
  financeForm.category = ''
  financeForm.amount = ''
  financeForm.reason = ''
  if (activeTab.value === 'finance' && scope.value === 'mine') {
    await fetchList()
  }
}

const formatTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

const statusClass = (status) => {
  if (status === 1) return 'bg-amber-100 text-amber-700'
  if (status === 2) return 'bg-emerald-100 text-emerald-700'
  if (status === 3) return 'bg-rose-100 text-rose-700'
  return 'bg-surface-container-high text-on-surface-variant'
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

onMounted(fetchList)
</script>