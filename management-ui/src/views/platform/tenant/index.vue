<template>
  <div class="h-full min-h-0 flex flex-col gap-4 overflow-hidden bg-surface text-on-surface">
    <section class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15">
      <div class="flex items-center justify-between gap-4">
        <div>
          <p class="text-[11px] font-black tracking-[0.28em] uppercase text-primary/70">平台视图</p>
          <h1 class="mt-1 text-3xl font-black tracking-tight text-primary">租户管理</h1>
          <p class="mt-1 text-sm text-on-surface-variant">仅平台超管可见，用于查看租户整体状态与续费风险。</p>
        </div>
        <button class="shrink-0 rounded-2xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition-all hover:opacity-90 active:scale-95">
          <span class="material-symbols-outlined mr-1.5 text-[18px] align-[-3px]">add</span>
          新增租户
        </button>
      </div>
    </section>

    <section class="grid grid-cols-4 gap-4">
      <article
        v-for="card in statCards"
        :key="card.title"
        class="rounded-3xl bg-surface-container-lowest px-5 py-4 shadow-sm ring-1 ring-outline-variant/15"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-[11px] font-black tracking-[0.2em] uppercase text-on-surface-variant">{{ card.title }}</p>
            <p class="mt-3 text-4xl font-black tracking-tight text-primary">{{ card.value }}</p>
            <p class="mt-2 text-xs font-bold" :class="card.subTextClass">{{ card.subText }}</p>
          </div>
          <div :class="card.iconClass" class="flex h-11 w-11 items-center justify-center rounded-2xl">
            <span class="material-symbols-outlined text-[22px]">{{ card.icon }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="flex-1 min-h-0 rounded-3xl bg-surface-container-lowest shadow-sm ring-1 ring-outline-variant/15 overflow-hidden flex flex-col">
      <div class="flex items-center justify-between gap-4 border-b border-outline-variant/10 px-5 py-3">
        <div class="flex items-center gap-3">
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px]">search</span>
            <input
              v-model.trim="filters.keyword"
              type="text"
              placeholder="搜索租户名称、编码或联系人"
              class="w-72 rounded-2xl bg-surface-container-low pl-10 pr-4 py-2.5 text-sm outline-none ring-1 ring-transparent transition-all focus:ring-primary/30"
            />
          </div>
          <select v-model="filters.version" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none">
            <option value="all">全部版本</option>
            <option value="Professional">Professional</option>
            <option value="Enterprise">Enterprise</option>
            <option value="Basic">Basic</option>
          </select>
          <select v-model="filters.status" class="rounded-2xl bg-surface-container-low px-4 py-2.5 text-sm outline-none">
            <option value="all">全部状态</option>
            <option value="Active">Active</option>
            <option value="Pending">Pending</option>
            <option value="Inactive">Inactive</option>
          </select>
        </div>
        <div class="text-xs font-bold text-on-surface-variant">
          当前显示 {{ filteredTenants.length }} / {{ tenantsData.length }} 个租户
        </div>
      </div>

      <div class="flex-1 min-h-0 overflow-auto">
        <table class="w-full min-w-[1080px] border-collapse text-left">
          <thead class="sticky top-0 z-10 bg-surface-container-low">
            <tr>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">租户名称</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">租户编码</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">联系人</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">版本</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">到期日期</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant">状态</th>
              <th class="px-5 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-on-surface-variant text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="tenant in filteredTenants" :key="tenant.code" class="hover:bg-surface-container-low/55 transition-colors">
              <td class="px-5 py-3">
                <div class="flex items-center gap-3">
                  <div :class="tenant.avatarColor" class="flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl text-xs font-black">
                    {{ tenant.initials }}
                  </div>
                  <div class="min-w-0">
                    <p class="truncate text-sm font-black text-primary">{{ tenant.name }}</p>
                    <p class="mt-0.5 truncate text-xs text-on-surface-variant">{{ tenant.location }}</p>
                  </div>
                </div>
              </td>
              <td class="px-5 py-3 text-sm font-bold text-on-surface">{{ tenant.code }}</td>
              <td class="px-5 py-3">
                <p class="text-sm font-bold text-on-surface">{{ tenant.contact }}</p>
                <p class="mt-0.5 text-xs text-on-surface-variant">{{ tenant.email }}</p>
              </td>
              <td class="px-5 py-3">
                <span :class="tenant.versionStyle" class="rounded-xl px-2.5 py-1 text-xs font-black">
                  {{ tenant.version }}
                </span>
              </td>
              <td class="px-5 py-3 text-sm font-medium text-on-surface">{{ tenant.expiryDate }}</td>
              <td class="px-5 py-3">
                <span class="inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-xs font-black" :class="tenant.statusBadge">
                  <span :class="tenant.statusColor" class="h-2 w-2 rounded-full"></span>
                  {{ tenant.status }}
                </span>
              </td>
              <td class="px-5 py-3 text-right">
                <button class="rounded-xl px-3 py-1.5 text-xs font-black text-primary hover:bg-primary/10 transition-colors">
                  查看
                </button>
              </td>
            </tr>
            <tr v-if="filteredTenants.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-sm text-on-surface-variant">没有符合条件的租户</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between border-t border-outline-variant/10 px-5 py-3">
        <p class="text-xs text-on-surface-variant">首屏优先展示关键租户状态，详情操作可再展开。</p>
        <div class="flex items-center gap-1.5">
          <button class="h-8 w-8 rounded-xl border border-outline-variant/20 text-xs font-black text-on-surface-variant" disabled>
            <span class="material-symbols-outlined text-[18px] align-[-4px]">chevron_left</span>
          </button>
          <button class="h-8 min-w-8 rounded-xl bg-primary px-2 text-xs font-black text-white">1</button>
          <button class="h-8 min-w-8 rounded-xl px-2 text-xs font-black text-on-surface-variant hover:bg-surface-container-low">2</button>
          <button class="h-8 min-w-8 rounded-xl px-2 text-xs font-black text-on-surface-variant hover:bg-surface-container-low">3</button>
          <button class="h-8 w-8 rounded-xl border border-outline-variant/20 text-xs font-black text-on-surface-variant">
            <span class="material-symbols-outlined text-[18px] align-[-4px]">chevron_right</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'

const filters = reactive({
  keyword: '',
  version: 'all',
  status: 'all'
})

const tenantsData = [
  {
    name: '华伦纺织集团',
    location: '上海，中国',
    code: 'HL-TX-2024',
    contact: '张伟',
    email: 'zhang.w@hualun.com',
    version: 'Enterprise',
    expiryDate: '2025-12-12',
    status: 'Active',
    initials: 'HL',
    avatarColor: 'bg-primary/10 text-primary',
    versionStyle: 'bg-primary/10 text-primary',
    statusColor: 'bg-emerald-500',
    statusBadge: 'bg-emerald-50 text-emerald-700'
  },
  {
    name: '胜龙针织有限公司',
    location: '广州，中国',
    code: 'SL-KNIT-092',
    contact: '李芳',
    email: 'li.f@slknit.cn',
    version: 'Professional',
    expiryDate: '2024-11-20',
    status: 'Pending',
    initials: 'SL',
    avatarColor: 'bg-secondary/10 text-secondary',
    versionStyle: 'bg-secondary/10 text-secondary',
    statusColor: 'bg-amber-500',
    statusBadge: 'bg-amber-50 text-amber-700'
  },
  {
    name: '杰美服饰工艺',
    location: '浙江，中国',
    code: 'JM-APPA-77',
    contact: '王杰',
    email: 'wang.j@jiemei.org',
    version: 'Basic',
    expiryDate: '2024-05-30',
    status: 'Inactive',
    initials: 'JM',
    avatarColor: 'bg-slate-200 text-slate-700',
    versionStyle: 'bg-slate-200 text-slate-700',
    statusColor: 'bg-rose-500',
    statusBadge: 'bg-rose-50 text-rose-700'
  },
  {
    name: '波特印染科技',
    location: '江苏，中国',
    code: 'BT-DYE-884',
    contact: '陈思思',
    email: 'chen.ss@boter.com',
    version: 'Enterprise',
    expiryDate: '2026-02-15',
    status: 'Active',
    initials: 'BT',
    avatarColor: 'bg-primary/10 text-primary',
    versionStyle: 'bg-primary/10 text-primary',
    statusColor: 'bg-emerald-500',
    statusBadge: 'bg-emerald-50 text-emerald-700'
  }
]

const statCards = computed(() => [
  {
    title: '总租户数',
    value: '1,284',
    subText: '较上月 +12%',
    subTextClass: 'text-emerald-600',
    icon: 'groups',
    iconClass: 'bg-primary/10 text-primary'
  },
  {
    title: '活跃租户',
    value: '1,156',
    subText: '状态正常可使用',
    subTextClass: 'text-on-surface-variant',
    icon: 'bolt',
    iconClass: 'bg-secondary/10 text-secondary'
  },
  {
    title: '本月到期',
    value: '42',
    subText: '建议尽快跟进',
    subTextClass: 'text-amber-700',
    icon: 'event_busy',
    iconClass: 'bg-amber-100 text-amber-700'
  },
  {
    title: '停用租户',
    value: '18',
    subText: '需排查续费与状态',
    subTextClass: 'text-rose-700',
    icon: 'block',
    iconClass: 'bg-rose-100 text-rose-700'
  }
])

const filteredTenants = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return tenantsData.filter((tenant) => {
    const matchKeyword =
      !keyword ||
      tenant.name.toLowerCase().includes(keyword) ||
      tenant.code.toLowerCase().includes(keyword) ||
      tenant.contact.toLowerCase().includes(keyword)

    const matchVersion = filters.version === 'all' || tenant.version === filters.version
    const matchStatus = filters.status === 'all' || tenant.status === filters.status
    return matchKeyword && matchVersion && matchStatus
  })
})
</script>

<style scoped>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24;
}
</style>
