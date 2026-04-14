<template>
  <div class="p-8 space-y-8 w-full h-full overflow-y-auto bg-surface text-on-surface font-body">

    <div class="flex justify-between items-end">
      <div>
        <h2 class="text-3xl font-extrabold text-primary tracking-tight">租户管理</h2>
        <p class="text-on-surface-variant text-sm mt-1">Manage global enterprise textile tenants and subscription lifecycle.</p>
      </div>
      <button class="bg-primary hover:bg-primary-container text-white px-6 py-2.5 rounded-lg flex items-center gap-2 shadow-lg transition-transform active:scale-95 duration-150">
        <span class="material-symbols-outlined text-sm">add</span>
        <span class="font-semibold text-sm">新增租户</span>
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-surface-container-lowest p-6 rounded-xl shadow-[0px_20px_40px_rgba(0,32,69,0.06)] flex flex-col justify-between border-l-4 border-primary">
        <div class="flex justify-between items-start">
          <span class="text-xs font-bold text-on-surface-variant tracking-widest uppercase">总租户数</span>
          <span class="material-symbols-outlined text-primary/20 text-3xl">groups</span>
        </div>
        <div class="mt-4 flex items-baseline gap-2">
          <span class="text-4xl font-black text-primary">{{ stats.total }}</span>
          <span class="text-xs font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">trending_up</span> {{ stats.growth }}
          </span>
        </div>
      </div>

      <div class="bg-surface-container-lowest p-6 rounded-xl shadow-[0px_20px_40px_rgba(0,32,69,0.06)] flex flex-col justify-between border-l-4 border-secondary">
        <div class="flex justify-between items-start">
          <span class="text-xs font-bold text-on-surface-variant tracking-widest uppercase">活跃租户</span>
          <span class="material-symbols-outlined text-secondary/20 text-3xl" style="font-variation-settings: 'FILL' 1;">bolt</span>
        </div>
        <div class="mt-4 flex items-baseline gap-2">
          <span class="text-4xl font-black text-primary">{{ stats.active }}</span>
          <span class="text-xs text-on-surface-variant">{{ stats.uptime }} uptime</span>
        </div>
      </div>

      <div class="relative overflow-hidden bg-white/70 backdrop-blur-xl p-6 rounded-xl shadow-[0px_20px_40px_rgba(0,32,69,0.06)] flex flex-col justify-between border-l-4 border-tertiary">
        <div class="absolute top-0 right-0 p-8 opacity-10 pointer-events-none">
          <span class="material-symbols-outlined text-8xl">event_busy</span>
        </div>
        <div class="flex justify-between items-start">
          <span class="text-xs font-bold text-on-surface-variant tracking-widest uppercase">本月到期</span>
          <span class="material-symbols-outlined text-tertiary text-2xl" style="font-variation-settings: 'FILL' 1;">notification_important</span>
        </div>
        <div class="mt-4 flex items-baseline gap-2">
          <span class="text-4xl font-black text-primary">{{ stats.expiring }}</span>
          <span class="text-xs font-semibold text-tertiary bg-tertiary-fixed px-2 py-0.5 rounded-full">需跟进</span>
        </div>
      </div>
    </div>

    <section class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col">
      <div class="p-4 bg-surface-container-low flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-2">
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3 top-2 text-slate-400 text-lg">filter_list</span>
            <select v-model="filters.version" class="pl-10 pr-8 py-2 bg-white border-none rounded-lg text-sm text-on-surface-variant focus:ring-1 focus:ring-primary shadow-sm appearance-none outline-none">
              <option value="all">所有版本</option>
              <option value="Professional">Professional</option>
              <option value="Enterprise">Enterprise</option>
              <option value="Basic">Basic</option>
            </select>
          </div>
          <div class="relative">
            <select v-model="filters.status" class="pl-4 pr-8 py-2 bg-white border-none rounded-lg text-sm text-on-surface-variant focus:ring-1 focus:ring-primary shadow-sm appearance-none outline-none">
              <option value="all">所有状态</option>
              <option value="Active">Active</option>
              <option value="Pending">Pending</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>
        </div>
        <div class="text-xs text-on-surface-variant font-medium">
          Showing 1-10 of {{ stats.total }} tenants
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
          <tr class="bg-surface-container-low/50">
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">租户名称</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">租户代码</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">联系人</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">版本</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">到期日期</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase">状态</th>
            <th class="px-6 py-4 text-xs font-bold text-on-surface-variant tracking-widest uppercase text-right">操作</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-surface-variant">
          <tr v-for="tenant in filteredTenants" :key="tenant.code" class="hover:bg-surface-container-high transition-colors group">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <div :class="['w-8 h-8 rounded flex items-center justify-center font-bold text-xs', tenant.avatarColor]">
                  {{ tenant.initials }}
                </div>
                <div>
                  <div class="text-sm font-semibold text-primary">{{ tenant.name }}</div>
                  <div class="text-xs text-on-surface-variant">{{ tenant.location }}</div>
                </div>
              </div>
            </td>
            <td class="px-6 py-4 text-sm font-mono text-on-surface-variant">{{ tenant.code }}</td>
            <td class="px-6 py-4">
              <div class="text-sm text-on-surface">{{ tenant.contact }}</div>
              <div class="text-xs text-on-surface-variant">{{ tenant.email }}</div>
            </td>
            <td class="px-6 py-4">
                <span :class="['text-xs font-bold px-2 py-1 rounded', tenant.versionStyle]">
                  {{ tenant.version }}
                </span>
            </td>
            <td class="px-6 py-4 text-sm text-on-surface">{{ tenant.expiryDate }}</td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-1.5">
                <div :class="['w-2 h-2 rounded-full', tenant.statusColor]"></div>
                <span class="text-xs font-medium text-on-surface">{{ tenant.status }}</span>
              </div>
            </td>
            <td class="px-6 py-4 text-right">
              <button class="text-slate-400 hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-xl">more_vert</span>
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="p-6 bg-surface-container-low/30 flex items-center justify-between border-t border-surface-variant">
        <button class="flex items-center gap-2 text-sm font-semibold text-on-surface-variant hover:text-primary transition-colors disabled:opacity-50" disabled>
          <span class="material-symbols-outlined text-lg">chevron_left</span>
          Previous
        </button>
        <div class="flex items-center gap-1">
          <button class="w-8 h-8 rounded-lg bg-primary text-white text-xs font-bold">1</button>
          <button class="w-8 h-8 rounded-lg hover:bg-surface-container-high text-xs font-bold transition-colors">2</button>
          <button class="w-8 h-8 rounded-lg hover:bg-surface-container-high text-xs font-bold transition-colors">3</button>
          <span class="px-2 text-xs text-on-surface-variant">...</span>
          <button class="w-8 h-8 rounded-lg hover:bg-surface-container-high text-xs font-bold transition-colors">128</button>
        </div>
        <button class="flex items-center gap-2 text-sm font-semibold text-on-surface-variant hover:text-primary transition-colors">
          Next
          <span class="material-symbols-outlined text-lg">chevron_right</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// --- 核心业务数据：看板统计 ---
const stats = ref({
  total: '1,284',
  growth: '12%',
  active: '1,156',
  uptime: '90.1%',
  expiring: '42'
})

// --- 核心业务数据：表格筛选状态 ---
const filters = ref({
  version: 'all',
  status: 'all'
})

// --- 核心业务数据：租户列表 ---
const tenantsData = ref([
  {
    name: '华伦纺织集团',
    location: 'Shanghai, China',
    code: 'HL-TX-2024',
    contact: '张伟',
    email: 'zhang.w@hualun.com',
    version: 'Enterprise',
    expiryDate: '2025-12-12',
    status: 'Active',
    initials: 'HL',
    avatarColor: 'bg-primary-fixed text-primary',
    versionStyle: 'bg-primary-container text-on-primary-fixed-variant',
    statusColor: 'bg-emerald-500'
  },
  {
    name: '胜龙针织有限公司',
    location: 'Guangzhou, China',
    code: 'SL-KNIT-092',
    contact: '李芳',
    email: 'li.f@slknit.cn',
    version: 'Professional',
    expiryDate: '2024-11-20',
    status: 'Pending',
    initials: 'SL',
    avatarColor: 'bg-secondary-fixed text-secondary',
    versionStyle: 'bg-secondary-container text-on-secondary-container',
    statusColor: 'bg-tertiary'
  },
  {
    name: '杰美服饰工艺',
    location: 'Zhejiang, China',
    code: 'JM-APPA-77',
    contact: '王杰',
    email: 'wang.j@jiemei.org',
    version: 'Basic',
    expiryDate: '2024-05-30',
    status: 'Inactive',
    initials: 'JM',
    avatarColor: 'bg-tertiary-fixed text-tertiary',
    versionStyle: 'bg-outline-variant text-on-surface-variant',
    statusColor: 'bg-error'
  },
  {
    name: '波特印染科技',
    location: 'Jiangsu, China',
    code: 'BT-DYE-884',
    contact: '陈思思',
    email: 'chen.ss@boter.com',
    version: 'Enterprise',
    expiryDate: '2026-02-15',
    status: 'Active',
    initials: 'BT',
    avatarColor: 'bg-primary-fixed text-primary',
    versionStyle: 'bg-primary-container text-on-primary-fixed-variant',
    statusColor: 'bg-emerald-500'
  }
])

// --- 核心业务逻辑：表格筛选计算属性 ---
const filteredTenants = computed(() => {
  return tenantsData.value.filter(tenant => {
    const matchVersion = filters.value.version === 'all' || tenant.version === filters.value.version
    const matchStatus = filters.value.status === 'all' || tenant.status === filters.value.status
    return matchVersion && matchStatus
  })
})
</script>

<style scoped>
/* 确保 Material Symbols 图标字体正常加载 */
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>